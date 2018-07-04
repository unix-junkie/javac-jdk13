package com.sun.tools.javac.v8.util;

public final class Hashtable {
	private static class Entry {

		final Object key;
		Object value;
		final int hash;
		Entry next;

		Entry(final Object obj, final Object obj1, final int i, final Entry entry) {
			this.key = obj;
			this.value = obj1;
			this.hash = i;
			this.next = entry;
		}
	}

	private Hashtable(final int initialCapacity, final float loadFactor) {
		int hashSize;
		for (hashSize = 1; hashSize < initialCapacity; hashSize <<= 1) {
		}
		this.hashSize = hashSize;
		this.hashMask = hashSize - 1;
		this.limit = (int) (hashSize * loadFactor);
		this.size = 0;
		this.table = new Entry[hashSize];
	}

	public Hashtable(final int initialCapacity) {
		this(initialCapacity, 0.75F);
	}

	public Hashtable() {
		this(32);
	}

	public static Hashtable make() {
		return new Hashtable();
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

	public Object get(final Object key) {
		final int hash = key.hashCode();
		for (Entry entry = this.table[hash & this.hashMask]; entry != null; entry = entry.next) {
			if (entry.hash == hash && entry.key.equals(key)) {
				return entry.value;
			}
		}

		return null;
	}

	public void put(final Object key, final Object value) {
		final int i = key.hashCode();
		for (Entry entry = this.table[i & this.hashMask]; entry != null; entry = entry.next) {
			if (entry.hash == i && entry.key.equals(key)) {
				entry.value = value;
				return;
			}
		}

		this.size++;
		if (this.size > this.limit) {
			this.dble();
		}
		final int j = i & this.hashMask;
		final Entry entry1 = new Entry(key, value, i, this.table[j]);
		this.table[j] = entry1;
	}

	public Object remove(final Object key) {
		final int hash = key.hashCode();
		Entry entry = null;
		for (Entry entry1 = this.table[hash & this.hashMask]; entry1 != null; entry1 = entry1.next) {
			if (entry1.hash == hash && entry1.key.equals(key)) {
				if (entry != null) {
					entry.next = entry1.next;
				} else {
					this.table[hash & this.hashMask] = entry1.next;
				}
				this.size--;
				return entry1.value;
			}
			entry = entry1;
		}

		return null;
	}

	public List keySet() {
		final ListBuffer listbuffer = new ListBuffer();
		for (int i = 0; i < this.table.length; i++) {
			for (Entry entry = this.table[i]; entry != null; entry = entry.next) {
				listbuffer.append(entry.key);
			}

		}

		return listbuffer.toList();
	}

	public int size() {
		return this.size;
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
