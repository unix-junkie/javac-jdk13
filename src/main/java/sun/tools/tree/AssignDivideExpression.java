package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;

public final class AssignDivideExpression extends AssignOpExpression {

	public AssignDivideExpression(final long l, final Expression expression, final Expression expression1) {
		super(3, l, expression, expression1);
	}

	void codeOperation(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 108 + this.itype.getTypeCodeOffset());
	}
}
