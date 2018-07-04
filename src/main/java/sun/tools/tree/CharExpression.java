package sun.tools.tree;

import java.io.PrintStream;

import sun.tools.java.Type;

public final class CharExpression extends IntegerExpression {

	public CharExpression(final long l, final char c) {
		super(63, l, Type.tChar, c);
	}

	public void print(final PrintStream printstream) {
		printstream.print(this.value + "c");
	}
}
