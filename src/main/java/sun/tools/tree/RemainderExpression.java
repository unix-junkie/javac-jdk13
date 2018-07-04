package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;

public final class RemainderExpression extends DivRemExpression {

	public RemainderExpression(final long l, final Expression expression, final Expression expression1) {
		super(32, l, expression, expression1);
	}

	Expression eval(final int i, final int j) {
		return new IntExpression(this.where, i % j);
	}

	Expression eval(final long l, final long l1) {
		return new LongExpression(this.where, l % l1);
	}

	Expression eval(final float f, final float f1) {
		return new FloatExpression(this.where, f % f1);
	}

	Expression eval(final double d, final double d1) {
		return new DoubleExpression(this.where, d % d1);
	}

	void codeOperation(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 112 + this.type.getTypeCodeOffset());
	}
}
