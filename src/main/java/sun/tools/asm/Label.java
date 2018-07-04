package sun.tools.asm;

import sun.tools.java.MemberDefinition;

public final class Label extends Instruction {

	public Label() {
		super(0L, -1, null);
		this.ID = ++labelCount;
	}

	Label getDestination() {
		Label label = this;
		if (this.next != null && this.next != this && this.depth == 0) {
			this.depth = 1;
			switch (this.next.opc) {
			default:
				break;

			case -1:
				label = ((Label) this.next).getDestination();
				break;

			case 167:
				label = ((Label) this.next.value).getDestination();
				break;

			case 18: // '\022'
			case 19: // '\023'
				if (!(this.next.value instanceof Integer)) {
					break;
				}
				Instruction instruction = this.next.next;
				if (instruction.opc == -1) {
					instruction = ((Instruction) ((Label) instruction).getDestination()).next;
				}
				if (instruction.opc == 153) {
					if (((Number) this.next.value).intValue() == 0) {
						label = (Label) instruction.value;
					} else {
						label = new Label();
						label.next = instruction.next;
						instruction.next = label;
					}
					label = label.getDestination();
					break;
				}
				if (instruction.opc != 154) {
					break;
				}
				if (((Number) this.next.value).intValue() == 0) {
					label = new Label();
					label.next = instruction.next;
					instruction.next = label;
				} else {
					label = (Label) instruction.value;
				}
				label = label.getDestination();
				break;
			}
			this.depth = 0;
		}
		return label;
	}

	public String toString() {
		final String s = "$" + this.ID + ':';
		if (this.value != null) {
			return s + " stack=" + this.value;
		}
		return s;
	}

	private static int labelCount;
	private final int ID;
	int depth;
	MemberDefinition locals[];

}
