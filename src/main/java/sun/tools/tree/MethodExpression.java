package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
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

public final class MethodExpression extends NaryExpression {

	public MethodExpression(final long l, final Expression expression, final Identifier identifier, final Expression aexpression[]) {
		super(47, l, Type.tError, expression, aexpression);
		this.id = identifier;
	}

	MethodExpression(final long l, final Expression expression, final MemberDefinition memberdefinition, final Expression aexpression[]) {
		super(47, l, memberdefinition.getType().getReturnType(), expression, aexpression);
		this.id = memberdefinition.getName();
		this.field = memberdefinition;
		this.clazz = memberdefinition.getClassDefinition();
	}

	public MethodExpression(final long l, final Expression expression, final MemberDefinition memberdefinition, final Expression aexpression[], final boolean flag) {
		this(l, expression, memberdefinition, aexpression);
		this.isSuper = flag;
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		final ClassDefinition classdefinition = context.field.getClassDefinition();
		Expression aexpression[] = this.args;
		if (this.id.equals(Constants.idInit)) {
			ClassDefinition classdefinition1 = classdefinition;
			try {
				Expression expression = null;
				if (this.right instanceof SuperExpression) {
					classdefinition1 = classdefinition1.getSuperClass().getClassDefinition(environment);
					expression = ((ThisExpression) (SuperExpression) this.right).outerArg;
				} else if (this.right instanceof ThisExpression) {
					expression = ((ThisExpression) this.right).outerArg;
				}
				aexpression = NewInstanceExpression.insertOuterLink(environment, context, this.where, classdefinition1, expression, aexpression);
			} catch (final ClassNotFound ignored) {
			}
		}
		Type atype[] = new Type[aexpression.length];
		ClassDefinition classdefinition2 = classdefinition;
		MemberDefinition memberdefinition = null;
		boolean flag = false;
		try {
			final ClassDeclaration classdeclaration;
			boolean flag1 = false;
			if (this.right == null) {
				flag1 = context.field.isStatic();
				ClassDefinition classdefinition3 = classdefinition;
				MemberDefinition memberdefinition1 = null;
				for (; classdefinition3 != null; classdefinition3 = classdefinition3.getOuterClass()) {
					memberdefinition1 = classdefinition3.findAnyMethod(environment, this.id);
					if (memberdefinition1 != null) {
						break;
					}
				}

				if (memberdefinition1 == null) {
					classdeclaration = context.field.getClassDeclaration();
				} else {
					classdeclaration = classdefinition3.getClassDeclaration();
					if (memberdefinition1.getClassDefinition() != classdefinition3) {
						for (ClassDefinition classdefinition4 = classdefinition3; (classdefinition4 = classdefinition4.getOuterClass()) != null;) {
							final MemberDefinition memberdefinition3 = classdefinition4.findAnyMethod(environment, this.id);
							if (memberdefinition3 != null && memberdefinition3.getClassDefinition() == classdefinition4) {
								environment.error(this.where, "inherited.hides.method", this.id, classdefinition3.getClassDeclaration(), classdefinition4.getClassDeclaration());
								break;
							}
						}

					}
				}
			} else {
				if (this.id.equals(Constants.idInit)) {
					final int i = context.getThisNumber();
					if (!context.field.isConstructor()) {
						environment.error(this.where, "invalid.constr.invoke");
						return vset.addVar(i);
					}
					if (!vset.isReallyDeadEnd() && vset.testVar(i)) {
						environment.error(this.where, "constr.invoke.not.first");
						return vset;
					}
					vset = vset.addVar(i);
					vset = this.right instanceof SuperExpression ? this.right.checkAmbigName(environment, context, vset, hashtable, this) : this.right.checkValue(environment, context, vset, hashtable);
				} else {
					vset = this.right.checkAmbigName(environment, context, vset, hashtable, this);
					if (this.right.type == Type.tPackage) {
						FieldExpression.reportFailedPackagePrefix(environment, this.right);
						return vset;
					}
					if (this.right instanceof TypeExpression) {
						flag1 = true;
					}
				}
				if (this.right.type.isType(10)) {
					classdeclaration = environment.getClassDeclaration(this.right.type);
				} else if (this.right.type.isType(9)) {
					flag = true;
					classdeclaration = environment.getClassDeclaration(Type.tObject);
				} else {
					if (!this.right.type.isType(13)) {
						environment.error(this.where, "invalid.method.invoke", this.right.type);
					}
					return vset;
				}
				if (this.right instanceof FieldExpression) {
					final Identifier identifier = ((FieldExpression) this.right).id;
					if (identifier == Constants.idThis) {
						classdefinition2 = ((FieldExpression) this.right).clazz;
					} else if (identifier == Constants.idSuper) {
						this.isSuper = true;
						classdefinition2 = ((FieldExpression) this.right).clazz;
					}
				} else if (this.right instanceof SuperExpression) {
					this.isSuper = true;
				}
				if (this.id != Constants.idInit && !FieldExpression.isTypeAccessible(this.where, environment, this.right.type, classdefinition2)) {
					final ClassDeclaration classdeclaration1 = classdefinition2.getClassDeclaration();
					if (flag1) {
						environment.error(this.where, "no.type.access", this.id, this.right.type.toString(), classdeclaration1);
					} else {
						environment.error(this.where, "cant.access.member.type", this.id, this.right.type.toString(), classdeclaration1);
					}
				}
			}
			if (this.id.equals(Constants.idInit)) {
				vset = vset.clearVar(context.getThisNumber());
			}
			boolean flag2 = false;
			for (int k = 0; k < aexpression.length; k++) {
				vset = aexpression[k].checkValue(environment, context, vset, hashtable);
				atype[k] = aexpression[k].type;
				flag2 |= atype[k].isType(13);
			}

			if (this.id.equals(Constants.idInit)) {
				vset = vset.addVar(context.getThisNumber());
			}
			if (flag2) {
				return vset;
			}
			this.clazz = classdeclaration.getClassDefinition(environment);
			if (this.field == null) {
				this.field = this.clazz.matchMethod(environment, classdefinition2, this.id, atype);
				if (this.field == null) {
					if (this.id.equals(Constants.idInit)) {
						if (this.diagnoseMismatch(environment, aexpression, atype)) {
							return vset;
						}
						String s = this.clazz.getName().getName().toString();
						s = Type.tMethod(Type.tError, atype).typeString(s, false, false);
						environment.error(this.where, "unmatched.constr", s, classdeclaration);
						return vset;
					}
					String s1 = this.id.toString();
					s1 = Type.tMethod(Type.tError, atype).typeString(s1, false, false);
					if (this.clazz.findAnyMethod(environment, this.id) == null) {
						if (context.getField(environment, this.id) != null) {
							environment.error(this.where, "invalid.method", this.id, classdeclaration);
						} else {
							environment.error(this.where, "undef.meth", s1, classdeclaration);
						}
					} else if (!this.diagnoseMismatch(environment, aexpression, atype)) {
						environment.error(this.where, "unmatched.meth", s1, classdeclaration);
					}
					return vset;
				}
			}
			this.type = this.field.getType().getReturnType();
			if (flag1 && !this.field.isStatic()) {
				environment.error(this.where, "no.static.meth.access", this.field, this.field.getClassDeclaration());
				return vset;
			}
			if (this.field.isProtected() && this.right != null && !(this.right instanceof SuperExpression) && (!(this.right instanceof FieldExpression) || ((FieldExpression) this.right).id != Constants.idSuper) && !classdefinition2.protectedAccess(environment, this.field, this.right.type)) {
				environment.error(this.where, "invalid.protected.method.use", this.field.getName(), this.field.getClassDeclaration(), this.right.type);
				return vset;
			}
			if (this.right instanceof FieldExpression && ((FieldExpression) this.right).id == Constants.idSuper && !this.field.isPrivate() && classdefinition2 != classdefinition) {
				memberdefinition = classdefinition2.getAccessMember(environment, context, this.field, true);
			}
			if (memberdefinition == null && this.field.isPrivate()) {
				final ClassDefinition classdefinition5 = this.field.getClassDefinition();
				if (classdefinition5 != classdefinition) {
					memberdefinition = classdefinition5.getAccessMember(environment, context, this.field, false);
				}
			}
			if (this.field.isAbstract() && this.right != null && ((Node) this.right).op == 83) {
				environment.error(this.where, "invoke.abstract", this.field, this.field.getClassDeclaration());
				return vset;
			}
			if (this.field.reportDeprecated(environment)) {
				if (this.field.isConstructor()) {
					environment.error(this.where, "warn.constr.is.deprecated", this.field);
				} else {
					environment.error(this.where, "warn.meth.is.deprecated", this.field, this.field.getClassDefinition());
				}
			}
			if (this.field.isConstructor() && context.field.equals(this.field)) {
				environment.error(this.where, "recursive.constr", this.field);
			}
			if (classdefinition2 == classdefinition) {
				final ClassDefinition classdefinition6 = this.field.getClassDefinition();
				if (!this.field.isConstructor() && classdefinition6.isPackagePrivate() && !classdefinition6.getName().getQualifier().equals(classdefinition2.getName().getQualifier())) {
					this.field = MemberDefinition.makeProxyMember(this.field, this.clazz, environment);
				}
			}
			classdefinition2.addDependency(this.field.getClassDeclaration());
			if (classdefinition2 != classdefinition) {
				classdefinition.addDependency(this.field.getClassDeclaration());
			}
		} catch (final ClassNotFound classnotfound1) {
			environment.error(this.where, "class.not.found", classnotfound1.name, context.field);
			return vset;
		} catch (final AmbiguousMember ambiguousmember) {
			environment.error(this.where, "ambig.field", this.id, ambiguousmember.field1, ambiguousmember.field2);
			return vset;
		}
		if (this.right == null && !this.field.isStatic()) {
			this.right = context.findOuterLink(environment, this.where, this.field);
			vset = this.right.checkValue(environment, context, vset, hashtable);
		}
		atype = this.field.getType().getArgumentTypes();
		for (int j = 0; j < aexpression.length; j++) {
			aexpression[j] = this.convert(environment, context, atype[j], aexpression[j]);
		}

		if (this.field.isConstructor()) {
			MemberDefinition memberdefinition2 = this.field;
			if (memberdefinition != null) {
				memberdefinition2 = memberdefinition;
			}
			final int l = aexpression.length;
			Expression aexpression2[] = aexpression;
			if (l > this.args.length) {
				final Object obj1;
				if (this.right instanceof SuperExpression) {
					obj1 = new SuperExpression(((Node) this.right).where, context);
					((ThisExpression) this.right).outerArg = aexpression[0];
				} else if (this.right instanceof ThisExpression) {
					obj1 = new ThisExpression(((Node) this.right).where, context);
				} else {
					throw new CompilerError("this.init");
				}
				if (memberdefinition != null) {
					aexpression2 = new Expression[l + 1];
					this.args = new Expression[l];
					aexpression2[0] = aexpression[0];
					this.args[0] = aexpression2[1] = new NullExpression(this.where);
					for (int i2 = 1; i2 < l; i2++) {
						this.args[i2] = aexpression2[i2 + 1] = aexpression[i2];
					}

				} else {
					System.arraycopy(aexpression, 1, this.args, 0, l - 1);

				}
				this.implementation = new MethodExpression(this.where, (Expression) obj1, memberdefinition2, aexpression2);
				this.implementation.type = this.type;
			} else {
				if (memberdefinition != null) {
					aexpression2 = new Expression[l + 1];
					aexpression2[0] = new NullExpression(this.where);
					System.arraycopy(aexpression, 0, aexpression2, 1, l);

				}
				this.implementation = new MethodExpression(this.where, this.right, memberdefinition2, aexpression2);
			}
		} else {
			if (aexpression.length > this.args.length) {
				throw new CompilerError("method arg");
			}
			if (memberdefinition != null) {
				final Expression aexpression1[] = this.args;
				if (this.field.isStatic()) {
					final MethodExpression methodexpression = new MethodExpression(this.where, null, memberdefinition, aexpression1);
					this.implementation = new CommaExpression(this.where, this.right, methodexpression);
				} else {
					final int i1 = aexpression1.length;
					final Expression aexpression3[] = new Expression[i1 + 1];
					aexpression3[0] = this.right;
					System.arraycopy(aexpression1, 0, aexpression3, 1, i1);

					this.implementation = new MethodExpression(this.where, null, memberdefinition, aexpression3);
				}
			}
		}
		if (context.field.isConstructor() && this.field.isConstructor() && this.right != null && ((Node) this.right).op == 83) {
			final Expression expression1 = this.makeVarInits(environment, context);
			if (expression1 != null) {
				if (this.implementation == null) {
					this.implementation = (Expression) this.clone();
				}
				this.implementation = new CommaExpression(this.where, this.implementation, expression1);
			}
		}
		ClassDeclaration aclassdeclaration[] = this.field.getExceptions(environment);
		if (flag && this.field.getName() == Constants.idClone && this.field.getType().getArgumentTypes().length == 0) {
			aclassdeclaration = new ClassDeclaration[0];
			for (Context context1 = context; context1 != null; context1 = context1.prev) {
				if (context1.node != null && context1.node.op == 101) {
					((TryStatement) context1.node).arrayCloneWhere = this.where;
				}
			}

		}
		for (int j1 = 0; j1 < aclassdeclaration.length; j1++) {
			if (hashtable.get(aclassdeclaration[j1]) == null) {
				hashtable.put(aclassdeclaration[j1], this);
			}
		}

		if (context.field.isConstructor() && this.field.isConstructor() && this.right != null && ((Node) this.right).op == 82) {
			final ClassDefinition classdefinition7 = this.field.getClassDefinition();
			for (MemberDefinition memberdefinition4 = classdefinition7.getFirstMember(); memberdefinition4 != null; memberdefinition4 = memberdefinition4.getNextMember()) {
				if (memberdefinition4.isVariable() && memberdefinition4.isBlankFinal() && !memberdefinition4.isStatic()) {
					vset = vset.addVar(context.getFieldNumber(memberdefinition4));
				}
			}

		}
		return vset;
	}

	public Vset check(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		return this.checkValue(environment, context, vset, hashtable);
	}

	boolean diagnoseMismatch(final Environment environment, final Expression aexpression[], final Type atype[]) throws ClassNotFound {
		final Type atype1[] = new Type[1];
		boolean flag = false;
		int k;
		for (int i = 0; i < atype.length; i = k + 1) {
			final int j = this.clazz.diagnoseMismatch(environment, this.id, atype, i, atype1);
			final String s = this.id.equals(Constants.idInit) ? "constructor" : Constants.opNames[this.op];
			if (j == -2) {
				environment.error(this.where, "wrong.number.args", s);
				flag = true;
			}
			if (j < 0) {
				return flag;
			}
			k = j >> 2;
			final boolean flag1 = (j & 2) != 0;
			final Type type = atype1[0];
			final String s1 = String.valueOf(type);
			if (flag1) {
				environment.error(((Node) aexpression[k]).where, "explicit.cast.needed", s, atype[k], s1);
			} else {
				environment.error(((Node) aexpression[k]).where, "incompatible.type", s, atype[k], s1);
			}
			flag = true;
		}

		return flag;
	}

	private Expression inlineMethod(final Environment environment, final Context context, final Statement statement, final boolean flag) {
		if (environment.dump()) {
			System.out.println("INLINE METHOD " + this.field + " in " + context.field);
		}
		final LocalMember alocalmember[] = LocalMember.copyArguments(context, this.field);
		final Statement astatement[] = new Statement[alocalmember.length + 2];
		int i = 0;
		if (this.field.isStatic()) {
			astatement[0] = new ExpressionStatement(this.where, this.right);
		} else {
			if (this.right != null && ((Node) this.right).op == 83) {
				this.right = new ThisExpression(((Node) this.right).where, context);
			}
			astatement[0] = new VarDeclarationStatement(this.where, alocalmember[i++], this.right);
		}
		for (int j = 0; j < this.args.length; j++) {
			astatement[j + 1] = new VarDeclarationStatement(this.where, alocalmember[i++], this.args[j]);
		}

		astatement[astatement.length - 1] = statement == null ? null : statement.copyInline(context, flag);
		LocalMember.doneWithArguments(context, alocalmember);
		final Type type = flag ? super.type : Type.tVoid;
		final Expression inlinemethodexpression = new InlineMethodExpression(this.where, type, this.field, new CompoundStatement(this.where, astatement));
		return flag ? inlinemethodexpression.inlineValue(environment, context) : inlinemethodexpression.inline(environment, context);
	}

	public Expression inline(final Environment environment, final Context context) {
		if (this.implementation != null) {
			return this.implementation.inline(environment, context);
		}
		try {
			if (this.right != null) {
				this.right = this.field.isStatic() ? this.right.inline(environment, context) : this.right.inlineValue(environment, context);
			}
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].inlineValue(environment, context);
			}

			final ClassDefinition classdefinition = context.field.getClassDefinition();
			Object obj = this;
			if (environment.opt() && this.field.isInlineable(environment, this.clazz.isFinal()) && (this.right == null || ((Node) this.right).op == 82 || this.field.isStatic()) && classdefinition.permitInlinedAccess(environment, this.field.getClassDeclaration()) && classdefinition.permitInlinedAccess(environment, this.field) && (this.right == null || classdefinition.permitInlinedAccess(environment, environment.getClassDeclaration(this.right.type))) && (this.id == null || !this.id.equals(Constants.idInit)) && !context.field.isInitializer() && context.field.isMethod() && context.getInlineMemberContext(this.field) == null) {
				final Statement statement = (Statement) this.field.getValue(environment);
				if (statement == null || statement.costInline(MAXINLINECOST, environment, context) < MAXINLINECOST) {
					obj = this.inlineMethod(environment, context, statement, false);
				}
			}
			return (Expression) obj;
		} catch (final ClassNotFound classnotfound) {
			throw new CompilerError(classnotfound);
		}
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		if (this.implementation != null) {
			return this.implementation.inlineValue(environment, context);
		}
		try {
			if (this.right != null) {
				this.right = this.field.isStatic() ? this.right.inline(environment, context) : this.right.inlineValue(environment, context);
			}
			if (this.field.getName().equals(Constants.idInit)) {
				final ClassDefinition classdefinition = this.field.getClassDefinition();
				final UplevelReference uplevelreference = classdefinition.getReferencesFrozen();
				if (uplevelreference != null) {
					uplevelreference.willCodeArguments(environment, context);
				}
			}
			for (int i = 0; i < this.args.length; i++) {
				this.args[i] = this.args[i].inlineValue(environment, context);
			}

			final ClassDefinition classdefinition1 = context.field.getClassDefinition();
			if (environment.opt() && this.field.isInlineable(environment, this.clazz.isFinal()) && (this.right == null || ((Node) this.right).op == 82 || this.field.isStatic()) && classdefinition1.permitInlinedAccess(environment, this.field.getClassDeclaration()) && classdefinition1.permitInlinedAccess(environment, this.field) && (this.right == null || classdefinition1.permitInlinedAccess(environment, environment.getClassDeclaration(this.right.type))) && !context.field.isInitializer() && context.field.isMethod() && context.getInlineMemberContext(this.field) == null) {
				final Statement statement = (Statement) this.field.getValue(environment);
				if (statement == null || statement.costInline(MAXINLINECOST, environment, context) < MAXINLINECOST) {
					return this.inlineMethod(environment, context, statement, true);
				}
			}
			return this;
		} catch (final ClassNotFound classnotfound) {
			throw new CompilerError(classnotfound);
		}
	}

	public Expression copyInline(final Context context) {
		return this.implementation != null ? this.implementation.copyInline(context) : super.copyInline(context);
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		if (this.implementation != null) {
			return this.implementation.costInline(i, environment, context);
		}
		return this.right != null && ((Node) this.right).op == 83 ? i : super.costInline(i, environment, context);
	}

	private Expression makeVarInits(final Environment environment, final Context context) {
		final ClassDefinition classdefinition = context.field.getClassDefinition();
		Object obj = null;
		for (MemberDefinition memberdefinition = classdefinition.getFirstMember(); memberdefinition != null; memberdefinition = memberdefinition.getNextMember()) {
			if (!memberdefinition.isVariable() && !memberdefinition.isInitializer() || memberdefinition.isStatic()) {
				continue;
			}
			try {
				memberdefinition.check(environment);
			} catch (final ClassNotFound classnotfound) {
				environment.error(memberdefinition.getWhere(), "class.not.found", classnotfound.name, memberdefinition.getClassDefinition());
			}
			Object obj1;
			if (memberdefinition.isUplevelValue()) {
				if (memberdefinition != classdefinition.findOuterMember()) {
					continue;
				}
				final IdentifierExpression identifierexpression = new IdentifierExpression(this.where, memberdefinition.getName());
				if (!identifierexpression.bind(environment, context)) {
					throw new CompilerError("bind " + identifierexpression.id);
				}
				obj1 = identifierexpression;
			} else if (memberdefinition.isInitializer()) {
				final Statement statement = (Statement) memberdefinition.getValue();
				obj1 = new InlineMethodExpression(this.where, Type.tVoid, memberdefinition, statement);
			} else {
				obj1 = memberdefinition.getValue();
			}
			if (obj1 != null) {
				final long l = memberdefinition.getWhere();
				obj1 = ((Expression) obj1).copyInline(context);
				Object obj2 = obj1;
				if (memberdefinition.isVariable()) {
					Object obj3 = new ThisExpression(l, context);
					obj3 = new FieldExpression(l, (Expression) obj3, memberdefinition);
					obj2 = new AssignExpression(l, (Expression) obj3, (Expression) obj1);
				}
				obj = obj != null ? (Object) new CommaExpression(l, (Expression) obj, (Expression) obj2) : obj2;
			}
		}

		return (Expression) obj;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		if (this.implementation != null) {
			throw new CompilerError("codeValue");
		}
		int i = 0;
		if (this.field.isStatic()) {
			if (this.right != null) {
				this.right.code(environment, context, assembler);
			}
		} else if (this.right == null) {
			assembler.add(this.where, 25, new Integer(0));
		} else if (((Node) this.right).op == 83) {
			this.right.codeValue(environment, context, assembler);
			if (Constants.idInit.equals(this.id)) {
				final ClassDefinition classdefinition = this.field.getClassDefinition();
				final UplevelReference uplevelreference = classdefinition.getReferencesFrozen();
				if (uplevelreference != null) {
					if (uplevelreference.isClientOuterField()) {
						this.args[i++].codeValue(environment, context, assembler);
					}
					uplevelreference.codeArguments(environment, context, assembler, this.where, this.field);
				}
			}
		} else {
			this.right.codeValue(environment, context, assembler);
		}
		for (; i < this.args.length; i++) {
			this.args[i].codeValue(environment, context, assembler);
		}

		if (this.field.isStatic()) {
			assembler.add(this.where, 184, this.field);
		} else if (this.field.isConstructor() || this.field.isPrivate() || this.isSuper) {
			assembler.add(this.where, 183, this.field);
		} else if (this.field.getClassDefinition().isInterface()) {
			assembler.add(this.where, 185, this.field);
		} else {
			assembler.add(this.where, 182, this.field);
		}
		if (this.right != null && ((Node) this.right).op == 83 && Constants.idInit.equals(this.id)) {
			final ClassDefinition classdefinition1 = context.field.getClassDefinition();
			final UplevelReference uplevelreference1 = classdefinition1.getReferencesFrozen();
			if (uplevelreference1 != null) {
				uplevelreference1.codeInitialization(environment, context, assembler, this.where, this.field);
			}
		}
	}

	public Expression firstConstructor() {
		return this.id.equals(Constants.idInit) ? this : null;
	}

	public void print(final PrintStream printstream) {
		printstream.print('(' + Constants.opNames[this.op]);
		if (this.right != null) {
			printstream.print(" ");
			this.right.print(printstream);
		}
		printstream.print(" " + (this.id != null ? this.id : Constants.idInit));
		for (int i = 0; i < this.args.length; i++) {
			printstream.print(" ");
			if (this.args[i] != null) {
				this.args[i].print(printstream);
			} else {
				printstream.print("<null>");
			}
		}

		printstream.print(")");
		if (this.implementation != null) {
			printstream.print("/IMPL=");
			this.implementation.print(printstream);
		}
	}

	private final Identifier id;
	private ClassDefinition clazz;
	private MemberDefinition field;
	private Expression implementation;
	private boolean isSuper;
	private static final int MAXINLINECOST;

	static {
		MAXINLINECOST = Statement.MAXINLINECOST;
	}
}
