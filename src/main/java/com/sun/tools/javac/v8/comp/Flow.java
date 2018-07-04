package com.sun.tools.javac.v8.comp;

import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.VarSymbol;
import com.sun.tools.javac.v8.tree.Tree;
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
import com.sun.tools.javac.v8.tree.Tree.Exec;
import com.sun.tools.javac.v8.tree.Tree.ForLoop;
import com.sun.tools.javac.v8.tree.Tree.Ident;
import com.sun.tools.javac.v8.tree.Tree.Indexed;
import com.sun.tools.javac.v8.tree.Tree.Labelled;
import com.sun.tools.javac.v8.tree.Tree.MethodDef;
import com.sun.tools.javac.v8.tree.Tree.NewArray;
import com.sun.tools.javac.v8.tree.Tree.NewClass;
import com.sun.tools.javac.v8.tree.Tree.Operation;
import com.sun.tools.javac.v8.tree.Tree.Return;
import com.sun.tools.javac.v8.tree.Tree.Select;
import com.sun.tools.javac.v8.tree.Tree.Switch;
import com.sun.tools.javac.v8.tree.Tree.Synchronized;
import com.sun.tools.javac.v8.tree.Tree.Throw;
import com.sun.tools.javac.v8.tree.Tree.Try;
import com.sun.tools.javac.v8.tree.Tree.TypeCast;
import com.sun.tools.javac.v8.tree.Tree.TypeTest;
import com.sun.tools.javac.v8.tree.Tree.VarDef;
import com.sun.tools.javac.v8.tree.Tree.Visitor;
import com.sun.tools.javac.v8.tree.Tree.WhileLoop;
import com.sun.tools.javac.v8.tree.TreeInfo;
import com.sun.tools.javac.v8.util.Bits;
import com.sun.tools.javac.v8.util.Hashtable;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Names;
import com.sun.tools.javac.v8.util.Pair;

public final class Flow extends Visitor {

	public Flow(final Log log1, final Symtab symtab, final Check check) {
		this.inits = new Bits();
		this.uninits = new Bits();
		this.uninitsTry = new Bits();
		this.vars = new VarSymbol[32];
		this.firstadr = 0;
		this.nextadr = 0;
		this.log = log1;
		this.syms = symtab;
		this.chk = check;
	}

	private boolean trackable(final Symbol symbol) {
		return symbol != null && symbol.kind == 4 && (symbol.owner.kind == 16 || symbol.owner == this.enclClass && (symbol.flags() & 0x40010) == 16);
	}

	private void newVar(final VarSymbol varsymbol) {
		if (this.nextadr == this.vars.length) {
			final VarSymbol avarsymbol[] = new VarSymbol[this.nextadr * 2];
			System.arraycopy(this.vars, 0, avarsymbol, 0, this.nextadr);
			this.vars = avarsymbol;
		}
		varsymbol.adr = this.nextadr;
		this.vars[this.nextadr] = varsymbol;
		this.nextadr++;
	}

	private void letInit(final int i, final VarSymbol varsymbol) {
		if (this.inits == null) {
			final Bits bits = this.initsWhenTrue.dup();
			final Bits bits1 = this.initsWhenFalse.dup();
			final Bits bits2 = this.uninitsWhenTrue.dup();
			final Bits bits3 = this.uninitsWhenFalse.dup();
			this.merge();
			final Bits bits4 = this.uninits.dup();
			this.letInit(i, varsymbol);
			this.initsWhenTrue = bits.orSet(this.inits);
			this.initsWhenFalse = bits1.orSet(this.inits);
			final Bits bits5 = bits4.diffSet(this.uninits);
			this.uninitsWhenTrue = bits2.diffSet(bits5);
			this.uninitsWhenFalse = bits3.diffSet(bits5);
			this.inits = null;
			this.uninits = null;
		} else {
			this.inits.incl(varsymbol.adr);
			if ((varsymbol.flags() & 0x10) != 0 && !this.uninits.isMember(varsymbol.adr)) {
				this.log.error(i, "var.might.already.be.assigned", varsymbol.toJava());
			}
			this.uninits.excl(varsymbol.adr);
		}
	}

	private void letInit(final Tree tree) {
		if (tree.tag == 31 || tree.tag == 30 && TreeInfo.name(((Select) tree).selected) == Names._this) {
			final Symbol symbol = TreeInfo.symbol(tree);
			if (this.trackable(symbol)) {
				this.letInit(tree.pos, (VarSymbol) symbol);
			}
		}
	}

	private void checkInit(final int i, final VarSymbol varsymbol) {
		if (!this.inits.isMember(varsymbol.adr)) {
			this.log.error(i, "var.might.not.have.been.initialized", varsymbol.toJava());
			this.inits.incl(varsymbol.adr);
		}
	}

	private void checkBackBranch(final int i, final Bits bits, final Bits bits1) {
		for (int j = this.firstadr; j < this.nextadr; j++) {
			final VarSymbol varsymbol = this.vars[j];
			if (varsymbol != null && (varsymbol.flags() & 0x10) != 0 && bits.isMember(j) && !bits1.isMember(j)) {
				this.log.error(i, "var.might.be.assigned.in.loop", varsymbol.toJava());
			}
		}

	}

	private static boolean isFalse(final Tree tree) {
		return tree.type.tag == 8 && tree.type.constValue != null && ((Number) tree.type.constValue).intValue() == 0;
	}

	private static boolean isTrue(final Tree tree) {
		return tree.type.tag == 8 && tree.type.constValue != null && ((Number) tree.type.constValue).intValue() != 0;
	}

	private void jump(final Tree tree, final Hashtable hashtable) {
		final Pair pair = (Pair) hashtable.get(tree);
		if (pair == null) {
			hashtable.put(tree, new Pair(this.inits.dup(), this.uninits.dup()));
		} else {
			((Bits) pair.fst).andSet(this.inits);
			((Bits) pair.snd).andSet(this.uninits);
		}
	}

	private void resolve(final Tree tree, final Hashtable hashtable) {
		final Pair pair = (Pair) hashtable.get(tree);
		if (pair != null) {
			this.inits.andSet((Bits) pair.fst);
			this.uninits.andSet((Bits) pair.snd);
			this.alive = true;
		}
	}

	private void markThrown(final int i, final ClassSymbol classsymbol) {
		if (!this.chk.isUnchecked(classsymbol)) {
			this.chk.checkHandled(i, classsymbol, this.reported);
			this.thrown = Check.incl(classsymbol, this.thrown);
		}
	}

	private void markDead() {
		this.inits.inclRange(this.firstadr, this.nextadr);
		this.uninitsTry.andSet(this.uninits);
		this.uninits.inclRange(this.firstadr, this.nextadr);
		this.alive = false;
	}

	private void split() {
		this.initsWhenFalse = this.inits.dup();
		this.uninitsWhenFalse = this.uninits.dup();
		this.initsWhenTrue = this.inits;
		this.uninitsWhenTrue = this.uninits;
	}

	private void merge() {
		this.inits = this.initsWhenFalse.andSet(this.initsWhenTrue);
		this.uninits = this.uninitsWhenFalse.andSet(this.uninitsWhenTrue);
	}

	private void analyze(final Tree tree, final int i) {
		if (tree != null) {
			final int j = this.mode;
			this.mode = i;
			tree.visit(this);
			this.mode = j;
		}
	}

	private void analyzeVar(final Tree tree) {
		this.analyze(tree, 2);
	}

	private void analyzeExpr(final Tree tree) {
		if (tree != null) {
			this.analyze(tree, 1);
			if (this.inits == null) {
				this.merge();
			}
		}
	}

	private void analyzeCond(final Tree tree) {
		if (isFalse(tree)) {
			this.initsWhenTrue = this.inits.dup();
			this.initsWhenTrue.inclRange(this.firstadr, this.nextadr);
			this.uninitsTry.andSet(this.uninits);
			this.uninitsWhenTrue = this.uninits.dup();
			this.uninitsWhenTrue.inclRange(this.firstadr, this.nextadr);
			this.initsWhenFalse = this.inits;
			this.uninitsWhenFalse = this.uninits;
		} else if (isTrue(tree)) {
			this.initsWhenFalse = this.inits.dup();
			this.initsWhenFalse.inclRange(this.firstadr, this.nextadr);
			this.uninitsTry.andSet(this.uninits);
			this.uninitsWhenFalse = this.uninits.dup();
			this.uninitsWhenFalse.inclRange(this.firstadr, this.nextadr);
			this.initsWhenTrue = this.inits;
			this.uninitsWhenTrue = this.uninits;
		} else {
			this.analyze(tree, 3);
			if (this.inits != null) {
				this.split();
			}
		}
		this.inits = null;
		this.uninits = null;
	}

	public void analyzeDef(final Tree tree) {
		this.analyze(tree, 0);
		if (tree != null && tree.tag == 6 && !this.alive) {
			this.log.error(tree.pos, "initializer.must.be.able.to.complete.normally");
		}
	}

	private void analyzeStat(final Tree tree) {
		if (!this.alive && tree != null && (tree.tag != 6 || ((Block) tree).stats.nonEmpty())) {
			this.log.error(tree.pos, "unreachable.stmt");
			this.alive = true;
		}
		this.analyze(tree, 0);
		this.resolve(tree, this.breakTargetStates);
	}

	private void analyzeExprs(final List list) {
		if (list != null) {
			for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
				this.analyzeExpr((Tree) list1.head);
			}

		}
	}

	private void analyzeStats(final List list) {
		if (list != null) {
			for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
				this.analyzeStat((Tree) list1.head);
			}

		}
	}

	public void _case(final ClassDef classdef) {
		if (classdef.sym == null) {
			return;
		}
		final List list = this.thrown;
		final List list1 = this.reported;
		final boolean flag = this.alive;
		final int i = this.firstadr;
		final ClassSymbol classsymbol = this.enclClass;
		final Hashtable hashtable = this.breakTargetStates;
		final Hashtable hashtable1 = this.continueTargetStates;
		this.thrown = ClassSymbol.emptyList;
		this.reported = ClassSymbol.emptyList;
		for (List list2 = classdef.defs; list2.nonEmpty(); list2 = list2.tail) {
			if (TreeInfo.isInitialConstructor((Tree) list2.head)) {
				final List list3 = ((Symbol) ((MethodDef) list2.head).sym).type.thrown();
				this.reported = this.reported == null ? list3 : Check.intersect(list3, this.reported);
			}
		}

		this.firstadr = this.nextadr;
		this.enclClass = classdef.sym;
		this.breakTargetStates = Hashtable.make();
		this.continueTargetStates = Hashtable.make();
		for (List list4 = classdef.defs; list4.nonEmpty(); list4 = list4.tail) {
			this.alive = true;
			if (((Tree) list4.head).tag != 4 && (TreeInfo.flags((Tree) list4.head) & 8) != 0) {
				this.analyzeDef((Tree) list4.head);
			}
		}

		for (List list5 = classdef.defs; list5.nonEmpty(); list5 = list5.tail) {
			this.alive = true;
			if (((Tree) list5.head).tag != 4 && (TreeInfo.flags((Tree) list5.head) & 8) == 0) {
				this.analyzeDef((Tree) list5.head);
			}
		}

		for (List list6 = classdef.defs; list6.nonEmpty(); list6 = list6.tail) {
			this.alive = true;
			if (((Tree) list6.head).tag == 4) {
				this.analyzeDef((Tree) list6.head);
			}
		}

		this.thrown = list;
		this.reported = list1;
		this.alive = flag;
		this.nextadr = this.firstadr;
		this.firstadr = i;
		this.enclClass = classsymbol;
		this.breakTargetStates = hashtable;
		this.continueTargetStates = hashtable1;
	}

	public void _case(final MethodDef methoddef) {
		if (methoddef.body != null) {
			final Bits bits = this.inits.dup();
			final Bits bits1 = this.uninits.dup();
			final List list = this.reported;
			final int i = this.nextadr;
			final List list1 = ((Symbol) methoddef.sym).type.thrown();
			final boolean flag = TreeInfo.isInitialConstructor(methoddef);
			this.reported = flag ? this.reported.prepend(list1) : list1;
			this.breakTargetStates.clear();
			this.continueTargetStates.clear();
			if (!flag) {
				for (int j = this.firstadr; j < this.nextadr; j++) {
					this.inits.incl(j);
					this.uninits.excl(j);
				}

			}
			for (List list2 = methoddef.params; list2.nonEmpty(); list2 = list2.tail) {
				this.analyzeDef((Tree) list2.head);
				this.letInit(((Tree) (VarDef) list2.head).pos, ((VarDef) list2.head).sym);
			}

			this.analyzeStat(methoddef.body);
			final int k = ((Tree) methoddef.body).pos;
			if (this.alive && ((Symbol) methoddef.sym).type.restype().tag != 9) {
				this.log.error(k, "missing.ret.stmt");
			}
			if (flag) {
				for (int l = this.firstadr; l < this.nextadr; l++) {
					this.checkInit(k, this.vars[l]);
				}

			}
			this.inits = bits;
			this.uninits = bits1;
			this.reported = list;
			this.nextadr = i;
		}
	}

	public void _case(final VarDef vardef) {
		final boolean flag = this.trackable(vardef.sym);
		if (flag) {
			this.newVar(vardef.sym);
		}
		if (vardef.init != null) {
			this.analyzeExpr(vardef.init);
			if (flag) {
				this.inits.incl(vardef.sym.adr);
				this.uninits.excl(vardef.sym.adr);
			}
		} else if (flag) {
			this.inits.excl(vardef.sym.adr);
			this.uninits.incl(vardef.sym.adr);
		}
	}

	public void _case(final Block block) {
		final int i = this.nextadr;
		this.analyzeStats(block.stats);
		this.nextadr = i;
	}

	public void _case(final DoLoop doloop) {
		final Bits bits = this.uninits.dup();
		this.analyzeStat(doloop.body);
		this.resolve(doloop, this.continueTargetStates);
		this.analyzeCond(doloop.cond);
		this.checkBackBranch(((Tree) doloop).pos, bits, this.uninitsWhenTrue);
		this.alive &= !isTrue(doloop.cond);
		this.inits = this.initsWhenFalse;
		this.uninits = this.uninitsWhenFalse;
	}

	public void _case(final WhileLoop whileloop) {
		final Bits bits = this.uninits.dup();
		this.analyzeCond(whileloop.cond);
		final Bits bits1 = this.initsWhenFalse;
		final Bits bits2 = this.uninitsWhenFalse;
		this.inits = this.initsWhenTrue;
		this.uninits = this.uninitsWhenTrue;
		final boolean flag = this.alive;
		this.alive &= !isFalse(whileloop.cond);
		this.analyzeStat(whileloop.body);
		this.checkBackBranch(((Tree) whileloop).pos, bits, this.uninits);
		this.alive = flag && !isTrue(whileloop.cond);
		this.inits = bits1;
		this.uninits = bits2;
	}

	public void _case(final ForLoop forloop) {
		final int i = this.nextadr;
		this.analyzeStats(forloop.init);
		final Bits bits = this.uninits.dup();
		final Bits bits1;
		final Bits bits2;
		final boolean flag;
		if (forloop.cond != null) {
			this.analyzeCond(forloop.cond);
			bits1 = this.initsWhenFalse;
			bits2 = this.uninitsWhenFalse;
			this.inits = this.initsWhenTrue;
			this.uninits = this.uninitsWhenFalse;
			flag = this.alive;
			this.alive &= !isFalse(forloop.cond);
		} else {
			bits1 = this.inits.dup();
			bits1.inclRange(this.firstadr, this.nextadr);
			this.uninitsTry.andSet(this.uninits);
			bits2 = this.uninits.dup();
			bits2.inclRange(this.firstadr, this.nextadr);
			flag = this.alive;
		}
		this.analyzeStat(forloop.body);
		this.resolve(forloop, this.continueTargetStates);
		this.analyzeExprs(forloop.step);
		this.checkBackBranch(((Tree) forloop).pos, bits, this.uninits);
		this.alive = flag && forloop.cond != null && !isTrue(forloop.cond);
		this.inits = bits1;
		this.uninits = bits2;
		this.nextadr = i;
	}

	public void _case(final Labelled labelled) {
		this.analyzeStat(labelled.body);
	}

	public void _case(final Switch switch1) {
		final int i = this.nextadr;
		this.analyzeExpr(switch1.selector);
		final Bits bits = this.inits;
		final boolean flag = this.alive;
		boolean flag1 = false;
		for (List list = switch1.cases; list.nonEmpty(); list = list.tail) {
			this.inits = bits.dup();
			this.alive = flag;
			this.analyzeStats(((Case) list.head).stats);
			if (((Case) list.head).pat == null) {
				flag1 = true;
			}
		}

		if (!flag1) {
			this.alive |= flag;
			this.inits.andSet(bits);
		}
		this.nextadr = i;
	}

	public void _case(final Synchronized synchronized1) {
		this.analyzeExpr(synchronized1.lock);
		this.analyzeStat(synchronized1.body);
	}

	public void _case(final Try try1) {
		final List list = this.reported;
		final List list1 = this.thrown;
		final Bits bits = this.uninitsTry;
		this.thrown = ClassSymbol.emptyList;
		for (List list2 = try1.catchers; list2.nonEmpty(); list2 = list2.tail) {
			final ClassSymbol classsymbol = (ClassSymbol) ((Tree) ((Catch) list2.head).param).type.tsym;
			this.reported = Check.incl(classsymbol, this.reported);
		}

		final Bits bits1 = this.inits.dup();
		this.uninitsTry = this.uninits.dup();
		final boolean flag = this.alive;
		this.analyzeStat(try1.body);
		this.uninitsTry.andSet(this.uninits);
		final Bits bits2 = this.inits;
		final Bits bits3 = this.uninits;
		boolean flag1 = this.alive;
		final List list3 = this.thrown;
		this.thrown = list1;
		List list4 = ClassSymbol.emptyList;
		final int i = this.nextadr;
		this.reported = list;
		for (List list5 = try1.catchers; list5.nonEmpty(); list5 = list5.tail) {
			final VarDef vardef = ((Catch) list5.head).param;
			final ClassSymbol classsymbol1 = (ClassSymbol) ((Tree) vardef).type.tsym;
			if (Check.subset(classsymbol1, list4)) {
				this.log.error(((Tree) (Catch) list5.head).pos, "except.already.caught", classsymbol1.fullname.toJava());
			} else if (!this.chk.isUnchecked(classsymbol1) && classsymbol1 != this.syms.throwableType.tsym && classsymbol1 != this.syms.exceptionType.tsym) {
				if (!Check.intersects(classsymbol1, list3)) {
					this.log.error(((Tree) (Catch) list5.head).pos, "except.never.thrown.in.try", classsymbol1.fullname.toJava());
				}
			}
			this.alive = true;
			list4 = Check.incl(classsymbol1, list4);
			this.inits = bits1.dup();
			this.uninits = this.uninitsTry.dup();
			this.analyzeDef(vardef);
			this.letInit(((Tree) vardef).pos, vardef.sym);
			this.analyzeStat(((Catch) list5.head).body);
			bits2.andSet(this.inits);
			bits3.andSet(this.uninits);
			flag1 |= this.alive;
			this.nextadr = i;
		}

		if (try1.finalizer != null) {
			this.inits = bits1.dup();
			this.uninits = this.uninitsTry.dup();
			this.alive = flag;
			this.analyzeStat(try1.finalizer);
			this.inits.orSet(bits2);
			this.uninits.andSet(bits3);
			this.alive &= flag1;
		} else {
			this.inits = bits2;
			this.uninits = bits3;
			this.alive = flag1;
		}
		this.thrown = Check.union(this.thrown, Check.diff(list3, list4));
		this.uninitsTry.andSet(bits).andSet(this.uninits);
	}

	public void _case(final Conditional conditional) {
		this.analyzeCond(conditional.cond);
		if (((Tree) conditional).tag == 17) {
			final Bits bits = this.initsWhenFalse;
			final Bits bits2 = this.uninitsWhenFalse;
			final boolean flag = this.alive;
			this.inits = this.initsWhenTrue;
			this.uninits = this.uninitsWhenTrue;
			this.analyzeStat(conditional.thenpart);
			if (conditional.elsepart != null) {
				final Bits bits5 = this.inits.dup();
				final Bits bits7 = this.uninits.dup();
				final boolean flag1 = this.alive;
				this.inits = bits;
				this.uninits = bits2;
				this.alive = flag;
				this.analyzeStat(conditional.elsepart);
				this.inits.andSet(bits5);
				this.uninits.andSet(bits7);
				this.alive |= flag1;
			} else {
				this.inits.andSet(bits);
				this.uninits.andSet(bits2);
				this.alive = true;
			}
		} else {
			final Bits bits1 = this.initsWhenFalse;
			final Bits bits3 = this.uninitsWhenFalse;
			this.inits = this.initsWhenTrue;
			this.uninits = this.uninitsWhenTrue;
			this.analyzeExpr(conditional.thenpart);
			final Bits bits4 = this.inits.dup();
			final Bits bits6 = this.uninits.dup();
			this.inits = bits1;
			this.uninits = bits3;
			this.analyzeExpr(conditional.elsepart);
			this.inits.andSet(bits4);
			this.uninits.andSet(bits6);
		}
	}

	public void _case(final Exec exec) {
		this.analyzeExpr(exec.expr);
	}

	public void _case(final Break break1) {
		this.jump(break1.target, this.breakTargetStates);
		this.markDead();
	}

	public void _case(final Continue continue1) {
		this.jump(continue1.target, this.continueTargetStates);
		this.markDead();
	}

	public void _case(final Return return1) {
		this.analyzeExpr(return1.expr);
		this.markDead();
	}

	public void _case(final Throw throw1) {
		this.analyzeExpr(throw1.expr);
		this.markThrown(((Tree) throw1).pos, (ClassSymbol) throw1.expr.type.tsym);
		this.markDead();
	}

	public void _case(final Apply apply) {
		this.analyzeExpr(apply.meth);
		this.analyzeExprs(apply.args);
		for (List list = TreeInfo.symbol(apply.meth).type.thrown(); list.nonEmpty(); list = list.tail) {
			this.markThrown(apply.meth.pos, (ClassSymbol) list.head);
		}

	}

	public void _case(final NewClass newclass) {
		this.analyzeExpr(newclass.encl);
		this.analyzeExprs(newclass.args);
		for (List list = newclass.constructor.type.thrown(); list.nonEmpty(); list = list.tail) {
			this.markThrown(((Tree) newclass).pos, (ClassSymbol) list.head);
		}

		this.analyzeDef(newclass.def);
	}

	public void _case(final NewArray newarray) {
		this.analyzeExprs(newarray.dims);
		this.analyzeExprs(newarray.elems);
	}

	public void _case(final Assign assign) {
		this.analyzeVar(assign.lhs);
		this.analyzeExpr(assign.rhs);
		this.letInit(assign.lhs);
	}

	public void _case(final Assignop assignop) {
		this.analyzeExpr(assignop.lhs);
		this.analyzeExpr(assignop.rhs);
		this.letInit(assignop.lhs);
	}

	public void _case(final Operation operation) {
		switch (((Tree) operation).tag) {
		case 47: // '/'
			this.analyzeCond((Tree) operation.args.head);
			final Bits bits = this.initsWhenFalse;
			final Bits bits1 = this.uninitsWhenFalse;
			this.inits = this.initsWhenTrue;
			this.uninits = this.uninitsWhenTrue;
			this.analyzeCond((Tree) operation.args.tail.head);
			this.initsWhenFalse.andSet(bits);
			this.uninitsWhenFalse.andSet(bits1);
			break;

		case 46: // '.'
			this.analyzeCond((Tree) operation.args.head);
			final Bits bits2 = this.initsWhenTrue;
			final Bits bits3 = this.uninitsWhenTrue;
			this.inits = this.initsWhenFalse;
			this.uninits = this.uninitsWhenFalse;
			this.analyzeCond((Tree) operation.args.tail.head);
			this.initsWhenTrue.andSet(bits2);
			this.uninitsWhenTrue.andSet(bits3);
			break;

		case 40: // '('
			this.analyzeCond((Tree) operation.args.head);
			Bits bits4 = this.initsWhenFalse;
			this.initsWhenFalse = this.initsWhenTrue;
			this.initsWhenTrue = bits4;
			bits4 = this.uninitsWhenFalse;
			this.uninitsWhenFalse = this.uninitsWhenTrue;
			this.uninitsWhenTrue = bits4;
			break;

		case 42: // '*'
		case 43: // '+'
		case 44: // ','
		case 45: // '-'
			this.analyzeExpr((Tree) operation.args.head);
			this.letInit((Tree) operation.args.head);
			break;

		case 41: // ')'
		default:
			for (List list = operation.args; list.nonEmpty(); list = list.tail) {
				this.analyzeExpr((Tree) list.head);
			}

			break;
		}
	}

	public void _case(final TypeCast typecast) {
		this.analyzeExpr(typecast.expr);
	}

	public void _case(final TypeTest typetest) {
		this.analyzeExpr(typetest.expr);
	}

	public void _case(final Indexed indexed) {
		this.analyzeExpr(indexed.indexed);
		this.analyzeExpr(indexed.index);
	}

	public void _case(final Ident ident) {
		if (this.trackable(ident.sym) && this.mode != 2) {
			this.checkInit(((Tree) ident).pos, (VarSymbol) ident.sym);
		}
	}

	public void _case(final Select select) {
		this.analyzeExpr(select.selected);
	}

	public void _case(final Tree tree) {
	}

	private final Log log;
	private final Symtab syms;
	private final Check chk;
	private Bits inits;
	private Bits uninits;
	private Bits uninitsTry;
	private Bits initsWhenTrue;
	private Bits initsWhenFalse;
	private Bits uninitsWhenTrue;
	private Bits uninitsWhenFalse;
	private VarSymbol[] vars;
	private List thrown;
	private List reported;
	private boolean alive;
	private int firstadr;
	private int nextadr;
	private Hashtable breakTargetStates;
	private Hashtable continueTargetStates;
	private ClassSymbol enclClass;
	static final int STATmode = 0;
	static final int EXPRmode = 1;
	static final int VARmode = 2;
	static final int CONDmode = 3;
	private int mode;
}
