package sun.tools.tree;

import sun.tools.java.Environment;

public abstract class DivRemExpression extends BinaryArithmeticExpression {

	DivRemExpression(final int i, final long l, final Expression expression, final Expression expression1) {
		super(i, l, expression, expression1);
	}

	public Expression inline(final Environment environment, final Context context) {
		if (this.type.inMask(62)) {
			this.right = this.right.inlineValue(environment, context);
			if (this.right.isConstant() && !this.right.equals(0)) {
				this.left = this.left.inline(environment, context);
				return this.left;
			}
			this.left = this.left.inlineValue(environment, context);
			try {
				return this.eval().simplify();
			} catch (final ArithmeticException ignored) {
				environment.error(this.where, "arithmetic.exception");
			}
			return this;
		}
		return super.inline(environment, context);
	}
}
