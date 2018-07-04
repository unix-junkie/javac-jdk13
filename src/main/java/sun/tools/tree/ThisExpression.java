package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public class ThisExpression extends Expression {

	public ThisExpression(final long where) {
		super(82, where, Type.tObject);
	}

	ThisExpression(final int op, final long where) {
		super(op, where, Type.tObject);
	}

	ThisExpression(final long where, final LocalMember localmember) {
		super(82, where, Type.tObject);
		this.field = localmember;
		localmember.readcount++;
	}

	ThisExpression(final long where, final Context context) {
		super(82, where, Type.tObject);
		this.field = context.getLocalField(Constants.idThis);
		this.field.readcount++;
	}

	public ThisExpression(final long where, final Expression outerArg) {
		this(where);
		this.outerArg = outerArg;
	}

	public Vset checkValue(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		if (context.field.isStatic()) {
			environment.error(this.where, "undef.var", Constants.opNames[this.op]);
			this.type = Type.tError;
			return vset;
		}
		if (this.field == null) {
			this.field = context.getLocalField(Constants.idThis);
			this.field.readcount++;
		}
		if (this.field.scopeNumber < context.frameNumber) {
			this.implementation = context.makeReference(environment, this.field);
		}
		if (!vset.testVar(this.field.number)) {
			environment.error(this.where, "access.inst.before.super", Constants.opNames[this.op]);
		}
		this.type = this.field == null ? context.field.getClassDeclaration().getType() : this.field.getType();
		return vset;
	}

	public boolean isNonNull() {
		return true;
	}

	public FieldUpdater getAssigner(final Environment environment, final Context context) {
		return null;
	}

	public FieldUpdater getUpdater(final Environment environment, final Context context) {
		return null;
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		if (this.implementation != null) {
			return this.implementation.inlineValue(environment, context);
		}
		if (this.field != null && this.field.isInlineable(environment, false)) {
			Expression expression = (Expression) this.field.getValue(environment);
			if (expression != null) {
				expression = expression.copyInline(context);
				expression.type = this.type;
				return expression;
			}
		}
		return this;
	}

	public Expression copyInline(final Context context) {
		if (this.implementation != null) {
			return this.implementation.copyInline(context);
		}
		final ThisExpression thisexpression = (ThisExpression) this.clone();
		if (this.field == null) {
			thisexpression.field = context.getLocalField(Constants.idThis);
			thisexpression.field.readcount++;
		} else {
			thisexpression.field = this.field.getCurrentInlineCopy(context);
		}
		if (this.outerArg != null) {
			thisexpression.outerArg = this.outerArg.copyInline(context);
		}
		return thisexpression;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 25, new Integer(this.field.number));
	}

	public void print(final PrintStream printstream) {
		if (this.outerArg != null) {
			printstream.print("(outer=");
			this.outerArg.print(printstream);
			printstream.print(" ");
		}
		String s = this.field != null ? this.field.getClassDefinition().getName().getFlatName().getName() + "." : "";
		s += Constants.opNames[this.op];
		printstream.print(s + '#' + (this.field == null ? 0 : this.field.hashCode()));
		if (this.outerArg != null) {
			printstream.print(")");
		}
	}

	private LocalMember field;
	private Expression implementation;
	Expression outerArg;
}
