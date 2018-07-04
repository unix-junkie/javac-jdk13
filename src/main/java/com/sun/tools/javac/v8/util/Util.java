package com.sun.tools.javac.v8.util;

public final class Util {
	private Util() {
	}

	public static void assertTrue(final boolean flag) {
		if (!flag) {
			throw new InternalError("assertion failed");
		}
	}

	public static void assertTrue(final boolean flag, final Object message) {
		if (!flag) {
			throw new InternalError("assertion failed: " + message);
		}
	}

	public static boolean contains(final String s, final String s1, final char c) {
		int i = 0;
		int j;
		for (j = pos(s, c, i); j >= i && !s.substring(i, j).equals(s1); j = pos(s, c, i)) {
			i = j + 1;
		}

		return j >= i;
	}

	public static int pos(final String s, final char c, final int i) {
		final int j = s.indexOf(c, i);
		return j >= 0 ? j : s.length();
	}
}
