package sun.tools.tree;

import sun.tools.java.Type;

class ConstantExpression extends Expression {

	ConstantExpression(final int i, final long l, final Type type) {
		super(i, l, type);
	}

	public boolean isConstant() {
		return true;
	}
}
