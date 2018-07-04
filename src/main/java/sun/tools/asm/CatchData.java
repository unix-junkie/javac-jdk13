package sun.tools.asm;

public final class CatchData {

	CatchData(final Object obj) {
		this.type = obj;
		this.label = new Label();
	}

	public Label getLabel() {
		return this.label;
	}

	public Object getType() {
		return this.type;
	}

	private final Object type;
	private final Label label;
}
