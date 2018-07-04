package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.java.Environment;
import sun.tools.java.Type;

public abstract class BinaryLogicalExpression extends BinaryExpression {

	BinaryLogicalExpression(final int i, final long l, final Expression expression, final Expression expression1) {
		super(i, l, Type.tBoolean, expression, expression1);
	}

	public Vset checkValue(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		final ConditionVars conditionvars = new ConditionVars();
		this.checkCondition(environment, context, vset, hashtable, conditionvars);
		return conditionvars.vsTrue.join(conditionvars.vsFalse);
	}

	protected abstract void checkCondition(Environment environment, Context context, Vset vset, Hashtable hashtable, ConditionVars conditionvars);

	public Expression inline(final Environment environment, final Context context) {
		this.left = this.left.inlineValue(environment, context);
		this.right = this.right.inlineValue(environment, context);
		return this;
	}
}
