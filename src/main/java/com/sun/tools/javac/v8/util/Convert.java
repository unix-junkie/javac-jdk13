package com.sun.tools.javac.v8.util;

public final class Convert {

	private Convert() {
	}

	public static int string2int(final String s, final int i) {
		if (i == 10) {
			return Integer.parseInt(s, 10);
		}
		final char ac[] = s.toCharArray();
		final int j = 0x7fffffff / (i / 2);
		int k = 0;
		for (int l = 0; l < ac.length; l++) {
			final int i1 = Character.digit(ac[l], i);
			if (k < 0 || k > j || k * i > 0x7fffffff - i1) {
				throw new NumberFormatException();
			}
			k = k * i + i1;
		}

		return k;
	}

	public static long string2long(final String s, final int i) {
		if (i == 10) {
			return Long.parseLong(s, 10);
		}
		final char ac[] = s.toCharArray();
		final long l = 0x7fffffffffffffffL / (i / 2);
		long l1 = 0L;
		for (int j = 0; j < ac.length; j++) {
			final int k = Character.digit(ac[j], i);
			if (l1 < 0L || l1 > l || l1 * i > 0x7fffffffffffffffL - k) {
				throw new NumberFormatException();
			}
			l1 = l1 * i + k;
		}

		return l1;
	}

	private static int utf2chars(final byte abyte0[], final int i, final char ac[], final int j, final int k) {
		int l = i;
		int i1 = j;
		for (final int j1 = i + k; l < j1;) {
			int k1 = abyte0[l++] & 0xff;
			if (k1 >= 224) {
				k1 = (k1 & 0xf) << 12;
				k1 |= (abyte0[l++] & 0x3f) << 6;
				k1 |= abyte0[l++] & 0x3f;
			} else if (k1 >= 192) {
				k1 = (k1 & 0x1f) << 6;
				k1 |= abyte0[l++] & 0x3f;
			}
			ac[i1++] = (char) k1;
		}

		return i1;
	}

	private static char[] utf2chars(final byte abyte0[], final int i, final int j) {
		final char ac[] = new char[j];
		final int k = utf2chars(abyte0, i, ac, 0, j);
		final char ac1[] = new char[k];
		System.arraycopy(ac, 0, ac1, 0, k);
		return ac1;
	}

	public static char[] utf2chars(final byte abyte0[]) {
		return utf2chars(abyte0, 0, abyte0.length);
	}

	public static String utf2string(final byte abyte0[], final int i, final int j) {
		final char ac[] = new char[j];
		final int k = utf2chars(abyte0, i, ac, 0, j);
		return new String(ac, 0, k);
	}

	public static String utf2string(final byte abyte0[]) {
		return utf2string(abyte0, 0, abyte0.length);
	}

	public static int chars2utf(final char ac[], final int i, final byte abyte0[], final int j, final int k) {
		int l = j;
		final int i1 = i + k;
		for (int j1 = i; j1 < i1; j1++) {
			final char c = ac[j1];
			if (c >= '\001' && c <= '\177') {
				abyte0[l++] = (byte) c;
			} else if (c <= '\u03FF') {
				abyte0[l++] = (byte) (0xc0 | c >> 6);
				abyte0[l++] = (byte) (0x80 | c & 0x3f);
			} else {
				abyte0[l++] = (byte) (0xe0 | c >> 12);
				abyte0[l++] = (byte) (0x80 | c >> 6 & 0x3f);
				abyte0[l++] = (byte) (0x80 | c & 0x3f);
			}
		}

		return l;
	}

	private static byte[] chars2utf(final char ac[], final int i, final int j) {
		final byte abyte0[] = new byte[j * 3];
		final int k = chars2utf(ac, i, abyte0, 0, j);
		final byte abyte1[] = new byte[k];
		System.arraycopy(abyte0, 0, abyte1, 0, k);
		return abyte1;
	}

	private static byte[] chars2utf(final char ac[]) {
		return chars2utf(ac, 0, ac.length);
	}

	public static byte[] string2utf(final String s) {
		return chars2utf(s.toCharArray());
	}

	public static String quote(final String s) {
		final StringBuffer stringbuffer = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			final char c = s.charAt(i);
			switch (c) {
			case 10: // '\n'
				stringbuffer.append("\\n");
				break;

			case 9: // '\t'
				stringbuffer.append("\\t");
				break;

			case 8: // '\b'
				stringbuffer.append("\\b");
				break;

			case 12: // '\f'
				stringbuffer.append("\\f");
				break;

			case 13: // '\r'
				stringbuffer.append("\\r");
				break;

			case 34: // '"'
				stringbuffer.append("\\\"");
				break;

			case 39: // '\''
				stringbuffer.append("\\'");
				break;

			case 92: // '\\'
				stringbuffer.append("\\\\");
				break;

			default:
				if (c < ' ' || c >= '\200' && c < '\377') {
					stringbuffer.append('\\');
					stringbuffer.append((char) (48 + (c >> 6) % 8));
					stringbuffer.append((char) (48 + (c >> 3) % 8));
					stringbuffer.append((char) (48 + c % 8));
				} else {
					stringbuffer.append(c);
				}
				break;
			}
		}

		return stringbuffer.toString();
	}

	public static String escapeUnicode(String s) {
		final int i = s.length();
		for (int j = 0; j < i;) {
			final char c = s.charAt(j);
			if (c > '\377') {
				final StringBuffer stringbuffer = new StringBuffer();
				stringbuffer.append(s.substring(0, j));
				for (; j < i; j++) {
					final char c1 = s.charAt(j);
					if (c1 > '\377') {
						stringbuffer.append("\\u");
						Character.forDigit((c1 >> 12) % 16, 16);
						Character.forDigit((c1 >> 8) % 16, 16);
						Character.forDigit((c1 >> 4) % 16, 16);
						Character.forDigit(c1 % 16, 16);
					} else {
						stringbuffer.append(c1);
					}
				}

				s = stringbuffer.toString();
			} else {
				j++;
			}
		}

		return s;
	}

	public static Name shortName(final Name name) {
		return name.subName(name.lastIndexOf((byte) 46) + 1, name.len);
	}

	public static Name packagePart(final Name name) {
		return name.subName(0, name.lastIndexOf((byte) 46));
	}
}
