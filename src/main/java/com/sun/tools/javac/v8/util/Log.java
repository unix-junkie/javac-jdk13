package com.sun.tools.javac.v8.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Log {

	public Log(final boolean flag, final boolean flag1) {
		this.MaxErrors = 100;
		this.MaxWarnings = 100;
		this.sourcename = Names.__input;
		this.nerrors = 0;
		this.nwarnings = 0;
		this.recorded = Set.make();
		this.buf = null;
		this.promptOnError = flag;
		this.emitWarnings = flag1;
	}

	public Log() {
		this(false, true);
	}

	public Name useSource(final Name name) {
		final Name name1 = this.sourcename;
		this.sourcename = name;
		if (name1 != this.sourcename) {
			this.buf = null;
		}
		return name1;
	}

	public Name currentSource() {
		return this.sourcename;
	}

	private static void print(final String s) {
		System.err.print(s);
	}

	public static void println(final String s) {
		print(s + '\n');
	}

	private void prompt() {
		if (this.promptOnError) {
			System.err.println(getLocalizedString("resume.abort"));
			try {
				do {
					switch (System.in.read()) {
					case 65: // 'A'
					case 97: // 'a'
						System.exit(-1);
						return;

					case 82: // 'R'
					case 114: // 'r'
						return;

					case 88: // 'X'
					case 120: // 'x'
						throw new InternalError("user abort");
					}
				} while (true);
			} catch (final IOException ignored) {
			}
		}
	}

	private void printErrLine(final int i, final int j) {
		try {
			if (this.buf == null) {
				final InputStream in = new FileInputStream(this.sourcename.toString());
				this.buf = new byte[in.available()];
				in.read(this.buf);
				in.close();
				this.bp = 0;
				this.lastLine = 1;
			} else if (this.lastLine > i) {
				this.bp = 0;
				this.lastLine = 1;
			}
			while (this.bp < this.buf.length && this.lastLine < i) {
				switch (this.buf[this.bp]) {
				case 13: // '\r'
					this.bp++;
					if (this.bp < this.buf.length && this.buf[this.bp] == 10) {
						this.bp++;
					}
					this.lastLine++;
					break;

				case 10: // '\n'
					this.bp++;
					this.lastLine++;
					break;

				default:
					this.bp++;
					break;
				}
			}
			int k;
			for (k = this.bp; k < this.buf.length && this.buf[k] != 13 && this.buf[k] != 10; k++) {
			}
			println(new String(this.buf, this.bp, k - this.bp));
			final byte abyte0[] = new byte[j];
			for (int l = 0; l < j - 1; l++) {
				abyte0[l] = 32;
			}

			abyte0[j - 1] = 94;
			println(new String(abyte0, 0, j));
		} catch (final IOException ignored) {
			println(getLocalizedString("source.unavailable"));
		}
	}

	private void printError(final int i, final String s) {
		if (i == 0) {
			print(getText("compiler.err.error", null, null, null, null, null, null, null));
			println(s);
		} else {
			final int j = Position.line(i);
			final int k = Position.column(i);
			println(this.sourcename + ":" + j + ": " + s);
			this.printErrLine(j, k);
		}
	}

	public void error(final int i, final String s) {
		this.error(i, s, null, null, null, null, null, null, null);
	}

	public void error(final int i, final String s, final String s1) {
		this.error(i, s, s1, null, null, null, null, null, null);
	}

	public void error(final int i, final String s, final String s1, final String s2) {
		this.error(i, s, s1, s2, null, null, null, null, null);
	}

	public void error(final int i, final String s, final String s1, final String s2, final String s3) {
		this.error(i, s, s1, s2, s3, null, null, null, null);
	}

	public void error(final int i, final String s, final String s1, final String s2, final String s3, final String s4) {
		this.error(i, s, s1, s2, s3, s4, null, null, null);
	}

	public void error(final int i, final String s, final String s1, final String s2, final String s3, final String s4, final String s5) {
		this.error(i, s, s1, s2, s3, s4, s5, null, null);
	}

	public void error(final int i, final String s, final String s1, final String s2, final String s3, final String s4, final String s5, final String s6) {
		this.error(i, s, s1, s2, s3, s4, s5, s6, null);
	}

	public void error(final int i, final String s, final String s1, final String s2, final String s3, final String s4, final String s5, final String s6, final String s7) {
		if (this.nerrors < this.MaxErrors) {
			final Pair pair = new Pair(this.sourcename, new Integer(i));
			if (!this.recorded.contains(pair)) {
				this.recorded.add(pair);
				final String s8 = getText("compiler.err." + s, s1, s2, s3, s4, s5, s6, s7);
				this.printError(i, s8);
				this.prompt();
				this.nerrors++;
			}
		}
	}

	public void warning(final int i, final String s) {
		this.warning(i, s, null, null, null, null);
	}

	public void warning(final int i, final String s, final String s1, final String s2) {
		this.warning(i, s, s1, s2, null, null);
	}

	public void warning(final int i, final String s, final String s1, final String s2, final String s3, final String s4) {
		if (this.nwarnings < this.MaxWarnings && this.emitWarnings) {
			final String s5 = getText("compiler.warn." + s, s1, s2, s3, s4, null, null, null);
			this.printError(i, getText("compiler.warn.warning", null, null, null, null, null, null, null) + s5);
		}
		this.nwarnings++;
	}

	public void note(final String s) {
		this.note(s, null);
	}

	public void note(final String s, final String s1) {
		if (this.emitWarnings) {
			print(getText("compiler.note.note", null, null, null, null, null, null, null));
			final String s2 = getText("compiler.note." + s, s1, null, null, null, null, null, null);
			println(s2);
		}
	}

	public static String getLocalizedString(final String s) {
		return getText("compiler.misc." + s, null, null, null, null, null, null, null);
	}

	public static String getLocalizedString(final String s, final String s1) {
		return getText("compiler.misc." + s, s1, null, null, null, null, null, null);
	}

	public static String getLocalizedString(final String s, final String s1, final String s2) {
		return getText("compiler.misc." + s, s1, s2, null, null, null, null, null);
	}

	public static String getLocalizedString(final String s, final String s1, final String s2, final String s3) {
		return getText("compiler.misc." + s, s1, s2, s3, null, null, null, null);
	}

	public static String getLocalizedString(final String s, final String s1, final String s2, final String s3, final String s4) {
		return getText("compiler.misc." + s, s1, s2, s3, s4, null, null, null);
	}

	private static void initResource() {
		try {
			messageRB = ResourceBundle.getBundle("com.sun.tools.javac.v8.resources.compiler");
		} catch (final MissingResourceException ignored) {
			throw new Error("Fatal: Resource for compiler is missing");
		}
	}

	private static String getText(final String s, String s1, String s2, String s3, String s4, String s5, String s6, String s7) {
		if (messageRB == null) {
			initResource();
		}
		try {
			final String as[] = { s1, s2, s3, s4, s5, s6, s7 };
			return MessageFormat.format(messageRB.getString(s), as);
		} catch (final MissingResourceException ignored) {
		}
		if (s1 == null) {
			s1 = "null";
		}
		if (s2 == null) {
			s2 = "null";
		}
		if (s3 == null) {
			s3 = "null";
		}
		if (s4 == null) {
			s4 = "null";
		}
		if (s5 == null) {
			s5 = "null";
		}
		if (s6 == null) {
			s6 = "null";
		}
		if (s7 == null) {
			s7 = "null";
		}
		final String as1[] = { s, s1, s2, s3, s4, s5, s6, s7 };
		final String s8 = "compiler message file broken: key={0} arguments={1}, {2}, {3}, {4}, {5}, {6}, {7}";
		return MessageFormat.format(s8, as1);
	}

	private final int MaxErrors;
	private final int MaxWarnings;
	private final boolean promptOnError;
	private final boolean emitWarnings;
	private Name sourcename;
	public int nerrors;
	public int nwarnings;
	private final Set recorded;
	private byte buf[];
	private int bp;
	private int lastLine;
	private static ResourceBundle messageRB;
}
