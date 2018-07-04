package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.AmbiguousClass;
import sun.tools.java.AmbiguousMember;
import sun.tools.java.ClassDeclaration;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassNotFound;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Type;

public final class NewInstanceExpression extends NaryExpression {

	public NewInstanceExpression(final long l, final Expression expression, final Expression aexpression[]) {
		super(42, l, Type.tError, expression, aexpression);
		this.implMethod = null;
	}

	public NewInstanceExpression(final long l, final Expression expression, final Expression aexpression[], final Expression expression1, final ClassDefinition classdefinition) {
		this(l, expression, aexpression);
		this.outerArg = expression1;
		this.body = classdefinition;
	}

	public Expression getOuterArg() {
		return this.outerArg;
	}

	int precedence() {
		return 100;
	}

	public Expression order() {
		if (this.outerArg != null && Constants.opPrecedence[46] > this.outerArg.precedence()) {
			final UnaryExpression unaryexpression = (UnaryExpression) this.outerArg;
			this.outerArg = unaryexpression.right;
			unaryexpression.right = this.order();
			return unaryexpression;
		}
		return this;
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		ClassDefinition classdefinition = null;
		Expression expression = null;
		try {
			if (this.outerArg != null) {
				vset = this.outerArg.checkValue(environment, context, vset, hashtable);
				expression = this.outerArg;
				final Identifier identifier = FieldExpression.toIdentifier(this.right);
				if (identifier != null && identifier.isQualified()) {
					environment.error(this.where, "unqualified.name.required", identifier);
				}
				if (identifier == null || !this.outerArg.type.isType(10)) {
					if (!this.outerArg.type.isType(13)) {
						environment.error(this.where, "invalid.field.reference", Constants.idNew, this.outerArg.type);
					}
					this.outerArg = null;
				} else {
					final ClassDefinition classdefinition1 = environment.getClassDefinition(this.outerArg.type);
					final Identifier identifier1 = classdefinition1.resolveInnerClass(environment, identifier);
					this.right = new TypeExpression(((Node) this.right).where, Type.tClass(identifier1));
					environment.resolve(((Node) this.right).where, context.field.getClassDefinition(), this.right.type);
				}
			}
			if (!(this.right instanceof TypeExpression)) {
				this.right = new TypeExpression(((Node) this.right).where, this.right.toType(environment, context));
			}
			if (this.right.type.isType(10)) {
				classdefinition = environment.getClassDefinition(this.right.type);
			}
		} catch (final AmbiguousClass ambiguousclass) {
			environment.error(this.where, "ambig.class", ambiguousclass.name1, ambiguousclass.name2);
		} catch (final ClassNotFound classnotfound) {
			environment.error(this.where, "class.not.found", classnotfound.name, context.field);
		}
		Type type = this.right.type;
		boolean flag = type.isType(13);
		if (!type.isType(10) && !flag) {
			environment.error(this.where, "invalid.arg.type", type, Constants.opNames[this.op]);
			flag = true;
		}
		if (classdefinition == null) {
			super.type = Type.tError;
			return vset;
		}
		Expression aexpression[] = this.args;
		aexpression = insertOuterLink(environment, context, this.where, classdefinition, this.outerArg, aexpression);
		if (aexpression.length > this.args.length) {
			this.outerArg = aexpression[0];
		} else if (this.outerArg != null) {
			this.outerArg = new CommaExpression(((Node) this.outerArg).where, this.outerArg, null);
		}
		Type atype[] = new Type[aexpression.length];
		for (int i = 0; i < aexpression.length; i++) {
			if (aexpression[i] != expression) {
				vset = aexpression[i].checkValue(environment, context, vset, hashtable);
			}
			atype[i] = aexpression[i].type;
			flag |= atype[i].isType(13);
		}

		try {
			if (flag) {
				super.type = Type.tError;
				return vset;
			}
			final ClassDefinition classdefinition2 = context.field.getClassDefinition();
			final ClassDeclaration classdeclaration = environment.getClassDeclaration(type);
			if (this.body != null) {
				final Identifier identifier2 = classdefinition2.getName().getQualifier();
				final ClassDefinition classdefinition4 = classdefinition.isInterface() ? environment.getClassDefinition(Constants.idJavaLangObject) : classdefinition;
				final MemberDefinition memberdefinition1 = classdefinition4.matchAnonConstructor(environment, identifier2, atype);
				if (memberdefinition1 != null) {
					Environment.dtEvent("NewInstanceExpression.checkValue: ANON CLASS " + this.body + " SUPER " + classdefinition);
					vset = this.body.checkLocalClass(environment, context, vset, classdefinition, aexpression, memberdefinition1.getType().getArgumentTypes());
					type = this.body.getClassDeclaration().getType();
					classdefinition = this.body;
				}
			} else {
				if (classdefinition.isInterface()) {
					environment.error(this.where, "new.intf", classdeclaration);
					return vset;
				}
				if (classdefinition.mustBeAbstract(environment)) {
					environment.error(this.where, "new.abstract", classdeclaration);
					return vset;
				}
			}
			this.field = classdefinition.matchMethod(environment, classdefinition2, Constants.idInit, atype);
			if (this.field == null) {
				final MemberDefinition memberdefinition = classdefinition.findAnyMethod(environment, Constants.idInit);
				if (memberdefinition != null && new MethodExpression(this.where, this.right, memberdefinition, aexpression).diagnoseMismatch(environment, aexpression, atype)) {
					return vset;
				}
				String s = classdeclaration.getName().getName().toString();
				s = Type.tMethod(Type.tError, atype).typeString(s, false, false);
				environment.error(this.where, "unmatched.constr", s, classdeclaration);
				return vset;
			}
			if (this.field.isPrivate()) {
				final ClassDefinition classdefinition3 = this.field.getClassDefinition();
				if (classdefinition3 != classdefinition2) {
					this.implMethod = classdefinition3.getAccessMember(environment, context, this.field, false);
				}
			}
			if (classdefinition.mustBeAbstract(environment)) {
				environment.error(this.where, "new.abstract", classdeclaration);
				return vset;
			}
			if (this.field.reportDeprecated(environment)) {
				environment.error(this.where, "warn.constr.is.deprecated", this.field, this.field.getClassDefinition());
			}
			if (this.field.isProtected() && !classdefinition2.getName().getQualifier().equals(this.field.getClassDeclaration().getName().getQualifier())) {
				environment.error(this.where, "invalid.protected.constructor.use", classdefinition2);
			}
		} catch (final ClassNotFound classnotfound1) {
			environment.error(this.where, "class.not.found", classnotfound1.name, Constants.opNames[this.op]);
			return vset;
		} catch (final AmbiguousMember ambiguousmember) {
			environment.error(this.where, "ambig.constr", ambiguousmember.field1, ambiguousmember.field2);
			return vset;
		}
		atype = this.field.getType().getArgumentTypes();
		for (int j = 0; j < aexpression.length; j++) {
			aexpression[j] = this.convert(environment, context, atype[j], aexpression[j]);
		}

		if (aexpression.length > this.args.length) {
			this.outerArg = aexpression[0];
			System.arraycopy(aexpression, 1, this.args, 0, aexpression.length - 1);

		}
		final ClassDeclaration aclassdeclaration[] = this.field.getExceptions(environment);
		for (int l = 0; l < aclassdeclaration.length; l++) {
			if (hashtable.get(aclassdeclaration[l]) == null) {
				hashtable.put(aclassdeclaration[l], this);
			}
		}

		super.type = type;
		return vset;
	}

	public static Expression[] insertOuterLink(final Environment environment, final Context context, final long l, final ClassDefinition classdefinition, Expression expression, final Expression[] aexpression) {
		if (!classdefinition.isTopLevel() && !classdefinition.isLocal()) {
			final Expression aexpression1[] = new Expression[1 + aexpression.length];
			System.arraycopy(aexpression, 0, aexpression1, 1, aexpression.length);
			try {
				if (expression == null) {
					expression = context.findOuterLink(environment, l, classdefinition.findAnyMethod(environment, Constants.idInit));
				}
			} catch (final ClassNotFound ignored) {
			}
			aexpression1[0] = expression;
			return aexpression1;
		}
		return aexpression;
	}

	public Vset check(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		return this.checkValue(environment, context, vset, hashtable);
	}

	public Expression copyInline(final Context context) {
		final NewInstanceExpression newinstanceexpression = (NewInstanceExpression) super.copyInline(context);
		if (this.outerArg != null) {
			newinstanceexpression.outerArg = this.outerArg.copyInline(context);
		}
		return newinstanceexpression;
	}

	Expression inlineNewInstance(final Environment environment, final Context context, final Statement statement) {
		if (environment.dump()) {
			System.out.println("INLINE NEW INSTANCE " + this.field + " in " + context.field);
		}
		final LocalMember alocalmember[] = LocalMember.copyArguments(context, this.field);
		final Statement astatement[] = new Statement[alocalmember.length + 2];
		byte byte0 = 1;
		if (this.outerArg != null && !this.outerArg.type.isType(11)) {
			byte0 = 2;
			astatement[1] = new VarDeclarationStatement(this.where, alocalmember[1], this.outerArg);
		} else if (this.outerArg != null) {
			astatement[0] = new ExpressionStatement(this.where, this.outerArg);
		}
		for (int i = 0; i < this.args.length; i++) {
			astatement[i + byte0] = new VarDeclarationStatement(this.where, alocalmember[i + byte0], this.args[i]);
		}

		astatement[astatement.length - 1] = statement == null ? null : statement.copyInline(context, false);
		LocalMember.doneWithArguments(context, alocalmember);
		return new InlineNewInstanceExpression(this.where, this.type, this.field, new CompoundStatement(this.where, astatement)).inline(environment, context);
	}

	public Expression inline(final Environment environment, final Context context) {
		return this.inlineValue(environment, context);
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		if (this.body != null) {
			this.body.inlineLocalClass(environment);
		}
		final ClassDefinition classdefinition = this.field.getClassDefinition();
		final UplevelReference uplevelreference = classdefinition.getReferencesFrozen();
		if (uplevelreference != null) {
			uplevelreference.willCodeArguments(environment, context);
		}
		// try {
		if (this.outerArg != null) {
			this.outerArg = this.outerArg.type.isType(11) ? this.outerArg.inline(environment, context) : this.outerArg.inlineValue(environment, context);
		}
		for (int i = 0; i < this.args.length; i++) {
			this.args[i] = this.args[i].inlineValue(environment, context);
		}

		// } catch(final ClassNotFound cnf) {
		// throw new CompilerError(cnf);
		// }
		if (this.outerArg != null && this.outerArg.type.isType(11)) {
			final Expression expression = this.outerArg;
			this.outerArg = null;
			return new CommaExpression(this.where, expression, this);
		}
		return this;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		if (this.body != null) {
			return i;
		}
		if (context == null) {
			return 2 + super.costInline(i, environment, context);
		}
		final ClassDefinition classdefinition = context.field.getClassDefinition();
		try {
			if (classdefinition.permitInlinedAccess(environment, this.field.getClassDeclaration()) && classdefinition.permitInlinedAccess(environment, this.field)) {
				return 2 + super.costInline(i, environment, context);
			}
		} catch (final ClassNotFound ignored) {
		}
		return i;
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		this.codeCommon(environment, context, assembler, false);
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.codeCommon(environment, context, assembler, true);
	}

	private void codeCommon(final Environment environment, final Context context, final Assembler assembler, final boolean flag) {
		assembler.add(this.where, 187, this.field.getClassDeclaration());
		if (flag) {
			assembler.add(this.where, 89);
		}
		final ClassDefinition classdefinition = this.field.getClassDefinition();
		final UplevelReference uplevelreference = classdefinition.getReferencesFrozen();
		if (uplevelreference != null) {
			uplevelreference.codeArguments(environment, context, assembler, this.where, this.field);
		}
		if (this.outerArg != null) {
			this.outerArg.codeValue(environment, context, assembler);
			switch (((Node) this.outerArg).op) {
			case 49: // '1'
			case 82: // 'R'
			case 83: // 'S'
				break;

			case 46: // '.'
				final MemberDefinition memberdefinition = ((FieldExpression) this.outerArg).field;
				if (memberdefinition != null && memberdefinition.isNeverNull()) {
					break;
					// fall through
				}

			default:
				try {
					final ClassDefinition classdefinition1 = environment.getClassDefinition(Constants.idJavaLangObject);
					final MemberDefinition memberdefinition1 = classdefinition1.getFirstMatch(Constants.idGetClass);
					assembler.add(this.where, 89);
					assembler.add(this.where, 182, memberdefinition1);
					assembler.add(this.where, 87);
				} catch (final ClassNotFound ignored) {
				}
				break;
			}
		}
		if (this.implMethod != null) {
			assembler.add(this.where, 1);
		}
		for (int i = 0; i < this.args.length; i++) {
			this.args[i].codeValue(environment, context, assembler);
		}

		assembler.add(this.where, 183, this.implMethod == null ? (Object) this.field : (Object) this.implMethod);
	}

	private MemberDefinition field;
	private Expression outerArg;
	private ClassDefinition body;
	private MemberDefinition implMethod;
}
