package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.java.Environment;

public class BinaryAssignExpression extends BinaryExpression {

	BinaryAssignExpression(final int i, final long l, final Expression expression, final Expression expression1) {
		super(i, l, expression.type, expression, expression1);
	}

	public Expression order() {
		if (this.precedence() >= this.left.precedence()) {
			final UnaryExpression unaryexpression = (UnaryExpression) this.left;
			this.left = unaryexpression.right;
			unaryexpression.right = this.order();
			return unaryexpression;
		}
		return this;
	}

	public Vset check(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		return this.checkValue(environment, context, vset, hashtable);
	}

	public Expression inline(final Environment environment, final Context context) {
		return this.implementation != null ? this.implementation.inline(environment, context) : this.inlineValue(environment, context);
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		if (this.implementation != null) {
			return this.implementation.inlineValue(environment, context);
		}
		this.left = this.left.inlineLHS(environment, context);
		this.right = this.right.inlineValue(environment, context);
		return this;
	}

	public Expression copyInline(final Context context) {
		return this.implementation != null ? this.implementation.copyInline(context) : super.copyInline(context);
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return this.implementation != null ? this.implementation.costInline(i, environment, context) : super.costInline(i, environment, context);
	}

	Expression implementation;
}
