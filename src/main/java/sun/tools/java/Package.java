package sun.tools.java;

import java.io.File;

public final class Package {

	public Package(final ClassPath classpath, final Identifier identifier) {
		this(classpath, classpath, identifier);
	}

	public Package(final ClassPath sourcePath, final ClassPath binaryPath, Identifier identifier) {
		if (identifier.isInner()) {
			identifier = Identifier.lookup(identifier.getQualifier(), identifier.getFlatName());
		}
		this.sourcePath = sourcePath;
		this.binaryPath = binaryPath;
		this.pkg = identifier.toString().replace('.', File.separatorChar);
	}

	public boolean classExists(final Identifier identifier) {
		return this.getBinaryFile(identifier) != null || !identifier.isInner() && this.getSourceFile(identifier) != null;
	}

	public boolean exists() {
		final ClassFile classfile = this.binaryPath.getDirectory(this.pkg);
		if (classfile != null && classfile.isDirectory()) {
			return true;
		}
		if (this.sourcePath != this.binaryPath) {
			final ClassFile classfile1 = this.sourcePath.getDirectory(this.pkg);
			if (classfile1 != null && classfile1.isDirectory()) {
				return true;
			}
		}
		final String s = this.pkg + File.separator;
		return this.binaryPath.getFiles(s, ".class").hasNext() || this.sourcePath.getFiles(s, ".java").hasNext();
	}

	private String makeName(final String s) {
		return this.pkg.length() == 0 ? s : this.pkg + File.separator + s;
	}

	public ClassFile getBinaryFile(Identifier identifier) {
		identifier = Type.mangleInnerType(identifier);
		final String s = identifier + ".class";
		return this.binaryPath.getFile(this.makeName(s));
	}

	public ClassFile getSourceFile(Identifier identifier) {
		identifier = identifier.getTopName();
		final String s = identifier + ".java";
		return this.sourcePath.getFile(this.makeName(s));
	}

	public ClassFile getSourceFile(final String s) {
		return s.endsWith(".java") ? this.sourcePath.getFile(this.makeName(s)) : null;
	}

	public String toString() {
		return this.pkg.length() == 0 ? "unnamed package" : "package " + this.pkg;
	}

	private final ClassPath sourcePath;
	private final ClassPath binaryPath;
	private final String pkg;
}
