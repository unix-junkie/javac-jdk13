package com.sun.tools.javac.v8;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import com.sun.tools.javac.v8.code.ClassReader;
import com.sun.tools.javac.v8.code.ClassReader.BadClassFile;
import com.sun.tools.javac.v8.code.ClassReader.SourceCompleter;
import com.sun.tools.javac.v8.code.ClassWriter;
import com.sun.tools.javac.v8.code.ClassWriter.PoolOverflow;
import com.sun.tools.javac.v8.code.ClassWriter.StringOverflow;
import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.CompletionFailure;
import com.sun.tools.javac.v8.comp.Attr;
import com.sun.tools.javac.v8.comp.Check;
import com.sun.tools.javac.v8.comp.Enter;
import com.sun.tools.javac.v8.comp.Env;
import com.sun.tools.javac.v8.comp.Flow;
import com.sun.tools.javac.v8.comp.Gen;
import com.sun.tools.javac.v8.comp.Resolve;
import com.sun.tools.javac.v8.comp.Symtab;
import com.sun.tools.javac.v8.comp.TransInner;
import com.sun.tools.javac.v8.comp.TransTypes;
import com.sun.tools.javac.v8.parser.Parser;
import com.sun.tools.javac.v8.parser.Scanner;
import com.sun.tools.javac.v8.tree.Pretty;
import com.sun.tools.javac.v8.tree.Tree;
import com.sun.tools.javac.v8.tree.Tree.ClassDef;
import com.sun.tools.javac.v8.tree.Tree.TopLevel;
import com.sun.tools.javac.v8.tree.TreeMaker;
import com.sun.tools.javac.v8.util.Abort;
import com.sun.tools.javac.v8.util.Hashtable;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.ListBuffer;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Set;

public final class JavaCompiler implements SourceCompleter {

	public static String version() {
		return System.getProperty("java.version");
	}

	private JavaCompiler(final Log log1, final Symtab symtab, final Hashtable hashtable) {
		this.todo = new ListBuffer();
		this.inputFiles = Set.make();
		this.log = log1;
		this.syms = symtab;
		this.syms.reader.sourceCompleter = this;
		this.chk = new Check(log1, symtab, hashtable);
		this.rs = new Resolve(log1, symtab, this.chk);
		this.make = new TreeMaker();
		this.enter = new Enter(log1, symtab, this.rs, this.chk, this.make, null, this.todo);
		this.attr = new Attr(log1, symtab, this.rs, this.chk, this.make, this.enter, hashtable);
		this.gen = new Gen(log1, symtab, this.chk, this.rs, this.make, hashtable);
		this.verbose = hashtable.get("-verbose") != null;
		this.sourceOutput = hashtable.get("-s") != null;
		this.classOutput = hashtable.get("-retrofit") == null;
		this.printFlat = hashtable.get("-printflat") != null;
		this.deprecation = hashtable.get("-deprecation") != null;
		this.warnunchecked = hashtable.get("-warnunchecked") != null;
		this.gj = hashtable.get("-gj") != null;
		this.encoding = (String) hashtable.get("-encoding");
	}

	private static JavaCompiler make(final Log log1, final Hashtable hashtable) {
		try {
			return new JavaCompiler(log1, new Symtab(new ClassReader(hashtable), new ClassWriter(hashtable)), hashtable);
		} catch (final CompletionFailure completionfailure) {
			log1.error(0, completionfailure.getMessage());
		}
		return null;
	}

	public static JavaCompiler make(final Hashtable hashtable) {
		return make(new Log(hashtable.get("-prompt") != null, hashtable.get("-nowarn") == null), hashtable);
	}

	int errorCount() {
		return this.log.nerrors;
	}

	private InputStream openSource(final String s) {
		try {
			final File file = new File(s);
			this.inputFiles.add(file);
			return new FileInputStream(file);
		} catch (final IOException ignored) {
			this.log.error(0, "cant.read.file", s);
		}
		return null;
	}

	private TopLevel parse(final String s, final InputStream inputstream) {
		final long l = System.currentTimeMillis();
		final Name name = this.log.useSource(Name.fromString(s));
		TopLevel toplevel = this.make.TopLevel(null, Tree.emptyList);
		if (inputstream != null) {
			if (this.verbose) {
				printVerbose("parsing.started", s);
			}
			try {
				final Scanner scanner = new Scanner(inputstream, this.log, this.encoding);
				inputstream.close();
				final Parser parser = new Parser(scanner, this.make, this.log, this.gj, this.sourceOutput);
				toplevel = parser.compilationUnit();
				if (this.verbose) {
					printVerbose("parsing.done", Long.toString(System.currentTimeMillis() - l));
				}
			} catch (final IOException ioexception) {
				this.log.error(0, "error.reading.file", s, ioexception.toString());
			}
		}
		this.log.useSource(name);
		toplevel.sourcefile = Name.fromString(s);
		return toplevel;
	}

	private TopLevel parse(final String s) {
		return this.parse(s, this.openSource(s));
	}

	private void printSource(final Env env, final ClassDef classdef) throws IOException {
		final File file = this.syms.writer.outputFile(classdef.sym, ".java");
		if (this.inputFiles.contains(file)) {
			this.log.error(((Tree) classdef).pos, "source.cant.overwrite.input.file", file.toString());
		} else {
			final PrintStream printstream = new PrintStream(new BufferedOutputStream(new FileOutputStream(file)));
			try {
				new Pretty(printstream).printUnit(env.toplevel, classdef);
				if (this.verbose) {
					printVerbose("wrote.file", file.getPath());
				}
			} finally {
				printstream.close();
			}
		}
	}

	private void genCode(final Env env, final ClassDef classdef) throws IOException {
		try {
			if (this.gen.genClass(env, classdef)) {
				this.writeClass(classdef.sym);
			}
		} catch (final PoolOverflow ignored) {
			this.log.error(((Tree) classdef).pos, "limit.pool");
		} catch (final StringOverflow stringoverflow) {
			this.log.error(((Tree) classdef).pos, "limit.string.overflow", stringoverflow.value.substring(0, 20));
		}
	}

	private void writeClass(final ClassSymbol classsymbol) throws IOException, PoolOverflow, StringOverflow {
		final File file = this.syms.writer.outputFile(classsymbol, ".class");
		OutputStream fileoutputstream = new FileOutputStream(file);
		try {
			this.syms.writer.writeClassFile(fileoutputstream, classsymbol);
			if (this.verbose) {
				printVerbose("wrote.file", file.getPath());
			}
			fileoutputstream.close();
			fileoutputstream = null;
		} finally {
			if (fileoutputstream != null) {
				fileoutputstream.close();
				file.delete();
			}
		}
	}

	public void complete(final ClassSymbol classsymbol, final String s, final InputStream inputstream) {
		final TopLevel toplevel = this.parse(s, inputstream);
		this.enter.main(List.make(toplevel));
		if (classsymbol.members() == null) {
			throw new BadClassFile(classsymbol, s, Log.getLocalizedString("file.doesnt.contain.class", classsymbol.fullname.toJava()));
		}
	}

	void compile(final List list) {
		final long l = System.currentTimeMillis();
		try {
			this.inputFiles.clear();
			Symbol.reset();
			this.chk.compiled.clear();
			final ListBuffer listbuffer1 = new ListBuffer();
			for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
				listbuffer1.append(this.parse((String) list1.head));
			}

			final List list2 = listbuffer1.toList();
			this.enter.main(list2);
			List list3 = null;
			if (this.sourceOutput) {
				final ListBuffer listbuffer2 = new ListBuffer();
				for (List list4 = list2; list4.nonEmpty(); list4 = list4.tail) {
					for (List list5 = ((TopLevel) list4.head).defs; list5.nonEmpty(); list5 = list5.tail) {
						if (list5.head instanceof ClassDef) {
							listbuffer2.append(list5.head);
						}
					}

				}

				list3 = listbuffer2.toList();
			}
			for (Name name; this.todo.nonEmpty(); this.log.useSource(name)) {
				final Env env = (Env) this.todo.first();
				this.todo.remove();
				final Tree tree = env.tree;
				if (this.verbose) {
					printVerbose("checking.attribution", env.enclClass.sym.toJava());
				}
				name = this.log.useSource(env.enclClass.sym.sourcefile);
				this.attr.attribClass(env.enclClass.sym);
				if (this.errorCount() == 0) {
					new Flow(this.log, this.syms, this.chk).analyzeDef(env.tree);
				}
				final TreeMaker treemaker = new TreeMaker(env.toplevel);
				if (this.errorCount() == 0) {
					env.tree = new TransTypes(this.log, treemaker).translateTopLevelClass(env.tree);
				}
				if (this.errorCount() == 0) {
					ClassDef classdef = null;
					try {
						if (this.sourceOutput) {
							classdef = (ClassDef) env.tree;
							if (list3.contains(tree)) {
								this.printSource(env, classdef);
							}
						} else {
							final List list6 = new TransInner(this.log, this.syms, this.rs, this.chk, this.attr, treemaker).translateTopLevelClass(env, env.tree);
							for (List list7 = list6; this.errorCount() == 0 && list7.nonEmpty(); list7 = list7.tail) {
								classdef = (ClassDef) list7.head;
								if (this.printFlat) {
									this.printSource(env, classdef);
								} else if (this.classOutput) {
									this.genCode(env, classdef);
								}
							}

						}
					} catch (final IOException ioexception) {
						this.log.error(((Tree) classdef).pos, "class.cant.write", classdef.sym.toJava(), ioexception.getMessage());
					}
				}
			}

		} catch (final Abort ignored) {
		}
		if (this.verbose) {
			printVerbose("total", Long.toString(System.currentTimeMillis() - l));
		}
		if (this.chk.deprecatedSource != null && !this.deprecation) {
			this.noteDeprecated(this.chk.deprecatedSource.toString());
		}
		if (this.chk.uncheckedSource != null && !this.warnunchecked) {
			this.noteUnchecked(this.chk.uncheckedSource.toString());
		}
		final int i = this.errorCount();
		if (i == 1) {
			printCount("error", 1);
		} else {
			printCount("error.plural", i);
		}
		if (this.log.nwarnings == 1) {
			printCount("warn", this.log.nwarnings);
		} else {
			printCount("warn.plural", this.log.nwarnings);
		}
	}

	private static void printVerbose(final String s, final String s1) {
		System.err.println(Log.getLocalizedString("verbose." + s, s1));
	}

	private void noteDeprecated(final String s) {
		if (s.equals("*")) {
			this.log.note("deprecated.plural");
		} else {
			this.log.note("deprecated.filename", s);
		}
		this.log.note("deprecated.recompile");
	}

	private void noteUnchecked(final String s) {
		if (s.equals("*")) {
			this.log.note("unchecked.plural");
		} else {
			this.log.note("unchecked.filename", s);
		}
		this.log.note("unchecked.recompile");
	}

	private static void printCount(final String s, final int i) {
		if (i != 0) {
			Log.println(Log.getLocalizedString("count." + s, Integer.toString(i)));
		}
	}

	public void close() {
		this.syms.reader.close();
	}

	private final Log log;
	private final Symtab syms;
	private final Check chk;
	private final Resolve rs;
	private final TreeMaker make;
	private final Enter enter;
	private final Attr attr;
	private final Gen gen;
	private final boolean verbose;
	private final boolean sourceOutput;
	private final boolean classOutput;
	private final boolean printFlat;
	private final boolean deprecation;
	private final boolean warnunchecked;
	private final boolean gj;
	private final String encoding;
	private final ListBuffer todo;
	private final Set inputFiles;
}
