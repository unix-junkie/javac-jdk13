package sun.tools.asm;

import sun.tools.java.Type;

public final class ArrayData {

	public ArrayData(final Type type1, final int i) {
		this.type = type1;
		this.nargs = i;
	}

	final Type type;
	final int nargs;
}
