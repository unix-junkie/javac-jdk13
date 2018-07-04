package sun.tools.asm;

import java.util.Vector;

public final class TryData {

	public TryData() {
		this.catches = new Vector();
		this.endLabel = new Label();
	}

	public void add(final Object obj) {
		final CatchData catchdata = new CatchData(obj);
		this.catches.addElement(catchdata);
	}

	public CatchData getCatch(final int i) {
		return (CatchData) this.catches.elementAt(i);
	}

	public Instruction getEndLabel() {
		return this.endLabel;
	}

	final Vector catches;
	private final Label endLabel;
}
