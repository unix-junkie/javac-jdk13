package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class BitNotExpression extends UnaryExpression {

	public BitNotExpression(final long l, final Expression expression) {
		super(38, l, expression.type, expression);
	}

	void selectType(final Environment environment, final Context context, final int i) {
		this.type = (i & 0x20) != 0 ? Type.tLong : Type.tInt;
		this.right = this.convert(environment, context, this.type, this.right);
	}

	Expression eval(final int i) {
		return new IntExpression(this.where, ~i);
	}

	Expression eval(final long l) {
		return new LongExpression(this.where, ~l);
	}

	Expression simplify() {
		return ((Node) this.right).op == 38 ? ((UnaryExpression) (BitNotExpression) this.right).right : this;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.right.codeValue(environment, context, assembler);
		if (this.type.isType(4)) {
			assembler.add(this.where, 18, new Integer(-1));
			assembler.add(this.where, 130);
		} else {
			assembler.add(this.where, 20, new Long(-1L));
			assembler.add(this.where, 131);
		}
	}
}
