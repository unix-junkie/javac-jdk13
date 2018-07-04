package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.Instruction;
import sun.tools.asm.Label;
import sun.tools.java.ClassNotFound;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class ConditionalExpression extends BinaryExpression {

	public ConditionalExpression(final long l, final Expression expression, final Expression expression1, final Expression expression2) {
		super(13, l, Type.tError, expression1, expression2);
		this.cond = expression;
	}

	public Expression order() {
		if (this.precedence() > this.cond.precedence()) {
			final UnaryExpression unaryexpression = (UnaryExpression) this.cond;
			this.cond = unaryexpression.right;
			unaryexpression.right = this.order();
			return unaryexpression;
		}
		return this;
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		final ConditionVars conditionvars = this.cond.checkCondition(environment, context, vset, hashtable);
		vset = this.left.checkValue(environment, context, conditionvars.vsTrue, hashtable).join(this.right.checkValue(environment, context, conditionvars.vsFalse, hashtable));
		this.cond = this.convert(environment, context, Type.tBoolean, this.cond);
		final int i = this.left.type.getTypeMask() | this.right.type.getTypeMask();
		if ((i & 0x2000) != 0) {
			this.type = Type.tError;
			return vset;
		}
		if (this.left.type.equals(this.right.type)) {
			this.type = this.left.type;
		} else if ((i & 0x80) != 0) {
			this.type = Type.tDouble;
		} else if ((i & 0x40) != 0) {
			this.type = Type.tFloat;
		} else if ((i & 0x20) != 0) {
			this.type = Type.tLong;
		} else if ((i & 0x700) != 0) {
			try {
				this.type = environment.implicitCast(this.right.type, this.left.type) ? this.left.type : this.right.type;
			} catch (final ClassNotFound ignored) {
				this.type = Type.tError;
			}
		} else if ((i & 4) != 0 && this.left.fitsType(environment, context, Type.tChar) && this.right.fitsType(environment, context, Type.tChar)) {
			this.type = Type.tChar;
		} else if ((i & 8) != 0 && this.left.fitsType(environment, context, Type.tShort) && this.right.fitsType(environment, context, Type.tShort)) {
			this.type = Type.tShort;
		} else if ((i & 2) != 0 && this.left.fitsType(environment, context, Type.tByte) && this.right.fitsType(environment, context, Type.tByte)) {
			this.type = Type.tByte;
		} else {
			this.type = Type.tInt;
		}
		this.left = this.convert(environment, context, this.type, this.left);
		this.right = this.convert(environment, context, this.type, this.right);
		return vset;
	}

	public Vset check(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		vset = this.cond.checkValue(environment, context, vset, hashtable);
		this.cond = this.convert(environment, context, Type.tBoolean, this.cond);
		return this.left.check(environment, context, vset.copy(), hashtable).join(this.right.check(environment, context, vset, hashtable));
	}

	public boolean isConstant() {
		return this.cond.isConstant() && this.left.isConstant() && this.right.isConstant();
	}

	Expression simplify() {
		if (this.cond.equals(true)) {
			return this.left;
		}
		return this.cond.equals(false) ? this.right : this;
	}

	public Expression inline(final Environment environment, final Context context) {
		this.left = this.left.inline(environment, context);
		this.right = this.right.inline(environment, context);
		if (this.left == null && this.right == null) {
			return this.cond.inline(environment, context);
		}
		if (this.left == null) {
			this.left = this.right;
			this.right = null;
			this.cond = new NotExpression(this.where, this.cond);
		}
		this.cond = this.cond.inlineValue(environment, context);
		return this.simplify();
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		this.cond = this.cond.inlineValue(environment, context);
		this.left = this.left.inlineValue(environment, context);
		this.right = this.right.inlineValue(environment, context);
		return this.simplify();
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return 1 + this.cond.costInline(i, environment, context) + this.left.costInline(i, environment, context) + (this.right != null ? this.right.costInline(i, environment, context) : 0);
	}

	public Expression copyInline(final Context context) {
		final ConditionalExpression conditionalexpression = (ConditionalExpression) this.clone();
		conditionalexpression.cond = this.cond.copyInline(context);
		conditionalexpression.left = this.left.copyInline(context);
		conditionalexpression.right = this.right != null ? this.right.copyInline(context) : null;
		return conditionalexpression;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		final Label label = new Label();
		final Instruction label1 = new Label();
		this.cond.codeBranch(environment, context, assembler, label, false);
		this.left.codeValue(environment, context, assembler);
		assembler.add(this.where, 167, label1);
		assembler.add(label);
		this.right.codeValue(environment, context, assembler);
		assembler.add(label1);
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		final Label label = new Label();
		this.cond.codeBranch(environment, context, assembler, label, false);
		this.left.code(environment, context, assembler);
		if (this.right != null) {
			final Instruction label1 = new Label();
			assembler.add(this.where, 167, label1);
			assembler.add(label);
			this.right.code(environment, context, assembler);
			assembler.add(label1);
		} else {
			assembler.add(label);
		}
	}

	public void print(final PrintStream printstream) {
		printstream.print('(' + Constants.opNames[this.op] + ' ');
		this.cond.print(printstream);
		printstream.print(" ");
		this.left.print(printstream);
		printstream.print(" ");
		if (this.right != null) {
			this.right.print(printstream);
		} else {
			printstream.print("<null>");
		}
		printstream.print(")");
	}

	private Expression cond;
}
