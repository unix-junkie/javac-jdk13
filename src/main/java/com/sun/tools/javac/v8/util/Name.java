package com.sun.tools.javac.v8.util;

public final class Name {

	private static int hashValue(final byte abyte0[], final int i, final int j) {
		return j > 0 ? j * 0x10d39 + abyte0[i] * 1681 + abyte0[i + j - 1] * 41 + abyte0[i + (j >> 1)] : 0;
	}

	private static boolean equals(final int i, final byte abyte0[], final int j, final int k) {
		int l;
		for (l = 0; l < k && names[i + l] == abyte0[j + l]; l++) {
		}
		return l == k;
	}

	public static Name fromUtf(final byte abyte0[], final int i, final int j) {
		final int k = hashValue(abyte0, i, j) & 0x7fff;
		Name name;
		for (name = hashtable[k]; name != null && (name.len != j || !equals(name.index, abyte0, i, j)); name = name.next) {
		}
		if (name == null) {
			for (byte[] abyte1; nc + j > names.length; names = abyte1) {
				abyte1 = new byte[names.length * 2];
				System.arraycopy(names, 0, abyte1, 0, names.length);
			}

			System.arraycopy(abyte0, i, names, nc, j);
			name = new Name();
			name.index = nc;
			name.len = j;
			name.next = hashtable[k];
			hashtable[k] = name;
			nc += j;
			if (j == 0) {
				nc++;
			}
		}
		return name;
	}

	public static Name fromUtf(final byte abyte0[]) {
		return fromUtf(abyte0, 0, abyte0.length);
	}

	public static Name fromChars(final char ac[], final int i, final int j) {
		for (byte[] abyte0; nc + j * 3 >= names.length; names = abyte0) {
			abyte0 = new byte[names.length * 2];
			System.arraycopy(names, 0, abyte0, 0, names.length);
		}

		final int k = Convert.chars2utf(ac, i, names, nc, j) - nc;
		final int l = hashValue(names, nc, k) & 0x7fff;
		Name name;
		for (name = hashtable[l]; name != null && (name.len != k || !equals(name.index, names, nc, k)); name = name.next) {
		}
		if (name == null) {
			name = new Name();
			name.index = nc;
			name.len = k;
			name.next = hashtable[l];
			hashtable[l] = name;
			nc += k;
			if (k == 0) {
				nc++;
			}
		}
		return name;
	}

	public static Name fromString(final String s) {
		final char ac[] = s.toCharArray();
		return fromChars(ac, 0, ac.length);
	}

	public byte[] toUtf() {
		final byte abyte0[] = new byte[this.len];
		System.arraycopy(names, this.index, abyte0, 0, this.len);
		return abyte0;
	}

	public String toString() {
		return Convert.utf2string(names, this.index, this.len);
	}

	public String toJava() {
		return this.toString();
	}

	private void getBytes(final byte abyte0[], final int i) {
		System.arraycopy(names, this.index, abyte0, i, this.len);
	}

	public int hashCode() {
		return this.index;
	}

	public boolean equals(final Object obj) {
		return obj instanceof Name && this.index == ((Name) obj).index;
	}

	public int length() {
		return this.len;
	}

	public byte byteAt(final int i) {
		return names[this.index + i];
	}

	public int indexOf(final byte byte0) {
		int i;
		for (i = 0; i < this.len && names[this.index + i] != byte0; i++) {
		}
		return i;
	}

	public int lastIndexOf(final byte byte0) {
		int i;
		for (i = this.len - 1; i >= 0 && names[this.index + i] != byte0; i--) {
		}
		return i;
	}

	public boolean startsWith(final Name name) {
		int i;
		for (i = 0; i < name.len && i < this.len && names[this.index + i] == names[name.index + i]; i++) {
		}
		return i == name.len;
	}

	public boolean endsWith(final Name name) {
		int i = this.len - 1;
		int j;
		for (j = name.len - 1; j >= 0 && i >= 0 && names[this.index + i] == names[name.index + j]; j--) {
			i--;
		}

		return j < 0;
	}

	public Name subName(final int i, int j) {
		if (j < i) {
			j = i;
		}
		return fromUtf(names, this.index + i, j - i);
	}

	public Name replace(final byte byte0, final byte byte1) {
		for (int i = 0; i < this.len; i++) {
			if (names[this.index + i] == byte0) {
				final byte abyte0[] = new byte[this.len];
				System.arraycopy(names, this.index, abyte0, 0, i);
				abyte0[i] = byte1;
				for (i++; i < this.len; i++) {
					final byte byte2 = names[this.index + i];
					abyte0[i] = byte2 != byte0 ? byte2 : byte1;
				}

				return fromUtf(abyte0, 0, this.len);
			}
		}

		return this;
	}

	public Name append(final Name name) {
		final byte abyte0[] = new byte[this.len + name.len];
		this.getBytes(abyte0, 0);
		name.getBytes(abyte0, this.len);
		return fromUtf(abyte0, 0, abyte0.length);
	}

	public static Name concat(final Name aname[]) {
		int i = 0;
		for (int j = 0; j < aname.length; j++) {
			i += aname[j].len;
		}

		final byte abyte0[] = new byte[i];
		i = 0;
		for (int k = 0; k < aname.length; k++) {
			aname[k].getBytes(abyte0, i);
			i += aname[k].len;
		}

		return fromUtf(abyte0, 0, i);
	}

	public int index;
	public int len;
	private Name next;
	private static final Name[] hashtable = new Name[32768];
	public static byte names[] = new byte[0x20000];
	private static int nc;

}
