package com.sun.tools.javac.v8.code;

import java.io.File;

import com.sun.tools.javac.v8.util.Name;

public final class ClassFile {
	static final class NameAndType {

		public boolean equals(final Object obj) {
			return obj instanceof NameAndType && this.name == ((NameAndType) obj).name && this.type.equals(((NameAndType) obj).type);
		}

		public int hashCode() {
			return this.name.hashCode() * this.type.hashCode();
		}

		final Name name;
		final Type type;

		NameAndType(final Name name1, final Type type1) {
			this.name = name1;
			this.type = type1;
		}
	}

	private ClassFile() {
	}

	public static boolean isValidTargetRelease(final String s) {
		for (int i = 0; i < releases.length; i++) {
			if (releases[i].equals(s)) {
				return true;
			}
		}

		return false;
	}

	static short classMajorVersion(final String s) {
		for (int i = 0; i < releases.length; i++) {
			if (releases[i].equals(s)) {
				return majorVersions[i];
			}
		}

		return 45;
	}

	static short classMinorVersion(final String s) {
		for (int i = 0; i < releases.length; i++) {
			if (releases[i].equals(s)) {
				return minorVersions[i];
			}
		}

		return 3;
	}

	static byte[] internalize(final byte abyte0[], final int i, final int j) {
		final byte abyte1[] = new byte[j];
		for (int k = 0; k < j; k++) {
			final byte byte0 = abyte0[i + k];
			abyte1[k] = byte0 == 47 ? 46 : byte0;
		}

		return abyte1;
	}

	private static byte[] externalize(final byte abyte0[], final int i, final int j) {
		final byte abyte1[] = new byte[j];
		for (int k = 0; k < j; k++) {
			final byte byte0 = abyte0[i + k];
			abyte1[k] = byte0 == 46 ? 47 : byte0;
		}

		return abyte1;
	}

	static byte[] externalize(final Name name) {
		return externalize(Name.names, name.index, name.len);
	}

	static String externalizeFileName(final Name name) {
		return name.toString().replace('.', File.separatorChar);
	}

	public static final int JAVA_MAGIC = 0xcafebabe;
	public static final int JAVA_MAJOR_VERSION = 47;
	public static final int JAVA_MINOR_VERSION = 0;
	public static final int JAVA_MIN_MAJOR_VERSION = 45;
	public static final int JAVA_MIN_MINOR_VERSION = 3;
	public static final int DEFAULT_MAJOR_VERSION = 45;
	public static final int DEFAULT_MINOR_VERSION = 3;
	public static final int CONSTANT_Utf8 = 1;
	public static final int CONSTANT_Unicode = 2;
	public static final int CONSTANT_Integer = 3;
	public static final int CONSTANT_Float = 4;
	public static final int CONSTANT_Long = 5;
	public static final int CONSTANT_Double = 6;
	public static final int CONSTANT_Class = 7;
	public static final int CONSTANT_String = 8;
	public static final int CONSTANT_Fieldref = 9;
	public static final int CONSTANT_Methodref = 10;
	public static final int CONSTANT_InterfaceMethodref = 11;
	public static final int CONSTANT_NameandType = 12;
	public static final int MAX_PARAMETERS = 255;
	public static final int MAX_DIMENSIONS = 255;
	public static final int MAX_CODE = 65535;
	public static final int MAX_LOCALS = 65535;
	public static final int MAX_STACK = 65535;
	private static final String[] releases = { "1.1", "1.2", "1.3" };
	private static final short[] majorVersions = { 45, 46, 47 };
	private static final short[] minorVersions = { 3, 0, 0 };
}
