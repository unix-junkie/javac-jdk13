package sun.tools.java;

import java.io.File;

public class Environment {

	public Environment(Environment environment, final Object obj) {
		if (environment != null && environment.env != null && environment.getClass() == this.getClass()) {
			environment = environment.env;
		}
		this.env = environment;
		this.source = obj;
	}

	protected Environment() {
		this(null, null);
	}

	public boolean isExemptPackage(final Identifier identifier) {
		return this.env.isExemptPackage(identifier);
	}

	public ClassDeclaration getClassDeclaration(final Identifier identifier) {
		return this.env.getClassDeclaration(identifier);
	}

	public final ClassDefinition getClassDefinition(final Identifier identifier) throws ClassNotFound {
		if (identifier.isInner()) {
			ClassDefinition classdefinition = this.getClassDefinition(identifier.getTopName());
			Identifier identifier1 = identifier.getFlatName();
			label0: while (identifier1.isQualified()) {
				identifier1 = identifier1.getTail();
				final Identifier identifier2 = identifier1.getHead();
				final String s = identifier2.toString();
				if (s.length() > 0 && Character.isDigit(s.charAt(0))) {
					final ClassDefinition classdefinition1 = classdefinition.getLocalClass(s);
					if (classdefinition1 != null) {
						classdefinition = classdefinition1;
						continue;
					}
				} else {
					for (MemberDefinition memberdefinition = classdefinition.getFirstMatch(identifier2); memberdefinition != null; memberdefinition = memberdefinition.getNextMatch()) {
						if (!memberdefinition.isInnerClass()) {
							continue;
						}
						classdefinition = memberdefinition.getInnerClass();
						continue label0;
					}

				}
				throw new ClassNotFound(Identifier.lookupInner(classdefinition.getName(), identifier2));
			}
			return classdefinition;
		}
		return this.getClassDeclaration(identifier).getClassDefinition(this);
	}

	public ClassDeclaration getClassDeclaration(final Type type) {
		return this.getClassDeclaration(type.getClassName());
	}

	public final ClassDefinition getClassDefinition(final Type type) throws ClassNotFound {
		return this.getClassDefinition(type.getClassName());
	}

	public boolean classExists(final Identifier identifier) {
		return this.env.classExists(identifier);
	}

	public final boolean classExists(final Type type) {
		return !type.isType(10) || this.classExists(type.getClassName());
	}

	public Package getPackage(final Identifier identifier) {
		return this.env.getPackage(identifier);
	}

	public void loadDefinition(final ClassDeclaration classdeclaration) {
		this.env.loadDefinition(classdeclaration);
	}

	public final Object getSource() {
		return this.source;
	}

	public boolean resolve(final long l, final ClassDefinition classdefinition, final Type type) {
		switch (type.getTypeCode()) {
		case 10: // '\n'
			try {
				final Identifier identifier = type.getClassName();
				if (!identifier.isQualified() && !identifier.isInner() && !this.classExists(identifier)) {
					this.resolve(identifier);
				}
				final ClassDefinition classdefinition1 = this.getQualifiedClassDefinition(l, identifier, classdefinition, false);
				if (!classdefinition.canAccess(this, classdefinition1.getClassDeclaration())) {
					this.error(l, "cant.access.class", classdefinition1);
					return true;
				}
				classdefinition1.noteUsedBy(classdefinition, l, this.env);
			} catch (final AmbiguousClass ambiguousclass) {
				this.error(l, "ambig.class", ambiguousclass.name1, ambiguousclass.name2);
				return false;
			} catch (final ClassNotFound cnf) {
				if (cnf.name.isInner() && this.getPackage(cnf.name.getTopName()).exists()) {
					this.env.error(l, "class.and.package", cnf.name.getTopName());
				}
				this.error(l, "class.not.found.no.context", cnf.name);
				return false;
			}
			return true;

		case 9: // '\t'
			return this.resolve(l, classdefinition, type.getElementType());

		case 12: // '\f'
			boolean flag = this.resolve(l, classdefinition, type.getReturnType());
			final Type atype[] = type.getArgumentTypes();
			for (int i = atype.length; i-- > 0;) {
				flag &= this.resolve(l, classdefinition, atype[i]);
			}

			return flag;

		case 11: // '\013'
		default:
			return true;
		}
	}

	public void resolveByName(final long l, final ClassDefinition classdefinition, final Identifier identifier) {
		this.resolveByName(l, classdefinition, identifier, false);
	}

	public void resolveExtendsByName(final long l, final ClassDefinition classdefinition, final Identifier identifier) {
		this.resolveByName(l, classdefinition, identifier, true);
	}

	private void resolveByName(final long l, final ClassDefinition classdefinition, final Identifier identifier, final boolean flag) {
		try {
			if (!identifier.isQualified() && !identifier.isInner() && !this.classExists(identifier)) {
				this.resolve(identifier);
			}
			final ClassDefinition classdefinition1 = this.getQualifiedClassDefinition(l, identifier, classdefinition, flag);
			final ClassDeclaration classdeclaration = classdefinition1.getClassDeclaration();
			if ((flag || !classdefinition.canAccess(this, classdeclaration)) && (!flag || !classdefinition.extendsCanAccess(this, classdeclaration))) {
				this.error(l, "cant.access.class", classdefinition1);
			}
		} catch (final AmbiguousClass ambiguousclass) {
			this.error(l, "ambig.class", ambiguousclass.name1, ambiguousclass.name2);
		} catch (final ClassNotFound cnf) {
			if (cnf.name.isInner() && this.getPackage(cnf.name.getTopName()).exists()) {
				this.env.error(l, "class.and.package", cnf.name.getTopName());
			}
			this.error(l, "class.not.found", cnf.name, "type name");
		}
	}

	private ClassDefinition getQualifiedClassDefinition(final long l, final Identifier identifier, final ClassDefinition classdefinition, final boolean flag) throws ClassNotFound {
		if (identifier.isInner()) {
			ClassDefinition classdefinition1 = this.getClassDefinition(identifier.getTopName());
			Identifier identifier1 = identifier.getFlatName();
			label0: while (identifier1.isQualified()) {
				identifier1 = identifier1.getTail();
				final Identifier identifier2 = identifier1.getHead();
				final String s = identifier2.toString();
				if (s.length() > 0 && Character.isDigit(s.charAt(0))) {
					final ClassDefinition classdefinition2 = classdefinition1.getLocalClass(s);
					if (classdefinition2 != null) {
						classdefinition1 = classdefinition2;
						continue;
					}
				} else {
					for (MemberDefinition memberdefinition = classdefinition1.getFirstMatch(identifier2); memberdefinition != null; memberdefinition = memberdefinition.getNextMatch()) {
						if (!memberdefinition.isInnerClass()) {
							continue;
						}
						final ClassDeclaration classdeclaration = classdefinition1.getClassDeclaration();
						classdefinition1 = memberdefinition.getInnerClass();
						final ClassDeclaration classdeclaration1 = classdefinition1.getClassDeclaration();
						if (flag ? !classdefinition.extendsCanAccess(this.env, classdeclaration1) : !classdefinition.canAccess(this.env, classdeclaration1)) {
							this.env.error(l, "no.type.access", identifier2, classdeclaration, classdefinition);
						}
						continue label0;
					}

				}
				throw new ClassNotFound(Identifier.lookupInner(classdefinition1.getName(), identifier2));
			}
			return classdefinition1;
		}
		return this.getClassDeclaration(identifier).getClassDefinition(this);
	}

	public Type resolveNames(final ClassDefinition classdefinition, final Type type, final boolean flag) {
		dtEvent("Environment.resolveNames: " + classdefinition + ", " + type);
		switch (type.getTypeCode()) {
		case 11: // '\013'
		default:
			return type;

		case 10: // '\n'
			final Identifier identifier = type.getClassName();
			final Identifier identifier1;
			identifier1 = flag ? this.resolvePackageQualifiedName(identifier) : classdefinition.resolveName(this, identifier);
			if (identifier != identifier1) {
				return Type.tClass(identifier1);
			}
			return type;

		case 9: // '\t'
			return Type.tArray(this.resolveNames(classdefinition, type.getElementType(), flag));

		case 12: // '\f'
			final Type type1 = type.getReturnType();
			final Type type2 = this.resolveNames(classdefinition, type1, flag);
			final Type atype[] = type.getArgumentTypes();
			final Type atype1[] = new Type[atype.length];
			boolean flag1 = type1 != type2;
			for (int i = atype.length; i-- > 0;) {
				final Type type3 = atype[i];
				final Type type4 = this.resolveNames(classdefinition, type3, flag);
				atype1[i] = type4;
				if (type3 != type4) {
					flag1 = true;
				}
			}

			if (flag1) {
				return Type.tMethod(type2, atype1);
			}
			return type;
		}
	}

	public Identifier resolveName(final Identifier identifier) {
		if (identifier.isQualified()) {
			final Identifier identifier1 = this.resolveName(identifier.getHead());
			if (identifier1.hasAmbigPrefix()) {
				return identifier1;
			}
			if (!this.classExists(identifier1)) {
				return this.resolvePackageQualifiedName(identifier);
			}
			try {
				return this.getClassDefinition(identifier1).resolveInnerClass(this, identifier.getTail());
			} catch (final ClassNotFound ignored) {
				return Identifier.lookupInner(identifier1, identifier.getTail());
			}
		}
		try {
			return this.resolve(identifier);
		} catch (final AmbiguousClass ignored) {
			return identifier.hasAmbigPrefix() ? identifier : identifier.addAmbigPrefix();
		} catch (final ClassNotFound ignored) {
			final Imports imports = this.getImports();
			return imports != null ? imports.forceResolve(this, identifier) : identifier;
		}
	}

	public final Identifier resolvePackageQualifiedName(Identifier identifier) {
		Identifier identifier1 = null;
		do {
			if (this.classExists(identifier)) {
				break;
			}
			if (!identifier.isQualified()) {
				identifier = identifier1 != null ? Identifier.lookup(identifier, identifier1) : identifier;
				identifier1 = null;
				break;
			}
			final Identifier identifier2 = identifier.getName();
			identifier1 = identifier1 != null ? Identifier.lookup(identifier2, identifier1) : identifier2;
			identifier = identifier.getQualifier();
		} while (true);
		if (identifier1 != null) {
			return Identifier.lookupInner(identifier, identifier1);
		}
		return identifier;
	}

	public Identifier resolve(final Identifier identifier) throws ClassNotFound {
		return this.env == null ? identifier : this.env.resolve(identifier);
	}

	public Imports getImports() {
		return this.env == null ? null : this.env.getImports();
	}

	public ClassDefinition makeClassDefinition(final Environment environment, final long l, final IdentifierToken identifiertoken, final String s, final int i, final IdentifierToken identifiertoken1, final IdentifierToken aidentifiertoken[], final ClassDefinition classdefinition) {
		return this.env == null ? null : this.env.makeClassDefinition(environment, l, identifiertoken, s, i, identifiertoken1, aidentifiertoken, classdefinition);
	}

	public MemberDefinition makeMemberDefinition(final Environment environment, final long l, final ClassDefinition classdefinition, final String s, final int i, final Type type, final Identifier identifier, final IdentifierToken aidentifiertoken[], final IdentifierToken aidentifiertoken1[], final Object obj) {
		return this.env == null ? null : this.env.makeMemberDefinition(environment, l, classdefinition, s, i, type, identifier, aidentifiertoken, aidentifiertoken1, obj);
	}

	public boolean isApplicable(final MemberDefinition memberdefinition, final Type atype[]) throws ClassNotFound {
		final Type type = memberdefinition.getType();
		if (!type.isType(12)) {
			return false;
		}
		final Type atype1[] = type.getArgumentTypes();
		if (atype.length != atype1.length) {
			return false;
		}
		for (int i = atype.length; --i >= 0;) {
			if (!this.isMoreSpecific(atype[i], atype1[i])) {
				return false;
			}
		}

		return true;
	}

	public boolean isMoreSpecific(final MemberDefinition memberdefinition, final MemberDefinition memberdefinition1) throws ClassNotFound {
		final Type type = memberdefinition.getClassDeclaration().getType();
		final Type type1 = memberdefinition1.getClassDeclaration().getType();
		return this.isMoreSpecific(type, type1) && this.isApplicable(memberdefinition1, memberdefinition.getType().getArgumentTypes());
	}

	public boolean isMoreSpecific(final Type type, final Type type1) throws ClassNotFound {
		return this.implicitCast(type, type1);
	}

	public boolean implicitCast(Type type, Type type1) throws ClassNotFound {
		if (type == type1) {
			return true;
		}
		final int i = type1.getTypeCode();
		switch (type.getTypeCode()) {
		case 1: // '\001'
			if (i == 3) {
				return true;
				// fall through
			}

		case 2: // '\002'
		case 3: // '\003'
			if (i == 4) {
				return true;
				// fall through
			}

		case 4: // '\004'
			if (i == 5) {
				return true;
				// fall through
			}

		case 5: // '\005'
			if (i == 6) {
				return true;
				// fall through
			}

		case 6: // '\006'
			if (i == 7) {
				return true;
				// fall through
			}

		case 7: // '\007'
		default:
			return false;

		case 8: // '\b'
			return type1.inMask(1792);

		case 9: // '\t'
			if (!type1.isType(9)) {
				return type1 == Type.tObject || type1 == Type.tCloneable || type1 == Type.tSerializable;
			}
			do {
				type = type.getElementType();
				type1 = type1.getElementType();
			} while (type.isType(9) && type1.isType(9));
			return type.inMask(1536) && type1.inMask(1536) ? this.isMoreSpecific(type, type1) : type.getTypeCode() == type1.getTypeCode();

		case 10: // '\n'
			break;
		}
		if (i == 10) {
			final ClassDefinition classdefinition = this.getClassDefinition(type);
			final ClassDefinition classdefinition1 = this.getClassDefinition(type1);
			return classdefinition1.implementedBy(this, classdefinition.getClassDeclaration());
		}
		return false;
	}

	public boolean explicitCast(final Type type, final Type type1) throws ClassNotFound {
		if (this.implicitCast(type, type1)) {
			return true;
		}
		if (type.inMask(254)) {
			return type1.inMask(254);
		}
		if (type.isType(10) && type1.isType(10)) {
			final ClassDefinition classdefinition = this.getClassDefinition(type);
			final ClassDefinition classdefinition1 = this.getClassDefinition(type1);
			if (classdefinition1.isFinal()) {
				return classdefinition.implementedBy(this, classdefinition1.getClassDeclaration());
			}
			if (classdefinition.isFinal()) {
				return classdefinition1.implementedBy(this, classdefinition.getClassDeclaration());
			}
			return classdefinition1.isInterface() && classdefinition.isInterface() ? classdefinition1.couldImplement(classdefinition) : classdefinition1.isInterface() || classdefinition.isInterface() || classdefinition.superClassOf(this, classdefinition1.getClassDeclaration());
		}
		if (type1.isType(9)) {
			if (type.isType(9)) {
				Type type2 = type.getElementType();
				Type type3;
				for (type3 = type1.getElementType(); type2.getTypeCode() == 9 && type3.getTypeCode() == 9; type3 = type3.getElementType()) {
					type2 = type2.getElementType();
				}

				if (type2.inMask(1536) && type3.inMask(1536)) {
					return this.explicitCast(type2, type3);
				}
			} else if (type == Type.tObject || type == Type.tCloneable || type == Type.tSerializable) {
				return true;
			}
		}
		return false;
	}

	protected int getFlags() {
		return this.env.getFlags();
	}

	public final boolean debug_lines() {
		return (this.getFlags() & 0x1000) != 0;
	}

	public final boolean debug_vars() {
		return (this.getFlags() & 0x2000) != 0;
	}

	public final boolean debug_source() {
		return (this.getFlags() & Constants.F_DEBUG_SOURCE) != 0;
	}

	public final boolean opt() {
		return (this.getFlags() & 0x4000) != 0;
	}

	public final boolean opt_interclass() {
		return (this.getFlags() & 0x8000) != 0;
	}

	public final boolean verbose() {
		return (this.getFlags() & 1) != 0;
	}

	public final boolean dump() {
		return (this.getFlags() & 2) != 0;
	}

	public final boolean warnings() {
		return (this.getFlags() & 4) != 0;
	}

	public final boolean dependencies() {
		return (this.getFlags() & 0x20) != 0;
	}

	public final boolean print_dependencies() {
		return (this.getFlags() & 0x400) != 0;
	}

	public final boolean deprecation() {
		return (this.getFlags() & 0x200) != 0;
	}

	public final boolean version12() {
		return (this.getFlags() & 0x800) != 0;
	}

	public final boolean strictdefault() {
		return (this.getFlags() & 0x20000) != 0;
	}

	public void shutdown() {
		if (this.env != null) {
			this.env.shutdown();
		}
	}

	public void error(final Object obj, final long l, final String s, final Object obj1, final Object obj2, final Object obj3) {
		this.env.error(obj, l, s, obj1, obj2, obj3);
	}

	public final void error(final long l, final String s, final Object obj, final Object obj1, final Object obj2) {
		this.error(this.source, l, s, obj, obj1, obj2);
	}

	public final void error(final long l, final String s, final Object obj, final Object obj1) {
		this.error(this.source, l, s, obj, obj1, null);
	}

	public final void error(final long l, final String s, final Object obj) {
		this.error(this.source, l, s, obj, null, null);
	}

	public final void error(final long l, final String s) {
		this.error(this.source, l, s, null, null, null);
	}

	public void output(final String s) {
		this.env.output(s);
	}

	public static void debugOutput(final Object obj) {
		if (debugging) {
			System.out.println(obj);
		}
	}

	public void setCharacterEncoding(final String s) {
		this.encoding = s;
	}

	public String getCharacterEncoding() {
		return this.encoding;
	}

	public short getMajorVersion() {
		return this.env == null ? 45 : this.env.getMajorVersion();
	}

	public short getMinorVersion() {
		return this.env == null ? 3 : this.env.getMinorVersion();
	}

	public final boolean coverage() {
		return (this.getFlags() & 0x40) != 0;
	}

	public final boolean covdata() {
		return (this.getFlags() & 0x80) != 0;
	}

	public File getcovFile() {
		return this.env.getcovFile();
	}

	public static void dtEnter(final String s) {
		if (dependtrace) {
			System.out.println(">>> " + s);
		}
	}

	public static void dtExit(final String s) {
		if (dependtrace) {
			System.out.println("<<< " + s);
		}
	}

	public static void dtEvent(final String s) {
		if (dependtrace) {
			System.out.println(s);
		}
	}

	public static boolean dumpModifiers() {
		return dumpmodifiers;
	}

	private final Environment env;
	private String encoding;
	private final Object source;
	private static final boolean debugging = System.getProperty("javac.debug") != null;
	private static final boolean dependtrace = System.getProperty("javac.trace.depend") != null;
	private static final boolean dumpmodifiers = System.getProperty("javac.dump.modifiers") != null;

}
