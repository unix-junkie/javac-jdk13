package sun.tools.tree;

import sun.tools.java.Environment;
import sun.tools.java.Type;

class BinaryCompareExpression extends BinaryExpression {

	BinaryCompareExpression(final int i, final long l, final Expression expression, final Expression expression1) {
		super(i, l, Type.tBoolean, expression, expression1);
	}

	void selectType(final Environment environment, final Context context, final int i) {
		Type type = Type.tInt;
		if ((i & 0x80) != 0) {
			type = Type.tDouble;
		} else if ((i & 0x40) != 0) {
			type = Type.tFloat;
		} else if ((i & 0x20) != 0) {
			type = Type.tLong;
		}
		this.left = this.convert(environment, context, type, this.left);
		this.right = this.convert(environment, context, type, this.right);
	}
}
