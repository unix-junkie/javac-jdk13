package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;

public final class PreIncExpression extends IncDecExpression {

	public PreIncExpression(final long l, final Expression expression) {
		super(39, l, expression);
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.codeIncDec(environment, context, assembler, true, true, true);
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		this.codeIncDec(environment, context, assembler, true, true, false);
	}
}
