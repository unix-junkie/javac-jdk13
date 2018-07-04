package com.sun.tools.javac.v8.util;

public final class Position {

	private Position() {
	}

	public static int line(final int i) {
		return i >>> 10;
	}

	public static int column(final int i) {
		return i & 0x3ff;
	}

	public static int make(final int i, final int j) {
		return (i << 10) + j;
	}

	public static final int LINESHIFT = 10;
	public static final int COLUMNMASK = 1023;
	public static final int NOPOS = 0;
	public static final int FIRSTPOS = 1025;
	public static final int MAXPOS = 0x7fffffff;
}
