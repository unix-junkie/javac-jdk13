package com.sun.tools.javac.v8;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.sun.tools.javac.v8.code.ClassFile;
import com.sun.tools.javac.v8.code.Type;
import com.sun.tools.javac.v8.util.FatalError;
import com.sun.tools.javac.v8.util.Hashtable;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.ListBuffer;
import com.sun.tools.javac.v8.util.Util;

public final class Main {

	public Main(final String ownName) {
		this.options = Hashtable.make();
		this.ownName = ownName;
		this.enabledOptions = standardOptions;
	}

	private void help() {
		System.err.println(getLocalizedString("msg.usage.header", this.ownName));
		for (int i = 0; i < this.enabledOptions.length; i++) {
			if (!this.enabledOptions[i][0].startsWith("-X")) {
				String s = "  " + this.enabledOptions[i][0] + ' ';
				if (this.enabledOptions[i][1].length() != 0) {
					s += getLocalizedString(this.enabledOptions[i][1]);
				}
				System.err.print(s);
				for (int j = s.length(); j < 28; j++) {
					System.err.print(" ");
				}

				System.err.println(getLocalizedString(this.enabledOptions[i][2]));
			}
		}

		System.err.println();
	}

	private void error(final String s, final String s1) {
		System.err.println(this.ownName + ": " + getLocalizedString(s, s1));
		this.help();
	}

	private static boolean matches(String s, final String s1) {
		final int i = s.length();
		int k = Util.pos(s, ':', 0);
		int l = Util.pos(s1, ':', 0);
		if (s.substring(0, k).equals(s1.substring(0, l))) {
			final int j = s1.length();
			if (k == i && l == j) {
				return true;
			}
			if (k < i && l < j) {
				l++;
				if (++k < i && s.charAt(k) == '{') {
					k++;
					s = s.substring(0, s.length() - 1) + ',';
				}
				int i1 = l;
				boolean flag;
				int j1;
				for (flag = true; flag & i1 < j; i1 = j1 + 1) {
					j1 = Util.pos(s1, ',', i1);
					final String s2 = s1.substring(i1, j1);
					int k1 = k;
					int l1;
					for (flag = false; !flag && k1 < i; k1 = l1 + 1) {
						l1 = Util.pos(s, ',', k1);
						if (s.substring(k1, l1).equals(s2)) {
							flag = true;
						}
					}

				}

				return flag;
			}
		}
		return false;
	}

	private List processArgs(final String as[]) {
		final ListBuffer listbuffer = new ListBuffer();
		for (int i = 0; i < as.length;) {
			String s = as[i];
			i++;
			if (s.length() > 0 && s.charAt(0) == '-') {
				int j;
				for (j = 0; j < this.enabledOptions.length && !matches(this.enabledOptions[j][0], s); j++) {
				}
				if (j == this.enabledOptions.length) {
					this.error("err.invalid.flag", s);
					return null;
				}
				String s1 = this.enabledOptions[j][1];
				if (s1.length() != 0) {
					if (i == as.length) {
						this.error("err.req.arg", s);
						return null;
					}
					s1 = as[i];
					i++;
				}
				if (s.equals("-moreinfo")) {
					Type.moreInfo = true;
				} else if (s.equals("-version")) {
					System.err.println(this.ownName + ' ' + JavaCompiler.version() + ", " + "DD-MMMMM-YY");
				} else {
					if (s.equals("-target")) {
						if (!ClassFile.isValidTargetRelease(s1)) {
							this.error("err.invalid.target", s1);
							return null;
						}
					} else if (s.length() >= 3 && s.charAt(2) == ':') {
						s1 = s.substring(3);
						s = s.substring(0, 3);
					}
					this.options.put(s, s1);
				}
			} else if (s.endsWith(".java")) {
				listbuffer.append(s);
			} else {
				this.error("err.invalid.arg", s);
				return null;
			}
		}

		return listbuffer.toList();
	}

	public int compile(final String args[]) {
		JavaCompiler javacompiler = null;
		try {
			if (args.length == 0) {
				this.help();
				return (byte) EXIT_CMDERR;
			}
			this.processArgs(forcedOpts);
			final List list = this.processArgs(CommandLine.parse(args));
			if (list == null) {
				return (byte) EXIT_CMDERR;
			}
			javacompiler = JavaCompiler.make(this.options);
			if (javacompiler == null) {
				return (byte) EXIT_SYSERR;
			}
			javacompiler.compile(list);
			System.err.flush();
			if (javacompiler.errorCount() != 0) {
				return EXIT_ERROR;
			}
		} catch (final IOException ioe) {
			ioMessage(ioe);
			return (byte) EXIT_SYSERR;
		} catch (final OutOfMemoryError oome) {
			resourceMessage(oome);
			return (byte) EXIT_SYSERR;
		} catch (final FatalError fe) {
			feMessage(fe);
			return (byte) EXIT_SYSERR;
		} catch (final Throwable t) {
			bugMessage(t);
			return (byte) EXIT_ABNORMAL;
		} finally {
			if (javacompiler != null) {
				javacompiler.close();
			}
			this.options = null;
		}
		return EXIT_OK;
	}

	private static void bugMessage(final Throwable throwable) {
		System.err.println(getLocalizedString("msg.bug", JavaCompiler.version()));
		throwable.printStackTrace();
	}

	private static void feMessage(final Throwable throwable) {
		System.err.println(throwable.getMessage());
	}

	private static void ioMessage(final Throwable throwable) {
		System.err.println(getLocalizedString("msg.io"));
		throwable.printStackTrace();
	}

	private static void resourceMessage(final Throwable throwable) {
		System.err.println(getLocalizedString("msg.resource"));
		throwable.printStackTrace();
	}

	private static String getLocalizedString(final String s) {
		return getText("javac." + s, null);
	}

	private static String getLocalizedString(final String s, final String s1) {
		return getText("javac." + s, s1);
	}

	private static void initResource() {
		try {
			messageRB = ResourceBundle.getBundle("com.sun.tools.javac.v8.resources.javac");
		} catch (final MissingResourceException missingresourceexception) {
			System.out.println(missingresourceexception.getMessage());
			System.out.println(missingresourceexception.getClassName());
			throw new FatalError("Fatal Error: Resource for javac is missing");
		}
	}

	private static String getText(final String s, String s1) {
		if (messageRB == null) {
			initResource();
		}
		try {
			final String as[] = { s1 };
			return MessageFormat.format(messageRB.getString(s), as);
		} catch (final MissingResourceException ignored) {
		}
		if (s1 == null) {
			s1 = "null";
		}
		final String as1[] = { s, s1 };
		final String s2 = "javac message file broken: key={0} arguments={1}";
		return MessageFormat.format(s2, as1);
	}

	private static final String[] forcedOpts = new String[0];
	private final String ownName;
	private static final int EXIT_OK = 0;
	private static final int EXIT_ERROR = 1;
	private static final int EXIT_CMDERR = 2;
	private static final int EXIT_SYSERR = 3;
	private static final int EXIT_ABNORMAL = 4;
	private static final String[][] standardOptions = { { "-g", "", "opt.g" }, { "-g:none", "", "opt.g.none" }, { "-g:{lines,vars,source}", "", "opt.g.lines.vars.source" }, { "-O", "", "opt.O" }, { "-nowarn", "", "opt.nowarn" }, { "-verbose", "", "opt.verbose" }, { "-deprecation", "", "opt.deprecation" }, { "-classpath", "opt.arg.path", "opt.classpath" }, { "-sourcepath", "opt.arg.path", "opt.sourcepath" }, { "-bootclasspath", "opt.arg.path", "opt.bootclasspath" }, { "-extdirs", "opt.arg.dirs", "opt.extdirs" }, { "-d", "opt.arg.directory", "opt.d" }, { "-encoding", "opt.arg.encoding", "opt.encoding" }, { "-target", "opt.arg.release", "opt.target" } };
	private final String[][] enabledOptions;
	private Hashtable options;
	private static ResourceBundle messageRB;

}
