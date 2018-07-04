package sun.tools.asm;

import java.io.DataOutputStream;
import java.io.IOException;

import sun.tools.java.Environment;
import sun.tools.tree.StringExpression;

final class StringExpressionConstantData extends ConstantPoolData {

	StringExpressionConstantData(final ConstantPool constantpool, final StringExpression stringexpression) {
		this.str = stringexpression;
		constantpool.put(stringexpression.getValue());
	}

	void write(final Environment environment, final DataOutputStream dataoutputstream, final ConstantPool constantpool) throws IOException {
		dataoutputstream.writeByte(8);
		dataoutputstream.writeShort(constantpool.index(this.str.getValue()));
	}

	public String toString() {
		return "StringExpressionConstantData[" + this.str.getValue() + "]=" + this.str.getValue().hashCode();
	}

	final StringExpression str;
}
