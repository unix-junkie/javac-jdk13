package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public class IntegerExpression extends ConstantExpression {

	IntegerExpression(final int i, final long l, final Type type, final int j) {
		super(i, l, type);
		this.value = j;
	}

	public boolean fitsType(final Environment environment, final Context context, final Type type) {
		if (super.type.isType(2)) {
			return super.fitsType(environment, context, type);
		}
		switch (type.getTypeCode()) {
		case 1: // '\001'
			return this.value == (byte) this.value;

		case 3: // '\003'
			return this.value == (short) this.value;

		case 2: // '\002'
			return this.value == (char) this.value;
		}
		return super.fitsType(environment, context, type);
	}

	public Object getValue() {
		return new Integer(this.value);
	}

	public boolean equals(final int i) {
		return this.value == i;
	}

	public boolean equalsDefault() {
		return this.value == 0;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 18, new Integer(this.value));
	}

	final int value;
}
