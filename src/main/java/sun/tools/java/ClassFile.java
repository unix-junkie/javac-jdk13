package sun.tools.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public final class ClassFile {

	public ClassFile(final File file) {
		this.file = file;
	}

	public ClassFile(final ZipFile zipfile, final ZipEntry zipentry) {
		this.zipFile = zipfile;
		this.zipEntry = zipentry;
	}

	public boolean isZipped() {
		return this.zipFile != null;
	}

	public InputStream getInputStream() throws IOException {
		if (this.file != null) {
			return new FileInputStream(this.file);
		}
		try {
			return this.zipFile.getInputStream(this.zipEntry);
		} catch (final ZipException zipexception) {
			throw new IOException(zipexception.getMessage());
		}
	}

	public boolean exists() {
		return this.file == null || this.file.exists();
	}

	public boolean isDirectory() {
		return this.file == null ? this.zipEntry.getName().length() > 0 && this.zipEntry.getName().charAt(this.zipEntry.getName().length() - 1) == '/' : this.file.isDirectory();
	}

	public long lastModified() {
		return this.file == null ? this.zipEntry.getTime() : this.file.lastModified();
	}

	public String getPath() {
		return this.file != null ? this.file.getPath() : this.zipFile.getName() + '(' + this.zipEntry.getName() + ')';
	}

	public String getName() {
		return this.file == null ? this.zipEntry.getName() : this.file.getName();
	}

	public String getAbsoluteName() {
		if (this.file != null) {
			try {
				return this.file.getCanonicalPath();
			} catch (final IOException ignored) {
				return this.file.getAbsolutePath();
			}
		}
		return this.zipFile.getName() + '(' + this.zipEntry.getName() + ')';
	}

	public long length() {
		return this.file == null ? this.zipEntry.getSize() : this.file.length();
	}

	public String toString() {
		return this.file == null ? this.zipEntry.toString() : this.file.toString();
	}

	private File file;
	private ZipFile zipFile;
	private ZipEntry zipEntry;
}
