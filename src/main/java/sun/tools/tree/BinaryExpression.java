package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.Instruction;
import sun.tools.asm.Label;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public class BinaryExpression extends UnaryExpression {

	BinaryExpression(final int i, final long l, final Type type, final Expression expression, final Expression expression1) {
		super(i, l, type, expression1);
		this.left = expression;
	}

	public Expression order() {
		if (this.precedence() > this.left.precedence()) {
			final UnaryExpression unaryexpression = (UnaryExpression) this.left;
			this.left = unaryexpression.right;
			unaryexpression.right = this.order();
			return unaryexpression;
		}
		return this;
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		vset = this.left.checkValue(environment, context, vset, hashtable);
		vset = this.right.checkValue(environment, context, vset, hashtable);
		final int i = this.left.type.getTypeMask() | this.right.type.getTypeMask();
		if ((i & 0x2000) != 0) {
			return vset;
		}
		this.selectType(environment, context, i);
		if (this.type.isType(13)) {
			environment.error(this.where, "invalid.args", Constants.opNames[this.op]);
		}
		return vset;
	}

	public boolean isConstant() {
		switch (this.op) {
		case 14: // '\016'
		case 15: // '\017'
		case 16: // '\020'
		case 17: // '\021'
		case 18: // '\022'
		case 19: // '\023'
		case 20: // '\024'
		case 21: // '\025'
		case 22: // '\026'
		case 23: // '\027'
		case 24: // '\030'
		case 26: // '\032'
		case 27: // '\033'
		case 28: // '\034'
		case 29: // '\035'
		case 30: // '\036'
		case 31: // '\037'
		case 32: // ' '
		case 33: // '!'
			return this.left.isConstant() && this.right.isConstant();

		case 25: // '\031'
		default:
			return false;
		}
	}

	Expression eval(final int i, final int j) {
		return this;
	}

	Expression eval(final long l, final long l1) {
		return this;
	}

	Expression eval(final float f, final float f1) {
		return this;
	}

	Expression eval(final double d, final double d1) {
		return this;
	}

	Expression eval(final boolean flag, final boolean flag1) {
		return this;
	}

	Expression eval(final String s, final String s1) {
		return this;
	}

	Expression eval() {
		if (((Node) this.left).op == ((Node) this.right).op) {
			switch (((Node) this.left).op) {
			case 62: // '>'
			case 63: // '?'
			case 64: // '@'
			case 65: // 'A'
				return this.eval(((IntegerExpression) this.left).value, ((IntegerExpression) this.right).value);

			case 66: // 'B'
				return this.eval(((LongExpression) this.left).value, ((LongExpression) this.right).value);

			case 67: // 'C'
				return this.eval(((FloatExpression) this.left).value, ((FloatExpression) this.right).value);

			case 68: // 'D'
				return this.eval(((DoubleExpression) this.left).value, ((DoubleExpression) this.right).value);

			case 61: // '='
				return this.eval(((BooleanExpression) this.left).value, ((BooleanExpression) this.right).value);

			case 69: // 'E'
				return this.eval(((StringExpression) this.left).value, ((StringExpression) this.right).value);
			}
		}
		return this;
	}

	public Expression inline(final Environment environment, final Context context) {
		this.left = this.left.inline(environment, context);
		this.right = this.right.inline(environment, context);
		return this.left != null ? new CommaExpression(this.where, this.left, this.right) : this.right;
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		this.left = this.left.inlineValue(environment, context);
		this.right = this.right.inlineValue(environment, context);
		try {
			return this.eval().simplify();
		} catch (final ArithmeticException ignored) {
			return this;
		}
	}

	public Expression copyInline(final Context context) {
		final BinaryExpression binaryexpression = (BinaryExpression) this.clone();
		if (this.left != null) {
			binaryexpression.left = this.left.copyInline(context);
		}
		if (this.right != null) {
			binaryexpression.right = this.right.copyInline(context);
		}
		return binaryexpression;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return 1 + (this.left == null ? 0 : this.left.costInline(i, environment, context)) + (this.right == null ? 0 : this.right.costInline(i, environment, context));
	}

	void codeOperation(final Environment environment, final Context context, final Assembler assembler) {
		throw new CompilerError("codeOperation: " + Constants.opNames[this.op]);
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		if (this.type.isType(0)) {
			final Label label = new Label();
			final Instruction label1 = new Label();
			this.codeBranch(environment, context, assembler, label, true);
			assembler.add(true, this.where, 18, new Integer(0));
			assembler.add(true, this.where, 167, label1);
			assembler.add(label);
			assembler.add(true, this.where, 18, new Integer(1));
			assembler.add(label1);
		} else {
			this.left.codeValue(environment, context, assembler);
			this.right.codeValue(environment, context, assembler);
			this.codeOperation(environment, context, assembler);
		}
	}

	public void print(final PrintStream printstream) {
		printstream.print('(' + Constants.opNames[this.op] + ' ');
		if (this.left != null) {
			this.left.print(printstream);
		} else {
			printstream.print("<null>");
		}
		printstream.print(" ");
		if (this.right != null) {
			this.right.print(printstream);
		} else {
			printstream.print("<null>");
		}
		printstream.print(")");
	}

	Expression left;
}
