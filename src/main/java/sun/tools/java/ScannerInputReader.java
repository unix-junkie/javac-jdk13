package sun.tools.java;

import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

final class ScannerInputReader extends FilterReader {

	ScannerInputReader(final Environment env, final InputStream in) throws UnsupportedEncodingException {
		super(env.getCharacterEncoding() == null ? (Reader) new InputStreamReader(in) : (Reader) new InputStreamReader(in, env.getCharacterEncoding()));
		this.pushBack = -1;
		this.currentIndex = 0;
		this.numChars = 0;
		this.env = env;
		this.chpos = Scanner.LINEINC;
	}

	private int getNextChar() throws IOException {
		if (this.currentIndex >= this.numChars) {
			this.numChars = this.in.read(this.buffer);
			if (this.numChars == -1) {
				return -1;
			}
			this.currentIndex = 0;
		}
		return this.buffer[this.currentIndex++];
	}

	public int read(final char ac[], final int i, final int j) {
		throw new CompilerError("ScannerInputReader is not a fully implemented reader.");
	}

	public int read() throws IOException {
		int i;
		label0: {
			this.pos = this.chpos;
			this.chpos++;
			i = this.pushBack;
			if (i == -1) {
				try {
					if (this.currentIndex >= this.numChars) {
						this.numChars = this.in.read(this.buffer);
						if (this.numChars == -1) {
							i = -1;
							break label0;
						}
						this.currentIndex = 0;
					}
					i = this.buffer[this.currentIndex++];
				} catch (final IOException ioe) {
					final String className = ioe.getClass().getName();
					if (className.equals("java.nio.charset.MalformedInputException") || className.equals("sun.io.MalformedInputException")) {
						/*
						 * sun.io.
						 * MalformedInputException: Java
						 * 1.3, not present in
						 * subsequent versions (original
						 * code). java.nio.charset.
						 * MalformedInputException: Java
						 * 1.4+
						 */
						this.env.error(this.pos, "invalid.encoding.char");
						return -1;
					}

					throw ioe;
				}
			} else {
				this.pushBack = -1;
			}
		}
		switch (i) {
		case -2:
			return 92;

		case 92: // '\\'
			if ((i = this.getNextChar()) != 117) {
				this.pushBack = i != 92 ? i : -2;
				return 92;
			}
			for (this.chpos++; (i = this.getNextChar()) == 117; this.chpos++) {
			}
			int j = 0;
			for (int k = 0; k < 4;) {
				switch (i) {
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
					j = (j << 4) + i - 48;
					break;

				case 97: // 'a'
				case 98: // 'b'
				case 99: // 'c'
				case 100: // 'd'
				case 101: // 'e'
				case 102: // 'f'
					j = (j << 4) + 10 + i - 97;
					break;

				case 65: // 'A'
				case 66: // 'B'
				case 67: // 'C'
				case 68: // 'D'
				case 69: // 'E'
				case 70: // 'F'
					j = (j << 4) + 10 + i - 65;
					break;

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
				default:
					this.env.error(this.pos, "invalid.escape.char");
					this.pushBack = i;
					return j;
				}
				k++;
				this.chpos++;
				i = this.getNextChar();
			}

			this.pushBack = i;
			switch (j) {
			case 10: // '\n'
				this.chpos += Scanner.LINEINC;
				return 10;

			case 13: // '\r'
				if ((i = this.getNextChar()) != 10) {
					this.pushBack = i;
				} else {
					this.chpos++;
				}
				this.chpos += Scanner.LINEINC;
				return 10;
			}
			return j;

		case 10: // '\n'
			this.chpos += Scanner.LINEINC;
			return 10;

		case 13: // '\r'
			if ((i = this.getNextChar()) != 10) {
				this.pushBack = i;
			} else {
				this.chpos++;
			}
			this.chpos += Scanner.LINEINC;
			return 10;
		}
		return i;
	}

	private final Environment env;
	long pos;
	private long chpos;
	private int pushBack;
	private final char buffer[] = new char[10240];
	private int currentIndex;
	private int numChars;
}
