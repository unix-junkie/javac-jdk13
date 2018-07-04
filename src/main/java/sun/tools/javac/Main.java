package sun.tools.javac;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import sun.tools.asm.Assembler;
import sun.tools.java.ClassDeclaration;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassFile;
import sun.tools.java.ClassNotFound;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.util.CommandLine;

public final class Main {
	private Main(final OutputStream out) {
		this.out = out;
	}

	private void output(final String s) {
		final PrintStream printstream = this.out instanceof PrintStream ? (PrintStream) this.out : new PrintStream(this.out, true);
		printstream.println(s);
	}

	private void error(final String s) {
		this.exitStatus = 2;
		this.output(getText(s));
	}

	private void error(final String s, final String s1) {
		this.exitStatus = 2;
		this.output(getText(s, s1));
	}

	private void error(final String s, final String s1, final String s2) {
		this.exitStatus = 2;
		this.output(getText(s, s1, s2));
	}

	private void usage_error() {
		this.error("main.usage", program);
	}

	private static void initResource() {
		try {
			messageRB = ResourceBundle.getBundle("sun.tools.javac.resources.javac");
		} catch (final MissingResourceException ignored) {
			throw new Error("Fatal: Resource for javac is missing");
		}
	}

	private static String getText(final String s) {
		return getText(s, (String) null);
	}

	private static String getText(final String s, final int i) {
		return getText(s, Integer.toString(i));
	}

	public static String getText(final String s, final String s1) {
		return getText(s, s1, null);
	}

	static String getText(final String s, final String s1, final String s2) {
		return getText(s, s1, s2, null);
	}

	static String getText(final String s, String s1, String s2, String s3) {
		if (messageRB == null) {
			initResource();
		}
		try {
			final String s4 = messageRB.getString(s);
			final String as[] = new String[3];
			as[0] = s1;
			as[1] = s2;
			as[2] = s3;
			return MessageFormat.format(s4, as);
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
		final String as1[] = { s, s1, s2, s3 };
		final String s5 = "JAVAC MESSAGE FILE IS BROKEN: key={0}, arguments={1}, {2}, {3}";
		return MessageFormat.format(s5, as1);
	}

	private synchronized boolean compile(String args[]) {
		long l = System.currentTimeMillis();
		this.exitStatus = 0;
		try {
			args = CommandLine.parse(args);
		} catch (final FileNotFoundException filenotfoundexception) {
			this.error("javac.err.cant.read", filenotfoundexception.getMessage());
			System.exit(1);
		} catch (final IOException ioexception) {
			ioexception.printStackTrace();
			System.exit(1);
		}
		String s9 = null;
		String s8 = null;
		String s7 = null;
		boolean flag1 = false;
		final Collection sourceFiles = new ArrayList();
		int i = 0x41004;
		final String s6 = "-Xjcov:file=";
		final String s5 = "-Xjcov";
		File file1 = null;
		File file = null;
		short word1 = 3;
		short word0 = 45;
		String s4 = null;
		boolean flag = false;
		String s3 = null;
		String s2 = null;
		String s1 = null;
		String s = null;
		for (int j = 0; j < args.length; j++) {
			if (args[j].equals("-g")) {
				if (s8 != null && !s8.equals("-g")) {
					this.error("main.conflicting.options", s8, "-g");
				}
				s8 = "-g";
				i |= 0x1000;
				i |= 0x2000;
				i |= Constants.F_DEBUG_SOURCE;
			} else if (args[j].equals("-g:none")) {
				if (s8 != null && !s8.equals("-g:none")) {
					this.error("main.conflicting.options", s8, "-g:none");
				}
				s8 = "-g:none";
				i &= 0xffffefff;
				i &= 0xffffdfff;
				i &= 0xfffbffff;
			} else if (args[j].startsWith("-g:")) {
				if (s8 != null && !s8.equals(args[j])) {
					this.error("main.conflicting.options", s8, args[j]);
				}
				s8 = args[j];
				String s10 = args[j].substring("-g:".length());
				i &= 0xffffefff;
				i &= 0xffffdfff;
				i &= 0xfffbffff;
				do {
					if (s10.startsWith("lines")) {
						i |= 0x1000;
						s10 = s10.substring("lines".length());
					} else if (s10.startsWith("vars")) {
						i |= 0x2000;
						s10 = s10.substring("vars".length());
					} else if (s10.startsWith("source")) {
						i |= Constants.F_DEBUG_SOURCE;
						s10 = s10.substring("source".length());
					} else {
						this.error("main.bad.debug.option", args[j]);
						this.usage_error();
						return false;
					}
					if (s10.length() == 0) {
						break;
					}
					if (s10.length() > 0 && s10.charAt(0) == ',') {
						s10 = s10.substring(",".length());
					}
				} while (true);
			} else if (args[j].equals("-O")) {
				if (s9 != null && !s9.equals("-O")) {
					this.error("main.conflicting.options", s9, "-O");
				}
				s9 = "-O";
			} else if (args[j].equals("-nowarn")) {
				i &= -5;
			} else if (args[j].equals("-deprecation")) {
				i |= 0x200;
			} else if (args[j].equals("-verbose")) {
				i |= 1;
			} else if (args[j].equals("-nowrite")) {
				flag1 = true;
			} else if (args[j].equals("-classpath")) {
				if (j + 1 < args.length) {
					if (s1 != null) {
						this.error("main.option.already.seen", "-classpath");
					}
					s1 = args[++j];
				} else {
					this.error("main.option.requires.argument", "-classpath");
					this.usage_error();
					return false;
				}
			} else if (args[j].equals("-sourcepath")) {
				if (j + 1 < args.length) {
					if (s != null) {
						this.error("main.option.already.seen", "-sourcepath");
					}
					s = args[++j];
				} else {
					this.error("main.option.requires.argument", "-sourcepath");
					this.usage_error();
					return false;
				}
			} else if (args[j].equals("-sysclasspath")) {
				if (j + 1 < args.length) {
					if (s2 != null) {
						this.error("main.option.already.seen", "-sysclasspath");
					}
					s2 = args[++j];
				} else {
					this.error("main.option.requires.argument", "-sysclasspath");
					this.usage_error();
					return false;
				}
			} else if (args[j].equals("-bootclasspath")) {
				if (j + 1 < args.length) {
					if (s2 != null) {
						this.error("main.option.already.seen", "-bootclasspath");
					}
					s2 = args[++j];
				} else {
					this.error("main.option.requires.argument", "-bootclasspath");
					this.usage_error();
					return false;
				}
			} else if (args[j].equals("-extdirs")) {
				if (j + 1 < args.length) {
					if (s3 != null) {
						this.error("main.option.already.seen", "-extdirs");
					}
					s3 = args[++j];
				} else {
					this.error("main.option.requires.argument", "-extdirs");
					this.usage_error();
					return false;
				}
			} else if (args[j].equals("-encoding")) {
				if (j + 1 < args.length) {
					if (s7 != null) {
						this.error("main.option.already.seen", "-encoding");
					}
					s7 = args[++j];
				} else {
					this.error("main.option.requires.argument", "-encoding");
					this.usage_error();
					return false;
				}
			} else if (args[j].equals("-target")) {
				if (j + 1 < args.length) {
					if (s4 != null) {
						this.error("main.option.already.seen", "-target");
					}
					s4 = args[++j];
					int k;
					for (k = 0; k < releases.length; k++) {
						if (!releases[k].equals(s4)) {
							continue;
						}
						word0 = majorVersions[k];
						word1 = minorVersions[k];
						break;
					}

					if (k == releases.length) {
						this.error("main.unknown.release", s4);
						this.usage_error();
						return false;
					}
				} else {
					this.error("main.option.requires.argument", "-target");
					this.usage_error();
					return false;
				}
			} else if (args[j].equals("-d")) {
				if (j + 1 < args.length) {
					if (file != null) {
						this.error("main.option.already.seen", "-d");
					}
					file = new File(args[++j]);
					if (!file.exists()) {
						this.error("main.no.such.directory", file.getPath());
						this.usage_error();
						return false;
					}
				} else {
					this.error("main.option.requires.argument", "-d");
					this.usage_error();
					return false;
				}
			} else if (args[j].equals(s5)) {
				i |= 0x40;
				i &= 0xffffbfff;
				i &= 0xffff7fff;
			} else if (args[j].startsWith(s6) && args[j].length() > s6.length()) {
				file1 = new File(args[j].substring(s6.length()));
				i &= 0xffffbfff;
				i &= 0xffff7fff;
				i |= 0x40;
				i |= 0x80;
			} else if (args[j].equals("-XO")) {
				if (s9 != null && !s9.equals("-XO")) {
					this.error("main.conflicting.options", s9, "-XO");
				}
				s9 = "-XO";
				i |= 0x4000;
			} else if (args[j].equals("-Xinterclass")) {
				if (s9 != null && !s9.equals("-Xinterclass")) {
					this.error("main.conflicting.options", s9, "-Xinterclass");
				}
				s9 = "-Xinterclass";
				i |= 0x4000;
				i |= 0x8000;
				i |= 0x20;
			} else if (args[j].equals("-Xdepend")) {
				i |= 0x20;
			} else if (args[j].equals("-Xdebug")) {
				i |= 2;
			} else if (args[j].equals("-xdepend") || args[j].equals("-Xjws")) {
				i |= 0x400;
				if (this.out == System.err) {
					this.out = System.out;
				}
			} else if (args[j].equals("-Xstrictdefault")) {
				i |= 0x20000;
			} else if (args[j].equals("-Xverbosepath")) {
				flag = true;
			} else if (args[j].equals("-Xstdout")) {
				this.out = System.out;
			} else {
				if (args[j].equals("-X")) {
					this.error("main.unsupported.usage");
					return false;
				}
				if (args[j].equals("-Xversion1.2")) {
					i |= 0x800;
				} else if (args[j].endsWith(".java")) {
					sourceFiles.add(args[j]);
				} else {
					this.error("main.no.such.option", args[j]);
					this.usage_error();
					return false;
				}
			}
		}

		if (sourceFiles.isEmpty() || this.exitStatus == 2) {
			this.usage_error();
			return false;
		}
		final BatchEnvironment batchenvironment = BatchEnvironment.create(this.out, s, s1, s2, s3);
		if (flag) {
			this.output(getText("main.path.msg", batchenvironment.sourcePath.toString(), batchenvironment.binaryPath.toString()));
		}
		batchenvironment.flags |= i;
		batchenvironment.majorVersion = word0;
		batchenvironment.minorVersion = word1;
		batchenvironment.covFile = file1;
		batchenvironment.setCharacterEncoding(s7);
		final String s11 = getText("main.no.memory");
		final String s12 = getText("main.stack.overflow");
		try {
			for (final Iterator it = sourceFiles.iterator(); it.hasNext();) {
				final File sourceFile = new File((String) it.next());
				try {
					batchenvironment.parseFile(new ClassFile(sourceFile));
				} catch (final FileNotFoundException ignored) {
					batchenvironment.error(0L, "cant.read", sourceFile.getPath());
					this.exitStatus = 2;
				}
			}

			for (final Enumeration enumeration1 = batchenvironment.getClasses(); enumeration1.hasMoreElements();) {
				final ClassDeclaration classdeclaration = (ClassDeclaration) enumeration1.nextElement();
				if (classdeclaration.getStatus() == 4 && !classdeclaration.getClassDefinition().isLocal()) {
					try {
						classdeclaration.getClassDefinition(batchenvironment);
					} catch (final ClassNotFound ignored) {
					}
				}
			}

			final ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream(4096);
			boolean flag3;
			do {
				flag3 = true;
				batchenvironment.flushErrors();
				for (final Enumeration enumeration2 = batchenvironment.getClasses(); enumeration2.hasMoreElements();) {
					final ClassDeclaration classdeclaration1 = (ClassDeclaration) enumeration2.nextElement();
					switch (classdeclaration1.getStatus()) {
					case 1: // '\001'
					case 2: // '\002'
					default:
						break;

					case 0: // '\0'
						if (!batchenvironment.dependencies()) {
							break;
							// fall through
						}

					case 3: // '\003'
						Environment.dtEvent("Main.compile (SOURCE): loading, " + classdeclaration1);
						flag3 = false;
						batchenvironment.loadDefinition(classdeclaration1);
						if (classdeclaration1.getStatus() != 4) {
							Environment.dtEvent("Main.compile (SOURCE): not parsed, " + classdeclaration1);
							break;
						}
						// fall through

					case 4: // '\004'
						if (classdeclaration1.getClassDefinition().isInsideLocal()) {
							Environment.dtEvent("Main.compile (PARSED): skipping local class, " + classdeclaration1);
							break;
						}
						flag3 = false;
						Environment.dtEvent("Main.compile (PARSED): checking, " + classdeclaration1);
						final ClassDefinition sourceclass = classdeclaration1.getClassDefinition(batchenvironment);
						sourceclass.check(batchenvironment);
						classdeclaration1.setDefinition(sourceclass, 5);
						// fall through

					case 5: // '\005'
						final SourceClass sourceclass1 = (SourceClass) classdeclaration1.getClassDefinition(batchenvironment);
						if (sourceclass1.getError()) {
							Environment.dtEvent("Main.compile (CHECKED): bailing out on error, " + classdeclaration1);
							classdeclaration1.setDefinition(sourceclass1, 6);
							break;
						}
						flag3 = false;
						bytearrayoutputstream.reset();
						Environment.dtEvent("Main.compile (CHECKED): compiling, " + classdeclaration1);
						sourceclass1.compile(bytearrayoutputstream);
						classdeclaration1.setDefinition(sourceclass1, 6);
						sourceclass1.cleanup(batchenvironment);
						if (sourceclass1.getNestError() || flag1) {
							break;
						}
						final String s14 = classdeclaration1.getName().getQualifier().toString().replace('.', File.separatorChar);
						final String s15 = classdeclaration1.getName().getFlatName().toString().replace('.', '$') + ".class";
						File file3;
						if (file != null) {
							if (s14.length() > 0) {
								file3 = new File(file, s14);
								if (!file3.exists()) {
									file3.mkdirs();
								}
								file3 = new File(file3, s15);
							} else {
								file3 = new File(file, s15);
							}
						} else {
							final ClassFile classfile = (ClassFile) sourceclass1.getSource();
							if (classfile.isZipped()) {
								batchenvironment.error(0L, "cant.write", classfile.getPath());
								this.exitStatus = 2;
								break;
							}
							file3 = new File(classfile.getPath());
							file3 = new File(file3.getParent(), s15);
						}
						try {
							final OutputStream fileoutputstream = new FileOutputStream(file3.getPath());
							bytearrayoutputstream.writeTo(fileoutputstream);
							fileoutputstream.close();
							if (batchenvironment.verbose()) {
								this.output(getText("main.wrote", file3.getPath()));
							}
						} catch (final IOException ignored) {
							batchenvironment.error(0L, "cant.write", file3.getPath());
							this.exitStatus = 2;
						}
						if (batchenvironment.print_dependencies()) {
							sourceclass1.printClassDependencies(batchenvironment);
						}
						break;
					}
				}

			} while (!flag3);
		} catch (final OutOfMemoryError ignored) {
			batchenvironment.output(s11);
			this.exitStatus = 3;
			return false;
		} catch (final StackOverflowError ignored) {
			batchenvironment.output(s12);
			this.exitStatus = 3;
			return false;
		} catch (final Error error1) {
			if (batchenvironment.nerrors == 0 || batchenvironment.dump()) {
				error1.printStackTrace();
				batchenvironment.error(0L, "fatal.error");
				this.exitStatus = 4;
			}
		} catch (final Exception exception) {
			if (batchenvironment.nerrors == 0 || batchenvironment.dump()) {
				exception.printStackTrace();
				batchenvironment.error(0L, "fatal.exception");
				this.exitStatus = 4;
			}
		}
		final int i1 = batchenvironment.deprecationFiles.size();
		if (i1 > 0 && batchenvironment.warnings()) {
			final int j1 = batchenvironment.ndeprecations;
			final Object obj1 = batchenvironment.deprecationFiles.elementAt(0);
			if (batchenvironment.deprecation()) {
				if (i1 > 1) {
					batchenvironment.error(0L, "warn.note.deprecations", new Integer(i1), new Integer(j1));
				} else {
					batchenvironment.error(0L, "warn.note.1deprecation", obj1, new Integer(j1));
				}
			} else if (i1 > 1) {
				batchenvironment.error(0L, "warn.note.deprecations.silent", new Integer(i1), new Integer(j1));
			} else {
				batchenvironment.error(0L, "warn.note.1deprecation.silent", obj1, new Integer(j1));
			}
		}
		batchenvironment.flushErrors();
		batchenvironment.shutdown();
		boolean flag2 = true;
		if (batchenvironment.nerrors > 0) {
			String s13 = batchenvironment.nerrors > 1 ? getText("main.errors", batchenvironment.nerrors) : getText("main.1error");
			if (batchenvironment.nwarnings > 0) {
				s13 = batchenvironment.nwarnings > 1 ? s13 + ", " + getText("main.warnings", batchenvironment.nwarnings) : s13 + ", " + getText("main.1warning");
			}
			this.output(s13);
			if (this.exitStatus == 0) {
				this.exitStatus = 1;
			}
			flag2 = false;
		} else if (batchenvironment.nwarnings > 0) {
			if (batchenvironment.nwarnings > 1) {
				this.output(getText("main.warnings", batchenvironment.nwarnings));
			} else {
				this.output(getText("main.1warning"));
			}
		}
		if (batchenvironment.covdata()) {
			Assembler.GenJCov(batchenvironment);
		}
		if (batchenvironment.verbose()) {
			l = System.currentTimeMillis() - l;
			this.output(getText("main.done_in", Long.toString(l)));
		}
		return flag2;
	}

	public static void main(final String args[]) {
		PrintStream out = System.err;
		if (Boolean.getBoolean("javac.pipe.output")) {
			out = System.out;
		}
		final Main main = new Main(out);
		System.exit(main.compile(args) ? 0 : main.exitStatus);
	}

	private static final String program = "javac";
	private OutputStream out;
	public static final int EXIT_OK = 0;
	public static final int EXIT_ERROR = 1;
	public static final int EXIT_CMDERR = 2;
	public static final int EXIT_SYSERR = 3;
	public static final int EXIT_ABNORMAL = 4;
	private int exitStatus;
	private static ResourceBundle messageRB;
	private static final String releases[] = { "1.1", "1.2", "1.3" };
	private static final short majorVersions[] = { 45, 46, 47 };
	private static final short minorVersions[] = { 3, 0, 0 };

}
