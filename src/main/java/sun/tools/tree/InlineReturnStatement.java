package sun.tools.tree;

import java.io.PrintStream;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;

public final class InlineReturnStatement extends Statement {

	InlineReturnStatement(final long l, final Expression expression) {
		super(149, l);
		this.expr = expression;
	}

	private static Context getDestination(Context context) {
		for (; context != null; context = context.prev) {
			if (context.node != null && (context.node.op == 150 || context.node.op == 151)) {
				return context;
			}
		}

		return null;
	}

	public Statement inline(final Environment environment, final Context context) {
		if (this.expr != null) {
			this.expr = this.expr.inlineValue(environment, context);
		}
		return this;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final InlineReturnStatement inlinereturnstatement = (InlineReturnStatement) this.clone();
		if (this.expr != null) {
			inlinereturnstatement.expr = this.expr.copyInline(context);
		}
		return inlinereturnstatement;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return 1 + (this.expr == null ? 0 : this.expr.costInline(i, environment, context));
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		if (this.expr != null) {
			this.expr.codeValue(environment, context, assembler);
		}
		final CodeContext codecontext = (CodeContext) getDestination(context);
		assembler.add(this.where, 167, codecontext.breakLabel);
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("inline-return");
		if (this.expr != null) {
			printstream.print(" ");
			this.expr.print(printstream);
		}
		printstream.print(";");
	}

	Expression expr;
}
