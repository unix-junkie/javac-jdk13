package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.java.Environment;

public final class ExprExpression extends UnaryExpression {

	public ExprExpression(final long l, final Expression expression) {
		super(56, l, expression.type, expression);
	}

	public void checkCondition(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final ConditionVars conditionvars) {
		this.right.checkCondition(environment, context, vset, hashtable, conditionvars);
		this.type = this.right.type;
	}

	public Vset checkAssignOp(final Environment environment, final Context context, Vset vset, final Hashtable hashtable, final Expression expression) {
		vset = this.right.checkAssignOp(environment, context, vset, hashtable, expression);
		this.type = this.right.type;
		return vset;
	}

	public FieldUpdater getUpdater(final Environment environment, final Context context) {
		return this.right.getUpdater(environment, context);
	}

	public boolean isNull() {
		return this.right.isNull();
	}

	public boolean isNonNull() {
		return this.right.isNonNull();
	}

	public Object getValue() {
		return this.right.getValue();
	}

	protected StringBuffer inlineValueSB(final Environment environment, final Context context, final StringBuffer stringbuffer) {
		return this.right.inlineValueSB(environment, context, stringbuffer);
	}

	void selectType(final Environment environment, final Context context, final int i) {
		this.type = this.right.type;
	}

	Expression simplify() {
		return this.right;
	}
}
