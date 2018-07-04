package com.sun.tools.javac.v8.code;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.sun.tools.javac.v8.code.ClassFile.NameAndType;
import com.sun.tools.javac.v8.code.Scope.Entry;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.MethodSymbol;
import com.sun.tools.javac.v8.code.Symbol.VarSymbol;
import com.sun.tools.javac.v8.code.Type.ArrayType;
import com.sun.tools.javac.v8.code.Type.MethodType;
import com.sun.tools.javac.v8.util.ByteBuffer;
import com.sun.tools.javac.v8.util.Convert;
import com.sun.tools.javac.v8.util.Hashtable;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.ListBuffer;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;
import com.sun.tools.javac.v8.util.Set;
import com.sun.tools.javac.v8.util.Util;

public final class ClassWriter {
	public static class StringOverflow extends Exception {
		private static final long serialVersionUID = 6459748958306622082L;

		public final String value;

		StringOverflow(final String s) {
			this.value = s;
		}
	}

	public static class PoolOverflow extends Exception {
		private static final long serialVersionUID = -5013046571533301379L;

	}

	public ClassWriter(final Hashtable hashtable) {
		this.outDir = null;
		this.databuf = new ByteBuffer(65520);
		this.poolbuf = new ByteBuffer(0x1fff0);
		this.sigbuf = new ByteBuffer();
		this.scramble = hashtable.get("-scramble") != null;
		this.scrambleAll = hashtable.get("-scrambleAll") != null;
		this.retrofit = hashtable.get("-retrofit") != null;
		final String target = (String) hashtable.get("-target");
		this.majorVersion = ClassFile.classMajorVersion(target);
		this.minorVersion = ClassFile.classMinorVersion(target);
		final String s = (String) hashtable.get("-g:");
		this.emitSourceFile = s == null || Util.contains(s, "source", ',');
		final String s1 = (String) hashtable.get("-d");
		if (s1 != null) {
			this.outDir = new File(s1);
		}
	}

	private static String flagNames(final int i) {
		final StringBuffer stringbuffer = new StringBuffer();
		int j = 0;
		for (int k = i & 0xfff; k != 0;) {
			if ((k & 1) != 0) {
				stringbuffer.append(' ').append(flagName[j]);
			}
			k >>= 1;
			j++;
		}

		return stringbuffer.toString();
	}

	private static void putChar(final ByteBuffer bytebuffer, final int i, final int j) {
		bytebuffer.elems[i] = (byte) (j >> 8 & 0xff);
		bytebuffer.elems[i + 1] = (byte) (j & 0xff);
	}

	private static void putInt(final ByteBuffer bytebuffer, final int i, final int j) {
		bytebuffer.elems[i] = (byte) (j >> 24 & 0xff);
		bytebuffer.elems[i + 1] = (byte) (j >> 16 & 0xff);
		bytebuffer.elems[i + 2] = (byte) (j >> 8 & 0xff);
		bytebuffer.elems[i + 3] = (byte) (j & 0xff);
	}

	private void assembleSig(final Type type) {
		switch (type.tag) {
		case 1: // '\001'
			this.sigbuf.appendByte(66);
			break;

		case 3: // '\003'
			this.sigbuf.appendByte(83);
			break;

		case 2: // '\002'
			this.sigbuf.appendByte(67);
			break;

		case 4: // '\004'
			this.sigbuf.appendByte(73);
			break;

		case 5: // '\005'
			this.sigbuf.appendByte(74);
			break;

		case 6: // '\006'
			this.sigbuf.appendByte(70);
			break;

		case 7: // '\007'
			this.sigbuf.appendByte(68);
			break;

		case 8: // '\b'
			this.sigbuf.appendByte(90);
			break;

		case 9: // '\t'
			this.sigbuf.appendByte(86);
			break;

		case 10: // '\n'
			final ClassSymbol classsymbol = (ClassSymbol) type.tsym;
			this.enterInner(classsymbol);
			if (type.outer().allparams().nonEmpty()) {
				this.assembleSig(type.outer());
				this.sigbuf.appendByte(46);
			}
			this.sigbuf.appendByte(76);
			this.sigbuf.appendBytes(ClassFile.externalize(classsymbol.flatname));
			this.sigbuf.appendByte(59);
			break;

		case 11: // '\013'
			final ArrayType arraytype = (ArrayType) type;
			this.sigbuf.appendByte(91);
			this.assembleSig(arraytype.elemtype);
			break;

		case 12: // '\f'
			final MethodType methodtype = (MethodType) type;
			this.sigbuf.appendByte(40);
			this.assembleSig(methodtype.argtypes);
			this.sigbuf.appendByte(41);
			this.assembleSig(methodtype.restype);
			break;

		default:
			throw new InternalError("typeSig" + type.tag);
		}
	}

	private void assembleSig(final List list) {
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			this.assembleSig((Type) list1.head);
		}

	}

	private Name typeSig(final Type type) {
		Util.assertTrue(this.sigbuf.length == 0);
		this.assembleSig(type);
		final Name name = this.sigbuf.toName();
		this.sigbuf.reset();
		return name;
	}

	public Name xClassName(final Type type) {
		if (type.tag == 10) {
			return Name.fromUtf(ClassFile.externalize(type.tsym.flatName()));
		}
		if (type.tag == 11) {
			return this.typeSig(type.erasure());
		}
		throw new InternalError("xClassName");
	}

	private void writePool(final Pool pool1) throws PoolOverflow, StringOverflow {
		final int i = this.poolbuf.length;
		this.poolbuf.appendChar(0);
		for (int j = 1; j < pool1.pp; j++) {
			final Object obj = pool1.pool[j];
			Util.assertTrue(obj != null);
			if (obj instanceof Name) {
				this.poolbuf.appendByte(1);
				final byte abyte0[] = ((Name) obj).toUtf();
				this.poolbuf.appendChar(abyte0.length);
				this.poolbuf.appendBytes(abyte0, 0, abyte0.length);
				if (abyte0.length > Pool.MAX_STRING_LENGTH) {
					throw new StringOverflow(obj.toString());
				}
			} else if (obj instanceof ClassSymbol) {
				final ClassSymbol classsymbol = (ClassSymbol) obj;
				if (((Symbol) classsymbol).owner.kind == 2) {
					pool1.put(((Symbol) classsymbol).owner);
				}
				this.poolbuf.appendByte(7);
				this.poolbuf.appendChar(pool1.put(Name.fromUtf(ClassFile.externalize(classsymbol.flatname))));
				this.enterInner(classsymbol);
			} else if (obj instanceof MethodSymbol) {
				final Symbol methodsymbol = (Symbol) obj;
				this.poolbuf.appendByte((methodsymbol.owner.flags() & 0x200) == 0 ? 10 : 11);
				this.poolbuf.appendChar(pool1.put(methodsymbol.owner));
				this.poolbuf.appendChar(pool1.put(this.nameType(methodsymbol)));
			} else if (obj instanceof VarSymbol) {
				final Symbol varsymbol = (Symbol) obj;
				this.poolbuf.appendByte(9);
				this.poolbuf.appendChar(pool1.put(varsymbol.owner));
				this.poolbuf.appendChar(pool1.put(this.nameType(varsymbol)));
			} else if (obj instanceof NameAndType) {
				final NameAndType nameandtype = (NameAndType) obj;
				this.poolbuf.appendByte(12);
				this.poolbuf.appendChar(pool1.put(nameandtype.name));
				this.poolbuf.appendChar(pool1.put(this.typeSig(nameandtype.type)));
			} else if (obj instanceof Integer) {
				this.poolbuf.appendByte(3);
				this.poolbuf.appendInt(((Number) obj).intValue());
			} else if (obj instanceof Long) {
				this.poolbuf.appendByte(5);
				this.poolbuf.appendLong(((Number) obj).longValue());
				j++;
			} else if (obj instanceof Float) {
				this.poolbuf.appendByte(4);
				this.poolbuf.appendFloat(((Number) obj).floatValue());
			} else if (obj instanceof Double) {
				this.poolbuf.appendByte(6);
				this.poolbuf.appendDouble(((Number) obj).doubleValue());
				j++;
			} else if (obj instanceof String) {
				this.poolbuf.appendByte(8);
				this.poolbuf.appendChar(pool1.put(Name.fromString((String) obj)));
			} else if (obj instanceof Type) {
				this.poolbuf.appendByte(7);
				this.poolbuf.appendChar(pool1.put(this.xClassName((Type) obj)));
			} else {
				throw new InternalError("writePool " + obj);
			}
		}

		if (pool1.pp > Pool.MAX_ENTRIES) {
			throw new PoolOverflow();
		}
		putChar(this.poolbuf, i, pool1.pp);
	}

	private Name fieldName(final Symbol symbol) {
		return this.scramble && (symbol.flags() & 2) != 0 || this.scrambleAll && (symbol.flags() & 5) == 0 ? Name.fromString("_$" + symbol.name.index) : symbol.name;
	}

	private NameAndType nameType(final Symbol symbol) {
		return new NameAndType(this.fieldName(symbol), this.retrofit ? symbol.erasure() : symbol.externalType());
	}

	private int writeAttr(final Name name) {
		this.databuf.appendChar(this.pool.put(name));
		this.databuf.appendInt(0);
		return this.databuf.length;
	}

	private void endAttr(final int i) {
		putInt(this.databuf, i - 4, this.databuf.length - i);
	}

	private int beginAttrs() {
		this.databuf.appendChar(0);
		return this.databuf.length;
	}

	private void endAttrs(final int i, final int j) {
		putChar(this.databuf, i - 2, j);
	}

	private int writeFlagAttrs(final int i) {
		int j = 0;
		if ((i & 0x20000) != 0) {
			final int k = this.writeAttr(Names.Deprecated);
			this.endAttr(k);
			j++;
		}
		if ((i & 0x10000) != 0) {
			final int l = this.writeAttr(Names.Synthetic);
			this.endAttr(l);
			j++;
		}
		return j;
	}

	private int writeMemberAttrs(final Symbol symbol) {
		return this.writeFlagAttrs(symbol.flags());
	}

	private void enterInner(final ClassSymbol classsymbol) {
		if (this.pool != null && ((Symbol) classsymbol).owner.kind != 1 && (this.innerClasses == null || !this.innerClasses.contains(classsymbol))) {
			if (((Symbol) classsymbol).owner.kind == 2) {
				this.enterInner((ClassSymbol) ((Symbol) classsymbol).owner);
			}
			this.pool.put(classsymbol);
			this.pool.put(((Symbol) classsymbol).name);
			if (this.innerClasses == null) {
				this.innerClasses = Set.make();
				this.innerClassesQueue = new ListBuffer();
				this.pool.put(Names.InnerClasses);
			}
			this.innerClasses.add(classsymbol);
			this.innerClassesQueue.append(classsymbol);
		}
	}

	private void writeInnerClasses() {
		final int i = this.writeAttr(Names.InnerClasses);
		this.databuf.appendChar(this.innerClassesQueue.length());
		for (List list = this.innerClassesQueue.toList(); list.nonEmpty(); list = list.tail) {
			final ClassSymbol classsymbol = (ClassSymbol) list.head;
			if (dumpInnerClassModifiers) {
				System.out.println("INNERCLASS  " + ((Symbol) classsymbol).name);
				System.out.println("---" + flagNames(((Symbol) classsymbol).flags_field));
			}
			this.databuf.appendChar(this.pool.get(classsymbol));
			this.databuf.appendChar(((Symbol) classsymbol).owner.kind != 2 ? 0 : this.pool.get(((Symbol) classsymbol).owner));
			this.databuf.appendChar(((Symbol) classsymbol).name.len == 0 ? 0 : this.pool.get(((Symbol) classsymbol).name));
			this.databuf.appendChar(((Symbol) classsymbol).flags_field);
		}

		this.endAttr(i);
	}

	private void writeField(final VarSymbol varsymbol) {
		this.databuf.appendChar(varsymbol.flags());
		if (dumpFieldModifiers) {
			System.out.println("FIELD  " + this.fieldName(varsymbol));
			System.out.println("---" + flagNames(varsymbol.flags()));
		}
		this.databuf.appendChar(this.pool.put(this.fieldName(varsymbol)));
		this.databuf.appendChar(this.pool.put(this.typeSig(varsymbol.erasure())));
		final int i = this.beginAttrs();
		int j = 0;
		if (varsymbol.constValue != null) {
			final int k = this.writeAttr(Names.ConstantValue);
			this.databuf.appendChar(this.pool.put(varsymbol.constValue));
			this.endAttr(k);
			j++;
		}
		j += this.writeMemberAttrs(varsymbol);
		this.endAttrs(i, j);
	}

	private void writeMethod(final MethodSymbol methodsymbol) {
		this.databuf.appendChar(methodsymbol.flags());
		if (dumpMethodModifiers) {
			System.out.println("METHOD  " + this.fieldName(methodsymbol));
			System.out.println("---" + flagNames(methodsymbol.flags()));
		}
		this.databuf.appendChar(this.pool.put(this.fieldName(methodsymbol)));
		this.databuf.appendChar(this.pool.put(this.typeSig(methodsymbol.externalType())));
		final int i = this.beginAttrs();
		int j = 0;
		if (methodsymbol.code != null) {
			final int k = this.writeAttr(Names.Code);
			this.writeCode(methodsymbol.code);
			methodsymbol.code = null;
			this.endAttr(k);
			j++;
		}
		final List list = ((Symbol) methodsymbol).type.thrown();
		if (list.nonEmpty()) {
			final int l = this.writeAttr(Names.Exceptions);
			this.databuf.appendChar(list.length());
			for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
				this.databuf.appendChar(this.pool.put(list1.head));
			}

			this.endAttr(l);
			j++;
		}
		j += this.writeMemberAttrs(methodsymbol);
		this.endAttrs(i, j);
	}

	private void writeCode(final Code code) {
		this.databuf.appendChar(code.max_stack);
		this.databuf.appendChar(code.max_locals);
		this.databuf.appendInt(code.cp);
		this.databuf.appendBytes(code.code, 0, code.cp);
		this.databuf.appendChar(code.catchInfo.length());
		for (List list = code.catchInfo.toList(); list.nonEmpty(); list = list.tail) {
			for (int i = 0; i < ((char[]) list.head).length; i++) {
				this.databuf.appendChar(((char[]) list.head)[i]);
			}

		}

		final int j = this.beginAttrs();
		int k = 0;
		if (code.lineInfo.nonEmpty()) {
			final int l = this.writeAttr(Names.LineNumberTable);
			this.databuf.appendChar(code.lineInfo.length());
			for (List list1 = code.lineInfo.reverse(); list1.nonEmpty(); list1 = list1.tail) {
				for (int k1 = 0; k1 < ((char[]) list1.head).length; k1++) {
					this.databuf.appendChar(((char[]) list1.head)[k1]);
				}

			}

			this.endAttr(l);
			k++;
		}
		if (code.nvars > 0) {
			final int i1 = this.writeAttr(Names.LocalVariableTable);
			int j1 = code.nvars;
			int l1 = 0;
			int j2 = 0;
			while (j1 > 0) {
				if (code.lvar[l1] != null) {
					if (code.lvar_start_pc[l1] != '\uFFFF') {
						j2++;
					} else {
						code.lvar[l1] = null;
					}
					j1--;
				}
				l1++;
			}
			this.databuf.appendChar(j2);
			for (int i2 = 0; j2 > 0; i2++) {
				if (code.lvar[i2] != null) {
					this.databuf.appendChar(code.lvar_start_pc[i2]);
					this.databuf.appendChar(code.lvar_length[i2]);
					this.databuf.appendChar(this.pool.put(((Symbol) code.lvar[i2]).name));
					this.databuf.appendChar(this.pool.put(this.typeSig(code.lvar[i2].erasure())));
					this.databuf.appendChar(code.lvar_reg[i2]);
					j2--;
				}
			}

			this.endAttr(i1);
			k++;
		}
		this.endAttrs(j, k);
	}

	private void writeFields(final Entry entry) {
		if (entry != null) {
			this.writeFields(entry.sibling);
			if (entry.sym.kind == 4) {
				this.writeField((VarSymbol) entry.sym);
			}
		}
	}

	private void writeMethods(final Entry entry) {
		if (entry != null) {
			this.writeMethods(entry.sibling);
			if (entry.sym.kind == 16) {
				this.writeMethod((MethodSymbol) entry.sym);
			}
		}
	}

	public void writeClassFile(final OutputStream outputstream, final ClassSymbol classsymbol) throws IOException, PoolOverflow, StringOverflow {
		this.databuf.reset();
		this.poolbuf.reset();
		this.sigbuf.reset();
		this.pool = classsymbol.pool;
		this.innerClasses = null;
		this.innerClassesQueue = null;
		final Type type = ((Symbol) classsymbol).type.supertype();
		final List list = ((Symbol) classsymbol).type.interfaces();
		int i = classsymbol.flags();
		if ((i & 4) != 0) {
			i |= 1;
		}
		i &= 0xe11 & 0xfffff7ff;
		if ((i & 0x200) == 0) {
			i |= 0x20;
		}
		if (dumpClassModifiers) {
			System.out.println();
			System.out.println("CLASSFILE  " + classsymbol.fullName());
			System.out.println("---" + flagNames(i));
		}
		this.databuf.appendChar(i);
		this.databuf.appendChar(this.pool.put(classsymbol));
		this.databuf.appendChar(type.tag != 10 ? 0 : this.pool.put(type.tsym));
		this.databuf.appendChar(list.length());
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			this.databuf.appendChar(this.pool.put(((Type) list1.head).tsym));
		}

		int j = 0;
		int k = 0;
		for (Entry entry = classsymbol.members().elems; entry != null; entry = entry.sibling) {
			switch (entry.sym.kind) {
			case 4: // '\004'
				j++;
				break;

			case 16: // '\020'
				k++;
				break;

			case 2: // '\002'
				this.enterInner((ClassSymbol) entry.sym);
				break;

			default:
				throw new InternalError();
			}
		}

		this.databuf.appendChar(j);
		this.writeFields(classsymbol.members().elems);
		this.databuf.appendChar(k);
		this.writeMethods(classsymbol.members().elems);
		final int l = this.beginAttrs();
		int i1 = 0;
		if (classsymbol.sourcefile != null && this.emitSourceFile) {
			final int j1 = this.writeAttr(Names.SourceFile);
			String s = classsymbol.sourcefile.toString();
			int k1 = s.lastIndexOf(File.separatorChar);
			final int l1 = s.lastIndexOf('/');
			if (l1 > k1) {
				k1 = l1;
			}
			if (k1 >= 0) {
				s = s.substring(k1 + 1);
			}
			this.databuf.appendChar(classsymbol.pool.put(Name.fromString(s)));
			this.endAttr(j1);
			i1++;
		}
		i1 += this.writeFlagAttrs(classsymbol.flags());
		this.poolbuf.appendInt(0xcafebabe);
		this.poolbuf.appendChar(this.minorVersion);
		this.poolbuf.appendChar(this.majorVersion);
		this.writePool(classsymbol.pool);
		if (classsymbol.pool.pp > Pool.MAX_ENTRIES) {
			throw new IOException(Log.getLocalizedString("too.many.constants"));
		}
		if (this.innerClasses != null) {
			this.writeInnerClasses();
			i1++;
		}
		this.endAttrs(l, i1);
		this.poolbuf.appendBytes(this.databuf.elems, 0, this.databuf.length);
		outputstream.write(this.poolbuf.elems, 0, this.poolbuf.length);
		this.pool = classsymbol.pool = null;
	}

	public File outputFile(final ClassSymbol classsymbol, final String s) {
		if (this.outDir == null) {
			final String s1 = Convert.shortName(classsymbol.flatname) + s;
			if (classsymbol.sourcefile == null) {
				return new File(s1);
			}
			final String s2 = new File(classsymbol.sourcefile.toString()).getParent();
			return s2 == null ? new File(s1) : new File(s2, s1);
		}
		return outputFile(this.outDir, classsymbol.flatname.toString(), s);
	}

	private static File outputFile(File file, final String s, final String s1) {
		int i = 0;
		for (int j = s.indexOf('.'); j >= i; j = s.indexOf('.', i)) {
			file = new File(file, s.substring(i, j));
			if (!file.exists()) {
				file.mkdir();
			}
			i = j + 1;
		}

		return new File(file, s.substring(i) + s1);
	}

	private File outDir;
	private final boolean scramble;
	private final boolean scrambleAll;
	private final boolean retrofit;
	private final boolean emitSourceFile;
	public final short majorVersion;
	private final short minorVersion;
	static final int DATA_BUF_SIZE = 65520;
	static final int POOL_BUF_SIZE = 0x1fff0;
	private final ByteBuffer databuf;
	private final ByteBuffer poolbuf;
	private final ByteBuffer sigbuf;
	private Pool pool;
	private Set innerClasses;
	private ListBuffer innerClassesQueue;
	private static final String dumpModFlags = System.getProperty("javac.dump.modifiers");
	private static final boolean dumpClassModifiers = dumpModFlags != null && dumpModFlags.indexOf('c') != -1;
	private static final boolean dumpFieldModifiers = dumpModFlags != null && dumpModFlags.indexOf('f') != -1;
	private static final boolean dumpInnerClassModifiers = dumpModFlags != null && dumpModFlags.indexOf('i') != -1;
	private static final boolean dumpMethodModifiers = dumpModFlags != null && dumpModFlags.indexOf('m') != -1;
	private static final String flagName[] = { "PUBLIC", "PRIVATE", "PROTECTED", "STATIC", "FINAL", "SUPER", "VOLATILE", "TRANSIENT", "NATIVE", "INTERFACE", "ABSTRACT", "STRICTFP" };

}
