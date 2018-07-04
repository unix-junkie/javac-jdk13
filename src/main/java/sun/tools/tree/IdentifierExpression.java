package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.LocalVariable;
import sun.tools.java.AmbiguousMember;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassNotFound;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.IdentifierToken;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Type;

public final class IdentifierExpression extends Expression {

	public IdentifierExpression(final long l, final Identifier identifier) {
		super(60, l, Type.tError);
		this.id = identifier;
	}

	public IdentifierExpression(final IdentifierToken identifiertoken) {
		this(identifiertoken.getWhere(), identifiertoken.getName());
	}

	public IdentifierExpression(final long l, final MemberDefinition memberdefinition) {
		super(60, l, memberdefinition.getType());
		this.id = memberdefinition.getName();
		this.field = memberdefinition;
	}

	public boolean equals(final Identifier identifier) {
		return this.id.equals(identifier);
	}

	private Vset assign(final Environment environment, final Context context, final Vset vset) {
		if (this.field.isLocal()) {
			final LocalMember localmember = (LocalMember) this.field;
			if (localmember.scopeNumber < context.frameNumber) {
				environment.error(this.where, "assign.to.uplevel", this.id);
			}
			if (localmember.isFinal()) {
				if (!localmember.isBlankFinal()) {
					environment.error(this.where, "assign.to.final", this.id);
				} else if (!vset.testVarUnassigned(localmember.number)) {
					environment.error(this.where, "assign.to.blank.final", this.id);
				}
			}
			vset.addVar(localmember.number);
			localmember.writecount++;
		} else if (this.field.isFinal()) {
			return FieldExpression.checkFinalAssign(environment, context, vset, this.where, this.field);
		}
		return vset;
	}

	private Vset get(final Environment environment, final Context context, final Vset vset) {
		if (this.field.isLocal()) {
			final LocalMember localmember = (LocalMember) this.field;
			if (localmember.scopeNumber < context.frameNumber && !localmember.isFinal()) {
				environment.error(this.where, "invalid.uplevel", this.id);
			}
			if (!vset.testVar(localmember.number)) {
				environment.error(this.where, "var.not.initialized", this.id);
				vset.addVar(localmember.number);
			}
			localmember.readcount++;
		} else {
			if (!this.field.isStatic() && !vset.testVar(context.getThisNumber())) {
				environment.error(this.where, "access.inst.before.super", this.id);
				this.implementation = null;
			}
			if (this.field.isBlankFinal()) {
				final int i = context.getFieldNumber(this.field);
				if (i >= 0 && !vset.testVar(i)) {
					environment.error(this.where, "var.not.initialized", this.id);
				}
			}
		}
		return vset;
	}

	boolean bind(final Environment environment, final Context context) {
		try {
			this.field = context.getField(environment, this.id);
			if (this.field == null) {
				for (ClassDefinition classdefinition = context.field.getClassDefinition(); classdefinition != null; classdefinition = classdefinition.getOuterClass()) {
					if (classdefinition.findAnyMethod(environment, this.id) != null) {
						environment.error(this.where, "invalid.var", this.id, context.field.getClassDeclaration());
						return false;
					}
				}

				environment.error(this.where, "undef.var", this.id);
				return false;
			}
			this.type = this.field.getType();
			if (!context.field.getClassDefinition().canAccess(environment, this.field)) {
				environment.error(this.where, "no.field.access", this.id, this.field.getClassDeclaration(), context.field.getClassDeclaration());
				return false;
			}
			if (this.field.isLocal()) {
				final LocalMember localmember = (LocalMember) this.field;
				if (localmember.scopeNumber < context.frameNumber) {
					this.implementation = context.makeReference(environment, localmember);
				}
			} else {
				final MemberDefinition memberdefinition = this.field;
				if (memberdefinition.reportDeprecated(environment)) {
					environment.error(this.where, "warn.field.is.deprecated", this.id, memberdefinition.getClassDefinition());
				}
				final ClassDefinition classdefinition1 = memberdefinition.getClassDefinition();
				if (classdefinition1 != context.field.getClassDefinition()) {
					final MemberDefinition memberdefinition1 = context.getApparentField(environment, this.id);
					if (memberdefinition1 != null && memberdefinition1 != memberdefinition) {
						ClassDefinition classdefinition2 = context.findScope(environment, classdefinition1);
						if (classdefinition2 == null) {
							classdefinition2 = memberdefinition.getClassDefinition();
						}
						if (memberdefinition1.isLocal()) {
							environment.error(this.where, "inherited.hides.local", this.id, classdefinition2.getClassDeclaration());
						} else {
							environment.error(this.where, "inherited.hides.field", this.id, classdefinition2.getClassDeclaration(), memberdefinition1.getClassDeclaration());
						}
					}
				}
				if (memberdefinition.isStatic()) {
					this.implementation = new FieldExpression(this.where, null, memberdefinition);
				} else {
					final Expression expression = context.findOuterLink(environment, this.where, memberdefinition);
					if (expression != null) {
						this.implementation = new FieldExpression(this.where, expression, memberdefinition);
					}
				}
			}
			if (!context.canReach(environment, this.field)) {
				environment.error(this.where, "forward.ref", this.id, this.field.getClassDeclaration());
				return false;
			}
			return true;
		} catch (final ClassNotFound classnotfound) {
			environment.error(this.where, "class.not.found", classnotfound.name, context.field);
		} catch (final AmbiguousMember ambiguousmember) {
			environment.error(this.where, "ambig.field", this.id, ambiguousmember.field1.getClassDeclaration(), ambiguousmember.field2.getClassDeclaration());
		}
		return false;
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		if (this.field != null) {
			return vset;
		}
		if (this.bind(environment, context)) {
			vset = this.get(environment, context, vset);
			context.field.getClassDefinition().addDependency(this.field.getClassDeclaration());
			if (this.implementation != null) {
				return this.implementation.checkValue(environment, context, vset, hashtable);
			}
		}
		return vset;
	}

	public Vset checkLHS(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		if (!this.bind(environment, context)) {
			return vset;
		}
		vset = this.assign(environment, context, vset);
		if (this.implementation != null) {
			vset = this.implementation.checkValue(environment, context, vset, hashtable);
		}
		return vset;
	}

	public Vset checkAssignOp(final Environment environment, final Context context, Vset vset, final Hashtable hashtable, final Expression expression) {
		if (!this.bind(environment, context)) {
			return vset;
		}
		vset = this.assign(environment, context, this.get(environment, context, vset));
		if (this.implementation != null) {
			vset = this.implementation.checkValue(environment, context, vset, hashtable);
		}
		return vset;
	}

	public FieldUpdater getAssigner(final Environment environment, final Context context) {
		return this.implementation != null ? this.implementation.getAssigner(environment, context) : null;
	}

	public FieldUpdater getUpdater(final Environment environment, final Context context) {
		return this.implementation != null ? this.implementation.getUpdater(environment, context) : null;
	}

	public Vset checkAmbigName(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final UnaryExpression unaryexpression) {
		try {
			if (context.getField(environment, this.id) != null) {
				return this.checkValue(environment, context, vset, hashtable);
			}
		} catch (final ClassNotFound ignored) {
		} catch (final AmbiguousMember ignored) {
		}
		final ClassDefinition classdefinition = this.toResolvedType(environment, context, true);
		if (classdefinition != null) {
			unaryexpression.right = new TypeExpression(this.where, classdefinition.getType());
			return vset;
		}
		this.type = Type.tPackage;
		return vset;
	}

	private ClassDefinition toResolvedType(final Environment environment, final Context context, final boolean flag) {
		final Identifier identifier = context.resolveName(environment, this.id);
		final Type type = Type.tClass(identifier);
		if (flag && !environment.classExists(type)) {
			return null;
		}
		if (environment.resolve(this.where, context.field.getClassDefinition(), type)) {
			try {
				final ClassDefinition classdefinition = environment.getClassDefinition(type);
				if (classdefinition.isMember()) {
					final ClassDefinition classdefinition1 = context.findScope(environment, classdefinition.getOuterClass());
					if (classdefinition1 != classdefinition.getOuterClass()) {
						final Identifier identifier1 = context.getApparentClassName(environment, this.id);
						if (!identifier1.equals(Constants.idNull) && !identifier1.equals(identifier)) {
							environment.error(this.where, "inherited.hides.type", this.id, classdefinition1.getClassDeclaration());
						}
					}
				}
				if (!classdefinition.getLocalName().equals(this.id.getFlatName().getName())) {
					environment.error(this.where, "illegal.mangled.name", this.id, classdefinition);
				}
				return classdefinition;
			} catch (final ClassNotFound ignored) {
			}
		}
		return null;
	}

	Type toType(final Environment environment, final Context context) {
		final ClassDefinition classdefinition = this.toResolvedType(environment, context, false);
		return classdefinition != null ? classdefinition.getType() : Type.tError;
	}

	public boolean isConstant() {
		if (this.implementation != null) {
			return this.implementation.isConstant();
		}
		return this.field != null && this.field.isConstant();
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		if (this.implementation != null) {
			return this.implementation.inlineValue(environment, context);
		}
		if (this.field == null) {
			return this;
		}
		try {
			if (this.field.isLocal()) {
				if (this.field.isInlineable(environment, false)) {
					final Expression expression = (Expression) this.field.getValue(environment);
					return expression != null ? expression.inlineValue(environment, context) : this;
				}
				return this;
			}
			return this;
		} catch (final ClassNotFound classnotfound) {
			throw new CompilerError(classnotfound);
		}
	}

	public Expression inlineLHS(final Environment environment, final Context context) {
		return this.implementation != null ? this.implementation.inlineLHS(environment, context) : this;
	}

	public Expression copyInline(final Context context) {
		if (this.implementation != null) {
			return this.implementation.copyInline(context);
		}
		final IdentifierExpression identifierexpression = (IdentifierExpression) super.copyInline(context);
		if (this.field != null && this.field.isLocal()) {
			identifierexpression.field = ((LocalMember) this.field).getCurrentInlineCopy(context);
		}
		return identifierexpression;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return this.implementation != null ? this.implementation.costInline(i, environment, context) : super.costInline(i, environment, context);
	}

	int codeLValue(final Environment environment, final Context context, final Assembler assembler) {
		return 0;
	}

	void codeLoad(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 21 + this.type.getTypeCodeOffset(), new Integer(((LocalMember) this.field).number));
	}

	void codeStore(final Environment environment, final Context context, final Assembler assembler) {
		final LocalMember localmember = (LocalMember) this.field;
		assembler.add(this.where, 54 + this.type.getTypeCodeOffset(), new LocalVariable(localmember, localmember.number));
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.codeLValue(environment, context, assembler);
		this.codeLoad(environment, context, assembler);
	}

	public void print(final PrintStream printstream) {
		printstream.print(this.id + "#" + (this.field == null ? 0 : this.field.hashCode()));
		if (this.implementation != null) {
			printstream.print("/IMPL=");
			this.implementation.print(printstream);
		}
	}

	final Identifier id;
	MemberDefinition field;
	private Expression implementation;
}
