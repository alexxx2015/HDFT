package edu.tum.uc.jvm.declassification.qif;

import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import edu.tum.uc.jvm.utility.analysis.Flow.Chop;

public abstract class AbstractMethodVisitor extends MethodVisitor{
	
	private List<Chop> chopNodes;

	AbstractMethodVisitor(int arg0, MethodVisitor mv, List<Chop> chopNodes) {
		super(arg0, mv);
		this.chopNodes = chopNodes;
	}

	/**
	 * Checks if chopnode exisits at the current location specified by the given
	 * label containing a bytecode offset and returns it.
	 * 
	 * @param label
	 *            The label with the bytecode index.
	 * @return A chopnode if one is found, otherwise null
	 */
	protected Chop checkChopNode(Label label) {
		// iterate through chopnode list and compare bytecode offsets
		if ((this.chopNodes != null) && (this.chopNodes.size() > 0)) {
			Iterator<Chop> it = this.chopNodes.iterator();
			while (it.hasNext()) {
				Chop c = it.next();
				int offset = label.getOffset();
				int byteCodeIndex = c.getByteCodeIndex();
				if ((byteCodeIndex != 0) && (offset == byteCodeIndex)) {
					return c;
				}
			}
		}
		return null;
	}

}
