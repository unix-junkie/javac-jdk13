package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.AmbiguousClass;
import sun.tools.java.AmbiguousMember;
import sun.tools.java.ClassDeclaration;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassNotFound;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Type;

public final class FieldExpression extends UnaryExpression {

	public FieldExpression(final long l, final Expression expression, final Identifier identifier) {
		super(46, l, Type.tError, expression);
		this.id = identifier;
	}

	public FieldExpression(final long l, final Expression expression, final MemberDefinition memberdefinition) {
		super(46, l, memberdefinition.getType(), expression);
		this.id = memberdefinition.getName();
		this.field = memberdefinition;
	}

	private boolean isQualSuper() {
		return this.superBase != null;
	}

	public static Identifier toIdentifier(Expression expression) {
		final StringBuffer stringbuffer = new StringBuffer();
		for (FieldExpression fieldexpression; ((Node) expression).op == 46; expression = ((UnaryExpression) fieldexpression).right) {
			fieldexpression = (FieldExpression) expression;
			if (fieldexpression.id == Constants.idThis || fieldexpression.id == Constants.idClass) {
				return null;
			}
			stringbuffer.insert(0, fieldexpression.id);
			stringbuffer.insert(0, '.');
		}

		if (((Node) expression).op != 60) {
			return null;
		}
		stringbuffer.insert(0, ((IdentifierExpression) expression).id);
		return Identifier.lookup(stringbuffer.toString());
	}

	Type toType(final Environment environment, final Context context) {
		final Identifier identifier = toIdentifier(this);
		if (identifier == null) {
			environment.error(this.where, "invalid.type.expr");
			return Type.tError;
		}
		final Type type = Type.tClass(context.resolveName(environment, identifier));
		return environment.resolve(this.where, context.field.getClassDefinition(), type) ? type : Type.tError;
	}

	public Vset checkAmbigName(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, UnaryExpression unaryexpression) {
		if (this.id == Constants.idThis || this.id == Constants.idClass) {
			unaryexpression = null;
		}
		return this.checkCommon(environment, context, vset, hashtable, unaryexpression, false);
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		vset = this.checkCommon(environment, context, vset, hashtable, null, false);
		if (this.id == Constants.idSuper && this.type != Type.tError) {
			environment.error(this.where, "undef.var.super", Constants.idSuper);
		}
		return vset;
	}

	static void reportFailedPackagePrefix(final Environment environment, final Expression expression) {
		reportFailedPackagePrefix(environment, expression, false);
	}

	private static void reportFailedPackagePrefix(final Environment environment, final Expression expression, final boolean flag) {
		Expression expression1;
		for (expression1 = expression; expression1 instanceof UnaryExpression; expression1 = ((UnaryExpression) expression1).right) {
		}
		final IdentifierExpression identifierexpression = (IdentifierExpression) expression1;
		try {
			environment.resolve(identifierexpression.id);
		} catch (final AmbiguousClass ambiguousclass) {
			environment.error(((Node) expression).where, "ambig.class", ambiguousclass.name1, ambiguousclass.name2);
			return;
		} catch (final ClassNotFound ignored) {
		}
		if (expression1 == expression) {
			if (flag) {
				environment.error(((Node) identifierexpression).where, "undef.class", identifierexpression.id);
			} else {
				environment.error(((Node) identifierexpression).where, "undef.var.or.class", identifierexpression.id);
			}
		} else if (flag) {
			environment.error(((Node) identifierexpression).where, "undef.class.or.package", identifierexpression.id);
		} else {
			environment.error(((Node) identifierexpression).where, "undef.var.class.or.package", identifierexpression.id);
		}
	}

	private Expression implementFieldAccess(final Environment environment, final Context context, final Expression expression, final boolean flag) {
		final ClassDefinition classdefinition = this.accessBase(environment, context);
		if (classdefinition != null) {
			if (this.field.isFinal()) {
				final Expression expression1 = (Expression) this.field.getValue();
				if (expression1 != null && expression1.isConstant() && !flag) {
					return expression1.copyInline(context);
				}
			}
			final MemberDefinition memberdefinition = classdefinition.getAccessMember(environment, context, this.field, this.isQualSuper());
			if (!flag) {
				if (this.field.isStatic()) {
					final Expression aexpression[] = new Expression[0];
					final MethodExpression methodexpression = new MethodExpression(this.where, null, memberdefinition, aexpression);
					return new CommaExpression(this.where, expression, methodexpression);
				}
				final Expression aexpression1[] = { expression };
				return new MethodExpression(this.where, null, memberdefinition, aexpression1);
			}
		}
		return null;
	}

	private ClassDefinition accessBase(final Environment environment, final Context context) {
		if (this.field.isPrivate()) {
			final ClassDefinition classdefinition = this.field.getClassDefinition();
			final ClassDefinition classdefinition2 = context.field.getClassDefinition();
			return classdefinition == classdefinition2 ? null : classdefinition;
		}
		if (this.field.isProtected()) {
			if (this.superBase == null) {
				return null;
			}
			final ClassDefinition classdefinition1 = this.field.getClassDefinition();
			final ClassDefinition classdefinition3 = context.field.getClassDefinition();
			return classdefinition1.inSamePackage(classdefinition3) ? null : this.superBase;
		}
		return null;
	}

	static boolean isTypeAccessible(final long l, final Environment environment, final Type type, final ClassDefinition classdefinition) {
		switch (type.getTypeCode()) {
		case 10: // '\n'
			try {
				type.getClassName();
				final ClassDefinition classdefinition1 = environment.getClassDefinition(type);
				return classdefinition.canAccess(environment, classdefinition1.getClassDeclaration());
			} catch (final ClassNotFound ignored) {
				return true;
			}

		case 9: // '\t'
			return isTypeAccessible(l, environment, type.getElementType(), classdefinition);
		}
		return true;
	}

	private Vset checkCommon(final Environment environment, final Context context, Vset vset, final Hashtable hashtable, final UnaryExpression unaryexpression, final boolean flag) {
		if (this.id == Constants.idClass) {
			final Type type = this.right.toType(environment, context);
			if (!type.isType(10) && !type.isType(9)) {
				if (type.isType(13)) {
					super.type = Type.tClassDesc;
					return vset;
				}
				final String s;
				switch (type.getTypeCode()) {
				case 11: // '\013'
					s = "Void";
					break;

				case 0: // '\0'
					s = "Boolean";
					break;

				case 1: // '\001'
					s = "Byte";
					break;

				case 2: // '\002'
					s = "Character";
					break;

				case 3: // '\003'
					s = "Short";
					break;

				case 4: // '\004'
					s = "Integer";
					break;

				case 6: // '\006'
					s = "Float";
					break;

				case 5: // '\005'
					s = "Long";
					break;

				case 7: // '\007'
					s = "Double";
					break;

				case 8: // '\b'
				case 9: // '\t'
				case 10: // '\n'
				default:
					environment.error(((Node) this.right).where, "invalid.type.expr");
					return vset;
				}
				final Identifier identifier1 = Identifier.lookup(Constants.idJavaLang + "." + s);
				final TypeExpression typeexpression = new TypeExpression(this.where, Type.tClass(identifier1));
				this.implementation = new FieldExpression(this.where, typeexpression, Constants.idTYPE);
				vset = this.implementation.checkValue(environment, context, vset, hashtable);
				super.type = this.implementation.type;
				return vset;
			}
			if (type.isVoidArray()) {
				super.type = Type.tClassDesc;
				environment.error(((Node) this.right).where, "void.array");
				return vset;
			}
			final long l = context.field.getWhere();
			final ClassDefinition classdefinition3 = context.field.getClassDefinition();
			final MemberDefinition memberdefinition = classdefinition3.getClassLiteralLookup(l);
			final String s1 = type.getTypeSignature();
			final String s2 = type.isType(10) ? s1.substring(1, s1.length() - 1).replace('/', '.') : s1.replace('/', '.');
			if (classdefinition3.isInterface()) {
				this.implementation = this.makeClassLiteralInlineRef(environment, context, memberdefinition, s2);
			} else {
				final ClassDefinition classdefinition5 = memberdefinition.getClassDefinition();
				final MemberDefinition memberdefinition1 = getClassLiteralCache(environment, context, s2, classdefinition5);
				this.implementation = this.makeClassLiteralCacheRef(environment, context, memberdefinition, memberdefinition1, s2);
			}
			vset = this.implementation.checkValue(environment, context, vset, hashtable);
			super.type = this.implementation.type;
			return vset;
		}
		if (this.field != null) {
			this.implementation = this.implementFieldAccess(environment, context, this.right, flag);
			return this.right != null ? this.right.checkAmbigName(environment, context, vset, hashtable, this) : vset;
		}
		vset = this.right.checkAmbigName(environment, context, vset, hashtable, this);
		if (this.right.type == Type.tPackage) {
			if (unaryexpression == null) {
				reportFailedPackagePrefix(environment, this.right);
				return vset;
			}
			final Identifier identifier = toIdentifier(this);
			if (identifier != null && environment.classExists(identifier)) {
				unaryexpression.right = new TypeExpression(this.where, Type.tClass(identifier));
				final ClassDefinition classdefinition1 = context.field.getClassDefinition();
				environment.resolve(this.where, classdefinition1, unaryexpression.right.type);
				return vset;
			}
			this.type = Type.tPackage;
			return vset;
		}
		final ClassDefinition classdefinition = context.field.getClassDefinition();
		final boolean flag1 = this.right instanceof TypeExpression;
		try {
			if (!this.right.type.isType(10)) {
				if (this.right.type.isType(9) && this.id.equals(Constants.idLength)) {
					if (!isTypeAccessible(this.where, environment, this.right.type, classdefinition)) {
						final ClassDeclaration classdeclaration = classdefinition.getClassDeclaration();
						if (flag1) {
							environment.error(this.where, "no.type.access", this.id, this.right.type.toString(), classdeclaration);
						} else {
							environment.error(this.where, "cant.access.member.type", this.id, this.right.type.toString(), classdeclaration);
						}
					}
					this.type = Type.tInt;
					this.implementation = new LengthExpression(this.where, this.right);
					return vset;
				}
				if (!this.right.type.isType(13)) {
					environment.error(this.where, "invalid.field.reference", this.id, this.right.type);
				}
				return vset;
			}
			ClassDefinition classdefinition2 = classdefinition;
			if (this.right instanceof FieldExpression) {
				final Identifier identifier2 = ((FieldExpression) this.right).id;
				if (identifier2 == Constants.idThis) {
					classdefinition2 = ((FieldExpression) this.right).clazz;
				} else if (identifier2 == Constants.idSuper) {
					classdefinition2 = ((FieldExpression) this.right).clazz;
					this.superBase = classdefinition2;
				}
			}
			this.clazz = environment.getClassDefinition(this.right.type);
			if (this.id == Constants.idThis || this.id == Constants.idSuper) {
				if (!flag1) {
					environment.error(((Node) this.right).where, "invalid.type.expr");
				}
				if (context.field.isSynthetic()) {
					throw new CompilerError("synthetic qualified this");
				}
				this.implementation = context.findOuterLink(environment, this.where, this.clazz, null, true);
				vset = this.implementation.checkValue(environment, context, vset, hashtable);
				this.type = this.id == Constants.idSuper ? this.clazz.getSuperClass().getType() : this.clazz.getType();
				return vset;
			}
			this.field = this.clazz.getVariable(environment, this.id, classdefinition2);
			if (this.field == null && flag1 && unaryexpression != null) {
				this.field = this.clazz.getInnerClass(environment, this.id);
				if (this.field != null) {
					return this.checkInnerClass(environment, context, vset, hashtable, unaryexpression);
				}
			}
			if (this.field == null) {
				if ((this.field = this.clazz.findAnyMethod(environment, this.id)) != null) {
					environment.error(this.where, "invalid.field", this.id, this.field.getClassDeclaration());
				} else {
					environment.error(this.where, "no.such.field", this.id, this.clazz);
				}
				return vset;
			}
			if (!isTypeAccessible(this.where, environment, this.right.type, classdefinition2)) {
				final ClassDeclaration classdeclaration1 = classdefinition2.getClassDeclaration();
				if (flag1) {
					environment.error(this.where, "no.type.access", this.id, this.right.type.toString(), classdeclaration1);
				} else {
					environment.error(this.where, "cant.access.member.type", this.id, this.right.type.toString(), classdeclaration1);
				}
			}
			this.type = this.field.getType();
			if (!classdefinition2.canAccess(environment, this.field)) {
				environment.error(this.where, "no.field.access", this.id, this.clazz, classdefinition2.getClassDeclaration());
				return vset;
			}
			if (flag1 && !this.field.isStatic()) {
				environment.error(this.where, "no.static.field.access", this.id, this.clazz);
				return vset;
			}
			this.implementation = this.implementFieldAccess(environment, context, this.right, flag);
			if (this.field.isProtected() && !(this.right instanceof SuperExpression) && (!(this.right instanceof FieldExpression) || ((FieldExpression) this.right).id != Constants.idSuper) && !classdefinition2.protectedAccess(environment, this.field, this.right.type)) {
				environment.error(this.where, "invalid.protected.field.use", this.field.getName(), this.field.getClassDeclaration(), this.right.type);
				return vset;
			}
			if (!this.field.isStatic() && ((Node) this.right).op == 82 && !vset.testVar(context.getThisNumber())) {
				environment.error(this.where, "access.inst.before.super", this.id);
			}
			if (this.field.reportDeprecated(environment)) {
				environment.error(this.where, "warn.field.is.deprecated", this.id, this.field.getClassDefinition());
			}
			if (classdefinition2 == classdefinition) {
				final ClassDefinition classdefinition4 = this.field.getClassDefinition();
				if (classdefinition4.isPackagePrivate() && !classdefinition4.getName().getQualifier().equals(classdefinition2.getName().getQualifier())) {
					this.field = MemberDefinition.makeProxyMember(this.field, this.clazz, environment);
				}
			}
			classdefinition2.addDependency(this.field.getClassDeclaration());
		} catch (final ClassNotFound classnotfound) {
			environment.error(this.where, "class.not.found", classnotfound.name, context.field);
		} catch (final AmbiguousMember ambiguousmember) {
			environment.error(this.where, "ambig.field", this.id, ambiguousmember.field1.getClassDeclaration(), ambiguousmember.field2.getClassDeclaration());
		}
		return vset;
	}

	public FieldUpdater getAssigner(final Environment environment, final Context context) {
		if (this.field == null) {
			return null;
		}
		final ClassDefinition classdefinition = this.accessBase(environment, context);
		if (classdefinition != null) {
			final MemberDefinition memberdefinition = classdefinition.getUpdateMember(environment, context, this.field, this.isQualSuper());
			final Expression expression = this.right != null ? this.right.copyInline(context) : null;
			return new FieldUpdater(this.where, this.field, expression, null, memberdefinition);
		}
		return null;
	}

	public FieldUpdater getUpdater(final Environment environment, final Context context) {
		if (this.field == null) {
			return null;
		}
		final ClassDefinition classdefinition = this.accessBase(environment, context);
		if (classdefinition != null) {
			final MemberDefinition memberdefinition = classdefinition.getAccessMember(environment, context, this.field, this.isQualSuper());
			final MemberDefinition memberdefinition1 = classdefinition.getUpdateMember(environment, context, this.field, this.isQualSuper());
			final Expression expression = this.right != null ? this.right.copyInline(context) : null;
			return new FieldUpdater(this.where, this.field, expression, memberdefinition, memberdefinition1);
		}
		return null;
	}

	private Vset checkInnerClass(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final UnaryExpression unaryexpression) {
		final ClassDefinition classdefinition = this.field.getInnerClass();
		this.type = classdefinition.getType();
		if (!classdefinition.isTopLevel()) {
			environment.error(this.where, "inner.static.ref", classdefinition.getName());
		}
		final Expression typeexpression = new TypeExpression(this.where, this.type);
		final ClassDefinition classdefinition1 = context.field.getClassDefinition();
		try {
			if (!classdefinition1.canAccess(environment, this.field)) {
				final ClassDefinition classdefinition2 = environment.getClassDefinition(this.right.type);
				environment.error(this.where, "no.type.access", this.id, classdefinition2, classdefinition1.getClassDeclaration());
				return vset;
			}
			if (this.field.isProtected() && !(this.right instanceof SuperExpression) && (!(this.right instanceof FieldExpression) || ((FieldExpression) this.right).id != Constants.idSuper) && !classdefinition1.protectedAccess(environment, this.field, this.right.type)) {
				environment.error(this.where, "invalid.protected.field.use", this.field.getName(), this.field.getClassDeclaration(), this.right.type);
				return vset;
			}
			classdefinition.noteUsedBy(classdefinition1, this.where, environment);
		} catch (final ClassNotFound classnotfound) {
			environment.error(this.where, "class.not.found", classnotfound.name, context.field);
		}
		classdefinition1.addDependency(this.field.getClassDeclaration());
		if (unaryexpression == null) {
			return typeexpression.checkValue(environment, context, vset, hashtable);
		}
		unaryexpression.right = typeexpression;
		return vset;
	}

	public Vset checkLHS(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		final boolean flag = this.field != null;
		this.checkCommon(environment, context, vset, hashtable, null, true);
		if (this.implementation != null) {
			return super.checkLHS(environment, context, vset, hashtable);
		}
		if (this.field != null && this.field.isFinal() && !flag) {
			if (this.field.isBlankFinal()) {
				if (this.field.isStatic()) {
					if (this.right != null) {
						environment.error(this.where, "qualified.static.final.assign");
					}
				} else if (this.right != null && ((Node) this.right).op != 82) {
					environment.error(this.where, "bad.qualified.final.assign", this.field.getName());
					return vset;
				}
				return checkFinalAssign(environment, context, vset, this.where, this.field);
			}
			environment.error(this.where, "assign.to.final", this.id);
		}
		return vset;
	}

	public Vset checkAssignOp(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final Expression expression) {
		this.checkCommon(environment, context, vset, hashtable, null, true);
		if (this.implementation != null) {
			return super.checkLHS(environment, context, vset, hashtable);
		}
		if (this.field != null && this.field.isFinal()) {
			environment.error(this.where, "assign.to.final", this.id);
		}
		return vset;
	}

	public static Vset checkFinalAssign(final Environment environment, final Context context, final Vset vset, final long l, final MemberDefinition memberdefinition) {
		if (memberdefinition.isBlankFinal() && memberdefinition.getClassDefinition() == context.field.getClassDefinition()) {
			final int i = context.getFieldNumber(memberdefinition);
			if (i >= 0 && vset.testVarUnassigned(i)) {
				return vset.addVar(i);
			}
			final Identifier identifier1 = memberdefinition.getName();
			environment.error(l, "assign.to.blank.final", identifier1);
		} else {
			final Identifier identifier = memberdefinition.getName();
			environment.error(l, "assign.to.final", identifier);
		}
		return vset;
	}

	private static MemberDefinition getClassLiteralCache(final Environment environment, final Context context, final String s, final ClassDefinition classdefinition) {
		String s1;
		if (!(s.length() > 0 && s.charAt(0) == '[')) {
			s1 = "class$" + s.replace('.', '$');
		} else {
			s1 = "array$" + s.substring(1);
			s1 = s1.replace('[', '$');
			if (s.length() > 0 && s.charAt(s.length() - 1) == ';') {
				s1 = s1.substring(0, s1.length() - 1);
				s1 = s1.replace('.', '$');
			}
		}
		final Identifier identifier = Identifier.lookup(s1);
		final MemberDefinition memberdefinition;
		try {
			memberdefinition = classdefinition.getVariable(environment, identifier, classdefinition);
		} catch (final ClassNotFound ignored) {
			return null;
		} catch (final AmbiguousMember ignored) {
			return null;
		}
		return memberdefinition != null && memberdefinition.getClassDefinition() == classdefinition ? memberdefinition : environment.makeMemberDefinition(environment, classdefinition.getWhere(), classdefinition, null, 0x80008, Type.tClassDesc, identifier, null, null, null);
	}

	private Expression makeClassLiteralCacheRef(final Environment environment, final Context context, final MemberDefinition memberdefinition, final MemberDefinition memberdefinition1, final String s) {
		final TypeExpression typeexpression = new TypeExpression(this.where, memberdefinition1.getClassDefinition().getType());
		final FieldExpression fieldexpression = new FieldExpression(this.where, typeexpression, memberdefinition1);
		final NotEqualExpression notequalexpression = new NotEqualExpression(this.where, fieldexpression.copyInline(context), new NullExpression(this.where));
		final TypeExpression typeexpression1 = new TypeExpression(this.where, memberdefinition.getClassDefinition().getType());
		final Expression stringexpression = new StringExpression(this.where, s);
		final Expression aexpression[] = { stringexpression };
		Object obj = new MethodExpression(this.where, typeexpression1, memberdefinition, aexpression);
		obj = new AssignExpression(this.where, fieldexpression.copyInline(context), (Expression) obj);
		return new ConditionalExpression(this.where, notequalexpression, fieldexpression, (Expression) obj);
	}

	private Expression makeClassLiteralInlineRef(final Environment environment, final Context context, final MemberDefinition memberdefinition, final String s) {
		final TypeExpression typeexpression = new TypeExpression(this.where, memberdefinition.getClassDefinition().getType());
		final Expression stringexpression = new StringExpression(this.where, s);
		final Expression aexpression[] = { stringexpression };
		return new MethodExpression(this.where, typeexpression, memberdefinition, aexpression);
	}

	public boolean isConstant() {
		if (this.implementation != null) {
			return this.implementation.isConstant();
		}
		return this.field != null && (this.right == null || this.right instanceof TypeExpression || ((Node) this.right).op == 82 && ((Node) this.right).where == this.where) && this.field.isConstant();
	}

	public Expression inline(final Environment environment, final Context context) {
		if (this.implementation != null) {
			return this.implementation.inline(environment, context);
		}
		final Expression expression = this.inlineValue(environment, context);
		if (expression instanceof FieldExpression) {
			final FieldExpression fieldexpression = (FieldExpression) expression;
			if (((UnaryExpression) fieldexpression).right != null && ((Node) ((UnaryExpression) fieldexpression).right).op == 82) {
				return null;
			}
		}
		return expression;
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		if (this.implementation != null) {
			return this.implementation.inlineValue(environment, context);
		}
		try {
			if (this.field == null) {
				return this;
			}
			if (this.field.isFinal()) {
				Expression expression = (Expression) this.field.getValue(environment);
				if (expression != null && expression.isConstant()) {
					expression = expression.copyInline(context);
					expression.where = this.where;
					return new CommaExpression(this.where, this.right, expression).inlineValue(environment, context);
				}
			}
			if (this.right != null) {
				if (this.field.isStatic()) {
					final Expression expression1 = this.right.inline(environment, context);
					this.right = null;
					if (expression1 != null) {
						return new CommaExpression(this.where, expression1, this);
					}
				} else {
					this.right = this.right.inlineValue(environment, context);
				}
			}
			return this;
		} catch (final ClassNotFound classnotfound) {
			throw new CompilerError(classnotfound);
		}
	}

	public Expression inlineLHS(final Environment environment, final Context context) {
		if (this.implementation != null) {
			return this.implementation.inlineLHS(environment, context);
		}
		if (this.right != null) {
			if (this.field.isStatic()) {
				final Expression expression = this.right.inline(environment, context);
				this.right = null;
				if (expression != null) {
					return new CommaExpression(this.where, expression, this);
				}
			} else {
				this.right = this.right.inlineValue(environment, context);
			}
		}
		return this;
	}

	public Expression copyInline(final Context context) {
		return this.implementation != null ? this.implementation.copyInline(context) : super.copyInline(context);
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		if (this.implementation != null) {
			return this.implementation.costInline(i, environment, context);
		}
		if (context == null) {
			return 3 + (this.right != null ? this.right.costInline(i, environment, context) : 0);
		}
		final ClassDefinition classdefinition = context.field.getClassDefinition();
		try {
			if (classdefinition.permitInlinedAccess(environment, this.field.getClassDeclaration()) && classdefinition.permitInlinedAccess(environment, this.field)) {
				if (this.right == null) {
					return 3;
				}
				final ClassDeclaration classdeclaration = environment.getClassDeclaration(this.right.type);
				if (classdefinition.permitInlinedAccess(environment, classdeclaration)) {
					return 3 + this.right.costInline(i, environment, context);
				}
			}
		} catch (final ClassNotFound ignored) {
		}
		return i;
	}

	int codeLValue(final Environment environment, final Context context, final Assembler assembler) {
		if (this.implementation != null) {
			throw new CompilerError("codeLValue");
		}
		if (this.field.isStatic()) {
			if (this.right != null) {
				this.right.code(environment, context, assembler);
				return 1;
			}
			return 0;
		}
		this.right.codeValue(environment, context, assembler);
		return 1;
	}

	void codeLoad(final Environment environment, final Context context, final Assembler assembler) {
		if (this.field == null) {
			throw new CompilerError("should not be null");
		}
		if (this.field.isStatic()) {
			assembler.add(this.where, 178, this.field);
		} else {
			assembler.add(this.where, 180, this.field);
		}
	}

	void codeStore(final Environment environment, final Context context, final Assembler assembler) {
		if (this.field.isStatic()) {
			assembler.add(this.where, 179, this.field);
		} else {
			assembler.add(this.where, 181, this.field);
		}
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.codeLValue(environment, context, assembler);
		this.codeLoad(environment, context, assembler);
	}

	public void print(final PrintStream printstream) {
		printstream.print("(");
		if (this.right != null) {
			this.right.print(printstream);
		} else {
			printstream.print("<empty>");
		}
		printstream.print("." + this.id + ')');
		if (this.implementation != null) {
			printstream.print("/IMPL=");
			this.implementation.print(printstream);
		}
	}

	final Identifier id;
	MemberDefinition field;
	private Expression implementation;
	ClassDefinition clazz;
	private ClassDefinition superBase;
}
