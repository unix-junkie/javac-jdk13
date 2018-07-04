package sun.tools.javac;

import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import sun.tools.asm.Assembler;
import sun.tools.java.ClassDeclaration;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassNotFound;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.IdentifierToken;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Type;
import sun.tools.tree.Context;
import sun.tools.tree.Expression;
import sun.tools.tree.ExpressionStatement;
import sun.tools.tree.LocalMember;
import sun.tools.tree.MethodExpression;
import sun.tools.tree.Node;
import sun.tools.tree.NullExpression;
import sun.tools.tree.Statement;
import sun.tools.tree.SuperExpression;
import sun.tools.tree.UplevelReference;
import sun.tools.tree.Vset;

public final class SourceMember extends MemberDefinition {

	public List getArguments() {
		return this.args;
	}

	SourceMember(final long l, final ClassDefinition classdefinition, final String documentation, final int i, final Type type, final Identifier identifier, final List args, final IdentifierToken aidentifiertoken[], final Node node) {
		super(l, classdefinition, i, type, identifier, aidentifiertoken, node);
		this.outerThisArg = null;
		this.resolved = false;
		this.documentation = documentation;
		this.args = args;
		if (ClassDefinition.containsDeprecated(this.documentation)) {
			this.modifiers |= Constants.M_DEPRECATED;
		}
	}

	private void createArgumentFields(final List args0) {
		if (this.isMethod()) {
			this.args = new Vector();
			if (this.isConstructor() || !this.isStatic() && !this.isInitializer()) {
				this.args.add(((SourceClass) this.clazz).getThisArgument());
			}
			if (args0 != null) {
				final Iterator iterator = args0.iterator();
				final Type atype[] = this.getType().getArgumentTypes();
				for (int i = 0; i < atype.length; i++) {
					final Object obj = iterator.next();
					if (obj instanceof LocalMember) {
						this.args = args0;
						return;
					}
					final Identifier identifier;
					final int j;
					final long l;
					if (obj instanceof Identifier) {
						identifier = (Identifier) obj;
						j = 0;
						l = this.getWhere();
					} else {
						final IdentifierToken identifiertoken = (IdentifierToken) obj;
						identifier = identifiertoken.getName();
						j = identifiertoken.getModifiers();
						l = identifiertoken.getWhere();
					}
					this.args.add(new LocalMember(l, this.clazz, j, atype[i], identifier));
				}

			}
		}
	}

	public LocalMember getOuterThisArg() {
		return this.outerThisArg;
	}

	private void addOuterThis() {
		UplevelReference uplevelreference;
		for (uplevelreference = this.clazz.getReferences(); uplevelreference != null && !uplevelreference.isClientOuterField(); uplevelreference = uplevelreference.getNext()) {
		}
		if (uplevelreference == null) {
			return;
		}
		final Type atype[] = this.type.getArgumentTypes();
		final Type atype1[] = new Type[atype.length + 1];
		final LocalMember localmember = uplevelreference.getLocalArgument();
		this.outerThisArg = localmember;
		this.args.add(1, localmember);
		atype1[0] = localmember.getType();
		System.arraycopy(atype, 0, atype1, 1, atype.length);

		this.type = Type.tMethod(this.type.getReturnType(), atype1);
	}

	void addUplevelArguments() {
		final UplevelReference uplevelreference = this.clazz.getReferences();
		this.clazz.getReferencesFrozen();
		int i = 0;
		for (UplevelReference uplevelreference1 = uplevelreference; uplevelreference1 != null; uplevelreference1 = uplevelreference1.getNext()) {
			if (!uplevelreference1.isClientOuterField()) {
				i++;
			}
		}

		if (i == 0) {
			return;
		}
		final Type atype[] = this.type.getArgumentTypes();
		final Type atype1[] = new Type[atype.length + i];
		int j = 0;
		for (UplevelReference uplevelreference2 = uplevelreference; uplevelreference2 != null; uplevelreference2 = uplevelreference2.getNext()) {
			if (!uplevelreference2.isClientOuterField()) {
				final LocalMember localmember = uplevelreference2.getLocalArgument();
				this.args.add(1 + j, localmember);
				atype1[j] = localmember.getType();
				j++;
			}
		}

		System.arraycopy(atype, 0, atype1, j, atype.length);

		this.type = Type.tMethod(this.type.getReturnType(), atype1);
	}

	public SourceMember(final ClassDefinition classdefinition) {
		super(classdefinition);
		this.outerThisArg = null;
		this.resolved = false;
	}

	public SourceMember(final MemberDefinition memberdefinition, final ClassDefinition classdefinition, final Environment environment) {
		this(memberdefinition.getWhere(), classdefinition, memberdefinition.getDocumentation(), memberdefinition.getModifiers() | 0x400, memberdefinition.getType(), memberdefinition.getName(), null, memberdefinition.getExceptionIds(), null);
		this.args = memberdefinition.getArguments();
		this.abstractSource = memberdefinition;
		this.exp = memberdefinition.getExceptions(environment);
	}

	public ClassDeclaration[] getExceptions(Environment environment) {
		if (!this.isMethod() || this.exp != null) {
			return this.exp;
		}
		if (this.expIds == null) {
			this.exp = new ClassDeclaration[0];
			return this.exp;
		}
		environment = ((SourceClass) this.getClassDefinition()).setupEnv();
		this.exp = new ClassDeclaration[this.expIds.length];
		for (int i = 0; i < this.exp.length; i++) {
			final Identifier identifier = this.expIds[i].getName();
			final Identifier identifier1 = this.getClassDefinition().resolveName(environment, identifier);
			this.exp[i] = environment.getClassDeclaration(identifier1);
		}

		return this.exp;
	}

	public void setExceptions(final ClassDeclaration aclassdeclaration[]) {
		this.exp = aclassdeclaration;
	}

	public void resolveTypeStructure(final Environment environment) {
		Environment.dtEnter("SourceMember.resolveTypeStructure: " + this);
		if (this.resolved) {
			Environment.dtEvent("SourceMember.resolveTypeStructure: OK " + this);
			throw new CompilerError("multiple member type resolution");
		}
		Environment.dtEvent("SourceMember.resolveTypeStructure: RESOLVING " + this);
		this.resolved = true;
		super.resolveTypeStructure(environment);
		if (this.isInnerClass()) {
			final ClassDefinition classdefinition = this.getInnerClass();
			if (classdefinition instanceof SourceClass && !classdefinition.isLocal()) {
				((SourceClass) classdefinition).resolveTypeStructure(environment);
			}
			this.type = this.innerClass.getType();
		} else {
			this.type = environment.resolveNames(this.getClassDefinition(), this.type, this.isSynthetic());
			this.getExceptions(environment);
			if (this.isMethod()) {
				final List args = this.args;
				this.args = null;
				this.createArgumentFields(args);
				if (this.isConstructor()) {
					this.addOuterThis();
				}
			}
		}
		Environment.dtExit("SourceMember.resolveTypeStructure: " + this);
	}

	public ClassDeclaration getDefiningClassDeclaration() {
		return this.abstractSource == null ? super.getDefiningClassDeclaration() : this.abstractSource.getDefiningClassDeclaration();
	}

	public boolean reportDeprecated(final Environment environment) {
		return false;
	}

	public void check(final Environment environment) throws ClassNotFound {
		Environment.dtEnter("SourceMember.check: " + this.getName() + ", status = " + this.status);
		if (this.status == 0) {
			if (this.isSynthetic() && this.getValue() == null) {
				this.status = 2;
				Environment.dtExit("SourceMember.check: BREAKING CYCLE");
				return;
			}
			Environment.dtEvent("SourceMember.check: CHECKING CLASS");
			this.clazz.check(environment);
			if (this.status == 0) {
				if (this.getClassDefinition().getError()) {
					this.status = 5;
				} else {
					Environment.dtExit("SourceMember.check: CHECK FAILED");
					throw new CompilerError("check failed");
				}
			}
		}
		Environment.dtExit("SourceMember.check: DONE " + this.getName() + ", status = " + this.status);
	}

	public Vset check(Environment environment, Context context, Vset vset) throws ClassNotFound {
		Environment.dtEvent("SourceMember.check: MEMBER " + this.getName() + ", status = " + this.status);
		if (this.status == 0) {
			if (this.isInnerClass()) {
				final ClassDefinition classdefinition = this.getInnerClass();
				if (classdefinition instanceof SourceClass && !classdefinition.isLocal() && classdefinition.isInsideLocal()) {
					this.status = 1;
					vset = ((SourceClass) classdefinition).checkInsideClass(environment, context, vset);
				}
				this.status = 2;
				return vset;
			}
			if (environment.dump()) {
				System.out.println("[check field " + this.getClassDeclaration().getName() + '.' + this.getName() + ']');
				if (this.getValue() != null) {
					this.getValue().print(System.out);
					System.out.println();
				}
			}
			environment = new Environment(environment, this);
			environment.resolve(this.where, this.getClassDefinition(), this.getType());
			if (this.isMethod()) {
				final ClassDeclaration classdeclaration = environment.getClassDeclaration(Constants.idJavaLangThrowable);
				final ClassDeclaration aclassdeclaration[] = this.getExceptions(environment);
				for (int k = 0; k < aclassdeclaration.length; k++) {
					long l = this.getWhere();
					if (this.expIds != null && k < this.expIds.length) {
						l = IdentifierToken.getWhere(this.expIds[k], l);
					}
					final ClassDefinition classdefinition1;
					try {
						classdefinition1 = aclassdeclaration[k].getClassDefinition(environment);
						environment.resolveByName(l, this.getClassDefinition(), classdefinition1.getName());
					} catch (final ClassNotFound classnotfound) {
						environment.error(l, "class.not.found", classnotfound.name, "throws");
						break;
					}
					classdefinition1.noteUsedBy(this.getClassDefinition(), l, environment);
					if (!this.getClassDefinition().canAccess(environment, classdefinition1.getClassDeclaration())) {
						environment.error(l, "cant.access.class", classdefinition1);
					} else if (!classdefinition1.subClassOf(environment, classdeclaration)) {
						environment.error(l, "throws.not.throwable", classdefinition1);
					}
				}

			}
			this.status = 1;
			if (this.isMethod() && this.args != null) {
				final int i = this.args.size();
				label0: for (int j = 0; j < i; j++) {
					final MemberDefinition localmember = (MemberDefinition) this.args.get(j);
					final Identifier identifier = localmember.getName();
					for (int i1 = j + 1; i1 < i; i1++) {
						final MemberDefinition localmember2 = (MemberDefinition) this.args.get(i1);
						final Identifier identifier1 = localmember2.getName();
						if (!identifier.equals(identifier1)) {
							continue;
						}
						environment.error(localmember2.getWhere(), "duplicate.argument", identifier);
						break label0;
					}

				}

			}
			if (this.getValue() != null) {
				context = new Context(context, this);
				if (this.isMethod()) {
					Statement statement = (Statement) this.getValue();
					LocalMember localmember1;
					for (final Iterator iterator = this.args.iterator(); iterator.hasNext(); vset.addVar(context.declare(environment, localmember1))) {
						localmember1 = (LocalMember) iterator.next();
					}

					if (this.isConstructor()) {
						vset.clearVar(context.getThisNumber());
						final Expression expression1 = statement.firstConstructor();
						if (expression1 == null && this.getClassDefinition().getSuperClass() != null) {
							final Expression expression2 = this.getDefaultSuperCall(environment);
							final Statement expressionstatement = new ExpressionStatement(this.where, expression2);
							statement = Statement.insertStatement(expressionstatement, statement);
							this.setValue(statement);
						}
					}
					final ClassDeclaration aclassdeclaration1[] = this.getExceptions(environment);
					final byte byte0 = (byte) (aclassdeclaration1.length <= 3 ? 7 : 17);
					final Hashtable hashtable1 = new Hashtable(byte0);
					vset = statement.checkMethod(environment, context, vset, hashtable1);
					final ClassDeclaration classdeclaration4 = environment.getClassDeclaration(Constants.idJavaLangError);
					final ClassDeclaration classdeclaration5 = environment.getClassDeclaration(Constants.idJavaLangRuntimeException);
					for (final Iterator iterator = hashtable1.keySet().iterator(); iterator.hasNext();) {
						final ClassDeclaration classdeclaration7 = (ClassDeclaration) iterator.next();
						final ClassDefinition classdefinition3 = classdeclaration7.getClassDefinition(environment);
						if (!classdefinition3.subClassOf(environment, classdeclaration4) && !classdefinition3.subClassOf(environment, classdeclaration5)) {
							boolean flag = false;
							if (!this.isInitializer()) {
								for (int j1 = 0; j1 < aclassdeclaration1.length; j1++) {
									if (classdefinition3.subClassOf(environment, aclassdeclaration1[j1])) {
										flag = true;
									}
								}

							}
							if (!flag) {
								final Node node1 = (Node) hashtable1.get(classdeclaration7);
								final long l1 = node1.getWhere();
								final String s;
								if (this.isConstructor()) {
									s = l1 == this.getClassDefinition().getWhere() ? "def.constructor.exception" : "constructor.exception";
								} else if (this.isInitializer()) {
									s = "initializer.exception";
								} else {
									s = "uncaught.exception";
								}
								environment.error(l1, s, classdeclaration7.getName());
							}
						}
					}

				} else {
					final Hashtable hashtable = new Hashtable(3);
					final Expression expression = (Expression) this.getValue();
					vset = expression.checkInitializer(environment, context, vset, this.getType(), hashtable);
					this.setValue(expression.convert(environment, context, this.getType(), expression));
					if (this.isStatic() && this.isFinal() && !this.clazz.isTopLevel() && !((Expression) this.getValue()).isConstant()) {
						environment.error(this.where, "static.inner.field", this.getName(), this);
						this.setValue(null);
					}
					final ClassDeclaration classdeclaration1 = environment.getClassDeclaration(Constants.idJavaLangThrowable);
					final ClassDeclaration classdeclaration2 = environment.getClassDeclaration(Constants.idJavaLangError);
					final ClassDeclaration classdeclaration3 = environment.getClassDeclaration(Constants.idJavaLangRuntimeException);
					for (final Iterator iterator = hashtable.keySet().iterator(); iterator.hasNext();) {
						final ClassDeclaration classdeclaration6 = (ClassDeclaration) iterator.next();
						final ClassDefinition classdefinition2 = classdeclaration6.getClassDefinition(environment);
						if (!classdefinition2.subClassOf(environment, classdeclaration2) && !classdefinition2.subClassOf(environment, classdeclaration3) && classdefinition2.subClassOf(environment, classdeclaration1)) {
							final Node node = (Node) hashtable.get(classdeclaration6);
							environment.error(node.getWhere(), "initializer.exception", classdeclaration6.getName());
						}
					}

				}
				if (environment.dump()) {
					this.getValue().print(System.out);
					System.out.println();
				}
			}
			this.status = this.getClassDefinition().getError() ? 5 : 2;
		}
		if (this.isInitializer() && vset.isDeadEnd()) {
			environment.error(this.where, "init.no.normal.completion");
			return vset.clearDeadEnd();
		}
		return vset;
	}

	private Expression getDefaultSuperCall(final Environment environment) {
		SuperExpression superexpression = null;
		final ClassDefinition classdefinition = this.getClassDefinition().getSuperClass().getClassDefinition();
		final ClassDefinition classdefinition1 = classdefinition != null ? classdefinition.isTopLevel() ? null : classdefinition.getOuterClass() : null;
		final ClassDefinition classdefinition2 = this.getClassDefinition();
		if (classdefinition1 != null && !Context.outerLinkExists(environment, classdefinition1, classdefinition2)) {
			superexpression = new SuperExpression(this.where, new NullExpression(this.where));
			environment.error(this.where, "no.default.outer.arg", classdefinition1, this.getClassDefinition());
		}
		if (superexpression == null) {
			superexpression = new SuperExpression(this.where);
		}
		return new MethodExpression(this.where, superexpression, Constants.idInit, new Expression[0]);
	}

	void inline(Environment environment) throws ClassNotFound {
		switch (this.status) {
		default:
			break;

		case 0: // '\0'
			this.check(environment);
			this.inline(environment);
			break;

		case 2: // '\002'
			if (environment.dump()) {
				System.out.println("[inline field " + this.getClassDeclaration().getName() + '.' + this.getName() + ']');
			}
			this.status = 3;
			environment = new Environment(environment, this);
			if (this.isMethod()) {
				if (!this.isNative() && !this.isAbstract()) {
					final Statement statement = (Statement) this.getValue();
					final Context context1 = new Context((Context) null, this);
					LocalMember localmember1;
					for (final Iterator iterator = this.args.iterator(); iterator.hasNext(); context1.declare(environment, localmember1)) {
						localmember1 = (LocalMember) iterator.next();
					}

					this.setValue(statement.inline(environment, context1));
				}
			} else {
				if (this.isInnerClass()) {
					final ClassDefinition classdefinition = this.getInnerClass();
					if (classdefinition instanceof SourceClass && !classdefinition.isLocal() && classdefinition.isInsideLocal()) {
						this.status = 3;
						((SourceClass) classdefinition).inlineLocalClass(environment);
					}
					this.status = 4;
					break;
				}
				if (this.getValue() != null) {
					final Context context = new Context((Context) null, this);
					if (!this.isStatic()) {
						final Context context2 = new Context(context, this);
						final LocalMember localmember = ((SourceClass) this.clazz).getThisArgument();
						context2.declare(environment, localmember);
						this.setValue(((Expression) this.getValue()).inlineValue(environment, context2));
					} else {
						this.setValue(((Expression) this.getValue()).inlineValue(environment, context));
					}
				}
			}
			if (environment.dump()) {
				System.out.println("[inlined field " + this.getClassDeclaration().getName() + '.' + this.getName() + ']');
				if (this.getValue() != null) {
					this.getValue().print(System.out);
					System.out.println();
				} else {
					System.out.println("<empty>");
				}
			}
			this.status = 4;
			break;
		}
	}

	public Node getValue(Environment environment) throws ClassNotFound {
		final Node node = this.getValue();
		if (node != null && this.status != 4) {
			environment = ((SourceClass) this.clazz).setupEnv();
			this.inline(environment);
			return this.status != 4 ? null : this.getValue();
		}
		return node;
	}

	public boolean isInlineable(final Environment environment, final boolean flag) throws ClassNotFound {
		if (super.isInlineable(environment, flag)) {
			this.getValue(environment);
			return this.status == 4 && !this.getClassDefinition().getError();
		}
		return false;
	}

	public Object getInitialValue() {
		return this.isMethod() || this.getValue() == null || !this.isFinal() || this.status != 4 ? null : ((Expression) this.getValue()).getValue();
	}

	public void code(Environment environment, final Assembler assembler) throws ClassNotFound {
		switch (this.status) {
		case 0: // '\0'
			this.check(environment);
			this.code(environment, assembler);
			return;

		case 2: // '\002'
			this.inline(environment);
			this.code(environment, assembler);
			return;

		case 4: // '\004'
			if (environment.dump()) {
				System.out.println("[code field " + this.getClassDeclaration().getName() + '.' + this.getName() + ']');
			}
			if (this.isMethod() && !this.isNative() && !this.isAbstract()) {
				environment = new Environment(environment, this);
				final Context context = new Context((Context) null, this);
				final Statement statement = (Statement) this.getValue();
				LocalMember localmember;
				for (final Iterator iterator = this.args.iterator(); iterator.hasNext(); context.declare(environment, localmember)) {
					localmember = (LocalMember) iterator.next();
				}

				if (statement != null) {
					statement.code(environment, context, assembler);
				}
				if (this.getType().getReturnType().isType(11) && !this.isInitializer()) {
					assembler.add(this.getWhere(), 177, true);
				}
			}
			return;

		case 1: // '\001'
		case 3: // '\003'
		default:
		}
	}

	public void codeInit(final Environment environment, final Context context, final Assembler assembler) throws ClassNotFound {
		if (this.isMethod()) {
			return;
		}
		switch (this.status) {
		case 0: // '\0'
			this.check(environment);
			this.codeInit(environment, context, assembler);
			return;

		case 2: // '\002'
			this.inline(environment);
			this.codeInit(environment, context, assembler);
			return;

		case 4: // '\004'
			if (environment.dump()) {
				System.out.println("[code initializer  " + this.getClassDeclaration().getName() + '.' + this.getName() + ']');
			}
			if (this.getValue() != null) {
				final Expression expression = (Expression) this.getValue();
				if (this.isStatic()) {
					if (this.getInitialValue() == null) {
						expression.codeValue(environment, context, assembler);
						assembler.add(this.getWhere(), 179, this);
					}
				} else {
					assembler.add(this.getWhere(), 25, new Integer(0));
					expression.codeValue(environment, context, assembler);
					assembler.add(this.getWhere(), 181, this);
				}
			}
			return;

		case 1: // '\001'
		case 3: // '\003'
		default:
		}
	}

	public void print(final PrintStream printstream) {
		super.print(printstream);
		if (this.getValue() != null) {
			this.getValue().print(printstream);
			printstream.println();
		}
	}

	private List args;
	private MemberDefinition abstractSource;
	private int status;
	static final int PARSED = 0;
	static final int CHECKING = 1;
	static final int CHECKED = 2;
	static final int INLINING = 3;
	static final int INLINED = 4;
	static final int ERROR = 5;
	private LocalMember outerThisArg;
	private boolean resolved;
}
