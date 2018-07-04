package sun.tools.java;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class BinaryAttribute {

	BinaryAttribute(final Identifier identifier, final byte abyte0[], final BinaryAttribute binaryattribute) {
		this.name = identifier;
		this.data = abyte0;
		this.next = binaryattribute;
	}

	static BinaryAttribute load(final DataInput datainputstream, final BinaryConstantPool binaryconstantpool, final int i) throws IOException {
		BinaryAttribute binaryattribute = null;
		final int j = datainputstream.readUnsignedShort();
		for (int k = 0; k < j; k++) {
			final Identifier identifier = binaryconstantpool.getIdentifier(datainputstream.readUnsignedShort());
			final int l = datainputstream.readInt();
			if (identifier.equals(Constants.idCode) && (i & 2) == 0) {
				datainputstream.skipBytes(l);
			} else {
				final byte abyte0[] = new byte[l];
				datainputstream.readFully(abyte0);
				binaryattribute = new BinaryAttribute(identifier, abyte0, binaryattribute);
			}
		}

		return binaryattribute;
	}

	static void write(final BinaryAttribute binaryattribute, final DataOutput dataoutputstream, final BinaryConstantPool binaryconstantpool, final Environment environment) throws IOException {
		int i = 0;
		for (BinaryAttribute binaryattribute1 = binaryattribute; binaryattribute1 != null; binaryattribute1 = binaryattribute1.next) {
			i++;
		}

		dataoutputstream.writeShort(i);
		for (BinaryAttribute binaryattribute2 = binaryattribute; binaryattribute2 != null; binaryattribute2 = binaryattribute2.next) {
			final byte abyte0[] = binaryattribute2.data;
			dataoutputstream.writeShort(binaryconstantpool.indexString(binaryattribute2.name.toString(), environment));
			dataoutputstream.writeInt(abyte0.length);
			dataoutputstream.write(abyte0, 0, abyte0.length);
		}

	}

	public Identifier getName() {
		return this.name;
	}

	final Identifier name;
	final byte[] data;
	BinaryAttribute next;
}
