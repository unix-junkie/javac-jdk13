package sun.tools.tree;

import java.io.PrintStream;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class NullExpression extends ConstantExpression {

	public NullExpression(final long l) {
		super(84, l, Type.tNull);
	}

	public boolean equals(final int i) {
		return i == 0;
	}

	public boolean isNull() {
		return true;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 1);
	}

	public void print(final PrintStream printstream) {
		printstream.print("null");
	}
}
