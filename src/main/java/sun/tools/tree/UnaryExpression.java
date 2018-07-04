package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public class UnaryExpression extends Expression {

	UnaryExpression(final int i, final long l, final Type type, final Expression expression) {
		super(i, l, type);
		this.right = expression;
	}

	public Expression order() {
		if (this.precedence() > this.right.precedence()) {
			final UnaryExpression unaryexpression = (UnaryExpression) this.right;
			this.right = unaryexpression.right;
			unaryexpression.right = this.order();
			return unaryexpression;
		}
		return this;
	}

	void selectType(final Environment environment, final Context context, final int i) {
		throw new CompilerError("selectType: " + Constants.opNames[this.op]);
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		vset = this.right.checkValue(environment, context, vset, hashtable);
		final int i = this.right.type.getTypeMask();
		this.selectType(environment, context, i);
		if ((i & 0x2000) == 0 && this.type.isType(13)) {
			environment.error(this.where, "invalid.arg", Constants.opNames[this.op]);
		}
		return vset;
	}

	public boolean isConstant() {
		switch (this.op) {
		case 35: // '#'
		case 36: // '$'
		case 37: // '%'
		case 38: // '&'
		case 55: // '7'
		case 56: // '8'
			return this.right.isConstant();
		}
		return false;
	}

	Expression eval(final int i) {
		return this;
	}

	Expression eval(final long l) {
		return this;
	}

	Expression eval(final float f) {
		return this;
	}

	Expression eval(final double d) {
		return this;
	}

	Expression eval(final boolean flag) {
		return this;
	}

	private Expression eval(final String s) {
		return this;
	}

	Expression eval() {
		switch (((Node) this.right).op) {
		case 62: // '>'
		case 63: // '?'
		case 64: // '@'
		case 65: // 'A'
			return this.eval(((IntegerExpression) this.right).value);

		case 66: // 'B'
			return this.eval(((LongExpression) this.right).value);

		case 67: // 'C'
			return this.eval(((FloatExpression) this.right).value);

		case 68: // 'D'
			return this.eval(((DoubleExpression) this.right).value);

		case 61: // '='
			return this.eval(((BooleanExpression) this.right).value);

		case 69: // 'E'
			return this.eval(((StringExpression) this.right).value);
		}
		return this;
	}

	public Expression inline(final Environment environment, final Context context) {
		return this.right.inline(environment, context);
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		this.right = this.right.inlineValue(environment, context);
		try {
			return this.eval().simplify();
		} catch (final ArithmeticException ignored) {
			return this;
		}
	}

	public Expression copyInline(final Context context) {
		final UnaryExpression unaryexpression = (UnaryExpression) this.clone();
		if (this.right != null) {
			unaryexpression.right = this.right.copyInline(context);
		}
		return unaryexpression;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return 1 + this.right.costInline(i, environment, context);
	}

	public void print(final PrintStream printstream) {
		printstream.print('(' + Constants.opNames[this.op] + ' ');
		this.right.print(printstream);
		printstream.print(")");
	}

	Expression right;
}
