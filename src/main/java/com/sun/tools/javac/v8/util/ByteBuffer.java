package com.sun.tools.javac.v8.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

public final class ByteBuffer {

	public ByteBuffer() {
		this(64);
	}

	public ByteBuffer(final int i) {
		this.elems = new byte[i];
		this.length = 0;
	}

	private void copy(final int i) {
		final byte abyte0[] = new byte[i];
		System.arraycopy(this.elems, 0, abyte0, 0, this.elems.length);
		this.elems = abyte0;
	}

	public void appendByte(final int i) {
		if (this.length >= this.elems.length) {
			this.copy(this.elems.length * 2);
		}
		this.elems[this.length++] = (byte) i;
	}

	public void appendBytes(final byte abyte0[], final int i, final int j) {
		while (this.length + j > this.elems.length) {
			this.copy(this.elems.length * 2);
		}
		System.arraycopy(abyte0, i, this.elems, this.length, j);
		this.length += j;
	}

	public void appendBytes(final byte abyte0[]) {
		this.appendBytes(abyte0, 0, abyte0.length);
	}

	public void appendChar(final int i) {
		while (this.length + 1 >= this.elems.length) {
			this.copy(this.elems.length * 2);
		}
		this.elems[this.length] = (byte) (i >> 8 & 0xff);
		this.elems[this.length + 1] = (byte) (i & 0xff);
		this.length += 2;
	}

	public void appendInt(final int i) {
		while (this.length + 3 >= this.elems.length) {
			this.copy(this.elems.length * 2);
		}
		this.elems[this.length] = (byte) (i >> 24 & 0xff);
		this.elems[this.length + 1] = (byte) (i >> 16 & 0xff);
		this.elems[this.length + 2] = (byte) (i >> 8 & 0xff);
		this.elems[this.length + 3] = (byte) (i & 0xff);
		this.length += 4;
	}

	public void appendLong(final long l) {
		final ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream(8);
		final DataOutput dataoutputstream = new DataOutputStream(bytearrayoutputstream);
		try {
			dataoutputstream.writeLong(l);
			this.appendBytes(bytearrayoutputstream.toByteArray(), 0, 8);
		} catch (final IOException ignored) {
			throw new InternalError("write");
		}
	}

	public void appendFloat(final float f) {
		final ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream(4);
		final DataOutput dataoutputstream = new DataOutputStream(bytearrayoutputstream);
		try {
			dataoutputstream.writeFloat(f);
			this.appendBytes(bytearrayoutputstream.toByteArray(), 0, 4);
		} catch (final IOException ignored) {
			throw new InternalError("write");
		}
	}

	public void appendDouble(final double d) {
		final ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream(8);
		final DataOutput dataoutputstream = new DataOutputStream(bytearrayoutputstream);
		try {
			dataoutputstream.writeDouble(d);
			this.appendBytes(bytearrayoutputstream.toByteArray(), 0, 8);
		} catch (final IOException ignored) {
			throw new InternalError("write");
		}
	}

	public void reset() {
		this.length = 0;
	}

	public Name toName() {
		return Name.fromUtf(this.elems, 0, this.length);
	}

	public byte elems[];
	public int length;
}
