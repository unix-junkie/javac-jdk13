package com.sun.tools.javac.v8.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;
import com.sun.tools.javac.v8.util.Position;

public final class Scanner {

	public Scanner(final InputStream inputstream, final Log log1, final String s) {
		this.errPos = 0;
		this.deprecatedFlag = false;
		this.sbuf = new char[128];
		this.unicodeConversionBp = 0;
		this.docComment = null;
		this.log = log1;
		try {
			final int i = inputstream.available() + 1;
			if (this.buf == null || this.buf.length < i) {
				this.buf = new char[i];
			}
			this.buflen = 0;
			final InputStreamReader inputstreamreader = s != null ? new InputStreamReader(inputstream, s) : new InputStreamReader(inputstream);
			do {
				int j = inputstreamreader.read(this.buf, this.buflen, this.buf.length - this.buflen);
				if (j < 0) {
					j = 0;
				}
				this.buflen += j;
				if (this.buflen < this.buf.length) {
					break;
				}
				final char ac[] = new char[this.buflen * 2];
				System.arraycopy(this.buf, 0, ac, 0, this.buflen);
				this.buf = ac;
			} while (true);
		} catch (final IOException ioexception) {
			this.lexError(ioexception.toString());
			this.buf = new char[1];
			this.buflen = 0;
		}
		this.buf[this.buflen] = '\0';
		this.line = 1;
		this.col = 0;
		this.bp = -1;
		this.scanChar();
		this.nextToken();
	}

	private void lexError(final int i, final String s, final String s1) {
		this.log.error(i, s, s1);
		this.token = 1;
		this.errPos = i;
	}

	private void lexError(final int i, final String s) {
		this.lexError(i, s, null);
	}

	private void lexError(final String s) {
		this.lexError(this.pos, s, null);
	}

	private void lexError(final String s, final String s1) {
		this.lexError(this.pos, s, s1);
	}

	private void convertUnicode() {
		final int i = this.col;
		if (this.ch == '\\') {
			this.bp++;
			this.ch = this.buf[this.bp];
			this.col++;
			if (this.ch == 'u') {
				do {
					this.bp++;
					this.ch = this.buf[this.bp];
					this.col++;
				} while (this.ch == 'u');
				final int j = this.bp + 3;
				if (j < this.buflen) {
					int k = Character.digit(this.ch, 16);
					int l;
					for (l = k; this.bp < j && k >= 0; l = (l << 4) + k) {
						this.bp++;
						this.ch = this.buf[this.bp];
						this.col++;
						k = Character.digit(this.ch, 16);
					}

					if (k >= 0) {
						this.ch = (char) l;
						this.unicodeConversionBp = this.bp;
						return;
					}
				}
				this.lexError(Position.make(this.line, i), "illegal.unicode.esc");
			} else {
				this.bp--;
				this.ch = '\\';
			}
		}
	}

	private void scanChar() {
		this.bp++;
		this.ch = this.buf[this.bp];
		this.col++;
		if (this.ch == '\\') {
			this.convertUnicode();
		}
	}

	private void scanCommentChar() {
		this.bp++;
		this.ch = this.buf[this.bp];
		this.col++;
		if (this.ch == '\\') {
			if (this.buf[this.bp + 1] == '\\') {
				this.bp++;
				this.col++;
			} else {
				this.convertUnicode();
			}
		}
	}

	private void putChar(final char c) {
		if (this.sp == this.sbuf.length) {
			final char ac[] = new char[this.sbuf.length * 2];
			System.arraycopy(this.sbuf, 0, ac, 0, this.sbuf.length);
			this.sbuf = ac;
		}
		this.sbuf[this.sp++] = c;
	}

	private void scanLitChar() {
		if (this.ch == '\\') {
			if (this.buf[this.bp + 1] == '\\' && this.unicodeConversionBp != this.bp) {
				this.bp++;
				this.col++;
				this.putChar('\\');
				this.scanChar();
			} else {
				this.scanChar();
				switch (this.ch) {
				case 48: // '0'
				case 49: // '1'
				case 50: // '2'
				case 51: // '3'
				case 52: // '4'
				case 53: // '5'
				case 54: // '6'
				case 55: // '7'
					final char c = this.ch;
					int i = Character.digit(this.ch, 8);
					this.scanChar();
					if (this.ch >= '0' && this.ch <= '7') {
						i = i * 8 + Character.digit(this.ch, 8);
						this.scanChar();
						if (c <= '3' && this.ch >= '0' && this.ch <= '7') {
							i = i * 8 + Character.digit(this.ch, 8);
							this.scanChar();
						}
					}
					this.putChar((char) i);
					break;

				case 98: // 'b'
					this.putChar('\b');
					this.scanChar();
					break;

				case 116: // 't'
					this.putChar('\t');
					this.scanChar();
					break;

				case 110: // 'n'
					this.putChar('\n');
					this.scanChar();
					break;

				case 102: // 'f'
					this.putChar('\f');
					this.scanChar();
					break;

				case 114: // 'r'
					this.putChar('\r');
					this.scanChar();
					break;

				case 39: // '\''
					this.putChar('\'');
					this.scanChar();
					break;

				case 34: // '"'
					this.putChar('"');
					this.scanChar();
					break;

				case 92: // '\\'
					this.putChar('\\');
					this.scanChar();
					break;

				default:
					this.lexError(Position.make(this.line, this.col), "illegal.esc.char");
					break;
				}
			}
		} else if (this.bp != this.buflen) {
			this.putChar(this.ch);
			this.scanChar();
		}
	}

	private void scanFraction() {
		for (; Character.digit(this.ch, 10) >= 0; this.scanChar()) {
			this.putChar(this.ch);
		}

		final int i = this.sp;
		if (this.ch == 'e' || this.ch == 'E') {
			this.putChar(this.ch);
			this.scanChar();
			if (this.ch == '+' || this.ch == '-') {
				this.putChar(this.ch);
				this.scanChar();
			}
			if (this.ch >= '0' && this.ch <= '9') {
				do {
					this.putChar(this.ch);
					this.scanChar();
				} while (this.ch >= '0' && this.ch <= '9');
				return;
			}
			this.lexError("malformed.fp.lit");
			this.sp = i;
		}
	}

	private void scanFractionAndSuffix() {
		this.scanFraction();
		if (this.ch == 'f' || this.ch == 'F') {
			this.scanChar();
			this.token = 53;
		} else {
			if (this.ch == 'd' || this.ch == 'D') {
				this.scanChar();
			}
			this.token = 54;
		}
	}

	private void scanNumber(final int i) {
		this.radix = i;
		for (; Character.digit(this.ch, i != 8 ? i : 10) >= 0; this.scanChar()) {
			this.putChar(this.ch);
		}

		if (i <= 10 && this.ch == '.') {
			this.putChar(this.ch);
			this.scanChar();
			this.scanFractionAndSuffix();
		} else if (i <= 10 && (this.ch == 'e' || this.ch == 'E' || this.ch == 'f' || this.ch == 'F' || this.ch == 'd' || this.ch == 'D')) {
			this.scanFractionAndSuffix();
		} else if (this.ch == 'l' || this.ch == 'L') {
			this.scanChar();
			this.token = 52;
		} else {
			this.token = 51;
		}
	}

	private void scanIdent() {
		do {
			label0: do {
				if (this.sp == this.sbuf.length) {
					this.putChar(this.ch);
				} else {
					this.sbuf[this.sp++] = this.ch;
				}
				this.bp++;
				this.ch = this.buf[this.bp];
				this.col++;
				if (this.ch == '\\') {
					this.convertUnicode();
				}
				switch (this.ch) {
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
		} while (this.ch > '\200' && Character.isJavaIdentifierPart(this.ch));
		this.name = Name.fromChars(this.sbuf, 0, this.sp);
		this.token = this.name.index <= maxKey ? key[this.name.index] : 2;
	}

	private static boolean isSpecial(final char c) {
		switch (c) {
		case 33: // '!'
		case 37: // '%'
		case 38: // '&'
		case 42: // '*'
		case 43: // '+'
		case 45: // '-'
		case 58: // ':'
		case 60: // '<'
		case 61: // '='
		case 62: // '>'
		case 63: // '?'
		case 94: // '^'
		case 124: // '|'
		case 126: // '~'
			return true;
		}
		return false;
	}

	private void scanOperator() {
		do {
			this.putChar(this.ch);
			final Name name1 = Name.fromChars(this.sbuf, 0, this.sp);
			if (name1.index > maxKey || key[name1.index] == 2) {
				this.sp--;
				break;
			}
			this.name = name1;
			this.token = key[name1.index];
			this.scanChar();
		} while (isSpecial(this.ch));
	}

	private void skipComment() {
		while (this.bp < this.buflen) {
			switch (this.ch) {
			case 26: // '\032'
				this.lexError(this.pos, "unclosed.comment");
				return;

			case 42: // '*'
				this.scanChar();
				if (this.ch == '/') {
					return;
				}
				break;

			case 9: // '\t'
				this.col = (this.col - 1) / 8 * 8 + 8;
				this.scanCommentChar();
				break;

			case 12: // '\f'
				this.col = 0;
				this.scanCommentChar();
				break;

			case 13: // '\r'
				this.line++;
				this.col = 0;
				this.scanCommentChar();
				if (this.ch == '\n') {
					this.col = 0;
					this.scanCommentChar();
				}
				break;

			case 10: // '\n'
				this.line++;
				this.col = 0;
				this.scanCommentChar();
				break;

			default:
				this.bp++;
				this.ch = this.buf[this.bp];
				this.col++;
				if (this.ch != '\\') {
					break;
				}
				if (this.buf[this.bp + 1] == '\\') {
					this.bp++;
					this.col++;
				} else {
					this.convertUnicode();
				}
				break;
			}
		}
	}

	private String scanDocComment() {
		while (this.bp < this.buflen && this.ch == '*') {
			this.scanCommentChar();
		}
		if (this.bp < this.buflen && this.ch == '/') {
			return "";
		}
		if (this.bp < this.buflen && this.ch == '\n') {
			this.line++;
			this.col = 0;
			this.scanCommentChar();
		}
		char[] ac = new char[1024];
		int i = 0;
		label0: while (this.bp < this.buflen) {
			label1: while (this.bp < this.buflen) {
				switch (this.ch) {
				default:
					break label1;

				case 32: // ' '
					this.scanCommentChar();
					break;

				case 9: // '\t'
					this.col = (this.col - 1) / 8 * 8 + 8;
					this.scanCommentChar();
					break;

				case 12: // '\f'
					this.col = 0;
					this.scanCommentChar();
					break;

				case 13: // '\r'
					this.line++;
					this.col = 0;
					this.scanCommentChar();
					if (this.ch == '\n') {
						this.col = 0;
						this.scanCommentChar();
					}
					break;

				case 10: // '\n'
					this.line++;
					this.col = 0;
					this.scanCommentChar();
					break;
				}
			}
			if (this.ch == '*') {
				do {
					this.scanCommentChar();
				} while (this.ch == '*');
				if (this.ch == '/') {
					break;
				}
			}
			boolean flag1 = true;
			label2: while (this.bp < this.buflen) {
				switch (this.ch) {
				case 26: // '\032'
					this.lexError(this.pos, "unclosed.comment");
					break label0;

				case 42: // '*'
					flag1 = false;
					this.scanCommentChar();
					if (this.ch != '/') {
						if (i == ac.length) {
							final char ac1[] = new char[ac.length * 2];
							System.arraycopy(ac, 0, ac1, 0, ac.length);
							ac = ac1;
						}
						ac[i++] = '*';
						break;
					}
					break label0;

				case 32: // ' '
					if (i == ac.length) {
						final char ac2[] = new char[ac.length * 2];
						System.arraycopy(ac, 0, ac2, 0, ac.length);
						ac = ac2;
					}
					ac[i++] = this.ch;
					this.scanCommentChar();
					break;

				case 12: // '\f'
					this.col = 0;
					this.scanCommentChar();
					break label2;

				case 13: // '\r'
					this.line++;
					this.col = 0;
					this.scanCommentChar();
					if (this.ch == '\n') {
						this.col = 0;
						this.scanCommentChar();
					}
					break label2;

				case 10: // '\n'
					this.line++;
					this.col = 0;
					if (i == ac.length) {
						final char ac3[] = new char[ac.length * 2];
						System.arraycopy(ac, 0, ac3, 0, ac.length);
						ac = ac3;
					}
					ac[i++] = this.ch;
					this.scanCommentChar();
					break label2;

				default:
					if (this.ch == '@' && flag1) {
						final int j = this.bp + 1;
						do {
							if (i == ac.length) {
								final char ac5[] = new char[ac.length * 2];
								System.arraycopy(ac, 0, ac5, 0, ac.length);
								ac = ac5;
							}
							ac[i++] = this.ch;
							this.scanCommentChar();
						} while (this.ch >= 'a' && this.ch <= 'z');
						if (Name.fromChars(this.buf, j, this.bp - j) == Names.deprecated) {
							this.deprecatedFlag = true;
						}
					} else {
						if (i == ac.length) {
							final char ac4[] = new char[ac.length * 2];
							System.arraycopy(ac, 0, ac4, 0, ac.length);
							ac = ac4;
						}
						ac[i++] = this.ch;
						this.scanCommentChar();
					}
					flag1 = false;
					break;
				}
			}
		}
		if (i > 0) {
			int k;
			label3: for (k = i - 1; k > -1;) {
				switch (ac[k]) {
				default:
					break label3;

				case 42: // '*'
					k--;
					break;
				}
			}

			i = k + 1;
			return new String(ac, 0, i);
		}
		return "";
	}

	public String stringVal() {
		return new String(this.sbuf, 0, this.sp);
	}

	public void nextToken() {
		this.lastPos = (this.line << 10) + this.col;
		this.sp = 0;
		this.docComment = null;
		label0: do {
			this.pos = (this.line << 10) + this.col;
			switch (this.ch) {
			case 11: // '\013'
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
			case 35: // '#'
			case 37: // '%'
			case 38: // '&'
			case 42: // '*'
			case 43: // '+'
			case 45: // '-'
			case 58: // ':'
			case 60: // '<'
			case 61: // '='
			case 62: // '>'
			case 63: // '?'
			case 64: // '@'
			case 92: // '\\'
			case 94: // '^'
			case 96: // '`'
			case 124: // '|'
			default:
				break label0;

			case 32: // ' '
				this.bp++;
				this.ch = this.buf[this.bp];
				this.col++;
				if (this.ch == '\\') {
					this.convertUnicode();
				}
				break;

			case 9: // '\t'
				this.col = (this.col - 1) / 8 * 8 + 8;
				this.scanChar();
				break;

			case 12: // '\f'
				this.col = 0;
				this.scanChar();
				break;

			case 13: // '\r'
				this.line++;
				this.col = 0;
				this.scanChar();
				if (this.ch == '\n') {
					this.col = 0;
					this.scanChar();
				}
				break;

			case 10: // '\n'
				this.line++;
				this.col = 0;
				this.scanChar();
				break;

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
				this.scanIdent();
				return;

			case 48: // '0'
				this.scanChar();
				if (this.ch == 'x' || this.ch == 'X') {
					this.scanChar();
					if (Character.digit(this.ch, 16) < 0) {
						this.lexError("invalid.hex.number");
					}
					this.scanNumber(16);
				} else {
					this.putChar('0');
					this.scanNumber(8);
				}
				return;

			case 49: // '1'
			case 50: // '2'
			case 51: // '3'
			case 52: // '4'
			case 53: // '5'
			case 54: // '6'
			case 55: // '7'
			case 56: // '8'
			case 57: // '9'
				this.scanNumber(10);
				return;

			case 46: // '.'
				this.scanChar();
				if (this.ch >= '0' && this.ch <= '9') {
					this.putChar('.');
					this.scanFractionAndSuffix();
				} else {
					this.token = 68;
				}
				return;

			case 44: // ','
				this.scanChar();
				this.token = 67;
				return;

			case 59: // ';'
				this.scanChar();
				this.token = 66;
				return;

			case 40: // '('
				this.scanChar();
				this.token = 60;
				return;

			case 41: // ')'
				this.scanChar();
				this.token = 61;
				return;

			case 91: // '['
				this.scanChar();
				this.token = 64;
				return;

			case 93: // ']'
				this.scanChar();
				this.token = 65;
				return;

			case 123: // '{'
				this.scanChar();
				this.token = 62;
				return;

			case 125: // '}'
				this.scanChar();
				this.token = 63;
				return;

			case 47: // '/'
				this.scanChar();
				switch (this.ch) {
				case '/':
					do {
						this.bp++;
						this.ch = this.buf[this.bp];
						this.col++;
						if (this.ch == '\\') {
							if (this.buf[this.bp + 1] == '\\') {
								this.bp++;
								this.col++;
							} else {
								this.convertUnicode();
							}
						}
					} while (this.ch != '\r' && this.ch != '\n' && this.bp < this.buflen);
					break;
				case '*':
					this.scanChar();
					if (this.ch == '*') {
						this.docComment = this.scanDocComment();
					} else {
						this.skipComment();
					}
					if (this.ch == '/') {
						this.scanChar();
					} else {
						this.lexError("unclosed.comment");
						return;
					}
					break;
				default:
					if (this.ch == '=') {
						this.name = Names.slashequals;
						this.token = 98;
						this.scanChar();
					} else {
						this.name = Names.slash;
						this.token = 87;
					}
					return;
				}
				break;

			case 39: // '\''
				this.scanChar();
				if (this.ch == '\'') {
					this.lexError("empty.char.lit");
				} else {
					if (this.ch == '\r' || this.ch == '\n') {
						this.lexError(this.pos, "illegal.line.end.in.char.lit");
					}
					this.scanLitChar();
					if (this.ch == '\'') {
						this.scanChar();
						this.token = 55;
					} else {
						this.lexError(this.pos, "unclosed.char.lit");
					}
				}
				return;

			case 34: // '"'
				this.scanChar();
				while (this.ch != '"' && this.ch != '\r' && this.ch != '\n' && this.bp < this.buflen) {
					this.scanLitChar();
				}
				if (this.ch == '"') {
					this.token = 56;
					this.scanChar();
				} else {
					this.lexError(this.pos, "unclosed.str.lit");
				}
				return;
			}
		} while (true);
		if (isSpecial(this.ch)) {
			this.scanOperator();
		} else if (Character.isJavaIdentifierStart(this.ch)) {
			this.scanIdent();
		} else if (this.bp == this.buflen || this.ch == '\032' && this.bp + 1 == this.buflen) {
			this.token = 0;
		} else {
			this.lexError("illegal.char", String.valueOf(this.ch));
			this.scanChar();
		}
	}

	public static String token2string(final int i) {
		switch (i) {
		case 2: // '\002'
			return Log.getLocalizedString("token.identifier");

		case 55: // '7'
			return Log.getLocalizedString("token.character");

		case 56: // '8'
			return Log.getLocalizedString("token.string");

		case 51: // '3'
			return Log.getLocalizedString("token.integer");

		case 52: // '4'
			return Log.getLocalizedString("token.long-integer");

		case 53: // '5'
			return Log.getLocalizedString("token.float");

		case 54: // '6'
			return Log.getLocalizedString("token.double");

		case 68: // 'D'
			return "'.'";

		case 67: // 'C'
			return "','";

		case 66: // 'B'
			return "';'";

		case 60: // '<'
			return "'('";

		case 61: // '='
			return "')'";

		case 64: // '@'
			return "'['";

		case 65: // 'A'
			return "']'";

		case 62: // '>'
			return "'{'";

		case 63: // '?'
			return "'}'";

		case 1: // '\001'
			return Log.getLocalizedString("token.bad-symbol");

		case 0: // '\0'
			return Log.getLocalizedString("token.end-of-input");

		case 3: // '\003'
		case 4: // '\004'
		case 5: // '\005'
		case 6: // '\006'
		case 7: // '\007'
		case 8: // '\b'
		case 9: // '\t'
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
		case 42: // '*'
		case 43: // '+'
		case 44: // ','
		case 45: // '-'
		case 46: // '.'
		case 47: // '/'
		case 48: // '0'
		case 49: // '1'
		case 50: // '2'
		case 57: // '9'
		case 58: // ':'
		case 59: // ';'
		default:
			return tokenName[i].toJava();
		}
	}

	private static void enterKeyword(final String s, final int i) {
		final Name name1 = Name.fromString(s);
		tokenName[i] = name1;
		if (name1.index > maxKey) {
			maxKey = name1.index;
		}
	}

	int token;
	int pos;
	int lastPos;
	int errPos;
	Name name;
	int radix;
	boolean deprecatedFlag;
	private char sbuf[];
	private int sp;
	private char buf[];
	private int bp;
	private int buflen;
	private char ch;
	private int line;
	private int col;
	private int unicodeConversionBp;
	private static final byte[] key;
	private static int maxKey;
	private final Log log;
	String docComment;
	private static final Name[] tokenName;

	static {
		maxKey = 0;
		tokenName = new Name[106];
		for (int i = 0; i < 106; i++) {
			tokenName[i] = null;
		}

		enterKeyword("+", 84);
		enterKeyword("-", 85);
		enterKeyword("!", 72);
		enterKeyword("%", 91);
		enterKeyword("^", 90);
		enterKeyword("&", 88);
		enterKeyword("*", 86);
		enterKeyword("|", 89);
		enterKeyword("~", 73);
		enterKeyword("/", 87);
		enterKeyword(">", 70);
		enterKeyword("<", 71);
		enterKeyword("?", 74);
		enterKeyword(":", 75);
		enterKeyword("=", 69);
		enterKeyword("++", 82);
		enterKeyword("--", 83);
		enterKeyword("==", 76);
		enterKeyword("<=", 77);
		enterKeyword(">=", 78);
		enterKeyword("!=", 79);
		enterKeyword("<<", 92);
		enterKeyword(">>", 93);
		enterKeyword(">>>", 94);
		enterKeyword("+=", 95);
		enterKeyword("-=", 96);
		enterKeyword("*=", 97);
		enterKeyword("/=", 98);
		enterKeyword("&=", 99);
		enterKeyword("|=", 100);
		enterKeyword("^=", 101);
		enterKeyword("%=", 102);
		enterKeyword("<<=", 103);
		enterKeyword(">>=", 104);
		enterKeyword(">>>=", 105);
		enterKeyword("||", 81);
		enterKeyword("&&", 80);
		enterKeyword("abstract", 3);
		enterKeyword("boolean", 4);
		enterKeyword("break", 5);
		enterKeyword("byte", 6);
		enterKeyword("case", 7);
		enterKeyword("catch", 8);
		enterKeyword("char", 9);
		enterKeyword("class", 10);
		enterKeyword("const", 11);
		enterKeyword("continue", 12);
		enterKeyword("default", 13);
		enterKeyword("do", 14);
		enterKeyword("double", 15);
		enterKeyword("else", 16);
		enterKeyword("extends", 17);
		enterKeyword("final", 18);
		enterKeyword("finally", 19);
		enterKeyword("float", 20);
		enterKeyword("for", 21);
		enterKeyword("goto", 22);
		enterKeyword("if", 23);
		enterKeyword("implements", 24);
		enterKeyword("import", 25);
		enterKeyword("instanceof", 26);
		enterKeyword("int", 27);
		enterKeyword("interface", 28);
		enterKeyword("long", 29);
		enterKeyword("native", 30);
		enterKeyword("new", 31);
		enterKeyword("package", 32);
		enterKeyword("private", 33);
		enterKeyword("protected", 34);
		enterKeyword("public", 35);
		enterKeyword("return", 36);
		enterKeyword("short", 37);
		enterKeyword("static", 38);
		enterKeyword("strictfp", 39);
		enterKeyword("super", 40);
		enterKeyword("switch", 41);
		enterKeyword("synchronized", 42);
		enterKeyword("this", 43);
		enterKeyword("throw", 44);
		enterKeyword("throws", 45);
		enterKeyword("transient", 46);
		enterKeyword("try", 47);
		enterKeyword("void", 48);
		enterKeyword("volatile", 49);
		enterKeyword("while", 50);
		enterKeyword("true", 57);
		enterKeyword("false", 58);
		enterKeyword("null", 59);
		key = new byte[maxKey + 1];
		for (int j = 0; j <= maxKey; j++) {
			key[j] = 2;
		}

		for (byte byte0 = 0; byte0 < 106; byte0++) {
			if (tokenName[byte0] != null) {
				key[tokenName[byte0].index] = byte0;
			}
		}

	}
}
