package com.sun.tools.javac.v8.util;

public final class Set {
	private static class Entry {

		final Object key;
		final int hash;
		Entry next;

		Entry(final Object obj, final int i, final Entry entry) {
			this.key = obj;
			this.hash = i;
			this.next = entry;
		}
	}

	private Set(final int i, final float f) {
		int j;
		for (j = 1; j < i; j <<= 1) {
		}
		this.hashSize = j;
		this.hashMask = j - 1;
		this.limit = (int) (j * f);
		this.size = 0;
		this.table = new Entry[j];
	}

	private Set(final int initialCapacity) {
		this(initialCapacity, 0.75F);
	}

	private Set() {
		this(32);
	}

	public static Set make() {
		return new Set();
	}

	private void dble() {
		this.hashSize <<= 1;
		this.hashMask = this.hashSize - 1;
		this.limit <<= 1;
		final Entry aentry[] = this.table;
		this.table = new Entry[this.hashSize];
		for (int i = 0; i < aentry.length; i++) {
			this.copy(aentry[i]);
		}

	}

	private void copy(final Entry entry) {
		if (entry != null) {
			this.copy(entry.next);
			entry.next = this.table[entry.hash & this.hashMask];
			this.table[entry.hash & this.hashMask] = entry;
		}
	}

	public boolean contains(final Object obj) {
		final int i = obj.hashCode();
		for (Entry entry = this.table[i & this.hashMask]; entry != null; entry = entry.next) {
			if (entry.hash == i && entry.key.equals(obj)) {
				return true;
			}
		}

		return false;
	}

	public void add(final Object o) {
		final int i = o.hashCode();
		for (Entry entry = this.table[i & this.hashMask]; entry != null; entry = entry.next) {
			if (entry.hash == i && entry.key.equals(o)) {
				return;
			}
		}

		this.size++;
		if (this.size > this.limit) {
			this.dble();
		}
		final int j = i & this.hashMask;
		final Entry entry1 = new Entry(o, i, this.table[j]);
		this.table[j] = entry1;
	}

	public void clear() {
		for (int i = 0; i < this.table.length; i++) {
			this.table[i] = null;
		}

		this.size = 0;
	}

	private int hashSize;
	private int hashMask;
	private int limit;
	private int size;
	private Entry table[];
}
