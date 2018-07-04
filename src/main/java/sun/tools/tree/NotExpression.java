package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.Label;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class NotExpression extends UnaryExpression {

	public NotExpression(final long l, final Expression expression) {
		super(37, l, Type.tBoolean, expression);
	}

	void selectType(final Environment environment, final Context context, final int i) {
		this.right = this.convert(environment, context, Type.tBoolean, this.right);
	}

	public void checkCondition(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final ConditionVars conditionvars) {
		this.right.checkCondition(environment, context, vset, hashtable, conditionvars);
		this.right = this.convert(environment, context, Type.tBoolean, this.right);
		final Vset vset1 = conditionvars.vsFalse;
		conditionvars.vsFalse = conditionvars.vsTrue;
		conditionvars.vsTrue = vset1;
	}

	Expression eval(final boolean flag) {
		return new BooleanExpression(this.where, !flag);
	}

	Expression simplify() {
		final BinaryExpression binaryexpression;
		switch (((Node) this.right).op) {
		case 37: // '%'
			return ((UnaryExpression) (NotExpression) this.right).right;

		case 25: // '\031'
		case 26: // '\032'
		case 27: // '\033'
		case 28: // '\034'
		case 29: // '\035'
		case 30: // '\036'
		case 31: // '\037'
		case 32: // ' '
		case 33: // '!'
		case 34: // '"'
		case 35: // '#'
		case 36: // '$'
		default:
			return this;

		case 19: // '\023'
		case 20: // '\024'
		case 21: // '\025'
		case 22: // '\026'
		case 23: // '\027'
		case 24: // '\030'
			binaryexpression = (BinaryExpression) this.right;
			break;
		}
		if (binaryexpression.left.type.inMask(192)) {
			return this;
		}
		switch (((Node) this.right).op) {
		case 20: // '\024'
			return new NotEqualExpression(this.where, binaryexpression.left, ((UnaryExpression) binaryexpression).right);

		case 19: // '\023'
			return new EqualExpression(this.where, binaryexpression.left, ((UnaryExpression) binaryexpression).right);

		case 24: // '\030'
			return new GreaterOrEqualExpression(this.where, binaryexpression.left, ((UnaryExpression) binaryexpression).right);

		case 23: // '\027'
			return new GreaterExpression(this.where, binaryexpression.left, ((UnaryExpression) binaryexpression).right);

		case 22: // '\026'
			return new LessOrEqualExpression(this.where, binaryexpression.left, ((UnaryExpression) binaryexpression).right);

		case 21: // '\025'
			return new LessExpression(this.where, binaryexpression.left, ((UnaryExpression) binaryexpression).right);
		}
		return this;
	}

	void codeBranch(final Environment environment, final Context context, final Assembler assembler, final Label label, final boolean flag) {
		this.right.codeBranch(environment, context, assembler, label, !flag);
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.right.codeValue(environment, context, assembler);
		assembler.add(this.where, 18, new Integer(1));
		assembler.add(this.where, 130);
	}
}
