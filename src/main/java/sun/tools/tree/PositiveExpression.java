package sun.tools.tree;

import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class PositiveExpression extends UnaryExpression {

	public PositiveExpression(final long l, final Expression expression) {
		super(35, l, expression.type, expression);
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

	Expression simplify() {
		return this.right;
	}
}
