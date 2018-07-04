package com.sun.tools.javac.v8.comp;

import com.sun.tools.javac.v8.code.Flags;
import com.sun.tools.javac.v8.code.Scope;
import com.sun.tools.javac.v8.code.Scope.Entry;
import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.CompletionFailure;
import com.sun.tools.javac.v8.code.Symbol.MethodSymbol;
import com.sun.tools.javac.v8.code.Symbol.OperatorSymbol;
import com.sun.tools.javac.v8.code.Symbol.VarSymbol;
import com.sun.tools.javac.v8.code.Type;
import com.sun.tools.javac.v8.code.Type.ArrayType;
import com.sun.tools.javac.v8.code.Type.ClassType;
import com.sun.tools.javac.v8.code.Type.MethodType;
import com.sun.tools.javac.v8.comp.Resolve.StaticError;
import com.sun.tools.javac.v8.tree.Pretty;
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
import com.sun.tools.javac.v8.tree.Tree.Erroneous;
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
import com.sun.tools.javac.v8.tree.Tree.TypeArray;
import com.sun.tools.javac.v8.tree.Tree.TypeCast;
import com.sun.tools.javac.v8.tree.Tree.TypeIdent;
import com.sun.tools.javac.v8.tree.Tree.TypeTest;
import com.sun.tools.javac.v8.tree.Tree.VarDef;
import com.sun.tools.javac.v8.tree.Tree.Visitor;
import com.sun.tools.javac.v8.tree.Tree.WhileLoop;
import com.sun.tools.javac.v8.tree.TreeInfo;
import com.sun.tools.javac.v8.tree.TreeMaker;
import com.sun.tools.javac.v8.util.Hashtable;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.ListBuffer;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;
import com.sun.tools.javac.v8.util.Set;

public final class Attr extends Visitor {

	public Attr(final Log log1, final Symtab symtab, final Resolve resolve, final Check check1, final TreeMaker treemaker, final Enter enter1, final Hashtable hashtable) {
		this.miranda = true;
		this.anyMethodType = new MethodType(null, null, null);
		this.methTemplateSupply = new ListBuffer();
		this.log = log1;
		this.syms = symtab;
		this.rs = resolve;
		this.chk = check1;
		this.make = treemaker;
		this.enter = enter1;
		if (enter1 != null) {
			enter1.attr = this;
		}
		this.cfolder = new ConstFold(log1, symtab);
		this.retrofit = hashtable.get("-retrofit") != null;
	}

	private Type check(final Tree tree, Type type, final int i, final int j, final Type type1) {
		if (type.tag != 18 && type1.tag != 12) {
			if ((i & ~j) == 0) {
				if (type1.tag != 17) {
					type = this.chk.checkType(tree.pos, type, type1);
				}
			} else {
				this.log.error(tree.pos, "unexpected.type", Resolve.kindNames(j), Resolve.kindName(i));
				type = Type.errType;
			}
		}
		tree.type = type;
		return type;
	}

	private static boolean isAssignableAsBlankFinal(final Symbol varsymbol, final Env env1) {
		final Symbol symbol = ((AttrContext) env1.info).scope.owner;
		return varsymbol.owner == symbol || (symbol.name == Names.init || symbol.kind == 4 || (symbol.flags() & 0x100000) != 0) && varsymbol.owner == symbol.owner && (varsymbol.flags() & 8) != 0 == Resolve.isStatic(env1);
	}

	private void checkAssignable(final int i, final Symbol varsymbol, final Tree tree, final Env env1) {
		if ((varsymbol.flags() & 0x10) != 0 && ((varsymbol.flags() & 0x40000) != 0 || tree != null && (tree.tag != 31 || TreeInfo.name(tree) != Names._this) || !isAssignableAsBlankFinal(varsymbol, env1))) {
			this.log.error(i, "cant.assign.val.to.final.var", varsymbol.toJava());
		}
	}

	private static boolean isStaticReference(final Tree tree) {
		if (tree.tag == 30) {
			final Symbol symbol = TreeInfo.symbol(((Select) tree).selected);
			if (symbol == null || symbol.kind != 2) {
				return false;
			}
		}
		return true;
	}

	private Symbol thisSym(final Env env1) {
		return this.rs.resolveSelf(0, env1, env1.enclClass.sym, Names._this, true);
	}

	private void addAbstractMethod(final ClassDef classdef, final Symbol methodsymbol, final Env env1) {
		final MethodSymbol methodsymbol1 = new MethodSymbol(methodsymbol.flags() | 0x200000, methodsymbol.name, ((Symbol) classdef.sym).type.memberType(methodsymbol), classdef.sym);
		final MethodDef methoddef = this.make.at(((Tree) classdef).pos).MethodDef(methodsymbol1, null);
		classdef.defs = classdef.defs.prepend(methoddef);
		this.enter.phase2.memberEnter(methoddef, env1);
	}

	private void implementInterfaceMethods(final ClassSymbol classsymbol, final Env env1) {
		final ClassDef classdef = (ClassDef) env1.tree;
		for (List list = ((Symbol) classsymbol).type.interfaces(); list.nonEmpty(); list = list.tail) {
			final ClassSymbol classsymbol1 = (ClassSymbol) ((Type) list.head).tsym;
			for (Entry entry = classsymbol1.members().elems; entry != null; entry = entry.sibling) {
				if (entry.sym.kind == 16 && (entry.sym.flags() & 8) == 0) {
					final MethodSymbol methodsymbol = (MethodSymbol) entry.sym;
					final MethodSymbol methodsymbol1 = methodsymbol.implementation(classdef.sym);
					if (methodsymbol1 == null) {
						this.addAbstractMethod(classdef, methodsymbol, env1);
					}
				}
			}

			this.implementInterfaceMethods(classsymbol1, env1);
		}

	}

	Type attribTree(final Tree tree, final Env env1, final int i, final Type type) {
		final Env env2 = this.env;
		final int j = this.pkind;
		final Type type1 = this.pt;
		try {
			this.env = env1;
			this.pkind = i;
			this.pt = type;
			tree.visit(this);
			this.env = env2;
			this.pkind = j;
			this.pt = type1;
			return this.result;
		} catch (final CompletionFailure completionfailure) {
			this.env = env2;
			this.pkind = j;
			this.pt = type1;
			return this.chk.completionError(tree.pos, completionfailure);
		}
	}

	private Type attribExpr(final Tree tree, final Env env1, final Type type) {
		return this.attribTree(tree, env1, 12, type);
	}

	private Type attribExpr(final Tree tree, final Env env1) {
		return this.attribTree(tree, env1, 12, Type.noType);
	}

	Type attribType(final Tree tree, final Env env1) {
		return this.attribTree(tree, env1, 2, Type.noType);
	}

	private Type attribStat(final Tree tree, final Env env1) {
		return this.attribTree(tree, env1, 0, Type.noType);
	}

	private void attribExprs(final List list, final Env env1, final Type type) {
		final ListBuffer listbuffer = new ListBuffer();
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			listbuffer.append(this.attribExpr((Tree) list1.head, env1, type));
		}

		listbuffer.toList();
	}

	void attribStats(final List list, final Env env1) {
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			this.attribStat((Tree) list1.head, env1);
		}

	}

	private List attribArgs(final List list, final Env env1) {
		final ListBuffer listbuffer = new ListBuffer();
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			listbuffer.append(this.chk.checkNonVoid(((Tree) list1.head).pos, this.attribTree((Tree) list1.head, env1, 12, Type.noType)));
		}

		return listbuffer.toList();
	}

	Type attribBase(final Tree tree, final Env env1, final int i) {
		final Type type = this.chk.checkClassType(tree.pos, this.attribType(tree, env1));
		if ((type.tsym.flags() & 0x200) != i) {
			if (i == 0) {
				this.log.error(tree.pos, "no.intf.expected.here");
			} else {
				this.log.error(tree.pos, "intf.expected.here");
			}
			return Type.errType;
		}
		if ((type.tsym.flags() & 0x10) != 0) {
			this.log.error(tree.pos, "cant.inherit.from.final", type.tsym.toJava());
		}
		return type;
	}

	public void _case(final ClassDef classdef) {
		if ((((AttrContext) this.env.info).scope.owner.kind & 0x14) != 0) {
			this.enter.classEnter(classdef, this.env);
		}
		final ClassSymbol classsymbol = classdef.sym;
		if (classsymbol == null) {
			this.result = null;
		} else {
			classsymbol.complete();
			if (((AttrContext) this.env.info).isSelfCall && this.env.tree.tag == 24 && ((NewClass) this.env.tree).encl == null) {
				classsymbol.flags_field |= 0x400000;
			}
			this.attribClass(classsymbol);
			this.result = classdef.type = ((Symbol) classsymbol).type;
		}
	}

	public void _case(final MethodDef methoddef) {
		final MethodSymbol methodsymbol = methoddef.sym;
		this.chk.checkOverride(((Tree) methoddef).pos, methodsymbol);
		final Env env1 = Enter.methodEnv(methoddef, this.env);
		for (List list = methoddef.params; list.nonEmpty(); list = list.tail) {
			this.attribStat((Tree) list.head, env1);
		}

		this.chk.validate(methoddef.restype);
		for (List list1 = methoddef.thrown; list1.nonEmpty(); list1 = list1.tail) {
			this.chk.checkType(((Tree) list1.head).pos, ((Tree) list1.head).type, this.syms.throwableType);
		}

		final ClassSymbol classsymbol = this.env.enclClass.sym;
		if (methoddef.body == null) {
			if ((classsymbol.flags() & 0x200) == 0 && (methoddef.flags & 0x500) == 0 && !this.retrofit) {
				this.log.error(((Tree) methoddef).pos, "missing.meth.body.or.decl.abstract");
			}
		} else if ((classsymbol.flags() & 0x200) != 0) {
			this.log.error(((Tree) methoddef).pos, "intf.meth.cant.have.body");
		} else if ((methoddef.flags & 0x400) != 0) {
			this.log.error(((Tree) methoddef).pos, "abstract.meth.cant.have.body");
		} else if ((methoddef.flags & 0x100) != 0) {
			this.log.error(((Tree) methoddef).pos, "native.meth.cant.have.body");
		} else {
			if (methoddef.name == Names.init && ((Symbol) classsymbol).type != this.syms.objectType) {
				final Block block = methoddef.body;
				if (block.stats.isEmpty() || !TreeInfo.isSelfCall((Tree) block.stats.head)) {
					block.stats = block.stats.prepend(Enter.SuperCall(this.make.at(((Tree) block).pos), VarDef.emptyList, false));
				}
			}
			this.attribStat(methoddef.body, env1);
		}
		((AttrContext) env1.info).scope.leave();
		this.result = methoddef.type = ((Symbol) methodsymbol).type;
	}

	public void _case(final VarDef vardef) {
		if (((AttrContext) this.env.info).scope.owner.kind == 16) {
			this.enter.phase2.memberEnter(vardef, this.env);
		}
		this.chk.validate(vardef.vartype);
		final VarSymbol varsymbol = vardef.sym;
		if (vardef.init != null) {
			varsymbol.pos = 0x7fffffff;
			if ((((Symbol) varsymbol).flags_field & 0x10) != 0) {
				this.evalInit(varsymbol);
			} else {
				this.attribExpr(vardef.init, Enter.initEnv(vardef, this.env), ((Symbol) varsymbol).type);
			}
			varsymbol.pos = ((Tree) vardef).pos;
		}
		this.result = vardef.type = ((Symbol) varsymbol).type;
	}

	public void _case(final Block block) {
		final Env env1 = this.env.dup(block, ((AttrContext) this.env.info).dup(((AttrContext) this.env.info).scope.dup()));
		if (((AttrContext) this.env.info).scope.owner.kind == 2) {
			((AttrContext) env1.info).scope.owner = new MethodSymbol(block.flags | 0x100000, Names.empty, null, ((AttrContext) this.env.info).scope.owner);
			if ((block.flags & 8) != 0) {
				((AttrContext) env1.info).staticLevel++;
			}
		}
		this.attribStats(block.stats, env1);
		((AttrContext) env1.info).scope.leave();
		this.result = null;
	}

	public void _case(final DoLoop doloop) {
		this.attribStat(doloop.body, this.env.dup(doloop));
		this.attribExpr(doloop.cond, this.env, Type.booleanType);
		this.result = null;
	}

	public void _case(final WhileLoop whileloop) {
		this.attribExpr(whileloop.cond, this.env, Type.booleanType);
		this.attribStat(whileloop.body, this.env.dup(whileloop));
		this.result = null;
	}

	public void _case(final ForLoop forloop) {
		final Env env1 = this.env.dup(this.env.tree, ((AttrContext) this.env.info).dup(((AttrContext) this.env.info).scope.dup()));
		this.attribStats(forloop.init, env1);
		if (forloop.cond != null) {
			this.attribExpr(forloop.cond, env1, Type.booleanType);
		}
		env1.tree = forloop;
		this.attribStats(forloop.step, env1);
		this.attribStat(forloop.body, env1);
		((AttrContext) env1.info).scope.leave();
		this.result = null;
	}

	public void _case(final Labelled labelled) {
		for (Env env1 = this.env; env1 != null; env1 = env1.next) {
			if (env1.tree.tag != 10 || ((Labelled) env1.tree).label != labelled.label) {
				continue;
			}
			this.log.error(((Tree) labelled).pos, "label.already.in.use", labelled.label.toJava());
			break;
		}

		this.attribStat(labelled.body, this.env.dup(labelled));
		this.result = null;
	}

	public void _case(final Switch switch1) {
		final Type type = this.attribExpr(switch1.selector, this.env, Type.intType);
		final Env env1 = this.env.dup(switch1, ((AttrContext) this.env.info).dup(((AttrContext) this.env.info).scope.dup()));
		final Set set = Set.make();
		boolean flag = false;
		for (List list = switch1.cases; list.nonEmpty(); list = list.tail) {
			final Case case1 = (Case) list.head;
			if (case1.pat != null) {
				final Type type1 = this.attribExpr(case1.pat, env1, Type.intType);
				if (type1.tag != 18) {
					if (type1.constValue == null) {
						this.log.error(case1.pat.pos, "const.expr.req");
					} else if (set.contains(type1.constValue)) {
						this.log.error(((Tree) case1).pos, "duplicate.case.label");
					} else {
						this.chk.checkType(case1.pat.pos, type1, type);
						set.add(type1.constValue);
					}
				}
			} else if (flag) {
				this.log.error(((Tree) case1).pos, "duplicate.default.label");
			} else {
				flag = true;
			}
			this.attribStats(case1.stats, env1);
		}

		((AttrContext) env1.info).scope.leave();
		this.result = null;
	}

	public void _case(final Synchronized synchronized1) {
		this.chk.checkRefType(((Tree) synchronized1).pos, this.attribExpr(synchronized1.lock, this.env, this.syms.objectType));
		this.attribStat(synchronized1.body, this.env);
		this.result = null;
	}

	public void _case(final Try try1) {
		this.attribStat(try1.body, this.env.dup(try1, ((AttrContext) this.env.info).dup()));
		for (List list = try1.catchers; list.nonEmpty(); list = list.tail) {
			final Catch catch1 = (Catch) list.head;
			final Env env1 = this.env.dup(catch1, ((AttrContext) this.env.info).dup(((AttrContext) this.env.info).scope.dup()));
			final Type type = this.attribStat(catch1.param, env1);
			this.chk.checkType(catch1.param.vartype.pos, type, this.syms.throwableType);
			this.attribStat(catch1.body, env1);
			((AttrContext) env1.info).scope.leave();
		}

		if (try1.finalizer != null) {
			this.attribStat(try1.finalizer, this.env);
		}
		this.result = null;
	}

	public void _case(final Conditional conditional) {
		this.attribExpr(conditional.cond, this.env, Type.booleanType);
		this.attribExpr(conditional.thenpart, this.env, this.pt);
		if (conditional.elsepart != null) {
			this.attribExpr(conditional.elsepart, this.env, this.pt);
		}
		this.result = ((Tree) conditional).tag == 17 ? null : this.check(conditional, this.condType(((Tree) conditional).pos, conditional.cond.type, conditional.thenpart.type, conditional.elsepart.type), 12, this.pkind, this.pt);
	}

	private Type condType(final int i, final Type type, final Type type1, final Type type2) {
		final Type type3 = this.condType1(i, type, type1, type2);
		if (type.constValue != null && type1.constValue != null && type2.constValue != null) {
			return ((Number) type.constValue).intValue() == 0 ? type2 : type1;
		}
		return type3;
	}

	private Type condType1(final int i, final Type type, final Type type1, final Type type2) {
		if (type1.tag < 4 && type2.tag == 4 && type2.isAssignable(type1)) {
			return type1.baseType();
		}
		if (type2.tag < 4 && type1.tag == 4 && type1.isAssignable(type2)) {
			return type2.baseType();
		}
		if (type1.tag <= 7 && type2.tag <= 7) {
			for (int j = 1; j <= 7; j++) {
				final Type type3 = Type.typeOfTag[j];
				if (type1.isSubType(type3) && type2.isSubType(type3)) {
					return type3;
				}
			}

		}
		if (type1.tsym == this.syms.stringType.tsym && type2.tsym == this.syms.stringType.tsym) {
			return this.syms.stringType;
		}
		if (type1.isSubType(type2)) {
			return type2.baseType();
		}
		this.chk.checkType(i, type2, type1);
		return type1.baseType();
	}

	public void _case(final Exec exec) {
		this.attribExpr(exec.expr, this.env);
		this.result = null;
	}

	public void _case(final Break break1) {
		break1.target = this.findJumpTarget(((Tree) break1).pos, ((Tree) break1).tag, break1.label, this.env);
		this.result = null;
	}

	public void _case(final Continue continue1) {
		continue1.target = this.findJumpTarget(((Tree) continue1).pos, ((Tree) continue1).tag, continue1.label, this.env);
		this.result = null;
	}

	private Tree findJumpTarget(final int i, final int j, final Name name, final Env env1) {
		label0: for (Env env2 = env1; env2 != null; env2 = env2.next) {
			switch (env2.tree.tag) {
			case 5: // '\005'
			case 6: // '\006'
			default:
				break;

			case 4: // '\004'
				break label0;

			case 10: // '\n'
				final Labelled labelled = (Labelled) env2.tree;
				if (name == labelled.label) {
					final Tree tree = TreeInfo.referencedStatement(labelled);
					if (j == 20 && tree.tag != 7 && tree.tag != 8 && tree.tag != 9) {
						this.log.error(i, "not.loop.label", name.toJava());
					}
					return tree;
				}
				break;

			case 7: // '\007'
			case 8: // '\b'
			case 9: // '\t'
				if (name == null) {
					return env2.tree;
				}
				break;

			case 11: // '\013'
				if (name == null && j == 19) {
					return env2.tree;
				}
				break;
			}
		}

		if (name != null) {
			this.log.error(i, "undef.label", name.toJava());
		} else if (j == 20) {
			this.log.error(i, "cont.outside.loop");
		} else {
			this.log.error(i, "break.outside.switch.loop");
		}
		return null;
	}

	public void _case(final Return return1) {
		if (this.env.enclMethod == null || ((Symbol) this.env.enclMethod.sym).owner != this.env.enclClass.sym) {
			this.log.error(((Tree) return1).pos, "ret.outside.meth");
		} else {
			final MethodSymbol methodsymbol = this.env.enclMethod.sym;
			if (((Symbol) methodsymbol).type.restype().tag == 9) {
				if (return1.expr != null) {
					this.log.error(return1.expr.pos, "cant.ret.val.from.meth.decl.void");
				}
			} else if (return1.expr == null) {
				this.log.error(((Tree) return1).pos, "missing.ret.val");
			} else {
				this.attribExpr(return1.expr, this.env, ((Symbol) methodsymbol).type.restype());
			}
		}
		this.result = null;
	}

	public void _case(final Throw throw1) {
		this.attribExpr(throw1.expr, this.env, this.syms.throwableType);
		this.result = null;
	}

	public void _case(final Apply apply) {
		Env env1 = this.env;
		final Name name = TreeInfo.name(apply.meth);
		final boolean flag = name == Names._this || name == Names._super;
		if (flag) {
			if (this.checkFirstConstructorStat(apply, this.env)) {
				env1 = this.env.dup(this.env.tree, ((AttrContext) this.env.info).dup());
				((AttrContext) env1.info).isSelfCall = true;
				final List list = this.attribArgs(apply.args, env1);
				Type type2 = ((Symbol) this.env.enclClass.sym).type;
				if (name == Names._super) {
					type2 = type2.supertype();
				}
				if (type2.tag == 10) {
					if (apply.meth.tag == 30) {
						final Tree tree = ((Select) apply.meth).selected;
						final Symbol symbol = TreeInfo.symbol(tree);
						if (symbol != null && symbol.kind == 2) {
							apply.meth = this.make.at(((Tree) apply).pos).Ident(name);
						} else if (type2.outer().tag == 10) {
							this.attribExpr(tree, env1, type2.outer());
						} else {
							this.log.error(tree.pos, "illegal.qual.not.icls", type2.tsym.toJava());
						}
					}
					final boolean flag1 = ((AttrContext) env1.info).selectSuper;
					((AttrContext) env1.info).selectSuper = true;
					final Symbol symbol1 = this.rs.resolveConstructor(apply.meth.pos, env1, type2, list);
					((AttrContext) env1.info).selectSuper = flag1;
					if (symbol1 == this.env.enclMethod.sym) {
						this.log.error(((Tree) apply).pos, "recursive.ctor.invocation");
					}
					TreeInfo.setSymbol(apply.meth, symbol1);
					this.checkId(apply.meth, type2, symbol1, this.env, 16, this.anyMethodType);
				}
			}
			this.result = Type.voidType;
		} else {
			final List list1 = this.attribArgs(apply.args, env1);
			final List list2 = this.methTemplateSupply.elems;
			final Type type3 = this.newMethTemplate(list1);
			final Type type1 = this.attribExpr(apply.meth, env1, type3);
			this.methTemplateSupply.elems = list2;
			this.result = this.check(apply, type1, 12, this.pkind, this.pt);
		}
	}

	private boolean checkFirstConstructorStat(final Apply apply, final Env env1) {
		final MethodDef methoddef = env1.enclMethod;
		if (methoddef != null && methoddef.name == Names.init) {
			final Block block = methoddef.body;
			if (((Tree) block.stats.head).tag == 18 && ((Exec) block.stats.head).expr == apply) {
				return true;
			}
		}
		this.log.error(((Tree) apply).pos, "call.must.be.first.stmt.in.ctor", TreeInfo.name(apply.meth).toJava());
		return false;
	}

	private Type newMethTemplate(final List list) {
		if (this.methTemplateSupply.elems == this.methTemplateSupply.last) {
			this.methTemplateSupply.append(new MethodType(null, null, null));
		}
		final MethodType methodtype = (MethodType) this.methTemplateSupply.elems.head;
		this.methTemplateSupply.elems = this.methTemplateSupply.elems.tail;
		methodtype.argtypes = list;
		return methodtype;
	}

	public void _case(final NewClass newclass) {
		Type type = Type.errType;
		final ClassDef classdef = newclass.def;
		Type type1 = null;
		Tree obj = newclass.clazz;
		final Tree obj1 = obj;
		Tree obj2 = obj1;
		if (newclass.encl != null) {
			type1 = this.attribExpr(newclass.encl, this.env);
			if (type1.tag == 10) {
				obj2 = this.make.at(obj.pos).Select(this.make.Type(type1), ((Ident) obj1).name);
				obj = obj2;
			}
		}
		Type type2 = this.chk.checkClassType(newclass.clazz.pos, this.attribType(obj, this.env));
		this.chk.validate(obj);
		if (newclass.encl != null) {
			newclass.clazz.type = type2;
			TreeInfo.setSymbol(obj1, TreeInfo.symbol(obj2));
			obj1.type = ((Ident) obj1).sym.type;
		} else if ((type2.tsym.flags() & 0x200) == 0 && type2.outer().tag == 10) {
			this.rs.resolveSelf(((Tree) newclass).pos, this.env, type2.outer().tsym, Names._this, false);
		}
		List list = this.attribArgs(newclass.args, this.env);
		if (type2.tag == 10) {
			if (classdef == null && (type2.tsym.flags() & 0x600) != 0) {
				this.log.error(((Tree) newclass).pos, "abstract.cant.be.instantiated", type2.tsym.toJava());
			} else if (classdef != null && (type2.tsym.flags() & 0x200) != 0) {
				if (list.nonEmpty()) {
					this.log.error(((Tree) newclass).pos, "anon.class.impl.intf.no.args");
					list = Type.emptyList;
				} else if (newclass.encl != null) {
					this.log.error(((Tree) newclass).pos, "anon.class.impl.intf.no.qual.for.new");
				}
			} else {
				final boolean flag = ((AttrContext) this.env.info).selectSuper;
				if (classdef != null) {
					((AttrContext) this.env.info).selectSuper = true;
				}
				newclass.constructor = this.rs.resolveConstructor(((Tree) newclass).pos, this.env, type2, list);
				((AttrContext) this.env.info).selectSuper = flag;
			}
			if (classdef != null) {
				if (Resolve.isStatic(this.env)) {
					classdef.flags |= 8;
				}
				if ((type2.tsym.flags() & 0x200) != 0) {
					classdef.implementing = List.make(obj);
				} else {
					classdef.extending = obj;
				}
				this.attribStat(classdef, this.env.dup(newclass));
				if (newclass.encl != null) {
					newclass.args = newclass.args.prepend(newclass.encl);
					list = list.prepend(type1);
					newclass.encl = null;
				}
				type2 = ((Symbol) classdef.sym).type;
				newclass.constructor = this.rs.resolveConstructor(((Tree) newclass).pos, this.env, type2, list);
			} else if (((AttrContext) this.env.info).isSelfCall && type2.tsym.hasOuterInstance() && newclass.encl == null) {
				this.chk.earlyRefError(((Tree) newclass).pos, this.thisSym(this.env));
			}
			if (newclass.constructor != null && newclass.constructor.kind == 16) {
				type = type2;
			}
		}
		this.result = this.check(newclass, type, 12, this.pkind, this.pt);
	}

	public void _case(final NewArray newarray) {
		Object obj = Type.errType;
		final Type type;
		if (newarray.elemtype != null) {
			type = this.attribType(newarray.elemtype, this.env);
			this.chk.validate(newarray.elemtype);
			obj = type;
			for (List list = newarray.dims; list.nonEmpty(); list = list.tail) {
				this.attribExpr((Tree) list.head, this.env, Type.intType);
				obj = new ArrayType((Type) obj);
			}

		} else if (this.pt.tag == 11) {
			type = this.pt.elemtype();
		} else {
			if (this.pt.tag != 18) {
				this.log.error(((Tree) newarray).pos, "illegal.initializer.for.type", this.pt.toJava());
			}
			type = Type.errType;
		}
		if (newarray.elems != null) {
			this.attribExprs(newarray.elems, this.env, type);
			obj = new ArrayType(type);
		}
		this.result = this.check(newarray, (Type) obj, 12, this.pkind, this.pt);
	}

	public void _case(final Assign assign) {
		final Type type = this.attribTree(assign.lhs, this.env.dup(assign), 4, this.pt);
		this.attribExpr(assign.rhs, this.env, type);
		this.result = this.check(assign, type, 12, this.pkind, this.pt);
	}

	public void _case(final Assignop assignop) {
		final List list = List.make(this.attribTree(assignop.lhs, this.env, 4, Type.noType), this.attribExpr(assignop.rhs, this.env));
		final Symbol symbol = assignop.operator = this.rs.resolveOperator(((Tree) assignop).pos, ((Tree) assignop).tag - 17, this.env, list);
		final Type type = (Type) list.head;
		if (symbol.kind == 16) {
			if (type.tag <= 7) {
				this.chk.checkCastable(assignop.rhs.pos, symbol.type.restype(), type);
			} else {
				this.chk.checkType(assignop.rhs.pos, symbol.type.restype(), type);
			}
		}
		this.result = this.check(assignop, type, 12, this.pkind, this.pt);
	}

	public void _case(final Operation operation) {
		final List list = ((Tree) operation).tag >= 42 && ((Tree) operation).tag <= 45 ? Type.emptyList.prepend(this.attribTree((Tree) operation.args.head, this.env, 4, Type.noType)) : this.attribArgs(operation.args, this.env);
		final Symbol symbol = operation.operator = this.rs.resolveOperator(((Tree) operation).pos, ((Tree) operation).tag, this.env, list);
		Type type = Type.errType;
		if (symbol.kind == 16) {
			type = symbol.type.restype();
			final int i = ((OperatorSymbol) symbol).opcode;
			List list1;
			for (list1 = list; list1.nonEmpty() && ((Type) list1.head).constValue != null; list1 = list1.tail) {
			}
			if (list1.isEmpty()) {
				final Type type1 = this.cfolder.fold(i, list);
				if (type1 != null) {
					type = ConstFold.coerce(type1, type);
					for (List list2 = operation.args; list2.nonEmpty(); list2 = list2.tail) {
						if (((Tree) list2.head).type.tsym == this.syms.stringType.tsym) {
							((Tree) list2.head).type = this.syms.stringType;
						}
					}

				}
			}
			if (i == 165 || i == 166) {
				if (!((Type) list.head).isCastable(((Type) list.tail.head).erasure()) && !((Type) list.tail.head).isCastable(((Type) list.head).erasure())) {
					this.log.error(((Tree) operation).pos, "incomparable.types", ((Type) list.head).toJava(), ((Type) list.tail.head).toJava());
				} else {
					this.chk.checkCompatible(((Tree) operation).pos, (Type) list.head, (Type) list.tail.head);
				}
			}
		}
		this.result = this.check(operation, type, 12, this.pkind, this.pt);
	}

	public void _case(final TypeCast typecast) {
		final Type type = this.attribType(typecast.clazz, this.env);
		final Type type1 = this.attribExpr(typecast.expr, this.env);
		Type type2 = this.chk.checkCastable(typecast.expr.pos, type1, type);
		if (type1.constValue != null) {
			type2 = ConstFold.coerce(type1, type2);
		}
		this.result = this.check(typecast, type2, 12, this.pkind, this.pt);
	}

	public void _case(final TypeTest typetest) {
		final Type type = this.attribExpr(typetest.expr, this.env);
		final Type type1 = this.chk.checkClassOrArrayType(typetest.clazz.pos, this.attribType(typetest.clazz, this.env));
		this.chk.checkCastable(typetest.expr.pos, type, type1);
		this.result = this.check(typetest, Type.booleanType, 12, this.pkind, this.pt);
	}

	public void _case(final Indexed indexed) {
		Type type = Type.errType;
		final Type type1 = this.attribExpr(indexed.indexed, this.env);
		this.attribExpr(indexed.index, this.env, Type.intType);
		if (type1.tag == 11) {
			type = type1.elemtype();
		} else if (type1.tag != 18) {
			this.log.error(((Tree) indexed).pos, "array.req.but.found", type1.toJava());
		}
		this.result = this.check(indexed, type, 4, this.pkind, this.pt);
	}

	public void _case(final Ident ident) {
		final Symbol symbol = this.pt.tag == 12 ? this.rs.resolveMethod(((Tree) ident).pos, this.env, ident.name, Type.emptyList, this.pt.argtypes()) : this.rs.resolveIdent(((Tree) ident).pos, this.env, ident.name, this.pkind);
		ident.sym = symbol;
		Env env1 = this.env;
		boolean flag = false;
		if (((Symbol) this.env.enclClass.sym).owner.kind != 1 && (symbol.kind & 0x16) != 0 && symbol.owner.kind == 2 && ident.name != Names._this && ident.name != Names._super) {
			for (; env1.outer != null && !env1.enclClass.sym.isSubClass(symbol.owner); env1 = env1.outer) {
				if ((env1.enclClass.sym.flags() & 0x400000) != 0) {
					flag = true;
				}
			}

			if (env1.enclClass.sym != symbol.owner) {
				Env env2 = env1;
				do {
					env2 = env2.outer;
				} while (env2 != null && (((AttrContext) env2.info).scope == null || ((AttrContext) env2.info).scope.owner.kind != 1 && this.checkNotHiding(((Tree) ident).pos, symbol, ((AttrContext) env2.info).scope)) && (env2.enclClass == null || this.checkNotHiding(((Tree) ident).pos, symbol, env2.enclClass.sym.members())));
			}
		}
		if (symbol.kind == 4) {
			final VarSymbol varsymbol = (VarSymbol) symbol;
			this.checkInit(ident, this.env, varsymbol);
			if (((Symbol) varsymbol).owner.kind == 16 && ((Symbol) varsymbol).owner != ((AttrContext) this.env.info).scope.owner && (((Symbol) varsymbol).flags_field & 0x80000) == 0) {
				varsymbol.flags_field |= 0x80000;
				if ((((Symbol) varsymbol).flags_field & 0x10) == 0) {
					this.log.error(((Tree) ident).pos, "local.var.accessed.from.icls.needs.final", varsymbol.toJava());
				}
			}
			if (this.pkind == 4) {
				this.checkAssignable(((Tree) ident).pos, varsymbol, null, this.env);
			}
		}
		if ((((AttrContext) env1.info).isSelfCall || flag) && (symbol.kind & 0x14) != 0 && symbol.owner.kind == 2 && (symbol.flags() & 8) == 0) {
			this.chk.earlyRefError(((Tree) ident).pos, symbol.kind != 4 ? this.thisSym(this.env) : symbol);
		}
		this.result = this.checkId(ident, ((Symbol) this.env.enclClass.sym).type, symbol, this.env, this.pkind, this.pt);
	}

	public void _case(final Select select) {
		int i = 0;
		if (select.name == Names._this || select.name == Names._super || select.name == Names._class) {
			i = 2;
		} else {
			if ((this.pkind & 1) != 0) {
				i |= 1;
			}
			if ((this.pkind & 2) != 0) {
				i |= 2 | 1;
			}
			if ((this.pkind & 0x1c) != 0) {
				i |= 0xc | 2;
			}
		}
		Type type = this.attribTree(select.selected, this.env, i, Type.noType);
		final Symbol symbol = TreeInfo.symbol(select.selected);
		final boolean flag = ((AttrContext) this.env.info).selectSuper;
		((AttrContext) this.env.info).selectSuper = symbol != null && (symbol.name == Names._super || symbol.kind == 2);
		final Symbol symbol1 = this.selectSym(select, type, this.env, this.pt, this.pkind);
		select.sym = symbol1;
		if (symbol1.kind == 4) {
			final VarSymbol varsymbol = (VarSymbol) symbol1;
			this.evalInit(varsymbol);
			if (this.pkind == 4) {
				this.checkAssignable(((Tree) select).pos, varsymbol, select.selected, this.env);
			}
		}
		if (((AttrContext) this.env.info).selectSuper) {
			if ((symbol1.flags() & 8) == 0 && symbol1.name != Names._this && symbol1.name != Names._super) {
				if (symbol.name == Names._super) {
					this.rs.checkNonAbstract(((Tree) select).pos, symbol1);
				} else if (symbol1.kind == 4 || symbol1.kind == 16) {
					this.rs.access(new StaticError(symbol1), ((Tree) select).pos, type, symbol1.name);
				}
				final Type type1 = ((Symbol) this.env.enclClass.sym).type.asSuper(type.tsym);
				if (type1 != null) {
					type = type1;
				}
			}
			if (((AttrContext) this.env.info).isSelfCall && select.name == Names._this && type.tsym == this.env.enclClass.sym) {
				this.chk.earlyRefError(((Tree) select).pos, symbol1);
			}
		}
		((AttrContext) this.env.info).selectSuper = flag;
		this.result = this.checkId(select, type, symbol1, this.env, this.pkind, this.pt);
	}

	private Symbol selectSym(final Select select, final Type type, final Env env1, final Type type1, final int i) {
		final int j = ((Tree) select).pos;
		final Name name = select.name;
		switch (type.tag) {
		case 13: // '\r'
			return this.rs.access(this.rs.findIdentInPackage(env1, type.tsym, name, i), j, type, name);

		case 10: // '\n'
		case 11: // '\013'
			if (type1.tag == 12) {
				return this.rs.resolveQualifiedMethod(j, env1, type, name, Type.emptyList, type1.argtypes());
			}
			if (name == Names._this || name == Names._super) {
				return this.rs.resolveSelf(j, env1, type.tsym, name, true);
			}
			return name == Names._class ? new VarSymbol(9, Names._class, this.syms.classType, type.tsym) : this.rs.access(this.rs.findIdentInType(env1, type, name, i), j, type, name);

		case 18: // '\022'
			return Symbol.errSymbol;
		}
		if (name == Names._class) {
			return new VarSymbol(9, Names._class, this.syms.classType, type.tsym);
		}
		this.log.error(j, "cant.deref", type.toJava());
		return Symbol.errSymbol;
	}

	private Type checkId(final Tree tree, final Type type, final Symbol symbol, final Env env1, final int i, final Type type1) {
		Object obj;
		switch (symbol.kind) {
		case 2: // '\002'
			obj = symbol.type;
			if (((Type) obj).tag == 10) {
				final Type type2 = ((Type) obj).outer();
				if (type2.tag == 10 && type != type2) {
					Type type3 = type;
					if (type3.tag == 10) {
						type3 = type.asOuterSuper(type2.tsym);
					}
					if (type3 != type2) {
						obj = new ClassType(type3, Type.emptyList, ((Type) obj).tsym);
					}
				}
			}
			break;

		case 4: // '\004'
			final VarSymbol varsymbol = (VarSymbol) symbol;
			obj = symbol.owner.kind != 2 || symbol.name == Names._this || symbol.name == Names._super ? (Object) symbol.type : (Object) type.memberType(symbol);
			if (varsymbol.constValue != null && isStaticReference(tree)) {
				obj = ((Type) obj).constType(varsymbol.constValue);
			}
			break;

		case 16: // '\020'
			if (symbol.name == Names.init) {
				obj = Type.voidType;
			} else {
				obj = Resolve.instantiate(type, symbol, Type.emptyList, type1.argtypes());
				if (obj == null && Type.isRaw(type1.argtypes())) {
					obj = Resolve.instantiate(type, symbol, Type.emptyList, Type.unerasure(type1.argtypes()));
				}
				if (obj == null) {
					this.log.error(tree.pos, "internal.error.cant.instantiate", symbol.toJava(), type.toJava(), Type.toJavaList(type1.argtypes()));
				}
			}
			break;

		case 1: // '\001'
		case 31: // '\037'
			obj = symbol.type;
			break;

		default:
			new Pretty().printExpr(tree);
			throw new InternalError("unexpected kind: " + symbol.kind);
		}
		if ((symbol.flags() & 0x20000) != 0) {
			this.chk.warnDeprecated(tree.pos, symbol);
		}
		return this.check(tree, (Type) obj, symbol.kind, i, type1);
	}

	private void checkInit(final Tree tree, final Env env1, final VarSymbol varsymbol) {
		if (varsymbol.pos > tree.pos && ((Symbol) varsymbol).owner.kind == 2 && canOwnInitializer(((AttrContext) env1.info).scope.owner) && ((Symbol) varsymbol).owner == ((AttrContext) env1.info).scope.owner.enclClass() && (varsymbol.flags() & 8) != 0 == Resolve.isStatic(env1)) {
			this.log.error(tree.pos, "illegal.forward.ref");
		}
		this.evalInit(varsymbol);
	}

	private static boolean canOwnInitializer(final Symbol symbol) {
		return (symbol.kind & 6) != 0 || symbol.kind == 16 && (symbol.flags() & 0x100000) != 0;
	}

	void evalInit(final VarSymbol varsymbol) {
		if (varsymbol.constValue instanceof Env) {
			final Env env1 = (Env) varsymbol.constValue;
			final Name name = this.log.useSource(env1.toplevel.sourcefile);
			varsymbol.constValue = null;
			final Type type = this.attribExpr(((VarDef) env1.tree).init, env1, ((Symbol) varsymbol).type);
			if (type.constValue != null) {
				varsymbol.constValue = ConstFold.coerce(type, ((Symbol) varsymbol).type).constValue;
			}
			this.log.useSource(name);
		}
	}

	private boolean checkNotHiding(final int i, final Symbol symbol, final Scope scope) {
		for (Entry entry = scope.lookup(symbol.name); entry.scope != null; entry = entry.next()) {
			if (entry.sym.owner != symbol.owner && entry.sym.kind == symbol.kind && entry.sym.owner == scope.owner) {
				this.log.error(i, "inherit.hides", symbol.toJava(), symbol.owner.toJava(), Resolve.kindName(symbol.kind), entry.sym.javaLocation());
				return false;
			}
		}

		return true;
	}

	public void _case(final Literal literal) {
		this.result = this.check(literal, this.litType(literal.typetag).constType(literal.value), 12, this.pkind, this.pt);
	}

	private Type litType(final int i) {
		return i != 10 ? Type.typeOfTag[i] : this.syms.stringType;
	}

	public void _case(final TypeIdent typeident) {
		this.result = this.check(typeident, Type.typeOfTag[typeident.typetag], 2, this.pkind, this.pt);
	}

	public void _case(final TypeArray typearray) {
		final Type type = this.attribType(typearray.elemtype, this.env);
		this.result = this.check(typearray, new ArrayType(type), 2, this.pkind, this.pt);
	}

	public void _case(final Erroneous erroneous) {
		this.result = erroneous.type = Type.errType;
	}

	public void _case(final Tree tree) {
		throw new InternalError();
	}

	public void attribClass(final ClassSymbol classsymbol) {
		final Type type = ((Symbol) classsymbol).type.supertype();
		if (type.tag == 10) {
			this.attribClass((ClassSymbol) type.tsym);
		}
		if (((Symbol) classsymbol).owner.kind == 2) {
			this.attribClass((ClassSymbol) ((Symbol) classsymbol).owner);
		}
		if ((((Symbol) classsymbol).flags_field & Flags.UNATTRIBUTED) != 0) {
			final Name name = this.log.useSource(classsymbol.sourcefile);
			classsymbol.flags_field &= 0xdfffffff;
			final Env env1 = (Env) this.enter.classEnvs.get(classsymbol);
			this.enter.classEnvs.remove(classsymbol);
			final ClassDef classdef = (ClassDef) env1.tree;
			if ((classsymbol.flags() & 0x200) == 0) {
				if (this.miranda) {
					this.implementInterfaceMethods(classsymbol, env1);
				}
			} else {
				this.chk.checkCompatibleInterfaces(((Tree) classdef).pos, classsymbol);
			}
			this.chk.validate(classdef.extending);
			this.chk.validate(classdef.implementing);
			if ((classsymbol.flags() & 0x600) == 0 && !this.retrofit) {
				this.chk.checkAllDefined(((Tree) classdef).pos, classsymbol);
			}
			classdef.type = ((Symbol) classsymbol).type;
			this.chk.checkImplementations(((Tree) classdef).pos, classsymbol);
			for (List list = classdef.defs; list.nonEmpty(); list = list.tail) {
				this.attribStat((Tree) list.head, env1);
				if (((Symbol) classsymbol).owner.kind != 1 && ((classsymbol.flags() & 8) == 0 || ((Symbol) classsymbol).name == Names.empty) && (TreeInfo.flags((Tree) list.head) & 0x208) != 0) {
					VarSymbol varsymbol = null;
					if (((Tree) list.head).tag == 5) {
						varsymbol = ((VarDef) list.head).sym;
					}
					if (varsymbol == null || ((Symbol) varsymbol).kind != 4 || varsymbol.constValue == null) {
						this.log.error(((Tree) list.head).pos, "icls.cant.have.static.decl");
					}
				}
			}

			classdef.type = ((Symbol) classsymbol).type;
			this.log.useSource(name);
		}
	}

	private final Log log;
	private final Symtab syms;
	private final Resolve rs;
	private final Check chk;
	private final TreeMaker make;
	public Enter enter;
	private final ConstFold cfolder;
	private final boolean retrofit;
	private final boolean miranda;
	private Env env;
	private int pkind;
	private Type pt;
	private Type result;
	private final Type anyMethodType;
	private final ListBuffer methTemplateSupply;
}
