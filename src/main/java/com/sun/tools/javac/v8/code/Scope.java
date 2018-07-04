package com.sun.tools.javac.v8.code;

import com.sun.tools.javac.v8.util.Name;

public final class Scope {
	public static class Entry {

		public Entry next() {
			Entry entry;
			for (entry = this.shadowed; entry.scope != null && entry.sym.name != this.sym.name; entry = entry.shadowed) {
			}
			return entry;
		}

		public final Symbol sym;
		Entry shadowed;
		public final Entry sibling;
		public final Scope scope;

		Entry(final Symbol symbol, final Entry entry, final Entry entry1, final Scope scope1) {
			this.sym = symbol;
			this.shadowed = entry;
			this.sibling = entry1;
			this.scope = scope1;
		}
	}

	private Scope(final Scope scope, final Symbol symbol, final Entry aentry[]) {
		this.nelems = 0;
		this.next = scope;
		this.owner = symbol;
		this.table = aentry;
		this.hashMask = aentry.length - 1;
		this.elems = null;
		this.nelems = 0;
	}

	public Scope(final Symbol symbol) {
		this(null, symbol, new Entry[128]);
		for (int i = 0; i < 128; i++) {
			this.table[i] = sentinel;
		}

	}

	public Scope dup() {
		return new Scope(this, this.owner, this.table);
	}

	public Scope dupUnshared() {
		return new Scope(this, this.owner, (Entry[]) this.table.clone());
	}

	public Scope leave() {
		for (; this.elems != null; this.elems = this.elems.sibling) {
			final int i = this.elems.sym.name.index & this.hashMask;
			final Entry entry = this.table[i];
			if (entry == this.elems) {
				this.table[i] = this.elems.shadowed;
			} else {
				throw new InternalError(this.elems.sym.toString());
			}
		}

		return this.next;
	}

	private void dble() {
		final Entry aentry[] = this.table;
		final Entry aentry1[] = new Entry[aentry.length * 2];
		Scope scope = this;
		do {
			scope.table = aentry1;
			scope.hashMask = aentry1.length - 1;
			scope = scope.next;
		} while (scope != null);
		for (int i = 0; i < aentry1.length; i++) {
			aentry1[i] = sentinel;
		}

		for (int j = 0; j < aentry.length; j++) {
			this.copy(aentry[j]);
		}

	}

	private void copy(final Entry entry) {
		if (entry.sym != null) {
			this.copy(entry.shadowed);
			final int i = entry.sym.name.index & this.hashMask;
			entry.shadowed = this.table[i];
			this.table[i] = entry;
		}
	}

	public void enter(final Symbol symbol) {
		this.enter(symbol, this);
	}

	public void enter(final Symbol symbol, final Scope scope) {
		if (this.nelems * 3 >= this.hashMask * 2) {
			this.dble();
		}
		final int i = symbol.name.index & this.hashMask;
		final Entry entry = new Entry(symbol, this.table[i], this.elems, scope);
		this.table[i] = entry;
		this.elems = entry;
		this.nelems++;
	}

	public void enterIfAbsent(final Symbol symbol) {
		Entry entry;
		for (entry = this.lookup(symbol.name); entry.scope == this && entry.sym.kind != symbol.kind; entry = entry.next()) {
		}
		if (entry.scope != this) {
			this.enter(symbol);
		}
	}

	public Entry lookup(final Name name) {
		Entry entry;
		for (entry = this.table[name.index & this.hashMask]; entry.scope != null && entry.sym.name != name; entry = entry.shadowed) {
		}
		return entry.scope == null && this == errScope ? new Entry(Symbol.errSymbol, null, null, null) : entry;
	}

	public final Scope next;
	public Symbol owner;
	private Entry[] table;
	private int hashMask;
	public Entry elems;
	private int nelems;
	private static final Entry sentinel = new Entry(null, null, null, null);
	static final Scope errScope = new Scope(null);

}
