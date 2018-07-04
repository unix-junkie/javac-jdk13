package sun.tools.tree;

import java.io.PrintStream;

import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public class NaryExpression extends UnaryExpression {

	NaryExpression(final int i, final long l, final Type type, final Expression expression, final Expression aexpression[]) {
		super(i, l, type, expression);
		this.args = aexpression;
	}

	public Expression copyInline(final Context context) {
		final NaryExpression naryexpression = (NaryExpression) this.clone();
		if (this.right != null) {
			naryexpression.right = this.right.copyInline(context);
		}
		naryexpression.args = new Expression[this.args.length];
		for (int i = 0; i < this.args.length; i++) {
			if (this.args[i] != null) {
				naryexpression.args[i] = this.args[i].copyInline(context);
			}
		}

		return naryexpression;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		int j = 3;
		if (this.right != null) {
			j += this.right.costInline(i, environment, context);
		}
		for (int k = 0; k < this.args.length && j < i; k++) {
			if (this.args[k] != null) {
				j += this.args[k].costInline(i, environment, context);
			}
		}

		return j;
	}

	public void print(final PrintStream printstream) {
		printstream.print('(' + Constants.opNames[this.op] + '#' + this.hashCode());
		if (this.right != null) {
			printstream.print(" ");
			this.right.print(printstream);
		}
		for (int i = 0; i < this.args.length; i++) {
			printstream.print(" ");
			if (this.args[i] != null) {
				this.args[i].print(printstream);
			} else {
				printstream.print("<null>");
			}
		}

		printstream.print(")");
	}

	Expression args[];
}
