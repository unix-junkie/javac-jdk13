package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public abstract class BinaryBitExpression extends BinaryExpression {

	BinaryBitExpression(final int i, final long l, final Expression expression, final Expression expression1) {
		super(i, l, expression.type, expression, expression1);
	}

	void selectType(final Environment environment, final Context context, final int i) {
		if ((i & 1) != 0) {
			this.type = Type.tBoolean;
		} else if ((i & 0x20) != 0) {
			this.type = Type.tLong;
		} else {
			this.type = Type.tInt;
		}
		this.left = this.convert(environment, context, this.type, this.left);
		this.right = this.convert(environment, context, this.type, this.right);
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.left.codeValue(environment, context, assembler);
		this.right.codeValue(environment, context, assembler);
		this.codeOperation(environment, context, assembler);
	}
}
