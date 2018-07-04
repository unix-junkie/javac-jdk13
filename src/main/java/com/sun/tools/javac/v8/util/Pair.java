package com.sun.tools.javac.v8.util;

public final class Pair {

	public Pair(final Object obj, final Object obj1) {
		this.fst = obj;
		this.snd = obj1;
	}

	private static boolean equals(final Object obj, final Object obj1) {
		return obj == null && obj1 == null || obj != null && obj.equals(obj1);
	}

	public boolean equals(final Object obj) {
		return obj instanceof Pair && equals(this.fst, ((Pair) obj).fst) && equals(this.snd, ((Pair) obj).snd);
	}

	public int hashCode() {
		if (this.fst == null) {
			return this.snd.hashCode() + 1;
		}
		return this.snd == null ? this.fst.hashCode() + 2 : this.fst.hashCode() * this.snd.hashCode();
	}

	public final Object fst;
	public final Object snd;
}
