package sun.tools.asm;

import java.io.DataOutputStream;
import java.io.IOException;

import sun.tools.java.Environment;

abstract class ConstantPoolData {

	abstract void write(Environment environment, DataOutputStream dataoutputstream, ConstantPool constantpool) throws IOException;

	int order() {
		return 0;
	}

	int width() {
		return 1;
	}

	int index;
}
