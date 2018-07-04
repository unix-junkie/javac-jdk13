package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;

public final class PostDecExpression extends IncDecExpression {

	public PostDecExpression(final long l, final Expression expression) {
		super(45, l, expression);
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.codeIncDec(environment, context, assembler, false, false, true);
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		this.codeIncDec(environment, context, assembler, false, false, false);
	}
}
