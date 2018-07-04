package sun.tools.java;

import java.io.IOException;
import java.io.InputStream;

public class Scanner {

	private void growBuffer() {
		final char ac[] = new char[this.buffer.length * 2];
		System.arraycopy(this.buffer, 0, ac, 0, this.buffer.length);
		this.buffer = ac;
	}

	private void putc(final int i) {
		if (this.count == this.buffer.length) {
			this.growBuffer();
		}
		this.buffer[this.count++] = (char) i;
	}

	private String bufferString() {
		return new String(this.buffer, 0, this.count);
	}

	Scanner(final Environment environment, final InputStream inputstream) throws IOException {
		this.scanComments = false;
		this.buffer = new char[1024];
		this.env = environment;
		this.useInputStream(inputstream);
	}

	private void useInputStream(final InputStream inputstream) throws IOException {
		try {
			this.in = new ScannerInputReader(this.env, inputstream);
		} catch (final Exception ignored) {
			this.env.setCharacterEncoding(null);
			this.in = new ScannerInputReader(this.env, inputstream);
		}
		this.ch = this.in.read();
		this.prevPos = this.in.pos;
		this.scan();
	}

	Scanner(final Environment environment) {
		this.scanComments = false;
		this.buffer = new char[1024];
		this.env = environment;
	}

	private static void defineKeyword(final int i) {
		Identifier.lookup(Constants.opNames[i]).setType(i);
	}

	private void skipComment() throws IOException {
		do {
			switch (this.ch) {
			case -1:
				this.env.error(this.pos, "eof.in.comment");
				return;

			case 42: // '*'
				if ((this.ch = this.in.read()) == 47) {
					this.ch = this.in.read();
					return;
				}
				break;

			default:
				this.ch = this.in.read();
				break;
			}
		} while (true);
	}

	private String scanDocComment() throws IOException {
		final ScannerInputReader scannerinputreader = this.in;
		char ac[] = this.buffer;
		int i;
		while ((i = scannerinputreader.read()) == 42) {
		}
		if (i == 47) {
			this.ch = scannerinputreader.read();
			return "";
		}
		if (i == 10) {
			i = scannerinputreader.read();
		}
		int j = 0;
		label0: do {
			label1: {
				switch (i) {
				case 9: // '\t'
				case 32: // ' '
					i = scannerinputreader.read();
					continue;
				}
				if (i == 42) {
					do {
						i = scannerinputreader.read();
					} while (i == 42);
					if (i == 47) {
						this.ch = scannerinputreader.read();
						break label1;
					}
				}
				label2: do {
					switch (i) {
					case -1:
						this.env.error(this.pos, "eof.in.comment");
						this.ch = -1;
						break label2;

					case 42: // '*'
						i = scannerinputreader.read();
						if (i == 47) {
							this.ch = scannerinputreader.read();
							break label2;
						}
						if (j == ac.length) {
							this.growBuffer();
							ac = this.buffer;
						}
						ac[j++] = '*';
						continue;

					case 10: // '\n'
						if (j == ac.length) {
							this.growBuffer();
							ac = this.buffer;
						}
						ac[j++] = '\n';
						i = scannerinputreader.read();
						continue label0;

					case 0: // '\0'
					case 1: // '\001'
					case 2: // '\002'
					case 3: // '\003'
					case 4: // '\004'
					case 5: // '\005'
					case 6: // '\006'
					case 7: // '\007'
					case 8: // '\b'
					case 9: // '\t'
					case 11: // '\013'
					case 12: // '\f'
					case 13: // '\r'
					case 14: // '\016'
					case 15: // '\017'
					case 16: // '\020'
					case 17: // '\021'
					case 18: // '\022'
					case 19: // '\023'
					case 20: // '\024'
					case 21: // '\025'
					case 22: // '\026'
					case 23: // '\027'
					case 24: // '\030'
					case 25: // '\031'
					case 26: // '\032'
					case 27: // '\033'
					case 28: // '\034'
					case 29: // '\035'
					case 30: // '\036'
					case 31: // '\037'
					case 32: // ' '
					case 33: // '!'
					case 34: // '"'
					case 35: // '#'
					case 36: // '$'
					case 37: // '%'
					case 38: // '&'
					case 39: // '\''
					case 40: // '('
					case 41: // ')'
					default:
						if (j == ac.length) {
							this.growBuffer();
							ac = this.buffer;
						}
						ac[j++] = (char) i;
						i = scannerinputreader.read();
						break;
					}
				} while (true);
			}
			if (j > 0) {
				int k;
				label3: for (k = j - 1; k > -1;) {
					switch (ac[k]) {
					case 0: // '\0'
					case 1: // '\001'
					case 2: // '\002'
					case 3: // '\003'
					case 4: // '\004'
					case 5: // '\005'
					case 6: // '\006'
					case 7: // '\007'
					case 8: // '\b'
					case 10: // '\n'
					case 11: // '\013'
					case 12: // '\f'
					case 13: // '\r'
					case 14: // '\016'
					case 15: // '\017'
					case 16: // '\020'
					case 17: // '\021'
					case 18: // '\022'
					case 19: // '\023'
					case 20: // '\024'
					case 21: // '\025'
					case 22: // '\026'
					case 23: // '\027'
					case 24: // '\030'
					case 25: // '\031'
					case 26: // '\032'
					case 27: // '\033'
					case 28: // '\034'
					case 29: // '\035'
					case 30: // '\036'
					case 31: // '\037'
					case 33: // '!'
					case 34: // '"'
					case 35: // '#'
					case 36: // '$'
					case 37: // '%'
					case 38: // '&'
					case 39: // '\''
					case 40: // '('
					case 41: // ')'
					default:
						break label3;

					case 9: // '\t'
					case 32: // ' '
					case 42: // '*'
						k--;
						break;
					}
				}

				j = k + 1;
				return new String(ac, 0, j);
			}
			return "";
		} while (true);
	}

	private void scanNumber() throws IOException {
		this.radix = this.ch != 48 ? 10 : 8;
		long l = this.ch - 48;
		this.count = 0;
		this.putc(this.ch);
		boolean flag2 = false;
		boolean flag1 = false;
		boolean flag = false;
		label0: do {
			switch (this.ch = this.in.read()) {
			case 46: // '.'
				if (this.radix != 16) {
					this.scanReal();
					return;
				}
				break label0;

			case 56: // '8'
			case 57: // '9'
				flag = true;
				// fall through

			case 48: // '0'
			case 49: // '1'
			case 50: // '2'
			case 51: // '3'
			case 52: // '4'
			case 53: // '5'
			case 54: // '6'
			case 55: // '7'
				flag2 = true;
				this.putc(this.ch);
				switch (this.radix) {
				case 10:
					flag1 |= l * 10L / 10L != l;
					l = l * 10L + this.ch - 48;
					flag1 |= l - 1L < -1L;
					break;
				case 8:
					flag1 |= l >>> 61 != 0L;
					l = (l << 3) + this.ch - 48;
					break;
				default:
					flag1 |= l >>> 60 != 0L;
					l = (l << 4) + this.ch - 48;
					break;
				}
				break;

			case 68: // 'D'
			case 69: // 'E'
			case 70: // 'F'
			case 100: // 'd'
			case 101: // 'e'
			case 102: // 'f'
				if (this.radix != 16) {
					this.scanReal();
					return;
				}
				// fall through

			case 65: // 'A'
			case 66: // 'B'
			case 67: // 'C'
			case 97: // 'a'
			case 98: // 'b'
			case 99: // 'c'
				flag2 = true;
				this.putc(this.ch);
				if (this.radix == 16) {
					flag1 |= l >>> 60 != 0L;
					l = (l << 4) + 10L + Character.toLowerCase((char) this.ch) - 97L;
					break;
				}
				break label0;

			case 76: // 'L'
			case 108: // 'l'
				this.ch = this.in.read();
				this.longValue = l;
				this.token = 66;
				break label0;

			case 88: // 'X'
			case 120: // 'x'
				if (this.count == 1 && this.radix == 8) {
					this.radix = 16;
					flag2 = false;
					break;
				}
				break label0;

			case 47: // '/'
			case 58: // ':'
			case 59: // ';'
			case 60: // '<'
			case 61: // '='
			case 62: // '>'
			case 63: // '?'
			case 64: // '@'
			case 71: // 'G'
			case 72: // 'H'
			case 73: // 'I'
			case 74: // 'J'
			case 75: // 'K'
			case 77: // 'M'
			case 78: // 'N'
			case 79: // 'O'
			case 80: // 'P'
			case 81: // 'Q'
			case 82: // 'R'
			case 83: // 'S'
			case 84: // 'T'
			case 85: // 'U'
			case 86: // 'V'
			case 87: // 'W'
			case 89: // 'Y'
			case 90: // 'Z'
			case 91: // '['
			case 92: // '\\'
			case 93: // ']'
			case 94: // '^'
			case 95: // '_'
			case 96: // '`'
			case 103: // 'g'
			case 104: // 'h'
			case 105: // 'i'
			case 106: // 'j'
			case 107: // 'k'
			case 109: // 'm'
			case 110: // 'n'
			case 111: // 'o'
			case 112: // 'p'
			case 113: // 'q'
			case 114: // 'r'
			case 115: // 's'
			case 116: // 't'
			case 117: // 'u'
			case 118: // 'v'
			case 119: // 'w'
			default:
				this.intValue = (int) l;
				this.token = 65;
				break label0;
			}
		} while (true);
		if (Character.isJavaIdentifierPart((char) this.ch) || this.ch == 46) {
			this.env.error(this.in.pos, "invalid.number");
			do {
				this.ch = this.in.read();
			} while (Character.isJavaIdentifierPart((char) this.ch) || this.ch == 46);
			this.intValue = 0;
			this.token = 65;
		} else if (this.radix == 8 && flag) {
			this.intValue = 0;
			this.token = 65;
			this.env.error(this.pos, "invalid.octal.number");
		} else if (this.radix == 16 && !flag2) {
			this.intValue = 0;
			this.token = 65;
			this.env.error(this.pos, "invalid.hex.number");
		} else if (this.token == 65) {
			flag1 |= (l & 0xffffffff00000000L) != 0L || this.radix == 10 && l > 0x80000000L;
			if (flag1) {
				this.intValue = 0;
				switch (this.radix) {
				case 8: // '\b'
					this.env.error(this.pos, "overflow.int.oct");
					break;

				case 10: // '\n'
					this.env.error(this.pos, "overflow.int.dec");
					break;

				case 16: // '\020'
					this.env.error(this.pos, "overflow.int.hex");
					break;

				default:
					throw new CompilerError("invalid radix");
				}
			}
		} else if (flag1) {
			this.longValue = 0L;
			switch (this.radix) {
			case 8: // '\b'
				this.env.error(this.pos, "overflow.long.oct");
				break;

			case 10: // '\n'
				this.env.error(this.pos, "overflow.long.dec");
				break;

			case 16: // '\020'
				this.env.error(this.pos, "overflow.long.hex");
				break;

			default:
				throw new CompilerError("invalid radix");
			}
		}
	}

	private void scanReal() throws IOException {
		if (this.ch == 46) {
			this.putc(this.ch);
			this.ch = this.in.read();
		}
		boolean flag1 = false;
		boolean flag = false;
		label0: do {
			switch (this.ch) {
			case 44: // ','
			case 46: // '.'
			case 47: // '/'
			case 58: // ':'
			case 59: // ';'
			case 60: // '<'
			case 61: // '='
			case 62: // '>'
			case 63: // '?'
			case 64: // '@'
			case 65: // 'A'
			case 66: // 'B'
			case 67: // 'C'
			case 71: // 'G'
			case 72: // 'H'
			case 73: // 'I'
			case 74: // 'J'
			case 75: // 'K'
			case 76: // 'L'
			case 77: // 'M'
			case 78: // 'N'
			case 79: // 'O'
			case 80: // 'P'
			case 81: // 'Q'
			case 82: // 'R'
			case 83: // 'S'
			case 84: // 'T'
			case 85: // 'U'
			case 86: // 'V'
			case 87: // 'W'
			case 88: // 'X'
			case 89: // 'Y'
			case 90: // 'Z'
			case 91: // '['
			case 92: // '\\'
			case 93: // ']'
			case 94: // '^'
			case 95: // '_'
			case 96: // '`'
			case 97: // 'a'
			case 98: // 'b'
			case 99: // 'c'
			default:
				break label0;

			case 48: // '0'
			case 49: // '1'
			case 50: // '2'
			case 51: // '3'
			case 52: // '4'
			case 53: // '5'
			case 54: // '6'
			case 55: // '7'
			case 56: // '8'
			case 57: // '9'
				this.putc(this.ch);
				break;

			case 69: // 'E'
			case 101: // 'e'
				if (!flag) {
					this.putc(this.ch);
					flag = true;
					break;
				}
				break label0;

			case 43: // '+'
			case 45: // '-'
				final char c = this.buffer[this.count - 1];
				if (c == 'e' || c == 'E') {
					this.putc(this.ch);
					break;
				}
				break label0;

			case 70: // 'F'
			case 102: // 'f'
				this.ch = this.in.read();
				flag1 = true;
				break label0;

			case 68: // 'D'
			case 100: // 'd'
				this.ch = this.in.read();
				break label0;
			}
			this.ch = this.in.read();
		} while (true);
		if (Character.isJavaIdentifierPart((char) this.ch) || this.ch == 46) {
			this.env.error(this.in.pos, "invalid.number");
			do {
				this.ch = this.in.read();
			} while (Character.isJavaIdentifierPart((char) this.ch) || this.ch == 46);
			this.doubleValue = 0.0D;
			this.token = 68;
		} else {
			this.token = flag1 ? 67 : 68;
			try {
				final char c1 = this.buffer[this.count - 1];
				if (c1 == 'e' || c1 == 'E' || c1 == '+' || c1 == '-') {
					this.env.error(this.in.pos - 1L, "float.format");
				} else if (flag1) {
					final String s = this.bufferString();
					this.floatValue = Float.valueOf(s).floatValue();
					if (Float.isInfinite(this.floatValue)) {
						this.env.error(this.pos, "overflow.float");
					} else if (this.floatValue == 0.0F && !looksLikeZero(s)) {
						this.env.error(this.pos, "underflow.float");
					}
				} else {
					final String s1 = this.bufferString();
					this.doubleValue = Double.valueOf(s1).doubleValue();
					if (Double.isInfinite(this.doubleValue)) {
						this.env.error(this.pos, "overflow.double");
					} else if (this.doubleValue == 0.0D && !looksLikeZero(s1)) {
						this.env.error(this.pos, "underflow.double");
					}
				}
			} catch (final NumberFormatException ignored) {
				this.env.error(this.pos, "float.format");
				this.doubleValue = 0.0D;
				this.floatValue = 0.0F;
			}
		}
	}

	private static boolean looksLikeZero(final String s) {
		final int i = s.length();
		for (int j = 0; j < i;) {
			switch (s.charAt(j)) {
			case 49: // '1'
			case 50: // '2'
			case 51: // '3'
			case 52: // '4'
			case 53: // '5'
			case 54: // '6'
			case 55: // '7'
			case 56: // '8'
			case 57: // '9'
				return false;

			case 69: // 'E'
			case 70: // 'F'
			case 101: // 'e'
			case 102: // 'f'
				return true;

			case 0: // '\0'
			case 46: // '.'
			default:
				j++;
				break;
			}
		}

		return true;
	}

	private int scanEscapeChar() throws IOException {
		final long l = this.in.pos;
		switch (this.ch = this.in.read()) {
		case 48: // '0'
		case 49: // '1'
		case 50: // '2'
		case 51: // '3'
		case 52: // '4'
		case 53: // '5'
		case 54: // '6'
		case 55: // '7'
			int i = this.ch - 48;
			for (int j = 2; j > 0; j--) {
				switch (this.ch = this.in.read()) {
				case 48: // '0'
				case 49: // '1'
				case 50: // '2'
				case 51: // '3'
				case 52: // '4'
				case 53: // '5'
				case 54: // '6'
				case 55: // '7'
					i = (i << 3) + this.ch - 48;
					break;

				default:
					if (i > 255) {
						this.env.error(l, "invalid.escape.char");
					}
					return i;
				}
			}

			this.ch = this.in.read();
			if (i > 255) {
				this.env.error(l, "invalid.escape.char");
			}
			return i;

		case 114: // 'r'
			this.ch = this.in.read();
			return 13;

		case 110: // 'n'
			this.ch = this.in.read();
			return 10;

		case 102: // 'f'
			this.ch = this.in.read();
			return 12;

		case 98: // 'b'
			this.ch = this.in.read();
			return 8;

		case 116: // 't'
			this.ch = this.in.read();
			return 9;

		case 92: // '\\'
			this.ch = this.in.read();
			return 92;

		case 34: // '"'
			this.ch = this.in.read();
			return 34;

		case 39: // '\''
			this.ch = this.in.read();
			return 39;
		}
		this.env.error(l, "invalid.escape.char");
		this.ch = this.in.read();
		return -1;
	}

	private void scanString() throws IOException {
		this.token = 69;
		this.count = 0;
		this.ch = this.in.read();
		do {
			switch (this.ch) {
			case -1:
				this.env.error(this.pos, "eof.in.string");
				this.stringValue = this.bufferString();
				return;

			case 10: // '\n'
			case 13: // '\r'
				this.ch = this.in.read();
				this.env.error(this.pos, "newline.in.string");
				this.stringValue = this.bufferString();
				return;

			case 34: // '"'
				this.ch = this.in.read();
				this.stringValue = this.bufferString();
				return;

			case 92: // '\\'
				final int i = this.scanEscapeChar();
				if (i >= 0) {
					this.putc((char) i);
				}
				break;

			default:
				this.putc(this.ch);
				this.ch = this.in.read();
				break;
			}
		} while (true);
	}

	private void scanCharacter() throws IOException {
		this.token = 63;
		switch (this.ch = this.in.read()) {
		case 92: // '\\'
			final int i = this.scanEscapeChar();
			this.charValue = i < 0 ? '\0' : (char) i;
			break;

		case 39: // '\''
			this.charValue = '\0';
			this.env.error(this.pos, "invalid.char.constant");
			for (this.ch = this.in.read(); this.ch == 39; this.ch = this.in.read()) {
			}
			return;

		case 10: // '\n'
		case 13: // '\r'
			this.charValue = '\0';
			this.env.error(this.pos, "invalid.char.constant");
			return;

		default:
			this.charValue = (char) this.ch;
			this.ch = this.in.read();
			break;
		}
		if (this.ch == 39) {
			this.ch = this.in.read();
		} else {
			this.env.error(this.pos, "invalid.char.constant");
			do {
				switch (this.ch) {
				case 39: // '\''
					this.ch = this.in.read();
					return;

				case -1:
				case 10: // '\n'
				case 59: // ';'
					return;
				}
				this.ch = this.in.read();
			} while (true);
		}
	}

	private void scanIdentifier() throws IOException {
		this.count = 0;
		do {
			label0: do {
				this.putc(this.ch);
				switch (this.ch = this.in.read()) {
				case 37: // '%'
				case 38: // '&'
				case 39: // '\''
				case 40: // '('
				case 41: // ')'
				case 42: // '*'
				case 43: // '+'
				case 44: // ','
				case 45: // '-'
				case 46: // '.'
				case 47: // '/'
				case 58: // ':'
				case 59: // ';'
				case 60: // '<'
				case 61: // '='
				case 62: // '>'
				case 63: // '?'
				case 64: // '@'
				case 91: // '['
				case 92: // '\\'
				case 93: // ']'
				case 94: // '^'
				case 96: // '`'
				default:
					break label0;

				case 36: // '$'
				case 48: // '0'
				case 49: // '1'
				case 50: // '2'
				case 51: // '3'
				case 52: // '4'
				case 53: // '5'
				case 54: // '6'
				case 55: // '7'
				case 56: // '8'
				case 57: // '9'
				case 65: // 'A'
				case 66: // 'B'
				case 67: // 'C'
				case 68: // 'D'
				case 69: // 'E'
				case 70: // 'F'
				case 71: // 'G'
				case 72: // 'H'
				case 73: // 'I'
				case 74: // 'J'
				case 75: // 'K'
				case 76: // 'L'
				case 77: // 'M'
				case 78: // 'N'
				case 79: // 'O'
				case 80: // 'P'
				case 81: // 'Q'
				case 82: // 'R'
				case 83: // 'S'
				case 84: // 'T'
				case 85: // 'U'
				case 86: // 'V'
				case 87: // 'W'
				case 88: // 'X'
				case 89: // 'Y'
				case 90: // 'Z'
				case 95: // '_'
				case 97: // 'a'
				case 98: // 'b'
				case 99: // 'c'
				case 100: // 'd'
				case 101: // 'e'
				case 102: // 'f'
				case 103: // 'g'
				case 104: // 'h'
				case 105: // 'i'
				case 106: // 'j'
				case 107: // 'k'
				case 108: // 'l'
				case 109: // 'm'
				case 110: // 'n'
				case 111: // 'o'
				case 112: // 'p'
				case 113: // 'q'
				case 114: // 'r'
				case 115: // 's'
				case 116: // 't'
				case 117: // 'u'
				case 118: // 'v'
				case 119: // 'w'
				case 120: // 'x'
				case 121: // 'y'
				case 122: // 'z'
					break;
				}
			} while (true);
		} while (Character.isJavaIdentifierPart((char) this.ch));
		this.idValue = Identifier.lookup(this.bufferString());
		this.token = this.idValue.getType();
	}

	public long getEndPos() {
		return this.in.pos;
	}

	IdentifierToken getIdToken() {
		return this.token == 60 ? new IdentifierToken(this.pos, this.idValue) : null;
	}

	long scan() throws IOException {
		return this.xscan();
	}

	private long xscan() throws IOException {
		final ScannerInputReader scannerinputreader = this.in;
		final long l = this.pos;
		this.prevPos = scannerinputreader.pos;
		this.docComment = null;
		do {
			this.pos = scannerinputreader.pos;
			switch (this.ch) {
			case -1:
				this.token = -1;
				return l;

			case 10: // '\n'
				if (this.scanComments) {
					this.ch = 32;
					this.token = 146;
					return l;
				}
				// fall through

			case 9: // '\t'
			case 12: // '\f'
			case 32: // ' '
				this.ch = scannerinputreader.read();
				break;

			case 47: // '/'
				switch (this.ch = scannerinputreader.read()) {
				case 47: // '/'
					while ((this.ch = scannerinputreader.read()) != -1 && this.ch != 10) {
					}
					if (this.scanComments) {
						this.token = 146;
						return l;
					}
					break;

				case 42: // '*'
					this.ch = scannerinputreader.read();
					if (this.ch == 42) {
						this.docComment = this.scanDocComment();
					} else {
						this.skipComment();
					}
					if (this.scanComments) {
						return l;
					}
					break;

				case 61: // '='
					this.ch = scannerinputreader.read();
					this.token = 3;
					return l;

				default:
					this.token = 31;
					return l;
				}
				break;

			case 34: // '"'
				this.scanString();
				return l;

			case 39: // '\''
				this.scanCharacter();
				return l;

			case 48: // '0'
			case 49: // '1'
			case 50: // '2'
			case 51: // '3'
			case 52: // '4'
			case 53: // '5'
			case 54: // '6'
			case 55: // '7'
			case 56: // '8'
			case 57: // '9'
				this.scanNumber();
				return l;

			case 46: // '.'
				switch (this.ch = scannerinputreader.read()) {
				case 48: // '0'
				case 49: // '1'
				case 50: // '2'
				case 51: // '3'
				case 52: // '4'
				case 53: // '5'
				case 54: // '6'
				case 55: // '7'
				case 56: // '8'
				case 57: // '9'
					this.count = 0;
					this.putc(46);
					this.scanReal();
					break;

				default:
					this.token = 46;
					break;
				}
				return l;

			case 123: // '{'
				this.ch = scannerinputreader.read();
				this.token = 138;
				return l;

			case 125: // '}'
				this.ch = scannerinputreader.read();
				this.token = 139;
				return l;

			case 40: // '('
				this.ch = scannerinputreader.read();
				this.token = 140;
				return l;

			case 41: // ')'
				this.ch = scannerinputreader.read();
				this.token = 141;
				return l;

			case 91: // '['
				this.ch = scannerinputreader.read();
				this.token = 142;
				return l;

			case 93: // ']'
				this.ch = scannerinputreader.read();
				this.token = 143;
				return l;

			case 44: // ','
				this.ch = scannerinputreader.read();
				this.token = 0;
				return l;

			case 59: // ';'
				this.ch = scannerinputreader.read();
				this.token = 135;
				return l;

			case 63: // '?'
				this.ch = scannerinputreader.read();
				this.token = 137;
				return l;

			case 126: // '~'
				this.ch = scannerinputreader.read();
				this.token = 38;
				return l;

			case 58: // ':'
				this.ch = scannerinputreader.read();
				this.token = 136;
				return l;

			case 45: // '-'
				switch (this.ch = scannerinputreader.read()) {
				case 45: // '-'
					this.ch = scannerinputreader.read();
					this.token = 51;
					return l;

				case 61: // '='
					this.ch = scannerinputreader.read();
					this.token = 6;
					return l;
				}
				this.token = 30;
				return l;

			case 43: // '+'
				switch (this.ch = scannerinputreader.read()) {
				case 43: // '+'
					this.ch = scannerinputreader.read();
					this.token = 50;
					return l;

				case 61: // '='
					this.ch = scannerinputreader.read();
					this.token = 5;
					return l;
				}
				this.token = 29;
				return l;

			case 60: // '<'
				switch (this.ch = scannerinputreader.read()) {
				case 60: // '<'
					if ((this.ch = scannerinputreader.read()) == 61) {
						this.ch = scannerinputreader.read();
						this.token = 7;
						return l;
					}
					this.token = 26;
					return l;

				case 61: // '='
					this.ch = scannerinputreader.read();
					this.token = 23;
					return l;
				}
				this.token = 24;
				return l;

			case 62: // '>'
				switch (this.ch = scannerinputreader.read()) {
				case 62: // '>'
					switch (this.ch = scannerinputreader.read()) {
					case 61: // '='
						this.ch = scannerinputreader.read();
						this.token = 8;
						return l;

					case 62: // '>'
						if ((this.ch = scannerinputreader.read()) == 61) {
							this.ch = scannerinputreader.read();
							this.token = 9;
							return l;
						}
						this.token = 28;
						return l;
					}
					this.token = 27;
					return l;

				case 61: // '='
					this.ch = scannerinputreader.read();
					this.token = 21;
					return l;
				}
				this.token = 22;
				return l;

			case 124: // '|'
				switch (this.ch = scannerinputreader.read()) {
				case 124: // '|'
					this.ch = scannerinputreader.read();
					this.token = 14;
					return l;

				case 61: // '='
					this.ch = scannerinputreader.read();
					this.token = 11;
					return l;
				}
				this.token = 16;
				return l;

			case 38: // '&'
				switch (this.ch = scannerinputreader.read()) {
				case 38: // '&'
					this.ch = scannerinputreader.read();
					this.token = 15;
					return l;

				case 61: // '='
					this.ch = scannerinputreader.read();
					this.token = 10;
					return l;
				}
				this.token = 18;
				return l;

			case 61: // '='
				if ((this.ch = scannerinputreader.read()) == 61) {
					this.ch = scannerinputreader.read();
					this.token = 20;
					return l;
				}
				this.token = 1;
				return l;

			case 37: // '%'
				if ((this.ch = scannerinputreader.read()) == 61) {
					this.ch = scannerinputreader.read();
					this.token = 4;
					return l;
				}
				this.token = 32;
				return l;

			case 94: // '^'
				if ((this.ch = scannerinputreader.read()) == 61) {
					this.ch = scannerinputreader.read();
					this.token = 12;
					return l;
				}
				this.token = 17;
				return l;

			case 33: // '!'
				if ((this.ch = scannerinputreader.read()) == 61) {
					this.ch = scannerinputreader.read();
					this.token = 19;
					return l;
				}
				this.token = 37;
				return l;

			case 42: // '*'
				if ((this.ch = scannerinputreader.read()) == 61) {
					this.ch = scannerinputreader.read();
					this.token = 2;
					return l;
				}
				this.token = 33;
				return l;

			case 36: // '$'
			case 65: // 'A'
			case 66: // 'B'
			case 67: // 'C'
			case 68: // 'D'
			case 69: // 'E'
			case 70: // 'F'
			case 71: // 'G'
			case 72: // 'H'
			case 73: // 'I'
			case 74: // 'J'
			case 75: // 'K'
			case 76: // 'L'
			case 77: // 'M'
			case 78: // 'N'
			case 79: // 'O'
			case 80: // 'P'
			case 81: // 'Q'
			case 82: // 'R'
			case 83: // 'S'
			case 84: // 'T'
			case 85: // 'U'
			case 86: // 'V'
			case 87: // 'W'
			case 88: // 'X'
			case 89: // 'Y'
			case 90: // 'Z'
			case 95: // '_'
			case 97: // 'a'
			case 98: // 'b'
			case 99: // 'c'
			case 100: // 'd'
			case 101: // 'e'
			case 102: // 'f'
			case 103: // 'g'
			case 104: // 'h'
			case 105: // 'i'
			case 106: // 'j'
			case 107: // 'k'
			case 108: // 'l'
			case 109: // 'm'
			case 110: // 'n'
			case 111: // 'o'
			case 112: // 'p'
			case 113: // 'q'
			case 114: // 'r'
			case 115: // 's'
			case 116: // 't'
			case 117: // 'u'
			case 118: // 'v'
			case 119: // 'w'
			case 120: // 'x'
			case 121: // 'y'
			case 122: // 'z'
				this.scanIdentifier();
				return l;

			case 26: // '\032'
				if ((this.ch = scannerinputreader.read()) == -1) {
					this.token = -1;
					return l;
				}
				this.env.error(this.pos, "funny.char");
				this.ch = scannerinputreader.read();
				break;

			case 0: // '\0'
			case 1: // '\001'
			case 2: // '\002'
			case 3: // '\003'
			case 4: // '\004'
			case 5: // '\005'
			case 6: // '\006'
			case 7: // '\007'
			case 8: // '\b'
			case 11: // '\013'
			case 13: // '\r'
			case 14: // '\016'
			case 15: // '\017'
			case 16: // '\020'
			case 17: // '\021'
			case 18: // '\022'
			case 19: // '\023'
			case 20: // '\024'
			case 21: // '\025'
			case 22: // '\026'
			case 23: // '\027'
			case 24: // '\030'
			case 25: // '\031'
			case 27: // '\033'
			case 28: // '\034'
			case 29: // '\035'
			case 30: // '\036'
			case 31: // '\037'
			case 35: // '#'
			case 64: // '@'
			case 92: // '\\'
			case 96: // '`'
			default:
				if (Character.isJavaIdentifierStart((char) this.ch)) {
					this.scanIdentifier();
					return l;
				}
				this.env.error(this.pos, "funny.char");
				this.ch = scannerinputreader.read();
				break;
			}
		} while (true);
	}

	void match(final int i, final int j) throws IOException {
		int k = 1;
		label0: do {
			do {
				do {
					this.scan();
					if (this.token != i) {
						break;
					}
					k++;
				} while (true);
				if (this.token != j) {
					continue label0;
				}
			} while (--k != 0);
			return;
		} while (this.token != -1);
		this.env.error(this.pos, "unbalanced.paren");
	}

	public static final long OFFSETINC = 1L;
	public static final long LINEINC = 0x100000000L;
	public static final int EOF = -1;
	public Environment env;
	private ScannerInputReader in;
	private final boolean scanComments;
	int token;
	long pos;
	long prevPos;
	private int ch;
	char charValue;
	int intValue;
	long longValue;
	float floatValue;
	double doubleValue;
	String stringValue;
	Identifier idValue;
	int radix;
	String docComment;
	private int count;
	private char buffer[];

	static {
		defineKeyword(92);
		defineKeyword(90);
		defineKeyword(91);
		defineKeyword(93);
		defineKeyword(94);
		defineKeyword(95);
		defineKeyword(96);
		defineKeyword(97);
		defineKeyword(98);
		defineKeyword(99);
		defineKeyword(100);
		defineKeyword(101);
		defineKeyword(102);
		defineKeyword(103);
		defineKeyword(104);
		defineKeyword(70);
		defineKeyword(71);
		defineKeyword(72);
		defineKeyword(73);
		defineKeyword(74);
		defineKeyword(75);
		defineKeyword(76);
		defineKeyword(77);
		defineKeyword(78);
		defineKeyword(25);
		defineKeyword(80);
		defineKeyword(81);
		defineKeyword(49);
		defineKeyword(82);
		defineKeyword(83);
		defineKeyword(84);
		defineKeyword(110);
		defineKeyword(111);
		defineKeyword(112);
		defineKeyword(113);
		defineKeyword(114);
		defineKeyword(115);
		defineKeyword(144);
		defineKeyword(120);
		defineKeyword(121);
		defineKeyword(122);
		defineKeyword(124);
		defineKeyword(125);
		defineKeyword(126);
		defineKeyword(127);
		defineKeyword(130);
		defineKeyword(129);
		defineKeyword(128);
		defineKeyword(131);
		defineKeyword(123);
		defineKeyword(58);
	}
}
