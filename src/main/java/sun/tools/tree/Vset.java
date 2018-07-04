package sun.tools.tree;

public final class Vset {

	public Vset() {
		this.x = emptyX;
	}

	private Vset(final long l, final long l1, final long al[]) {
		this.vset = l;
		this.uset = l1;
		this.x = al;
	}

	public Vset copy() {
		if (this == DEAD_END) {
			return this;
		}
		final Vset vset1 = new Vset(this.vset, this.uset, this.x);
		if (this.x.length > 0) {
			vset1.growX(this.x.length);
		}
		return vset1;
	}

	private void growX(final int i) {
		final long al[] = new long[i];
		final long al1[] = this.x;
		System.arraycopy(al1, 0, al, 0, al1.length);

		this.x = al;
	}

	public boolean isDeadEnd() {
		return this == DEAD_END;
	}

	public boolean isReallyDeadEnd() {
		return this.x == fullX;
	}

	public Vset clearDeadEnd() {
		return this == DEAD_END ? new Vset(-1L, -1L, fullX) : this;
	}

	public boolean testVar(final int i) {
		final long l = 1L << i;
		if (i >= 64) {
			final int j = (i / 64 - 1) * 2;
			return j >= this.x.length ? this.x == fullX : (this.x[j] & l) != 0L;
		}
		return (this.vset & l) != 0L;
	}

	public boolean testVarUnassigned(final int i) {
		final long l = 1L << i;
		if (i >= 64) {
			final int j = (i / 64 - 1) * 2 + 1;
			return j >= this.x.length ? this.x == fullX : (this.x[j] & l) != 0L;
		}
		return (this.uset & l) != 0L;
	}

	public Vset addVar(final int i) {
		if (this.x == fullX) {
			return this;
		}
		final long l = 1L << i;
		if (i >= 64) {
			final int j = (i / 64 - 1) * 2;
			if (j >= this.x.length) {
				this.growX(j + 1);
			}
			this.x[j] |= l;
			if (j + 1 < this.x.length) {
				this.x[j + 1] &= ~l;
			}
		} else {
			this.vset |= l;
			this.uset &= ~l;
		}
		return this;
	}

	public Vset addVarUnassigned(final int i) {
		if (this.x == fullX) {
			return this;
		}
		final long l = 1L << i;
		if (i >= 64) {
			final int j = (i / 64 - 1) * 2 + 1;
			if (j >= this.x.length) {
				this.growX(j + 1);
			}
			this.x[j] |= l;
			this.x[j - 1] &= ~l;
		} else {
			this.uset |= l;
			this.vset &= ~l;
		}
		return this;
	}

	public Vset clearVar(final int i) {
		if (this.x == fullX) {
			return this;
		}
		final long l = 1L << i;
		if (i >= 64) {
			final int j = (i / 64 - 1) * 2;
			if (j >= this.x.length) {
				return this;
			}
			this.x[j] &= ~l;
			if (j + 1 < this.x.length) {
				this.x[j + 1] &= ~l;
			}
		} else {
			this.vset &= ~l;
			this.uset &= ~l;
		}
		return this;
	}

	public Vset join(final Vset vset1) {
		if (this == DEAD_END) {
			return vset1.copy();
		}
		if (vset1 == DEAD_END) {
			return this;
		}
		if (this.x == fullX) {
			return vset1.copy();
		}
		if (vset1.x == fullX) {
			return this;
		}
		this.vset &= vset1.vset;
		this.uset &= vset1.uset;
		if (vset1.x == emptyX) {
			this.x = emptyX;
		} else {
			final long al[] = vset1.x;
			final int i = this.x.length;
			final int j = al.length >= i ? i : al.length;
			for (int k = 0; k < j; k++) {
				this.x[k] &= al[k];
			}

			for (int l = j; l < i; l++) {
				this.x[l] = 0L;
			}

		}
		return this;
	}

	public Vset addDAandJoinDU(final Vset vset1) {
		if (this == DEAD_END) {
			return this;
		}
		if (vset1 == DEAD_END) {
			return vset1;
		}
		if (this.x == fullX) {
			return this;
		}
		if (vset1.x == fullX) {
			return vset1.copy();
		}
		this.vset |= vset1.vset;
		this.uset &= vset1.uset & ~vset1.vset;
		final int i = this.x.length;
		final long al[] = vset1.x;
		final int j = al.length;
		if (al != emptyX) {
			if (j > i) {
				this.growX(j);
			}
			for (int k = 0; k < j; k++) {
				this.x[k] |= al[k];
				if (++k == j) {
					break;
				}
				this.x[k] &= al[k] & ~al[k - 1];
			}

		}
		for (int l = j | 1; l < i; l += 2) {
			this.x[l] = 0L;
		}

		return this;
	}

	public static Vset firstDAandSecondDU(final Vset vset1, final Vset vset2) {
		if (vset1.x == fullX) {
			return vset1.copy();
		}
		final long al[] = vset1.x;
		final int i = al.length;
		final long al1[] = vset2.x;
		final int j = al1.length;
		final int k = i <= j ? j : i;
		long al2[] = emptyX;
		if (k > 0) {
			al2 = new long[k];
			for (int l = 0; l < i; l += 2) {
				al2[l] = al[l];
			}

			for (int i1 = 1; i1 < j; i1 += 2) {
				al2[i1] = al1[i1];
			}

		}
		return new Vset(vset1.vset, vset2.uset, al2);
	}

	public Vset removeAdditionalVars(final int i) {
		if (this.x == fullX) {
			return this;
		}
		final long l = 1L << i;
		if (i >= 64) {
			int j = (i / 64 - 1) * 2;
			if (j < this.x.length) {
				this.x[j] &= l - 1L;
				if (++j < this.x.length) {
					this.x[j] &= l - 1L;
				}
				while (++j < this.x.length) {
					this.x[j] = 0L;
				}
			}
		} else {
			if (this.x.length > 0) {
				this.x = emptyX;
			}
			this.vset &= l - 1L;
			this.uset &= l - 1L;
		}
		return this;
	}

	public int varLimit() {
		long l;
		int i;
		label0: {
			for (int j = this.x.length / 2 * 2; j >= 0; j -= 2) {
				if (j == this.x.length) {
					continue;
				}
				l = this.x[j];
				if (j + 1 < this.x.length) {
					l |= this.x[j + 1];
				}
				if (l == 0L) {
					continue;
				}
				i = (j / 2 + 1) * 64;
				break label0;
			}

			l = this.vset;
			l |= this.uset;
			if (l != 0L) {
				i = 0;
			} else {
				return 0;
			}
		}
		for (; l != 0L; l >>>= 1) {
			i++;
		}

		return i;
	}

	public String toString() {
		if (this == DEAD_END) {
			return "{DEAD_END}";
		}
		final StringBuffer stringbuffer = new StringBuffer("{");
		final int i = 64 * (1 + (this.x.length + 1) / 2);
		for (int j = 0; j < i; j++) {
			if (!this.testVarUnassigned(j)) {
				if (stringbuffer.length() > 1) {
					stringbuffer.append(' ');
				}
				stringbuffer.append(j);
				if (!this.testVar(j)) {
					stringbuffer.append('?');
				}
			}
		}

		if (this.x == fullX) {
			stringbuffer.append("...DEAD_END");
		}
		stringbuffer.append('}');
		return stringbuffer.toString();
	}

	private long vset;
	private long uset;
	private long[] x;
	private static final long[] emptyX = new long[0];
	private static final long[] fullX;
	static final int VBITS = 64;
	static final Vset DEAD_END;

	static {
		fullX = new long[0];
		DEAD_END = new Vset(-1L, -1L, fullX);
	}
}
