package sun.tools.tree;

import java.io.PrintStream;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class LongExpression extends ConstantExpression {

	public LongExpression(final long l, final long l1) {
		super(66, l, Type.tLong);
		this.value = l1;
	}

	public Object getValue() {
		return new Long(this.value);
	}

	public boolean equals(final int i) {
		return this.value == i;
	}

	public boolean equalsDefault() {
		return this.value == 0L;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 20, new Long(this.value));
	}

	public void print(final PrintStream printstream) {
		printstream.print(this.value + "L");
	}

	final long value;
}
