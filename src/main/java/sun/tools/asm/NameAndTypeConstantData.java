package sun.tools.asm;

import java.io.DataOutputStream;
import java.io.IOException;

import sun.tools.java.Environment;

final class NameAndTypeConstantData extends ConstantPoolData {

	NameAndTypeConstantData(final ConstantPool constantpool, final NameAndTypeData nameandtypedata) {
		this.name = nameandtypedata.field.getName().toString();
		this.type = nameandtypedata.field.getType().getTypeSignature();
		constantpool.put(this.name);
		constantpool.put(this.type);
	}

	void write(final Environment environment, final DataOutputStream dataoutputstream, final ConstantPool constantpool) throws IOException {
		dataoutputstream.writeByte(12);
		dataoutputstream.writeShort(constantpool.index(this.name));
		dataoutputstream.writeShort(constantpool.index(this.type));
	}

	int order() {
		return 3;
	}

	final String name;
	final String type;
}
