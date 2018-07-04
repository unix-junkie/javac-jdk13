package com.sun.tools.javac.v8.tree;

import com.sun.tools.javac.v8.tree.Tree.Apply;
import com.sun.tools.javac.v8.tree.Tree.Assign;
import com.sun.tools.javac.v8.tree.Tree.Assignop;
import com.sun.tools.javac.v8.tree.Tree.Block;
import com.sun.tools.javac.v8.tree.Tree.Break;
import com.sun.tools.javac.v8.tree.Tree.Case;
import com.sun.tools.javac.v8.tree.Tree.Catch;
import com.sun.tools.javac.v8.tree.Tree.ClassDef;
import com.sun.tools.javac.v8.tree.Tree.Conditional;
import com.sun.tools.javac.v8.tree.Tree.Continue;
import com.sun.tools.javac.v8.tree.Tree.DoLoop;
import com.sun.tools.javac.v8.tree.Tree.Erroneous;
import com.sun.tools.javac.v8.tree.Tree.Exec;
import com.sun.tools.javac.v8.tree.Tree.ForLoop;
import com.sun.tools.javac.v8.tree.Tree.Ident;
import com.sun.tools.javac.v8.tree.Tree.Import;
import com.sun.tools.javac.v8.tree.Tree.Indexed;
import com.sun.tools.javac.v8.tree.Tree.Labelled;
import com.sun.tools.javac.v8.tree.Tree.Literal;
import com.sun.tools.javac.v8.tree.Tree.MethodDef;
import com.sun.tools.javac.v8.tree.Tree.NewArray;
import com.sun.tools.javac.v8.tree.Tree.NewClass;
import com.sun.tools.javac.v8.tree.Tree.Operation;
import com.sun.tools.javac.v8.tree.Tree.Return;
import com.sun.tools.javac.v8.tree.Tree.Select;
import com.sun.tools.javac.v8.tree.Tree.Switch;
import com.sun.tools.javac.v8.tree.Tree.Synchronized;
import com.sun.tools.javac.v8.tree.Tree.Throw;
import com.sun.tools.javac.v8.tree.Tree.TopLevel;
import com.sun.tools.javac.v8.tree.Tree.Try;
import com.sun.tools.javac.v8.tree.Tree.TypeArray;
import com.sun.tools.javac.v8.tree.Tree.TypeCast;
import com.sun.tools.javac.v8.tree.Tree.TypeIdent;
import com.sun.tools.javac.v8.tree.Tree.TypeTest;
import com.sun.tools.javac.v8.tree.Tree.VarDef;
import com.sun.tools.javac.v8.tree.Tree.Visitor;
import com.sun.tools.javac.v8.tree.Tree.WhileLoop;
import com.sun.tools.javac.v8.util.List;

public class TreeTranslator extends Visitor {

	public Tree translate(final Tree tree) {
		if (tree == null) {
			return null;
		}
		tree.visit(this);
		return this.result;
	}

	public List translate(final List list) {
		if (list == null) {
			return null;
		}
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			list1.head = this.translate((Tree) list1.head);
		}

		return list;
	}

	public List translateVarDefs(final List list) {
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			list1.head = this.translate((Tree) list1.head);
		}

		return list;
	}

	public List translateCases(final List list) {
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			list1.head = this.translate((Tree) list1.head);
		}

		return list;
	}

	private List translateCatchers(final List list) {
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			list1.head = this.translate((Tree) list1.head);
		}

		return list;
	}

	public void _case(final TopLevel toplevel) {
		toplevel.pid = this.translate(toplevel.pid);
		toplevel.defs = this.translate(toplevel.defs);
		this.result = toplevel;
	}

	public void _case(final Import import1) {
		import1.qualid = this.translate(import1.qualid);
		this.result = import1;
	}

	public void _case(final ClassDef classdef) {
		classdef.extending = this.translate(classdef.extending);
		classdef.implementing = this.translate(classdef.implementing);
		classdef.defs = this.translate(classdef.defs);
		this.result = classdef;
	}

	public void _case(final MethodDef methoddef) {
		methoddef.restype = this.translate(methoddef.restype);
		methoddef.params = this.translateVarDefs(methoddef.params);
		methoddef.thrown = this.translate(methoddef.thrown);
		methoddef.body = (Block) this.translate(methoddef.body);
		this.result = methoddef;
	}

	public void _case(final VarDef vardef) {
		vardef.vartype = this.translate(vardef.vartype);
		vardef.init = this.translate(vardef.init);
		this.result = vardef;
	}

	public void _case(final Block block) {
		block.stats = this.translate(block.stats);
		this.result = block;
	}

	public void _case(final DoLoop doloop) {
		doloop.body = this.translate(doloop.body);
		doloop.cond = this.translate(doloop.cond);
		this.result = doloop;
	}

	public void _case(final WhileLoop whileloop) {
		whileloop.cond = this.translate(whileloop.cond);
		whileloop.body = this.translate(whileloop.body);
		this.result = whileloop;
	}

	public void _case(final ForLoop forloop) {
		forloop.init = this.translate(forloop.init);
		forloop.cond = this.translate(forloop.cond);
		forloop.step = this.translate(forloop.step);
		forloop.body = this.translate(forloop.body);
		this.result = forloop;
	}

	public void _case(final Labelled labelled) {
		labelled.body = this.translate(labelled.body);
		this.result = labelled;
	}

	public void _case(final Switch switch1) {
		switch1.selector = this.translate(switch1.selector);
		switch1.cases = this.translateCases(switch1.cases);
		this.result = switch1;
	}

	public void _case(final Case case1) {
		case1.pat = this.translate(case1.pat);
		case1.stats = this.translate(case1.stats);
		this.result = case1;
	}

	public void _case(final Synchronized synchronized1) {
		synchronized1.lock = this.translate(synchronized1.lock);
		synchronized1.body = this.translate(synchronized1.body);
		this.result = synchronized1;
	}

	public void _case(final Try try1) {
		try1.body = this.translate(try1.body);
		try1.catchers = this.translateCatchers(try1.catchers);
		try1.finalizer = this.translate(try1.finalizer);
		this.result = try1;
	}

	public void _case(final Catch catch1) {
		catch1.param = (VarDef) this.translate(catch1.param);
		catch1.body = this.translate(catch1.body);
		this.result = catch1;
	}

	public void _case(final Conditional conditional) {
		conditional.cond = this.translate(conditional.cond);
		conditional.thenpart = this.translate(conditional.thenpart);
		conditional.elsepart = this.translate(conditional.elsepart);
		this.result = conditional;
	}

	public void _case(final Exec exec) {
		exec.expr = this.translate(exec.expr);
		this.result = exec;
	}

	public void _case(final Break break1) {
		this.result = break1;
	}

	public void _case(final Continue continue1) {
		this.result = continue1;
	}

	public void _case(final Return return1) {
		return1.expr = this.translate(return1.expr);
		this.result = return1;
	}

	public void _case(final Throw throw1) {
		throw1.expr = this.translate(throw1.expr);
		this.result = throw1;
	}

	public void _case(final Apply apply) {
		apply.meth = this.translate(apply.meth);
		apply.args = this.translate(apply.args);
		this.result = apply;
	}

	public void _case(final NewClass newclass) {
		newclass.encl = this.translate(newclass.encl);
		newclass.clazz = this.translate(newclass.clazz);
		newclass.args = this.translate(newclass.args);
		newclass.def = (ClassDef) this.translate(newclass.def);
		this.result = newclass;
	}

	public void _case(final NewArray newarray) {
		newarray.elemtype = this.translate(newarray.elemtype);
		newarray.dims = this.translate(newarray.dims);
		newarray.elems = this.translate(newarray.elems);
		this.result = newarray;
	}

	public void _case(final Assign assign) {
		assign.lhs = this.translate(assign.lhs);
		assign.rhs = this.translate(assign.rhs);
		this.result = assign;
	}

	public void _case(final Assignop assignop) {
		assignop.lhs = this.translate(assignop.lhs);
		assignop.rhs = this.translate(assignop.rhs);
		this.result = assignop;
	}

	public void _case(final Operation operation) {
		operation.args = this.translate(operation.args);
		this.result = operation;
	}

	public void _case(final TypeCast typecast) {
		typecast.clazz = this.translate(typecast.clazz);
		typecast.expr = this.translate(typecast.expr);
		this.result = typecast;
	}

	public void _case(final TypeTest typetest) {
		typetest.expr = this.translate(typetest.expr);
		typetest.clazz = this.translate(typetest.clazz);
		this.result = typetest;
	}

	public void _case(final Indexed indexed) {
		indexed.indexed = this.translate(indexed.indexed);
		indexed.index = this.translate(indexed.index);
		this.result = indexed;
	}

	public void _case(final Select select) {
		select.selected = this.translate(select.selected);
		this.result = select;
	}

	public void _case(final Ident ident) {
		this.result = ident;
	}

	public void _case(final Literal literal) {
		this.result = literal;
	}

	public void _case(final TypeIdent typeident) {
		this.result = typeident;
	}

	public void _case(final TypeArray typearray) {
		typearray.elemtype = this.translate(typearray.elemtype);
		this.result = typearray;
	}

	public void _case(final Erroneous erroneous) {
		this.result = erroneous;
	}

	public void _case(final Tree tree) {
		throw new InternalError();
	}

	public Tree result;
}
