package com.sun.tools.javac.v8.util;

import java.util.Enumeration;

public final class ListBuffer {
	static class Enumerator implements Enumeration {

		public boolean hasMoreElements() {
			return this.elems != this.last;
		}

		public Object nextElement() {
			final Object obj = this.elems.head;
			this.elems = this.elems.tail;
			return obj;
		}

		List elems;
		final List last;

		Enumerator(final List list, final List list1) {
			this.elems = list;
			this.last = list1;
		}
	}

	public ListBuffer() {
		this.elems = new List();
		this.last = this.elems;
		this.count = 0;
		this.shared = false;
	}

	public int length() {
		return this.count;
	}

	public boolean isEmpty() {
		return this.count == 0;
	}

	public boolean nonEmpty() {
		return this.count != 0;
	}

	private void copy() {
		List list = this.elems;
		this.elems = new List();
		this.last = this.elems;
		for (; list.nonEmpty(); list = list.tail) {
			this.last.head = list.head;
			this.last.tail = new List();
			this.last = this.last.tail;
		}

		this.shared = false;
	}

	public void append(final Object obj) {
		if (this.shared) {
			this.copy();
		}
		this.last.head = obj;
		this.last.tail = new List();
		this.last = this.last.tail;
		this.count++;
	}

	public void appendList(List list) {
		for (; list.nonEmpty(); list = list.tail) {
			this.append(list.head);
		}

	}

	public List toList() {
		this.shared = true;
		return this.elems;
	}

	public Object first() {
		return this.elems.head;
	}

	public void remove() {
		if (this.elems != this.last) {
			this.elems = this.elems.tail;
			this.count--;
		}
	}

	public Enumeration elements() {
		return new Enumerator(this.elems, this.last);
	}

	public List elems;
	public List last;
	private int count;
	private boolean shared;
}
