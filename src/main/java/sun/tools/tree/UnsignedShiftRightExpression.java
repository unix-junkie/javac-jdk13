package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;

public final class UnsignedShiftRightExpression extends BinaryShiftExpression {

	public UnsignedShiftRightExpression(final long l, final Expression expression, final Expression expression1) {
		super(28, l, expression, expression1);
	}

	Expression eval(final int i, final int j) {
		return new IntExpression(this.where, i >>> j);
	}

	Expression eval(final long l, final long l1) {
		return new LongExpression(this.where, l >>> l1);
	}

	Expression simplify() {
		if (this.right.equals(0)) {
			return this.left;
		}
		return this.left.equals(0) ? new CommaExpression(this.where, this.right, this.left).simplify() : this;
	}

	void codeOperation(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 124 + this.type.getTypeCodeOffset());
	}
}
