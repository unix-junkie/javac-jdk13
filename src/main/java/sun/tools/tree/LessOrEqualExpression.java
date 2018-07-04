package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.asm.Label;
import sun.tools.java.CompilerError;
import sun.tools.java.Environment;

public final class LessOrEqualExpression extends BinaryCompareExpression {

	public LessOrEqualExpression(final long l, final Expression expression, final Expression expression1) {
		super(23, l, expression, expression1);
	}

	Expression eval(final int i, final int j) {
		return new BooleanExpression(this.where, i <= j);
	}

	Expression eval(final long l, final long l1) {
		return new BooleanExpression(this.where, l <= l1);
	}

	Expression eval(final float f, final float f1) {
		return new BooleanExpression(this.where, f <= f1);
	}

	Expression eval(final double d, final double d1) {
		return new BooleanExpression(this.where, d <= d1);
	}

	Expression simplify() {
		return this.left.isConstant() && !this.right.isConstant() ? (Expression) new GreaterOrEqualExpression(this.where, this.right, this.left) : this;
	}

	void codeBranch(final Environment environment, final Context context, final Assembler assembler, final Label label, final boolean flag) {
		this.left.codeValue(environment, context, assembler);
		switch (this.left.type.getTypeCode()) {
		case 4: // '\004'
			if (!this.right.equals(0)) {
				this.right.codeValue(environment, context, assembler);
				assembler.add(this.where, flag ? 164 : 163, label, flag);
				return;
			}
			break;

		case 5: // '\005'
			this.right.codeValue(environment, context, assembler);
			assembler.add(this.where, 148);
			break;

		case 6: // '\006'
			this.right.codeValue(environment, context, assembler);
			assembler.add(this.where, 150);
			break;

		case 7: // '\007'
			this.right.codeValue(environment, context, assembler);
			assembler.add(this.where, 152);
			break;

		default:
			throw new CompilerError("Unexpected Type");
		}
		assembler.add(this.where, flag ? 158 : 157, label, flag);
	}
}
