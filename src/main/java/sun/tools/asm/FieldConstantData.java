package sun.tools.asm;

import java.io.DataOutputStream;
import java.io.IOException;

import sun.tools.java.Environment;
import sun.tools.java.MemberDefinition;

final class FieldConstantData extends ConstantPoolData {

	FieldConstantData(final ConstantPool constantpool, final MemberDefinition memberdefinition) {
		this.field = memberdefinition;
		this.nt = new NameAndTypeData(memberdefinition);
		constantpool.put(memberdefinition.getClassDeclaration());
		constantpool.put(this.nt);
	}

	void write(final Environment environment, final DataOutputStream dataoutputstream, final ConstantPool constantpool) throws IOException {
		if (this.field.isMethod()) {
			if (this.field.getClassDefinition().isInterface()) {
				dataoutputstream.writeByte(11);
			} else {
				dataoutputstream.writeByte(10);
			}
		} else {
			dataoutputstream.writeByte(9);
		}
		dataoutputstream.writeShort(constantpool.index(this.field.getClassDeclaration()));
		dataoutputstream.writeShort(constantpool.index(this.nt));
	}

	int order() {
		return 2;
	}

	final MemberDefinition field;
	private final NameAndTypeData nt;
}
