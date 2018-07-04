package com.sun.tools.javac.v8.comp;

import com.sun.tools.javac.v8.code.ClassReader.BadClassFile;
import com.sun.tools.javac.v8.code.Scope;
import com.sun.tools.javac.v8.code.Scope.Entry;
import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.CompletionFailure;
import com.sun.tools.javac.v8.code.Symbol.MethodSymbol;
import com.sun.tools.javac.v8.code.Symbol.TypeSymbol;
import com.sun.tools.javac.v8.code.Type;
import com.sun.tools.javac.v8.code.Type.MethodType;
import com.sun.tools.javac.v8.tree.Tree;
import com.sun.tools.javac.v8.tree.Tree.Select;
import com.sun.tools.javac.v8.tree.Tree.TypeArray;
import com.sun.tools.javac.v8.tree.Tree.Visitor;
import com.sun.tools.javac.v8.tree.TreeInfo;
import com.sun.tools.javac.v8.util.Abort;
import com.sun.tools.javac.v8.util.Hashtable;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;

public final class Check {
	class Validator extends Visitor {

		public void _case(final TypeArray typearray) {
			Check.this.validate(typearray.elemtype);
		}

		public void _case(final Select select) {
			if (((Tree) select).type.tag == 10) {
				if (((Tree) select).type.outer().tag == 10) {
					Check.this.validate(select.selected);
				} else if (select.selected.type.isParameterized()) {
					Check.this.log.error(((Tree) select).pos, "cant.select.static.class.from.param.type");
				}
				if (((Tree) select).type.isRaw() && ((Tree) select).type.allparams().nonEmpty()) {
					Check.this.log.error(((Tree) select).pos, "improperly.formed.type.param.missing");
				}
			}
		}

		public void _case(final Tree tree) {
		}

	}

	public Check(final Log log1, final Symtab symtab, final Hashtable hashtable) {
		this.compiled = Hashtable.make();
		this.validator = new Validator();
		this.log = log1;
		this.syms = symtab;
		this.gj = hashtable.get("-gj") != null;
		this.warnunchecked = hashtable.get("-warnunchecked") != null;
		this.deprecation = hashtable.get("-deprecation") != null;
	}

	void warnDeprecated(final int i, final Symbol symbol) {
		if (this.compiled.get(symbol.enclClass().flatname) == null) {
			if (this.deprecatedSource == null) {
				this.deprecatedSource = this.log.currentSource();
			} else if (this.deprecatedSource != this.log.currentSource()) {
				this.deprecatedSource = Names.asterisk;
			}
			if (this.deprecation) {
				this.log.warning(i, "has.been.deprecated", symbol.toJava(), symbol.javaLocation());
			}
		}
	}

	private void warnUnchecked(final int i, final String s1, final String s2) {
		this.warnUnchecked(i, "unchecked assignment", s1, s2, null);
	}

	void warnUnchecked(final int i, final String s1, final String s2, final String s3) {
		this.warnUnchecked(i, "unchecked.meth.invocation.applied", s1, s2, s3);
	}

	private void warnUnchecked(final int i, final String s, final String s1, final String s2, final String s3) {
		if (this.uncheckedSource == null) {
			this.uncheckedSource = this.log.currentSource();
		} else if (this.uncheckedSource != this.log.currentSource()) {
			this.uncheckedSource = Names.asterisk;
		}
		if (this.warnunchecked) {
			this.log.warning(i, s, s1, s2, s3, null);
		}
	}

	Type completionError(final int i, final CompletionFailure completionfailure) {
		this.log.error(i, "cant.access", completionfailure.sym.toJava(), completionfailure.errmsg);
		if (completionfailure instanceof BadClassFile) {
			throw new Abort();
		}
		return Type.errType;
	}

	private Type typeError(final int i, final String s, final Type type, final Type type1) {
		this.log.error(i, "prob.found.req", s, type.toJava(), type1.toJava());
		return Type.errType;
	}

	private Type typeTagError(final int i, final String s, final Type type) {
		this.log.error(i, "type.found.req", type.toJava(), s);
		return Type.errType;
	}

	void earlyRefError(final int i, final Symbol symbol) {
		this.log.error(i, "cant.ref.before.ctor.called", symbol.toJava());
	}

	void boundError(final int i, final Type type, final Type type1, final String s) {
		if (s.length() == 0) {
			this.log.error(i, "not.within.bounds", type.toJava());
		} else {
			this.log.error(i, "not.within.bounds.explain", type.toJava(), s);
		}
	}

	Name localClassName(final ClassSymbol classsymbol, final int i) {
		final ClassSymbol classsymbol1 = classsymbol.outermostClass();
		final Name name = Name.fromString(classsymbol1.flatname + "$" + i + (((Symbol) classsymbol).name.len != 0 ? "$" + ((Symbol) classsymbol).name : ""));
		return this.compiled.get(name) != null ? this.localClassName(classsymbol, i + 1) : name;
	}

	Type checkType(final int i, final Type type, final Type type1) {
		if (type1.tag == 18) {
			return type1;
		}
		if (type.isAssignable(type1)) {
			return type;
		}
		if (type.isRaw() && type.unerasure().isAssignable(type1)) {
			this.warnUnchecked(i, type.toJava(), type1.toJava());
			return type1;
		}
		final String s = type.tag <= 7 && type1.tag <= 7 ? Log.getLocalizedString("possible.loss.of.precision") : Log.getLocalizedString("incompatible.types");
		return this.typeError(i, s, type, type1);
	}

	Type checkCastable(final int i, final Type type, final Type type1) {
		if (type.isCastable(type1)) {
			this.checkCompatible(i, type, type1);
			return type1;
		}
		return this.typeError(i, Log.getLocalizedString("inconvertible.types"), type, type1);
	}

	Type checkNonVoid(final int i, final Type type) {
		if (type.tag == 9) {
			this.log.error(i, "void.not.allowed.here");
			return Type.errType;
		}
		return type;
	}

	Type checkClassType(final int i, final Type type) {
		return type.tag != 10 && type.tag != 11 && type.tag != 18 ? this.typeTagError(i, Log.getLocalizedString("type.req.class"), type) : type;
	}

	Type checkClassOrArrayType(final int i, final Type type) {
		return type.tag != 10 && type.tag != 11 && type.tag != 18 ? this.typeTagError(i, Log.getLocalizedString("type.req.class.array"), type) : type;
	}

	void checkRefType(final int i, final Type type) {
		if (type.tag != 10 && type.tag != 11 && type.tag != 14 && type.tag != 18) {
			this.typeTagError(i, Log.getLocalizedString("type.req.ref"), type);
		}
	}

	private boolean checkDisjoint(final int i, final int j, final int k, final int l) {
		if ((j & k) != 0 && (j & l) != 0) {
			this.log.error(i, "illegal.combination.of.modifiers", TreeInfo.flagNames(TreeInfo.firstFlag(j & k)), TreeInfo.flagNames(TreeInfo.firstFlag(j & l)));
			return false;
		}
		return true;
	}

	int checkFlags(final int i, final int j, final Symbol symbol) {
		int l = 0;
		int k;
		switch (symbol.kind) {
		case 4: // '\004'
			if (symbol.owner.kind != 2) {
				k = 16;
				break;
			}
			k = (symbol.owner.flags_field & 0x200) != 0 ? (l = 25) : 223;
			break;

		case 16: // '\020'
			if (symbol.name == Names.init) {
				k = 7;
			} else if ((symbol.owner.flags_field & 0x200) != 0) {
				k = l = 1025;
			} else {
				k = 3391;
			}
			l |= symbol.owner.flags_field & 0x800;
			break;

		case 2: // '\002'
			if (symbol.isLocal()) {
				k = 3088;
				if (symbol.name.len == 0) {
					k |= 8;
				}
			} else if (symbol.owner.kind == 2) {
				k = 3607;
				if (symbol.owner.owner.kind == 1 || (symbol.owner.flags_field & 8) != 0) {
					k |= 8;
				}
				if ((j & 0x200) != 0) {
					l = 8;
				}
			} else {
				k = 3601;
			}
			if ((j & 0x200) != 0) {
				l |= 0x400;
			}
			l |= symbol.owner.flags_field & 0x800;
			break;

		default:
			throw new InternalError();
		}
		final int i1 = j & 0xfff & ~k;
		if (i1 != 0) {
			this.log.error(i, "mod.not.allowed.here", TreeInfo.flagNames(i1));
		} else if ((symbol.kind == 2 || this.checkDisjoint(i, j, 1024, 10)) && this.checkDisjoint(i, j, 1536, 304) && this.checkDisjoint(i, j, 1, 6) && this.checkDisjoint(i, j, 2, 5) && this.checkDisjoint(i, j, 16, 64) && symbol.kind != 2) {
			if (!this.checkDisjoint(i, j, 1280, 2048)) {
			}
		}
		return j & (k | 0xfffff000) | l;
	}

	void validate(final Tree tree) {
		try {
			if (tree != null) {
				tree.visit(this.validator);
			}
		} catch (final CompletionFailure completionfailure) {
			this.completionError(tree.pos, completionfailure);
		}
	}

	void validate(final List list) {
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			this.validate((Tree) list1.head);
		}

	}

	static boolean subset(final Symbol classsymbol, final List list) {
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			if (classsymbol.isSubClass((Symbol) list1.head)) {
				return true;
			}
		}

		return false;
	}

	static boolean intersects(final Symbol classsymbol, final List list) {
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			if (classsymbol.isSubClass((Symbol) list1.head) || ((Symbol) list1.head).isSubClass(classsymbol)) {
				return true;
			}
		}

		return false;
	}

	static List incl(final ClassSymbol classsymbol, final List list) {
		return subset(classsymbol, list) ? list : excl(classsymbol, list).prepend(classsymbol);
	}

	private static List excl(final ClassSymbol classsymbol, final List list) {
		if (list.isEmpty()) {
			return list;
		}
		final List list1 = excl(classsymbol, list.tail);
		if (((Symbol) list.head).isSubClass(classsymbol)) {
			return list1;
		}
		return list1 == list.tail ? list : list1.prepend(list.head);
	}

	static List union(final List list, final List list1) {
		List list2 = list;
		for (List list3 = list1; list3.nonEmpty(); list3 = list3.tail) {
			list2 = incl((ClassSymbol) list3.head, list2);
		}

		return list2;
	}

	static List diff(final List list, final List list1) {
		List list2 = list;
		for (List list3 = list1; list3.nonEmpty(); list3 = list3.tail) {
			list2 = excl((ClassSymbol) list3.head, list2);
		}

		return list2;
	}

	static List intersect(final List list, final List list1) {
		List list2 = ClassSymbol.emptyList;
		for (List list3 = list; list3.nonEmpty(); list3 = list3.tail) {
			if (subset((Symbol) list3.head, list1)) {
				list2 = incl((ClassSymbol) list3.head, list2);
			}
		}

		for (List list4 = list1; list4.nonEmpty(); list4 = list4.tail) {
			if (subset((Symbol) list4.head, list)) {
				list2 = incl((ClassSymbol) list4.head, list2);
			}
		}

		return list2;
	}

	boolean isUnchecked(final Symbol classsymbol) {
		return classsymbol == Type.errType.tsym || classsymbol.isSubClass(this.syms.errorType.tsym) || classsymbol.isSubClass(this.syms.runtimeExceptionType.tsym);
	}

	private boolean isHandled(final Symbol classsymbol, final List list) {
		return this.isUnchecked(classsymbol) || subset(classsymbol, list);
	}

	private List unHandled(final List list, final List list1) {
		List list2 = ClassSymbol.emptyList;
		for (List list3 = list; list3.nonEmpty(); list3 = list3.tail) {
			if (!this.isHandled((Symbol) list3.head, list1)) {
				list2 = list2.prepend(list3.head);
			}
		}

		return list2;
	}

	void checkHandled(final int i, final Symbol classsymbol, final List list) {
		if (!this.isHandled(classsymbol, list)) {
			this.log.error(i, "unreported.exception.need.to.catch.or.throw", classsymbol.type.toJava());
		}
	}

	private static int protection(final int i) {
		switch (i & 7) {
		case 2: // '\002'
			return 3;

		case 4: // '\004'
			return 1;

		case 1: // '\001'
			return 0;

		case 3: // '\003'
		default:
			return 2;
		}
	}

	private static String protectionString(final int i) {
		final int j = i & 7;
		return j != 0 ? TreeInfo.flagNames(j) : "package";
	}

	private static String cannotOverride(final Symbol methodsymbol, final Symbol methodsymbol1) {
		if ((methodsymbol1.flags() & 0x200000 | methodsymbol1.owner.flags() & 0x200) == 0) {
			return Log.getLocalizedString("cant.override", methodsymbol.toJava(), methodsymbol.javaLocation(), methodsymbol1.toJava(), methodsymbol1.javaLocation());
		}
		if ((methodsymbol.flags() & 0x200000 | methodsymbol.owner.flags() & 0x200) == 0) {
			return Log.getLocalizedString("cant.implement", methodsymbol.toJava(), methodsymbol.javaLocation(), methodsymbol1.toJava(), methodsymbol1.javaLocation());
		}
		return Log.getLocalizedString("clashes.with", methodsymbol.toJava(), methodsymbol.javaLocation(), methodsymbol1.toJava(), methodsymbol1.javaLocation());
	}

	private void checkOverride(final int i, final Symbol methodsymbol, final Symbol methodsymbol1, final Symbol classsymbol) {
		if ((methodsymbol1.flags() & 0x10000) == 0) {
			if ((methodsymbol.flags() & 8) != 0 && (methodsymbol1.flags() & 8) == 0) {
				this.log.error(i, "override.static", cannotOverride(methodsymbol, methodsymbol1), methodsymbol.toJava(), methodsymbol1.toJava());
			} else if ((methodsymbol1.flags() & 0x10) != 0 || (methodsymbol.flags() & 8) == 0 && (methodsymbol1.flags() & 8) != 0) {
				this.log.error(i, "override.meth", cannotOverride(methodsymbol, methodsymbol1), TreeInfo.flagNames(methodsymbol1.flags() & 0x18));
			} else if ((classsymbol.flags() & 0x200) == 0 && protection(methodsymbol.flags()) > protection(methodsymbol1.flags())) {
				this.log.error(i, "override.weaker.access", cannotOverride(methodsymbol, methodsymbol1), protectionString(methodsymbol1.flags()));
			} else {
				final Type type = classsymbol.type.memberType(methodsymbol);
				final Type type1 = classsymbol.type.memberType(methodsymbol1);
				final List list = type.typarams();
				final List list1 = type1.typarams();
				final Type type2 = type1.restype().subst(list1, list);
				if ((!this.gj || !type.restype().isSubType(type2)) && !type.restype().isSameType(type2)) {
					if (this.gj && (methodsymbol.flags() & 0x200000) != 0 && type2.isSubType(type.restype())) {
						((MethodType) type).restype = type2;
					} else {
						this.typeError(i, Log.getLocalizedString("override.incompatible.ret", cannotOverride(methodsymbol, methodsymbol1)), type.restype(), type1.restype().subst(list1, list));
					}
				} else if ((classsymbol.flags() & 0x200) == 0) {
					final List list2 = this.unHandled(type.thrown(), type1.thrown());
					if (list2.nonEmpty()) {
						if ((methodsymbol.flags() & 0x200000) != 0) {
							((MethodType) type).thrown = type.thrown().prepend(list2);
						} else {
							this.log.error(i, "override.meth.doesnt.throw", cannotOverride(methodsymbol, methodsymbol1), ((Symbol) list2.head).toJava());
						}
					}
				}
				if ((methodsymbol1.flags() & 0x20000) != 0) {
					this.warnDeprecated(i, methodsymbol1);
				}
			}
		}
	}

	void checkOverride(final int i, final MethodSymbol methodsymbol) {
		final TypeSymbol classsymbol = (TypeSymbol) ((Symbol) methodsymbol).owner;
		for (Type type = ((Symbol) classsymbol).type.supertype(); type.tag == 10; type = type.supertype()) {
			final TypeSymbol typesymbol = type.tsym;
			for (Entry entry = typesymbol.members().lookup(((Symbol) methodsymbol).name); entry.scope != null; entry = entry.next()) {
				if (methodsymbol.overrides(entry.sym, classsymbol)) {
					this.checkOverride(i, methodsymbol, entry.sym, classsymbol);
					return;
				}
			}

		}

	}

	private Symbol firstIncompatibility(final Type type, final Type type1) {
		return this.firstIncompatibility(type, type1, type1);
	}

	private Symbol firstIncompatibility(final Type type, final Type type1, final Type type2) {
		for (Entry entry = type2.tsym.members().elems; entry != null; entry = entry.sibling) {
			if (entry.sym.kind == 16 && !this.isCompatible(type, type, type1, entry.sym)) {
				return entry.sym;
			}
		}

		for (List list = type2.interfaces(); list.nonEmpty(); list = list.tail) {
			final Symbol symbol = this.firstIncompatibility(type, type1, (Type) list.head);
			if (symbol != null) {
				return symbol;
			}
		}

		return null;
	}

	private boolean isCompatible(final Type type, final Type type1, final Type type2, final Symbol symbol) {
		for (Entry entry = type1.tsym.members().lookup(symbol.name); entry.scope != null; entry = entry.next()) {
			if (entry.sym.kind == 16 && entry.sym.type.hasSameArgs(symbol.type)) {
				return entry.sym.type.restype().isSameType(symbol.type.restype());
			}
		}

		for (List list = type1.interfaces(); list.nonEmpty(); list = list.tail) {
			if (!this.isCompatible(type, (Type) list.head, type2, symbol)) {
				return false;
			}
		}

		return true;
	}

	boolean checkCompatible(final int i, final Type type, final Type type1) {
		if (type.tag == 11 && type1.tag == 11) {
			this.checkCompatible(i, type.elemtype(), type1.elemtype());
		} else if (type.tag == 10 && (type.tsym.flags() & 0x200) != 0 && type1.tag == 10 && (type1.tsym.flags() & 0x200) != 0) {
			final Symbol symbol = this.firstIncompatibility(type, type1);
			if (symbol != null) {
				this.log.error(i, "intf.incompatible.diff.ret", type.toJava(), type1.toJava(), symbol.toJava());
				return false;
			}
		}
		return true;
	}

	void checkAllDefined(final int i, final ClassSymbol classsymbol) {
		final MethodSymbol methodsymbol = this.firstUndef(classsymbol, classsymbol);
		if (methodsymbol != null) {
			final Symbol methodsymbol1 = new MethodSymbol(methodsymbol.flags(), ((Symbol) methodsymbol).name, ((Symbol) classsymbol).type.memberType(methodsymbol), ((Symbol) methodsymbol).owner);
			this.log.error(i, "should.be.abstract.doesnt.def", classsymbol.toJava(), methodsymbol1.toJava(), methodsymbol1.javaLocation());
		}
	}

	private MethodSymbol firstUndef(final ClassSymbol classsymbol, final Symbol classsymbol1) {
		MethodSymbol methodsymbol = null;
		if (classsymbol1 == classsymbol || (classsymbol1.flags() & 0x600) != 0) {
			final Scope scope = classsymbol1.members();
			for (Entry entry = scope.elems; methodsymbol == null && entry != null; entry = entry.sibling) {
				if (entry.sym.kind == 16 && (entry.sym.flags() & 0x400) != 0) {
					final MethodSymbol methodsymbol1 = (MethodSymbol) entry.sym;
					final MethodSymbol methodsymbol2 = methodsymbol1.implementation(classsymbol);
					if (methodsymbol2 == null || methodsymbol2 == methodsymbol1) {
						methodsymbol = methodsymbol1;
					}
				}
			}

			if (methodsymbol == null) {
				final Type type = classsymbol1.type.supertype();
				if (type.tag == 10) {
					methodsymbol = this.firstUndef(classsymbol, type.tsym);
				}
			}
			for (List list = classsymbol1.type.interfaces(); methodsymbol == null && list.nonEmpty(); list = list.tail) {
				methodsymbol = this.firstUndef(classsymbol, ((Type) list.head).tsym);
			}

		}
		return methodsymbol;
	}

	void checkImplementations(final int i, final ClassSymbol classsymbol) {
		for (List list = ((Symbol) classsymbol).type.interfaces(); list.nonEmpty(); list = list.tail) {
			this.checkImplementations(i, classsymbol, ((Type) list.head).tsym);
		}

	}

	private void checkImplementations(final int i, final ClassSymbol classsymbol, final Symbol classsymbol1) {
		for (Entry entry = classsymbol1.members().elems; entry != null; entry = entry.sibling) {
			if (entry.sym.kind == 16 && (entry.sym.flags() & 8) == 0) {
				final MethodSymbol methodsymbol = (MethodSymbol) entry.sym;
				final MethodSymbol methodsymbol1 = methodsymbol.implementation(classsymbol);
				if (methodsymbol1 != null) {
					this.checkOverride(i, methodsymbol1, methodsymbol, classsymbol);
				}
			}
		}

		for (List list = classsymbol1.type.interfaces(); list.nonEmpty(); list = list.tail) {
			this.checkImplementations(i, classsymbol, ((Type) list.head).tsym);
		}

	}

	void checkCompatibleInterfaces(final int i, final ClassSymbol classsymbol) {
		for (List list = ((Symbol) classsymbol).type.interfaces(); list.nonEmpty(); list = list.tail) {
			for (List list1 = ((Symbol) classsymbol).type.interfaces(); list1 != list; list1 = list1.tail) {
				if (!this.checkCompatible(i, (Type) list.head, (Type) list1.head)) {
					return;
				}
			}

		}

	}

	final Log log;
	private final Symtab syms;
	private final boolean gj;
	private final boolean warnunchecked;
	private final boolean deprecation;
	public final Hashtable compiled;
	public Name deprecatedSource;
	public Name uncheckedSource;
	private final Validator validator;
}
