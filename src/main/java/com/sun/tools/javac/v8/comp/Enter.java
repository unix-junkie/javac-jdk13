package com.sun.tools.javac.v8.comp;

import java.io.File;
import java.io.IOException;

import com.sun.tools.javac.v8.code.Flags;
import com.sun.tools.javac.v8.code.Scope;
import com.sun.tools.javac.v8.code.Scope.Entry;
import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.Completer;
import com.sun.tools.javac.v8.code.Symbol.CompletionFailure;
import com.sun.tools.javac.v8.code.Symbol.MethodSymbol;
import com.sun.tools.javac.v8.code.Symbol.PackageSymbol;
import com.sun.tools.javac.v8.code.Symbol.TypeSymbol;
import com.sun.tools.javac.v8.code.Symbol.VarSymbol;
import com.sun.tools.javac.v8.code.Type;
import com.sun.tools.javac.v8.code.Type.ClassType;
import com.sun.tools.javac.v8.code.Type.MethodType;
import com.sun.tools.javac.v8.tree.Tree;
import com.sun.tools.javac.v8.tree.Tree.ClassDef;
import com.sun.tools.javac.v8.tree.Tree.Import;
import com.sun.tools.javac.v8.tree.Tree.MethodDef;
import com.sun.tools.javac.v8.tree.Tree.NewClass;
import com.sun.tools.javac.v8.tree.Tree.Select;
import com.sun.tools.javac.v8.tree.Tree.TopLevel;
import com.sun.tools.javac.v8.tree.Tree.TypeParameter;
import com.sun.tools.javac.v8.tree.Tree.VarDef;
import com.sun.tools.javac.v8.tree.Tree.Visitor;
import com.sun.tools.javac.v8.tree.TreeInfo;
import com.sun.tools.javac.v8.tree.TreeMaker;
import com.sun.tools.javac.v8.util.FatalError;
import com.sun.tools.javac.v8.util.Hashtable;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.ListBuffer;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;
import com.sun.tools.javac.v8.util.Set;

public class Enter extends Visitor {
	class CompleteEnter implements Completer {

		public void complete(final Symbol symbol) {
			if (!Enter.this.completionEnabled) {
				symbol.completer = this;
				return;
			}
			final ClassSymbol classsymbol = (ClassSymbol) symbol;
			final ClassType classtype = (ClassType) ((Symbol) classsymbol).type;
			final Env env1 = (Env) Enter.this.classEnvs.get(classsymbol);
			final ClassDef classdef = (ClassDef) env1.tree;
			final Name name = Enter.this.log.useSource(env1.toplevel.sourcefile);
			final boolean flag = Enter.this.halfcompleted.isEmpty();
			try {
				Enter.this.halfcompleted.append(env1);
				if (((Symbol) classsymbol).owner.kind == 1) {
					Enter.this.phase2.memberEnter(env1.toplevel, env1.enclosing(1));
					Enter.this.todo.append(env1);
				}
				classsymbol.flags_field |= 0x30000000;
				if (((Symbol) classsymbol).owner.kind == 2) {
					classsymbol.owner = this.checkNonCyclic(((Tree) classdef).pos, ((Symbol) classsymbol).owner.type, env1).tsym;
					((Symbol) classsymbol).owner.complete();
				}
				final Type type;
				if (classsymbol.fullname == Names.java_lang_Object) {
					type = Type.noType;
				} else if (classdef.extending == null) {
					type = Enter.this.syms.objectType;
				} else {
					type = this.attribBase(classdef.extending, env1, 0);
				}
				classtype.supertype_field = type;
				final ListBuffer listbuffer = new ListBuffer();
				final Set set = Set.make();
				for (List list = classdef.implementing; list.nonEmpty(); list = list.tail) {
					final Type type1 = this.attribBase((Tree) list.head, env1, 512);
					if (type1.tag == 10) {
						listbuffer.append(type1);
						if (set.contains(type1)) {
							Enter.this.log.error(((Tree) list.head).pos, "repeated.interface");
						} else {
							set.add(type1);
						}
					}
				}

				classtype.interfaces_field = listbuffer.toList();
				classsymbol.flags_field &= 0xefffffff;
				Enter.this.attr.attribStats(classdef.typarams, env1);
				if ((classsymbol.flags() & 0x200) == 0 && !TreeInfo.hasConstructors(classdef.defs)) {
					List list1 = Type.emptyList;
					List list2 = ClassSymbol.emptyList;
					boolean flag1 = false;
					if (((Symbol) classsymbol).name.len == 0) {
						final NewClass newclass = (NewClass) env1.next.tree;
						if (newclass.constructor != null) {
							final Type type2 = ((Symbol) classsymbol).type.memberType(newclass.constructor);
							list1 = type2.argtypes();
							if (newclass.encl != null) {
								list1 = list1.prepend(newclass.encl.type);
								flag1 = true;
							}
							list2 = type2.thrown();
						}
					}
					final Tree tree = Enter.this.DefaultConstructor(Enter.this.make.at(((Tree) classdef).pos), classsymbol, list1, list2, flag1);
					classdef.defs = classdef.defs.prepend(tree);
				}
				if ((((Symbol) classsymbol).flags_field & 0x200) == 0) {
					final VarSymbol varsymbol = new VarSymbol(0x40010, Names._this, ((Symbol) classsymbol).type, classsymbol);
					varsymbol.pos = 1025;
					((AttrContext) env1.info).scope.enter(varsymbol);
					if (type.tag == 10) {
						final VarSymbol varsymbol1 = new VarSymbol(0x40010, Names._super, type, classsymbol);
						varsymbol1.pos = 1025;
						((AttrContext) env1.info).scope.enter(varsymbol1);
					}
				}
			} catch (final CompletionFailure completionfailure) {
				Enter.this.chk.completionError(((Tree) classdef).pos, completionfailure);
			}
			if (((Symbol) classsymbol).owner.kind == 1 && Enter.this.syms.reader.enterPackage(classsymbol.fullname).exists()) {
				Enter.this.log.error(((Tree) classdef).pos, "clash.with.pkg.of.same.name", classsymbol.toJava());
			}
			Enter.this.log.useSource(name);
			if (flag) {
				for (; Enter.this.halfcompleted.nonEmpty(); Enter.this.halfcompleted.remove()) {
					this.finish((Env) Enter.this.halfcompleted.first());
				}
			}

		}

		private void finish(final Env env1) {
			final Name name = Enter.this.log.useSource(env1.toplevel.sourcefile);
			final ClassDef classdef = (ClassDef) env1.tree;
			Enter.this.phase2.memberEnter(classdef.defs, env1);
			Enter.this.log.useSource(name);
		}

		private Type attribBase(final Tree tree, final Env env1, final int i) {
			final Type type = Enter.this.attr.attribBase(tree, env1, i);
			return this.checkNonCyclic(tree.pos, type, env1);
		}

		private Type checkNonCyclic(final int i, final Type type, final Env env1) {
			if ((type.tsym.flags() & Flags.LOCKED) != 0) {
				Enter.this.log.error(i, "cyclic.inheritance", type.tsym.toJava());
				return Type.errType;
			}
			return type;
		}

	}

	class MemberEnter extends Visitor {

		private boolean isIncluded(final Symbol symbol, final Scope scope) {
			for (Entry entry = scope.lookup(symbol.name); entry.scope == scope; entry = entry.next()) {
				if (entry.sym.kind == symbol.kind && entry.sym.fullName() == symbol.fullName()) {
					return true;
				}
			}

			return false;
		}

		private void importAll(final int i, final TypeSymbol typesymbol, final Scope scope) {
			if (((Symbol) typesymbol).kind == 1 && !typesymbol.exists()) {
				if (((PackageSymbol) typesymbol).fullname.equals(Names.java_lang)) {
					final String s = Log.getLocalizedString("fatal.err.no.java.lang");
					throw new FatalError(s);
				}
				Enter.this.log.error(i, "doesnt.exist", typesymbol.toJava());
			}
			final Scope scope1 = typesymbol.members();
			for (Entry entry = scope1.elems; entry != null; entry = entry.sibling) {
				if (entry.sym.kind == 2 && !this.isIncluded(entry.sym, scope)) {
					scope.enter(entry.sym, scope1);
				}
			}

		}

		private void importNamed(final int i, final Symbol symbol, final Scope scope) {
			if (symbol.kind == 2 && Enter.this.checkUnique(i, symbol, scope)) {
				scope.enter(symbol, symbol.owner.members());
			}
		}

		Type signature(final List list, final List list1, final Tree tree, final List list2, final Env env1) {
			final ListBuffer listbuffer = new ListBuffer();
			for (List list3 = list1; list3.nonEmpty(); list3 = list3.tail) {
				listbuffer.append(Enter.this.attr.attribType(((VarDef) list3.head).vartype, env1));
			}

			final Type type = tree != null ? Enter.this.attr.attribType(tree, env1) : Type.voidType;
			final ListBuffer listbuffer1 = new ListBuffer();
			for (List list4 = list2; list4.nonEmpty(); list4 = list4.tail) {
				listbuffer1.append(Enter.this.chk.checkClassType(((Tree) list4.head).pos, Enter.this.attr.attribType((Tree) list4.head, env1)).tsym);
			}

			return new MethodType(listbuffer.toList(), type, listbuffer1.toList());
		}

		void memberEnter(final Tree tree, final Env env1) {
			final Env env2 = this.env;
			try {
				this.env = env1;
				tree.visit(this);
				this.env = env2;
			} catch (final CompletionFailure completionfailure) {
				this.env = env2;
				Enter.this.chk.completionError(tree.pos, completionfailure);
			}
		}

		void memberEnter(final List list, final Env env1) {
			for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
				this.memberEnter((Tree) list1.head, env1);
			}

		}

		public void _case(final TopLevel toplevel) {
			if (toplevel.pid != null) {
				for (Object obj = toplevel.packge; ((Symbol) obj).owner != Symbol.rootPackage; obj = ((Symbol) obj).owner) {
					((Symbol) obj).owner.complete();
					if (Enter.this.syms.reader.classes.get(((Symbol) obj).fullName()) != null) {
						Enter.this.log.error(((Tree) toplevel).pos, "pkg.clashes.with.class.of.same.name", ((Symbol) obj).toJava());
					}
				}

			}
			this.importAll(((Tree) toplevel).pos, Enter.this.syms.reader.enterPackage(Names.java_lang), this.env.toplevel.starImportScope);
			this.memberEnter(toplevel.defs, this.env);
		}

		public void _case(final Import import1) {
			final Tree tree = import1.qualid;
			final Name name = TreeInfo.name(tree);
			Enter.this.completionEnabled = false;
			if (tree.tag == 30) {
				final TypeSymbol typesymbol = Enter.this.attr.attribTree(((Select) tree).selected, this.env, 3, Type.noType).tsym;
				if (name == Names.asterisk) {
					this.importAll(((Tree) import1).pos, typesymbol, this.env.toplevel.starImportScope);
				} else {
					final TypeSymbol typesymbol1 = Enter.this.attr.attribType(tree, this.env).tsym;
					this.importNamed(((Tree) import1).pos, typesymbol1, this.env.toplevel.namedImportScope);
				}
			} else {
				this.importNamed(((Tree) import1).pos, Enter.this.rs.access(Enter.this.rs.findIdentInPackage(this.env, Symbol.emptyPackage, name, 2), ((Tree) import1).pos, ((Symbol) this.env.enclClass.sym).type, name), this.env.toplevel.namedImportScope);
			}
			Enter.this.completionEnabled = true;
		}

		public void _case(final MethodDef methoddef) {
			final Scope scope = enterScope(this.env);
			final MethodSymbol methodsymbol = new MethodSymbol(0, methoddef.name, null, scope.owner);
			methodsymbol.flags_field = Enter.this.chk.checkFlags(((Tree) methoddef).pos, methoddef.flags, methodsymbol);
			methoddef.sym = methodsymbol;
			final Env env1 = methodEnv(methoddef, this.env);
			methodsymbol.type = this.signature(methoddef.typarams, methoddef.params, methoddef.restype, methoddef.thrown, env1);
			((AttrContext) env1.info).scope.leave();
			if (Enter.this.checkUnique(((Tree) methoddef).pos, methodsymbol, scope)) {
				Enter.this.checkNotReserved(((Tree) methoddef).pos, methoddef.name);
				scope.enter(methodsymbol);
			}
		}

		public void _case(final VarDef vardef) {
			Enter.this.attr.attribType(vardef.vartype, this.env);
			final Scope scope = enterScope(this.env);
			final VarSymbol varsymbol = new VarSymbol(0, vardef.name, vardef.vartype.type, scope.owner);
			varsymbol.flags_field = Enter.this.chk.checkFlags(((Tree) vardef).pos, vardef.flags, varsymbol);
			vardef.sym = varsymbol;
			if (vardef.init != null) {
				varsymbol.flags_field |= 0x40000;
				if ((((Symbol) varsymbol).flags_field & 0x10) != 0) {
					varsymbol.constValue = initEnv(vardef, this.env);
				}
			}
			if (Enter.this.checkUnique(((Tree) vardef).pos, varsymbol, scope)) {
				Enter.this.checkNotReserved(((Tree) vardef).pos, vardef.name);
				Enter.this.checkTransparentVar(((Tree) vardef).pos, varsymbol, scope);
				scope.enter(varsymbol);
			}
			varsymbol.pos = ((Tree) vardef).pos;
		}

		public void _case(final Tree tree) {
		}

		Env env;

	}

	public Enter(final Log log1, final Symtab symtab, final Resolve resolve, final Check check, final TreeMaker treemaker, final Attr attr1, final ListBuffer listbuffer) {
		this.classEnvs = Hashtable.make();
		this.halfcompleted = new ListBuffer();
		this.completionEnabled = true;
		this.log = log1;
		this.syms = symtab;
		this.rs = resolve;
		this.chk = check;
		this.make = treemaker;
		this.attr = attr1;
		if (attr1 != null) {
			attr1.enter = this;
		}
		this.todo = listbuffer;
		this.phase2 = new MemberEnter();
		this.predefClassDef = new ClassDef(1, ((Symbol) symtab.predefClass).name, null, null, null, null, symtab.predefClass);
	}

	static Tree SuperCall(final TreeMaker treemaker, List list, final boolean flag) {
		final Object obj;
		if (flag) {
			obj = treemaker.Select(treemaker.Ident((VarDef) list.head), Names._super);
			list = list.tail;
		} else {
			obj = treemaker.Ident(Names._super);
		}
		return treemaker.Exec(treemaker.Apply((Tree) obj, treemaker.Idents(list)));
	}

	Tree DefaultConstructor(final TreeMaker treemaker, final Symbol classsymbol, final List list, final List list1, final boolean flag) {
		final List list2 = treemaker.Params(list, Symbol.noSymbol);
		List list3 = Tree.emptyList;
		if (classsymbol.type != this.syms.objectType) {
			list3 = list3.prepend(SuperCall(treemaker, list2, flag));
		}
		final ListBuffer listbuffer = new ListBuffer();
		for (List list4 = list1; list4.nonEmpty(); list4 = list4.tail) {
			listbuffer.append(treemaker.Ident((Symbol) list4.head));
		}

		return treemaker.MethodDef(classsymbol.flags() & 7, Names.init, null, TypeParameter.emptyList, list2, listbuffer.toList(), treemaker.Block(0, list3));
	}

	private void duplicateError(final int i, final Symbol symbol) {
		if (!symbol.type.isErroneous()) {
			this.log.error(i, "already.defined", symbol.toJava(), symbol.javaLocation());
		}
	}

	boolean checkUnique(final int i, final Symbol symbol, final Scope scope) {
		for (Entry entry = scope.lookup(symbol.name); entry.scope == scope; entry = entry.next()) {
			if (symbol != entry.sym && symbol.kind == entry.sym.kind && symbol.name != Names.error && (symbol.kind != 16 || symbol.type.hasSameArgs(entry.sym.type))) {
				this.duplicateError(i, entry.sym);
				return false;
			}
		}

		return true;
	}

	void checkTransparentVar(final int i, final VarSymbol varsymbol, final Scope scope) {
		if (scope.next != null) {
			for (Entry entry = scope.next.lookup(((Symbol) varsymbol).name); entry.scope != null && entry.sym.owner == ((Symbol) varsymbol).owner; entry = entry.next()) {
				if (entry.sym.kind == 4 && (entry.sym.owner.kind & 0x14) != 0 && ((Symbol) varsymbol).name != Names.error) {
					this.duplicateError(i, entry.sym);
					return;
				}
			}

		}
	}

	private void checkTransparentClass(final int i, final ClassSymbol classsymbol, final Scope scope) {
		if (scope.next != null) {
			for (Entry entry = scope.next.lookup(((Symbol) classsymbol).name); entry.scope != null && entry.sym.owner == ((Symbol) classsymbol).owner; entry = entry.next()) {
				if (entry.sym.kind == 2 && (entry.sym.owner.kind & 0x14) != 0 && ((Symbol) classsymbol).name != Names.error) {
					this.duplicateError(i, entry.sym);
					return;
				}
			}

		}
	}

	private boolean checkUniqueClassName(final int i, final Name name, final Scope scope) {
		for (Entry entry = scope.lookup(name); entry.scope == scope; entry = entry.next()) {
			if (entry.sym.kind == 2 && entry.sym.name != Names.error) {
				this.duplicateError(i, entry.sym);
				return false;
			}
		}

		for (Symbol symbol = scope.owner; symbol != null; symbol = symbol.owner) {
			if (symbol.kind == 2 && symbol.name == name && symbol.name != Names.error) {
				this.duplicateError(i, symbol);
				return true;
			}
		}

		return true;
	}

	void checkNotReserved(final int i, final Name name) {
		if (name == Names.classDollar || name.startsWith(Names.thisDollar)) {
			this.log.error(i, "name.reserved.for.internal.use", name.toJava());
		}
	}

	static Env methodEnv(final MethodDef methoddef, final Env env1) {
		final Env env2 = env1.dup(methoddef, ((AttrContext) env1.info).dup(((AttrContext) env1.info).scope.dupUnshared()));
		env2.enclMethod = methoddef;
		((AttrContext) env2.info).scope.owner = methoddef.sym;
		if ((methoddef.flags & 8) != 0) {
			((AttrContext) env2.info).staticLevel++;
		}
		return env2;
	}

	private static Env classEnv(final ClassDef classdef, final Env env1) {
		final Env env2 = env1.dup(classdef, ((AttrContext) env1.info).dup(new Scope(classdef.sym)));
		env2.enclClass = classdef;
		env2.outer = env1;
		((AttrContext) env2.info).isSelfCall = false;
		return env2;
	}

	private Env topLevelEnv(final TopLevel toplevel) {
		final Env env1 = new Env(toplevel, new AttrContext());
		env1.toplevel = toplevel;
		env1.enclClass = this.predefClassDef;
		toplevel.namedImportScope = new Scope(toplevel.packge);
		toplevel.starImportScope = new Scope(toplevel.packge);
		((AttrContext) env1.info).scope = toplevel.namedImportScope;
		return env1;
	}

	static Env initEnv(final VarDef vardef, final Env env1) {
		final Env env2 = env1.dup(vardef, ((AttrContext) env1.info).dup());
		if (((Symbol) vardef.sym).owner.kind == 2) {
			((AttrContext) env2.info).scope = ((AttrContext) env1.info).scope.dup();
			((AttrContext) env2.info).scope.owner = vardef.sym;
		}
		if ((vardef.flags & 8) != 0 || (env1.enclClass.sym.flags() & 0x200) != 0) {
			((AttrContext) env2.info).staticLevel++;
		}
		return env2;
	}

	static Scope enterScope(final Env env1) {
		return env1.tree.tag != 3 ? ((AttrContext) env1.info).scope : ((ClassDef) env1.tree).sym.members_field;
	}

	Type classEnter(final Tree tree, final Env env1) {
		final Env env2 = this.env;
		try {
			this.env = env1;
			tree.visit(this);
			this.env = env2;
			return this.result;
		} catch (final CompletionFailure completionfailure) {
			this.env = env2;
			return this.chk.completionError(tree.pos, completionfailure);
		}
	}

	private List classEnter(final List list, final Env env1) {
		final ListBuffer listbuffer = new ListBuffer();
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			listbuffer.append(this.classEnter((Tree) list1.head, env1));
		}

		return listbuffer.toList();
	}

	public void _case(final TopLevel toplevel) {
		final Name name = this.log.useSource(toplevel.sourcefile);
		toplevel.packge = toplevel.pid != null ? this.syms.reader.enterPackage(TreeInfo.fullName(toplevel.pid)) : Symbol.emptyPackage;
		toplevel.packge.complete();
		this.classEnter(toplevel.defs, this.topLevelEnv(toplevel));
		this.log.useSource(name);
		this.result = null;
	}

	public void _case(final ClassDef classdef) {
		final Symbol symbol = ((AttrContext) this.env.info).scope.owner;
		final Scope scope = enterScope(this.env);
		final ClassSymbol classsymbol;
		if (symbol.kind == 1) {
			classsymbol = this.syms.reader.enterClass(classdef.name, symbol);
			symbol.members().enterIfAbsent(classsymbol);
			if ((classdef.flags & 1) != 0 && !classNameMatchesFileName(classsymbol, this.env)) {
				this.log.error(((Tree) classdef).pos, "class.public.should.be.in.file", classdef.name.toJava());
			}
		} else {
			if (classdef.name.len != 0 && !this.checkUniqueClassName(((Tree) classdef).pos, classdef.name, scope)) {
				this.result = null;
				return;
			}
			if (symbol.kind == 2) {
				classsymbol = this.syms.reader.enterClass(classdef.name, symbol);
				if ((symbol.flags_field & 0x200) != 0) {
					classdef.flags |= 9;
				}
			} else {
				classsymbol = this.syms.reader.defineClass(classdef.name, symbol);
				classsymbol.flatname = this.chk.localClassName(classsymbol, 1);
				if (((Symbol) classsymbol).name.len != 0) {
					this.checkTransparentClass(((Tree) classdef).pos, classsymbol, ((AttrContext) this.env.info).scope);
				}
			}
		}
		if (this.chk.compiled.get(classsymbol.flatname) != null) {
			this.log.error(((Tree) classdef).pos, "duplicate.class", classsymbol.fullname.toJava());
			this.result = null;
			return;
		}
		this.chk.compiled.put(classsymbol.flatname, classsymbol);
		scope.enter(classsymbol);
		classdef.sym = classsymbol;
		final Env env1 = classEnv(classdef, this.env);
		this.classEnvs.put(classsymbol, env1);
		classsymbol.completer = new CompleteEnter();
		classsymbol.flags_field = this.chk.checkFlags(((Tree) classdef).pos, classdef.flags, classsymbol);
		classsymbol.sourcefile = this.env.toplevel.sourcefile;
		classsymbol.members_field = new Scope(classsymbol);
		final ClassType classtype = (ClassType) ((Symbol) classsymbol).type;
		if (symbol.kind != 1 && ((((Symbol) classsymbol).flags_field & 8) == 0 || (((Symbol) classsymbol).flags_field & 0x200) != 0)) {
			Symbol symbol1;
			for (symbol1 = symbol; (symbol1.kind & 0x14) != 0 && (symbol1.flags_field & 8) == 0; symbol1 = symbol1.owner) {
			}
			if (symbol1.kind == 2) {
				classtype.outer_field = symbol1.type;
			}
		}
		classtype.typarams_field = this.classEnter(classdef.typarams, env1);
		if (!classsymbol.isLocal() && this.uncompleted != null) {
			this.uncompleted.append(classsymbol);
		}
		this.classEnter(classdef.defs, env1);
		this.result = ((Symbol) classsymbol).type;
	}

	private static boolean classNameMatchesFileName(final ClassSymbol classsymbol, final Env env1) {
		final String s = env1.toplevel.sourcefile.toString();
		final String s1 = ((Symbol) classsymbol).name + ".java";
		try {
			return endsWith(s, s1) || endsWith(new File(s).getCanonicalPath(), s1);
		} catch (final IOException ignored) {
			return false;
		}
	}

	private static boolean endsWith(final String s, final String s1) {
		return s.endsWith(s1) && (s.length() == s1.length() || s.charAt(s.length() - s1.length() - 1) == File.separatorChar);
	}

	public void _case(final Tree tree) {
		this.result = null;
	}

	public void main(final List list) {
		final ListBuffer listbuffer = this.uncompleted;
		this.uncompleted = new ListBuffer();
		this.classEnter(list, null);
		final List list1 = this.uncompleted.toList();
		if (this.completionEnabled) {
			for (List list2 = list1; list2.nonEmpty(); list2 = list2.tail) {
				((Symbol) list2.head).complete();
			}

		}
		this.uncompleted = listbuffer;
	}

	final Log log;
	final Symtab syms;
	final Resolve rs;
	final Check chk;
	final TreeMaker make;
	Attr attr;
	final MemberEnter phase2;
	final ListBuffer todo;
	final Hashtable classEnvs;
	private ListBuffer uncompleted;
	final ListBuffer halfcompleted;
	private final ClassDef predefClassDef;
	boolean completionEnabled;
	private Env env;
	private Type result;

}
