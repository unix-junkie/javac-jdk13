package sun.tools.javac;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import sun.tools.asm.Assembler;
import sun.tools.asm.ConstantPool;
import sun.tools.java.AmbiguousClass;
import sun.tools.java.ClassDeclaration;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassFile;
import sun.tools.java.ClassNotFound;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.IdentifierToken;
import sun.tools.java.Imports;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Type;
import sun.tools.tree.AssignExpression;
import sun.tools.tree.CatchStatement;
import sun.tools.tree.CompoundStatement;
import sun.tools.tree.Context;
import sun.tools.tree.Expression;
import sun.tools.tree.ExpressionStatement;
import sun.tools.tree.FieldExpression;
import sun.tools.tree.IdentifierExpression;
import sun.tools.tree.LocalMember;
import sun.tools.tree.MethodExpression;
import sun.tools.tree.NewInstanceExpression;
import sun.tools.tree.ReturnStatement;
import sun.tools.tree.Statement;
import sun.tools.tree.StringExpression;
import sun.tools.tree.SuperExpression;
import sun.tools.tree.ThisExpression;
import sun.tools.tree.ThrowStatement;
import sun.tools.tree.TryStatement;
import sun.tools.tree.TypeExpression;
import sun.tools.tree.UplevelReference;
import sun.tools.tree.Vset;

public final class SourceClass extends ClassDefinition {

	SourceClass(final Environment toplevelEnv, final long l, final ClassDeclaration classdeclaration, final String documentation, final int i, final IdentifierToken identifiertoken, final IdentifierToken aidentifiertoken[], final SourceClass outerClass, final Identifier identifier) {
		super(toplevelEnv.getSource(), l, classdeclaration, i, identifiertoken, aidentifiertoken);
		this.tab = new ConstantPool();
		this.deps = new Hashtable(11);
		this.dummyArgumentType = null;
		this.sourceFileChecked = false;
		this.supersChecked = false;
		this.basicChecking = false;
		this.basicCheckDone = false;
		this.resolving = false;
		this.inlinedLocalClass = false;
		this.lookup = null;
		this.setOuterClass(outerClass);
		this.toplevelEnv = toplevelEnv;
		this.documentation = documentation;
		if (ClassDefinition.containsDeprecated(documentation)) {
			this.modifiers |= Constants.M_DEPRECATED;
		}
		if (this.isStatic() && outerClass == null) {
			toplevelEnv.error(l, "static.class", this);
			this.modifiers &= -9;
		}
		if (this.isLocal() || outerClass != null && !outerClass.isTopLevel()) {
			if (this.isInterface()) {
				toplevelEnv.error(l, "inner.interface");
			} else if (this.isStatic()) {
				toplevelEnv.error(l, "static.inner.class", this);
				this.modifiers &= -9;
				if (this.innerClassMember != null) {
					this.innerClassMember.subModifiers(8);
				}
			}
		}
		if (this.isPrivate() && outerClass == null) {
			toplevelEnv.error(l, "private.class", this);
			this.modifiers &= -3;
		}
		if (this.isProtected() && outerClass == null) {
			toplevelEnv.error(l, "protected.class", this);
			this.modifiers &= -5;
		}
		if (!this.isTopLevel() && !this.isLocal()) {
			final LocalMember localmember = outerClass.getThisArgument();
			final UplevelReference uplevelreference = this.getReference(localmember);
			this.setOuterMember(uplevelreference.getLocalField(toplevelEnv));
		}
		if (identifier != null) {
			this.setLocalName(identifier);
		}
		final Identifier identifier1 = this.getLocalName();
		if (identifier1 != Constants.idNull) {
			for (Object obj = outerClass; obj != null; obj = ((ClassDefinition) obj).getOuterClass()) {
				final Identifier identifier2 = ((ClassDefinition) obj).getLocalName();
				if (identifier1.equals(identifier2)) {
					toplevelEnv.error(l, "inner.redefined", identifier1);
				}
			}

		}
	}

	private long getEndPosition() {
		return this.endPosition;
	}

	public void setEndPosition(final long l) {
		this.endPosition = l;
	}

	public String getAbsoluteName() {
		return ((ClassFile) this.getSource()).getAbsoluteName();
	}

	public Imports getImports() {
		return this.toplevelEnv.getImports();
	}

	public LocalMember getThisArgument() {
		if (this.thisArg == null) {
			this.thisArg = new LocalMember(this.where, this, 0, this.getType(), Constants.idThis);
		}
		return this.thisArg;
	}

	public void addDependency(final ClassDeclaration classdeclaration) {
		if (this.tab != null) {
			this.tab.put(classdeclaration);
		}
		if (this.toplevelEnv.print_dependencies() && classdeclaration != this.getClassDeclaration()) {
			this.deps.put(classdeclaration, classdeclaration);
		}
	}

	public void addMember(final Environment environment, final MemberDefinition memberdefinition) {
		switch (memberdefinition.getModifiers() & 7) {
		case 3: // '\003'
		default:
			environment.error(memberdefinition.getWhere(), "inconsistent.modifier", memberdefinition);
			if (memberdefinition.isPublic()) {
				memberdefinition.subModifiers(6);
			} else {
				memberdefinition.subModifiers(2);
			}
			break;

		case 0: // '\0'
		case 1: // '\001'
		case 2: // '\002'
		case 4: // '\004'
			break;
		}
		if (memberdefinition.isStatic() && !this.isTopLevel() && !memberdefinition.isSynthetic()) {
			if (memberdefinition.isMethod()) {
				environment.error(memberdefinition.getWhere(), "static.inner.method", memberdefinition, this);
				memberdefinition.subModifiers(8);
			} else if (memberdefinition.isVariable()) {
				if (!memberdefinition.isFinal() || memberdefinition.isBlankFinal()) {
					environment.error(memberdefinition.getWhere(), "static.inner.field", memberdefinition.getName(), this);
					memberdefinition.subModifiers(8);
				}
			} else {
				memberdefinition.subModifiers(8);
			}
		}
		if (memberdefinition.isMethod()) {
			if (memberdefinition.isConstructor()) {
				if (memberdefinition.getClassDefinition().isInterface()) {
					environment.error(memberdefinition.getWhere(), "intf.constructor");
					return;
				}
				if (memberdefinition.isNative() || memberdefinition.isAbstract() || memberdefinition.isStatic() || memberdefinition.isSynchronized() || memberdefinition.isFinal()) {
					environment.error(memberdefinition.getWhere(), "constr.modifier", memberdefinition);
					memberdefinition.subModifiers(1336);
				}
			} else if (memberdefinition.isInitializer() && memberdefinition.getClassDefinition().isInterface()) {
				environment.error(memberdefinition.getWhere(), "intf.initializer");
				return;
			}
			if (memberdefinition.getType().getReturnType().isVoidArray()) {
				environment.error(memberdefinition.getWhere(), "void.array");
			}
			if (memberdefinition.getClassDefinition().isInterface() && (memberdefinition.isStatic() || memberdefinition.isSynchronized() || memberdefinition.isNative() || memberdefinition.isFinal() || memberdefinition.isPrivate() || memberdefinition.isProtected())) {
				environment.error(memberdefinition.getWhere(), "intf.modifier.method", memberdefinition);
				memberdefinition.subModifiers(314);
			}
			if (memberdefinition.isTransient()) {
				environment.error(memberdefinition.getWhere(), "transient.meth", memberdefinition);
				memberdefinition.subModifiers(128);
			}
			if (memberdefinition.isVolatile()) {
				environment.error(memberdefinition.getWhere(), "volatile.meth", memberdefinition);
				memberdefinition.subModifiers(64);
			}
			if (memberdefinition.isAbstract()) {
				if (memberdefinition.isPrivate()) {
					environment.error(memberdefinition.getWhere(), "abstract.private.modifier", memberdefinition);
					memberdefinition.subModifiers(2);
				}
				if (memberdefinition.isStatic()) {
					environment.error(memberdefinition.getWhere(), "abstract.static.modifier", memberdefinition);
					memberdefinition.subModifiers(8);
				}
				if (memberdefinition.isFinal()) {
					environment.error(memberdefinition.getWhere(), "abstract.final.modifier", memberdefinition);
					memberdefinition.subModifiers(16);
				}
				if (memberdefinition.isNative()) {
					environment.error(memberdefinition.getWhere(), "abstract.native.modifier", memberdefinition);
					memberdefinition.subModifiers(256);
				}
				if (memberdefinition.isSynchronized()) {
					environment.error(memberdefinition.getWhere(), "abstract.synchronized.modifier", memberdefinition);
					memberdefinition.subModifiers(32);
				}
			}
			if (memberdefinition.isAbstract() || memberdefinition.isNative()) {
				if (memberdefinition.getValue() != null) {
					environment.error(memberdefinition.getWhere(), "invalid.meth.body", memberdefinition);
					memberdefinition.setValue(null);
				}
			} else if (memberdefinition.getValue() == null) {
				if (memberdefinition.isConstructor()) {
					environment.error(memberdefinition.getWhere(), "no.constructor.body", memberdefinition);
				} else {
					environment.error(memberdefinition.getWhere(), "no.meth.body", memberdefinition);
				}
				memberdefinition.addModifiers(1024);
			}
			final List arguments = memberdefinition.getArguments();
			if (arguments != null) {
				arguments.size();
				final Type atype[] = memberdefinition.getType().getArgumentTypes();
				for (int j = 0; j < atype.length; j++) {
					Object obj = arguments.get(j);
					long l = memberdefinition.getWhere();
					if (obj instanceof MemberDefinition) {
						l = ((MemberDefinition) obj).getWhere();
						obj = ((MemberDefinition) obj).getName();
					}
					if (atype[j].isType(11) || atype[j].isVoidArray()) {
						environment.error(l, "void.argument", obj);
					}
				}

			}
		} else if (memberdefinition.isInnerClass()) {
			if (memberdefinition.isVolatile() || memberdefinition.isTransient() || memberdefinition.isNative() || memberdefinition.isSynchronized()) {
				environment.error(memberdefinition.getWhere(), "inner.modifier", memberdefinition);
				memberdefinition.subModifiers(480);
			}
			if (memberdefinition.getClassDefinition().isInterface() && (memberdefinition.isPrivate() || memberdefinition.isProtected())) {
				environment.error(memberdefinition.getWhere(), "intf.modifier.field", memberdefinition);
				memberdefinition.subModifiers(6);
				memberdefinition.addModifiers(1);
				final ClassDefinition classdefinition = memberdefinition.getInnerClass();
				classdefinition.subModifiers(6);
				classdefinition.addModifiers(1);
			}
		} else {
			if (memberdefinition.getType().isType(11) || memberdefinition.getType().isVoidArray()) {
				environment.error(memberdefinition.getWhere(), "void.inst.var", memberdefinition.getName());
				return;
			}
			if (memberdefinition.isSynchronized() || memberdefinition.isAbstract() || memberdefinition.isNative()) {
				environment.error(memberdefinition.getWhere(), "var.modifier", memberdefinition);
				memberdefinition.subModifiers(1312);
			}
			if (memberdefinition.isStrict()) {
				environment.error(memberdefinition.getWhere(), "var.floatmodifier", memberdefinition);
				memberdefinition.subModifiers(0x200000);
			}
			if (memberdefinition.isTransient() && this.isInterface()) {
				environment.error(memberdefinition.getWhere(), "transient.modifier", memberdefinition);
				memberdefinition.subModifiers(128);
			}
			if (memberdefinition.isVolatile() && (this.isInterface() || memberdefinition.isFinal())) {
				environment.error(memberdefinition.getWhere(), "volatile.modifier", memberdefinition);
				memberdefinition.subModifiers(64);
			}
			if (memberdefinition.isFinal() && memberdefinition.getValue() == null && this.isInterface()) {
				environment.error(memberdefinition.getWhere(), "initializer.needed", memberdefinition);
				memberdefinition.subModifiers(16);
			}
			if (memberdefinition.getClassDefinition().isInterface() && (memberdefinition.isPrivate() || memberdefinition.isProtected())) {
				environment.error(memberdefinition.getWhere(), "intf.modifier.field", memberdefinition);
				memberdefinition.subModifiers(6);
				memberdefinition.addModifiers(1);
			}
		}
		if (!memberdefinition.isInitializer()) {
			for (MemberDefinition memberdefinition1 = this.getFirstMatch(memberdefinition.getName()); memberdefinition1 != null; memberdefinition1 = memberdefinition1.getNextMatch()) {
				if (memberdefinition.isVariable() && memberdefinition1.isVariable()) {
					environment.error(memberdefinition.getWhere(), "var.multidef", memberdefinition, memberdefinition1);
					return;
				}
				if (memberdefinition.isInnerClass() && memberdefinition1.isInnerClass() && !memberdefinition.getInnerClass().isLocal() && !memberdefinition1.getInnerClass().isLocal()) {
					environment.error(memberdefinition.getWhere(), "inner.class.multidef", memberdefinition);
					return;
				}
			}

		}
		super.addMember(environment, memberdefinition);
	}

	Environment setupEnv() {
		return new Environment(this.toplevelEnv, this);
	}

	public boolean reportDeprecated(final Environment environment) {
		return false;
	}

	public void noteUsedBy(ClassDefinition classdefinition, final long l, final Environment environment) {
		super.noteUsedBy(classdefinition, l, environment);
		Object obj;
		for (obj = this; ((ClassDefinition) obj).isInnerClass(); obj = ((ClassDefinition) obj).getOuterClass()) {
		}
		if (((ClassDefinition) obj).isPublic()) {
			return;
		}
		for (; classdefinition.isInnerClass(); classdefinition = classdefinition.getOuterClass()) {
		}
		if (((ClassDefinition) obj).getSource().equals(classdefinition.getSource())) {
			return;
		}
		((SourceClass) obj).checkSourceFile(environment, l);
	}

	public void check(final Environment environment) throws ClassNotFound {
		Environment.dtEnter("SourceClass.check: " + this.getName());
		if (this.isInsideLocal()) {
			Environment.dtEvent("SourceClass.check: INSIDE LOCAL " + this.getOuterClass().getName());
			this.getOuterClass().check(environment);
		} else {
			if (this.isInnerClass()) {
				Environment.dtEvent("SourceClass.check: INNER CLASS " + this.getOuterClass().getName());
				((SourceClass) this.getOuterClass()).maybeCheck(environment);
			}
			Environment.dtEvent("SourceClass.check: CHECK INTERNAL " + this.getName());
			final Context context = null;
			this.checkInternal(this.setupEnv(), context, new Vset());
		}
		Environment.dtExit("SourceClass.check: " + this.getName());
	}

	private void maybeCheck(final Environment environment) throws ClassNotFound {
		Environment.dtEvent("SourceClass.maybeCheck: " + this.getName());
		final ClassDeclaration classdeclaration = this.getClassDeclaration();
		if (classdeclaration.getStatus() == 4) {
			classdeclaration.setDefinition(this, 5);
			this.check(environment);
		}
	}

	private Vset checkInternal(final Environment environment, final Context context, Vset vset) throws ClassNotFound {
		final Identifier identifier = this.getClassDeclaration().getName();
		if (environment.verbose()) {
			environment.output("[checking class " + identifier + ']');
		}
		this.classContext = context;
		this.basicCheck(Context.newEnvironment(environment, context));
		final ClassDeclaration classdeclaration = this.getSuperClass();
		if (classdeclaration != null) {
			long l = this.getWhere();
			l = IdentifierToken.getWhere(this.superClassId, l);
			environment.resolveExtendsByName(l, this, classdeclaration.getName());
		}
		for (int i = 0; i < this.interfaces.length; i++) {
			final ClassDeclaration classdeclaration1 = this.interfaces[i];
			long l1 = this.getWhere();
			if (this.interfaceIds != null && this.interfaceIds.length == this.interfaces.length) {
				l1 = IdentifierToken.getWhere(this.interfaceIds[i], l1);
			}
			environment.resolveExtendsByName(l1, this, classdeclaration1.getName());
		}

		if (!this.isInnerClass() && !this.isInsideLocal()) {
			final Identifier identifier1 = identifier.getName();
			try {
				final Imports imports = this.toplevelEnv.getImports();
				final Identifier identifier2 = imports.resolve(environment, identifier1);
				if (identifier2 != this.getName()) {
					environment.error(this.where, "class.multidef.import", identifier1, identifier2);
				}
			} catch (final AmbiguousClass ambiguousclass) {
				final Identifier identifier3 = ambiguousclass.name1 == this.getName() ? ambiguousclass.name2 : ambiguousclass.name1;
				environment.error(this.where, "class.multidef.import", identifier1, identifier3);
			} catch (final ClassNotFound ignored) {
			}
			if (this.isPublic()) {
				this.checkSourceFile(environment, this.getWhere());
			}
		}
		vset = this.checkMembers(environment, context, vset);
		return vset;
	}

	private void checkSourceFile(final Environment environment, final long l) {
		if (this.sourceFileChecked) {
			return;
		}
		this.sourceFileChecked = true;
		final String s = this.getName().getName() + ".java";
		final String s1 = ((ClassFile) this.getSource()).getName();
		if (!s1.equals(s)) {
			if (this.isPublic()) {
				environment.error(l, "public.class.file", this, s);
			} else {
				environment.error(l, "warn.package.class.file", this, s1, s);
			}
		}
	}

	public ClassDeclaration getSuperClass(final Environment environment) {
		Environment.dtEnter("SourceClass.getSuperClass: " + this);
		if (this.superClass == null && this.superClassId != null && !this.supersChecked) {
			this.resolveTypeStructure(environment);
		}
		Environment.dtExit("SourceClass.getSuperClass: " + this);
		return this.superClass;
	}

	private void checkSupers(final Environment environment) {
		label0: {
			this.supersCheckStarted = true;
			Environment.dtEnter("SourceClass.checkSupers: " + this);
			if (this.isInterface()) {
				if (this.isFinal()) {
					final Identifier identifier = this.getClassDeclaration().getName();
					environment.error(this.getWhere(), "final.intf", identifier);
				}
				break label0;
			}
			if (this.getSuperClass(environment) != null) {
				long l = this.getWhere();
				l = IdentifierToken.getWhere(this.superClassId, l);
				try {
					final ClassDefinition classdefinition = this.getSuperClass().getClassDefinition(environment);
					classdefinition.resolveTypeStructure(environment);
					if (!this.extendsCanAccess(environment, this.getSuperClass())) {
						environment.error(l, "cant.access.class", this.getSuperClass());
						this.superClass = null;
					} else if (classdefinition.isFinal()) {
						environment.error(l, "super.is.final", this.getSuperClass());
						this.superClass = null;
					} else if (classdefinition.isInterface()) {
						environment.error(l, "super.is.intf", this.getSuperClass());
						this.superClass = null;
					} else if (this.superClassOf(environment, this.getSuperClass())) {
						environment.error(l, "cyclic.super");
						this.superClass = null;
					} else {
						classdefinition.noteUsedBy(this, l, environment);
					}
					if (this.superClass == null) {
					} else {
						ClassDefinition classdefinition1 = classdefinition;
						do {
							if (this.enclosingClassOf(classdefinition1)) {
								environment.error(l, "super.is.inner");
								this.superClass = null;
								break;
							}
							final ClassDeclaration classdeclaration1 = classdefinition1.getSuperClass(environment);
							if (classdeclaration1 == null) {
								break;
							}
							classdefinition1 = classdeclaration1.getClassDefinition(environment);
						} while (true);
					}
					break label0;
				} catch (final ClassNotFound classnotfound) {
					try {
						environment.resolve(classnotfound.name);
					} catch (final AmbiguousClass ambiguousclass) {
						environment.error(l, "ambig.class", ambiguousclass.name1, ambiguousclass.name2);
						this.superClass = null;
						break label0;
					} catch (final ClassNotFound ignored) {
					}
					environment.error(l, "super.not.found", classnotfound.name, this);
					this.superClass = null;
				}
			} else {
				if (this.isAnonymous()) {
					throw new CompilerError("anonymous super");
				}
				if (!this.getName().equals(Constants.idJavaLangObject)) {
					throw new CompilerError("unresolved super");
				}
			}
		}
		this.supersChecked = true;
		for (int i = 0; i < this.interfaces.length; i++) {
			label1: {
				final ClassDeclaration classdeclaration = this.interfaces[i];
				long l1 = this.getWhere();
				if (this.interfaceIds != null && this.interfaceIds.length == this.interfaces.length) {
					l1 = IdentifierToken.getWhere(this.interfaceIds[i], l1);
				}
				try {
					final ClassDefinition classdefinition2 = classdeclaration.getClassDefinition(environment);
					classdefinition2.resolveTypeStructure(environment);
					if (!this.extendsCanAccess(environment, classdeclaration)) {
						environment.error(l1, "cant.access.class", classdeclaration);
					} else if (!classdeclaration.getClassDefinition(environment).isInterface()) {
						environment.error(l1, "not.intf", classdeclaration);
					} else if (this.isInterface() && this.implementedBy(environment, classdeclaration)) {
						environment.error(l1, "cyclic.intf", classdeclaration);
					} else {
						classdefinition2.noteUsedBy(this, l1, environment);
						continue;
					}
					break label1;
				} catch (final ClassNotFound classnotfound2) {
					try {
						environment.resolve(classnotfound2.name);
					} catch (final AmbiguousClass ambiguousclass1) {
						environment.error(l1, "ambig.class", ambiguousclass1.name1, ambiguousclass1.name2);
						this.superClass = null;
						break label1;
					} catch (final ClassNotFound ignored) {
					}
					environment.error(l1, "intf.not.found", classnotfound2.name, this);
					this.superClass = null;
				}
			}
			final ClassDeclaration aclassdeclaration[] = new ClassDeclaration[this.interfaces.length - 1];
			System.arraycopy(this.interfaces, 0, aclassdeclaration, 0, i);
			System.arraycopy(this.interfaces, i + 1, aclassdeclaration, i, aclassdeclaration.length - i);
			this.interfaces = aclassdeclaration;
			i--;
		}

		Environment.dtExit("SourceClass.checkSupers: " + this);
	}

	private Vset checkMembers(final Environment environment, final Context context, final Vset vset) throws ClassNotFound {
		if (this.getError()) {
			return vset;
		}
		for (MemberDefinition memberdefinition = this.getFirstMember(); memberdefinition != null; memberdefinition = memberdefinition.getNextMember()) {
			if (memberdefinition.isInnerClass()) {
				final SourceClass clazz = (SourceClass) memberdefinition.getInnerClass();
				if (clazz.isMember()) {
					clazz.basicCheck(environment);
				}
			}
		}

		if (this.isFinal() && this.isAbstract()) {
			environment.error(this.where, "final.abstract", this.getName().getName());
		}
		if (!this.isInterface() && !this.isAbstract() && this.mustBeAbstract(environment)) {
			this.modifiers |= 0x400;
			MemberDefinition memberdefinition1;
			for (final Iterator iterator = this.getPermanentlyAbstractMethods(); iterator.hasNext(); environment.error(this.where, "abstract.class.cannot.override", this.getClassDeclaration(), memberdefinition1, memberdefinition1.getDefiningClassDeclaration())) {
				memberdefinition1 = (MemberDefinition) iterator.next();
			}

			for (final Iterator iterator1 = this.getMethods(environment); iterator1.hasNext();) {
				final MemberDefinition memberdefinition2 = (MemberDefinition) iterator1.next();
				if (memberdefinition2.isAbstract()) {
					environment.error(this.where, "abstract.class", this.getClassDeclaration(), memberdefinition2, memberdefinition2.getDefiningClassDeclaration());
				}
			}

		}
		final Context context1 = new Context(context);
		Vset vset1 = vset.copy();
		Vset vset2 = vset.copy();
		for (MemberDefinition memberdefinition3 = this.getFirstMember(); memberdefinition3 != null; memberdefinition3 = memberdefinition3.getNextMember()) {
			if (memberdefinition3.isVariable() && memberdefinition3.isBlankFinal()) {
				final int i = context1.declareFieldNumber(memberdefinition3);
				if (memberdefinition3.isStatic()) {
					vset2 = vset2.addVarUnassigned(i);
					vset1 = vset1.addVar(i);
				} else {
					vset1 = vset1.addVarUnassigned(i);
					vset2 = vset2.addVar(i);
				}
			}
		}

		final Context context2 = new Context(context1, this);
		final LocalMember localmember = this.getThisArgument();
		final int j = context2.declare(environment, localmember);
		vset1 = vset1.addVar(j);
		for (MemberDefinition memberdefinition4 = this.getFirstMember(); memberdefinition4 != null; memberdefinition4 = memberdefinition4.getNextMember()) {
			try {
				if (memberdefinition4.isVariable() || memberdefinition4.isInitializer()) {
					if (memberdefinition4.isStatic()) {
						vset2 = memberdefinition4.check(environment, context1, vset2);
					} else {
						vset1 = memberdefinition4.check(environment, context2, vset1);
					}
				}
			} catch (final ClassNotFound classnotfound) {
				environment.error(memberdefinition4.getWhere(), "class.not.found", classnotfound.name, this);
			}
		}

		this.checkBlankFinals(environment, context1, vset2, true);
		for (MemberDefinition memberdefinition5 = this.getFirstMember(); memberdefinition5 != null; memberdefinition5 = memberdefinition5.getNextMember()) {
			try {
				if (memberdefinition5.isConstructor()) {
					final Vset vset3 = memberdefinition5.check(environment, context1, vset1.copy());
					this.checkBlankFinals(environment, context1, vset3, false);
				} else {
					memberdefinition5.check(environment, context, vset.copy());
				}
			} catch (final ClassNotFound classnotfound1) {
				environment.error(memberdefinition5.getWhere(), "class.not.found", classnotfound1.name, this);
			}
		}

		this.getClassDeclaration().setDefinition(this, 5);
		for (MemberDefinition memberdefinition6 = this.getFirstMember(); memberdefinition6 != null; memberdefinition6 = memberdefinition6.getNextMember()) {
			if (memberdefinition6.isInnerClass()) {
				final SourceClass sourceclass1 = (SourceClass) memberdefinition6.getInnerClass();
				if (!sourceclass1.isInsideLocal()) {
					sourceclass1.maybeCheck(environment);
				}
			}
		}

		return vset;
	}

	private void checkBlankFinals(final Environment environment, final Context context, final Vset vset, final boolean flag) {
		for (int i = 0; i < context.getVarNumber(); i++) {
			if (!vset.testVar(i)) {
				final MemberDefinition memberdefinition = context.getElement(i);
				if (memberdefinition != null && memberdefinition.isBlankFinal() && memberdefinition.isStatic() == flag && memberdefinition.getClassDefinition() == this) {
					environment.error(memberdefinition.getWhere(), "final.var.not.initialized", memberdefinition.getName());
				}
			}
		}

	}

	protected void basicCheck(Environment environment) throws ClassNotFound {
		Environment.dtEnter("SourceClass.basicCheck: " + this.getName());
		super.basicCheck(environment);
		if (this.basicChecking || this.basicCheckDone) {
			Environment.dtExit("SourceClass.basicCheck: OK " + this.getName());
			return;
		}
		Environment.dtEvent("SourceClass.basicCheck: CHECKING " + this.getName());
		this.basicChecking = true;
		environment = this.setupEnv();
		final Imports imports = environment.getImports();
		if (imports != null) {
			imports.resolve(environment);
		}
		this.resolveTypeStructure(environment);
		if (!this.isInterface() && !this.hasConstructor()) {
			final CompoundStatement compoundstatement = new CompoundStatement(this.getWhere(), new Statement[0]);
			final Type type = Type.tMethod(Type.tVoid);
			final int i = this.getModifiers() & (this.isInnerClass() ? 5 : 1);
			environment.makeMemberDefinition(environment, this.getWhere(), this, null, i, type, Constants.idInit, null, null, compoundstatement);
		}
		if (ClassDefinition.doInheritanceChecks) {
			this.collectInheritedMethods(environment);
		}
		this.basicChecking = false;
		this.basicCheckDone = true;
		Environment.dtExit("SourceClass.basicCheck: " + this.getName());
	}

	protected void addMirandaMethods(final Environment environment, final Iterator iterator) {
		for (MemberDefinition memberdefinition; iterator.hasNext(); this.addMember(memberdefinition)) {
			memberdefinition = (MemberDefinition) iterator.next();
		}

	}

	public void resolveTypeStructure(Environment environment) {
		Environment.dtEnter("SourceClass.resolveTypeStructure: " + this.getName());
		final ClassDefinition classdefinition = this.getOuterClass();
		if (classdefinition instanceof SourceClass && !((ClassDefinition) (SourceClass) classdefinition).resolved) {
			((SourceClass) classdefinition).resolveTypeStructure(environment);
		}
		if (this.resolved || this.resolving) {
			Environment.dtExit("SourceClass.resolveTypeStructure: OK " + this.getName());
			return;
		}
		this.resolving = true;
		Environment.dtEvent("SourceClass.resolveTypeStructure: RESOLVING " + this.getName());
		environment = this.setupEnv();
		this.resolveSupers(environment);
		this.checkSupers(environment);
		for (MemberDefinition memberdefinition = this.getFirstMember(); memberdefinition != null; memberdefinition = memberdefinition.getNextMember()) {
			if (memberdefinition instanceof SourceMember) {
				((SourceMember) memberdefinition).resolveTypeStructure(environment);
			}
		}

		this.resolving = false;
		this.resolved = true;
		for (MemberDefinition memberdefinition1 = this.getFirstMember(); memberdefinition1 != null; memberdefinition1 = memberdefinition1.getNextMember()) {
			if (!memberdefinition1.isInitializer() && memberdefinition1.isMethod()) {
				for (MemberDefinition memberdefinition2 = memberdefinition1; (memberdefinition2 = memberdefinition2.getNextMatch()) != null;) {
					if (memberdefinition2.isMethod()) {
						if (memberdefinition1.getType().equals(memberdefinition2.getType())) {
							environment.error(memberdefinition1.getWhere(), "meth.multidef", memberdefinition1);
						} else if (memberdefinition1.getType().equalArguments(memberdefinition2.getType())) {
							environment.error(memberdefinition1.getWhere(), "meth.redef.rettype", memberdefinition1, memberdefinition2);
						}
					}
				}

			}
		}

		Environment.dtExit("SourceClass.resolveTypeStructure: " + this.getName());
	}

	private void resolveSupers(final Environment environment) {
		Environment.dtEnter("SourceClass.resolveSupers: " + this);
		if (this.superClassId != null && this.superClass == null) {
			this.superClass = this.resolveSuper(environment, this.superClassId);
			if (this.superClass == this.getClassDeclaration() && this.getName().equals(Constants.idJavaLangObject)) {
				this.superClass = null;
				this.superClassId = null;
			}
		}
		if (this.interfaceIds != null && this.interfaces == null) {
			this.interfaces = new ClassDeclaration[this.interfaceIds.length];
			for (int i = 0; i < this.interfaces.length; i++) {
				this.interfaces[i] = this.resolveSuper(environment, this.interfaceIds[i]);
				for (int j = 0; j < i; j++) {
					if (this.interfaces[i] == this.interfaces[j]) {
						final Identifier identifier = this.interfaceIds[i].getName();
						final long l = this.interfaceIds[j].getWhere();
						environment.error(l, "intf.repeated", identifier);
					}
				}

			}

		}
		Environment.dtExit("SourceClass.resolveSupers: " + this);
	}

	private ClassDeclaration resolveSuper(final Environment environment, final IdentifierToken identifiertoken) {
		Identifier identifier = identifiertoken.getName();
		Environment.dtEnter("SourceClass.resolveSuper: " + identifier);
		identifier = this.isInnerClass() ? this.outerClass.resolveName(environment, identifier) : environment.resolveName(identifier);
		final ClassDeclaration classdeclaration = environment.getClassDeclaration(identifier);
		Environment.dtExit("SourceClass.resolveSuper: " + identifier);
		return classdeclaration;
	}

	public Vset checkLocalClass(Environment environment, final Context context, Vset vset, final ClassDefinition classdefinition, final Expression aexpression[], final Type atype[]) throws ClassNotFound {
		environment = this.setupEnv();
		if (classdefinition != null != this.isAnonymous()) {
			throw new CompilerError("resolveAnonymousStructure");
		}
		if (this.isAnonymous()) {
			this.resolveAnonymousStructure(environment, classdefinition, aexpression, atype);
		}
		vset = this.checkInternal(environment, context, vset);
		return vset;
	}

	public void inlineLocalClass(final Environment environment) {
		for (MemberDefinition memberdefinition = this.getFirstMember(); memberdefinition != null; memberdefinition = memberdefinition.getNextMember()) {
			if (!memberdefinition.isVariable() && !memberdefinition.isInitializer() || memberdefinition.isStatic()) {
				try {
					((SourceMember) memberdefinition).inline(environment);
				} catch (final ClassNotFound classnotfound) {
					environment.error(memberdefinition.getWhere(), "class.not.found", classnotfound.name, this);
				}
			}
		}

		if (this.getReferencesFrozen() != null && !this.inlinedLocalClass) {
			this.inlinedLocalClass = true;
			for (MemberDefinition memberdefinition1 = this.getFirstMember(); memberdefinition1 != null; memberdefinition1 = memberdefinition1.getNextMember()) {
				if (memberdefinition1.isConstructor()) {
					((SourceMember) memberdefinition1).addUplevelArguments();
				}
			}

		}
	}

	public Vset checkInsideClass(final Environment environment, final Context context, final Vset vset) throws ClassNotFound {
		if (!this.isInsideLocal() || this.isLocal()) {
			throw new CompilerError("checkInsideClass");
		}
		return this.checkInternal(environment, context, vset);
	}

	private void resolveAnonymousStructure(final Environment environment, ClassDefinition classdefinition, final Expression aexpression[], final Type atype[]) throws ClassNotFound {
		Environment.dtEvent("SourceClass.resolveAnonymousStructure: " + this + ", super " + classdefinition);
		if (classdefinition.isInterface()) {
			final int i = this.interfaces != null ? this.interfaces.length : 0;
			final ClassDeclaration aclassdeclaration[] = new ClassDeclaration[1 + i];
			if (i > 0) {
				System.arraycopy(this.interfaces, 0, aclassdeclaration, 1, i);
				if (this.interfaceIds != null && this.interfaceIds.length == i) {
					final IdentifierToken aidentifiertoken1[] = new IdentifierToken[1 + i];
					System.arraycopy(this.interfaceIds, 0, aidentifiertoken1, 1, i);
					aidentifiertoken1[0] = new IdentifierToken(classdefinition.getName());
				}
			}
			aclassdeclaration[0] = classdefinition.getClassDeclaration();
			this.interfaces = aclassdeclaration;
			classdefinition = this.toplevelEnv.getClassDefinition(Constants.idJavaLangObject);
		}
		this.superClass = classdefinition.getClassDeclaration();
		if (this.hasConstructor()) {
			throw new CompilerError("anonymous constructor");
		}
		final Type type = Type.tMethod(Type.tVoid, atype);
		final IdentifierToken aidentifiertoken[] = new IdentifierToken[atype.length];
		for (int j = 0; j < aidentifiertoken.length; j++) {
			aidentifiertoken[j] = new IdentifierToken(aexpression[j].getWhere(), Identifier.lookup("$" + j));
		}

		final int k = !classdefinition.isTopLevel() && !classdefinition.isLocal() ? 1 : 0;
		final Expression aexpression1[] = new Expression[-k + aexpression.length];
		for (int l = k; l < aexpression.length; l++) {
			aexpression1[-k + l] = new IdentifierExpression(aidentifiertoken[l]);
		}

		final long l1 = this.getWhere();
		final SuperExpression superexpression = k == 0 ? new SuperExpression(l1) : new SuperExpression(l1, new IdentifierExpression(aidentifiertoken[0]));
		final MethodExpression methodexpression = new MethodExpression(l1, superexpression, Constants.idInit, aexpression1);
		final Statement astatement[] = { new ExpressionStatement(l1, methodexpression) };
		final CompoundStatement compoundstatement = new CompoundStatement(l1, astatement);
		final int i1 = 0x80000;
		environment.makeMemberDefinition(environment, l1, this, null, i1, type, Constants.idInit, aidentifiertoken, null, compoundstatement);
	}

	private static String classModifierString(int i) {
		String s = "";
		for (int j = 0; j < classModifierBits.length; j++) {
			if ((i & classModifierBits[j]) != 0) {
				s = s + ' ' + classModifierNames[j];
				i &= ~classModifierBits[j];
			}
		}

		if (i != 0) {
			return s + " ILLEGAL:" + Integer.toHexString(i);
		}
		return s;
	}

	public MemberDefinition getAccessMember(final Environment environment, final Context context, final MemberDefinition memberdefinition, final boolean flag) {
		return this.getAccessMember(environment, context, memberdefinition, false, flag);
	}

	public MemberDefinition getUpdateMember(final Environment environment, final Context context, final MemberDefinition memberdefinition, final boolean flag) {
		if (!memberdefinition.isVariable()) {
			throw new CompilerError("method");
		}
		return this.getAccessMember(environment, context, memberdefinition, true, flag);
	}

	private MemberDefinition getAccessMember(final Environment environment, final Context context, final MemberDefinition memberdefinition, final boolean flag, final boolean flag1) {
		final boolean flag2 = memberdefinition.isStatic();
		final boolean flag3 = memberdefinition.isMethod();
		MemberDefinition memberdefinition1;
		for (memberdefinition1 = this.getFirstMember(); memberdefinition1 != null; memberdefinition1 = memberdefinition1.getNextMember()) {
			if (memberdefinition1.getAccessMethodTarget() != memberdefinition) {
				continue;
			}
			if (flag3 && memberdefinition1.isSuperAccessMethod() == flag1) {
				break;
			}
			final int i = memberdefinition1.getType().getArgumentTypes().length;
			if (i == (flag2 ? 0 : 1)) {
				break;
			}
		}

		if (memberdefinition1 != null) {
			if (!flag) {
				return memberdefinition1;
			}
			final MemberDefinition memberdefinition2 = memberdefinition1.getAccessUpdateMember();
			if (memberdefinition2 != null) {
				return memberdefinition2;
			}
		} else if (flag) {
			memberdefinition1 = this.getAccessMember(environment, context, memberdefinition, false, flag1);
		}
		Type type = null;
		Identifier identifier;
		if (memberdefinition.isConstructor()) {
			identifier = Constants.idInit;
			final SourceClass sourceclass = (SourceClass) this.getTopClass();
			type = sourceclass.dummyArgumentType;
			if (type == null) {
				final IdentifierToken identifiertoken = new IdentifierToken(0L, Constants.idJavaLangObject);
				final IdentifierToken identifiertoken1 = new IdentifierToken(0L, Constants.idNull);
				int i1 = 0x90008;
				if (sourceclass.isInterface()) {
					i1 |= 1;
				}
				final IdentifierToken[] aidentifiertoken = new IdentifierToken[0];
				final ClassDefinition classdefinition = this.toplevelEnv.makeClassDefinition(this.toplevelEnv, 0L, identifiertoken1, null, i1, identifiertoken, aidentifiertoken, sourceclass);
				classdefinition.getClassDeclaration().setDefinition(classdefinition, 4);
				try {
					final ClassDefinition classdefinition1 = this.toplevelEnv.getClassDefinition(Constants.idJavaLangObject);
					final Expression[] aexpression = new Expression[0];
					classdefinition.checkLocalClass(this.toplevelEnv, null, new Vset(), classdefinition1, aexpression, Type.noArgs);
				} catch (final ClassNotFound ignored) {
				}
				type = classdefinition.getType();
				sourceclass.dummyArgumentType = type;
			}
		} else {
			int j = 0;
			do {
				identifier = Identifier.lookup("access$" + j);
				if (this.getFirstMatch(identifier) == null) {
					break;
				}
				j++;
			} while (true);
		}
		Type type1 = memberdefinition.getType();
		final Type[] atype;
		if (flag2) {
			if (!flag3) {
				if (!flag) {
					atype = Type.noArgs;
					type1 = Type.tMethod(type1);
				} else {
					atype = new Type[] { type1 };
					type1 = Type.tMethod(Type.tVoid, atype);
				}
			} else {
				atype = type1.getArgumentTypes();
			}
		} else {
			final Type type2 = this.getType();
			if (!flag3) {
				if (!flag) {
					atype = new Type[] { type2 };
					type1 = Type.tMethod(type1, atype);
				} else {
					atype = new Type[] { type2, type1 };
					type1 = Type.tMethod(Type.tVoid, atype);
				}
			} else {
				final Type atype5[] = type1.getArgumentTypes();
				final int j1 = atype5.length;
				if (memberdefinition.isConstructor()) {
					final LocalMember localmember = ((SourceMember) memberdefinition).getOuterThisArg();
					if (localmember != null) {
						if (atype5[0] != localmember.getType()) {
							throw new CompilerError("misplaced outer this");
						}
						atype = new Type[j1];
						atype[0] = type;
						System.arraycopy(atype5, 1, atype, 1, j1 - 1);

					} else {
						atype = new Type[j1 + 1];
						atype[0] = type;
						System.arraycopy(atype5, 0, atype, 1, j1);

					}
				} else {
					atype = new Type[j1 + 1];
					atype[0] = type2;
					System.arraycopy(atype5, 0, atype, 1, j1);

				}
				type1 = Type.tMethod(type1.getReturnType(), atype);
			}
		}
		final int k = atype.length;
		final long l = memberdefinition.getWhere();
		final IdentifierToken aidentifiertoken1[] = new IdentifierToken[k];
		for (int j2 = 0; j2 < k; j2++) {
			aidentifiertoken1[j2] = new IdentifierToken(l, Identifier.lookup("$" + j2));
		}

		Object obj = null;
		final Expression[] aexpression1;
		if (flag2) {
			aexpression1 = new Expression[k];
			for (int k2 = 0; k2 < k; k2++) {
				aexpression1[k2] = new IdentifierExpression(aidentifiertoken1[k2]);
			}

		} else {
			final Object obj2;
			if (memberdefinition.isConstructor()) {
				obj2 = new ThisExpression(l);
				aexpression1 = new Expression[k - 1];
				for (int l2 = 1; l2 < k; l2++) {
					aexpression1[l2 - 1] = new IdentifierExpression(aidentifiertoken1[l2]);
				}

			} else {
				obj2 = new IdentifierExpression(aidentifiertoken1[0]);
				aexpression1 = new Expression[k - 1];
				for (int i3 = 1; i3 < k; i3++) {
					aexpression1[i3 - 1] = new IdentifierExpression(aidentifiertoken1[i3]);
				}

			}
			obj = obj2;
		}
		if (!flag3) {
			obj = new FieldExpression(l, (Expression) obj, memberdefinition);
			if (flag) {
				obj = new AssignExpression(l, (Expression) obj, aexpression1[0]);
			}
		} else {
			obj = new MethodExpression(l, (Expression) obj, memberdefinition, aexpression1, flag1);
		}
		Statement obj3 = type1.getReturnType().isType(11) ? (Statement) new ExpressionStatement(l, (Expression) obj) : new ReturnStatement(l, (Expression) obj);
		final Statement astatement[] = { obj3 };
		obj3 = new CompoundStatement(l, astatement);
		int j3 = 0x80000;
		if (!memberdefinition.isConstructor()) {
			j3 |= 8;
		}
		final SourceMember sourcemember = (SourceMember) environment.makeMemberDefinition(environment, l, this, null, j3, type1, identifier, aidentifiertoken1, memberdefinition.getExceptionIds(), obj3);
		sourcemember.setExceptions(memberdefinition.getExceptions(environment));
		sourcemember.setAccessMethodTarget(memberdefinition);
		if (flag) {
			memberdefinition1.setAccessUpdateMember(sourcemember);
		}
		sourcemember.setIsSuperAccessMethod(flag1);
		final Context context1 = sourcemember.getClassDefinition().getClassContext();
		if (context1 != null) {
			try {
				sourcemember.check(environment, context1, new Vset());
			} catch (final ClassNotFound classnotfound1) {
				environment.error(l, "class.not.found", classnotfound1.name, this);
			}
		}
		return sourcemember;
	}

	private SourceClass findLookupContext() {
		for (MemberDefinition memberdefinition = this.getFirstMember(); memberdefinition != null; memberdefinition = memberdefinition.getNextMember()) {
			if (memberdefinition.isInnerClass()) {
				final SourceClass sourceclass = (SourceClass) memberdefinition.getInnerClass();
				if (!sourceclass.isInterface()) {
					return sourceclass;
				}
			}
		}

		for (MemberDefinition memberdefinition1 = this.getFirstMember(); memberdefinition1 != null; memberdefinition1 = memberdefinition1.getNextMember()) {
			if (memberdefinition1.isInnerClass()) {
				final SourceClass sourceclass1 = ((SourceClass) memberdefinition1.getInnerClass()).findLookupContext();
				if (sourceclass1 != null) {
					return sourceclass1;
				}
			}
		}

		return null;
	}

	public MemberDefinition getClassLiteralLookup(final long l) {
		if (this.lookup != null) {
			return this.lookup;
		}
		if (this.outerClass != null) {
			this.lookup = this.outerClass.getClassLiteralLookup(l);
			return this.lookup;
		}
		SourceClass sourceclass = this;
		boolean flag = false;
		if (this.isInterface()) {
			sourceclass = this.findLookupContext();
			if (sourceclass == null) {
				flag = true;
				final IdentifierToken identifiertoken = new IdentifierToken(l, Constants.idJavaLangObject);
				final IdentifierToken aidentifiertoken[] = new IdentifierToken[0];
				final IdentifierToken identifiertoken1 = new IdentifierToken(l, Constants.idNull);
				final int i = 0x90009;
				sourceclass = (SourceClass) this.toplevelEnv.makeClassDefinition(this.toplevelEnv, l, identifiertoken1, null, i, identifiertoken, aidentifiertoken, this);
			}
		}
		final Identifier identifier = Identifier.lookup("class$");
		final Type atype[] = { Type.tString };
		final long l1 = sourceclass.getWhere();
		final IdentifierToken identifiertoken2 = new IdentifierToken(l1, identifier);
		Expression obj = new IdentifierExpression(identifiertoken2);
		final Expression aexpression[] = { obj };
		final Identifier identifier1 = Identifier.lookup("forName");
		obj = new MethodExpression(l1, new TypeExpression(l1, Type.tClassDesc), identifier1, aexpression);
		Object obj1 = new ReturnStatement(l1, obj);
		final Identifier identifier2 = Identifier.lookup("java.lang.ClassNotFoundException");
		final Identifier identifier3 = Identifier.lookup("java.lang.NoClassDefFoundError");
		final Type type = Type.tClass(identifier2);
		final Type type1 = Type.tClass(identifier3);
		final Identifier identifier4 = Identifier.lookup("getMessage");
		obj = new IdentifierExpression(l1, identifier1);
		obj = new MethodExpression(l1, obj, identifier4, new Expression[0]);
		final Expression aexpression1[] = { obj };
		obj = new NewInstanceExpression(l1, new TypeExpression(l1, type1), aexpression1);
		final Statement catchstatement = new CatchStatement(l1, new TypeExpression(l1, type), new IdentifierToken(identifier1), new ThrowStatement(l1, obj));
		final Statement astatement[] = { catchstatement };
		obj1 = new TryStatement(l1, (Statement) obj1, astatement);
		final Type type2 = Type.tMethod(Type.tClassDesc, atype);
		final IdentifierToken aidentifiertoken1[] = { identifiertoken2 };
		this.lookup = this.toplevelEnv.makeMemberDefinition(this.toplevelEnv, l1, sourceclass, null, 0x80008, type2, identifier, aidentifiertoken1, null, obj1);
		if (flag) {
			if (sourceclass.getClassDeclaration().getStatus() == 5) {
				throw new CompilerError("duplicate check");
			}
			sourceclass.getClassDeclaration().setDefinition(sourceclass, 4);
			try {
				final ClassDefinition classdefinition = this.toplevelEnv.getClassDefinition(Constants.idJavaLangObject);
				final Expression[] aexpression2 = new Expression[0];
				sourceclass.checkLocalClass(this.toplevelEnv, null, new Vset(), classdefinition, aexpression2, Type.noArgs);
			} catch (final ClassNotFound ignored) {
			}
		}
		return this.lookup;
	}

	public void compile(final OutputStream outputstream) throws InterruptedException, IOException {
		synchronized (active) {
			for (; active.contains(this.getName()); active.wait()) {
			}
			active.addElement(this.getName());
		}
		try {
			this.compileClass(this.toplevelEnv, outputstream);
		} catch (final ClassNotFound classnotfound) {
			throw new CompilerError(classnotfound);
		} finally {
			synchronized (active) {
				active.removeElement(this.getName());
				active.notifyAll();
			}
		}
	}

	private static void assertModifiers(final int i, final int j) {
		if ((i & j) != j) {
			throw new CompilerError("illegal class modifiers");
		}
	}

	private void compileClass(final Environment environment, final OutputStream outputstream) throws IOException, ClassNotFound {
		final Vector vector2 = new Vector();
		final CompilerMember compilermember = new CompilerMember(new MemberDefinition(this.getWhere(), this, 8, Type.tMethod(Type.tVoid), Constants.idClassInit, null, null), new Assembler());
		final Context context = new Context((Context) null, compilermember.field);
		for (Object obj = this; ((ClassDefinition) obj).isInnerClass(); obj = ((ClassDefinition) obj).getOuterClass()) {
			vector2.addElement(obj);
		}

		final int i = vector2.size();
		for (int j = i; --j >= 0;) {
			vector2.addElement(vector2.elementAt(j));
		}

		for (int k = i; --k >= 0;) {
			vector2.removeElementAt(k);
		}

		boolean flag = this.isDeprecated();
		boolean flag1 = this.isSynthetic();
		boolean flag2 = false;
		boolean flag3 = false;
		final Vector vector1 = new Vector();
		final Vector vector = new Vector();
		for (SourceMember sourcemember = (SourceMember) this.getFirstMember(); sourcemember != null; sourcemember = (SourceMember) sourcemember.getNextMember()) {
			flag |= sourcemember.isDeprecated();
			flag1 |= sourcemember.isSynthetic();
			try {
				if (sourcemember.isMethod()) {
					flag3 |= sourcemember.getExceptions(environment).length > 0;
					if (sourcemember.isInitializer()) {
						if (sourcemember.isStatic()) {
							sourcemember.code(environment, compilermember.asm);
						}
					} else {
						final CompilerMember compilermember1 = new CompilerMember(sourcemember, new Assembler());
						sourcemember.code(environment, compilermember1.asm);
						vector1.addElement(compilermember1);
					}
				} else if (sourcemember.isInnerClass()) {
					vector2.addElement(sourcemember.getInnerClass());
				} else if (sourcemember.isVariable()) {
					sourcemember.inline(environment);
					final CompilerMember compilermember2 = new CompilerMember(sourcemember, null);
					vector.addElement(compilermember2);
					if (sourcemember.isStatic()) {
						sourcemember.codeInit(environment, context, compilermember.asm);
					}
					flag2 |= sourcemember.getInitialValue() != null;
				}
			} catch (final CompilerError compilererror) {
				compilererror.printStackTrace();
				environment.error(sourcemember, 0L, "generic", sourcemember.getClassDeclaration() + ":" + sourcemember + '@' + compilererror, null, null);
			}
		}

		if (!compilermember.asm.empty()) {
			compilermember.asm.add(this.getWhere(), 177, true);
			vector1.addElement(compilermember);
		}
		if (this.getNestError()) {
			return;
		}
		if (!vector1.isEmpty()) {
			this.tab.put("Code");
		}
		if (flag2) {
			this.tab.put("ConstantValue");
		}
		String s = null;
		int l = 0;
		if (environment.debug_source()) {
			s = ((ClassFile) this.getSource()).getName();
			this.tab.put("SourceFile");
			this.tab.put(s);
			l++;
		}
		if (flag3) {
			this.tab.put("Exceptions");
		}
		if (environment.debug_lines()) {
			this.tab.put("LineNumberTable");
		}
		if (flag) {
			this.tab.put("Deprecated");
			if (this.isDeprecated()) {
				l++;
			}
		}
		if (flag1) {
			this.tab.put("Synthetic");
			if (this.isSynthetic()) {
				l++;
			}
		}
		if (environment.coverage()) {
			l += 2;
			this.tab.put("AbsoluteSourcePath");
			this.tab.put("TimeStamp");
			this.tab.put("CoverageTable");
		}
		if (environment.debug_vars()) {
			this.tab.put("LocalVariableTable");
		}
		if (!vector2.isEmpty()) {
			this.tab.put("InnerClasses");
			l++;
		}
		String s1 = "";
		long l1 = 0L;
		if (environment.coverage()) {
			s1 = this.getAbsoluteName();
			l1 = System.currentTimeMillis();
			this.tab.put(s1);
		}
		this.tab.put(this.getClassDeclaration());
		if (this.getSuperClass() != null) {
			this.tab.put(this.getSuperClass());
		}
		for (int i1 = 0; i1 < this.interfaces.length; i1++) {
			this.tab.put(this.interfaces[i1]);
		}

		final CompilerMember acompilermember[] = new CompilerMember[vector1.size()];
		vector1.copyInto(acompilermember);
		Arrays.sort(acompilermember);
		for (int j1 = 0; j1 < vector1.size(); j1++) {
			vector1.setElementAt(acompilermember[j1], j1);
		}

		for (final Iterator iterator = vector1.iterator(); iterator.hasNext();) {
			final CompilerMember compilermember3 = (CompilerMember) iterator.next();
			try {
				compilermember3.asm.optimize(environment);
				compilermember3.asm.collect(environment, compilermember3.field, this.tab);
				this.tab.put(compilermember3.name);
				this.tab.put(compilermember3.sig);
				final ClassDeclaration aclassdeclaration[] = compilermember3.field.getExceptions(environment);
				for (int k1 = 0; k1 < aclassdeclaration.length; k1++) {
					this.tab.put(aclassdeclaration[k1]);
				}

			} catch (final Exception exception) {
				exception.printStackTrace();
				environment.error(compilermember3.field, -1L, "generic", compilermember3.field.getName() + "@" + exception, null, null);
				compilermember3.asm.listing(System.out);
			}
		}

		for (final Iterator iterator = vector.iterator(); iterator.hasNext();) {
			final CompilerMember compilermember4 = (CompilerMember) iterator.next();
			this.tab.put(compilermember4.name);
			this.tab.put(compilermember4.sig);
			final Object obj1 = compilermember4.field.getInitialValue();
			if (obj1 != null) {
				this.tab.put(obj1 instanceof String ? (Object) new StringExpression(compilermember4.field.getWhere(), (String) obj1) : obj1);
			}
		}

		for (final Iterator iterator = vector2.iterator(); iterator.hasNext();) {
			final ClassDefinition classdefinition = (ClassDefinition) iterator.next();
			this.tab.put(classdefinition.getClassDeclaration());
			if (!classdefinition.isLocal()) {
				final ClassDefinition classdefinition1 = classdefinition.getOuterClass();
				this.tab.put(classdefinition1.getClassDeclaration());
			}
			final Identifier identifier = classdefinition.getLocalName();
			if (identifier != Constants.idNull) {
				this.tab.put(identifier.toString());
			}
		}

		final DataOutputStream dataoutputstream = new DataOutputStream(outputstream);
		dataoutputstream.writeInt(0xcafebabe);
		dataoutputstream.writeShort(this.toplevelEnv.getMinorVersion());
		dataoutputstream.writeShort(this.toplevelEnv.getMajorVersion());
		this.tab.write(environment, dataoutputstream);
		int i2 = this.getModifiers() & 0x200611;
		if (this.isInterface()) {
			assertModifiers(i2, 1024);
		} else {
			i2 |= 0x20;
		}
		if (this.outerClass != null) {
			if (this.isProtected()) {
				i2 |= 1;
			}
			if (this.outerClass.isInterface()) {
				assertModifiers(i2, 1);
			}
		}
		dataoutputstream.writeShort(i2);
		if (Environment.dumpModifiers()) {
			final Identifier identifier1 = this.getName();
			final Identifier identifier2 = Identifier.lookup(identifier1.getQualifier(), identifier1.getFlatName());
			System.out.println();
			System.out.println("CLASSFILE  " + identifier2);
			System.out.println("---" + classModifierString(i2));
		}
		dataoutputstream.writeShort(this.tab.index(this.getClassDeclaration()));
		dataoutputstream.writeShort(this.getSuperClass() == null ? 0 : this.tab.index(this.getSuperClass()));
		dataoutputstream.writeShort(this.interfaces.length);
		for (int j2 = 0; j2 < this.interfaces.length; j2++) {
			dataoutputstream.writeShort(this.tab.index(this.interfaces[j2]));
		}

		final ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream(256);
		final ByteArrayOutputStream bytearrayoutputstream1 = new ByteArrayOutputStream(256);
		final DataOutputStream dataoutputstream1 = new DataOutputStream(bytearrayoutputstream);
		dataoutputstream.writeShort(vector.size());
		for (final Iterator iterator = vector.iterator(); iterator.hasNext();) {
			final CompilerMember compilermember5 = (CompilerMember) iterator.next();
			final Object obj2 = compilermember5.field.getInitialValue();
			dataoutputstream.writeShort(compilermember5.field.getModifiers() & 0xdf);
			dataoutputstream.writeShort(this.tab.index(compilermember5.name));
			dataoutputstream.writeShort(this.tab.index(compilermember5.sig));
			int k2 = obj2 == null ? 0 : 1;
			final boolean flag4 = compilermember5.field.isDeprecated();
			final boolean flag5 = compilermember5.field.isSynthetic();
			k2 += (flag4 ? 1 : 0) + (flag5 ? 1 : 0);
			dataoutputstream.writeShort(k2);
			if (obj2 != null) {
				dataoutputstream.writeShort(this.tab.index("ConstantValue"));
				dataoutputstream.writeInt(2);
				dataoutputstream.writeShort(this.tab.index(obj2 instanceof String ? (Object) new StringExpression(compilermember5.field.getWhere(), (String) obj2) : obj2));
			}
			if (flag4) {
				dataoutputstream.writeShort(this.tab.index("Deprecated"));
				dataoutputstream.writeInt(0);
			}
			if (flag5) {
				dataoutputstream.writeShort(this.tab.index("Synthetic"));
				dataoutputstream.writeInt(0);
			}
		}

		dataoutputstream.writeShort(vector1.size());
		for (final Iterator iterator = vector1.iterator(); iterator.hasNext();) {
			final CompilerMember compilermember6 = (CompilerMember) iterator.next();
			int l2 = compilermember6.field.getModifiers() & 0x20053f;
			if ((l2 & 0x200000) != 0 || (i2 & 0x200000) != 0 || environment.strictdefault()) {
				l2 |= 0x800;
			}
			dataoutputstream.writeShort(l2);
			dataoutputstream.writeShort(this.tab.index(compilermember6.name));
			dataoutputstream.writeShort(this.tab.index(compilermember6.sig));
			final ClassDeclaration aclassdeclaration1[] = compilermember6.field.getExceptions(environment);
			int i3 = aclassdeclaration1.length <= 0 ? 0 : 1;
			final boolean flag6 = compilermember6.field.isDeprecated();
			final boolean flag7 = compilermember6.field.isSynthetic();
			i3 += (flag6 ? 1 : 0) + (flag7 ? 1 : 0);
			if (!compilermember6.asm.empty()) {
				dataoutputstream.writeShort(i3 + 1);
				compilermember6.asm.write(environment, dataoutputstream1, compilermember6.field, this.tab);
				int k3 = 0;
				if (environment.debug_lines()) {
					k3++;
				}
				if (environment.coverage()) {
					k3++;
				}
				if (environment.debug_vars()) {
					k3++;
				}
				dataoutputstream1.writeShort(k3);
				if (environment.debug_lines()) {
					compilermember6.asm.writeLineNumberTable(environment, new DataOutputStream(bytearrayoutputstream1), this.tab);
					dataoutputstream1.writeShort(this.tab.index("LineNumberTable"));
					dataoutputstream1.writeInt(bytearrayoutputstream1.size());
					bytearrayoutputstream1.writeTo(bytearrayoutputstream);
					bytearrayoutputstream1.reset();
				}
				if (environment.coverage()) {
					compilermember6.asm.writeCoverageTable(environment, this, new DataOutputStream(bytearrayoutputstream1), this.tab, compilermember6.field.getWhere());
					dataoutputstream1.writeShort(this.tab.index("CoverageTable"));
					dataoutputstream1.writeInt(bytearrayoutputstream1.size());
					bytearrayoutputstream1.writeTo(bytearrayoutputstream);
					bytearrayoutputstream1.reset();
				}
				if (environment.debug_vars()) {
					compilermember6.asm.writeLocalVariableTable(environment, compilermember6.field, new DataOutputStream(bytearrayoutputstream1), this.tab);
					dataoutputstream1.writeShort(this.tab.index("LocalVariableTable"));
					dataoutputstream1.writeInt(bytearrayoutputstream1.size());
					bytearrayoutputstream1.writeTo(bytearrayoutputstream);
					bytearrayoutputstream1.reset();
				}
				dataoutputstream.writeShort(this.tab.index("Code"));
				dataoutputstream.writeInt(bytearrayoutputstream.size());
				bytearrayoutputstream.writeTo(dataoutputstream);
				bytearrayoutputstream.reset();
			} else {
				if (environment.coverage() && (compilermember6.field.getModifiers() & 0x100) > 0) {
					Assembler.addNativeToJcovTab(environment, this);
				}
				dataoutputstream.writeShort(i3);
			}
			if (aclassdeclaration1.length > 0) {
				dataoutputstream.writeShort(this.tab.index("Exceptions"));
				dataoutputstream.writeInt(2 + aclassdeclaration1.length * 2);
				dataoutputstream.writeShort(aclassdeclaration1.length);
				for (int l3 = 0; l3 < aclassdeclaration1.length; l3++) {
					dataoutputstream.writeShort(this.tab.index(aclassdeclaration1[l3]));
				}

			}
			if (flag6) {
				dataoutputstream.writeShort(this.tab.index("Deprecated"));
				dataoutputstream.writeInt(0);
			}
			if (flag7) {
				dataoutputstream.writeShort(this.tab.index("Synthetic"));
				dataoutputstream.writeInt(0);
			}
		}

		dataoutputstream.writeShort(l);
		if (environment.debug_source()) {
			dataoutputstream.writeShort(this.tab.index("SourceFile"));
			dataoutputstream.writeInt(2);
			dataoutputstream.writeShort(this.tab.index(s));
		}
		if (this.isDeprecated()) {
			dataoutputstream.writeShort(this.tab.index("Deprecated"));
			dataoutputstream.writeInt(0);
		}
		if (this.isSynthetic()) {
			dataoutputstream.writeShort(this.tab.index("Synthetic"));
			dataoutputstream.writeInt(0);
		}
		if (environment.coverage()) {
			dataoutputstream.writeShort(this.tab.index("AbsoluteSourcePath"));
			dataoutputstream.writeInt(2);
			dataoutputstream.writeShort(this.tab.index(s1));
			dataoutputstream.writeShort(this.tab.index("TimeStamp"));
			dataoutputstream.writeInt(8);
			dataoutputstream.writeLong(l1);
		}
		if (!vector2.isEmpty()) {
			dataoutputstream.writeShort(this.tab.index("InnerClasses"));
			dataoutputstream.writeInt(2 + 8 * vector2.size());
			dataoutputstream.writeShort(vector2.size());
			for (final Iterator iterator = vector2.iterator(); iterator.hasNext();) {
				final ClassDefinition classdefinition2 = (ClassDefinition) iterator.next();
				dataoutputstream.writeShort(this.tab.index(classdefinition2.getClassDeclaration()));
				if (classdefinition2.isLocal() || classdefinition2.isAnonymous()) {
					dataoutputstream.writeShort(0);
				} else {
					final ClassDefinition classdefinition3 = classdefinition2.getOuterClass();
					dataoutputstream.writeShort(this.tab.index(classdefinition3.getClassDeclaration()));
				}
				final Identifier identifier3 = classdefinition2.getLocalName();
				if (identifier3 == Constants.idNull) {
					if (!classdefinition2.isAnonymous()) {
						throw new CompilerError("compileClass(), anonymous");
					}
					dataoutputstream.writeShort(0);
				} else {
					dataoutputstream.writeShort(this.tab.index(identifier3.toString()));
				}
				int j3 = classdefinition2.getInnerClassMember().getModifiers() & 0xe1f;
				if (classdefinition2.isInterface()) {
					assertModifiers(j3, 1032);
				}
				if (classdefinition2.getOuterClass().isInterface()) {
					j3 &= -7;
					assertModifiers(j3, 9);
				}
				dataoutputstream.writeShort(j3);
				if (Environment.dumpModifiers()) {
					final Identifier identifier4 = classdefinition2.getInnerClassMember().getName();
					final Identifier identifier5 = Identifier.lookup(identifier4.getQualifier(), identifier4.getFlatName());
					System.out.println("INNERCLASS " + identifier5);
					System.out.println("---" + classModifierString(j3));
				}
			}

		}
		dataoutputstream.flush();
		this.tab = null;
		if (environment.covdata()) {
			Assembler.GenVecJCov(environment, this, l1);
		}
	}

	public void printClassDependencies(final Environment environment) {
		if (this.toplevelEnv.print_dependencies()) {
			final String s = ((ClassFile) this.getSource()).getAbsoluteName();
			final String s1 = Type.mangleInnerType(this.getName()).toString();
			final long l = this.getWhere() >> 32;
			final long l1 = this.getEndPosition() >> 32;
			System.out.println("CLASS:" + s + ',' + l + ',' + l1 + ',' + s1);
			String s2;
			for (final Iterator iterator = this.deps.values().iterator(); iterator.hasNext(); environment.output("CLDEP:" + s1 + ',' + s2)) {
				final ClassDeclaration classdeclaration = (ClassDeclaration) iterator.next();
				s2 = Type.mangleInnerType(classdeclaration.getName()).toString();
			}

		}
	}

	private final Environment toplevelEnv;
	private ConstantPool tab;
	private final Map deps;
	private LocalMember thisArg;
	private long endPosition;
	private Type dummyArgumentType;
	private boolean sourceFileChecked;
	private boolean supersChecked;
	private boolean basicChecking;
	private boolean basicCheckDone;
	private boolean resolving;
	private boolean inlinedLocalClass;
	private static final int[] classModifierBits = { 1, 2, 4, 8, 16, 512, 1024, 32, 0x10000, 0x20000, 0x200000, 2048 };
	private static final String[] classModifierNames = { "PUBLIC", "PRIVATE", "PROTECTED", "STATIC", "FINAL", "INTERFACE", "ABSTRACT", "SUPER", "ANONYMOUS", "LOCAL", "STRICTFP", "STRICT" };
	private MemberDefinition lookup;
	private static final Vector active = new Vector();

}
