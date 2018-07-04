package sun.tools.asm;

import java.io.DataOutputStream;
import java.io.IOException;

import sun.tools.java.Environment;

final class NumberConstantData extends ConstantPoolData {

	NumberConstantData(final ConstantPool constantpool, final Number number) {
		this.num = number;
	}

	void write(final Environment environment, final DataOutputStream dataoutputstream, final ConstantPool constantpool) throws IOException {
		if (this.num instanceof Integer) {
			dataoutputstream.writeByte(3);
			dataoutputstream.writeInt(this.num.intValue());
		} else if (this.num instanceof Long) {
			dataoutputstream.writeByte(5);
			dataoutputstream.writeLong(this.num.longValue());
		} else if (this.num instanceof Float) {
			dataoutputstream.writeByte(4);
			dataoutputstream.writeFloat(this.num.floatValue());
		} else if (this.num instanceof Double) {
			dataoutputstream.writeByte(6);
			dataoutputstream.writeDouble(this.num.doubleValue());
		}
	}

	int order() {
		return this.width() != 1 ? 3 : 0;
	}

	int width() {
		return !(this.num instanceof Double) && !(this.num instanceof Long) ? 1 : 2;
	}

	final Number num;
}
