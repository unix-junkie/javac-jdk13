package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.Label;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class AndExpression extends BinaryLogicalExpression {

	public AndExpression(final long l, final Expression expression, final Expression expression1) {
		super(15, l, expression, expression1);
	}

	public void checkCondition(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final ConditionVars conditionvars) {
		this.left.checkCondition(environment, context, vset, hashtable, conditionvars);
		this.left = this.convert(environment, context, Type.tBoolean, this.left);
		final Vset vset1 = conditionvars.vsTrue.copy();
		final Vset vset2 = conditionvars.vsFalse.copy();
		this.right.checkCondition(environment, context, vset1, hashtable, conditionvars);
		this.right = this.convert(environment, context, Type.tBoolean, this.right);
		conditionvars.vsFalse = conditionvars.vsFalse.join(vset2);
	}

	Expression eval(final boolean flag, final boolean flag1) {
		return new BooleanExpression(this.where, flag && flag1);
	}

	Expression simplify() {
		if (this.left.equals(true)) {
			return this.right;
		}
		if (this.right.equals(false)) {
			return new CommaExpression(this.where, this.left, this.right).simplify();
		}
		if (this.right.equals(true)) {
			return this.left;
		}
		return this.left.equals(false) ? this.left : this;
	}

	void codeBranch(final Environment environment, final Context context, final Assembler assembler, final Label label, final boolean flag) {
		if (flag) {
			final Label label1 = new Label();
			this.left.codeBranch(environment, context, assembler, label1, false);
			this.right.codeBranch(environment, context, assembler, label, true);
			assembler.add(label1);
		} else {
			this.left.codeBranch(environment, context, assembler, label, false);
			this.right.codeBranch(environment, context, assembler, label, false);
		}
	}
}
