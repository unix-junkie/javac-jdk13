package sun.tools.javac;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import sun.tools.java.BinaryClass;
import sun.tools.java.ClassDeclaration;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassFile;
import sun.tools.java.ClassNotFound;
import sun.tools.java.ClassPath;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.IdentifierToken;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Package;
import sun.tools.java.Type;
import sun.tools.tree.Node;

public final class BatchEnvironment extends Environment implements ErrorConsumer {
	private BatchEnvironment(final OutputStream outputstream, final ClassPath classpath, final ErrorConsumer errorconsumer) {
		this(outputstream, classpath, classpath, errorconsumer);
	}

	private BatchEnvironment(final OutputStream outputstream, final ClassPath classpath, final ClassPath classpath1) {
		this(outputstream, classpath, classpath1, (ErrorConsumer) null);
	}

	private BatchEnvironment(final OutputStream outputstream, final ClassPath classpath, final ClassPath classpath1, final ErrorConsumer errorconsumer) {
		this.packages = new Hashtable(31);
		this.classesOrdered = new Vector();
		this.classes = new Hashtable(351);
		this.majorVersion = 45;
		this.minorVersion = 3;
		this.deprecationFiles = new Vector();
		this.errorLimit = 100;
		this.out = outputstream;
		this.sourcePath = classpath;
		this.binaryPath = classpath1;
		this.errorConsumer = errorconsumer != null ? errorconsumer : (ErrorConsumer) this;
	}

	static BatchEnvironment create(final OutputStream outputstream, final String s, final String s1, final String s2, final String s3) {
		final ClassPath aclasspath[] = classPaths(s, s1, s2, s3);
		return new BatchEnvironment(outputstream, aclasspath[0], aclasspath[1]);
	}

	private static ClassPath[] classPaths(String s, String s1, String s2, String s3) {
		final StringBuffer stringbuffer = new StringBuffer();
		if (s1 == null) {
			s1 = System.getProperty("env.class.path");
			if (s1 == null) {
				s1 = ".";
			}
		}
		if (s == null) {
			s = s1;
		}
		if (s2 == null) {
			s2 = System.getProperty("sun.boot.class.path");
			if (s2 == null) {
				s2 = System.getProperty("java.class.path");
				if (s2 == null) {
					s2 = "";
				}
			}
		}
		appendPath(stringbuffer, s2);
		if (s3 == null) {
			s3 = System.getProperty("java.ext.dirs");
		}
		if (s3 != null) {
			for (final StringTokenizer stringtokenizer = new StringTokenizer(s3, File.pathSeparator); stringtokenizer.hasMoreTokens();) {
				String s4 = stringtokenizer.nextToken();
				final File file = new File(s4);
				if (!s4.endsWith(File.separator)) {
					s4 += File.separator;
				}
				if (file.isDirectory()) {
					final String as[] = file.list();
					for (int i = 0; i < as.length; i++) {
						final String s5 = as[i];
						if (s5.endsWith(".jar")) {
							appendPath(stringbuffer, s4 + s5);
						}
					}

				}
			}

		}
		appendPath(stringbuffer, s1);
		final ClassPath classpath = new ClassPath(s);
		final ClassPath classpath1 = new ClassPath(stringbuffer.toString());
		return new ClassPath[] { classpath, classpath1 };
	}

	private static void appendPath(final StringBuffer stringbuffer, final String s) {
		if (s.length() > 0) {
			if (stringbuffer.length() > 0) {
				stringbuffer.append(File.pathSeparator);
			}
			stringbuffer.append(s);
		}
	}

	public int getFlags() {
		return this.flags;
	}

	public short getMajorVersion() {
		return this.majorVersion;
	}

	public short getMinorVersion() {
		return this.minorVersion;
	}

	public File getcovFile() {
		return this.covFile;
	}

	public Enumeration getClasses() {
		return this.classesOrdered.elements();
	}

	public boolean isExemptPackage(final Identifier identifier) {
		if (this.exemptPackages == null) {
			this.setExemptPackages();
		}
		return this.exemptPackages.contains(identifier);
	}

	private void setExemptPackages() {
		this.exemptPackages = new HashSet(101);
		for (final Enumeration enumeration = this.getClasses(); enumeration.hasMoreElements();) {
			final ClassDeclaration classdeclaration = (ClassDeclaration) enumeration.nextElement();
			if (classdeclaration.getStatus() == 4) {
				final SourceClass sourceclass = (SourceClass) classdeclaration.getClassDefinition();
				if (!sourceclass.isLocal()) {
					for (Identifier identifier = sourceclass.getImports().getCurrentPackage(); identifier != Constants.idNull && this.exemptPackages.add(identifier); identifier = identifier.getQualifier()) {
					}
				}
			}
		}

		if (!this.exemptPackages.contains(Constants.idJavaLang)) {
			this.exemptPackages.add(Constants.idJavaLang);
			if (!this.getPackage(Constants.idJavaLang).exists()) {
				this.error(0L, "package.not.found.strong", Constants.idJavaLang);
			}
		}
	}

	public ClassDeclaration getClassDeclaration(final Identifier identifier) {
		return this.getClassDeclaration(Type.tClass(identifier));
	}

	public ClassDeclaration getClassDeclaration(final Type type) {
		ClassDeclaration classdeclaration = (ClassDeclaration) this.classes.get(type);
		if (classdeclaration == null) {
			this.classes.put(type, classdeclaration = new ClassDeclaration(type.getClassName()));
			this.classesOrdered.addElement(classdeclaration);
		}
		return classdeclaration;
	}

	public boolean classExists(Identifier identifier) {
		if (identifier.isInner()) {
			identifier = identifier.getTopName();
		}
		final Type type = Type.tClass(identifier);
		final ClassDeclaration classdeclaration = (ClassDeclaration) this.classes.get(type);
		return classdeclaration == null ? this.getPackage(identifier.getQualifier()).classExists(identifier.getName()) : classdeclaration.getName().equals(identifier);
	}

	public Package getPackage(final Identifier identifier) {
		Package package1 = (Package) this.packages.get(identifier);
		if (package1 == null) {
			this.packages.put(identifier, package1 = new Package(this.sourcePath, this.binaryPath, identifier));
		}
		return package1;
	}

	public void parseFile(final ClassFile classfile) throws FileNotFoundException {
		long l = System.currentTimeMillis();
		dtEnter("parseFile: PARSING SOURCE " + classfile);
		final Environment environment = new Environment(this, classfile);
		final InputStream inputstream;
		final BatchParser batchparser;
		try {
			inputstream = classfile.getInputStream();
			environment.setCharacterEncoding(this.getCharacterEncoding());
			batchparser = new BatchParser(environment, inputstream);
		} catch (final IOException ignored) {
			dtEvent("parseFile: IO EXCEPTION " + classfile);
			throw new FileNotFoundException();
		}
		try {
			batchparser.parseFile();
		} catch (final Exception exception) {
			throw new CompilerError(exception);
		}
		try {
			inputstream.close();
		} catch (final IOException ignored) {
		}
		if (this.verbose()) {
			l = System.currentTimeMillis() - l;
			this.output(Main.getText("benv.parsed_in", classfile.getPath(), Long.toString(l)));
		}
		if (batchparser.classes.isEmpty()) {
			batchparser.imports.resolve(environment);
		} else {
			final Iterator iterator = batchparser.classes.iterator();
			final ClassDefinition classdefinition = (ClassDefinition) iterator.next();
			if (classdefinition.isInnerClass()) {
				throw new CompilerError("BatchEnvironment, first is inner");
			}
			ClassDefinition classdefinition1 = classdefinition;
			while (iterator.hasNext()) {
				final ClassDefinition classdefinition2 = (ClassDefinition) iterator.next();
				if (!classdefinition2.isInnerClass()) {
					classdefinition1.addDependency(classdefinition2.getClassDeclaration());
					classdefinition2.addDependency(classdefinition1.getClassDeclaration());
					classdefinition1 = classdefinition2;
				}
			}
			if (classdefinition1 != classdefinition) {
				classdefinition1.addDependency(classdefinition.getClassDeclaration());
				classdefinition.addDependency(classdefinition1.getClassDeclaration());
			}
		}
		dtExit("parseFile: SOURCE PARSED " + classfile);
	}

	private BinaryClass loadFile(final ClassFile classfile) throws IOException {
		long l = System.currentTimeMillis();
		final InputStream inputstream = classfile.getInputStream();
		dtEnter("loadFile: LOADING CLASSFILE " + classfile);
		final BinaryClass binaryclass;
		try {
			final DataInputStream datainputstream = new DataInputStream(new BufferedInputStream(inputstream));
			binaryclass = BinaryClass.load(new Environment(this, classfile), datainputstream, 0);
		} catch (final ClassFormatError classformaterror) {
			this.error(0L, "class.format", classfile.getPath(), classformaterror.getMessage());
			dtExit("loadFile: CLASS FORMAT ERROR " + classfile);
			return null;
		} catch (final EOFException ignored) {
			this.error(0L, "truncated.class", classfile.getPath());
			return null;
		}
		inputstream.close();
		if (this.verbose()) {
			l = System.currentTimeMillis() - l;
			this.output(Main.getText("benv.loaded_in", classfile.getPath(), Long.toString(l)));
		}
		dtExit("loadFile: CLASSFILE LOADED " + classfile);
		return binaryclass;
	}

	private boolean needsCompilation(final Hashtable hashtable, final ClassDeclaration classdeclaration) {
		switch (classdeclaration.getStatus()) {
		case 0: // '\0'
			dtEnter("needsCompilation: UNDEFINED " + classdeclaration.getName());
			this.loadDefinition(classdeclaration);
			return this.needsCompilation(hashtable, classdeclaration);

		case 1: // '\001'
			dtEnter("needsCompilation: UNDECIDED " + classdeclaration.getName());
			if (hashtable.get(classdeclaration) == null) {
				hashtable.put(classdeclaration, classdeclaration);
				final BinaryClass binaryclass = (BinaryClass) classdeclaration.getClassDefinition();
				for (final Iterator it = binaryclass.getDependencies(); it.hasNext();) {
					final ClassDeclaration dependency = (ClassDeclaration) it.next();
					if (this.needsCompilation(hashtable, dependency)) {
						classdeclaration.setDefinition(binaryclass, 3);
						dtExit("needsCompilation: YES (source) " + classdeclaration.getName());
						return true;
					}
				}

			}
			dtExit("needsCompilation: NO (undecided) " + classdeclaration.getName());
			return false;

		case 2: // '\002'
			dtEnter("needsCompilation: BINARY " + classdeclaration.getName());
			dtExit("needsCompilation: NO (binary) " + classdeclaration.getName());
			return false;
		}
		dtExit("needsCompilation: YES " + classdeclaration.getName());
		return true;
	}

	public void loadDefinition(final ClassDeclaration classdeclaration) {
		dtEnter("loadDefinition: ENTER " + classdeclaration.getName() + ", status " + classdeclaration.getStatus());
		switch (classdeclaration.getStatus()) {
		case 0: // '\0'
			dtEvent("loadDefinition: STATUS IS UNDEFINED");
			final Identifier identifier = classdeclaration.getName();
			final Package package1 = this.getPackage(identifier.getQualifier());
			final ClassFile classfile1 = package1.getBinaryFile(identifier.getName());
			if (classfile1 == null) {
				classdeclaration.setDefinition(null, 3);
				dtExit("loadDefinition: MUST BE SOURCE (no binary) " + classdeclaration.getName());
				return;
			}
			ClassFile classfile2 = package1.getSourceFile(identifier.getName());
			if (classfile2 == null) {
				dtEvent("loadDefinition: NO SOURCE " + classdeclaration.getName());
				BinaryClass binaryclass;
				try {
					binaryclass = this.loadFile(classfile1);
				} catch (final IOException ignored) {
					classdeclaration.setDefinition(null, 7);
					this.error(0L, "io.exception", classfile1);
					dtExit("loadDefinition: IO EXCEPTION (binary)");
					return;
				}
				if (binaryclass != null && !binaryclass.getName().equals(identifier)) {
					this.error(0L, "wrong.class", classfile1.getPath(), classdeclaration, binaryclass);
					binaryclass = null;
					dtEvent("loadDefinition: WRONG CLASS (binary)");
				}
				if (binaryclass == null) {
					classdeclaration.setDefinition(null, 7);
					dtExit("loadDefinition: NOT FOUND (source or binary)");
					return;
				}
				if (binaryclass.getSource() != null) {
					classfile2 = new ClassFile(new File((String) binaryclass.getSource()));
					classfile2 = package1.getSourceFile(classfile2.getName());
					if (classfile2 != null && classfile2.exists()) {
						dtEvent("loadDefinition: FILENAME IN BINARY " + classfile2);
						if (classfile2.lastModified() > classfile1.lastModified()) {
							classdeclaration.setDefinition(binaryclass, 3);
							dtEvent("loadDefinition: SOURCE IS NEWER " + classfile2);
							binaryclass.loadNested(this);
							dtExit("loadDefinition: MUST BE SOURCE " + classdeclaration.getName());
							return;
						}
						if (this.dependencies()) {
							classdeclaration.setDefinition(binaryclass, 1);
							dtEvent("loadDefinition: UNDECIDED " + classdeclaration.getName());
						} else {
							classdeclaration.setDefinition(binaryclass, 2);
							dtEvent("loadDefinition: MUST BE BINARY " + classdeclaration.getName());
						}
						binaryclass.loadNested(this);
						dtExit("loadDefinition: EXIT " + classdeclaration.getName() + ", status " + classdeclaration.getStatus());
						return;
					}
				}
				classdeclaration.setDefinition(binaryclass, 2);
				dtEvent("loadDefinition: MUST BE BINARY (no source) " + classdeclaration.getName());
				binaryclass.loadNested(this);
				dtExit("loadDefinition: EXIT " + classdeclaration.getName() + ", status " + classdeclaration.getStatus());
				return;
			}
			BinaryClass binaryclass1 = null;
			try {
				if (classfile2.lastModified() > classfile1.lastModified()) {
					classdeclaration.setDefinition(null, 3);
					dtEvent("loadDefinition: MUST BE SOURCE (younger than binary) " + classdeclaration.getName());
					return;
				}
				binaryclass1 = this.loadFile(classfile1);
			} catch (final IOException ignored) {
				this.error(0L, "io.exception", classfile1);
				dtEvent("loadDefinition: IO EXCEPTION (binary)");
			}
			if (binaryclass1 != null && !binaryclass1.getName().equals(identifier)) {
				this.error(0L, "wrong.class", classfile1.getPath(), classdeclaration, binaryclass1);
				binaryclass1 = null;
				dtEvent("loadDefinition: WRONG CLASS (binary)");
			}
			if (binaryclass1 != null) {
				final Identifier identifier2 = binaryclass1.getName();
				if (identifier2.equals(classdeclaration.getName())) {
					if (this.dependencies()) {
						classdeclaration.setDefinition(binaryclass1, 1);
						dtEvent("loadDefinition: UNDECIDED " + identifier2);
					} else {
						classdeclaration.setDefinition(binaryclass1, 2);
						dtEvent("loadDefinition: MUST BE BINARY " + identifier2);
					}
				} else {
					classdeclaration.setDefinition(null, 7);
					dtEvent("loadDefinition: NOT FOUND (source or binary)");
					if (this.dependencies()) {
						this.getClassDeclaration(identifier2).setDefinition(binaryclass1, 1);
						dtEvent("loadDefinition: UNDECIDED " + identifier2);
					} else {
						this.getClassDeclaration(identifier2).setDefinition(binaryclass1, 2);
						dtEvent("loadDefinition: MUST BE BINARY " + identifier2);
					}
				}
			} else {
				classdeclaration.setDefinition(null, 7);
				dtEvent("loadDefinition: NOT FOUND (source or binary)");
			}
			if (binaryclass1 != null && binaryclass1 == classdeclaration.getClassDefinition()) {
				binaryclass1.loadNested(this);
			}
			dtExit("loadDefinition: EXIT " + classdeclaration.getName() + ", status " + classdeclaration.getStatus());
			return;

		case 1: // '\001'
			dtEvent("loadDefinition: STATUS IS UNDECIDED");
			final Hashtable hashtable = new Hashtable();
			if (!this.needsCompilation(hashtable, classdeclaration)) {
				for (final Iterator iterator = hashtable.keySet().iterator(); iterator.hasNext();) {
					final ClassDeclaration classdeclaration1 = (ClassDeclaration) iterator.next();
					if (classdeclaration1.getStatus() == 1) {
						classdeclaration1.setDefinition(classdeclaration1.getClassDefinition(), 2);
						dtEvent("loadDefinition: MUST BE BINARY " + classdeclaration1);
					}
				}

			}
			dtExit("loadDefinition: EXIT " + classdeclaration.getName() + ", status " + classdeclaration.getStatus());
			return;

		case 3: // '\003'
			dtEvent("loadDefinition: STATUS IS SOURCE");
			ClassFile classfile;
			final Package package2;
			if (classdeclaration.getClassDefinition() != null) {
				package2 = this.getPackage(classdeclaration.getName().getQualifier());
				classfile = package2.getSourceFile((String) classdeclaration.getClassDefinition().getSource());
				if (classfile == null) {
					final String s = (String) classdeclaration.getClassDefinition().getSource();
					classfile = new ClassFile(new File(s));
				}
			} else {
				final Identifier identifier1 = classdeclaration.getName();
				package2 = this.getPackage(identifier1.getQualifier());
				classfile = package2.getSourceFile(identifier1.getName());
				if (classfile == null) {
					classdeclaration.setDefinition(null, 7);
					dtExit("loadDefinition: SOURCE NOT FOUND " + classdeclaration.getName() + ", status " + classdeclaration.getStatus());
					return;
				}
			}
			try {
				this.parseFile(classfile);
			} catch (final FileNotFoundException ignored) {
				this.error(0L, "io.exception", classfile);
				dtEvent("loadDefinition: IO EXCEPTION (source)");
			}
			if (classdeclaration.getClassDefinition() == null || classdeclaration.getStatus() == 3) {
				this.error(0L, "wrong.source", classfile.getPath(), classdeclaration, package2);
				classdeclaration.setDefinition(null, 7);
				dtEvent("loadDefinition: WRONG CLASS (source) " + classdeclaration.getName());
			}
			dtExit("loadDefinition: EXIT " + classdeclaration.getName() + ", status " + classdeclaration.getStatus());
			return;

		case 2: // '\002'
		default:
			dtExit("loadDefinition: EXIT " + classdeclaration.getName() + ", status " + classdeclaration.getStatus());
		}
	}

	public ClassDefinition makeClassDefinition(final Environment environment, final long l, final IdentifierToken identifiertoken, final String s, final int i, IdentifierToken identifiertoken1, final IdentifierToken aidentifiertoken[], final ClassDefinition classdefinition) {
		final Identifier identifier = identifiertoken.getName();
		final long l1 = identifiertoken.getWhere();
		String s1 = null;
		ClassDefinition classdefinition1 = null;
		Identifier identifier2 = null;
		final Identifier identifier1;
		if (identifier.isQualified() || identifier.isInner()) {
			identifier1 = identifier;
		} else if ((i & 0x30000) != 0) {
			classdefinition1 = classdefinition.getTopClass();
			int j = 1;
			do {
				s1 = j + (identifier.equals(Constants.idNull) ? "" : "$" + identifier);
				if (classdefinition1.getLocalClass(s1) == null) {
					break;
				}
				j++;
			} while (true);
			final Identifier identifier3 = classdefinition1.getName();
			identifier1 = Identifier.lookupInner(identifier3, Identifier.lookup(s1));
			identifier2 = (i & 0x10000) != 0 ? Constants.idNull : identifier;
		} else if (classdefinition != null) {
			identifier1 = Identifier.lookupInner(classdefinition.getName(), identifier);
		} else {
			identifier1 = identifier;
		}
		ClassDeclaration classdeclaration = environment.getClassDeclaration(identifier1);
		if (classdeclaration.isDefined()) {
			environment.error(l1, "class.multidef", classdeclaration.getName(), classdeclaration.getClassDefinition().getSource());
			classdeclaration = new ClassDeclaration(identifier1);
		}
		if (identifiertoken1 == null && !identifier1.equals(Constants.idJavaLangObject)) {
			identifiertoken1 = new IdentifierToken(Constants.idJavaLangObject);
		}
		final SourceClass sourceclass = new SourceClass(environment, l, classdeclaration, s, i, identifiertoken1, aidentifiertoken, (SourceClass) classdefinition, identifier2);
		if (classdefinition != null) {
			classdefinition.addMember(environment, new SourceMember(sourceclass));
			if ((i & 0x30000) != 0) {
				classdefinition1.addLocalClass(sourceclass, s1);
			}
		}
		return sourceclass;
	}

	public MemberDefinition makeMemberDefinition(final Environment environment, final long l, final ClassDefinition classdefinition, final String s, final int i, final Type type, final Identifier identifier, final IdentifierToken aidentifiertoken[], final IdentifierToken aidentifiertoken1[], final Object obj) {
		dtEvent("makeMemberDefinition: " + identifier + " IN " + classdefinition);
		Vector vector = null;
		if (aidentifiertoken != null) {
			vector = new Vector(aidentifiertoken.length);
			for (int j = 0; j < aidentifiertoken.length; j++) {
				vector.addElement(aidentifiertoken[j]);
			}

		}
		final MemberDefinition sourcemember = new SourceMember(l, classdefinition, s, i, type, identifier, vector, aidentifiertoken1, (Node) obj);
		classdefinition.addMember(environment, sourcemember);
		return sourcemember;
	}

	public void shutdown() {
		try {
			if (this.sourcePath != null) {
				this.sourcePath.close();
			}
			if (this.binaryPath != null && this.binaryPath != this.sourcePath) {
				this.binaryPath.close();
			}
		} catch (final IOException ioexception) {
			this.output(Main.getText("benv.failed_to_close_class_path", ioexception.toString()));
		}
		this.sourcePath = null;
		this.binaryPath = null;
		super.shutdown();
	}

	private static String errorString(final String s, final Object obj, final Object obj1, final Object obj2) {
		final String s1 = s.startsWith("warn.") ? "javac.err." + s.substring(5) : "javac.err." + s;
		return Main.getText(s1, obj == null ? null : obj.toString(), obj1 == null ? null : obj1.toString(), obj2 == null ? null : obj2.toString());
	}

	private boolean insertError(final long l, final String s) {
		if (this.errors == null || this.errors.where > l) {
			final ErrorMessage errormessage = new ErrorMessage(l, s);
			errormessage.next = this.errors;
			this.errors = errormessage;
		} else {
			if (this.errors.where == l && this.errors.message.equals(s)) {
				return false;
			}
			ErrorMessage errormessage1;
			ErrorMessage errormessage2;
			for (errormessage1 = this.errors; (errormessage2 = errormessage1.next) != null && errormessage2.where < l; errormessage1 = errormessage2) {
			}
			ErrorMessage errormessage3;
			while ((errormessage3 = errormessage1.next) != null && errormessage3.where == l) {
				if (errormessage3.message.equals(s)) {
					return false;
				}
				errormessage1 = errormessage3;
			}
			final ErrorMessage errormessage4 = new ErrorMessage(l, s);
			errormessage4.next = errormessage1.next;
			errormessage1.next = errormessage4;
		}
		return true;
	}

	public void pushError(final String s, final int i, final String s1, final String s2, final String s3) {
		final int j = this.errorLimit + this.nwarnings;
		if (++this.errorsPushed >= j && this.errorLimit >= 0) {
			if (!this.hitErrorLimit) {
				this.hitErrorLimit = true;
				this.output(errorString("too.many.errors", new Integer(this.errorLimit), null, null));
			}
			return;
		}
		if (s.endsWith(".java")) {
			this.output(s + ':' + i + ": " + s1);
			this.output(s2);
			this.output(s3);
		} else {
			this.output(s + ": " + s1);
		}
	}

	public void flushErrors() {
		if (this.errors == null) {
			return;
		}
		boolean flag = false;
		char ac[] = null;
		int i = 0;
		try {
			final FileInputStream fileinputstream = new FileInputStream(this.errorFileName);
			ac = new char[fileinputstream.available()];
			final InputStreamReader inputstreamreader = this.getCharacterEncoding() == null ? new InputStreamReader(fileinputstream) : new InputStreamReader(fileinputstream, this.getCharacterEncoding());
			i = inputstreamreader.read(ac);
			inputstreamreader.close();
			flag = true;
		} catch (final IOException ignored) {
		}
		for (ErrorMessage errormessage = this.errors; errormessage != null; errormessage = errormessage.next) {
			final int j = (int) (errormessage.where >>> 32);
			int k = (int) (errormessage.where & 0xffffffffL);
			if (k > i) {
				k = i;
			}
			String s = "";
			String s1 = "";
			if (flag) {
				int l;
				for (l = k; l > 0 && ac[l - 1] != '\n' && ac[l - 1] != '\r'; l--) {
				}
				int i1;
				for (i1 = k; i1 < i && ac[i1] != '\n' && ac[i1] != '\r'; i1++) {
				}
				s = new String(ac, l, i1 - l);
				final char ac1[] = new char[k - l + 1];
				for (int j1 = l; j1 < k; j1++) {
					ac1[j1 - l] = ac[j1] != '\t' ? ' ' : '\t';
				}

				ac1[k - l] = '^';
				s1 = new String(ac1);
			}
			this.errorConsumer.pushError(this.errorFileName, j, errormessage.message, s, s1);
		}

		this.errors = null;
	}

	private void reportError(final Object obj, final long l, final String s, final String s1) {
		if (obj == null) {
			if (this.errorFileName != null) {
				this.flushErrors();
				this.errorFileName = null;
			}
			if (s.startsWith("warn.")) {
				if (this.warnings()) {
					this.nwarnings++;
					this.output(s1);
				}
				return;
			}
			this.output("error: " + s1);
			this.nerrors++;
			this.flags |= 0x10000;
		} else if (obj instanceof String) {
			final String s2 = (String) obj;
			if (!s2.equals(this.errorFileName)) {
				this.flushErrors();
				this.errorFileName = s2;
			}
			if (s.startsWith("warn.")) {
				if (s.indexOf("is.deprecated") >= 0) {
					if (!this.deprecationFiles.contains(obj)) {
						this.deprecationFiles.addElement(obj);
					}
					if (this.deprecation()) {
						if (this.insertError(l, s1)) {
							this.ndeprecations++;
						}
					} else {
						this.ndeprecations++;
					}
				} else if (this.warnings()) {
					if (this.insertError(l, s1)) {
						this.nwarnings++;
					}
				} else {
					this.nwarnings++;
				}
			} else if (this.insertError(l, s1)) {
				this.nerrors++;
				this.flags |= 0x10000;
			}
		} else if (obj instanceof ClassFile) {
			this.reportError(((ClassFile) obj).getPath(), l, s, s1);
		} else if (obj instanceof Identifier) {
			this.reportError(obj.toString(), l, s, s1);
		} else if (obj instanceof ClassDeclaration) {
			try {
				this.reportError(((ClassDeclaration) obj).getClassDefinition(this), l, s, s1);
			} catch (final ClassNotFound ignored) {
				this.reportError(((ClassDeclaration) obj).getName(), l, s, s1);
			}
		} else if (obj instanceof ClassDefinition) {
			final ClassDefinition classdefinition = (ClassDefinition) obj;
			if (!s.startsWith("warn.")) {
				classdefinition.setError();
			}
			this.reportError(classdefinition.getSource(), l, s, s1);
		} else if (obj instanceof MemberDefinition) {
			this.reportError(((MemberDefinition) obj).getClassDeclaration(), l, s, s1);
		} else {
			this.output(obj + ":error=" + s + ':' + s1);
		}
	}

	public void error(final Object obj, final long l, final String s, final Object obj1, final Object obj2, final Object obj3) {
		if (this.errorsPushed >= this.errorLimit + this.nwarnings) {
			return;
		}
		if (System.getProperty("javac.dump.stack") != null) {
			this.output("javac.err." + s + ": " + errorString(s, obj1, obj2, obj3));
			new Exception("Stack trace").printStackTrace(new PrintStream(this.out));
		}
		this.reportError(obj, l, s, errorString(s, obj1, obj2, obj3));
	}

	public void output(final String s) {
		final PrintStream printstream = this.out instanceof PrintStream ? (PrintStream) this.out : new PrintStream(this.out, true);
		printstream.println(s);
	}

	private final OutputStream out;
	ClassPath sourcePath;
	ClassPath binaryPath;
	private final Hashtable packages;
	private final Vector classesOrdered;
	private final Hashtable classes;
	public int flags;
	short majorVersion;
	short minorVersion;
	File covFile;
	int nerrors;
	int nwarnings;
	int ndeprecations;
	final Vector deprecationFiles;
	private final ErrorConsumer errorConsumer;
	private Set exemptPackages;
	private String errorFileName;
	private ErrorMessage errors;
	private int errorsPushed;
	private final int errorLimit;
	private boolean hitErrorLimit;
}
