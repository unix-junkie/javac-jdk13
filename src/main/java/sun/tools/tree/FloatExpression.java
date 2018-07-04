package sun.tools.tree;

import java.io.PrintStream;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class FloatExpression extends ConstantExpression {

	public FloatExpression(final long l, final float f) {
		super(67, l, Type.tFloat);
		this.value = f;
	}

	public Object getValue() {
		return new Float(this.value);
	}

	public boolean equals(final int i) {
		return this.value == i;
	}

	public boolean equalsDefault() {
		return Float.floatToIntBits(this.value) == 0;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 18, new Float(this.value));
	}

	public void print(final PrintStream printstream) {
		printstream.print(this.value + "F");
	}

	final float value;
}
