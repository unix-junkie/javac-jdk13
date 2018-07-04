package sun.tools.tree;

import java.io.PrintStream;

import sun.tools.java.Type;

public final class ByteExpression extends IntegerExpression {
	ByteExpression(final long l, final byte byte0) {
		super(62, l, Type.tByte, byte0);
	}

	public void print(final PrintStream printstream) {
		printstream.print(this.value + "b");
	}
}
