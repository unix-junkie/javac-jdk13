package sun.tools.asm;

import java.io.DataOutputStream;
import java.io.IOException;

import sun.tools.java.Environment;

final class StringConstantData extends ConstantPoolData {

	StringConstantData(final ConstantPool constantpool, final String s) {
		this.str = s;
	}

	void write(final Environment environment, final DataOutputStream dataoutputstream, final ConstantPool constantpool) throws IOException {
		dataoutputstream.writeByte(1);
		dataoutputstream.writeUTF(this.str);
	}

	int order() {
		return 4;
	}

	public String toString() {
		return "StringConstantData[" + this.str + "]=" + this.str.hashCode();
	}

	final String str;
}
