package com.sun.tools.javac.v8.comp;

import com.sun.tools.javac.v8.code.ClassReader.BadClassFile;
import com.sun.tools.javac.v8.code.Scope;
import com.sun.tools.javac.v8.code.Scope.Entry;
import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.CompletionFailure;
import com.sun.tools.javac.v8.code.Symbol.TypeSymbol;
import com.sun.tools.javac.v8.code.Type;
import com.sun.tools.javac.v8.code.Type.MethodType;
import com.sun.tools.javac.v8.tree.TreeInfo;
import com.sun.tools.javac.v8.util.FatalError;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;

public final class Resolve {
	static class AmbiguityError extends ResolveError {

		void report(final Log log1, final int i, final Type type, final Name name, final List list, final List list1) {
			Name name1 = this.sym1.name;
			if (name1 == Names.init) {
				name1 = this.sym1.owner.name;
			}
			log1.error(i, "ref.ambiguous", name1.toJava(), kindName(this.sym1.kind), this.sym1.toJava(), this.sym1.javaLocation(type), kindName(this.sym2.kind), this.sym2.toJava(), this.sym2.javaLocation(type));
		}

		final Symbol sym1;
		final Symbol sym2;

		AmbiguityError(final Symbol symbol, final Symbol symbol1) {
			super(256);
			this.sym1 = symbol;
			this.sym2 = symbol1;
		}
	}

	static class StaticError extends ResolveError {

		void report(final Log log1, final int i, final Type type, final Name name, final List list, final List list1) {
			log1.error(i, "non-static.cant.be.ref", kindName(this.sym.kind), this.sym.toJava());
		}

		StaticError(final Symbol symbol) {
			super(257);
			this.sym = symbol;
		}
	}

	static class AccessError extends ResolveError {

		void report(final Log log1, final int i, final Type type, final Name name, final List list, final List list1) {
			if (this.sym.owner.type.tag != 18) {
				if (this.sym.name == Names.init && this.sym.owner != type.tsym) {
					ResolveError.methodNotFound.report(log1, i, type, name, list, list1);
				}
				if ((this.sym.flags() & 1) != 0) {
					log1.error(i, "not.def.public.class.intf.cant.access", this.sym.toJava(), this.sym.javaLocation());
				} else if ((this.sym.flags() & 6) != 0) {
					log1.error(i, "report.access", this.sym.toJava(), TreeInfo.flagNames(this.sym.flags() & 6), this.sym.javaLocation());
				} else {
					log1.error(i, "not.def.public.cant.access", this.sym.toJava(), this.sym.javaLocation());
				}
			}
		}

		AccessError(final Symbol symbol) {
			super(257);
			this.sym = symbol;
		}
	}

	static class ResolveError extends Symbol {

		Symbol setWrongSym(final Symbol symbol) {
			this.wrongSym = symbol;
			return this;
		}

		void report(final Log log1, final int i, final Type type, final Name name, final List list, final List list1) {
			if (name != Names.error) {
				String s = absentKindName(this.kind);
				String s1 = name.toJava();
				String s2 = "";
				String s3 = "";
				if (this.kind >= 259 && this.kind <= 261) {
					if (isOperator(name)) {
						log1.error(i, "operator.cant.be.applied", name.toJava(), Type.toJavaList(list1));
						return;
					}
					if (name == Names.init) {
						s = Log.getLocalizedString("kindname.constructor");
						s1 = ((Symbol) type.tsym).name.toJava();
					}
					if (list.nonEmpty()) {
						s2 = '<' + Type.toJavaList(list) + '>';
					}
					s3 = '(' + Type.toJavaList(list1) + ')';
				}
				if (this.kind == 260) {
					log1.error(i, "cant.apply.symbol", this.wrongSym.asMemberOf(type).toJava(), this.wrongSym.javaLocation(type), Type.toJavaList(list1));
				} else if (((Symbol) type.tsym).name.len != 0) {
					log1.error(i, "cant.resolve.location", s, s1, s2, s3, typeKindName(type), type.toJava());
				} else {
					log1.error(i, "cant.resolve", s, s1, s2, s3);
				}
			}
		}

		static boolean isOperator(final Name name) {
			int i;
			for (i = 0; i < name.len && "+-~!*/%&|^<>=".indexOf(name.byteAt(i)) >= 0; i++) {
			}
			return i > 0 && i == name.len;
		}

		static final ResolveError varNotFound = new ResolveError(258);
		static final ResolveError wrongMethod = new ResolveError(260);
		static final Symbol wrongMethods = new ResolveError(259);
		static final ResolveError methodNotFound = new ResolveError(261);
		static final Symbol typeNotFound = new ResolveError(262);
		Symbol sym;
		Symbol wrongSym;

		ResolveError(final int i) {
			super(i, 0, null, null, null);
			this.sym = Symbol.errSymbol;
		}
	}

	public Resolve(final Log log1, final Symtab symtab, final Check check) {
		this.syms = symtab;
		this.log = log1;
		this.chk = check;
	}

	static boolean isStatic(final Env env) {
		return ((AttrContext) env.info).staticLevel > ((AttrContext) env.outer.info).staticLevel;
	}

	static int staticLevel(Symbol symbol) {
		int i = 0;
		do {
			if ((symbol.flags() & 8) != 0) {
				i++;
			}
			symbol = symbol.owner;
		} while (symbol.kind != 1);
		return i;
	}

	static boolean isAccessible(final Env env, final Symbol typesymbol) {
		switch (typesymbol.flags() & 7) {
		case 2: // '\002'
			return env.enclClass.sym.outermostClass() == typesymbol.owner.outermostClass();

		case 0: // '\0'
			return env.toplevel.packge == typesymbol.owner || env.toplevel.packge == typesymbol.packge();

		case 1: // '\001'
			return true;

		case 4: // '\004'
			return env.toplevel.packge == typesymbol.owner || env.toplevel.packge == typesymbol.packge() || isInnerSubClass(env.enclClass.sym, typesymbol.owner);

		case 3: // '\003'
		default:
			return true;
		}
	}

	private static boolean isInnerSubClass(ClassSymbol classsymbol, final Symbol symbol) {
		for (; classsymbol != null && !classsymbol.isSubClass(symbol); classsymbol = ((Symbol) classsymbol).owner.enclClass()) {
		}
		return classsymbol != null;
	}

	private boolean isAccessible(final Env env, final Type type) {
		return type.tag != 11 ? isAccessible(env, type.tsym) : this.isAccessible(env, type.elemtype());
	}

	private boolean isAccessible(final Env env, final Type type, final Symbol symbol) {
		if (symbol.name == Names.init && symbol.owner != type.tsym) {
			return false;
		}
		switch (symbol.flags() & 7) {
		case 2: // '\002'
			return (env.enclClass.sym == symbol.owner || env.enclClass.sym.outermostClass() == symbol.owner.outermostClass()) && symbol.isMemberOf(type.tsym);

		case 0: // '\0'
			return (env.toplevel.packge == symbol.owner.owner || env.toplevel.packge == symbol.packge()) && symbol.isMemberOf(type.tsym);

		case 4: // '\004'
			return env.toplevel.packge == symbol.owner.owner || env.toplevel.packge == symbol.packge() || isProtectedAccessible(symbol, env.enclClass.sym, type) || ((AttrContext) env.info).selectSuper && (symbol.flags() & 8) == 0 && symbol.kind != 2;

		case 1: // '\001'
		case 3: // '\003'
		default:
			return this.isAccessible(env, type);
		}
	}

	private static boolean isProtectedAccessible(final Symbol symbol, ClassSymbol classsymbol, final Type type) {
		for (; classsymbol != null && (!classsymbol.isSubClass(symbol.owner) || (symbol.flags() & 8) == 0 && symbol.kind != 2 && !type.tsym.isSubClass(classsymbol)); classsymbol = ((Symbol) classsymbol).owner.enclClass()) {
		}
		return classsymbol != null;
	}

	static Type instantiate(final Type type, final Symbol symbol, final List list, final List list1) {
		final Type type1 = type.memberType(symbol);
		final MethodType methodtype = (MethodType) type1;
		final List list2 = methodtype.argtypes;
		final Type type2 = symbol.name != Names.init ? methodtype.restype : type;
		return Type.isSubTypes(list1, list2) ? type2 : null;
	}

	private boolean isAsGood(final Env env, final Type type, final Symbol symbol, final Symbol symbol1) {
		return symbol.kind < symbol1.kind || symbol.kind == symbol1.kind && (!this.isAccessible(env, type, symbol1) || this.isAccessible(env, type, symbol) && (((symbol.flags() | symbol1.flags()) & 8) != 0 || (symbol1.owner.flags() & 0x200) != 0 || symbol.owner.isSubClass(symbol1.owner)) && instantiate(type, symbol1, Type.emptyList, type.memberType(symbol).argtypes()) != null);
	}

	private Symbol findField(final Env env, final Type type, final Name name, final Symbol typesymbol) {
		Object obj = ResolveError.varNotFound;
		for (Entry entry = typesymbol.members().lookup(name); entry.scope != null; entry = entry.next()) {
			if (entry.sym.kind == 4 && (entry.sym.flags_field & 0x10000) == 0) {
				return this.isAccessible(env, type, entry.sym) ? entry.sym : new AccessError(entry.sym);
			}
		}

		final Type type1 = typesymbol.type.supertype();
		if (type1 != null && type1.tag == 10) {
			final Symbol symbol = this.findField(env, type, name, type1.tsym);
			if (symbol.kind < ((Symbol) obj).kind) {
				obj = symbol;
			}
		}
		for (List list = typesymbol.type.interfaces(); ((Symbol) obj).kind != 256 && list.nonEmpty(); list = list.tail) {
			final Symbol symbol1 = this.findField(env, type, name, ((Type) list.head).tsym);
			if (((Symbol) obj).kind < 256 && symbol1.kind < 256 && symbol1.owner != ((Symbol) obj).owner) {
				obj = new AmbiguityError((Symbol) obj, symbol1);
			} else if (symbol1.kind < ((Symbol) obj).kind) {
				obj = symbol1;
			}
		}

		return (Symbol) obj;
	}

	private Symbol findVar(final Env env, final Name name) {
		Object obj = ResolveError.varNotFound;
		Env env1 = env;
		for (boolean flag = false; env1.outer != null; env1 = env1.outer) {
			if (isStatic(env1)) {
				flag = true;
			}
			Entry entry;
			for (entry = ((AttrContext) env1.info).scope.lookup(name); entry.scope != null && (entry.sym.kind != 4 || (entry.sym.flags_field & 0x10000) != 0); entry = entry.next()) {
			}
			final Symbol symbol = entry.scope == null ? this.findField(env1, ((Symbol) env1.enclClass.sym).type, name, env1.enclClass.sym) : entry.sym;
			if (symbol.kind <= 256) {
				return flag && symbol.kind == 4 && symbol.owner.kind == 2 && (symbol.flags() & 8) == 0 ? new StaticError(symbol) : symbol;
			}
			if (symbol.kind < ((Symbol) obj).kind) {
				obj = symbol;
			}
			if ((env1.enclClass.sym.flags() & 8) != 0) {
				flag = true;
			}
		}

		final Symbol symbol1 = this.findField(env, ((Symbol) this.syms.predefClass).type, name, this.syms.predefClass);
		return symbol1.kind <= 256 ? symbol1 : (Symbol) obj;
	}

	private Symbol findInterfaceMethod(final Env env, final Type type, final Name name, final List list, final List list1, final ClassSymbol classsymbol, Symbol symbol) {
		for (List list2 = ((Symbol) classsymbol).type.interfaces(); list2.nonEmpty(); list2 = list2.tail) {
			final ClassSymbol classsymbol1 = (ClassSymbol) ((Type) list2.head).tsym;
			for (Entry entry = classsymbol1.members().lookup(name); entry.scope != null; entry = entry.next()) {
				if (entry.sym.kind == 16) {
					if (instantiate(type, entry.sym, list, list1) != null && !this.isAsGood(env, type, symbol, entry.sym)) {
						symbol = entry.sym;
					} else if (symbol.kind > 260) {
						symbol = ResolveError.wrongMethod.setWrongSym(entry.sym);
					} else if (symbol.kind == 260) {
						symbol = ResolveError.wrongMethods;
					}
				}
			}

			symbol = this.findInterfaceMethod(env, type, name, list, list1, classsymbol1, symbol);
		}

		return symbol;
	}

	private Symbol checkBestInterfaceMethod(final Env env, final Type type, final Name name, final List list, final List list1, final ClassSymbol classsymbol, Symbol symbol) {
		for (List list2 = ((Symbol) classsymbol).type.interfaces(); symbol.kind == 16 && list2.nonEmpty(); list2 = list2.tail) {
			final ClassSymbol classsymbol1 = (ClassSymbol) ((Type) list2.head).tsym;
			for (Entry entry = classsymbol1.members().lookup(name); symbol.kind == 16 && entry.scope != null; entry = entry.next()) {
				if (entry.sym.kind == 16 && instantiate(type, entry.sym, list, list1) != null && !this.isAsGood(env, type, symbol, entry.sym)) {
					symbol = new AmbiguityError(symbol, entry.sym);
				}
			}

			symbol = this.checkBestInterfaceMethod(env, type, name, list, list1, classsymbol1, symbol);
		}

		return symbol;
	}

	private Symbol findMethod(final Env env, final Type type, final Name name, final List list, final List list1) {
		Object obj = ResolveError.methodNotFound;
		Type type1 = ((Symbol) type.tsym).type;
		boolean flag = true;
		for (; type1.tag == 10; type1 = type1.supertype()) {
			final ClassSymbol classsymbol = (ClassSymbol) type1.tsym;
			if ((classsymbol.flags() & 0x600) == 0) {
				flag = false;
			}
			for (Entry entry = classsymbol.members().lookup(name); entry.scope != null; entry = entry.next()) {
				if (entry.sym.kind == 16 && (entry.sym.flags_field & 0x10000) == 0) {
					if (instantiate(type, entry.sym, list, list1) != null && !this.isAsGood(env, type, (Symbol) obj, entry.sym)) {
						obj = entry.sym;
					} else if (((Symbol) obj).kind > 260) {
						obj = ResolveError.wrongMethod.setWrongSym(entry.sym);
					} else if (((Symbol) obj).kind == 260) {
						obj = ResolveError.wrongMethods;
					}
				}
			}

			if (flag && !((AttrContext) env.info).selectSuper) {
				obj = this.findInterfaceMethod(env, type, name, list, list1, classsymbol, (Symbol) obj);
			}
		}

		if (((Symbol) obj).kind == 16 && !this.isAccessible(env, type, (Symbol) obj)) {
			obj = new AccessError((Symbol) obj);
		}
		type1 = ((Symbol) type.tsym).type;
		flag = true;
		for (; ((Symbol) obj).kind == 16 && type1.tag == 10; type1 = type1.supertype()) {
			final ClassSymbol classsymbol1 = (ClassSymbol) type1.tsym;
			if ((classsymbol1.flags() & 0x600) == 0) {
				flag = false;
			}
			for (Entry entry1 = classsymbol1.members().lookup(name); ((Symbol) obj).kind == 16 && entry1.scope != null; entry1 = entry1.next()) {
				if (obj != entry1.sym && entry1.sym.kind == 16 && (entry1.sym.flags_field & 0x10000) == 0 && instantiate(type, entry1.sym, list, list1) != null && !this.isAsGood(env, type, (Symbol) obj, entry1.sym)) {
					obj = new AmbiguityError((Symbol) obj, entry1.sym);
				}
			}

			if (((Symbol) obj).kind == 16 && flag && !((AttrContext) env.info).selectSuper) {
				obj = this.checkBestInterfaceMethod(env, type, name, list, list1, classsymbol1, (Symbol) obj);
			}
		}

		return (Symbol) obj;
	}

	private Symbol findFun(final Env env, final Name name, final List list, final List list1) {
		Object obj = ResolveError.methodNotFound;
		Env env1 = env;
		for (boolean flag = false; env1.outer != null; env1 = env1.outer) {
			if (isStatic(env1)) {
				flag = true;
			}
			final Symbol symbol = this.findMethod(env1, ((Symbol) env1.enclClass.sym).type, name, list, list1);
			if (symbol.kind <= 256 || symbol.kind == 259 || symbol.kind == 260) {
				return flag && symbol.kind == 16 && symbol.owner.kind == 2 && (symbol.flags() & 8) == 0 ? new StaticError(symbol) : symbol;
			}
			if (symbol.kind < ((Symbol) obj).kind) {
				obj = symbol;
			}
			if ((env1.enclClass.sym.flags() & 8) != 0) {
				flag = true;
			}
		}

		final Symbol symbol1 = this.findMethod(env, ((Symbol) this.syms.predefClass).type, name, list, list1);
		return symbol1.kind <= 256 ? symbol1 : (Symbol) obj;
	}

	private Symbol loadClass(final Env env, final Name name) {
		try {
			final ClassSymbol classsymbol = this.syms.reader.loadClass(name);
			return isAccessible(env, classsymbol) ? (Symbol) classsymbol : new AccessError(classsymbol);
		} catch (final BadClassFile badclassfile) {
			throw badclassfile;
		} catch (final CompletionFailure ignored) {
			return ResolveError.typeNotFound;
		}
	}

	private Symbol findMemberType(final Env env, final Type type, final Name name, final Symbol typesymbol) {
		Object obj = ResolveError.typeNotFound;
		for (Entry entry = typesymbol.members().lookup(name); entry.scope != null; entry = entry.next()) {
			if (entry.sym.kind == 2) {
				return this.isAccessible(env, type, entry.sym) ? entry.sym : new AccessError(entry.sym);
			}
		}

		final Type type1 = typesymbol.type.supertype();
		if (type1 != null && type1.tag == 10) {
			final Symbol symbol = this.findMemberType(env, type, name, type1.tsym);
			if (symbol.kind < ((Symbol) obj).kind) {
				obj = symbol;
			}
		}
		for (List list = typesymbol.type.interfaces(); ((Symbol) obj).kind != 256 && list.nonEmpty(); list = list.tail) {
			final Symbol symbol1 = this.findMemberType(env, type, name, ((Type) list.head).tsym);
			if (((Symbol) obj).kind < 256 && symbol1.kind < 256 && symbol1.owner != ((Symbol) obj).owner) {
				obj = new AmbiguityError((Symbol) obj, symbol1);
			} else if (symbol1.kind < ((Symbol) obj).kind) {
				obj = symbol1;
			}
		}

		return (Symbol) obj;
	}

	private Symbol findGlobalType(final Env env, final Scope scope, final Name name) {
		Object obj = ResolveError.typeNotFound;
		for (Entry entry = scope.lookup(name); entry.scope != null; entry = entry.next()) {
			if (entry.scope.owner == entry.sym.owner) {
				final Symbol symbol = this.loadClass(env, entry.sym.flatName());
				if (((Symbol) obj).kind == 2 && symbol.kind == 2 && obj != symbol) {
					return new AmbiguityError((Symbol) obj, symbol);
				}
				if (symbol.kind < ((Symbol) obj).kind) {
					obj = symbol;
				}
			}
		}

		return (Symbol) obj;
	}

	private Symbol findType(final Env env, final Name name) {
		Object obj = ResolveError.typeNotFound;
		Env env1 = env;
		for (boolean flag = false; env1.outer != null; env1 = env1.outer) {
			if (isStatic(env1)) {
				flag = true;
			}
			for (Entry entry = ((AttrContext) env1.info).scope.lookup(name); entry.scope != null; entry = entry.next()) {
				if (entry.sym.kind == 2) {
					return flag && entry.sym.type.tag == 14 && entry.sym.owner.kind == 2 ? new StaticError(entry.sym) : entry.sym;
				}
			}

			final Symbol symbol = this.findMemberType(env1, ((Symbol) env1.enclClass.sym).type, name, env1.enclClass.sym);
			if (symbol.kind <= 256) {
				return symbol;
			}
			if (symbol.kind < ((Symbol) obj).kind) {
				obj = symbol;
			}
			final int i = env1.enclClass.sym.flags();
			if ((i & 8) != 0 && (i & 0x200) == 0) {
				flag = true;
			}
		}

		Symbol symbol1 = this.findGlobalType(env, env.toplevel.namedImportScope, name);
		if (symbol1.kind <= 256) {
			return symbol1;
		}
		if (symbol1.kind < ((Symbol) obj).kind) {
			obj = symbol1;
		}
		symbol1 = this.findGlobalType(env, env.toplevel.packge.members(), name);
		if (symbol1.kind <= 256) {
			return symbol1;
		}
		if (symbol1.kind < ((Symbol) obj).kind) {
			obj = symbol1;
		}
		symbol1 = this.findGlobalType(env, env.toplevel.starImportScope, name);
		if (symbol1.kind <= 256) {
			return symbol1;
		}
		if (symbol1.kind < ((Symbol) obj).kind) {
			obj = symbol1;
		}
		return (Symbol) obj;
	}

	private Symbol findIdent(final Env env, final Name name, final int i) {
		Object obj = ResolveError.typeNotFound;
		if ((i & 4) != 0) {
			final Symbol symbol = this.findVar(env, name);
			if (symbol.kind <= 256) {
				return symbol;
			}
			if (symbol.kind < ((Symbol) obj).kind) {
				obj = symbol;
			}
		}
		if ((i & 2) != 0) {
			final Symbol symbol1 = this.findType(env, name);
			if (symbol1.kind <= 256) {
				return symbol1;
			}
			if (symbol1.kind < ((Symbol) obj).kind) {
				obj = symbol1;
			}
		}
		return (i & 1) != 0 ? this.syms.reader.enterPackage(name) : (Symbol) obj;
	}

	Symbol findIdentInPackage(final Env env, final Symbol typesymbol, final Name name, final int i) {
		final Name name1 = TypeSymbol.formFullName(name, typesymbol);
		Object obj = ResolveError.typeNotFound;
		if ((i & 2) != 0) {
			final Symbol symbol = this.loadClass(env, name1);
			if (symbol.kind <= 256) {
				return symbol;
			}
			if (symbol.kind < ((Symbol) obj).kind) {
				obj = symbol;
			}
		}
		return (i & 1) != 0 ? this.syms.reader.enterPackage(name1) : (Symbol) obj;
	}

	Symbol findIdentInType(final Env env, final Type type, final Name name, final int i) {
		Object obj = ResolveError.typeNotFound;
		if ((i & 4) != 0) {
			final Symbol symbol = this.findField(env, type, name, type.tsym);
			if (symbol.kind <= 256) {
				return symbol;
			}
			if (symbol.kind < ((Symbol) obj).kind) {
				obj = symbol;
			}
		}
		if ((i & 2) != 0) {
			final Symbol symbol1 = this.findMemberType(env, type, name, type.tsym);
			if (symbol1.kind <= 256) {
				return symbol1;
			}
			if (symbol1.kind < ((Symbol) obj).kind) {
				obj = symbol1;
			}
		}
		return (Symbol) obj;
	}

	private Symbol access(final Symbol symbol, final int i, final Type type, final Name name, final List list, final List list1) {
		if (symbol.kind >= 256) {
			if (!type.isErroneous() && !Type.isErroneous(list1)) {
				((ResolveError) symbol).report(this.log, i, type, name, list, list1);
			}
			return ((ResolveError) symbol).sym;
		}
		return symbol;
	}

	Symbol access(final Symbol symbol, final int i, final Type type, final Name name) {
		return symbol.kind >= 256 ? this.access(symbol, i, type, name, Type.emptyList, Type.emptyList) : symbol;
	}

	void checkNonAbstract(final int i, final Symbol symbol) {
		if ((symbol.flags() & 0x400) != 0) {
			this.log.error(i, "abstract.cant.be.accessed.directly", kindName(symbol.kind), symbol.toJava());
		}
	}

	private static void printscopes(Scope scope) {
		for (; scope != null; scope = scope.next) {
			if (scope.owner != null) {
				System.err.print(scope.owner + ": ");
			}
			for (Entry entry = scope.elems; entry != null; entry = entry.sibling) {
				if ((entry.sym.flags() & 0x400) != 0) {
					System.err.print("abstract ");
				}
				System.err.print(entry.sym + " ");
			}

			System.err.println();
		}

	}

	static void printscopes(Env env) {
		for (; env.outer != null; env = env.outer) {
			System.err.println("------------------------------");
			printscopes(((AttrContext) env.info).scope);
		}

	}

	public static void printscopes(Type type) {
		for (; type.tag == 10; type = type.supertype()) {
			printscopes(type.tsym.members());
		}

	}

	private void warnUncheckedInvocation(final int i, final Symbol symbol, final List list) {
		if (symbol.kind < 256) {
			this.chk.warnUnchecked(i, symbol.toJava(), symbol.javaLocation(), Type.toJavaList(list));
		}
	}

	Symbol resolveIdent(final int i, final Env env, final Name name, final int j) {
		return this.access(this.findIdent(env, name, j), i, ((Symbol) env.enclClass.sym).type, name);
	}

	Symbol resolveMethod(final int i, final Env env, final Name name, final List list, final List list1) {
		Symbol symbol = this.findFun(env, name, list, list1);
		if (symbol.kind >= 259 && Type.isRaw(list1)) {
			symbol = this.findFun(env, name, list, Type.unerasure(list1));
			this.warnUncheckedInvocation(i, symbol, list1);
		}
		if (symbol.kind >= 256) {
			return this.access(symbol, i, ((Symbol) env.enclClass.sym).type, name, list, list1);
		}
		return symbol;
	}

	Symbol resolveQualifiedMethod(final int i, final Env env, final Type type, final Name name, final List list, final List list1) {
		Symbol symbol = this.findMethod(env, type, name, list, list1);
		if (symbol.kind >= 259 && Type.isRaw(list1)) {
			symbol = this.findMethod(env, type, name, list, Type.unerasure(list1));
			this.warnUncheckedInvocation(i, symbol, list1);
		}
		if (symbol.kind >= 256) {
			return this.access(symbol, i, type, name, list, list1);
		}
		return symbol;
	}

	Symbol resolveInternalMethod(final int i, final Env env, final Type type, final Name name, final List list, final List list1) {
		final Symbol symbol = this.resolveQualifiedMethod(i, env, type, name, list, list1);
		if (symbol.kind == 16) {
			return symbol;
		}
		throw new FatalError(Log.getLocalizedString("fatal.err.cant.locate.meth", name.toJava()));
	}

	Symbol resolveConstructor(final int i, final Env env, final Type type, final List list) {
		final Symbol symbol = this.resolveQualifiedMethod(i, env, type, Names.init, Type.emptyList, list);
		if ((symbol.flags() & 0x20000) != 0) {
			this.chk.warnDeprecated(i, symbol);
		}
		return symbol;
	}

	Symbol resolveOperator(final int i, final int j, final Env env, final List list) {
		final Name name = TreeInfo.operatorName(j);
		return this.access(this.findMethod(env, ((Symbol) this.syms.predefClass).type, name, Type.emptyList, list), i, ((Symbol) env.enclClass.sym).type, name, Type.emptyList, list);
	}

	Symbol resolveSelf(final int i, final Env env, final Symbol typesymbol, final Name name, final boolean flag) {
		Env env1 = env;
		for (boolean flag1 = false; env1.outer != null; env1 = env1.outer) {
			if (isStatic(env1)) {
				flag1 = true;
			}
			if (env1.enclClass.sym == typesymbol || !flag && env1.enclClass.sym.isSubClass(typesymbol)) {
				Object obj = ((AttrContext) env1.info).scope.lookup(name).sym;
				if (obj != null) {
					if (flag1) {
						obj = new StaticError((Symbol) obj);
					}
					return this.access((Symbol) obj, i, ((Symbol) env.enclClass.sym).type, name);
				}
			}
			if ((env1.enclClass.sym.flags() & 8) != 0) {
				flag1 = true;
			}
		}

		this.log.error(i, "not.encl.class", typesymbol.toJava());
		return Symbol.errSymbol;
	}

	static String kindName(final int i) {
		switch (i) {
		case 1: // '\001'
			return Log.getLocalizedString("kindname.package");

		case 2: // '\002'
			return Log.getLocalizedString("kindname.class");

		case 4: // '\004'
			return Log.getLocalizedString("kindname.variable");

		case 12: // '\f'
			return Log.getLocalizedString("kindname.value");

		case 16: // '\020'
			return Log.getLocalizedString("kindname.method");
		}
		return Log.getLocalizedString("kindname.default", Integer.toString(i));
	}

	static String kindNames(final int i) {
		final String as[] = new String[4];
		int j = 0;
		if ((i & 0xc) != 0) {
			as[j++] = (i & 0xc) != 4 ? Log.getLocalizedString("kindname.value") : Log.getLocalizedString("kindname.variable");
		}
		if ((i & 0x10) != 0) {
			as[j++] = Log.getLocalizedString("kindname.method");
		}
		if ((i & 2) != 0) {
			as[j++] = Log.getLocalizedString("kindname.class");
		}
		if ((i & 1) != 0) {
			as[j++] = Log.getLocalizedString("kindname.package");
		}
		String s = "";
		for (int k = 0; k < j - 1; k++) {
			s = s + as[k] + ", ";
		}

		if (j >= 1) {
			s += as[j - 1];
		} else {
			return Log.getLocalizedString("kindname.default", Integer.toString(i));
		}
		return s;
	}

	static String typeKindName(final Type type) {
		if (type.tag == 14) {
			return Log.getLocalizedString("kindname.type.variable");
		}
		if (type.tag == 13) {
			return Log.getLocalizedString("kindname.package");
		}
		return (((Symbol) type.tsym).flags_field & 0x200) != 0 ? Log.getLocalizedString("kindname.interface") : Log.getLocalizedString("kindname.class");
	}

	static String absentKindName(final int i) {
		switch (i) {
		case 258:
			return Log.getLocalizedString("kindname.variable");

		case 259:
		case 260:
		case 261:
			return Log.getLocalizedString("kindname.method");

		case 262:
			return Log.getLocalizedString("kindname.class");
		}
		return Log.getLocalizedString("kindname.identifier");
	}

	static final int AMBIGUOUS = 256;
	static final int HIDDEN = 257;
	static final int ABSENT_VAR = 258;
	static final int WRONG_MTHS = 259;
	static final int WRONG_MTH = 260;
	static final int ABSENT_MTH = 261;
	static final int ABSENT_TYP = 262;
	private final Log log;
	private final Symtab syms;
	private final Check chk;
}
