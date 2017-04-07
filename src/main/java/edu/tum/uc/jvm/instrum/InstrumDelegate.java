package edu.tum.uc.jvm.instrum;

import java.lang.reflect.Field;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.restfb.Parameter;

import de.tum.in.i22.uc.cm.datatypes.basic.EventBasic;
import de.tum.in.i22.uc.cm.datatypes.basic.StatusBasic.EStatus;
import de.tum.in.i22.uc.cm.datatypes.interfaces.IEvent;
import de.tum.in.i22.uc.cm.datatypes.interfaces.IResponse;
import de.tum.in.i22.uc.cm.datatypes.java.names.SourceSinkName;
import edu.tum.uc.jvm.UcCommunicator;
import edu.tum.uc.jvm.extractor.FileDescriptorExtractor;
import edu.tum.uc.jvm.extractor.IExtractor;
import edu.tum.uc.jvm.utility.ConfigProperties;
import edu.tum.uc.jvm.utility.UnsafeUtil;
import edu.tum.uc.jvm.utility.Utility;
import edu.tum.uc.jvm.utility.analysis.Flow;
import edu.tum.uc.jvm.utility.analysis.SinkSource;
import edu.tum.uc.jvm.utility.analysis.StaticAnalysis;
import edu.tum.uc.jvm.utility.eval.JavaEventName;
import edu.tum.uc.jvm.utility.eval.StatisticsUtil;

/**
 * This class acts as the delegate of the instrumentation. Calls to public
 * methods of this class are inserted into bytecode to generate events.
 * 
 * @author alex
 *
 */
public class InstrumDelegate {

	/**
	 * The singleton instance of UcCommunicator.
	 */
	private static UcCommunicator ucCom = UcCommunicator.getInstance();

	/**
	 * A set of the fully qualified names of wrapper methods to know during the
	 * instrumentation which methods have already been instrumented.
	 */
	public static Set<String> HelperMethods = new HashSet<String>();

	/**
	 * A set of fully qualified class names to be able to retrieve its
	 * instrumentation status.
	 */
	private static Set<String> InstrumentedClasses = new HashSet<String>();

	/**
	 * A list of all sinks AND sources from the used JOANA report.
	 */
	private static List<SinkSource> sinksAndSources = new Vector<SinkSource>();
	private static boolean EVENTTIMER = false;
	static {
		sinksAndSources.addAll(StaticAnalysis.getSources());
		sinksAndSources.addAll(StaticAnalysis.getSinks());
		EVENTTIMER = Boolean.parseBoolean(ConfigProperties.getProperty(ConfigProperties.PROPERTIES.EVENTTIMER));
	}
	private static IExtractor FileExt = new FileDescriptorExtractor();

	/**
	 * Returns true if the class with the given name is instrumented.
	 * 
	 * @param className
	 *            The name of the given class.
	 * @return The instrumentation status of the class with the given name.
	 */
	public static boolean classIsInstrumented(String className) {
		return InstrumentedClasses.contains(className);
	}

	/**
	 * Adds a class with the given name to the set of instrumented classes.
	 * 
	 * @param className
	 *            The name of the given class.
	 */
	public static void addInstrumentedClassName(String className) {
		InstrumentedClasses.add(className.replace("/", "."));
	}

	/**
	 * Starts a timer in <code> StatisticsUtil </code> for the given method.
	 * 
	 * @param methodFQName
	 *            The fully qualified name of the given method.
	 */
	public static void startMethodTimer(String methodFQName) {
		StatisticsUtil.startMethodTimer(methodFQName);
	}

	/**
	 * Stops the timer in <code> StatisticsUtil </code> for the given method.
	 * 
	 * @param methodFQName
	 *            The fully qualified name of the given method.
	 */
	public static void stopMethodTimer(String methodFQName) {
		StatisticsUtil.stopMethodTimer(methodFQName);
	}

	/**
	 * Writes the statistics collected in <code>StatisticsUtil</code> to a file
	 * on disk.
	 */
	public static void dumpStatistics() {
		StatisticsUtil.writeToFile();
	}

	/**
	 * Starts an event timer in <code>StatisticsUtil</code> with the ifs of all
	 * sources and sinks found in <code>sinksAndSources</code> (using the given
	 * owner method name and bytecode offset) attached.
	 * 
	 * @param eventName
	 *            The name of the event to start a timer for.
	 * @param bci
	 *            The bytecode offset inside the owner method where the event is
	 *            thrown.
	 * @param cnOwnerMethod
	 *            The owner method name.
	 * @param cnLabel
	 *            The chopnode label from the JOANA report.
	 */
	public static void startEventTimer(String eventName, int bci, String cnOwnerMethod, String cnLabel) {
		Set<String> sinkSources = new HashSet<>();
		for (SinkSource currentSinkSource : sinksAndSources) {
			if (currentSinkSource.getLocation().equals(cnOwnerMethod) && currentSinkSource.getOffset() == bci) {
				sinkSources.add(currentSinkSource.getId());
			}
		}
		StatisticsUtil.startEventTimer(eventName, bci, cnOwnerMethod, cnLabel, sinkSources);
	}

	/**
	 * Extracts the addresses of all objects, puts the given parameters into a
	 * map and creates a <i>ReadArray</i> event.
	 * 
	 * @param array
	 *            The array where the read operation is performed on.
	 * @param index
	 *            The index specifying the element which is being read.
	 * @param arrayAtIndex
	 *            The object or value stored in <code>array</code> at
	 *            <code>index</code>.
	 * @param parentObject
	 *            The object where the <code>parentMethod</code> is called on.
	 *            Should be null if <code>parentMethod</code> is static.
	 * @param parentMethod
	 *            A fully qualified name of the method where the operation is
	 *            performed.
	 * @param label
	 *            The chopnode label from the JOANA report describing the
	 *            operation.
	 */
	public static void readArray(Object array, int index, Object arrayAtIndex, Object parentObject, String parentMethod,
			String label) {

		Map<String, String> eventParams = new HashMap<String, String>();
		eventParams.put("parentObjectAddress", getAddress(parentObject));
		eventParams.put("parentClass", getClass(parentObject, parentMethod));
		eventParams.put("parentMethod", getMethod(parentMethod));
		eventParams.put("arrayClass", getClass(array));
		eventParams.put("arrayAddress", getAddress(array));
		eventParams.put("index", String.valueOf(index));
		eventParams.put("elementClass", getClass(arrayAtIndex));
		eventParams.put("elementAddress", getAddress(arrayAtIndex));
		eventParams.put("chopLabel", label);
		createEvent(JavaEventName.READ_ARRAY, eventParams);

		// System.out.println();
	}

	/**
	 * Extracts the addresses of all objects, puts the given parameters into a
	 * map and creates a <i>WriteAray</i> event.
	 * 
	 * @param array
	 *            The array where the write operation is performed on.
	 * @param index
	 *            The index specifying the element where <code>value</code> is
	 *            being written.
	 * @param value
	 *            The object or value to be written into the <code>array</code>
	 *            at <code>index</code>.
	 * @param parentObject
	 *            The object where the <code>parentMethod</code> is called on.
	 *            Should be null if <code>parentMethod</code> is static.
	 * @param parentMethod
	 *            A fully qualified name of the method where the operation is
	 *            performed.
	 * @param label
	 *            The chopnode label from the JOANA report describing the
	 *            operation.
	 */
	public static void writeArray(Object array, int index, Object value, Object parentObject, String parentMethod,
			String label) {
		// System.out.println("Write array!!");
		// System.out.println("Chopnode Label = " + label);
		// System.out.println("Parent obj = " + objectToString(parentObject));
		// System.out.println("Parent Method = " + parentMethod);
		// System.out.println("Array = " + objectToString(array));
		// System.out.println("Index = " + index);
		// System.out.println("Value to insert = " + objectToString(value));

		Map<String, String> eventParams = new HashMap<String, String>();
		eventParams.put("parentObjectAddress", getAddress(parentObject));
		eventParams.put("parentClass", getClass(parentObject, parentMethod));
		eventParams.put("parentMethod", getMethod(parentMethod));
		eventParams.put("arrayClass", getClass(array));
		eventParams.put("arrayAddress", getAddress(array));
		eventParams.put("index", String.valueOf(index));
		eventParams.put("valueClass", getClass(value));
		eventParams.put("valueAddress", getAddress(value));
		eventParams.put("chopLabel", label);
		createEvent(JavaEventName.WRITE_ARRAY, eventParams);

		// System.out.println();
	}

	/**
	 * Extracts the addresses of all objects, puts the given parameters into a
	 * map and creates a <i>ReadField</i> event.
	 * 
	 * @param fieldOwnerObject
	 *            The object owning the given field. Should be null if the field
	 *            is static.
	 * @param fieldValue
	 *            The object or value stored in the given field.
	 * @param fieldOwnerClass
	 *            The class where the given field is declared.
	 * @param fieldName
	 *            The name of the given field.
	 * @param parentObject
	 *            The object where the <code>parentMethod</code> is called on.
	 *            Should be null if <code>parentMethod</code> is static.
	 * @param parentMethod
	 *            A fully qualified name of the method where the operation is
	 *            performed.
	 * @param label
	 *            The chopnode label from the JOANA report describing the
	 *            operation.
	 */
	public static void readField(Object fieldOwnerObject, Object fieldValue, String fieldOwnerClass, String fieldName,
			Object parentObject, String parentMethod, String label) {
		// System.out.println("Read field!!");
		// System.out.println("Chopnode Label = " + label);
		// System.out.println("Parent obj = " + objectToString(parentObject));
		// System.out.println("Parent Method = " + parentMethod);
		// System.out.println("Field owner object = " +
		// objectToString(fieldOwnerObject));
		// System.out.println("Field owner class = " + fieldOwnerClass);
		// System.out.println("Field name = " + fieldName);
		// System.out.println("Field value = " + objectToString(fieldValue));

		Map<String, String> eventParams = new HashMap<String, String>();
		eventParams.put("parentObjectAddress", getAddress(parentObject));
		eventParams.put("parentClass", getClass(parentObject, parentMethod));
		eventParams.put("parentMethod", getMethod(parentMethod));
		eventParams.put("fieldOwnerClass", fieldOwnerObject != null ? getClass(fieldOwnerObject) : fieldOwnerClass);
		eventParams.put("fieldOwnerAddress", getAddress(fieldOwnerObject));
		eventParams.put("fieldName", fieldName);
		eventParams.put("fieldValueClass", getClass(fieldValue));
		eventParams.put("fieldValueAddress", getAddress(fieldValue));
		eventParams.put("chopLabel", label);
		createEvent(JavaEventName.READ_FIELD, eventParams);

		// System.out.println();
	}

	/**
	 * Extracts the addresses of all objects, puts the given parameters into a
	 * map and creates a <i>WriteField</i> event.
	 * 
	 * @param fieldOwnerObject
	 *            The object owning the given field. Should be null if the field
	 *            is static.
	 * @param assignee
	 *            The object or value to be stored in the given field.
	 * @param fieldOwnerClass
	 *            The class where the given field is declared.
	 * @param fieldName
	 *            The name of the given field.
	 * @param parentObject
	 *            The object where the <code>parentMethod</code> is called on.
	 *            Should be null if <code>parentMethod</code> is static.
	 * @param parentMethod
	 *            A fully qualified name of the method where the operation is
	 *            performed.
	 * @param label
	 *            The chopnode label from the JOANA report describing the
	 *            operation.
	 */
	public static void writeField(Object fieldOwnerObject, Object assignee, String fieldOwnerClass, String fieldName,
			Object parentObject, String parentMethod, String label) {

		// System.out.println("Write field!!");
		// System.out.println("Chopnode Label = " + label);
		// System.out.println("Parent obj = " + objectToString(parentObject));
		// System.out.println("Parent Method = " + parentMethod);
		// System.out.println("Field owner object = " +
		// objectToString(fieldOwnerObject));
		// System.out.println("Field owner class = " + fieldOwnerClass);
		// System.out.println("Field name = " + fieldName);
		// System.out.println("Assignee = " + objectToString(assignee));

		Map<String, String> eventParams = new HashMap<String, String>();
		eventParams.put("parentObjectAddress", getAddress(parentObject));
		eventParams.put("parentClass", getClass(parentObject, parentMethod));
		eventParams.put("parentMethod", getMethod(parentMethod));
		eventParams.put("fieldOwnerClass", fieldOwnerObject != null ? getClass(fieldOwnerObject) : fieldOwnerClass);
		eventParams.put("fieldOwnerClassIsInstrumented",
				String.valueOf(classIsInstrumented(getClass(fieldOwnerObject, fieldOwnerClass))));
		eventParams.put("fieldOwnerAddress", getAddress(fieldOwnerObject));
		eventParams.put("fieldName", fieldName);
		eventParams.put("assigneeClass", getClass(assignee));
		eventParams.put("assigneeAddress", getAddress(assignee));
		eventParams.put("chopLabel", label);
		createEvent(JavaEventName.WRITE_FIELD, eventParams);

		// System.out.println();
	}

	/**
	 * Extracts the addresses of all objects, puts the given parameters into a
	 * map and creates a <i>UnaryAssign</i> event.
	 * 
	 * @param arg
	 *            The operand.
	 * @param parentObject
	 *            The object where the <code>parentMethod</code> is called on.
	 *            Should be null if <code>parentMethod</code> is static.
	 * @param parentMethod
	 *            A fully qualified name of the method where the operation is
	 *            performed.
	 * @param label
	 *            The chopnode label from the JOANA report describing the
	 *            operation.
	 */
	public static void unaryAssign(Object arg, Object parentObject, String parentMethod, String label) {

		// System.out.println("Unary assign operation!!");
		// System.out.println("Chopnode Label = " + label);
		// System.out.println("Parent obj = " + objectToString(parentObject));
		// System.out.println("Parent Method = " + parentMethod);
		// System.out.println("Arguments = " + objectToString(arg));

		Map<String, String> eventParams = new HashMap<String, String>();
		eventParams.put("parentObjectAddress", getAddress(parentObject));
		eventParams.put("parentClass", getClass(parentObject, parentMethod));
		eventParams.put("parentMethod", getMethod(parentMethod));
		eventParams.put("argumentClass", getClass(arg));
		eventParams.put("argumentAddress", getAddress(arg));
		eventParams.put("chopLabel", label);
		createEvent(JavaEventName.UNARY_ASSIGN, eventParams);

		// System.out.println();
	}

	/**
	 * Extracts the addresses of all objects, puts the given parameters into a
	 * map and creates a <i>BinaryAssign</i> event.
	 * 
	 * @param arg1
	 *            The first operand.
	 * @param arg2
	 *            The second operand.
	 * @param parentObject
	 *            The object where the <code>parentMethod</code> is called on.
	 *            Should be null if <code>parentMethod</code> is static.
	 * @param parentMethod
	 *            A fully qualified name of the method where the operation is
	 *            performed.
	 * @param label
	 *            The chopnode label from the JOANA report describing the
	 *            operation.
	 */
	public static void binaryAssign(Object arg1, Object arg2, Object parentObject, String parentMethod, String label) {

		// System.out.println("Binary assign operation!!");
		// System.out.println("Chopnode Label = " + label);
		// System.out.println("Parent obj = " + objectToString(parentObject));
		// System.out.println("Parent Method = " + parentMethod);
		// System.out.println("Argument 1 = " + objectToString(arg1));
		// System.out.println("Argument 2 = " + objectToString(arg2));

		Map<String, String> eventParams = new HashMap<String, String>();
		eventParams.put("parentObjectAddress", getAddress(parentObject));
		eventParams.put("parentClass", getClass(parentObject, parentMethod));
		eventParams.put("parentMethod", getMethod(parentMethod));
		eventParams.put("argument1", objectToString(arg1));
		eventParams.put("argument2", objectToString(arg2));
		eventParams.put("chopLabel", label);
		createEvent(JavaEventName.BINARY_ASSIGN, eventParams);

		// System.out.println();
	}

	/**
	 * Extracts the addresses of all objects, puts the given parameters into a
	 * map and creates a <i>CallInstanceMethod</i> event.
	 * 
	 * @param parentMethod
	 *            A fully qualified name of the method where the operation is
	 *            performed.
	 * @param label
	 *            The chopnode label from the JOANA report describing the
	 *            operation.
	 * @param calledMethod
	 *            A fully qualified name of the method whichs is being invoked.
	 * @param args
	 *            An array containing the arguments of the invoked method.
	 * @param parentObject
	 *            The object where the <code>parentMethod</code> is called on.
	 *            Should be null if <code>parentMethod</code> is static.
	 * @param caller
	 *            The object on which <code>calledMethod</code> is being called.
	 */
	public static void instanceMethodInvoked(String parentMethod, String label, String calledMethod, Object[] args,
			Object parentObject, Object caller) {
		instanceMethodInvoked(parentMethod, label, calledMethod, args, parentObject, caller, null);
	}

	public static void instanceMethodInvoked(String parentMethod, String label, String calledMethod, Object[] args,
			Object parentObject, Object callee, String p_label) {
		// System.out.println("Instance method invoked!!");
		// System.out.println("Chopnode Label = " + label);
		// System.out.println("Parent obj = " + objectToString(parentObject));
		// System.out.println("Parent Method = " + parentMethod);
		// System.out.println("callee obj = " + objectToString(caller));
		// System.out.println("Called Method = " + calledMethod);
		// System.out.println("Arguments = " +
		// JSONArray.toJSONString(Arrays.asList(objectsToStrings(args))));

		Map<String, String> eventParams = new HashMap<String, String>();
		eventParams.put("parentObjectAddress", getAddress(parentObject));
		eventParams.put("parentClass", getClass(parentObject, parentMethod));
		eventParams.put("parentMethod", getMethod(parentMethod));
		eventParams.put("calleeObjectAddress", getAddress(callee));
		eventParams.put("calleeObjectClass", getClass(callee, calledMethod));
		eventParams.put("calledMethod", getMethod(calledMethod));
		eventParams.put("methodArgTypes", JSONArray.toJSONString(Arrays.asList(getClasses(args))));
		eventParams.put("methodArgAddresses", JSONArray.toJSONString(Arrays.asList(getAddresses(args))));
		eventParams.put("calleeObjectIsInstrumented",
				String.valueOf(classIsInstrumented(getClass(callee, calledMethod))));
		eventParams.put("chopLabel", label);
		if (p_label != null)
			eventParams.put("methodLabel", p_label);
		createEvent(JavaEventName.CALL_INSTANCE_METHOD, eventParams);
	}

	/**
	 * Extracts the addresses of all objects, puts the given parameters into a
	 * map and creates a <i>CallStaticMethod</i> event.
	 * 
	 * @param parentMethod
	 *            A fully qualified name of the method where the operation is
	 *            performed.
	 * @param label
	 *            The chopnode label from the JOANA report describing the
	 *            operation.
	 * @param calledMethod
	 *            A fully qualified name of the method which is being invoked.
	 * @param args
	 *            An array containing the arguments of the invoked method.
	 * @param parentObject
	 *            The object where the <code>parentMethod</code> is called on.
	 *            Should be null if <code>parentMethod</code> is static.
	 */
	public static void staticMethodInvoked(String parentMethod, String label, String calledMethod, Object[] args,
			Object parentObject) {
		staticMethodInvoked(parentMethod, label, calledMethod, args, parentObject, null);
	}

	public static void staticMethodInvoked(String parentMethod, String label, String calledMethod, Object[] args,
			Object parentObject, String p_label) {

		// System.out.println("Static method invoked!!");
		// System.out.println("Chopnode Label = " + label);
		// System.out.println("Parent obj = " + objectToString(parentObject));
		// System.out.println("Parent Method = " + parentMethod);
		// System.out.println("Called Method = " + calledMethod);
		// System.out.println("Arguments = " +
		// JSONArray.toJSONString(Arrays.asList(objectsToStrings(args))));

		Map<String, String> eventParams = new HashMap<String, String>();
		eventParams.put("parentObjectAddress", getAddress(parentObject));
		eventParams.put("parentClass", getClass(parentObject, parentMethod));
		eventParams.put("parentMethod", getMethod(parentMethod));
		eventParams.put("calleeClass", getClass(calledMethod));
		eventParams.put("calledMethod", getMethod(calledMethod));
		eventParams.put("methodArgTypes", JSONArray.toJSONString(Arrays.asList(getClasses(args))));
		eventParams.put("methodArgAddresses", JSONArray.toJSONString(Arrays.asList(getAddresses(args))));
		eventParams.put("chopLabel", label);
		if (p_label != null)
			eventParams.put("methodLabel", p_label);
		createEvent(JavaEventName.CALL_STATIC_METHOD, eventParams);

		// System.out.println();
	}

	public static boolean sourceInvoked(Object p_sourceobj, Object p_ownerobj, String p_ownerclass,
			String p_ownermethod, Object p_parentobj, String p_parentClass, String p_parentmethodname, String p_source,
			String p_chopLabel, Object[] p_paramArgs) {
		return sourceInvoked(p_sourceobj, p_ownerobj, p_ownerclass, p_ownermethod, p_parentobj, p_parentClass,
				p_parentmethodname, p_source, p_chopLabel, p_paramArgs, null);
	}

	public static boolean sourceInvoked(Object p_sourceobj, Object p_ownerobj, String p_ownerclass,
			String p_ownermethod, Object p_parentobj, String p_parentClass, String p_parentmethodname, String p_source,
			String p_chopLabel, Object[] p_paramArgs, String p_label) {
		boolean _return = true;
		Map<String, String> contextInformation = (Map<String, String>) FileExt.extract(p_ownerobj);
		String[] sourceIds = p_source.split("\\|");
		String calleeObjMemAddr = getAddress(p_ownerobj);
		String parentObjMemAddr = getAddress(p_parentobj);
		String sourceObjMemAddr = getAddress(p_sourceobj);
		String sourceObjectClass = "";
		if (p_sourceobj != null) {
			sourceObjectClass = p_sourceobj.getClass().getCanonicalName();
		}
		for (String s : sourceIds) {
			// Object o =
			// UnsafeUtil.objectFromAddress(Long.parseLong(calleeObjMemAddr));
			SinkSource source = StaticAnalysis.getSourceById(s);
			int param = source.getParam();
			String sourceParam = "";
			if (source.isReturn()) {
				sourceParam = "ret";
			} else if (param > 0) {
				sourceParam = String.valueOf(param);
			}

			Map<String, String> eventParams = new HashMap<String, String>();
			eventParams.put("parentObjectAddress", parentObjMemAddr);
			eventParams.put("parentClass", p_parentClass);
			eventParams.put("parentMethod", p_parentmethodname);
			eventParams.put("calleeObjectClass", p_ownerclass);
			eventParams.put("calleeObjectAddress", calleeObjMemAddr);
			eventParams.put("calleeMethod", p_ownermethod);
			eventParams.put("sourceObjectAddress", sourceObjMemAddr);
			eventParams.put("sourceObjectClass", sourceObjectClass);
			eventParams.put("sourceParam", sourceParam);
			eventParams.put("sourceId", source.getId());
			eventParams.put("javaMapIdentifier", source.getId());
			eventParams.put("contextInformation", JSONObject.toJSONString(contextInformation));
			eventParams.put("chopLabel", p_chopLabel);
			eventParams.put("methodArgTypes", JSONArray.toJSONString(Arrays.asList(getClasses(p_paramArgs))));
			eventParams.put("methodArgAddresses", JSONArray.toJSONString(Arrays.asList(getAddresses(p_paramArgs))));
			eventParams.put("methodArgValues", JSONArray.toJSONString(Arrays.asList(getValues(p_paramArgs))));
			if (p_label != null)
				eventParams.put("methodLabel", p_label);
			createEvent(JavaEventName.SOURCE_INVOKED, eventParams, false);
		}
		return _return;
	}

	public static boolean sinkInvoked(Object p_ownerobj, String p_ownerclass, String p_ownermethod,
			Object[] p_ownermethodparams, Object p_parentobj, String p_parentClass, String p_parentmethodname,
			String p_source, String p_chopLabel) {
		return sinkInvoked(p_ownerobj, p_ownerclass, p_ownermethod, p_ownermethodparams, p_parentobj, p_parentClass,
				p_parentmethodname, p_source, p_chopLabel, null);
	}

	public static boolean sinkInvoked(Object p_ownerobj, String p_ownerclass, String p_ownermethod,
			Object[] p_ownermethodparams, Object p_parentobj, String p_parentClass, String p_parentmethodname,
			String p_source, String p_chopLabel, String p_label) {
		boolean _return = true;
		Map<String, String> contextInformation = (Map<String, String>) FileExt.extract(p_ownerobj);
		String[] sinkIds = p_source.split("\\|");
		String calleeObjMemAddr = getAddress(p_ownerobj);
		String parentObjMemAddr = getAddress(p_parentobj);
		List<String> dependsOnSources = new LinkedList<String>();
		for (String s : sinkIds) {
			SinkSource sink = StaticAnalysis.getSinkById(s);
			int param = sink.getParam();
			String sinkParam = "";
			if (sink.isReturn()) {
				sinkParam = "ret";
			} else {
				sinkParam = String.valueOf(param);
			}
			for (Flow f : StaticAnalysis.getFlows()) {
				if (f.getSink().equals(sink.getId())) {
					dependsOnSources = f.getSource();
					break;
				}
			}

			Map<String, String> eventParams = new HashMap<String, String>();
			eventParams.put("parentObjectAddress", parentObjMemAddr);
			eventParams.put("parentClass", p_parentClass);
			eventParams.put("parentMethod", p_parentmethodname);
			eventParams.put("calledObjectClass", p_ownerclass);
			eventParams.put("calledObjectAddress", calleeObjMemAddr);
			eventParams.put("calledMethod", p_ownermethod);
			eventParams.put("contextInformation", JSONObject.toJSONString(contextInformation));
			eventParams.put("chopLabel", p_chopLabel);
			eventParams.put("sinkParam", sinkParam);
			eventParams.put("sinkId", sink.getId());
			eventParams.put("dependsOnSources", JSONArray.toJSONString(dependsOnSources));
			eventParams.put("methodArgTypes", JSONArray.toJSONString(Arrays.asList(getClasses(p_ownermethodparams))));
			eventParams.put("methodArgAddresses",
					JSONArray.toJSONString(Arrays.asList(getAddresses(p_ownermethodparams))));
			eventParams.put("methodArgValues", JSONArray.toJSONString(Arrays.asList(getValues(p_ownermethodparams))));
			if (p_label != null)
				eventParams.put("methodLabel", p_label);
			createEvent(JavaEventName.SINK_INVOKED, eventParams);
		}
		return _return;
	}

	public static Parameter[] addSinkSourceParam(Parameter[] p_param, SourceSinkName.Type p_sinksource,
			String p_sinksourceId) {
		return Utility.addSinkSourceParam(p_param, p_sinksource, p_sinksourceId);
	}

	/**
	 * Extracts the addresses of all objects, puts the given parameters into a
	 * map and creates a <i>ReturnInstanceMethod</i> event.
	 * 
	 * @param returnValue
	 *            The object or value which was returned from
	 *            <code>calledMethod</code>. Should be null if
	 *            <code>calledMethod</code> returns void.
	 * @param argsCount
	 *            The parameter count of the called method.
	 * @param bytecodeOffset
	 *            The bytecode offset where the method call was performed.
	 * @param parentMethod
	 *            A fully qualified name of the method from which the method
	 *            call was performed.
	 * @param label
	 *            The chopnode label from the JOANA report describing the
	 *            operation.
	 * @param calledMethod
	 *            A fully qualified name of the method which has returned.
	 * @param parentObject
	 *            The object where the <code>parentMethod</code> is called on.
	 *            Should be null if <code>parentMethod</code> is static.
	 * @param callee
	 *            The object on which <code>calledMethod</code> was called.
	 */
	public static void instanceMethodReturned(Object returnValue, int argsCount, int bytecodeOffset,
			String parentMethod, String label, String calledMethod, Object parentObject, Object callee) {

		// System.out.println("Instance method returned!!");
		// System.out.println("Chopnode Label = " + label);
		// System.out.println("Parent obj = " + objectToString(parentObject));
		// System.out.println("Parent Method = " + parentMethod);
		// System.out.println("callee obj = " + objectToString(caller));
		// System.out.println("Called Method = " + calledMethod);
		// System.out.println("Return value = " + objectToString(returnValue));
		// System.out.println("Arguments count = " + argsCount);

		Map<String, String> eventParams = new HashMap<String, String>();
		eventParams.put("parentObjectAddress", getAddress(parentObject));
		eventParams.put("parentClass", getClass(parentObject, parentMethod));
		eventParams.put("parentMethod", getMethod(parentMethod));
		eventParams.put("calleeObjectAddress", getAddress(callee));
		eventParams.put("calleeObjectClass", getClass(callee, calledMethod));
		eventParams.put("calledMethod", getMethod(calledMethod));
		eventParams.put("returnValueClass", getClass(returnValue));
		eventParams.put("returnValueAddress", getAddress(returnValue));
		eventParams.put("argsCount", String.valueOf(argsCount));
		eventParams.put("sourcesMap", JSONObject.toJSONString(getSourcesMap(parentMethod, bytecodeOffset)));
		eventParams.put("calleeObjectIsInstrumented",
				String.valueOf(classIsInstrumented(getClass(callee, calledMethod))));
		eventParams.put("chopLabel", label);
		createEvent(JavaEventName.RETURN_INSTANCE_METHOD, eventParams);
	}

	/**
	 * Extracts the addresses of all objects, puts the given parameters into a
	 * map and creates a <i>ReturnStaticMethod</i> event.
	 * 
	 * @param returnValue
	 *            The object or value which was returned from
	 *            <code>calledMethod</code>. Should be null if
	 *            <code>calledMethod</code> returns void.
	 * @param argsCount
	 *            The parameter count of the called method.
	 * @param bytecodeOffset
	 *            The bytecode offset where the method call was performed.
	 * @param parentMethod
	 *            A fully qualified name of the method from which the method
	 *            call was performed.
	 * @param label
	 *            The chopnode label from the JOANA report describing the
	 *            operation.
	 * @param calledMethod
	 *            A fully qualified name of the method which has returned.
	 * @param parentObject
	 *            The object where the <code>parentMethod</code> is called on.
	 *            Should be null if <code>parentMethod</code> is static.
	 */
	public static void staticMethodReturned(Object returnValue, int argsCount, int bytecodeOffset, String parentMethod,
			String label, String calledMethod, Object parentObject) {

		// System.out.println("Static method returned!!");
		// System.out.println("Chopnode Label = " + label);
		// System.out.println("Parent obj = " + objectToString(parentObject));
		// System.out.println("Parent Method = " + parentMethod);
		// System.out.println("Called Method = " + calledMethod);
		// System.out.println("Return value = " + objectToString(returnValue));
		// System.out.println("Arguments count = " + argsCount);

		Map<String, String> eventParams = new HashMap<String, String>();
		eventParams.put("parentObjectAddress", getAddress(parentObject));
		eventParams.put("parentClass", getClass(parentObject, parentMethod));
		eventParams.put("parentMethod", getMethod(parentMethod));
		eventParams.put("calleeClass", getClass(calledMethod));
		eventParams.put("calledMethod", getMethod(calledMethod));
		eventParams.put("returnValueClass", getClass(returnValue));
		eventParams.put("returnValueAddress", getAddress(returnValue));
		eventParams.put("argsCount", String.valueOf(argsCount));
		eventParams.put("sourcesMap", JSONObject.toJSONString(getSourcesMap(parentMethod, bytecodeOffset)));
		eventParams.put("calleeClassIsInstrumented", String.valueOf(classIsInstrumented(getClass(calledMethod))));
		eventParams.put("chopLabel", label);
		createEvent(JavaEventName.RETURN_STATIC_METHOD, eventParams);

		// System.out.println();
	}

	/**
	 * Puts the given parameters into a map and creates a
	 * <i>ReturnMainMethod</i> event.
	 * 
	 * @param calledMethod
	 *            A fully qualified name of the (<code>main</code>) method which
	 *            has returned.
	 */
	public static void mainMethodReturned(String calledMethod) {
		// System.out.println("START: Main method returned!!");
		// System.out.println("Called Method = " + calledMethod);

		Map<String, String> eventParams = new HashMap<String, String>();
		eventParams.put("calleeClass", getClass(calledMethod));
		eventParams.put("calledMethod", getMethod(calledMethod));
		createEvent(JavaEventName.RETURN_MAIN_METHOD, eventParams);
		// System.out.println("END: Main method returned!!");
	}

	public static void mainMethodInvoked() {
		if (true)
			return;// AF-added
		UcCommunicator.getInstance().initPDP();
	}

	/**
	 * Extracts the addresses of all objects, puts the given parameters into a
	 * map and creates a <i>PrepareMethodReturn</i> event.
	 * 
	 * @param returnValue
	 *            The object or value being returned from the method. Should be
	 *            null if it returns void.
	 * @param parentObject
	 *            The object where the <code>parentMethod</code> is called on.
	 *            Should be null if <code>parentMethod</code> is static.
	 * @param parentMethod
	 *            A fully qualified name of the method about to return.
	 * @param label
	 *            The chopnode label from the JOANA report describing the
	 *            operation.
	 */
	public static void prepareMethodReturn(Object returnValue, Object parentObject, String parentMethod, String label) {

		// System.out.println("Prepare method return!!");
		// System.out.println("Chopnode Label = " + label);
		// System.out.println("Parent obj = " + objectToString(parentObject));
		// System.out.println("Parent Method = " + parentMethod);
		// System.out.println("Return value = " + objectToString(returnValue));

		Map<String, String> eventParams = new HashMap<String, String>();
		eventParams.put("parentObjectAddress", getAddress(parentObject));
		eventParams.put("parentClass", getClass(parentObject, parentMethod));
		eventParams.put("parentMethod", getMethod(parentMethod));
		eventParams.put("returnValueClass", getClass(returnValue));
		eventParams.put("returnValueAddress", getAddress(returnValue));
		eventParams.put("chopLabel", label);
		createEvent(JavaEventName.PREPARE_METHOD_RETURN, eventParams);

		// System.out.println();
	}

	/**
	 * Creates an event with the given name and the parameter dictionary. The
	 * event is handed to the <code>UcCommunicator</code> singleton to be sent
	 * to the PIP. Before sending the event, the creation timer for this event
	 * is stopped and a new one for the communication started (in
	 * <code>StatisticsUtil</code>). After receiving a response, this timer is
	 * stopped.
	 *
	 * @param eventName
	 *            The given name of the event to create.
	 * @param specificParams
	 *            The event parameter dictionary.
	 */
	private static void createEvent(String eventName, Map<String, String> specificParams) {
		createEvent(eventName, specificParams, true);
	}

	private static void createEvent(String eventName, Map<String, String> specificParams, boolean isActual) {
		Map<String, String> allParams = new HashMap<String, String>(specificParams);
		allParams.put("PEP", "Java");
		allParams.put("threadId", Utility.getThreadId());
		allParams.put("processId", Utility.getPID());
		IEvent event = new EventBasic(eventName, allParams, isActual);
		if (EVENTTIMER)
			StatisticsUtil.endEventCreation(eventName);
		// boolean success = ucCom.sendEvent2Pdp(event);
		IResponse response = ucCom.sendEvent(event, false); // send event to
															// PDP/PIP
		boolean success = (response != null && (response.getAuthorizationAction().isStatus(EStatus.ALLOW)
				|| response.getAuthorizationAction().isStatus(EStatus.MODIFY))) ? true : false;
		if (EVENTTIMER)
			StatisticsUtil.stopEventTimer(eventName);
		if (!isActual && success) {
			createEvent(eventName, specificParams, true);

			IEvent modifiedEvent = response.getModifiedEvent();
			if (modifiedEvent != null) {
				Map<String, String> modifiedParams = response.getModifiedEvent().getParameters();
				Map<String, String> originalParams = event.getParameters();
				try {
					if (originalParams.containsKey("calleeObjectClass") && originalParams.containsKey("calleeMethod")
							&& originalParams.containsKey("calleeObjectAddress")
							&& originalParams.containsKey("methodArgValues")) {
						String calleeObjectClass = originalParams.get("calleeObjectClass");
						String calleeMethod = originalParams.get("calleeMethod");
						String calleeObjectAddress = originalParams.get("calleeObjectAddress");
						String methodArgValuesStr = originalParams.get("methodArgValues");
						if (calleeObjectClass.toLowerCase().equals("java.util.map")
								&& calleeMethod.toLowerCase().equals("get")) {
							JSONArray methodArgValuesJSON = (JSONArray) new JSONParser().parse(methodArgValuesStr);
							String[] methodArgValues = new String[methodArgValuesJSON.size()];
							methodArgValuesJSON.toArray(methodArgValues);
							String key = methodArgValues[0];
							Object oldObject = UnsafeUtil.objectFromAddress(Long.parseLong(calleeObjectAddress));
							if (oldObject instanceof Map) {
								Map<?, ?> map = (Map<?, ?>) oldObject;

								Object o = map.get(key);
								if (o != null && o instanceof ArrayList) {
									ArrayList<?> a = (ArrayList<?>) o;
									Iterator<?> ita = a.iterator();
									while (ita.hasNext()) {
										Object o2 = ita.next();
										Field[] fields = o2.getClass().getDeclaredFields();
										for (String k : modifiedParams.keySet()) {
											for (Field f : fields) {
												if (k.toLowerCase().equals(f.getName().toLowerCase())) {
													boolean accessible = f.isAccessible();
													f.setAccessible(true);
													if (f.getType().equals(Date.class)) {
														SimpleDateFormat sdt = new SimpleDateFormat("YYYY/MM/DD");
														java.util.Date datum = sdt.parse(modifiedParams.get(k));
														Date d = new Date(datum.getTime());
														f.set(o2, d);
													} else {
														f.set(o2, modifiedParams.get(k));
													}
													f.setAccessible(accessible);
												}
											}
										}
									}
								}
							}
						}
					}
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (java.text.ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else if (!success) {
			// try{
			// Map<String,String> param = event.getParameters();
			// if(param.containsKey("calleeObjectClass") &&
			// param.containsKey("calleeMethod") &&
			// param.containsKey("calleeObjectAddress") &&
			// param.containsKey("methodArgValues")){
			// String calleeObjectClass = param.get("calleeObjectClass");
			// String calleeMethod = param.get("calleeMethod");
			// String calleeObjectAddress = param.get("calleeObjectAddress");
			// String methodArgValuesStr = param.get("methodArgValues");
			// if(calleeObjectClass.toLowerCase().equals("java.util.map") &&
			// calleeMethod.toLowerCase().equals("get")){
			// JSONArray methodArgValuesJSON = (JSONArray) new
			// JSONParser().parse(methodArgValuesStr);
			// String[] methodArgValues = new
			// String[methodArgValuesJSON.size()];
			// methodArgValuesJSON.toArray(methodArgValues);
			// String key = methodArgValues[0];
			// System.out.println("\nJAVAPEP: removing key "+key+", from object
			// "+calleeObjectAddress+", "+calleeObjectClass);
			//
			// Object oldObject =
			// UnsafeUtil.objectFromAddress(Long.parseLong(calleeObjectAddress));
			// if(oldObject instanceof Map){
			// Map<?, ?> o = (Map<?, ?>)oldObject;
			// o.remove(key);
			// }
			// }
			// }
			// }catch(ParseException e){}
		}

		// System.out.println(success ? "Event sent successfully" : "Error
		// sending event");
	}

	/**
	 * Converts a given array of objects to a string array of their classes and
	 * addresses.
	 * 
	 * @param objects
	 *            The array of objects.
	 * @return A string array with classes and addresses of the provided
	 *         objects.
	 */
	private static String[] objectsToStrings(Object[] objects) {
		String[] strings = new String[objects.length];
		for (int i = 0; i < objects.length; i++) {
			strings[i] = objectToString(objects[i]);
		}
		return strings;
	}

	/**
	 * Retrieves the class and the address of the given object and returns a
	 * string containing both.
	 * 
	 * @param object
	 *            The given object.
	 * @return A string containing the class and the object separated by "|".
	 *         Returns null
	 */
	private static String objectToString(Object object) {
		if (object == null)
			return "null";
		return getClass(object) + "|" + getAddress(object);
	}

	/**
	 * Returns the string representation of the given object's address using
	 * <code>UnsafeUtil</code>.
	 * 
	 * @param object
	 *            The given object.
	 * @return A string containing the object's address. Returns null if the
	 *         given object is null or a boxed primitive.
	 */
	private static String getAddress(Object object) {
		if (object == null)
			return "null";
		if (object instanceof Double || object instanceof Float || object instanceof Long || object instanceof Integer
				|| object instanceof Character || object instanceof Byte || object instanceof Boolean
				|| object instanceof Short) {
			return "null";
		}
		return String.valueOf(UnsafeUtil.getObjectAddress(object));
	}

	/**
	 * Returns the class of the given object.
	 * 
	 * @param object
	 *            The given object.
	 * @return A string containing the class of the object. Returns null if the
	 *         given object is null.
	 */
	private static String getClass(Object object) {
		if (object == null)
			return "null";
		return object.getClass().getName();
	}

	/**
	 * Returns the classes of the objects in the provided array.
	 * 
	 * @param objects
	 *            The array of objects.
	 * @return A string array with class names.
	 */
	private static String[] getClasses(Object[] objects) {
		String[] objectClasses = new String[objects.length];
		for (int i = 0; i < objects.length; i++) {
			objectClasses[i] = getClass(objects[i]);
		}
		return objectClasses;
	}

	private static String[] getValues(Object[] objects) {
		String[] _return = new String[objects.length];
		for (int i = 0; i < objects.length; i++) {
			// String clazz = getClass(objects[i]);
			// if(clazz.toLowerCase().equals("java.lang.String")
			// || clazz.toLowerCase().equals("java.lang.Double")
			// || clazz.toLowerCase().equals("java.lang.Float")
			// || clazz.toLowerCase().equals("java.lang.Long")
			// || clazz.toLowerCase().equals("java.lang.Integer")
			// || clazz.toLowerCase().equals("java.lang.Character")
			// || clazz.toLowerCase().equals("java.lang.Byte")
			// || clazz.toLowerCase().equals("java.lang.Boolean")
			// || clazz.toLowerCase().equals("java.lang.Short"))
			_return[i] = String.valueOf(objects[i]);
		}
		return _return;
	}

	/**
	 * Returns the addresses of the objects in the provided array.
	 * 
	 * @param objects
	 *            The array of objects.
	 * @return A string array with addresses.
	 */
	private static String[] getAddresses(Object[] objects) {
		String[] objectAddresses = new String[objects.length];
		for (int i = 0; i < objects.length; i++) {
			objectAddresses[i] = getAddress(objects[i]);
		}
		return objectAddresses;
	}

	/**
	 * Returns the class name of the given object if it is not null. If the
	 * given object is null, this method returns the class from the given method
	 * fully qualified name.
	 * 
	 * @param object
	 *            The given object.
	 * @param methodFQN
	 *            The fully qualified name of a method.
	 * @return A class name.
	 */
	private static String getClass(Object object, String methodFQN) {
		if (object == null)
			return getClass(methodFQN);
		return getClass(object);
	}

	/**
	 * Returns the class name part from the given method fully qualified name.
	 * 
	 * @param methodFQN
	 *            The method fully qualified name.
	 * @return A class name.
	 */
	private static String getClass(String methodFQN) {
		return methodFQN.split("\\|")[0];
	}

	/**
	 * Returns the method name part from the given method fully qualified name.
	 * 
	 * @param methodFQN
	 *            The method fully qualified name.
	 * @return A method name.
	 */
	private static String getMethod(String methodFQN) {
		return methodFQN.split("\\|")[1];
	}

	/**
	 * Searches the list of sources from <code>StaticAnalysis</code> (parsed
	 * from JOANA report) for the ones where their location and bytecode offset
	 * matches the give method fully qualified name and bytecode offset.
	 * Matching sources are put into a dictionary with their IDs as values
	 * together with the current timestamp and the parameter number of the
	 * source as the key. This dictionary is returned.
	 * 
	 * @param parentMethodFQN
	 *            The fully qualified name of the method where the source should
	 *            be located.
	 * @param bytecodeOffset
	 *            The bytecode offset to filter the sources.
	 * @return A map of #param -> (sourceId, timeStamp)
	 */
	private static Map<String, Map<String, String>> getSourcesMap(String parentMethodFQN, int bytecodeOffset) {
		Map<String, Map<String, String>> sources = new HashMap<>();
		for (SinkSource source : StaticAnalysis.getSources()) {
			if (source.getLocation().equals(parentMethodFQN.replace("|", "."))
					&& source.getOffset() == bytecodeOffset) {
				Map<String, String> sourceIdAndDate = new HashMap<>();
				sourceIdAndDate.put("sourceId", source.getId());
				sourceIdAndDate.put("timeStamp", String.valueOf(System.currentTimeMillis()));
				if (source.isReturn()) {
					sources.put("ret", sourceIdAndDate);
				} else {
					if (source.getParam() == 0) {
						sources.put("obj", sourceIdAndDate);
					} else {
						sources.put("p" + source.getParam(), sourceIdAndDate);
					}
				}
			}
		}
		return sources;
	}
}
