package com.sun.tools.javac.v8.code;

import com.sun.tools.javac.v8.code.Scope.Entry;
import com.sun.tools.javac.v8.code.Type.ClassType;
import com.sun.tools.javac.v8.code.Type.MethodType;
import com.sun.tools.javac.v8.code.Type.PackageType;
import com.sun.tools.javac.v8.util.FileEntry;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;

public class Symbol {
	public static class CompletionFailure extends RuntimeException {
		private static final long serialVersionUID = 5103528953758376385L;

		public String getMessage() {
			return this.errmsg;
		}

		public final Symbol sym;
		public final String errmsg;

		public CompletionFailure(final Symbol symbol, final String s) {
			this.sym = symbol;
			this.errmsg = s;
		}
	}

	public interface Completer {

		void complete(Symbol symbol);
	}

	public static class OperatorSymbol extends MethodSymbol {

		public final int opcode;

		public OperatorSymbol(final Name name1, final Type type1, final int i, final Symbol symbol) {
			super(9, name1, type1, symbol);
			this.opcode = i;
		}
	}

	public static class MethodSymbol extends Symbol {

		public Symbol clone(final Symbol symbol) {
			final MethodSymbol methodsymbol = new MethodSymbol(this.flags_field, this.name, this.type, symbol);
			methodsymbol.code = this.code;
			return methodsymbol;
		}

		public String toString() {
			if ((this.flags() & 0x100000) != 0) {
				return "body of " + this.owner;
			}
			final String s = this.name == Names.init ? "constructor " + this.owner.name : "method " + this.name;
			return s + '(' + this.type.argtypes() + ')';
		}

		public String toJava() {
			if ((this.flags() & 0x100000) != 0) {
				return this.owner.name.toString();
			}
			final String s = this.name == Names.init ? this.owner.name.toString() : this.name.toString();
			return s + '(' + this.type.argtypes() + ')';
		}

		public boolean overrides(final Symbol symbol, final TypeSymbol typesymbol) {
			return !this.isConstructor() && symbol.kind == 16 && symbol.isMemberOf(this.owner) && ((Symbol) typesymbol).type.memberType(this).hasSameArgs(((Symbol) typesymbol).type.memberType(symbol));
		}

		public MethodSymbol implementation(final TypeSymbol typesymbol) {
			for (Type type1 = ((Symbol) typesymbol).type; type1.tag == 10; type1 = type1.supertype()) {
				final TypeSymbol typesymbol1 = type1.tsym;
				for (Entry entry = typesymbol1.members().lookup(this.name); entry.scope != null; entry = entry.next()) {
					if (entry.sym.kind == 16) {
						final MethodSymbol methodsymbol = (MethodSymbol) entry.sym;
						if (methodsymbol.overrides(this, typesymbol) && (methodsymbol.flags() & 0x10000) == 0) {
							return methodsymbol;
						}
					}
				}

			}

			return null;
		}

		public Symbol asMemberOf(final Type type1) {
			return new MethodSymbol(this.flags_field, this.name, type1.memberType(this), this.owner);
		}

		public Code code;

		public MethodSymbol(final int i, final Name name1, final Type type1, final Symbol symbol) {
			super(16, i, name1, type1, symbol);
			this.code = null;
		}
	}

	public static class VarSymbol extends Symbol {

		public Symbol clone(final Symbol symbol) {
			final VarSymbol varsymbol = new VarSymbol(this.flags_field, this.name, this.type, symbol);
			varsymbol.pos = this.pos;
			varsymbol.adr = this.adr;
			varsymbol.constValue = this.constValue;
			return varsymbol;
		}

		public String toString() {
			return "variable " + this.name;
		}

		public Symbol asMemberOf(final Type type1) {
			return new VarSymbol(this.flags_field, this.name, type1.memberType(this), this.owner);
		}

		public int pos;
		public int adr;
		public Object constValue;
		public static final List emptyList = new List();

		public VarSymbol(final int i, final Name name1, final Type type1, final Symbol symbol) {
			super(4, i, name1, type1, symbol);
			this.pos = 0;
			this.adr = -1;
		}
	}

	public static class ClassSymbol extends TypeSymbol {

		public String toString() {
			return ((this.flags_field & 0x200) == 0 ? "class " : "interface ") + this.className();
		}

		public String toJava() {
			return this.className();
		}

		public int flags() {
			if (this.completer != null) {
				this.complete();
			}
			return this.flags_field;
		}

		public Scope members() {
			if (this.completer != null) {
				this.complete();
			}
			return this.members_field;
		}

		public Type erasure() {
			if (this.erasure_field == null) {
				this.erasure_field = this.type.isParameterized() ? new ClassType(this.type.outer().erasure(), Type.emptyList, this) : this.type;
			}
			return this.erasure_field;
		}

		String className() {
			return this.name.len == 0 ? Log.getLocalizedString("anonymous.class", this.flatname.toString()) : this.fullname.toString();
		}

		public Name fullName() {
			return this.fullname;
		}

		public Name flatName() {
			return this.flatname;
		}

		public boolean isSubClass(final Symbol symbol) {
			if (this == symbol) {
				return true;
			}
			if ((symbol.flags() & 0x200) != 0) {
				for (Type type1 = this.type; type1.tag == 10; type1 = type1.supertype()) {
					for (List list = type1.interfaces(); list.nonEmpty(); list = list.tail) {
						if (((Type) list.head).tsym.isSubClass(symbol)) {
							return true;
						}
					}

				}

			} else {
				for (Type type2 = this.type; type2.tag == 10; type2 = type2.supertype()) {
					if (type2.tsym == symbol) {
						return true;
					}
				}

			}
			return false;
		}

		public Scope members_field;
		public Name fullname;
		public Name flatname;
		public Name sourcefile;
		public FileEntry classfile;
		public Pool pool;
		public static final List emptyList = new List();

		public ClassSymbol(final int i, final Name name1, final Type type1, final Symbol symbol) {
			super(i, name1, type1, symbol);
			this.members_field = null;
			this.fullname = TypeSymbol.formFullName(name1, symbol);
			this.flatname = TypeSymbol.formFlatName(name1, symbol);
			this.sourcefile = null;
			this.classfile = null;
			this.pool = null;
		}

		public ClassSymbol(final int i, final Name name1, final Symbol symbol) {
			this(i, name1, new ClassType(Type.noType, Type.emptyList, null), symbol);
			this.type.tsym = this;
		}
	}

	public static class PackageSymbol extends TypeSymbol {

		public String toString() {
			return "package " + this.fullname;
		}

		public String toJava() {
			return this.fullname.toString();
		}

		public Name fullName() {
			return this.fullname;
		}

		public Scope members() {
			if (this.completer != null) {
				this.complete();
			}
			return this.members_field;
		}

		public int flags() {
			if (this.completer != null) {
				this.complete();
			}
			return this.flags_field;
		}

		public boolean exists() {
			return (this.flags() & 0x800000) != 0 || this.members().elems != null;
		}

		public Scope members_field;
		public final Name fullname;

		PackageSymbol(final Name name1, final Type type1, final Symbol symbol) {
			super(0, name1, type1, symbol);
			this.kind = 1;
			this.members_field = null;
			this.fullname = TypeSymbol.formFullName(name1, symbol);
		}

		public PackageSymbol(final Name name1, final Symbol symbol) {
			this(name1, null, symbol);
			this.type = new PackageType(this);
		}
	}

	public static class TypeSymbol extends Symbol {

		public String toString() {
			return "type variable " + this.name;
		}

		public static Name formFullName(final Name name1, final Symbol symbol) {
			if (symbol == null || (symbol.kind & 0x14) != 0) {
				return name1;
			}
			final Name name2 = symbol.fullName();
			return name2 == null || name2 == Names.empty || name2 == Names.emptyPackage ? name1 : name2.append(Names.period).append(name1);
		}

		public static Name formFlatName(final Name name1, final Symbol symbol) {
			if (symbol == null || (symbol.kind & 0x14) != 0) {
				return name1;
			}
			final Name name2 = symbol.kind != 2 ? Names.period : Names.dollar;
			final Name name3 = symbol.flatName();
			return name3 == null || name3 == Names.empty || name3 == Names.emptyPackage ? name1 : name3.append(name2).append(name1);
		}

		public boolean exists() {
			return true;
		}

		public TypeSymbol(final int i, final Name name1, final Type type1, final Symbol symbol) {
			super(2, i, name1, type1, symbol);
		}
	}

	public int flags() {
		return this.flags_field;
	}

	protected Symbol(final int i, final int j, final Name name1, final Type type1, final Symbol symbol) {
		this.kind = i;
		this.flags_field = j;
		this.name = name1;
		this.type = type1;
		this.owner = symbol;
		this.completer = null;
		this.erasure_field = null;
	}

	public Symbol clone(final Symbol symbol) {
		throw new InternalError();
	}

	private static void init() {
		rootPackage = new PackageSymbol(Names.empty, null);
		emptyPackage = new PackageSymbol(Names.emptyPackage, rootPackage);
		noSymbol = new TypeSymbol(0, Names.empty, Type.noType, rootPackage);
		noSymbol.kind = 0;
		errSymbol = new ClassSymbol(9, Names.any, Type.errType, rootPackage);
		errSymbol.kind = 31;
		errSymbol.members_field = Scope.errScope;
		Scope.errScope.owner = errSymbol;
		Type.init();
	}

	public static void reset() {
		rootPackage.members_field = null;
		emptyPackage.members_field = null;
	}

	public String toString() {
		return this.name.toString();
	}

	public String toJava() {
		return this.name.toString();
	}

	public String javaLocation() {
		return this.owner.name == null || this.owner.name.len == 0 ? "" : this.owner.toJava();
	}

	public String javaLocation(final Type type1) {
		if (this.owner.name == null || this.owner.name.len == 0) {
			return "";
		}
		if (this.owner.type.tag == 10) {
			final Type type2 = type1.asOuterSuper(this.owner);
			if (type2 != null) {
				return type2.toJava();
			}
		}
		return this.owner.toJava();
	}

	public Type erasure() {
		if (this.erasure_field == null) {
			this.erasure_field = this.type.erasure();
		}
		return this.erasure_field;
	}

	public Type externalType() {
		final Type type1 = this.erasure();
		if (this.name == Names.init && this.owner.hasOuterInstance()) {
			final Type type2 = this.owner.type.outer().erasure();
			return new MethodType(type1.argtypes().prepend(type2), type1.restype(), type1.thrown());
		}
		return type1;
	}

	public boolean isLocal() {
		return (this.owner.kind & 0x14) != 0 || this.owner.kind == 2 && this.owner.isLocal();
	}

	public boolean isConstructor() {
		return this.name == Names.init;
	}

	public Name fullName() {
		return this.name;
	}

	public Name flatName() {
		return this.fullName();
	}

	public Scope members() {
		return null;
	}

	public boolean isInner() {
		return this.type.outer().tag == 10;
	}

	public boolean hasOuterInstance() {
		return this.type.outer().tag == 10 && (this.flags() & 0x400200) == 0;
	}

	public ClassSymbol enclClass() {
		Symbol symbol;
		for (symbol = this; symbol != null && (symbol.kind & 2) == 0; symbol = symbol.owner) {
		}
		return (ClassSymbol) symbol;
	}

	public ClassSymbol outermostClass() {
		Symbol symbol = this;
		Symbol symbol1 = null;
		for (; symbol.kind != 1; symbol = symbol.owner) {
			symbol1 = symbol;
		}

		return (ClassSymbol) symbol1;
	}

	public PackageSymbol packge() {
		Symbol symbol;
		for (symbol = this; symbol.kind != 1; symbol = symbol.owner) {
		}
		return (PackageSymbol) symbol;
	}

	public boolean isSubClass(final Symbol symbol) {
		throw new InternalError("isSubClass " + this);
	}

	public boolean isMemberOf(Symbol symbol) {
		if ((this.flags_field & 2) != 0) {
			return this.owner == symbol;
		}
		if ((this.flags_field & 5) == 0) {
			final PackageSymbol packagesymbol = this.owner.packge();
			for (; symbol != null && symbol != this.owner; symbol = symbol.type.supertype().tsym) {
				if (symbol.packge() != packagesymbol) {
					return false;
				}
			}

		}
		return true;
	}

	public Symbol asMemberOf(final Type type1) {
		throw new InternalError();
	}

	public void complete() {
		if (this.completer != null) {
			final Completer completer1 = this.completer;
			this.completer = null;
			completer1.complete(this);
		}
	}

	public int kind;
	public int flags_field;
	public Name name;
	public Type type;
	public Symbol owner;
	public Completer completer;
	public Type erasure_field;
	public static PackageSymbol rootPackage;
	public static PackageSymbol emptyPackage;
	public static TypeSymbol noSymbol;
	public static ClassSymbol errSymbol;

	static {
		init();
	}
}
