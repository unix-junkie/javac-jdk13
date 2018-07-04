package sun.tools.tree;

import sun.tools.java.ClassNotFound;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

class BinaryEqualityExpression extends BinaryExpression {

	BinaryEqualityExpression(final int i, final long l, final Expression expression, final Expression expression1) {
		super(i, l, Type.tBoolean, expression, expression1);
	}

	void selectType(final Environment environment, final Context context, final int i) {
		if ((i & 0x2000) != 0) {
			return;
		}
		if ((i & 0x700) != 0) {
			try {
				if (environment.explicitCast(this.left.type, this.right.type) || environment.explicitCast(this.right.type, this.left.type)) {
					return;
				}
				environment.error(this.where, "incompatible.type", this.left.type, this.left.type, this.right.type);
			} catch (final ClassNotFound classnotfound) {
				environment.error(this.where, "class.not.found", classnotfound.name, Constants.opNames[this.op]);
			}
			return;
		}
		final Type type;
		if ((i & 0x80) != 0) {
			type = Type.tDouble;
		} else if ((i & 0x40) != 0) {
			type = Type.tFloat;
		} else if ((i & 0x20) != 0) {
			type = Type.tLong;
		} else if ((i & 1) != 0) {
			type = Type.tBoolean;
		} else {
			type = Type.tInt;
		}
		this.left = this.convert(environment, context, type, this.left);
		this.right = this.convert(environment, context, type, this.right);
	}
}
