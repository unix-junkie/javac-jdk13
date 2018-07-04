package sun.tools.asm;

import java.io.DataOutputStream;
import java.io.IOException;

import sun.tools.java.ClassDeclaration;
import sun.tools.java.Environment;
import sun.tools.java.Type;

final class ClassConstantData extends ConstantPoolData {

	ClassConstantData(final ConstantPool constantpool, final ClassDeclaration classdeclaration) {
		final String s = classdeclaration.getType().getTypeSignature();
		this.name = s.substring(1, s.length() - 1);
		constantpool.put(this.name);
	}

	ClassConstantData(final ConstantPool constantpool, final Type type) {
		this.name = type.getTypeSignature();
		constantpool.put(this.name);
	}

	void write(final Environment environment, final DataOutputStream dataoutputstream, final ConstantPool constantpool) throws IOException {
		dataoutputstream.writeByte(7);
		dataoutputstream.writeShort(constantpool.index(this.name));
	}

	int order() {
		return 1;
	}

	public String toString() {
		return "ClassConstantData[" + this.name + ']';
	}

	final String name;
}
