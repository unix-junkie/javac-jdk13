package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.asm.Label;
import sun.tools.java.CompilerError;
import sun.tools.java.Environment;

public final class NotEqualExpression extends BinaryEqualityExpression {

	public NotEqualExpression(final long l, final Expression expression, final Expression expression1) {
		super(19, l, expression, expression1);
	}

	Expression eval(final int i, final int j) {
		return new BooleanExpression(this.where, i != j);
	}

	Expression eval(final long l, final long l1) {
		return new BooleanExpression(this.where, l != l1);
	}

	Expression eval(final float f, final float f1) {
		return new BooleanExpression(this.where, f != f1);
	}

	Expression eval(final double d, final double d1) {
		return new BooleanExpression(this.where, d != d1);
	}

	Expression eval(final boolean flag, final boolean flag1) {
		return new BooleanExpression(this.where, flag != flag1);
	}

	Expression simplify() {
		return this.left.isConstant() && !this.right.isConstant() ? new NotEqualExpression(this.where, this.right, this.left) : this;
	}

	void codeBranch(final Environment environment, final Context context, final Assembler assembler, final Label label, final boolean flag) {
		this.left.codeValue(environment, context, assembler);
		switch (this.left.type.getTypeCode()) {
		case 0: // '\0'
		case 4: // '\004'
			if (!this.right.equals(0)) {
				this.right.codeValue(environment, context, assembler);
				assembler.add(this.where, flag ? 160 : 159, label, flag);
				return;
			}
			break;

		case 5: // '\005'
			this.right.codeValue(environment, context, assembler);
			assembler.add(this.where, 148);
			break;

		case 6: // '\006'
			this.right.codeValue(environment, context, assembler);
			assembler.add(this.where, 149);
			break;

		case 7: // '\007'
			this.right.codeValue(environment, context, assembler);
			assembler.add(this.where, 151);
			break;

		case 8: // '\b'
		case 9: // '\t'
		case 10: // '\n'
			if (this.right.equals(0)) {
				assembler.add(this.where, flag ? 199 : 198, label, flag);
			} else {
				this.right.codeValue(environment, context, assembler);
				assembler.add(this.where, flag ? 166 : 165, label, flag);
			}
			return;

		case 1: // '\001'
		case 2: // '\002'
		case 3: // '\003'
		default:
			throw new CompilerError("Unexpected Type");
		}
		assembler.add(this.where, flag ? 154 : 153, label, flag);
	}
}
