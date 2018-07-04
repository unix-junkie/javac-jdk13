package sun.tools.tree;

import java.io.PrintStream;

import sun.tools.java.Type;

public final class ShortExpression extends IntegerExpression {
	ShortExpression(final long l, final short word0) {
		super(64, l, Type.tShort, word0);
	}

	public void print(final PrintStream printstream) {
		printstream.print(this.value + "s");
	}
}
