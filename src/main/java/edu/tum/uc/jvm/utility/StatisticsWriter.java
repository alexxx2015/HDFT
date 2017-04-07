package edu.tum.uc.jvm.utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class StatisticsWriter implements Runnable {
	private String filename;
	private ClassNode classNode;
	private byte[] instrumented;
	private long time;

	private static int totalNumMethods = 0;
	private static int totalNumBytecode = 0;
	private static int totalNumClasses = 0;

	private static int totalNumInstrMethods = 0;
	private static int totalNumInstrBytecode = 0;
	private static int totalNumInstrClasses = 0;
	
	public static long time4GenSinkSource = 0;
	public static int numObj = 0;
	private static Map<String,Integer> countSinkSource = new HashMap<String,Integer>();

	private static Map<String, Long> executionTimeT3 = new HashMap<String, Long>();// Per
																					// Source/Sink
	private static Map<String, Long> executionTimeT4 = new HashMap<String, Long>();// Native
																					// Execution
	private static Map<String, Long> executionTimeT5 = new HashMap<String, Long>();// Network
	private static StringBuilder ToBeDumpedData = new StringBuilder();

	private static long executionTimeT1 = 0;
	private static long executionTimerT2 = 0;

	StatisticsWriter(ClassNode cn, byte[] instrumented, long time) {
		this.classNode = cn;
		this.instrumented = instrumented;
		this.time = time;
	}

	public static void clear() {
		totalNumMethods = 0;
		totalNumBytecode = 0;
		totalNumClasses = 0;

		totalNumInstrMethods = 0;
		totalNumInstrBytecode = 0;
		totalNumInstrClasses = 0;

		executionTimeT3 = new HashMap<String, Long>();
		executionTimeT5 = new HashMap<String, Long>();
		ToBeDumpedData = new StringBuilder();

		executionTimeT1 = 0;
	}

	public static void logExecutionTimerT1(long start, long end) {
		executionTimeT1 = end - start;
		// System.out.println("Execution "+executionTimeTotal+", Start "+start+", End "+end);
	}

	public static void logExectionTimerT2(long start, long end) {
		executionTimerT2 += end - start;
		// System.out.println("Execution "+executionTimeTotal+", Start "+start+", End "+end);
	}

	public static void logExecutionTimerT3(String event, long totalTime) {
		totalTime += executionTimeT3.containsKey(event) ? executionTimeT3.get(event) : 0;
		executionTimeT3.put(event, totalTime);
		
		int countsinksource = countSinkSource.containsKey(event) ? countSinkSource.get(event) : 0;
		countSinkSource.put(event, ++countsinksource);			
	}

	public static void logExecutionTimerT4(String event, long totalTime) {
		if (executionTimeT4.containsKey(event))
			totalTime += executionTimeT4.get(event);
		executionTimeT4.put(event, totalTime);
	}

	public static void logExecutionTimerT5(String event, long networkTime) {
		if (executionTimeT5.containsKey(event))
			networkTime += executionTimeT5.get(event);
		executionTimeT5.put(event, networkTime);
	}

	public static void dumpFile(String filename) {
		File f = new File(filename);
		FileWriter fw;
		try {
			if (!f.getParentFile().exists()) {
				f.getParentFile().mkdirs();
				fw = new FileWriter(f);
			} else {
				fw = new FileWriter(f, true);
			}
			// Add Instrumentationdata
			StringBuilder sb = new StringBuilder();
			sb.append("---- INSTRUMENTATION STATISTIC ----\n");
			sb.append(ToBeDumpedData).append("\n");

			sb.append("---- RUNTIME STATISTIC ----\n");

			Set<String> keySet = new HashSet<String>();
			keySet.addAll(executionTimeT3.keySet());
			keySet.addAll(executionTimeT4.keySet());
			keySet.addAll(executionTimeT5.keySet());

			if (keySet.size() > 0) {
				Iterator<String> runtimeExecIt = keySet.iterator();
				long timer3 = 0;
				long timer4 = 0;
				long timer5 = 0;
				while (runtimeExecIt.hasNext()) {
					String key = runtimeExecIt.next();
					long t3 = 0;
					if(executionTimeT3.containsKey(key))
						t3 = executionTimeT3.get(key);
					long t4 = 0;
					if(executionTimeT4.containsKey(key))
						t4 = executionTimeT4.get(key);
					long t5 = 0;
					if(executionTimeT5.containsKey(key))
						t5 = executionTimeT5.get(key);
					int count = 0;
					if(countSinkSource.containsKey(key))
						count = countSinkSource.get(key); 
					sb.append("Timer 3: " + t3 + " ns, Timer 4: " + t4
							+ " ns, Timer 5:" + t5 + " ns, Count: "+count+", " + key + "\n");
					timer3 += t3;
					timer4 += t4;
					timer5 += t5;
				}
				sb.append("=== Timer3 total: " + timer3 + " ns, Timer4 total: "
						+ timer4 + " ns, Timer5 total: " + timer5 + " ns ===\n");
			}
			sb.append("===Timer1 total: " + executionTimeT1
					+ " ns, Timer2 total: " + executionTimerT2 + " ns, Time for generating Sink/Source event objects: "+time4GenSinkSource+" ns, Generated #Sink/Source event objects: "+numObj+"===\n");
			
			fw.append(sb.toString());
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// Prepare non-instrumented data
		int numBytecodes = 0;
		if (this.classNode.methods != null) {
			Iterator<MethodNode> it = this.classNode.methods.iterator();
			while (it.hasNext()) {
				MethodNode md = it.next();
				if (md.instructions != null) {
					numBytecodes += md.instructions.size();
					this.addTotalNumBytecode(md.instructions.size());
				}
			}
			this.addTotalNumMethods(this.classNode.methods.size());
		}
		this.addTotalNumClasses(1);

		StringBuilder sb = new StringBuilder("=== ").append("Class:")
				.append(this.classNode.name).append(" ===\n");

		// Prepare instrumented-data
		ClassReader cr = new ClassReader(this.instrumented);
		ClassNode cnInstrumentd = new ClassNode();
		cr.accept(cnInstrumentd, 0);
		int numInstrBytecodes = 0;
		if (cnInstrumentd.methods != null) {
			Iterator<MethodNode> it = cnInstrumentd.methods.iterator();
			while (it.hasNext()) {
				MethodNode md = it.next();
				if (md.instructions != null) {
					sb.append(">Method: ").append(md.name).append("\n");
					sb.append("#Bytecodes (instr): ")
							.append(md.instructions.size()).append("\n");
					Iterator<MethodNode> it2 = this.classNode.methods
							.iterator();
					while (it2.hasNext()) {
						MethodNode md2 = it2.next();
						if (md2.instructions != null
								&& md.name.equals(md2.name) && md.desc.equals(md2.desc)) {
							sb.append("#Bytecodes: ")
									.append(md2.instructions.size())
									.append("\n");
						}
					}
					sb.append("\n");
					numInstrBytecodes += md.instructions.size();
					this.addTotalNumInstrBytecode(md.instructions.size());
				}
			}
			this.addTotalNumInstrMethods(cnInstrumentd.methods.size());
		}
		this.addTotalNumInstrClasses(1);

		sb.append("Total #Method: ").append(this.classNode.methods.size())
				.append("\n");
		sb.append("Total #Bytecodes: ").append(numBytecodes).append("\n");
		sb.append("Total #Bytecodes (instrumented): ")
				.append(numInstrBytecodes).append("\n");
		sb.append("Total time for instrumentation: ").append(this.time)
				.append(" ns").append("\n===");
		ToBeDumpedData.append(sb);
		// sb.append("=== ").append("#totalClasses: ")
		// .append(totalNumClasses).append(", #totalMethod: ")
		// .append(StatisticsWriter.totalNumMethods)
		// .append(", #totalByteCode: ").append(totalNumBytecode)
		// .append(", #totalByteCode (instr): ").append(totalNumInstrBytecode)
		// .append(", time for instrumentation: ").append(this.time).append(" ms")
		// .append(" ===\n");
		// }
	}

	synchronized private void addTotalNumBytecode(int i) {
		this.totalNumBytecode += i;
	}

	synchronized private void addTotalNumClasses(int i) {
		this.totalNumClasses += i;
	}

	synchronized private void addTotalNumMethods(int i) {
		this.totalNumMethods += i;
	}

	synchronized private void addTotalNumInstrBytecode(int i) {
		this.totalNumInstrBytecode += i;
	}

	synchronized private void addTotalNumInstrClasses(int i) {
		this.totalNumInstrClasses += i;
	}

	synchronized private void addTotalNumInstrMethods(int i) {
		this.totalNumInstrMethods += i;
	}

	public static void logInstrumentation(ClassNode cn, byte[] instrumented,
			long time) {
		StatisticsWriter sw = new StatisticsWriter(cn, instrumented, time);
		sw.run();
//		Thread t = new Thread(sw);
//		 t.start();
//		t.run();
	}

}
