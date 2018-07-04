package sun.tools.tree;

import java.io.PrintStream;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class DoubleExpression extends ConstantExpression {

	public DoubleExpression(final long l, final double d) {
		super(68, l, Type.tDouble);
		this.value = d;
	}

	public Object getValue() {
		return new Double(this.value);
	}

	public boolean equals(final int i) {
		return this.value == i;
	}

	public boolean equalsDefault() {
		return Double.doubleToLongBits(this.value) == 0L;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 20, new Double(this.value));
	}

	public void print(final PrintStream printstream) {
		printstream.print(this.value + "D");
	}

	final double value;
}
