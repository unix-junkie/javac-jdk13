package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.Label;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class OrExpression extends BinaryLogicalExpression {

	public OrExpression(final long l, final Expression expression, final Expression expression1) {
		super(14, l, expression, expression1);
	}

	public void checkCondition(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final ConditionVars conditionvars) {
		this.left.checkCondition(environment, context, vset, hashtable, conditionvars);
		this.left = this.convert(environment, context, Type.tBoolean, this.left);
		final Vset vset1 = conditionvars.vsTrue.copy();
		final Vset vset2 = conditionvars.vsFalse.copy();
		this.right.checkCondition(environment, context, vset2, hashtable, conditionvars);
		this.right = this.convert(environment, context, Type.tBoolean, this.right);
		conditionvars.vsTrue = conditionvars.vsTrue.join(vset1);
	}

	Expression eval(final boolean flag, final boolean flag1) {
		return new BooleanExpression(this.where, flag || flag1);
	}

	Expression simplify() {
		if (this.right.equals(false)) {
			return this.left;
		}
		if (this.left.equals(true)) {
			return this.left;
		}
		if (this.left.equals(false)) {
			return this.right;
		}
		return this.right.equals(true) ? new CommaExpression(this.where, this.left, this.right).simplify() : this;
	}

	void codeBranch(final Environment environment, final Context context, final Assembler assembler, final Label label, final boolean flag) {
		if (flag) {
			this.left.codeBranch(environment, context, assembler, label, true);
			this.right.codeBranch(environment, context, assembler, label, true);
		} else {
			final Label label1 = new Label();
			this.left.codeBranch(environment, context, assembler, label1, true);
			this.right.codeBranch(environment, context, assembler, label, false);
			assembler.add(label1);
		}
	}
}
