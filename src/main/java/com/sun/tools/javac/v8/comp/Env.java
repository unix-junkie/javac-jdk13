package com.sun.tools.javac.v8.comp;

import com.sun.tools.javac.v8.tree.Tree;
import com.sun.tools.javac.v8.tree.Tree.ClassDef;
import com.sun.tools.javac.v8.tree.Tree.MethodDef;
import com.sun.tools.javac.v8.tree.Tree.TopLevel;

public final class Env {

	Env(final Tree tree1, final Object obj) {
		this.next = null;
		this.outer = null;
		this.tree = tree1;
		this.toplevel = null;
		this.enclClass = null;
		this.enclMethod = null;
		this.info = obj;
	}

	public Env dup(final Tree tree1, final Object obj) {
		final Env env = new Env(tree1, obj);
		env.next = this;
		env.outer = this.outer;
		env.toplevel = this.toplevel;
		env.enclClass = this.enclClass;
		env.enclMethod = this.enclMethod;
		return env;
	}

	public Env dup(final Tree tree1) {
		return this.dup(tree1, this.info);
	}

	Env enclosing(final int i) {
		Env env;
		for (env = this; env != null && env.tree.tag != i; env = env.next) {
		}
		return env;
	}

	public Env next;
	public Env outer;
	public Tree tree;
	public TopLevel toplevel;
	public ClassDef enclClass;
	MethodDef enclMethod;
	final Object info;
}
