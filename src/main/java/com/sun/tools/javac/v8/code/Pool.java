package com.sun.tools.javac.v8.code;

import com.sun.tools.javac.v8.util.Hashtable;

public final class Pool {
	Pool(final int i, final Object aobj[]) {
		this.pp = i;
		this.pool = aobj;
		this.indices = new Hashtable(aobj.length);
		for (int j = 1; j < i; j++) {
			if (aobj[j] != null) {
				this.indices.put(aobj[j], new Integer(j));
			}
		}

	}

	public Pool() {
		this(1, new Object[64]);
	}

	public int numEntries() {
		return this.pp;
	}

	public void reset() {
		this.pp = 1;
		this.indices.clear();
	}

	private void doublePool() {
		final Object aobj[] = new Object[this.pool.length * 2];
		System.arraycopy(this.pool, 0, aobj, 0, this.pool.length);
		this.pool = aobj;
	}

	public int put(final Object obj) {
		Integer integer = (Integer) this.indices.get(obj);
		if (integer == null) {
			integer = new Integer(this.pp);
			this.indices.put(obj, integer);
			if (this.pp == this.pool.length) {
				this.doublePool();
			}
			this.pool[this.pp++] = obj;
			if (obj instanceof Long || obj instanceof Double) {
				if (this.pp == this.pool.length) {
					this.doublePool();
				}
				this.pool[this.pp++] = null;
			}
		}
		return integer.intValue();
	}

	public int get(final Object obj) {
		final Number integer = (Number) this.indices.get(obj);
		return integer != null ? integer.intValue() : -1;
	}

	public static final int MAX_ENTRIES = 65535;
	public static final int MAX_STRING_LENGTH = 65535;
	int pp;
	Object pool[];
	private final Hashtable indices;
}
