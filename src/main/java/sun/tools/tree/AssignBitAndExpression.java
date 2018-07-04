package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;

public final class AssignBitAndExpression extends AssignOpExpression {

	public AssignBitAndExpression(final long l, final Expression expression, final Expression expression1) {
		super(10, l, expression, expression1);
	}

	void codeOperation(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 126 + this.itype.getTypeCodeOffset());
	}
}
