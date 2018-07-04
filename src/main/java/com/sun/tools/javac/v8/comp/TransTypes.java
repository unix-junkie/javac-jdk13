package com.sun.tools.javac.v8.comp;

import com.sun.tools.javac.v8.code.Scope.Entry;
import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.code.Type;
import com.sun.tools.javac.v8.tree.Tree;
import com.sun.tools.javac.v8.tree.Tree.Apply;
import com.sun.tools.javac.v8.tree.Tree.Assign;
import com.sun.tools.javac.v8.tree.Tree.Assignop;
import com.sun.tools.javac.v8.tree.Tree.Block;
import com.sun.tools.javac.v8.tree.Tree.Case;
import com.sun.tools.javac.v8.tree.Tree.ClassDef;
import com.sun.tools.javac.v8.tree.Tree.Conditional;
import com.sun.tools.javac.v8.tree.Tree.DoLoop;
import com.sun.tools.javac.v8.tree.Tree.Exec;
import com.sun.tools.javac.v8.tree.Tree.ForLoop;
import com.sun.tools.javac.v8.tree.Tree.Ident;
import com.sun.tools.javac.v8.tree.Tree.Indexed;
import com.sun.tools.javac.v8.tree.Tree.MethodDef;
import com.sun.tools.javac.v8.tree.Tree.NewArray;
import com.sun.tools.javac.v8.tree.Tree.NewClass;
import com.sun.tools.javac.v8.tree.Tree.Operation;
import com.sun.tools.javac.v8.tree.Tree.Select;
import com.sun.tools.javac.v8.tree.Tree.Switch;
import com.sun.tools.javac.v8.tree.Tree.Synchronized;
import com.sun.tools.javac.v8.tree.Tree.Throw;
import com.sun.tools.javac.v8.tree.Tree.TypeArray;
import com.sun.tools.javac.v8.tree.Tree.TypeCast;
import com.sun.tools.javac.v8.tree.Tree.TypeParameter;
import com.sun.tools.javac.v8.tree.Tree.TypeTest;
import com.sun.tools.javac.v8.tree.Tree.VarDef;
import com.sun.tools.javac.v8.tree.Tree.WhileLoop;
import com.sun.tools.javac.v8.tree.TreeInfo;
import com.sun.tools.javac.v8.tree.TreeMaker;
import com.sun.tools.javac.v8.tree.TreeTranslator;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.Log;

public final class TransTypes extends TreeTranslator {

	public TransTypes(final Log log1, final TreeMaker treemaker) {
		this.log = log1;
		this.make = treemaker;
	}

	private Tree cast(Tree tree, final Type type) {
		final int i = this.make.pos;
		this.make.at(tree.pos);
		if (!tree.type.isSameType(type)) {
			tree = this.make.TypeCast(this.make.Type(type), tree).setType(type);
		}
		this.make.pos = i;
		return tree;
	}

	private Tree coerce(final Tree tree, final Type type) {
		final Type type1 = type.baseType();
		return tree.type.isAssignable(type1) ? tree : this.cast(tree, type1);
	}

	private Tree retype(final Tree tree, final Type type, final Type type1) {
		if (type.tag > 8) {
			tree.type = type;
			if (type1 != null) {
				return this.coerce(tree, type1);
			}
		}
		return tree;
	}

	private List translateArgs(final List list, List list1) {
		for (List list2 = list; list2.nonEmpty(); list2 = list2.tail) {
			list2.head = this.translate((Tree) list2.head, (Type) list1.head);
			list1 = list1.tail;
		}

		return list;
	}

	private Tree translate(final Tree tree, final Type type) {
		final Type type1 = this.pt;
		this.pt = type;
		if (tree == null) {
			this.result = null;
		} else {
			tree.visit(this);
		}
		this.pt = type1;
		return this.result;
	}

	private List translate(final List list, final Type type) {
		final Type type1 = this.pt;
		this.pt = type;
		final List list1 = this.translate(list);
		this.pt = type1;
		return list1;
	}

	public void _case(final ClassDef classdef) {
		classdef.typarams = TypeParameter.emptyList;
		super._case(classdef);
		this.make.at(((Tree) classdef).pos);
		classdef.type = ((Tree) classdef).type.erasure();
		this.result = classdef;
	}

	public void _case(final MethodDef methoddef) {
		methoddef.restype = this.translate(methoddef.restype, null);
		methoddef.typarams = TypeParameter.emptyList;
		methoddef.params = this.translateVarDefs(methoddef.params);
		methoddef.thrown = this.translate(methoddef.thrown, null);
		methoddef.body = (Block) this.translate(methoddef.body, methoddef.sym.erasure().restype());
		methoddef.type = ((Tree) methoddef).type.erasure();
		this.result = methoddef;
		for (Entry entry = ((Symbol) methoddef.sym).owner.members().lookup(methoddef.name); entry.sym != null; entry = entry.next()) {
			if (entry.sym != methoddef.sym && entry.sym.type.erasure().isSameType(((Tree) methoddef).type)) {
				this.log.error(((Tree) methoddef).pos, "name.clash.same.erasure", methoddef.sym.toJava(), entry.sym.toJava());
				return;
			}
		}

	}

	public void _case(final VarDef vardef) {
		vardef.vartype = this.translate(vardef.vartype, null);
		vardef.init = this.translate(vardef.init, vardef.sym.erasure());
		vardef.type = ((Tree) vardef).type.erasure();
		this.result = vardef;
	}

	public void _case(final DoLoop doloop) {
		doloop.body = this.translate(doloop.body);
		doloop.cond = this.translate(doloop.cond, null);
		this.result = doloop;
	}

	public void _case(final WhileLoop whileloop) {
		whileloop.cond = this.translate(whileloop.cond, null);
		whileloop.body = this.translate(whileloop.body);
		this.result = whileloop;
	}

	public void _case(final ForLoop forloop) {
		forloop.init = this.translate(forloop.init, null);
		forloop.cond = this.translate(forloop.cond, null);
		forloop.step = this.translate(forloop.step, null);
		forloop.body = this.translate(forloop.body);
		this.result = forloop;
	}

	public void _case(final Switch switch1) {
		switch1.selector = this.translate(switch1.selector, null);
		switch1.cases = this.translateCases(switch1.cases);
		this.result = switch1;
	}

	public void _case(final Case case1) {
		case1.pat = this.translate(case1.pat, null);
		case1.stats = this.translate(case1.stats);
		this.result = case1;
	}

	public void _case(final Synchronized synchronized1) {
		synchronized1.lock = this.translate(synchronized1.lock, null);
		synchronized1.body = this.translate(synchronized1.body);
		this.result = synchronized1;
	}

	public void _case(final Conditional conditional) {
		conditional.cond = this.translate(conditional.cond, null);
		conditional.thenpart = this.translate(conditional.thenpart);
		conditional.elsepart = this.translate(conditional.elsepart);
		this.result = conditional;
	}

	public void _case(final Exec exec) {
		exec.expr = this.translate(exec.expr, null);
		this.result = exec;
	}

	public void _case(final Throw throw1) {
		throw1.expr = this.translate(throw1.expr, throw1.expr.type.erasure());
		this.result = throw1;
	}

	public void _case(final Apply apply) {
		apply.meth = this.translate(apply.meth, null);
		final Type type = TreeInfo.symbol(apply.meth).erasure();
		apply.args = this.translateArgs(apply.args, type.argtypes());
		this.result = this.retype(apply, type.restype(), this.pt);
	}

	public void _case(final NewClass newclass) {
		if (newclass.encl != null) {
			newclass.encl = this.translate(newclass.encl, newclass.encl.type.erasure());
		}
		newclass.clazz = this.translate(newclass.clazz, null);
		newclass.args = this.translateArgs(newclass.args, newclass.constructor.erasure().argtypes());
		newclass.def = (ClassDef) this.translate(newclass.def, null);
		newclass.type = ((Tree) newclass).type.erasure();
		this.result = newclass;
	}

	public void _case(final NewArray newarray) {
		newarray.elemtype = this.translate(newarray.elemtype, null);
		newarray.dims = this.translate(newarray.dims, null);
		newarray.elems = this.translate(newarray.elems, ((Tree) newarray).type.elemtype().erasure());
		newarray.type = ((Tree) newarray).type.erasure();
		this.result = newarray;
	}

	public void _case(final Assign assign) {
		assign.lhs = this.translate(assign.lhs, null);
		assign.rhs = this.translate(assign.rhs, assign.lhs.type.erasure());
		assign.type = ((Tree) assign).type.erasure();
		this.result = assign;
	}

	public void _case(final Assignop assignop) {
		assignop.lhs = this.translate(assignop.lhs, null);
		assignop.rhs = this.translate(assignop.rhs, null);
		assignop.type = ((Tree) assignop).type.erasure();
		this.result = assignop;
	}

	public void _case(final Operation operation) {
		operation.args = this.translateArgs(operation.args, operation.operator.type.argtypes());
		this.result = operation;
	}

	public void _case(final TypeCast typecast) {
		typecast.clazz = this.translate(typecast.clazz, null);
		typecast.expr = this.translate(typecast.expr, null);
		typecast.type = ((Tree) typecast).type.erasure();
		this.result = typecast;
	}

	public void _case(final TypeTest typetest) {
		typetest.expr = this.translate(typetest.expr, null);
		typetest.clazz = this.translate(typetest.clazz, null);
		this.result = typetest;
	}

	public void _case(final Indexed indexed) {
		indexed.indexed = this.translate(indexed.indexed, indexed.indexed.type.erasure());
		indexed.index = this.translate(indexed.index, null);
		this.result = this.retype(indexed, indexed.indexed.type.elemtype(), this.pt);
	}

	public void _case(final Ident ident) {
		final Type type = ident.sym.erasure();
		if (ident.sym.kind == 2 && ident.sym.type.tag == 14) {
			this.result = this.make.at(((Tree) ident).pos).Type(type);
		} else if (((Tree) ident).type.constValue != null) {
			this.result = ident;
		} else if (ident.sym.kind == 4) {
			this.result = this.retype(ident, type, this.pt);
		} else {
			ident.type = ((Tree) ident).type.erasure();
			this.result = ident;
		}
	}

	public void _case(final Select select) {
		select.selected = this.translate(select.selected, select.selected.type.erasure());
		if (((Tree) select).type.constValue != null) {
			this.result = select;
		} else if (select.sym.kind == 4) {
			this.result = this.retype(select, select.sym.erasure(), this.pt);
		} else {
			select.type = ((Tree) select).type.erasure();
			this.result = select;
		}
	}

	public void _case(final TypeArray typearray) {
		typearray.elemtype = this.translate(typearray.elemtype, null);
		typearray.type = ((Tree) typearray).type.erasure();
		this.result = typearray;
	}

	public Tree translateTopLevelClass(final Tree tree) {
		return this.translate(tree, null);
	}

	private final Log log;
	private final TreeMaker make;
	private Type pt;
}
