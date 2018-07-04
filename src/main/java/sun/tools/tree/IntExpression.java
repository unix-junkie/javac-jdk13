package sun.tools.tree;

import java.io.PrintStream;

import sun.tools.java.Type;

public final class IntExpression extends IntegerExpression {

	public IntExpression(final long l, final int i) {
		super(65, l, Type.tInt, i);
	}

	public boolean equals(final Object obj) {
		return obj instanceof IntExpression && this.value == ((IntegerExpression) (IntExpression) obj).value;
	}

	public int hashCode() {
		return this.value;
	}

	public void print(final PrintStream printstream) {
		printstream.print(this.value);
	}
}
