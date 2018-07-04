package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class CaseStatement extends Statement {

	public CaseStatement(final long l, final Expression expression) {
		super(96, l);
		this.expr = expression;
	}

	Vset check(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		if (this.expr != null) {
			this.expr.checkValue(environment, context, vset, hashtable);
			this.expr = this.convert(environment, context, Type.tInt, this.expr);
			this.expr = this.expr.inlineValue(environment, context);
		}
		return vset.clearDeadEnd();
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return 6;
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		if (this.expr == null) {
			printstream.print("default");
		} else {
			printstream.print("case ");
			this.expr.print(printstream);
		}
		printstream.print(":");
	}

	Expression expr;
}
