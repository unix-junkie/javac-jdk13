package sun.tools.java;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import sun.tools.asm.Assembler;
import sun.tools.javac.SourceMember;
import sun.tools.tree.Context;
import sun.tools.tree.Expression;
import sun.tools.tree.Node;
import sun.tools.tree.Statement;
import sun.tools.tree.Vset;

public class MemberDefinition {

	public MemberDefinition(final long where, final ClassDefinition clazz, final int modifiers, final Type type, final Identifier name, IdentifierToken expIds[], final Node value) {
		if (expIds == null) {
			expIds = new IdentifierToken[0];
		}
		this.where = where;
		this.clazz = clazz;
		this.modifiers = modifiers;
		this.type = type;
		this.name = name;
		this.expIds = expIds;
		this.value = value;
	}

	protected MemberDefinition(final ClassDefinition innerClass) {
		this(innerClass.getWhere(), innerClass.getOuterClass(), innerClass.getModifiers(), innerClass.getType(), innerClass.getName().getFlatName().getName(), null, null);
		this.innerClass = innerClass;
	}

	public static MemberDefinition makeProxyMember(final MemberDefinition memberdefinition, final ClassDefinition classdefinition, final Environment environment) {
		if (proxyCache == null) {
			proxyCache = new HashMap();
		}
		final String s = memberdefinition + "@" + classdefinition;
		final MemberDefinition memberdefinition1 = (MemberDefinition) proxyCache.get(s);
		if (memberdefinition1 != null) {
			return memberdefinition1;
		}
		final MemberDefinition memberdefinition2 = new MemberDefinition(memberdefinition.getWhere(), classdefinition, memberdefinition.getModifiers(), memberdefinition.getType(), memberdefinition.getName(), memberdefinition.getExceptionIds(), null);
		memberdefinition2.exp = memberdefinition.getExceptions(environment);
		proxyCache.put(s, memberdefinition2);
		return memberdefinition2;
	}

	public final long getWhere() {
		return this.where;
	}

	public final ClassDeclaration getClassDeclaration() {
		return this.clazz.getClassDeclaration();
	}

	public void resolveTypeStructure(final Environment environment) {
	}

	public ClassDeclaration getDefiningClassDeclaration() {
		return this.getClassDeclaration();
	}

	public final ClassDefinition getClassDefinition() {
		return this.clazz;
	}

	final ClassDefinition getTopClass() {
		return this.clazz.getTopClass();
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

	public final Type getType() {
		return this.type;
	}

	public final Identifier getName() {
		return this.name;
	}

	public List getArguments() {
		return this.isMethod() ? new Vector() : null;
	}

	public ClassDeclaration[] getExceptions(final Environment environment) {
		if (this.expIds != null && this.exp == null) {
			if (this.expIds.length == 0) {
				this.exp = new ClassDeclaration[0];
			} else {
				throw new CompilerError("getExceptions " + this);
			}
		}
		return this.exp;
	}

	public final IdentifierToken[] getExceptionIds() {
		return this.expIds;
	}

	public ClassDefinition getInnerClass() {
		return this.innerClass;
	}

	public boolean isUplevelValue() {
		if (!this.isSynthetic() || !this.isVariable() || this.isStatic()) {
			return false;
		}
		final String s = this.name.toString();
		return s.startsWith("val$") || s.startsWith("loc$") || s.startsWith("this$");
	}

	private boolean isAccessMethod() {
		return this.isSynthetic() && this.isMethod() && this.accessPeer != null;
	}

	public MemberDefinition getAccessMethodTarget() {
		if (this.isAccessMethod()) {
			for (MemberDefinition memberdefinition = this.accessPeer; memberdefinition != null; memberdefinition = memberdefinition.accessPeer) {
				if (!memberdefinition.isAccessMethod()) {
					return memberdefinition;
				}
			}

		}
		return null;
	}

	public void setAccessMethodTarget(final MemberDefinition memberdefinition) {
		if (this.getAccessMethodTarget() != memberdefinition) {
			if (this.accessPeer != null || memberdefinition.accessPeer != null) {
				throw new CompilerError("accessPeer");
			}
			this.accessPeer = memberdefinition;
		}
	}

	public MemberDefinition getAccessUpdateMember() {
		if (this.isAccessMethod()) {
			for (MemberDefinition memberdefinition = this.accessPeer; memberdefinition != null; memberdefinition = memberdefinition.accessPeer) {
				if (memberdefinition.isAccessMethod()) {
					return memberdefinition;
				}
			}

		}
		return null;
	}

	public void setAccessUpdateMember(final MemberDefinition memberdefinition) {
		if (this.getAccessUpdateMember() != memberdefinition) {
			if (!this.isAccessMethod() || memberdefinition.getAccessMethodTarget() != this.getAccessMethodTarget()) {
				throw new CompilerError("accessPeer");
			}
			memberdefinition.accessPeer = this.accessPeer;
			this.accessPeer = memberdefinition;
		}
	}

	public final boolean isSuperAccessMethod() {
		return this.superAccessMethod;
	}

	public final void setIsSuperAccessMethod(final boolean flag) {
		this.superAccessMethod = flag;
	}

	public final boolean isBlankFinal() {
		return this.isFinal() && !this.isSynthetic() && this.getValue() == null;
	}

	public boolean isNeverNull() {
		return this.isUplevelValue() && !this.name.toString().startsWith("val$");
	}

	public Node getValue(final Environment environment) throws ClassNotFound {
		return this.value;
	}

	public final Node getValue() {
		return this.value;
	}

	public final void setValue(final Node node) {
		this.value = node;
	}

	public Object getInitialValue() {
		return null;
	}

	public final MemberDefinition getNextMember() {
		return this.nextMember;
	}

	public final MemberDefinition getNextMatch() {
		return this.nextMatch;
	}

	public String getDocumentation() {
		return this.documentation;
	}

	public void check(final Environment environment) throws ClassNotFound {
	}

	public Vset check(final Environment environment, final Context context, final Vset vset) throws ClassNotFound {
		return vset;
	}

	public void code(final Environment environment, final Assembler assembler) throws ClassNotFound {
		throw new CompilerError("code");
	}

	public void codeInit(final Environment environment, final Context context, final Assembler assembler) throws ClassNotFound {
		throw new CompilerError("codeInit");
	}

	public boolean reportDeprecated(final Environment environment) {
		return this.isDeprecated() || this.clazz.reportDeprecated(environment);
	}

	public final boolean canReach(final Environment environment, MemberDefinition memberdefinition) {
		if (memberdefinition.isLocal() || !memberdefinition.isVariable() || !this.isVariable() && !this.isInitializer()) {
			return true;
		}
		if (this.getClassDeclaration().equals(memberdefinition.getClassDeclaration()) && this.isStatic() == memberdefinition.isStatic()) {
			while ((memberdefinition = memberdefinition.getNextMember()) != null && memberdefinition != this) {
			}
			return memberdefinition != null;
		}
		return true;
	}

	private int getAccessLevel() {
		if (this.isPublic()) {
			return 1;
		}
		if (this.isProtected()) {
			return 2;
		}
		if (this.isPackagePrivate()) {
			return 3;
		}
		if (this.isPrivate()) {
			return 4;
		}
		throw new CompilerError("getAccessLevel()");
	}

	private void reportError(final Environment environment, final String s, final ClassDeclaration classdeclaration, final MemberDefinition memberdefinition) {
		if (classdeclaration == null) {
			environment.error(this.getWhere(), s, this, this.getClassDeclaration(), memberdefinition.getClassDeclaration());
		} else {
			environment.error(classdeclaration.getClassDefinition().getWhere(), s, this, this.getClassDeclaration(), memberdefinition.getClassDeclaration());
		}
	}

	boolean sameReturnType(final MemberDefinition memberdefinition) {
		if (!this.isMethod() || !memberdefinition.isMethod()) {
			throw new CompilerError("sameReturnType: not method");
		}
		final Type type1 = this.getType().getReturnType();
		final Type type2 = memberdefinition.getType().getReturnType();
		return type1 == type2;
	}

	void checkOverride(final Environment environment, final MemberDefinition memberdefinition) {
		this.checkOverride(environment, memberdefinition, null);
	}

	private boolean checkOverride(final Environment environment, final MemberDefinition memberdefinition, final ClassDeclaration classdeclaration) {
		if (!this.isMethod()) {
			throw new CompilerError("checkOverride(), expected method");
		}
		if (this.isSynthetic()) {
			if (memberdefinition.isFinal() || !memberdefinition.isConstructor() && !memberdefinition.isStatic() && !this.isStatic()) {
				throw new CompilerError("checkOverride() synthetic");
			}
			return true;
		}
		if (this.getName() != memberdefinition.getName() || !this.getType().equalArguments(memberdefinition.getType())) {
			throw new CompilerError("checkOverride(), signature mismatch");
		}
		boolean flag = true;
		if (memberdefinition.isStatic() && !this.isStatic()) {
			this.reportError(environment, "override.static.with.instance", classdeclaration, memberdefinition);
			flag = false;
		}
		if (!memberdefinition.isStatic() && this.isStatic()) {
			this.reportError(environment, "hide.instance.with.static", classdeclaration, memberdefinition);
			flag = false;
		}
		if (memberdefinition.isFinal()) {
			this.reportError(environment, "override.final.method", classdeclaration, memberdefinition);
			flag = false;
		}
		if (memberdefinition.reportDeprecated(environment) && !this.isDeprecated() && this instanceof SourceMember) {
			this.reportError(environment, "warn.override.is.deprecated", classdeclaration, memberdefinition);
		}
		if (this.getAccessLevel() > memberdefinition.getAccessLevel()) {
			this.reportError(environment, "override.more.restrictive", classdeclaration, memberdefinition);
			flag = false;
		}
		if (!this.sameReturnType(memberdefinition)) {
			this.reportError(environment, "override.different.return", classdeclaration, memberdefinition);
			flag = false;
		}
		if (!this.exceptionsFit(environment, memberdefinition)) {
			this.reportError(environment, "override.incompatible.exceptions", classdeclaration, memberdefinition);
			return false;
		}
		return flag;
	}

	public boolean checkMeet(final Environment environment, final MemberDefinition memberdefinition, final ClassDeclaration classdeclaration) {
		if (!this.isMethod()) {
			throw new CompilerError("checkMeet(), expected method");
		}
		if (!this.isAbstract() && !memberdefinition.isAbstract()) {
			throw new CompilerError("checkMeet(), no abstract method");
		}
		if (!this.isAbstract()) {
			return this.checkOverride(environment, memberdefinition, classdeclaration);
		}
		if (!memberdefinition.isAbstract()) {
			return memberdefinition.checkOverride(environment, this, classdeclaration);
		}
		if (this.getName() != memberdefinition.getName() || !this.getType().equalArguments(memberdefinition.getType())) {
			throw new CompilerError("checkMeet(), signature mismatch");
		}
		if (!this.sameReturnType(memberdefinition)) {
			environment.error(classdeclaration.getClassDefinition().getWhere(), "meet.different.return", this, this.getClassDeclaration(), memberdefinition.getClassDeclaration());
			return false;
		}
		return true;
	}

	boolean couldOverride(final Environment environment, final MemberDefinition memberdefinition) {
		if (!this.isMethod()) {
			throw new CompilerError("coulcOverride(), expected method");
		}
		return memberdefinition.isAbstract() && this.getAccessLevel() <= memberdefinition.getAccessLevel() && this.exceptionsFit(environment, memberdefinition);
	}

	private boolean exceptionsFit(final Environment environment, final MemberDefinition memberdefinition) {
		final ClassDeclaration aclassdeclaration[] = this.getExceptions(environment);
		final ClassDeclaration aclassdeclaration1[] = memberdefinition.getExceptions(environment);
		label0: for (int i = 0; i < aclassdeclaration.length; i++) {
			try {
				final ClassDefinition classdefinition = aclassdeclaration[i].getClassDefinition(environment);
				for (int j = 0; j < aclassdeclaration1.length; j++) {
					if (classdefinition.subClassOf(environment, aclassdeclaration1[j])) {
						continue label0;
					}
				}

				if (!classdefinition.subClassOf(environment, environment.getClassDeclaration(Constants.idJavaLangError)) && !classdefinition.subClassOf(environment, environment.getClassDeclaration(Constants.idJavaLangRuntimeException))) {
					return false;
				}
			} catch (final ClassNotFound classnotfound) {
				environment.error(this.getWhere(), "class.not.found", classnotfound.name, memberdefinition.getClassDeclaration());
			}
		}

		return true;
	}

	public final boolean isPublic() {
		return (this.modifiers & 1) != 0;
	}

	public final boolean isPrivate() {
		return (this.modifiers & 2) != 0;
	}

	public final boolean isProtected() {
		return (this.modifiers & 4) != 0;
	}

	public final boolean isPackagePrivate() {
		return (this.modifiers & 7) == 0;
	}

	public final boolean isFinal() {
		return (this.modifiers & 0x10) != 0;
	}

	public final boolean isStatic() {
		return (this.modifiers & 8) != 0;
	}

	public final boolean isSynchronized() {
		return (this.modifiers & 0x20) != 0;
	}

	public final boolean isAbstract() {
		return (this.modifiers & 0x400) != 0;
	}

	public final boolean isNative() {
		return (this.modifiers & 0x100) != 0;
	}

	public final boolean isVolatile() {
		return (this.modifiers & 0x40) != 0;
	}

	public final boolean isTransient() {
		return (this.modifiers & 0x80) != 0;
	}

	public final boolean isMethod() {
		return this.type.isType(12);
	}

	public final boolean isVariable() {
		return !this.type.isType(12) && this.innerClass == null;
	}

	public final boolean isSynthetic() {
		return (this.modifiers & 0x80000) != 0;
	}

	public final boolean isDeprecated() {
		return (this.modifiers & Constants.M_DEPRECATED) != 0;
	}

	public final boolean isStrict() {
		return (this.modifiers & 0x200000) != 0;
	}

	public final boolean isInnerClass() {
		return this.innerClass != null;
	}

	public final boolean isInitializer() {
		return this.getName().equals(Constants.idClassInit);
	}

	public final boolean isConstructor() {
		return this.getName().equals(Constants.idInit);
	}

	public boolean isLocal() {
		return false;
	}

	public boolean isInlineable(final Environment environment, final boolean flag) throws ClassNotFound {
		return (this.isStatic() || this.isPrivate() || this.isFinal() || this.isConstructor() || flag) && !this.isSynchronized() && !this.isNative();
	}

	public boolean isConstant() {
		if (this.isFinal() && this.isVariable() && this.value != null) {
			try {
				this.modifiers &= 0xffffffef;
				return ((Expression) this.value).isConstant();
			} finally {
				this.modifiers |= 0x10;
			}
		}
		return false;
	}

	public String toString() {
		final Identifier identifier = this.getClassDefinition().getName();
		if (this.isInitializer()) {
			return this.isStatic() ? "static {}" : "instance {}";
		}
		if (this.isConstructor()) {
			final StringBuffer stringbuffer = new StringBuffer();
			stringbuffer.append(identifier);
			stringbuffer.append('(');
			final Type atype[] = this.getType().getArgumentTypes();
			for (int i = 0; i < atype.length; i++) {
				if (i > 0) {
					stringbuffer.append(',');
				}
				stringbuffer.append(atype[i]);
			}

			stringbuffer.append(')');
			return stringbuffer.toString();
		}
		return this.isInnerClass() ? this.getInnerClass().toString() : this.type.typeString(this.getName().toString());
	}

	public void print(final PrintStream printstream) {
		if (this.isPublic()) {
			printstream.print("public ");
		}
		if (this.isPrivate()) {
			printstream.print("private ");
		}
		if (this.isProtected()) {
			printstream.print("protected ");
		}
		if (this.isFinal()) {
			printstream.print("final ");
		}
		if (this.isStatic()) {
			printstream.print("static ");
		}
		if (this.isSynchronized()) {
			printstream.print("synchronized ");
		}
		if (this.isAbstract()) {
			printstream.print("abstract ");
		}
		if (this.isNative()) {
			printstream.print("native ");
		}
		if (this.isVolatile()) {
			printstream.print("volatile ");
		}
		if (this.isTransient()) {
			printstream.print("transient ");
		}
		printstream.println(this + ";");
	}

	void cleanup(final Environment environment) {
		this.documentation = null;
		if (this.isMethod() && this.value != null) {
			int i = 0;
			if (this.isPrivate() || this.isInitializer() || (i = ((Statement) this.value).costInline(Statement.MAXINLINECOST, null, null)) >= Statement.MAXINLINECOST) {
				this.value = Statement.empty;
			} else {
				try {
					if (!this.isInlineable(null, true)) {
						this.value = Statement.empty;
					}
				} catch (final ClassNotFound ignored) {
				}
			}
			if (this.value != Statement.empty && environment.dump()) {
				environment.output("[after cleanup of " + this.getName() + ", " + i + " expression cost units remain]");
			}
		} else if (this.isVariable() && (this.isPrivate() || !this.isFinal() || this.type.isType(9))) {
			this.value = null;
		}
	}

	protected final long where;
	protected int modifiers;
	protected Type type;
	protected String documentation;
	protected final IdentifierToken[] expIds;
	protected ClassDeclaration exp[];
	private Node value;
	protected final ClassDefinition clazz;
	protected Identifier name;
	protected ClassDefinition innerClass;
	MemberDefinition nextMember;
	MemberDefinition nextMatch;
	public MemberDefinition accessPeer;
	private boolean superAccessMethod;
	private static Map proxyCache;
	static final int PUBLIC_ACCESS = 1;
	static final int PROTECTED_ACCESS = 2;
	static final int PACKAGE_ACCESS = 3;
	static final int PRIVATE_ACCESS = 4;
}
