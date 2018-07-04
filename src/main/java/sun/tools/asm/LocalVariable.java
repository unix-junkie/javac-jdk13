package sun.tools.asm;

import sun.tools.java.MemberDefinition;

public final class LocalVariable {

	public LocalVariable(final MemberDefinition memberdefinition, final int i) {
		if (memberdefinition == null) {
			new Exception().printStackTrace();
		}
		this.field = memberdefinition;
		this.slot = i;
		this.to = -1;
	}

	LocalVariable(final MemberDefinition memberdefinition, final int i, final int j, final int k) {
		this.field = memberdefinition;
		this.slot = i;
		this.from = j;
		this.to = k;
	}

	public String toString() {
		return this.field + "/" + this.slot;
	}

	final MemberDefinition field;
	final int slot;
	int from;
	int to;
}
