package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class NegativeExpression extends UnaryExpression {

	public NegativeExpression(final long l, final Expression expression) {
		super(36, l, expression.type, expression);
	}

	void selectType(final Environment environment, final Context context, final int i) {
		if ((i & 0x80) != 0) {
			this.type = Type.tDouble;
		} else if ((i & 0x40) != 0) {
			this.type = Type.tFloat;
		} else if ((i & 0x20) != 0) {
			this.type = Type.tLong;
		} else {
			this.type = Type.tInt;
		}
		this.right = this.convert(environment, context, this.type, this.right);
	}

	Expression eval(final int i) {
		return new IntExpression(this.where, -i);
	}

	Expression eval(final long l) {
		return new LongExpression(this.where, -l);
	}

	Expression eval(final float f) {
		return new FloatExpression(this.where, -f);
	}

	Expression eval(final double d) {
		return new DoubleExpression(this.where, -d);
	}

	Expression simplify() {
		return ((Node) this.right).op == 36 ? ((UnaryExpression) (NegativeExpression) this.right).right : this;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.right.codeValue(environment, context, assembler);
		assembler.add(this.where, 116 + this.type.getTypeCodeOffset());
	}
}
