package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;

public final class PostIncExpression extends IncDecExpression {

	public PostIncExpression(final long l, final Expression expression) {
		super(44, l, expression);
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.codeIncDec(environment, context, assembler, true, false, true);
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		this.codeIncDec(environment, context, assembler, true, false, false);
	}
}
