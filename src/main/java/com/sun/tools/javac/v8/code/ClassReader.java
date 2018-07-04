package com.sun.tools.javac.v8.code;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.sun.tools.javac.v8.code.ClassFile.NameAndType;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.Completer;
import com.sun.tools.javac.v8.code.Symbol.CompletionFailure;
import com.sun.tools.javac.v8.code.Symbol.MethodSymbol;
import com.sun.tools.javac.v8.code.Symbol.PackageSymbol;
import com.sun.tools.javac.v8.code.Symbol.TypeSymbol;
import com.sun.tools.javac.v8.code.Symbol.VarSymbol;
import com.sun.tools.javac.v8.code.Type.ArrayType;
import com.sun.tools.javac.v8.code.Type.ClassType;
import com.sun.tools.javac.v8.code.Type.MethodType;
import com.sun.tools.javac.v8.util.Convert;
import com.sun.tools.javac.v8.util.FileEntry;
import com.sun.tools.javac.v8.util.FileEntry.Regular;
import com.sun.tools.javac.v8.util.FileEntry.Zipped;
import com.sun.tools.javac.v8.util.Hashtable;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.ListBuffer;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;
import com.sun.tools.javac.v8.util.Util;

public final class ClassReader implements Completer {
	public interface SourceCompleter {

		void complete(ClassSymbol classsymbol, String s, InputStream inputstream);
	}

	static class Archive {

		final ZipFile zdir;
		final List entries;

		Archive(final ZipFile zipfile, final List list1) {
			this.zdir = zipfile;
			this.entries = list1;
		}
	}

	public static class BadClassFile extends CompletionFailure {
		private static final long serialVersionUID = 678642410759219917L;

		public BadClassFile(final Symbol classsymbol, final String s, final String s1) {
			super(classsymbol, Log.getLocalizedString("bad.class.file.header", s, s1));
		}
	}

	public ClassReader(final Hashtable hashtable) {
		this.readAllOfClassFile = false;
		this.sourceCompleter = null;
		this.classes = Hashtable.make();
		this.packages = Hashtable.make();
		this.typevars = new Scope(null);
		this.currentClassFileName = null;
		this.currentOwner = null;
		this.buf = new byte[65520];
		this.archives = Hashtable.make();
		this.verbose = hashtable.get("-verbose") != null;
		this.checkClassFile = hashtable.get("-checkclassfile") != null;
		this.setClassPaths(hashtable);
		this.packages.put(Symbol.rootPackage.fullname, Symbol.rootPackage);
		Symbol.rootPackage.completer = this;
		this.packages.put(Symbol.emptyPackage.fullname, Symbol.emptyPackage);
		Symbol.emptyPackage.completer = this;
	}

	private void setClassPaths(final Hashtable hashtable) {
		String s = (String) hashtable.get("-classpath");
		if (s == null) {
			s = System.getProperty("env.class.path");
		}
		if (s == null) {
			s = ".";
		}
		this.classPath = terminate(s);
		String s1 = (String) hashtable.get("-bootclasspath");
		if (s1 == null) {
			s1 = System.getProperty("sun.boot.class.path");
		}
		if (s1 == null) {
			s1 = System.getProperty("java.class.path");
		}
		this.bootClassPath = s1 == null ? "" : terminate(s1);
		String s2 = (String) hashtable.get("-extdirs");
		if (s2 == null) {
			s2 = System.getProperty("java.ext.dirs");
		}
		if (s2 == null) {
			s2 = "";
		}
		final String s3 = terminate(s2);
		int i = 0;
		int k;
		for (final int j = s3.length(); i < j; i = k + 1) {
			k = s3.indexOf(pathSep, i);
			final String s5 = s3.substring(i, k);
			this.addArchives(s5);
		}

		final String s4 = (String) hashtable.get("-sourcepath");
		this.sourceClassPath = s4 != null ? terminate(s4) : null;
	}

	private void addArchives(final String s) {
		final String as[] = new File(s).list();
		for (int i = 0; as != null && i < as.length; i++) {
			if (as[i].endsWith(".jar")) {
				final String s1 = s.endsWith(File.separator) ? s : s + File.separator;
				this.bootClassPath = this.bootClassPath + s1 + as[i] + pathSep;
			}
		}

	}

	private static String terminate(final String s) {
		return s.endsWith(pathSep) ? s : s + pathSep;
	}

	private static void enterMember(final ClassSymbol classsymbol, final Symbol symbol) {
		if ((symbol.flags_field & 0x10000) == 0) {
			classsymbol.members_field.enter(symbol);
		}
	}

	private RuntimeException badClassFile(final String s) {
		return new BadClassFile(this.currentOwner.enclClass(), this.currentClassFileName, Log.getLocalizedString(s));
	}

	private RuntimeException badClassFile(final String s, final String s1) {
		return new BadClassFile(this.currentOwner.enclClass(), this.currentClassFileName, Log.getLocalizedString(s, s1));
	}

	private RuntimeException badClassFile(final String s, final String s1, final String s2) {
		return new BadClassFile(this.currentOwner.enclClass(), this.currentClassFileName, Log.getLocalizedString(s, s1, s2));
	}

	private RuntimeException badClassFile(final String s, final String s1, final String s2, final String s3, final String s4) {
		return new BadClassFile(this.currentOwner.enclClass(), this.currentClassFileName, Log.getLocalizedString(s, s1, s2, s3, s4));
	}

	private char nextChar() {
		return (char) (((this.buf[this.bp++] & 0xff) << 8) + (this.buf[this.bp++] & 0xff));
	}

	private int nextInt() {
		return ((this.buf[this.bp++] & 0xff) << 24) + ((this.buf[this.bp++] & 0xff) << 16) + ((this.buf[this.bp++] & 0xff) << 8) + (this.buf[this.bp++] & 0xff);
	}

	private char getChar(final int i) {
		return (char) (((this.buf[i] & 0xff) << 8) + (this.buf[i + 1] & 0xff));
	}

	private int getInt(final int i) {
		return ((this.buf[i] & 0xff) << 24) + ((this.buf[i + 1] & 0xff) << 16) + ((this.buf[i + 2] & 0xff) << 8) + (this.buf[i + 3] & 0xff);
	}

	private long getLong(final int i) {
		final DataInput datainputstream = new DataInputStream(new ByteArrayInputStream(this.buf, i, 8));
		try {
			return datainputstream.readLong();
		} catch (final IOException ignored) {
			throw new InternalError();
		}
	}

	private float getFloat(final int i) {
		final DataInput datainputstream = new DataInputStream(new ByteArrayInputStream(this.buf, i, 4));
		try {
			return datainputstream.readFloat();
		} catch (final IOException ignored) {
			throw new InternalError("get");
		}
	}

	private double getDouble(final int i) {
		final DataInput datainputstream = new DataInputStream(new ByteArrayInputStream(this.buf, i, 8));
		try {
			return datainputstream.readDouble();
		} catch (final IOException ignored) {
			throw new InternalError("get");
		}
	}

	private void indexPool() {
		this.poolIdx = new int[this.nextChar()];
		this.poolObj = new Object[this.poolIdx.length];
		for (int i = 1; i < this.poolIdx.length;) {
			this.poolIdx[i++] = this.bp;
			final byte byte0 = this.buf[this.bp++];
			switch (byte0) {
			case 1: // '\001'
			case 2: // '\002'
				final char c = this.nextChar();
				this.bp += c;
				break;

			case 7: // '\007'
			case 8: // '\b'
				this.bp += 2;
				break;

			case 3: // '\003'
			case 4: // '\004'
			case 9: // '\t'
			case 10: // '\n'
			case 11: // '\013'
			case 12: // '\f'
				this.bp += 4;
				break;

			case 5: // '\005'
			case 6: // '\006'
				this.bp += 8;
				i++;
				break;

			default:
				throw this.badClassFile("bad.const.pool.tag.at", Byte.toString(byte0), Integer.toString(this.bp - 1));
			}
		}

	}

	private Object readPool(final int i) {
		final Object obj = this.poolObj[i];
		if (obj != null) {
			return obj;
		}
		final int j = this.poolIdx[i];
		if (j == 0) {
			return null;
		}
		final byte byte0 = this.buf[j];
		switch (byte0) {
		case 1: // '\001'
			this.poolObj[i] = Name.fromUtf(this.buf, j + 3, this.getChar(j + 1));
			break;

		case 2: // '\002'
			throw this.badClassFile("unicode.str.not.supported");

		case 7: // '\007'
			this.poolObj[i] = this.readClassOrType(this.getChar(j + 1));
			break;

		case 8: // '\b'
			this.poolObj[i] = this.readName(this.getChar(j + 1)).toString();
			break;

		case 9: // '\t'
			final ClassSymbol classsymbol = this.readClassSymbol(this.getChar(j + 1));
			final NameAndType nameandtype = (NameAndType) this.readPool(this.getChar(j + 3));
			this.poolObj[i] = new VarSymbol(0, nameandtype.name, nameandtype.type, classsymbol);
			break;

		case 10: // '\n'
		case 11: // '\013'
			final ClassSymbol classsymbol1 = this.readClassSymbol(this.getChar(j + 1));
			final NameAndType nameandtype1 = (NameAndType) this.readPool(this.getChar(j + 3));
			this.poolObj[i] = new MethodSymbol(0, nameandtype1.name, nameandtype1.type, classsymbol1);
			break;

		case 12: // '\f'
			this.poolObj[i] = new NameAndType(this.readName(this.getChar(j + 1)), this.readType(this.getChar(j + 3)));
			break;

		case 3: // '\003'
			this.poolObj[i] = new Integer(this.getInt(j + 1));
			break;

		case 4: // '\004'
			this.poolObj[i] = new Float(this.getFloat(j + 1));
			break;

		case 5: // '\005'
			this.poolObj[i] = new Long(this.getLong(j + 1));
			break;

		case 6: // '\006'
			this.poolObj[i] = new Double(this.getDouble(j + 1));
			break;

		default:
			throw this.badClassFile("bad.const.pool.tag", Byte.toString(byte0));
		}
		return this.poolObj[i];
	}

	private Type readType(final int i) {
		final int j = this.poolIdx[i];
		return this.sigToType(this.buf, j + 3, this.getChar(j + 1));
	}

	private Object readClassOrType(final int i) {
		final int j = this.poolIdx[i];
		final char c = this.getChar(j + 1);
		final int k = j + 3;
		return this.buf[k] != 91 && this.buf[k + c - 1] != 59 ? (Object) this.enterClass(Name.fromUtf(ClassFile.internalize(this.buf, k, c))) : (Object) this.sigToType(this.buf, k, c);
	}

	private ClassSymbol readClassSymbol(final int i) {
		return (ClassSymbol) this.readPool(i);
	}

	private Name readName(final int i) {
		return (Name) this.readPool(i);
	}

	private Type sigToType(final byte abyte0[], final int i, final int j) {
		this.signature = abyte0;
		this.sigp = i;
		this.siglimit = i + j;
		return this.sigToType();
	}

	private Type sigToType() {
		switch ((char) this.signature[this.sigp]) {
		case 66: // 'B'
			this.sigp++;
			return Type.byteType;

		case 67: // 'C'
			this.sigp++;
			return Type.charType;

		case 68: // 'D'
			this.sigp++;
			return Type.doubleType;

		case 70: // 'F'
			this.sigp++;
			return Type.floatType;

		case 73: // 'I'
			this.sigp++;
			return Type.intType;

		case 74: // 'J'
			this.sigp++;
			return Type.longType;

		case 76: // 'L'
			Type type;
			for (type = this.classSigToType(Type.noType); this.sigp < this.siglimit && this.signature[this.sigp] == 46; type = this.classSigToType(type)) {
				this.sigp++;
			}

			return type;

		case 83: // 'S'
			this.sigp++;
			return Type.shortType;

		case 86: // 'V'
			this.sigp++;
			return Type.voidType;

		case 90: // 'Z'
			this.sigp++;
			return Type.booleanType;

		case 91: // '['
			for (this.sigp++; this.signature[this.sigp] >= 48 && this.signature[this.sigp] <= 57; this.sigp++) {
			}
			return new ArrayType(this.sigToType());

		case 40: // '('
			final List list1 = this.sigToTypes(')');
			final Type type1 = this.sigToType();
			return new MethodType(list1, type1, ClassSymbol.emptyList);
		}
		throw this.badClassFile("bad.signature", Convert.utf2string(this.signature, this.sigp, 10));
	}

	private Type classSigToType(final Type type) {
		if (this.signature[this.sigp] == 76) {
			this.sigp++;
			final int i = this.sigp;
			for (; this.signature[this.sigp] != 59 && this.signature[this.sigp] != 60; this.sigp++) {
			}
			ClassType classtype = (ClassType) ((Symbol) this.enterClass(Name.fromUtf(ClassFile.internalize(this.signature, i, this.sigp - i)))).type;
			if (this.signature[this.sigp] == 60) {
				classtype = new ClassType(classtype.outer_field, this.sigToTypes('>'), ((Type) classtype).tsym);
			}
			if (type.isParameterized()) {
				classtype.outer_field = type;
			}
			this.sigp++;
			return classtype;
		}
		throw this.badClassFile("bad.class.signature", Convert.utf2string(this.signature, this.sigp, 10));
	}

	private List sigToTypes(final char c) {
		this.sigp++;
		final ListBuffer listbuffer = new ListBuffer();
		while (this.signature[this.sigp] != c) {
			listbuffer.append(this.sigToType());
		}
		this.sigp++;
		return listbuffer.toList();
	}

	private void unrecogized(final Name name) {
		if (this.checkClassFile) {
			printCCF("ccf.unrecognized.attribute", name.toJava());
		}
	}

	private void readMemberAttr(final Symbol symbol, final Name name, final int i) {
		if (name == Names.ConstantValue) {
			final Object obj = this.readPool(this.nextChar());
			if ((symbol.flags() & 0x10) != 0) {
				((VarSymbol) symbol).constValue = obj;
			}
		} else if (name == Names.Code) {
			if (this.readAllOfClassFile) {
				((MethodSymbol) symbol).code = null;
			} else {
				this.bp += i;
			}
		} else if (name == Names.Exceptions) {
			final char c = this.nextChar();
			final ListBuffer listbuffer = new ListBuffer();
			for (int j = 0; j < c; j++) {
				listbuffer.append(this.readClassSymbol(this.nextChar()));
			}

			symbol.type.asMethodType().thrown = listbuffer.toList();
		} else if (name == Names.Synthetic) {
			symbol.flags_field |= 0x10000;
		} else if (name == Names.Deprecated) {
			symbol.flags_field |= 0x20000;
		} else {
			this.unrecogized(name);
			this.bp += i;
		}
	}

	private void readMemberAttrs(final Symbol symbol) {
		final char c = this.nextChar();
		for (int i = 0; i < c; i++) {
			final Name name = this.readName(this.nextChar());
			final int j = this.nextInt();
			this.readMemberAttr(symbol, name, j);
		}

	}

	private void readClassAttr(final ClassSymbol classsymbol, final Name name, final int i) {
		if (name == Names.SourceFile) {
			classsymbol.sourcefile = this.readName(this.nextChar());
		} else if (name == Names.InnerClasses) {
			this.readInnerClasses(classsymbol);
		} else {
			this.readMemberAttr(classsymbol, name, i);
		}
	}

	private void readClassAttrs(final ClassSymbol classsymbol) {
		final char c = this.nextChar();
		for (int i = 0; i < c; i++) {
			final Name name = this.readName(this.nextChar());
			final int j = this.nextInt();
			this.readClassAttr(classsymbol, name, j);
		}

	}

	private Symbol readField() {
		final char c = this.nextChar();
		final Name name = this.readName(this.nextChar());
		final Type type = this.readType(this.nextChar());
		final Symbol varsymbol = new VarSymbol(c, name, type, this.currentOwner);
		this.readMemberAttrs(varsymbol);
		return varsymbol;
	}

	private Symbol readMethod() {
		final char c = this.nextChar();
		final Name name = this.readName(this.nextChar());
		Object obj = this.readType(this.nextChar());
		if (name == Names.init && this.currentOwner.hasOuterInstance()) {
			obj = new MethodType(((Type) obj).argtypes().tail, ((Type) obj).restype(), ((Type) obj).thrown());
		}
		final Symbol methodsymbol = new MethodSymbol(c, name, (Type) obj, this.currentOwner);
		final Symbol symbol = this.currentOwner;
		this.currentOwner = methodsymbol;
		this.readMemberAttrs(methodsymbol);
		this.currentOwner = symbol;
		return methodsymbol;
	}

	private void skipMember() {
		this.bp += 6;
		final char c = this.nextChar();
		for (int i = 0; i < c; i++) {
			this.bp += 2;
			final int j = this.nextInt();
			this.bp += j;
		}

	}

	private void enterTypevars(final Type type) {
		if (type.outer().tag == 10) {
			this.enterTypevars(type.outer());
		}
		for (List list1 = type.typarams(); list1.nonEmpty(); list1 = list1.tail) {
			this.typevars.enter(((Type) list1.head).tsym);
		}

	}

	private void readClass(final ClassSymbol classsymbol) {
		final ClassType classtype = (ClassType) ((Symbol) classsymbol).type;
		classsymbol.members_field = new Scope(classsymbol);
		this.typevars = this.typevars.dup();
		if (classtype.outer().tag == 10) {
			this.enterTypevars(classtype.outer());
		}
		final int i = this.nextChar();
		if (((Symbol) classsymbol).owner.kind == 1) {
			classsymbol.flags_field = i;
		}
		final ClassSymbol classsymbol1 = this.readClassSymbol(this.nextChar());
		if (classsymbol != classsymbol1) {
			throw this.badClassFile("class.file.wrong.class", classsymbol1.flatname.toJava());
		}
		final int j = this.bp;
		this.nextChar();
		final char c = this.nextChar();
		this.bp += c * 2;
		final char c1 = this.nextChar();
		for (int k = 0; k < c1; k++) {
			this.skipMember();
		}

		final char c2 = this.nextChar();
		for (int l = 0; l < c2; l++) {
			this.skipMember();
		}

		this.readClassAttrs(classsymbol);
		if (this.readAllOfClassFile) {
			for (int i1 = 1; i1 < this.poolObj.length; i1++) {
				this.readPool(i1);
			}

			classsymbol.pool = new Pool(this.poolObj.length, this.poolObj);
		}
		this.bp = j;
		char c3 = this.nextChar();
		classtype.supertype_field = c3 != 0 ? ((Symbol) this.readClassSymbol(c3)).type : Type.noType;
		c3 = this.nextChar();
		final ListBuffer listbuffer = new ListBuffer();
		for (int j1 = 0; j1 < c3; j1++) {
			listbuffer.append(((Symbol) this.readClassSymbol(this.nextChar())).type);
		}

		classtype.interfaces_field = listbuffer.toList();
		Util.assertTrue(c1 == this.nextChar());
		for (int k1 = 0; k1 < c1; k1++) {
			enterMember(classsymbol, this.readField());
		}

		Util.assertTrue(c2 == this.nextChar());
		for (int l1 = 0; l1 < c2; l1++) {
			enterMember(classsymbol, this.readMethod());
		}

		this.typevars = this.typevars.leave();
	}

	private void readInnerClasses(final ClassSymbol classsymbol) {
		final char c = this.nextChar();
		for (int i = 0; i < c; i++) {
			this.nextChar();
			final ClassSymbol classsymbol1 = this.readClassSymbol(this.nextChar());
			Name name = this.readName(this.nextChar());
			if (name == null) {
				name = Names.empty;
			}
			final int j = this.nextChar();
			if (classsymbol1 != null) {
				final ClassSymbol classsymbol2 = this.enterClass(name, classsymbol1);
				if ((j & 8) == 0) {
					((ClassType) ((Symbol) classsymbol2).type).outer_field = ((Symbol) classsymbol1).type;
				}
				if (classsymbol == classsymbol1) {
					classsymbol2.flags_field = j;
					enterMember(classsymbol, classsymbol2);
				}
			}
		}

	}

	private void readClassFile(final ClassSymbol classsymbol) {
		final int i = this.nextInt();
		if (i != 0xcafebabe) {
			throw this.badClassFile("illegal.start.of.class.file");
		}
		final char c = this.nextChar();
		final char c1 = this.nextChar();
		if (c1 > '/' || c1 * 1000 + c < 45003) {
			throw this.badClassFile("wrong.version", Integer.toString(c1), Integer.toString(c), Integer.toString(47), Integer.toString(0));
		}
		if (this.checkClassFile && c1 == '/' && c > 0) {
			printCCF("found.later.version", Integer.toString(c));
		}
		this.indexPool();
		this.readClass(classsymbol);
	}

	private static boolean isZip(final String s) {
		return new File(s).isFile();
	}

	private Archive openArchive(final String s) throws IOException {
		Archive archive = (Archive) this.archives.get(s);
		if (archive == null) {
			final ZipFile zipfile = new ZipFile(s);
			final ListBuffer listbuffer = new ListBuffer();
			for (final Enumeration enumeration = zipfile.entries(); enumeration.hasMoreElements(); listbuffer.append(enumeration.nextElement())) {
			}
			archive = new Archive(zipfile, listbuffer.toList());
			this.archives.put(s, archive);
		}
		return archive;
	}

	public void close() {
		for (List keys = this.archives.keySet(); keys.nonEmpty(); keys = keys.tail) {
			final Archive archive = (Archive) this.archives.remove(keys.head);
			try {
				archive.zdir.close();
			} catch (final IOException ignored) {
			}
		}

	}

	public ClassSymbol defineClass(final Name name, final Symbol symbol) {
		final ClassSymbol classsymbol = new ClassSymbol(0, name, symbol);
		classsymbol.completer = this;
		return classsymbol;
	}

	public ClassSymbol enterClass(final Name name, final Symbol typesymbol) {
		final Name name1 = TypeSymbol.formFlatName(name, typesymbol);
		ClassSymbol classsymbol = (ClassSymbol) this.classes.get(name1);
		if (classsymbol == null) {
			classsymbol = this.defineClass(name, typesymbol);
			this.classes.put(name1, classsymbol);
		} else if ((((Symbol) classsymbol).name != name || ((Symbol) classsymbol).owner != typesymbol) && typesymbol.kind == 2) {
			classsymbol.name = name;
			classsymbol.owner = typesymbol;
			classsymbol.fullname = TypeSymbol.formFullName(name, typesymbol);
		}
		return classsymbol;
	}

	public ClassSymbol enterClass(final Name name) {
		ClassSymbol classsymbol = (ClassSymbol) this.classes.get(name);
		if (classsymbol == null) {
			Name name1 = Convert.packagePart(name);
			if (name1 == Names.empty) {
				name1 = Names.emptyPackage;
			}
			classsymbol = this.defineClass(Convert.shortName(name), this.enterPackage(name1));
			this.classes.put(name, classsymbol);
		}
		return classsymbol;
	}

	public void complete(final Symbol symbol) {
		if (symbol.kind == 2) {
			final ClassSymbol classsymbol = (ClassSymbol) symbol;
			((Symbol) classsymbol).owner.complete();
			this.fillIn(classsymbol);
		} else if (symbol.kind == 1) {
			final PackageSymbol packagesymbol = (PackageSymbol) symbol;
			this.fillIn(packagesymbol);
		}
	}

	private void fillIn(final ClassSymbol classsymbol) {
		this.currentOwner = classsymbol;
		final FileEntry fileentry = classsymbol.classfile;
		classsymbol.members_field = Scope.errScope;
		if (fileentry != null) {
			try {
				final InputStream inputstream = fileentry.open();
				this.currentClassFileName = fileentry.getPath();
				if (this.verbose) {
					printVerbose("loading", this.currentClassFileName);
				}
				if (fileentry.getName().endsWith(Names.dot_class)) {
					final int i = (int) fileentry.length();
					if (this.buf.length < i) {
						this.buf = new byte[i];
					}
					for (int j = 0; j < i; j += inputstream.read(this.buf, j, i - j)) {
					}
					inputstream.close();
					this.bp = 0;
					this.readClassFile(classsymbol);
				} else {
					this.sourceCompleter.complete(classsymbol, this.currentClassFileName, inputstream);
				}
				return;
			} catch (final IOException ioexception) {
				throw this.badClassFile("unable.to.access.file", ioexception.getMessage());
			}
		}
		final String s = ClassFile.externalizeFileName(classsymbol.flatname);
		throw new CompletionFailure(classsymbol, Log.getLocalizedString("dot.class.not.found", s));
	}

	public ClassSymbol loadClass(final Name name) {
		final boolean flag = this.classes.get(name) == null;
		final ClassSymbol classsymbol = this.enterClass(name);
		if (classsymbol.members_field == null && ((Symbol) classsymbol).completer != null) {
			try {
				classsymbol.complete();
			} catch (final CompletionFailure completionfailure) {
				if (flag) {
					this.classes.remove(name);
				}
				throw completionfailure;
			}
		}
		return classsymbol;
	}

	public PackageSymbol enterPackage(final Name name) {
		PackageSymbol packagesymbol = (PackageSymbol) this.packages.get(name);
		if (packagesymbol == null) {
			packagesymbol = new PackageSymbol(Convert.shortName(name), this.enterPackage(Convert.packagePart(name)));
			packagesymbol.completer = this;
			this.packages.put(name, packagesymbol);
		}
		return packagesymbol;
	}

	private void includeClassFile(final PackageSymbol packagesymbol, final FileEntry fileentry) {
		Symbol obj = packagesymbol;
		do {
			obj.flags_field |= 0x800000;
			obj = obj.owner;
		} while (obj != null && obj.kind == 1);
		final Name name = fileentry.getName();
		final int i;
		final byte byte0;
		if (name.endsWith(Names.dot_class)) {
			i = 0x2000000;
			byte0 = 6;
		} else {
			i = 0x4000000;
			byte0 = 5;
		}
		final Name name1 = name.subName(0, name.len - byte0);
		ClassSymbol classsymbol = (ClassSymbol) packagesymbol.members_field.lookup(name1).sym;
		if (classsymbol == null) {
			classsymbol = this.enterClass(name1, packagesymbol);
			classsymbol.classfile = fileentry;
			packagesymbol.members_field.enter(classsymbol);
		} else if (classsymbol.classfile != null && (((Symbol) classsymbol).flags_field & i) == 0 && (((Symbol) classsymbol).flags_field & 0x6000000) != 0) {
			final long l = fileentry.lastModified();
			final long l1 = classsymbol.classfile.lastModified();
			if (l >= 0L && l1 >= 0L && l > l1) {
				classsymbol.classfile = fileentry;
			}
		}
		classsymbol.flags_field |= i;
	}

	private void list(final String s, String s1, final String as[], final PackageSymbol packagesymbol) {
		try {
			if (isZip(s)) {
				final Archive archive = this.openArchive(s);
				if (s1.length() != 0) {
					s1 = s1.replace('\\', '/');
					if (!(s1.length() > 0 && s1.charAt(s1.length() - 1) == '/')) {
						s1 += "/";
					}
				}
				final int i = s1.length();
				for (List list1 = archive.entries; list1.nonEmpty(); list1 = list1.tail) {
					final ZipEntry zipentry = (ZipEntry) list1.head;
					final String s3 = zipentry.getName();
					if (s3.startsWith(s1) && endsWith(s3, as)) {
						final String s4 = s3.substring(i);
						if (s4.length() > 0 && s4.indexOf('/') < 0) {
							this.includeClassFile(packagesymbol, new Zipped(s4, archive.zdir, zipentry));
						}
					}
				}

			} else {
				final File file = s1.length() == 0 ? new File(s) : new File(s, s1);
				final String as1[] = file.list();
				if (as1 != null && caseMapCheck(file, s1)) {
					for (int j = 0; j < as1.length; j++) {
						final String s2 = as1[j];
						if (endsWith(s2, as)) {
							this.includeClassFile(packagesymbol, new Regular(s2, new File(file, s2)));
						}
					}

				}
			}
		} catch (final IOException ignored) {
		}
	}

	private static boolean endsWith(final String s, final String as[]) {
		for (int i = 0; i < as.length; i++) {
			if (s.endsWith(as[i])) {
				return true;
			}
		}

		return false;
	}

	private static boolean caseMapCheck(final File file, final String s) throws IOException {
		if (fileSystemIsCaseSensitive) {
			return true;
		}
		final String s1 = file.getCanonicalPath();
		final char ac[] = s1.toCharArray();
		final char ac1[] = s.toCharArray();
		int i = ac.length - 1;
		int j;
		for (j = ac1.length - 1; i >= 0 && j >= 0;) {
			while (i >= 0 && ac[i] == File.separatorChar) {
				i--;
			}
			for (; j >= 0 && ac1[j] == File.separatorChar; j--) {
			}
			if (i >= 0 && j >= 0) {
				if (ac[i] != ac1[j]) {
					return false;
				}
				i--;
				j--;
			}
		}

		return j < 0;
	}

	private void listAll(final String s, final String s1, final String as[], final PackageSymbol packagesymbol) {
		int i = 0;
		int k;
		for (final int j = s.length(); i < j; i = k + 1) {
			k = s.indexOf(pathSep, i);
			final String s2 = s.substring(i, k);
			this.list(s2, s1, as, packagesymbol);
		}

	}

	private void fillIn(final PackageSymbol packagesymbol) {
		if (packagesymbol.members_field == null) {
			packagesymbol.members_field = new Scope(packagesymbol);
		}
		Name name = packagesymbol.fullname;
		if (name == Names.emptyPackage) {
			name = Names.empty;
		}
		final String s = ClassFile.externalizeFileName(name);
		this.listAll(this.bootClassPath, s, classOnly, packagesymbol);
		if (this.sourceCompleter != null && this.sourceClassPath == null) {
			this.listAll(this.classPath, s, classOrJava, packagesymbol);
		} else {
			this.listAll(this.classPath, s, classOnly, packagesymbol);
			if (this.sourceCompleter != null) {
				this.listAll(this.sourceClassPath, s, javaOnly, packagesymbol);
			}
		}
	}

	private static void printVerbose(final String s, final String s1) {
		System.err.println(Log.getLocalizedString("verbose." + s, s1));
	}

	private static void printCCF(final String s, final String s1) {
		System.out.println(Log.getLocalizedString("verbose." + s, s1));
	}

	private static final String pathSep = System.getProperty("path.separator");
	private final boolean verbose;
	private final boolean checkClassFile;
	private final boolean readAllOfClassFile;
	private String classPath;
	private String bootClassPath;
	private String sourceClassPath;
	public SourceCompleter sourceCompleter;
	public Hashtable classes;
	private final Hashtable packages;
	private Scope typevars;
	private String currentClassFileName;
	private Symbol currentOwner;
	private byte[] buf;
	private int bp;
	private Object[] poolObj;
	private int[] poolIdx;
	private byte[] signature;
	private int sigp;
	private int siglimit;
	private final Hashtable archives;
	private static final boolean fileSystemIsCaseSensitive;
	private static final String[] classOnly = { ".class" };
	private static final String[] javaOnly = { ".java" };
	private static final String[] classOrJava = { ".class", ".java" };

	static {
		fileSystemIsCaseSensitive = File.separatorChar == '/';
	}
}
