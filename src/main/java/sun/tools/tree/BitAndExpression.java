package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;

public final class BitAndExpression extends BinaryBitExpression {

	public BitAndExpression(final long l, final Expression expression, final Expression expression1) {
		super(18, l, expression, expression1);
	}

	Expression eval(final boolean flag, final boolean flag1) {
		return new BooleanExpression(this.where, flag & flag1);
	}

	Expression eval(final int i, final int j) {
		return new IntExpression(this.where, i & j);
	}

	Expression eval(final long l, final long l1) {
		return new LongExpression(this.where, l & l1);
	}

	Expression simplify() {
		if (this.left.equals(true)) {
			return this.right;
		}
		if (this.right.equals(true)) {
			return this.left;
		}
		if (this.left.equals(false) || this.left.equals(0)) {
			return new CommaExpression(this.where, this.right, this.left).simplify();
		}
		return this.right.equals(false) || this.right.equals(0) ? new CommaExpression(this.where, this.left, this.right).simplify() : this;
	}

	void codeOperation(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 126 + this.type.getTypeCodeOffset());
	}
}
