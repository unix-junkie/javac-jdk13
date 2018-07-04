package com.sun.tools.javac.v8.util;

public final class List {

	public List(final Object obj, final List list) {
		this.tail = list;
		this.head = obj;
	}

	public List() {
		this(null, null);
	}

	public static List make(final Object obj) {
		return new List(obj, new List());
	}

	public static List make(final Object obj, final Object obj1) {
		return new List(obj, new List(obj1, new List()));
	}

	public static List make(final Object obj, final Object obj1, final Object obj2) {
		return new List(obj, new List(obj1, new List(obj2, new List())));
	}

	public static List make(final Object aobj[]) {
		List list = new List();
		for (int i = aobj.length - 1; i >= 0; i--) {
			list = new List(aobj[i], list);
		}

		return list;
	}

	public static List make(final int i, final Object obj) {
		List list = new List();
		for (int j = 0; j < i; j++) {
			list = new List(obj, list);
		}

		return list;
	}

	public boolean isEmpty() {
		return this.tail == null;
	}

	public boolean nonEmpty() {
		return this.tail != null;
	}

	public int length() {
		List list = this;
		int i;
		for (i = 0; list.tail != null; i++) {
			list = list.tail;
		}

		return i;
	}

	public List prepend(final Object obj) {
		return new List(obj, this);
	}

	public List prepend(final List list) {
		if (this.isEmpty()) {
			return list;
		}
		return list.isEmpty() ? this : this.prepend(list.tail).prepend(list.head);
	}

	public List reverse() {
		List list = new List();
		for (List list1 = this; list1.nonEmpty(); list1 = list1.tail) {
			list = new List(list1.head, list);
		}

		return list;
	}

	public List append(final Object obj) {
		return make(obj).prepend(this);
	}

	public Object[] toArray(final Object aobj[]) {
		int i = 0;
		for (List list = this; list.nonEmpty() && i < aobj.length; i++) {
			aobj[i] = list.head;
			list = list.tail;
		}

		return aobj;
	}

	private String toString(final String s) {
		if (this.isEmpty()) {
			return "";
		}
		final StringBuffer stringbuffer = new StringBuffer();
		stringbuffer.append(this.head);
		for (List list = this.tail; list.nonEmpty(); list = list.tail) {
			stringbuffer.append(s);
			stringbuffer.append(list.head);
		}

		return stringbuffer.toString();
	}

	public String toString() {
		return this.toString(",");
	}

	public int hashCode() {
		List list = this;
		int i = 0;
		for (; list.tail != null; list = list.tail) {
			i = i * 41 + (this.head == null ? 0 : this.head.hashCode());
		}

		return i;
	}

	public boolean equals(final Object obj) {
		return obj instanceof List && equals(this, (List) obj);
	}

	private static boolean equals(List list, List list1) {
		for (; list.tail != null && list1.tail != null; list1 = list1.tail) {
			if (list.head == null) {
				if (list1.head != null) {
					return false;
				}
			} else if (!list.head.equals(list1.head)) {
				return false;
			}
			list = list.tail;
		}

		return list.tail == null && list1.tail == null;
	}

	public boolean contains(final Object obj) {
		for (List list = this; list.tail != null; list = list.tail) {
			if (obj == null) {
				if (list.head == null) {
					return true;
				}
			} else if (obj.equals(list.head)) {
				return true;
			}
		}

		return false;
	}

	public Object head;
	public List tail;
}
