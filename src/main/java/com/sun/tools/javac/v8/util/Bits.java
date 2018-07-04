package com.sun.tools.javac.v8.util;

public final class Bits {

	public Bits() {
		this(new int[1]);
	}

	private Bits(final int ai[]) {
		this.bits = ai;
	}

	public Bits(final int i, final int j) {
		this();
		this.inclRange(i, j);
	}

	private void sizeTo(final int i) {
		if (this.bits.length < i) {
			final int ai[] = new int[i];
			System.arraycopy(this.bits, 0, ai, 0, this.bits.length);
			this.bits = ai;
		}
	}

	public void clear() {
		for (int i = 0; i < this.bits.length; i++) {
			this.bits[i] = 0;
		}

	}

	public Bits dup() {
		final int ai[] = new int[this.bits.length];
		System.arraycopy(this.bits, 0, ai, 0, this.bits.length);
		return new Bits(ai);
	}

	public void incl(final int i) {
		Util.assertTrue(i >= 0);
		this.sizeTo((i >>> 5) + 1);
		this.bits[i >>> 5] |= 1 << (i & 0x1f);
	}

	public void inclRange(final int i, final int j) {
		this.sizeTo((j >>> 5) + 1);
		for (int k = i; k < j; k++) {
			this.bits[k >>> 5] |= 1 << (k & 0x1f);
		}

	}

	public void excl(final int i) {
		Util.assertTrue(i >= 0);
		this.sizeTo((i >>> 5) + 1);
		this.bits[i >>> 5] &= ~(1 << (i & 0x1f));
	}

	public boolean isMember(final int i) {
		return i >= 0 && i < this.bits.length << 5 && (this.bits[i >>> 5] & 1 << (i & 0x1f)) != 0;
	}

	public Bits andSet(final Bits bits1) {
		this.sizeTo(bits1.bits.length);
		for (int i = 0; i < bits1.bits.length; i++) {
			this.bits[i] &= bits1.bits[i];
		}

		return this;
	}

	public Bits orSet(final Bits bits1) {
		this.sizeTo(bits1.bits.length);
		for (int i = 0; i < bits1.bits.length; i++) {
			this.bits[i] |= bits1.bits[i];
		}

		return this;
	}

	public Bits diffSet(final Bits bits1) {
		for (int i = 0; i < this.bits.length; i++) {
			if (i < bits1.bits.length) {
				this.bits[i] &= ~bits1.bits[i];
			}
		}

		return this;
	}

	public String toString() {
		final char ac[] = new char[this.bits.length * 32];
		for (int i = 0; i < this.bits.length * 32; i++) {
			ac[i] = this.isMember(i) ? '1' : '0';
		}

		return new String(ac);
	}

	private int bits[];
}
