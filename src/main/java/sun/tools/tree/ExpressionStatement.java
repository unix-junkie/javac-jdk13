package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;

public final class ExpressionStatement extends Statement {

	public ExpressionStatement(final long l, final Expression expression) {
		super(106, l);
		this.expr = expression;
	}

	Vset check(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		this.checkLabel(environment, context);
		return this.expr.check(environment, context, this.reach(environment, vset), hashtable);
	}

	public Statement inline(final Environment environment, final Context context) {
		if (this.expr != null) {
			this.expr = this.expr.inline(environment, context);
			return this.expr != null ? this : null;
		}
		return null;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final ExpressionStatement expressionstatement = (ExpressionStatement) this.clone();
		expressionstatement.expr = this.expr.copyInline(context);
		return expressionstatement;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return this.expr.costInline(i, environment, context);
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		this.expr.code(environment, context, assembler);
	}

	public Expression firstConstructor() {
		return this.expr.firstConstructor();
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		if (this.expr != null) {
			this.expr.print(printstream);
		} else {
			printstream.print("<empty>");
		}
		printstream.print(";");
	}

	private Expression expr;
}
