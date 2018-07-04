package com.sun.tools.javac.v8.comp;

import java.util.Enumeration;

import com.sun.tools.javac.v8.code.Code;
import com.sun.tools.javac.v8.code.Code.Chain;
import com.sun.tools.javac.v8.code.Pool;
import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.CompletionFailure;
import com.sun.tools.javac.v8.code.Symbol.MethodSymbol;
import com.sun.tools.javac.v8.code.Symbol.OperatorSymbol;
import com.sun.tools.javac.v8.code.Symbol.VarSymbol;
import com.sun.tools.javac.v8.code.Type;
import com.sun.tools.javac.v8.code.Type.MethodType;
import com.sun.tools.javac.v8.comp.Items.CondItem;
import com.sun.tools.javac.v8.comp.Items.Item;
import com.sun.tools.javac.v8.comp.Items.LocalItem;
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
import com.sun.tools.javac.v8.tree.Tree.Try;
import com.sun.tools.javac.v8.tree.Tree.TypeCast;
import com.sun.tools.javac.v8.tree.Tree.TypeTest;
import com.sun.tools.javac.v8.tree.Tree.VarDef;
import com.sun.tools.javac.v8.tree.Tree.Visitor;
import com.sun.tools.javac.v8.tree.Tree.WhileLoop;
import com.sun.tools.javac.v8.tree.TreeInfo;
import com.sun.tools.javac.v8.tree.TreeMaker;
import com.sun.tools.javac.v8.util.FatalError;
import com.sun.tools.javac.v8.util.Hashtable;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.ListBuffer;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;
import com.sun.tools.javac.v8.util.Util;

public final class Gen extends Visitor {
	static class GenContext {

		void addExit(final Chain chain) {
			this.exit = Code.mergeChains(chain, this.exit);
		}

		void addCont(final Chain chain) {
			this.cont = Code.mergeChains(chain, this.cont);
		}

		Chain exit;
		Chain cont;
		GenFinalizer finalize;

		GenContext() {
			this.exit = null;
			this.cont = null;
			this.finalize = null;
		}
	}

	abstract static class GenFinalizer {

		abstract void gen();

	}

	public Gen(final Log log1, final Symtab symtab, final Check check, final Resolve resolve, final TreeMaker treemaker, final Hashtable hashtable) {
		this.pool = new Pool();
		this.nerrs = 0;
		this.log = log1;
		this.syms = symtab;
		this.chk = check;
		this.rs = resolve;
		this.make = treemaker;
		this.switchCheck = hashtable.get("-switchcheck") != null;
		final String s = (String) hashtable.get("-g:");
		if (s != null) {
			this.lineDebugInfo = Util.contains(s, "lines", ',');
			this.varDebugInfo = Util.contains(s, "vars", ',');
		} else {
			this.lineDebugInfo = true;
			this.varDebugInfo = hashtable.get("-g") != null;
		}
	}

	private void loadIntConst(final int i) {
		this.items.makeImmediateItem(Type.intType, new Integer(i)).load();
	}

	private static int zero(final int i) {
		switch (i) {
		case 0: // '\0'
		case 5: // '\005'
		case 6: // '\006'
		case 7: // '\007'
			return 3;

		case 1: // '\001'
			return 9;

		case 2: // '\002'
			return 11;

		case 3: // '\003'
			return 14;

		case 4: // '\004'
		default:
			throw new InternalError("zero");
		}
	}

	private static int one(final int i) {
		return zero(i) + 1;
	}

	private void emitMinusOne(final int i) {
		if (i == 1) {
			this.items.makeImmediateItem(Type.longType, new Long(-1L)).load();
		} else {
			this.code.emitop(2);
		}
	}

	private Symbol makeAccessible(final Symbol symbol, final Type type) {
		if (this.syms.writer.majorVersion >= MAJOR_VERSION_1_2) {
			return symbol.owner == type.tsym || type.tag != 10 || symbol.owner.kind == 2 && ((ClassSymbol) symbol.owner).fullname == Names.java_lang_Object || symbol.kind == 4 && (symbol.owner.flags() & 0x200) != 0 ? symbol : symbol.clone(type.tsym);
		}
		return Resolve.isAccessible(this.attrEnv, symbol.owner) ? symbol : symbol.clone(type.tsym);
	}

	private int makeRef(final int i, final Type type) {
		this.checkDimension(i, type);
		return this.pool.put(type.tag != 10 ? (Object) type : (Object) type.tsym);
	}

	private void checkDimension(final int i, final Type type) {
		if (type.dimensions() > 255) {
			this.log.error(i, "limit.dimensions");
			this.nerrs++;
		}
	}

	private LocalItem makeTemp(final Type type) {
		return (LocalItem) this.items.makeLocalItem(type, this.code.newLocal(type));
	}

	private void callMethod(final int i, final Type type, final Name name, final List list, final boolean flag) {
		final Symbol symbol = this.rs.resolveQualifiedMethod(i, this.attrEnv, type, name, Type.emptyList, list);
		if (symbol.kind == 16) {
			if (flag) {
				this.items.makeStaticItem(symbol).invoke();
			} else {
				this.items.makeMemberItem(symbol, name == Names.init).invoke();
			}
		} else {
			throw new FatalError(Log.getLocalizedString("cant.locate.meth", name.toJava()));
		}
	}

	private void callFinalizer(final Env env1) {
		if (this.code.isAlive()) {
			if (env1.tree.tag == 14) {
				final Tree tree = ((Try) env1.tree).finalizer;
				if (tree != null) {
					((GenContext) env1.info).cont = new Chain(this.code.emitJump(168), this.code.stacksize + 1, ((GenContext) env1.info).cont);
				}
			} else if (env1.tree.tag == 13) {
				((GenContext) env1.info).finalize.gen();
			}
		}
	}

	private Env unwind(final Tree tree, Env env1) {
		Env env2;
		for (env2 = env1; env2 != null && env2.tree != tree; env2 = env2.next) {
		}
		Tree tree1 = null;
		for (Env env3 = null; env3 != env2;) {
			if (env1.tree != tree1) {
				this.callFinalizer(env1);
				tree1 = env1.tree;
			}
			env3 = env1;
			env1 = env1.next;
		}

		return env2;
	}

	private static boolean hasFinalizers(final Tree tree, Env env1) {
		for (; env1.tree != tree; env1 = env1.next) {
			if (env1.tree.tag == 14 && ((Try) env1.tree).finalizer != null || env1.tree.tag == 13) {
				return true;
			}
		}

		return false;
	}

	private List normalizeDefs(final List list, final ClassSymbol classsymbol) {
		final ListBuffer listbuffer = new ListBuffer();
		final ListBuffer listbuffer1 = new ListBuffer();
		final ListBuffer listbuffer2 = new ListBuffer();
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			final Tree tree = (Tree) list1.head;
			switch (tree.tag) {
			case 6: // '\006'
				final Block block = (Block) tree;
				if ((block.flags & 8) != 0) {
					listbuffer1.append(tree);
				} else {
					listbuffer.append(tree);
				}
				break;

			case 4: // '\004'
				listbuffer2.append(tree);
				break;

			case 5: // '\005'
				final VarDef vardef = (VarDef) tree;
				final VarSymbol varsymbol = vardef.sym;
				this.checkDimension(((Tree) vardef).pos, ((Symbol) varsymbol).type);
				if (vardef.init == null) {
					break;
				}
				if ((varsymbol.flags() & 8) == 0) {
					final Tree tree1 = this.make.at(((Tree) vardef).pos).Assignment(vardef.sym, vardef.init);
					listbuffer.append(tree1);
					break;
				}
				if (varsymbol.constValue == null) {
					final Tree tree2 = this.make.at(((Tree) vardef).pos).Assignment(vardef.sym, vardef.init);
					listbuffer1.append(tree2);
				} else {
					this.checkStringConstant(vardef.init.pos, varsymbol.constValue);
				}
				break;

			default:
				throw new InternalError();
			}
		}

		if (!listbuffer.isEmpty()) {
			final List list2 = listbuffer.toList();
			for (final Enumeration enumeration = listbuffer2.elements(); enumeration.hasMoreElements(); normalizeMethod((MethodDef) enumeration.nextElement(), list2)) {
			}
		}
		if (!listbuffer1.isEmpty()) {
			final MethodSymbol methodsymbol = new MethodSymbol(8, Names.clinit, new MethodType(Type.emptyList, Type.voidType, ClassSymbol.emptyList), classsymbol);
			classsymbol.members().enter(methodsymbol);
			final List list3 = listbuffer1.toList();
			this.make.at(((Tree) list3.head).pos);
			listbuffer2.append(this.make.MethodDef(methodsymbol, this.make.Block(0, list3)));
		}
		return listbuffer2.toList();
	}

	private void checkStringConstant(final int i, final Object obj) {
		if (this.nerrs != 0 || !(obj instanceof String) || ((String) obj).length() < Pool.MAX_STRING_LENGTH) {
			return;
		}
		this.log.error(i, "limit.string");
		this.nerrs++;
	}

	private static void normalizeMethod(final MethodDef methoddef, final List list) {
		if (methoddef.name == Names.init && TreeInfo.isInitialConstructor(methoddef)) {
			List list1 = methoddef.body.stats;
			final ListBuffer listbuffer = new ListBuffer();
			if (list1.nonEmpty()) {
				listbuffer.append(list1.head);
				for (list1 = list1.tail; list1.nonEmpty() && TreeInfo.isSyntheticInit((Tree) list1.head); list1 = list1.tail) {
					listbuffer.append(list1.head);
				}

				listbuffer.appendList(list);
				for (; list1.nonEmpty(); list1 = list1.tail) {
					listbuffer.append(list1.head);
				}

			}
			methoddef.body.stats = listbuffer.toList();
		}
	}

	private void genDef(final Tree tree, final Env env1) {
		final Env env2 = this.env;
		try {
			this.env = env1;
			tree.visit(this);
			this.env = env2;
		} catch (final CompletionFailure completionfailure) {
			this.env = env2;
			this.chk.completionError(tree.pos, completionfailure);
		}
	}

	void genStat(final Tree tree, final Env env1) {
		if (this.code.isAlive()) {
			this.code.statBegin(tree.pos);
			this.genDef(tree, env1);
		}
	}

	private void genStats(final List list, final Env env1) {
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			this.genStat((Tree) list1.head, env1);
		}

	}

	private Item genExpr(final Tree tree, final Type type) {
		final Type type1 = this.pt;
		try {
			if (tree.type.constValue != null) {
				this.checkStringConstant(tree.pos, tree.type.constValue);
				this.result = this.items.makeImmediateItem(tree.type, tree.type.constValue);
			} else {
				this.pt = type;
				tree.visit(this);
				this.pt = type1;
			}
			return this.result.coerce(type);
		} catch (final CompletionFailure completionfailure) {
			this.pt = type1;
			this.chk.completionError(tree.pos, completionfailure);
			return this.items.makeStackItem(type);
		}
	}

	private void genArgs(final List list, List list1) {
		for (List list2 = list; list2.nonEmpty(); list2 = list2.tail) {
			this.genExpr((Tree) list2.head, (Type) list1.head).load();
			list1 = list1.tail;
		}

	}

	public void _case(final MethodDef methoddef) {
		final Env env1 = this.env.dup(methoddef);
		env1.enclMethod = methoddef;
		this.pt = methoddef.sym.erasure().restype();
		this.genMethod(methoddef, env1, false);
	}

	private void genMethod(final MethodDef methoddef, final Env env1, final boolean flag) {
		final MethodSymbol methodsymbol = methoddef.sym;
		if (Code.width(((Symbol) env1.enclMethod.sym).type.erasure().argtypes()) + ((methoddef.flags & 8) != 0 && !methodsymbol.isConstructor() ? 0 : 1) > 255) {
			this.log.error(((Tree) methoddef).pos, "limit.parameters");
			this.nerrs++;
		} else if (methoddef.body != null) {
			methodsymbol.code = this.code = new Code(flag, this.lineDebugInfo, this.varDebugInfo);
			this.items = new Items(this.pool, this.code);
			if ((methoddef.flags & 8) == 0) {
				this.code.setDefined(this.code.newLocal(new VarSymbol(16, Names._this, ((Symbol) methodsymbol).owner.type, ((Symbol) methodsymbol).owner)));
			}
			for (List list = methoddef.params; list.nonEmpty(); list = list.tail) {
				this.checkDimension(((Tree) (VarDef) list.head).pos, ((Symbol) ((VarDef) list.head).sym).type);
				this.code.setDefined(this.code.newLocal(((VarDef) list.head).sym));
			}

			this.genStat(methoddef.body, env1);
			Util.assertTrue(this.code.stacksize == 0);
			if (this.code.isAlive()) {
				this.code.statBegin(TreeInfo.endPos(methoddef.body));
				this.code.emitop(177);
			}
			this.code.endScopes(0);
			if (this.code.checkLimits(((Tree) methoddef).pos, this.log)) {
				this.nerrs++;
				return;
			}
			if (!flag && this.code.fatcode) {
				this.genMethod(methoddef, env1, true);
			}
		}
	}

	public void _case(final VarDef vardef) {
		final VarSymbol varsymbol = vardef.sym;
		this.code.newLocal(varsymbol);
		if (vardef.init != null) {
			this.checkStringConstant(vardef.init.pos, varsymbol.constValue);
			if (varsymbol.constValue == null || this.varDebugInfo) {
				this.genExpr(vardef.init, varsymbol.erasure()).load();
				this.items.makeLocalItem(varsymbol).store();
			}
		}
		this.checkDimension(((Tree) vardef).pos, ((Symbol) varsymbol).type);
	}

	public void _case(final Block block) {
		final int i = this.code.nextadr;
		this.genStats(block.stats, this.env);
		this.code.endScopes(i);
	}

	public void _case(final DoLoop doloop) {
		this.genLoop(doloop, doloop.body, doloop.cond, Tree.emptyList, null);
	}

	public void _case(final WhileLoop whileloop) {
		this.genLoop(whileloop, whileloop.body, whileloop.cond, Tree.emptyList, this.code.branch(167));
	}

	public void _case(final ForLoop forloop) {
		this.genStats(forloop.init, this.env);
		this.genLoop(forloop, forloop.body, forloop.cond, forloop.step, this.code.branch(167));
	}

	private void genLoop(final Tree tree, final Tree tree1, final Tree tree2, final List list, final Chain chain) {
		final Env env1 = this.env.dup(tree, new GenContext());
		final int i = this.code.entryPoint();
		this.genStat(tree1, env1);
		this.code.resolve(((GenContext) env1.info).cont);
		this.genStats(list, env1);
		this.code.resolve(chain);
		final CondItem conditem;
		if (tree2 != null) {
			this.code.statBegin(tree2.pos);
			conditem = this.genExpr(tree2, Type.booleanType).mkCond();
		} else {
			conditem = this.items.makeCondItem(167);
		}
		this.code.resolve(conditem.jumpTrue(), i);
		this.code.resolve(conditem.falseJumps);
		this.code.resolve(((GenContext) env1.info).exit);
	}

	public void _case(final Labelled labelled) {
		final Env env1 = this.env.dup(TreeInfo.referencedStatement(labelled), new GenContext());
		this.genStat(labelled.body, env1);
		this.code.resolve(((GenContext) env1.info).exit);
	}

	public void _case(final Switch switch1) {
		final Item item = this.genExpr(switch1.selector, Type.intType);
		final List list = switch1.cases;
		if (list.isEmpty()) {
			item.drop();
		} else {
			item.load();
			final Env env1 = this.env.dup(switch1, new GenContext());
			int i = 0x7fffffff;
			int j = 0x80000000;
			int k = 0;
			final int ai[] = new int[list.length()];
			int l = -1;
			List list1 = list;
			for (int i1 = 0; i1 < ai.length; i1++) {
				if (((Case) list1.head).pat != null) {
					final int j1 = ((Number) ((Case) list1.head).pat.type.constValue).intValue();
					ai[i1] = j1;
					if (j1 < i) {
						i = j1;
					}
					if (j < j1) {
						j = j1;
					}
					k++;
				} else {
					Util.assertTrue(l == -1);
					l = i1;
				}
				list1 = list1.tail;
			}

			final long l1 = 4L + j - i + 1L;
			final long l2 = 3L;
			final long l3 = 3L + 2L * k;
			final long l4 = k;
			final char c = k <= 0 || l1 + 3L * l2 > l3 + 3L * l4 ? '\253' : '\252';
			final int k1 = this.code.curPc();
			this.code.emitop(c);
			this.code.align(4);
			final int i2 = this.code.curPc();
			this.code.emit4(-1);
			int[] ai1 = null;
			if (c == '\252') {
				this.code.emit4(i);
				this.code.emit4(j);
				for (int j2 = i; j2 <= j; j2++) {
					this.code.emit4(-1);
				}

			} else {
				this.code.emit4(k);
				for (int k2 = 0; k2 < k; k2++) {
					this.code.emit4(-1);
					this.code.emit4(-1);
				}

				ai1 = new int[ai.length];
			}
			this.code.markDead();
			list1 = list;
			for (int i3 = 0; i3 < ai.length; i3++) {
				final Case case1 = (Case) list1.head;
				list1 = list1.tail;
				final int i4 = this.code.entryPoint();
				if (i3 != l) {
					if (c == '\252') {
						this.code.put4(i2 + 4 * (ai[i3] - i + 3), i4 - k1);
					} else {
						ai1[i3] = i4 - k1;
					}
				} else {
					this.code.put4(i2, i4 - k1);
				}
				this.genStats(case1.stats, env1);
				if (this.switchCheck && this.code.isAlive() && case1.stats.nonEmpty() && i3 < ai.length - 1) {
					this.log.warning(((Tree) case1).pos, "possible.fall-through.into.case");
				}
			}

			this.code.resolve(((GenContext) env1.info).exit);
			if (this.code.get4(i2) == -1) {
				this.code.put4(i2, this.code.entryPoint() - k1);
			}
			if (c == '\252') {
				final int j3 = this.code.get4(i2);
				for (int j4 = i; j4 <= j; j4++) {
					if (this.code.get4(i2 + 4 * (j4 - i + 3)) == -1) {
						this.code.put4(i2 + 4 * (j4 - i + 3), j3);
					}
				}

			} else {
				if (l >= 0) {
					for (int k3 = l; k3 < ai.length - 1; k3++) {
						ai[k3] = ai[k3 + 1];
						ai1[k3] = ai1[k3 + 1];
					}

				}
				if (k > 0) {
					qsort2(ai, ai1, 0, k - 1);
				}
				for (int k4 = 0; k4 < k; k4++) {
					final int i5 = i2 + 8 * (k4 + 1);
					this.code.put4(i5, ai[k4]);
					this.code.put4(i5 + 4, ai1[k4]);
				}

			}
		}
	}

	private static void qsort2(final int ai[], final int ai1[], final int i, final int j) {
		int k = i;
		int l = j;
		final int i1 = ai[(k + l) / 2];
		do {
			while (ai[k] < i1) {
				k++;
			}
			for (; i1 < ai[l]; l--) {
			}
			if (k <= l) {
				final int j1 = ai[k];
				ai[k] = ai[l];
				ai[l] = j1;
				final int k1 = ai1[k];
				ai1[k] = ai1[l];
				ai1[l] = k1;
				k++;
				l--;
			}
		} while (k <= l);
		if (i < l) {
			qsort2(ai, ai1, i, l);
		}
		if (k < j) {
			qsort2(ai, ai1, k, j);
		}
	}

	public void _case(final Synchronized synchronized1) {
		final LocalItem lockVar = this.makeTemp(this.syms.objectType);
		this.genExpr(synchronized1.lock, synchronized1.lock.type).load();
		lockVar.store();
		lockVar.load();
		this.code.emitop(194);
		final Env env1 = this.env.dup(synchronized1, new GenContext());
		((GenContext) env1.info).finalize = new GenFinalizer() {

			void gen() {
				lockVar.load();
				Gen.this.code.emitop(195);
			}

		};
		this.genTry(synchronized1.body, Catch.emptyList, env1);
		this.code.resolve(((GenContext) env1.info).exit);
	}

	public void _case(final Try tree) {
		final Env env1 = this.env.dup(tree, new GenContext());
		if (tree.finalizer != null) {
			((GenContext) env1.info).finalize = new GenFinalizer() {

				void gen() {
					Gen.this.genStat(tree.finalizer, Gen.this.env);
				}

			};
		}
		this.genTry(tree.body, tree.catchers, env1);
		this.code.resolve(((GenContext) env1.info).exit);
	}

	private void genTry(final Tree tree, final List list, final Env env1) {
		final int i = this.code.nextadr;
		final int j = this.code.curPc();
		this.genStat(tree, env1);
		final int k = this.code.curPc();
		this.code.statBegin(TreeInfo.endPos(tree));
		this.callFinalizer(env1);
		Chain chain = this.code.branch(167);
		if (j != k) {
			for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
				this.code.entryPoint(1);
				this.genCatch((Catch) list1.head, env1, j, k);
				this.callFinalizer(env1);
				chain = Code.mergeChains(chain, this.code.branch(167));
			}

		}
		if (((GenContext) env1.info).finalize != null) {
			this.code.newRegSegment();
			final int l = this.code.entryPoint(1);
			this.registerCatch(tree.pos, j, l, l, 0);
			this.code.statBegin(TreeInfo.finalizerPos(env1.tree));
			this.code.markStatBegin();
			final LocalItem localitem = this.makeTemp(this.syms.throwableType);
			localitem.store();
			this.callFinalizer(env1);
			localitem.load();
			this.code.emitop(191);
			if (((GenContext) env1.info).cont != null) {
				this.code.entryPoint(1);
				this.code.resolve(((GenContext) env1.info).cont);
				this.code.statBegin(TreeInfo.finalizerPos(env1.tree));
				this.code.markStatBegin();
				final LocalItem localitem1 = this.makeTemp(this.syms.throwableType);
				localitem1.store();
				((GenContext) env1.info).finalize.gen();
				this.code.emitop1w(169, this.code.regOf(localitem1.adr));
				this.code.markDead();
			}
		}
		this.code.resolve(chain);
		this.code.endScopes(i);
	}

	private void genCatch(final Catch catch1, final Env env1, final int i, final int j) {
		if (i != j) {
			this.registerCatch(((Tree) catch1).pos, i, j, this.code.curPc(), this.makeRef(((Tree) catch1).pos, ((Tree) catch1.param).type));
			final VarSymbol varsymbol = catch1.param.sym;
			this.code.statBegin(TreeInfo.firstStatPos(catch1.body));
			this.code.markStatBegin();
			final int k = this.code.newLocal(varsymbol);
			this.items.makeLocalItem(varsymbol).store();
			this.code.setDefined(k);
			this.genStat(catch1.body, env1);
			this.code.statBegin(TreeInfo.endPos(catch1.body));
		}
	}

	private void registerCatch(final int i, final int j, final int k, final int l, final int i1) {
		if (j != k) {
			final char c = (char) j;
			final char c1 = (char) k;
			final char c2 = (char) l;
			if (c == j && c1 == k && c2 == l) {
				this.code.addCatch(c, c1, c2, (char) i1);
			} else {
				this.log.error(i, "compiler.err.limit.code.too.large.for.try.stmt");
				this.nerrs++;
			}
		}
	}

	public void _case(final Conditional conditional) {
		Chain chain = null;
		final CondItem conditem = this.genExpr(conditional.cond, Type.booleanType).mkCond();
		final Chain chain1 = conditem.jumpFalse();
		if (conditem.trueJumps != null || conditem.opcode != 168) {
			this.code.resolve(conditem.trueJumps);
			if (((Tree) conditional).tag == 17) {
				this.genStat(conditional.thenpart, this.env);
			} else {
				this.genExpr(conditional.thenpart, this.pt).load();
			}
			chain = this.code.branch(167);
		}
		if (conditional.elsepart != null && chain1 != null) {
			this.code.resolve(chain1);
			if (((Tree) conditional).tag == 17) {
				this.genStat(conditional.elsepart, this.env);
			} else {
				this.genExpr(conditional.elsepart, this.pt).load();
			}
			this.code.resolve(chain);
		} else {
			this.code.resolve(chain);
			this.code.resolve(chain1);
		}
		if (((Tree) conditional).tag == 16) {
			this.result = this.items.makeStackItem(this.pt);
		}
	}

	public void _case(final Exec exec) {
		if (exec.expr.tag == 44) {
			exec.expr.tag = 42;
		} else if (exec.expr.tag == 45) {
			exec.expr.tag = 43;
		}
		this.genExpr(exec.expr, exec.expr.type).drop();
	}

	public void _case(final Break break1) {
		((GenContext) this.unwind(break1.target, this.env).info).addExit(this.code.branch(167));
	}

	public void _case(final Continue continue1) {
		((GenContext) this.unwind(continue1.target, this.env).info).addCont(this.code.branch(167));
	}

	public void _case(final Return return1) {
		if (return1.expr != null) {
			Object obj = this.genExpr(return1.expr, this.pt);
			if (hasFinalizers(this.env.enclMethod, this.env)) {
				((Item) obj).load();
				obj = this.makeTemp(this.pt);
				((Item) obj).store();
			}
			this.unwind(this.env.enclMethod, this.env);
			((Item) obj).load();
			this.code.emitop(172 + Code.truncate(Code.typecode(this.pt)));
		} else {
			this.unwind(this.env.enclMethod, this.env);
			this.code.emitop(177);
		}
	}

	public void _case(final Throw throw1) {
		this.genExpr(throw1.expr, throw1.expr.type).load();
		this.code.emitop(191);
	}

	public void _case(final Apply apply) {
		final Item item = this.genExpr(apply.meth, methodType);
		if (apply.args.length() != TreeInfo.symbol(apply.meth).externalType().argtypes().length()) {
			throw new InternalError("argument length mismatch: " + TreeInfo.symbol(apply.meth));
		}
		this.genArgs(apply.args, TreeInfo.symbol(apply.meth).externalType().argtypes());
		this.result = item.invoke();
	}

	public void _case(final NewClass newclass) {
		Util.assertTrue(newclass.encl == null && newclass.def == null);
		this.code.emitop2(187, this.makeRef(((Tree) newclass).pos, ((Tree) newclass).type));
		this.code.emitop(89);
		this.genArgs(newclass.args, newclass.constructor.externalType().argtypes());
		this.items.makeMemberItem(newclass.constructor, true).invoke();
		this.result = this.items.makeStackItem(((Tree) newclass).type);
	}

	public void _case(final NewArray newarray) {
		if (newarray.elems != null) {
			final Type type = ((Tree) newarray).type.elemtype();
			this.loadIntConst(newarray.elems.length());
			final Item item = this.makeNewArray(((Tree) newarray).pos, ((Tree) newarray).type, 1);
			int i = 0;
			for (List list1 = newarray.elems; list1.nonEmpty(); list1 = list1.tail) {
				item.duplicate();
				this.loadIntConst(i);
				i++;
				this.genExpr((Tree) list1.head, type).load();
				this.items.makeIndexedItem(type).store();
			}

			this.result = item;
		} else {
			for (List list = newarray.dims; list.nonEmpty(); list = list.tail) {
				this.genExpr((Tree) list.head, Type.intType).load();
			}

			this.result = this.makeNewArray(((Tree) newarray).pos, ((Tree) newarray).type, newarray.dims.length());
		}
	}

	private Item makeNewArray(final int i, final Type type, final int j) {
		final Type type1 = type.elemtype();
		if (type1.dimensions() + j > 255) {
			this.log.error(i, "limit.dimensions");
			this.nerrs++;
		}
		final int k = Code.arraycode(type1);
		if (k == 0 || k == 1 && j == 1) {
			this.code.emitop2(189, this.makeRef(i, type1));
		} else if (k == 1) {
			this.code.emitop(197, 1 - j);
			this.code.emit2(this.makeRef(i, type));
			this.code.emit1(j);
		} else {
			this.code.emitop1(188, k);
		}
		return this.items.makeStackItem(type);
	}

	public void _case(final Assign assign) {
		final Item item = this.genExpr(assign.lhs, assign.lhs.type);
		this.genExpr(assign.rhs, assign.lhs.type).load();
		this.result = this.items.makeAssignItem(item);
	}

	public void _case(final Assignop assignop) {
		final OperatorSymbol operatorsymbol = (OperatorSymbol) assignop.operator;
		final Item item;
		if (operatorsymbol.opcode == 256) {
			this.makeStringBuffer(((Tree) assignop).pos);
			item = this.genExpr(assignop.lhs, assignop.lhs.type);
			if (item.width() > 0) {
				this.code.emitop(90 + 3 * (item.width() - 1));
			}
			item.load();
			this.appendString(assignop.lhs);
			this.appendStrings(assignop.rhs);
			this.bufferToString(((Tree) assignop).pos);
		} else {
			item = this.genExpr(assignop.lhs, assignop.lhs.type);
			if ((((Tree) assignop).tag == 77 || ((Tree) assignop).tag == 78) && item instanceof LocalItem && assignop.lhs.type.tag <= 4 && assignop.rhs.type.tag <= 4 && assignop.rhs.type.constValue != null) {
				int i = ((Number) assignop.rhs.type.constValue).intValue();
				if (((Tree) assignop).tag == 78) {
					i = -i;
				}
				if (i >= -128 && i <= 127) {
					((LocalItem) item).incr(i);
					this.result = item;
					return;
				}
			}
			item.duplicate();
			item.coerce((Type) ((Symbol) operatorsymbol).type.argtypes().head).load();
			this.completeBinop(assignop.lhs, assignop.rhs, operatorsymbol).coerce(assignop.lhs.type);
		}
		this.result = this.items.makeAssignItem(item);
	}

	public void _case(final Operation operation) {
		final OperatorSymbol operatorsymbol = (OperatorSymbol) operation.operator;
		if (operatorsymbol.opcode == 256) {
			this.makeStringBuffer(((Tree) operation).pos);
			this.appendStrings(operation);
			this.bufferToString(((Tree) operation).pos);
			this.result = this.items.makeStackItem(this.syms.stringType);
		} else {
			final Item item = this.genExpr((Tree) operation.args.head, (Type) ((Symbol) operatorsymbol).type.argtypes().head);
			switch (((Tree) operation).tag) {
			case 38: // '&'
				this.result = item.load();
				break;

			case 39: // '\''
				this.result = item.load();
				this.code.emitop(operatorsymbol.opcode);
				break;

			case 40: // '('
				this.result = item.mkCond().negate();
				break;

			case 41: // ')'
				this.result = item.load();
				this.emitMinusOne(item.typecode);
				this.code.emitop(operatorsymbol.opcode);
				break;

			case 42: // '*'
			case 43: // '+'
				item.duplicate();
				if (item instanceof LocalItem && (operatorsymbol.opcode == 96 || operatorsymbol.opcode == 100)) {
					((LocalItem) item).incr(((Tree) operation).tag != 42 ? -1 : 1);
					this.result = item;
					break;
				}
				item.load();
				this.code.emitop(one(item.typecode));
				this.code.emitop(operatorsymbol.opcode);
				if (item.typecode != 0 && Code.truncate(item.typecode) == 0) {
					this.code.emitop(145 + item.typecode - 5);
				}
				this.result = this.items.makeAssignItem(item);
				break;

			case 44: // ','
			case 45: // '-'
				item.duplicate();
				if (item instanceof LocalItem && operatorsymbol.opcode == 96) {
					final Item item1 = item.load();
					((LocalItem) item).incr(((Tree) operation).tag != 44 ? -1 : 1);
					this.result = item1;
					break;
				}
				final Item item2 = item.load();
				item.stash(item.typecode);
				this.code.emitop(one(item.typecode));
				this.code.emitop(operatorsymbol.opcode);
				if (item.typecode != 0 && Code.truncate(item.typecode) == 0) {
					this.code.emitop(145 + item.typecode - 5);
				}
				item.store();
				this.result = item2;
				break;

			case 46: // '.'
				final CondItem conditem = item.mkCond();
				if (conditem.falseJumps != null || conditem.opcode != 167) {
					final Chain chain = conditem.jumpTrue();
					this.code.resolve(conditem.falseJumps);
					final CondItem conditem2 = this.genExpr((Tree) operation.args.tail.head, ((Tree) operation.args.tail.head).type).mkCond();
					this.result = this.items.makeCondItem(conditem2.opcode, Code.mergeChains(chain, conditem2.trueJumps), conditem2.falseJumps);
				} else {
					this.result = conditem;
				}
				break;

			case 47: // '/'
				final CondItem conditem1 = item.mkCond();
				if (conditem1.trueJumps != null || conditem1.opcode != 168) {
					final Chain chain1 = conditem1.jumpFalse();
					this.code.resolve(conditem1.trueJumps);
					final CondItem conditem3 = this.genExpr((Tree) operation.args.tail.head, ((Tree) operation.args.tail.head).type).mkCond();
					this.result = this.items.makeCondItem(conditem3.opcode, conditem3.trueJumps, Code.mergeChains(chain1, conditem3.falseJumps));
				} else {
					this.result = conditem1;
				}
				break;

			default:
				item.load();
				this.result = this.completeBinop((Tree) operation.args.head, (Tree) operation.args.tail.head, operatorsymbol);
				break;
			}
		}
	}

	private void makeStringBuffer(final int i) {
		this.code.emitop2(187, this.makeRef(i, this.syms.stringBufferType));
		this.code.emitop(89);
		this.callMethod(i, this.syms.stringBufferType, Names.init, Type.emptyList, false);
	}

	private void appendString(final Tree tree) {
		Type type = tree.type;
		if (type.tag > 8 && type.tsym != this.syms.stringType.tsym) {
			type = this.syms.objectType;
		}
		this.callMethod(tree.pos, this.syms.stringBufferType, Names.append, Type.emptyList.prepend(type), false);
	}

	private void appendStrings(final Tree tree) {
		if (tree.tag == 60 && tree.type.constValue == null) {
			final Operation operation = (Operation) tree;
			if (operation.operator.kind == 16 && ((OperatorSymbol) operation.operator).opcode == 256) {
				this.appendStrings((Tree) operation.args.head);
				this.appendStrings((Tree) operation.args.tail.head);
				return;
			}
		}
		this.genExpr(tree, tree.type).load();
		this.appendString(tree);
	}

	private void bufferToString(final int i) {
		this.callMethod(i, this.syms.stringBufferType, Names.toString, Type.emptyList, false);
	}

	private Item completeBinop(final Tree tree, final Tree tree1, final OperatorSymbol operatorsymbol) {
		final MethodType methodtype = (MethodType) ((Symbol) operatorsymbol).type;
		int i = operatorsymbol.opcode;
		if (i >= 159 && i <= 164 && tree1.type.constValue instanceof Number && ((Number) tree1.type.constValue).intValue() == 0) {
			i -= 6;
		} else if (i >= 165 && i <= 166 && TreeInfo.symbol(tree1) == this.syms.nullConst) {
			i += 33;
		} else {
			Type type = (Type) operatorsymbol.erasure().argtypes().tail.head;
			if (i >= 270 && i <= 275) {
				i -= 150;
				type = Type.intType;
			}
			this.genExpr(tree1, type).load();
			if (i >= 512) {
				this.code.emitop(i >> 9);
				i &= 0xff;
			}
		}
		if (i >= 153 && i <= 166 || i == 198 || i == 199) {
			return this.items.makeCondItem(i);
		}
		this.code.emitop(i);
		return this.items.makeStackItem(methodtype.restype);
	}

	public void _case(final TypeCast typecast) {
		this.result = this.genExpr(typecast.expr, typecast.clazz.type).load();
		if (typecast.clazz.type.tag > 8 && typecast.expr.type.asSuper(typecast.clazz.type.tsym) == null) {
			this.code.emitop2(192, this.makeRef(((Tree) typecast).pos, typecast.clazz.type));
		}
	}

	public void _case(final TypeTest typetest) {
		this.genExpr(typetest.expr, typetest.expr.type).load();
		this.code.emitop2(193, this.makeRef(((Tree) typetest).pos, typetest.clazz.type));
		this.result = this.items.makeStackItem(Type.booleanType);
	}

	public void _case(final Indexed indexed) {
		this.genExpr(indexed.indexed, indexed.indexed.type).load();
		this.genExpr(indexed.index, Type.intType).load();
		this.result = this.items.makeIndexedItem(((Tree) indexed).type);
	}

	public void _case(final Ident ident) {
		Symbol symbol = ident.sym;
		if (ident.name == Names._this || ident.name == Names._super) {
			Item item = ident.name != Names._this ? this.items.makeSuperItem() : this.items.makeThisItem();
			if (symbol.kind == 16) {
				item.load();
				item = this.items.makeMemberItem(symbol, true);
			}
			this.result = item;
		} else if (ident.name == Names._null) {
			this.code.emitop(1);
			this.result = this.items.makeStackItem(((Tree) ident).type);
		} else if (symbol.kind == 4 && symbol.owner.kind == 16) {
			this.result = this.items.makeLocalItem((VarSymbol) symbol);
		} else if ((symbol.flags() & 8) != 0) {
			this.result = this.items.makeStaticItem(symbol);
		} else {
			this.items.makeThisItem().load();
			symbol = this.makeAccessible(symbol, ((Tree) this.env.enclClass).type);
			this.result = this.items.makeMemberItem(symbol, (symbol.flags() & 2) != 0);
		}
	}

	public void _case(final Select select) {
		Symbol symbol = select.sym;
		if (select.name == Names._class) {
			throw new InternalError();
		}
		final Symbol symbol1 = TreeInfo.symbol(select.selected);
		final boolean flag = symbol1 != null && (symbol1.kind == 2 || symbol1.name == Names._super);
		final boolean flag1 = isOddAccessName(this.env.enclMethod.name);
		final Item item = flag ? this.items.makeSuperItem() : this.genExpr(select.selected, select.selected.type);
		if (symbol.kind == 4 && ((VarSymbol) symbol).constValue != null) {
			item.drop();
			this.result = this.items.makeImmediateItem(symbol.type, ((VarSymbol) symbol).constValue);
		} else {
			symbol = this.makeAccessible(symbol, select.selected.type);
			if ((symbol.flags() & 8) != 0) {
				item.drop();
				this.result = this.items.makeStaticItem(symbol);
			} else {
				item.load();
				if (symbol == this.syms.lengthVar) {
					this.code.emitop(190);
					this.result = this.items.makeStackItem(Type.intType);
				} else {
					this.result = this.items.makeMemberItem(symbol, (symbol.flags() & 2) != 0 || flag || flag1);
				}
			}
		}
	}

	private static boolean isOddAccessName(final Name name) {
		return name.startsWith(Names.accessDollar) && (name.byteAt(name.len - 1) & 1) == 1;
	}

	public void _case(final Literal literal) {
		this.result = this.items.makeImmediateItem(((Tree) literal).type, literal.value);
	}

	public boolean genClass(final Env env1, final ClassDef classdef) {
		try {
			this.attrEnv = env1;
			final ClassSymbol classsymbol = classdef.sym;
			classdef.defs = this.normalizeDefs(classdef.defs, classsymbol);
			classsymbol.pool = this.pool;
			this.pool.reset();
			final Env env2 = new Env(classdef, new GenContext());
			env2.toplevel = env1.toplevel;
			env2.enclClass = classdef;
			for (List list = classdef.defs; list.nonEmpty(); list = list.tail) {
				this.genDef((Tree) list.head, env2);
			}

			if (this.pool.numEntries() > Pool.MAX_ENTRIES) {
				this.log.error(((Tree) classdef).pos, "limit.pool");
				this.nerrs++;
			}
			if (this.nerrs != 0) {
				for (List list1 = classdef.defs; list1.nonEmpty(); list1 = list1.tail) {
					if (((Tree) list1.head).tag == 4) {
						((MethodDef) list1.head).sym.code = null;
					}
				}

			}
			return this.nerrs == 0;
		} finally {
			this.nerrs = 0;
		}
	}

	private final Log log;
	private final Symtab syms;
	private final Check chk;
	private final Resolve rs;
	private final TreeMaker make;
	private final boolean switchCheck;
	private final boolean lineDebugInfo;
	private final boolean varDebugInfo;
	private final Pool pool;
	Code code;
	private Items items;
	private Env attrEnv;
	private int nerrs;
	private static final short MAJOR_VERSION_1_2 = 46;
	Env env;
	private Type pt;
	private Item result;
	private static final Type methodType = new MethodType(null, null, null);

}
