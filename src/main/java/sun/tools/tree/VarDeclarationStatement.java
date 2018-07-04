package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.LocalVariable;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassNotFound;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.Type;

public final class VarDeclarationStatement extends Statement {

	public VarDeclarationStatement(final long l, final Expression expression) {
		super(108, l);
		this.expr = expression;
	}

	public VarDeclarationStatement(final long l, final LocalMember localmember, final Expression expression) {
		super(108, l);
		this.field = localmember;
		this.expr = expression;
	}

	Vset checkDeclaration(final Environment environment, final Context context, Vset vset, final int i, Type type, final Hashtable hashtable) {
		if (this.labels != null) {
			environment.error(this.where, "declaration.with.label", this.labels[0]);
		}
		if (this.field != null) {
			if (context.getLocalClass(this.field.getName()) != null && this.field.isInnerClass()) {
				environment.error(this.where, "local.class.redefined", this.field.getName());
			}
			context.declare(environment, this.field);
			if (this.field.isInnerClass()) {
				final ClassDefinition classdefinition = this.field.getInnerClass();
				try {
					return classdefinition.checkLocalClass(environment, context, vset, null, null, null);
				} catch (final ClassNotFound classnotfound) {
					environment.error(this.where, "class.not.found", classnotfound.name, Constants.opNames[this.op]);
				}
				return vset;
			}
			vset.addVar(this.field.number);
			return this.expr == null ? vset : this.expr.checkValue(environment, context, vset, hashtable);
		}
		Expression expression = this.expr;
		if (((Node) expression).op == 1) {
			this.expr = ((UnaryExpression) (AssignExpression) expression).right;
			expression = ((BinaryExpression) (AssignExpression) expression).left;
		} else {
			this.expr = null;
		}
		boolean flag = type.isType(13);
		while (((Node) expression).op == 48) {
			final ArrayAccessExpression arrayaccessexpression = (ArrayAccessExpression) expression;
			if (arrayaccessexpression.index != null) {
				environment.error(((Node) arrayaccessexpression.index).where, "array.dim.in.type");
				flag = true;
			}
			expression = ((UnaryExpression) arrayaccessexpression).right;
			type = Type.tArray(type);
		}
		if (((Node) expression).op == 60) {
			final Identifier identifier = ((IdentifierExpression) expression).id;
			if (context.getLocalField(identifier) != null) {
				environment.error(this.where, "local.redefined", identifier);
			}
			this.field = new LocalMember(((Node) expression).where, context.field.getClassDefinition(), i, type, identifier);
			context.declare(environment, this.field);
			if (this.expr != null) {
				vset = this.expr.checkInitializer(environment, context, vset, type, hashtable);
				this.expr = this.convert(environment, context, type, this.expr);
				this.field.setValue(this.expr);
				if (this.field.isConstant()) {
					this.field.addModifiers(0x100000);
				}
				vset.addVar(this.field.number);
			} else if (flag) {
				vset.addVar(this.field.number);
			} else {
				vset.addVarUnassigned(this.field.number);
			}
			return vset;
		}
		environment.error(((Node) expression).where, "invalid.decl");
		return vset;
	}

	public Statement inline(final Environment environment, Context context) {
		if (this.field.isInnerClass()) {
			final ClassDefinition classdefinition = this.field.getInnerClass();
			classdefinition.inlineLocalClass(environment);
			return null;
		}
		if (environment.opt() && !this.field.isUsed()) {
			return new ExpressionStatement(this.where, this.expr).inline(environment, context);
		}
		context.declare(environment, this.field);
		if (this.expr != null) {
			this.expr = this.expr.inlineValue(environment, context);
			this.field.setValue(this.expr);
			if (environment.opt() && this.field.writecount == 0) {
				if (((Node) this.expr).op == 60) {
					final IdentifierExpression identifierexpression = (IdentifierExpression) this.expr;
					if (identifierexpression.field.isLocal() && (context = context.getInlineContext()) != null && ((LocalMember) identifierexpression.field).number < context.varNumber) {
						this.field.setValue(this.expr);
						this.field.addModifiers(0x100000);
					}
				}
				if (this.expr.isConstant() || ((Node) this.expr).op == 82 || ((Node) this.expr).op == 83) {
					this.field.setValue(this.expr);
					this.field.addModifiers(0x100000);
				}
			}
		}
		return this;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final VarDeclarationStatement vardeclarationstatement = (VarDeclarationStatement) this.clone();
		if (this.expr != null) {
			vardeclarationstatement.expr = this.expr.copyInline(context);
		}
		return vardeclarationstatement;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		if (this.field != null && this.field.isInnerClass()) {
			return i;
		}
		return this.expr == null ? 0 : this.expr.costInline(i, environment, context);
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		if (this.expr != null && !this.expr.type.isType(11)) {
			context.declare(environment, this.field);
			this.expr.codeValue(environment, context, assembler);
			assembler.add(this.where, 54 + this.field.getType().getTypeCodeOffset(), new LocalVariable(this.field, this.field.number));
		} else {
			context.declare(environment, this.field);
			if (this.expr != null) {
				this.expr.code(environment, context, assembler);
			}
		}
	}

	public void print(final PrintStream printstream, final int i) {
		printstream.print("local ");
		if (this.field != null) {
			printstream.print(this.field + "#" + this.field.hashCode());
			if (this.expr != null) {
				printstream.print(" = ");
				this.expr.print(printstream);
			}
		} else {
			this.expr.print(printstream);
			printstream.print(";");
		}
	}

	private LocalMember field;
	private Expression expr;
}
