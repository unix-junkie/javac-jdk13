package sun.tools.tree;

import sun.tools.java.Environment;
import sun.tools.java.Type;

class BinaryShiftExpression extends BinaryExpression {

	BinaryShiftExpression(final int i, final long l, final Expression expression, final Expression expression1) {
		super(i, l, expression.type, expression, expression1);
	}

	Expression eval() {
		return ((Node) this.left).op == 66 && ((Node) this.right).op == 65 ? this.eval(((LongExpression) this.left).value, ((IntegerExpression) (IntExpression) this.right).value) : super.eval();
	}

	void selectType(final Environment environment, final Context context, final int i) {
		if (this.left.type == Type.tLong) {
			this.type = Type.tLong;
		} else if (this.left.type.inMask(62)) {
			this.type = Type.tInt;
			this.left = this.convert(environment, context, this.type, this.left);
		} else {
			this.type = Type.tError;
		}
		this.right = this.right.type.inMask(62) ? new ConvertExpression(this.where, Type.tInt, this.right) : this.convert(environment, context, Type.tInt, this.right);
	}
}
