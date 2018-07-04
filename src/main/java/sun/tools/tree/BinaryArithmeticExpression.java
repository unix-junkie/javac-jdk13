package sun.tools.tree;

import sun.tools.java.Environment;
import sun.tools.java.Type;

class BinaryArithmeticExpression extends BinaryExpression {

	BinaryArithmeticExpression(final int i, final long l, final Expression expression, final Expression expression1) {
		super(i, l, expression.type, expression, expression1);
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
		this.left = this.convert(environment, context, this.type, this.left);
		this.right = this.convert(environment, context, this.type, this.right);
	}
}
