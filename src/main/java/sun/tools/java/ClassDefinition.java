package sun.tools.java;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import sun.tools.javac.SourceClass;
import sun.tools.javac.SourceMember;
import sun.tools.tree.Context;
import sun.tools.tree.Expression;
import sun.tools.tree.LocalMember;
import sun.tools.tree.UplevelReference;
import sun.tools.tree.Vset;

public class ClassDefinition {

	public Context getClassContext() {
		return this.classContext;
	}

	protected ClassDefinition(final Object obj, final long l, final ClassDeclaration declaration, final int i, final IdentifierToken identifiertoken, final IdentifierToken aidentifiertoken[]) {
		this.fieldHash = new Hashtable(31);
		this.localClasses = null;
		this.supersCheckStarted = !(this instanceof SourceClass);
		this.allMethods = null;
		this.permanentlyAbstractMethods = new ArrayList();
		this.source = obj;
		this.where = l;
		this.declaration = declaration;
		this.modifiers = i;
		this.superClassId = identifiertoken;
		this.interfaceIds = aidentifiertoken;
	}

	public final Object getSource() {
		return this.source;
	}

	public final boolean getError() {
		return this.error;
	}

	public final void setError() {
		this.error = true;
		this.setNestError();
	}

	public final boolean getNestError() {
		return this.nestError || this.outerClass != null && this.outerClass.getNestError();
	}

	private void setNestError() {
		this.nestError = true;
		if (this.outerClass != null) {
			this.outerClass.setNestError();
		}
	}

	public final long getWhere() {
		return this.where;
	}

	public final ClassDeclaration getClassDeclaration() {
		return this.declaration;
	}

	public final int getModifiers() {
		return this.modifiers;
	}

	public final void subModifiers(final int i) {
		this.modifiers &= ~i;
	}

	public final void addModifiers(final int i) {
		this.modifiers |= i;
	}

	public final ClassDeclaration getSuperClass() {
		if (!this.supersCheckStarted) {
			throw new CompilerError("unresolved super");
		}
		return this.superClass;
	}

	public ClassDeclaration getSuperClass(final Environment environment) {
		return this.getSuperClass();
	}

	private ClassDeclaration[] getInterfaces() {
		if (this.interfaces == null) {
			throw new CompilerError("getInterfaces");
		}
		return this.interfaces;
	}

	public final ClassDefinition getOuterClass() {
		return this.outerClass;
	}

	protected final void setOuterClass(final ClassDefinition classdefinition) {
		if (this.outerClass != null) {
			throw new CompilerError("setOuterClass");
		}
		this.outerClass = classdefinition;
	}

	protected final void setOuterMember(final MemberDefinition memberdefinition) {
		if (this.isStatic() || !this.isInnerClass()) {
			throw new CompilerError("setOuterField");
		}
		if (this.outerMember != null) {
			throw new CompilerError("setOuterField");
		}
		this.outerMember = memberdefinition;
	}

	public final boolean isInnerClass() {
		return this.outerClass != null;
	}

	public final boolean isMember() {
		return this.outerClass != null && !this.isLocal();
	}

	public final boolean isTopLevel() {
		return this.outerClass == null || this.isStatic() || this.isInterface();
	}

	public final boolean isInsideLocal() {
		return this.isLocal() || this.outerClass != null && this.outerClass.isInsideLocal();
	}

	public Identifier getLocalName() {
		return this.localName != null ? this.localName : this.getName().getFlatName().getName();
	}

	public void setLocalName(final Identifier identifier) {
		if (this.isLocal()) {
			this.localName = identifier;
		}
	}

	public final MemberDefinition getInnerClassMember() {
		if (this.outerClass == null) {
			return null;
		}
		if (this.innerClassMember == null) {
			final Identifier identifier = this.getName().getFlatName().getName();
			for (MemberDefinition memberdefinition = this.outerClass.getFirstMatch(identifier); memberdefinition != null; memberdefinition = memberdefinition.getNextMatch()) {
				if (!memberdefinition.isInnerClass()) {
					continue;
				}
				this.innerClassMember = memberdefinition;
				break;
			}

			if (this.innerClassMember == null) {
				throw new CompilerError("getInnerClassField");
			}
		}
		return this.innerClassMember;
	}

	public final MemberDefinition findOuterMember() {
		return this.outerMember;
	}

	public final boolean isStatic() {
		return (this.modifiers & 8) != 0;
	}

	public final ClassDefinition getTopClass() {
		ClassDefinition classdefinition;
		ClassDefinition classdefinition1;
		for (classdefinition = this; (classdefinition1 = classdefinition.outerClass) != null; classdefinition = classdefinition1) {
		}
		return classdefinition;
	}

	public final MemberDefinition getFirstMember() {
		return this.firstMember;
	}

	public final MemberDefinition getFirstMatch(final Identifier identifier) {
		return (MemberDefinition) this.fieldHash.get(identifier);
	}

	public final Identifier getName() {
		return this.declaration.getName();
	}

	public final Type getType() {
		return this.declaration.getType();
	}

	public String getDocumentation() {
		return this.documentation;
	}

	public static boolean containsDeprecated(final String s) {
		if (s == null) {
			return false;
		}
		label0: for (int i = 0; (i = s.indexOf("@deprecated", i)) >= 0; i += "@deprecated".length()) {
			for (int j = i - 1; j >= 0; j--) {
				final char c = s.charAt(j);
				if (c == '\n' || c == '\r') {
					break;
				}
				if (!Character.isWhitespace(c)) {
					continue label0;
				}
			}

			final int k = i + "@deprecated".length();
			if (k < s.length()) {
				final char c1 = s.charAt(k);
				if (c1 != '\n' && c1 != '\r' && !Character.isWhitespace(c1)) {
					continue;
				}
			}
			return true;
		}

		return false;
	}

	private boolean inSamePackage(final ClassDeclaration classdeclaration) {
		return this.inSamePackage(classdeclaration.getName().getQualifier());
	}

	public final boolean inSamePackage(final ClassDefinition classdefinition) {
		return this.inSamePackage(classdefinition.getName().getQualifier());
	}

	private boolean inSamePackage(final Identifier identifier) {
		return this.getName().getQualifier().equals(identifier);
	}

	public final boolean isInterface() {
		return (this.getModifiers() & 0x200) != 0;
	}

	public final boolean isClass() {
		return (this.getModifiers() & 0x200) == 0;
	}

	public final boolean isPublic() {
		return (this.getModifiers() & 1) != 0;
	}

	public final boolean isPrivate() {
		return (this.getModifiers() & 2) != 0;
	}

	public final boolean isProtected() {
		return (this.getModifiers() & 4) != 0;
	}

	public final boolean isPackagePrivate() {
		return (this.modifiers & 7) == 0;
	}

	public final boolean isFinal() {
		return (this.getModifiers() & 0x10) != 0;
	}

	public final boolean isAbstract() {
		return (this.getModifiers() & 0x400) != 0;
	}

	public final boolean isSynthetic() {
		return (this.getModifiers() & 0x80000) != 0;
	}

	public final boolean isDeprecated() {
		return (this.getModifiers() & Constants.M_DEPRECATED) != 0;
	}

	public final boolean isAnonymous() {
		return (this.getModifiers() & 0x10000) != 0;
	}

	public final boolean isLocal() {
		return (this.getModifiers() & 0x20000) != 0;
	}

	public final boolean hasConstructor() {
		return this.getFirstMatch(Constants.idInit) != null;
	}

	public final boolean mustBeAbstract(final Environment environment) {
		if (this.isAbstract()) {
			return true;
		}
		this.collectInheritedMethods(environment);
		for (final Iterator iterator = this.getMethods(); iterator.hasNext();) {
			final MemberDefinition memberdefinition = (MemberDefinition) iterator.next();
			if (memberdefinition.isAbstract()) {
				return true;
			}
		}

		return this.getPermanentlyAbstractMethods().hasNext();
	}

	public boolean superClassOf(final Environment environment, ClassDeclaration classdeclaration) throws ClassNotFound {
		for (; classdeclaration != null; classdeclaration = classdeclaration.getClassDefinition(environment).getSuperClass()) {
			if (this.getClassDeclaration().equals(classdeclaration)) {
				return true;
			}
		}

		return false;
	}

	public boolean enclosingClassOf(ClassDefinition classdefinition) {
		while ((classdefinition = classdefinition.getOuterClass()) != null) {
			if (this == classdefinition) {
				return true;
			}
		}
		return false;
	}

	public boolean subClassOf(final Environment environment, final ClassDeclaration classdeclaration) throws ClassNotFound {
		for (ClassDeclaration classdeclaration1 = this.getClassDeclaration(); classdeclaration1 != null; classdeclaration1 = classdeclaration1.getClassDefinition(environment).getSuperClass()) {
			if (classdeclaration1.equals(classdeclaration)) {
				return true;
			}
		}

		return false;
	}

	public boolean implementedBy(final Environment environment, ClassDeclaration classdeclaration) throws ClassNotFound {
		for (; classdeclaration != null; classdeclaration = classdeclaration.getClassDefinition(environment).getSuperClass()) {
			if (this.getClassDeclaration().equals(classdeclaration)) {
				return true;
			}
			final ClassDeclaration aclassdeclaration[] = classdeclaration.getClassDefinition(environment).getInterfaces();
			for (int i = 0; i < aclassdeclaration.length; i++) {
				if (this.implementedBy(environment, aclassdeclaration[i])) {
					return true;
				}
			}

		}

		return false;
	}

	public boolean couldImplement(final ClassDefinition classdefinition) {
		if (!doInheritanceChecks) {
			throw new CompilerError("couldImplement: no checks");
		}
		if (!this.isInterface() || !classdefinition.isInterface()) {
			throw new CompilerError("couldImplement: not interface");
		}
		if (this.allMethods == null) {
			throw new CompilerError("couldImplement: called early");
		}
		for (final Iterator iterator = classdefinition.getMethods(); iterator.hasNext();) {
			final MemberDefinition memberdefinition = (MemberDefinition) iterator.next();
			final Identifier identifier = memberdefinition.getName();
			final Type type = memberdefinition.getType();
			final MemberDefinition memberdefinition1 = this.allMethods.lookupSig(identifier, type);
			if (memberdefinition1 != null && !memberdefinition1.sameReturnType(memberdefinition)) {
				return false;
			}
		}

		return true;
	}

	public boolean extendsCanAccess(final Environment environment, final ClassDeclaration classdeclaration) throws ClassNotFound {
		if (this.outerClass != null) {
			return this.outerClass.canAccess(environment, classdeclaration);
		}
		final ClassDefinition classdefinition = classdeclaration.getClassDefinition(environment);
		if (classdefinition.isLocal()) {
			throw new CompilerError("top local");
		}
		if (classdefinition.isInnerClass()) {
			final MemberDefinition memberdefinition = classdefinition.getInnerClassMember();
			return memberdefinition.isPublic() || (memberdefinition.isPrivate() ? this.getClassDeclaration().equals(memberdefinition.getTopClass().getClassDeclaration()) : this.getName().getQualifier().equals(memberdefinition.getClassDeclaration().getName().getQualifier()));
		}
		return classdefinition.isPublic() || this.getName().getQualifier().equals(classdeclaration.getName().getQualifier());
	}

	public boolean canAccess(final Environment environment, final ClassDeclaration classdeclaration) throws ClassNotFound {
		final ClassDefinition classdefinition = classdeclaration.getClassDefinition(environment);
		if (classdefinition.isLocal()) {
			return true;
		}
		if (classdefinition.isInnerClass()) {
			return this.canAccess(environment, classdefinition.getInnerClassMember());
		}
		return classdefinition.isPublic() || this.getName().getQualifier().equals(classdeclaration.getName().getQualifier());
	}

	public boolean canAccess(final Environment environment, final MemberDefinition memberdefinition) throws ClassNotFound {
		return memberdefinition.isPublic() || memberdefinition.isProtected() && this.subClassOf(environment, memberdefinition.getClassDeclaration()) || (memberdefinition.isPrivate() ? this.getTopClass().getClassDeclaration().equals(memberdefinition.getTopClass().getClassDeclaration()) : this.getName().getQualifier().equals(memberdefinition.getClassDeclaration().getName().getQualifier()));
	}

	public boolean permitInlinedAccess(final Environment environment, final ClassDeclaration classdeclaration) throws ClassNotFound {
		return environment.opt() && classdeclaration.equals(this.declaration) || environment.opt_interclass() && this.canAccess(environment, classdeclaration);
	}

	public boolean permitInlinedAccess(final Environment environment, final MemberDefinition memberdefinition) throws ClassNotFound {
		return environment.opt() && memberdefinition.clazz.getClassDeclaration().equals(this.declaration) || environment.opt_interclass() && this.canAccess(environment, memberdefinition);
	}

	public boolean protectedAccess(final Environment environment, final MemberDefinition memberdefinition, final Type type) throws ClassNotFound {
		return memberdefinition.isStatic() || type.isType(9) && memberdefinition.getName() == Constants.idClone && memberdefinition.getType().getArgumentTypes().length == 0 || type.isType(10) && environment.getClassDefinition(type.getClassName()).subClassOf(environment, this.getClassDeclaration()) || this.getName().getQualifier().equals(memberdefinition.getClassDeclaration().getName().getQualifier());
	}

	public MemberDefinition getAccessMember(final Environment environment, final Context context, final MemberDefinition memberdefinition, final boolean flag) {
		throw new CompilerError("binary getAccessMember");
	}

	public MemberDefinition getUpdateMember(final Environment environment, final Context context, final MemberDefinition memberdefinition, final boolean flag) {
		throw new CompilerError("binary getUpdateMember");
	}

	public MemberDefinition getVariable(final Environment environment, final Identifier identifier, final ClassDefinition classdefinition) throws AmbiguousMember, ClassNotFound {
		return this.getVariable0(environment, identifier, classdefinition, true, true);
	}

	private MemberDefinition getVariable0(final Environment environment, final Identifier identifier, final ClassDefinition classdefinition, final boolean flag, final boolean flag1) throws AmbiguousMember, ClassNotFound {
		for (MemberDefinition memberdefinition = this.getFirstMatch(identifier); memberdefinition != null; memberdefinition = memberdefinition.getNextMatch()) {
			if (memberdefinition.isVariable()) {
				return (flag || !memberdefinition.isPrivate()) && (flag1 || !memberdefinition.isPackagePrivate()) ? memberdefinition : null;
			}
		}

		final ClassDeclaration classdeclaration = this.getSuperClass();
		MemberDefinition memberdefinition1 = null;
		if (classdeclaration != null) {
			memberdefinition1 = classdeclaration.getClassDefinition(environment).getVariable0(environment, identifier, classdefinition, false, flag1 && this.inSamePackage(classdeclaration));
		}
		for (int i = 0; i < this.interfaces.length; i++) {
			final MemberDefinition memberdefinition2 = this.interfaces[i].getClassDefinition(environment).getVariable0(environment, identifier, classdefinition, true, true);
			if (memberdefinition2 != null) {
				if (memberdefinition1 != null && classdefinition.canAccess(environment, memberdefinition1) && memberdefinition2 != memberdefinition1) {
					throw new AmbiguousMember(memberdefinition2, memberdefinition1);
				}
				memberdefinition1 = memberdefinition2;
			}
		}

		return memberdefinition1;
	}

	public boolean reportDeprecated(final Environment environment) {
		return this.isDeprecated() || this.outerClass != null && this.outerClass.reportDeprecated(environment);
	}

	public void noteUsedBy(final ClassDefinition classdefinition, final long l, final Environment environment) {
		if (this.reportDeprecated(environment)) {
			environment.error(l, "warn.class.is.deprecated", this);
		}
	}

	public MemberDefinition getInnerClass(final Environment environment, final Identifier identifier) throws ClassNotFound {
		for (MemberDefinition memberdefinition = this.getFirstMatch(identifier); memberdefinition != null; memberdefinition = memberdefinition.getNextMatch()) {
			if (memberdefinition.isInnerClass() && !memberdefinition.getInnerClass().isLocal()) {
				return memberdefinition;
			}
		}

		final ClassDeclaration classdeclaration = this.getSuperClass(environment);
		return classdeclaration != null ? classdeclaration.getClassDefinition(environment).getInnerClass(environment, identifier) : null;
	}

	private MemberDefinition matchMethod(final Environment environment, final ClassDefinition classdefinition, final Identifier identifier, final Type atype[], final boolean flag, final Identifier identifier1) throws AmbiguousMember, ClassNotFound {
		if (this.allMethods == null || !this.allMethods.isFrozen()) {
			throw new CompilerError("matchMethod called early");
		}
		MemberDefinition memberdefinition = null;
		List arraylist = null;
		for (final Iterator iterator = this.allMethods.lookupName(identifier); iterator.hasNext();) {
			final MemberDefinition memberdefinition1 = (MemberDefinition) iterator.next();
			if (environment.isApplicable(memberdefinition1, atype) && (classdefinition == null ? !flag || !memberdefinition1.isPrivate() && (!memberdefinition1.isPackagePrivate() || identifier1 == null || this.inSamePackage(identifier1)) : classdefinition.canAccess(environment, memberdefinition1))) {
				if (memberdefinition == null || environment.isMoreSpecific(memberdefinition1, memberdefinition)) {
					memberdefinition = memberdefinition1;
				} else if (!environment.isMoreSpecific(memberdefinition, memberdefinition1)) {
					if (arraylist == null) {
						arraylist = new ArrayList();
					}
					arraylist.add(memberdefinition1);
				}
			}
		}

		if (memberdefinition != null && arraylist != null) {
			for (final Iterator iterator1 = arraylist.iterator(); iterator1.hasNext();) {
				final MemberDefinition memberdefinition2 = (MemberDefinition) iterator1.next();
				if (!environment.isMoreSpecific(memberdefinition, memberdefinition2)) {
					throw new AmbiguousMember(memberdefinition, memberdefinition2);
				}
			}

		}
		return memberdefinition;
	}

	public MemberDefinition matchMethod(final Environment environment, final ClassDefinition classdefinition, final Identifier identifier, final Type atype[]) throws AmbiguousMember, ClassNotFound {
		return this.matchMethod(environment, classdefinition, identifier, atype, false, null);
	}

	public MemberDefinition matchMethod(final Environment environment, final ClassDefinition classdefinition, final Identifier identifier) throws AmbiguousMember, ClassNotFound {
		return this.matchMethod(environment, classdefinition, identifier, Type.noArgs, false, null);
	}

	public MemberDefinition matchAnonConstructor(final Environment environment, final Identifier identifier, final Type atype[]) throws AmbiguousMember, ClassNotFound {
		return this.matchMethod(environment, null, Constants.idInit, atype, true, identifier);
	}

	protected void basicCheck(final Environment environment) throws ClassNotFound {
		if (this.outerClass != null) {
			this.outerClass.basicCheck(environment);
		}
	}

	public void check(final Environment environment) throws ClassNotFound {
	}

	public Vset checkLocalClass(final Environment environment, final Context context, final Vset vset, final ClassDefinition classdefinition, final Expression aexpression[], final Type atype[]) throws ClassNotFound {
		throw new CompilerError("checkLocalClass");
	}

	protected Iterator getPermanentlyAbstractMethods() {
		if (this.allMethods == null) {
			throw new CompilerError("isPermanentlyAbstract() called early");
		}
		return this.permanentlyAbstractMethods.iterator();
	}

	public static void turnOffInheritanceChecks() {
		doInheritanceChecks = false;
	}

	private void collectOneClass(final Environment environment, final ClassDeclaration classdeclaration, final MethodSet methodset, final MethodSet methodset1, final MethodSet methodset2) {
		try {
			final ClassDefinition classdefinition = classdeclaration.getClassDefinition(environment);
			for (final Iterator iterator = classdefinition.getMethods(environment); iterator.hasNext();) {
				Object obj = iterator.next();
				if (!((MemberDefinition) obj).isPrivate() && !((MemberDefinition) obj).isConstructor() && (!classdefinition.isInterface() || ((MemberDefinition) obj).isAbstract())) {
					final Identifier identifier = ((MemberDefinition) obj).getName();
					final Type type = ((MemberDefinition) obj).getType();
					final MemberDefinition memberdefinition = methodset.lookupSig(identifier, type);
					if (((MemberDefinition) obj).isPackagePrivate() && !this.inSamePackage(((MemberDefinition) obj).getClassDeclaration())) {
						if (memberdefinition != null && this instanceof SourceClass) {
							environment.error(((MemberDefinition) obj).getWhere(), "warn.no.override.access", memberdefinition, memberdefinition.getClassDeclaration(), ((MemberDefinition) obj).getClassDeclaration());
						}
						if (((MemberDefinition) obj).isAbstract()) {
							this.permanentlyAbstractMethods.add(obj);
						}
					} else if (memberdefinition != null) {
						memberdefinition.checkOverride(environment, (MemberDefinition) obj);
					} else {
						final MemberDefinition memberdefinition1 = methodset1.lookupSig(identifier, type);
						if (memberdefinition1 == null) {
							if (methodset2 != null && classdefinition.isInterface() && !this.isInterface()) {
								obj = new SourceMember((MemberDefinition) obj, this, environment);
								methodset2.add((MemberDefinition) obj);
							}
							methodset1.add((MemberDefinition) obj);
						} else if (this.isInterface() && !memberdefinition1.isAbstract() && ((MemberDefinition) obj).isAbstract()) {
							methodset1.replace((MemberDefinition) obj);
						} else if (memberdefinition1.checkMeet(environment, (MemberDefinition) obj, this.getClassDeclaration()) && !memberdefinition1.couldOverride(environment, (MemberDefinition) obj)) {
							if (((MemberDefinition) obj).couldOverride(environment, memberdefinition1)) {
								if (methodset2 != null && classdefinition.isInterface() && !this.isInterface()) {
									obj = new SourceMember((MemberDefinition) obj, this, environment);
									methodset2.replace((MemberDefinition) obj);
								}
								methodset1.replace((MemberDefinition) obj);
							} else {
								environment.error(this.where, "nontrivial.meet", obj, memberdefinition1.getClassDefinition(), ((MemberDefinition) obj).getClassDeclaration());
							}
						}
					}
				}
			}

		} catch (final ClassNotFound classnotfound) {
			environment.error(this.getWhere(), "class.not.found", classnotfound.name, this);
		}
	}

	protected void collectInheritedMethods(final Environment environment) {
		if (this.allMethods != null) {
			if (this.allMethods.isFrozen()) {
				return;
			}
			throw new CompilerError("collectInheritedMethods()");
		}
		final MethodSet methodset = new MethodSet();
		this.allMethods = new MethodSet();
		final MethodSet methodset1 = environment.version12() ? null : new MethodSet();
		for (MemberDefinition memberdefinition = this.getFirstMember(); memberdefinition != null; memberdefinition = memberdefinition.nextMember) {
			if (memberdefinition.isMethod() && !memberdefinition.isInitializer()) {
				methodset.add(memberdefinition);
				this.allMethods.add(memberdefinition);
			}
		}

		final ClassDeclaration classdeclaration = this.getSuperClass(environment);
		if (classdeclaration != null) {
			this.collectOneClass(environment, classdeclaration, methodset, this.allMethods, methodset1);
			final ClassDefinition classdefinition = classdeclaration.getClassDefinition();
			for (final Iterator iterator = classdefinition.getPermanentlyAbstractMethods(); iterator.hasNext(); this.permanentlyAbstractMethods.add(iterator.next())) {
			}
		}
		for (int i = 0; i < this.interfaces.length; i++) {
			this.collectOneClass(environment, this.interfaces[i], methodset, this.allMethods, methodset1);
		}

		this.allMethods.freeze();
		if (methodset1 != null && methodset1.size() > 0) {
			this.addMirandaMethods(environment, methodset1.iterator());
		}
	}

	public Iterator getMethods(final Environment environment) {
		if (this.allMethods == null) {
			this.collectInheritedMethods(environment);
		}
		return this.getMethods();
	}

	private Iterator getMethods() {
		if (this.allMethods == null) {
			throw new CompilerError("getMethods: too early");
		}
		return this.allMethods.iterator();
	}

	protected void addMirandaMethods(final Environment environment, final Iterator iterator) {
	}

	public void inlineLocalClass(final Environment environment) {
	}

	public void resolveTypeStructure(final Environment environment) {
	}

	public Identifier resolveName(final Environment environment, final Identifier identifier) {
		Environment.dtEvent("ClassDefinition.resolveName: " + identifier);
		if (identifier.isQualified()) {
			final Identifier identifier1 = this.resolveName(environment, identifier.getHead());
			if (identifier1.hasAmbigPrefix()) {
				return identifier1;
			}
			if (!environment.classExists(identifier1)) {
				return environment.resolvePackageQualifiedName(identifier);
			}
			try {
				return environment.getClassDefinition(identifier1).resolveInnerClass(environment, identifier.getTail());
			} catch (final ClassNotFound ignored) {
				return Identifier.lookupInner(identifier1, identifier.getTail());
			}
		}
		int i = -2;
		LocalMember localmember = null;
		if (this.classContext != null) {
			localmember = this.classContext.getLocalClass(identifier);
			if (localmember != null) {
				i = localmember.getScopeNumber();
			}
		}
		for (ClassDefinition classdefinition = this; classdefinition != null; classdefinition = classdefinition.outerClass) {
			try {
				final MemberDefinition memberdefinition = classdefinition.getInnerClass(environment, identifier);
				if (memberdefinition != null && (localmember == null || this.classContext.getScopeNumber(classdefinition) > i)) {
					return memberdefinition.getInnerClass().getName();
				}
			} catch (final ClassNotFound ignored) {
			}
		}

		return localmember != null ? localmember.getInnerClass().getName() : environment.resolveName(identifier);
	}

	public Identifier resolveInnerClass(final Environment environment, final Identifier identifier) {
		if (identifier.isInner()) {
			throw new CompilerError("inner");
		}
		if (identifier.isQualified()) {
			final Identifier identifier1 = this.resolveInnerClass(environment, identifier.getHead());
			try {
				return environment.getClassDefinition(identifier1).resolveInnerClass(environment, identifier.getTail());
			} catch (final ClassNotFound ignored) {
				return Identifier.lookupInner(identifier1, identifier.getTail());
			}
		}
		try {
			final MemberDefinition memberdefinition = this.getInnerClass(environment, identifier);
			if (memberdefinition != null) {
				return memberdefinition.getInnerClass().getName();
			}
		} catch (final ClassNotFound ignored) {
		}
		return Identifier.lookupInner(this.getName(), identifier);
	}

	public boolean innerClassExists(final Identifier identifier) {
		for (MemberDefinition memberdefinition = this.getFirstMatch(identifier.getHead()); memberdefinition != null; memberdefinition = memberdefinition.getNextMatch()) {
			if (memberdefinition.isInnerClass() && !memberdefinition.getInnerClass().isLocal()) {
				return !identifier.isQualified() || memberdefinition.getInnerClass().innerClassExists(identifier.getTail());
			}
		}

		return false;
	}

	public MemberDefinition findAnyMethod(final Environment environment, final Identifier identifier) throws ClassNotFound {
		for (MemberDefinition memberdefinition = this.getFirstMatch(identifier); memberdefinition != null; memberdefinition = memberdefinition.getNextMatch()) {
			if (memberdefinition.isMethod()) {
				return memberdefinition;
			}
		}

		final ClassDeclaration classdeclaration = this.getSuperClass();
		return classdeclaration == null ? null : classdeclaration.getClassDefinition(environment).findAnyMethod(environment, identifier);
	}

	public int diagnoseMismatch(final Environment environment, final Identifier identifier, final Type atype[], final int i, final Type atype1[]) throws ClassNotFound {
		final int ai[] = new int[atype.length];
		final Type atype2[] = new Type[atype.length];
		if (!this.diagnoseMismatch(environment, identifier, atype, i, ai, atype2)) {
			return -2;
		}
		for (int j = i; j < atype.length; j++) {
			if (ai[j] < 4) {
				atype1[0] = atype2[j];
				return j << 2 | ai[j];
			}
		}

		return -1;
	}

	private boolean diagnoseMismatch(final Environment environment, final Identifier identifier, final Type atype[], final int i, final int ai[], final Type atype1[]) throws ClassNotFound {
		boolean flag = false;
		for (MemberDefinition memberdefinition = this.getFirstMatch(identifier); memberdefinition != null; memberdefinition = memberdefinition.getNextMatch()) {
			if (memberdefinition.isMethod()) {
				final Type atype2[] = memberdefinition.getType().getArgumentTypes();
				if (atype2.length == atype.length) {
					flag = true;
					for (int j = i; j < atype.length; j++) {
						final Type type = atype[j];
						final Type type1 = atype2[j];
						if (environment.implicitCast(type, type1)) {
							ai[j] = 4;
							continue;
						}
						if (ai[j] <= 2 && environment.explicitCast(type, type1)) {
							if (ai[j] < 2) {
								atype1[j] = null;
							}
							ai[j] = 2;
						} else if (ai[j] > 0) {
							continue;
						}
						if (atype1[j] == null) {
							atype1[j] = type1;
						} else if (atype1[j] != type1) {
							ai[j] |= 1;
						}
					}

				}
			}
		}

		if (identifier.equals(Constants.idInit)) {
			return flag;
		}
		final ClassDeclaration classdeclaration = this.getSuperClass();
		return classdeclaration != null && classdeclaration.getClassDefinition(environment).diagnoseMismatch(environment, identifier, atype, i, ai, atype1) || flag;
	}

	public void addMember(final MemberDefinition memberdefinition) {
		if (this.firstMember == null) {
			this.firstMember = this.lastMember = memberdefinition;
		} else if (memberdefinition.isSynthetic() && memberdefinition.isFinal() && memberdefinition.isVariable()) {
			memberdefinition.nextMember = this.firstMember;
			this.firstMember = memberdefinition;
			memberdefinition.nextMatch = (MemberDefinition) this.fieldHash.get(memberdefinition.name);
		} else {
			this.lastMember.nextMember = memberdefinition;
			this.lastMember = memberdefinition;
			memberdefinition.nextMatch = (MemberDefinition) this.fieldHash.get(memberdefinition.name);
		}
		this.fieldHash.put(memberdefinition.name, memberdefinition);
	}

	public void addMember(final Environment environment, final MemberDefinition memberdefinition) {
		this.addMember(memberdefinition);
		if (this.resolved) {
			memberdefinition.resolveTypeStructure(environment);
		}
	}

	public UplevelReference getReference(final LocalMember localmember) {
		for (UplevelReference uplevelreference = this.references; uplevelreference != null; uplevelreference = uplevelreference.getNext()) {
			if (uplevelreference.getTarget() == localmember) {
				return uplevelreference;
			}
		}

		return this.addReference(localmember);
	}

	private UplevelReference addReference(final LocalMember localmember) {
		if (localmember.getClassDefinition() == this) {
			throw new CompilerError("addReference " + localmember);
		}
		this.referencesMustNotBeFrozen();
		final UplevelReference uplevelreference = new UplevelReference(this, localmember);
		this.references = uplevelreference.insertInto(this.references);
		return uplevelreference;
	}

	public UplevelReference getReferences() {
		return this.references;
	}

	public UplevelReference getReferencesFrozen() {
		this.referencesFrozen = true;
		return this.references;
	}

	public final void referencesMustNotBeFrozen() {
		if (this.referencesFrozen) {
			throw new CompilerError("referencesMustNotBeFrozen " + this);
		}
	}

	public MemberDefinition getClassLiteralLookup(final long l) {
		throw new CompilerError("binary class");
	}

	public void addDependency(final ClassDeclaration classdeclaration) {
		throw new CompilerError("addDependency");
	}

	public ClassDefinition getLocalClass(final String s) {
		return this.localClasses == null ? null : (ClassDefinition) this.localClasses.get(s);
	}

	public void addLocalClass(final ClassDefinition classdefinition, final String s) {
		if (this.localClasses == null) {
			this.localClasses = new Hashtable(31);
		}
		this.localClasses.put(s, classdefinition);
	}

	public void print(final PrintStream printstream) {
		if (this.isPublic()) {
			printstream.print("public ");
		}
		if (this.isInterface()) {
			printstream.print("interface ");
		} else {
			printstream.print("class ");
		}
		printstream.print(this.getName() + " ");
		if (this.getSuperClass() != null) {
			printstream.print("extends " + this.getSuperClass().getName() + ' ');
		}
		if (this.interfaces.length > 0) {
			printstream.print("implements ");
			for (int i = 0; i < this.interfaces.length; i++) {
				if (i > 0) {
					printstream.print(", ");
				}
				printstream.print(this.interfaces[i].getName());
				printstream.print(" ");
			}

		}
		printstream.println("{");
		for (MemberDefinition memberdefinition = this.getFirstMember(); memberdefinition != null; memberdefinition = memberdefinition.getNextMember()) {
			printstream.print("    ");
			memberdefinition.print(printstream);
		}

		printstream.println("}");
	}

	public String toString() {
		return this.getClassDeclaration().toString();
	}

	public void cleanup(final Environment environment) {
		if (environment.dump()) {
			environment.output("[cleanup " + this.getName() + ']');
		}
		for (MemberDefinition memberdefinition = this.getFirstMember(); memberdefinition != null; memberdefinition = memberdefinition.getNextMember()) {
			memberdefinition.cleanup(environment);
		}

		this.documentation = null;
	}

	Object source;
	protected final long where;
	protected int modifiers;
	private Identifier localName;
	private final ClassDeclaration declaration;
	protected IdentifierToken superClassId;
	protected final IdentifierToken[] interfaceIds;
	protected ClassDeclaration superClass;
	protected ClassDeclaration interfaces[];
	protected ClassDefinition outerClass;
	private MemberDefinition outerMember;
	protected MemberDefinition innerClassMember;
	MemberDefinition firstMember;
	private MemberDefinition lastMember;
	public boolean resolved;
	protected String documentation;
	private boolean error;
	private boolean nestError;
	private UplevelReference references;
	private boolean referencesFrozen;
	private final Hashtable fieldHash;
	private Hashtable localClasses;
	protected Context classContext;
	protected boolean supersCheckStarted;
	private MethodSet allMethods;
	private final List permanentlyAbstractMethods;
	protected static boolean doInheritanceChecks = true;

}
