package sun.tools.tree;

import java.io.PrintStream;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class StringExpression extends ConstantExpression {

	public StringExpression(final long l, final String s) {
		super(69, l, Type.tString);
		this.value = s;
	}

	public boolean equals(final String s) {
		return this.value.equals(s);
	}

	public boolean isNonNull() {
		return true;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 18, this);
	}

	public Object getValue() {
		return this.value;
	}

	public int hashCode() {
		return this.value.hashCode() ^ 0xc8d;
	}

	public boolean equals(final Object obj) {
		return obj instanceof StringExpression && this.value.equals(((StringExpression) obj).value);
	}

	public void print(final PrintStream printstream) {
		printstream.print('"' + this.value + '"');
	}

	final String value;
}
