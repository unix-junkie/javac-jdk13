package com.sun.tools.javac.v8.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class FileEntry {
	public static class Zipped extends FileEntry {

		public InputStream open() throws IOException {
			return this.zdir.getInputStream(this.entry);
		}

		public Name getName() {
			return this.name;
		}

		public String getPath() {
			return this.zdir.getName() + '(' + this.entry + ')';
		}

		public long length() {
			return this.entry.getSize();
		}

		long lastMod() {
			return this.entry.getTime();
		}

		final Name name;
		final ZipFile zdir;
		final ZipEntry entry;

		public Zipped(final String s, final ZipFile zipfile, final ZipEntry zipentry) {
			this.name = Name.fromString(s);
			this.zdir = zipfile;
			this.entry = zipentry;
		}
	}

	public static class Regular extends FileEntry {

		public InputStream open() throws IOException {
			return new FileInputStream(this.f);
		}

		public Name getName() {
			return this.name;
		}

		public String getPath() {
			return this.f.getPath();
		}

		public long length() {
			return this.f.length();
		}

		long lastMod() {
			return this.f.lastModified();
		}

		final Name name;
		final File f;

		public Regular(final String s, final File file) {
			this.name = Name.fromString(s);
			this.f = file;
		}
	}

	FileEntry() {
		this.lastmod = -2L;
	}

	public abstract InputStream open() throws IOException;

	public abstract Name getName();

	public abstract String getPath();

	public abstract long length();

	public String toString() {
		return this.getName().toString();
	}

	abstract long lastMod();

	public long lastModified() {
		if (this.lastmod == -2L) {
			this.lastmod = this.lastMod();
		}
		return this.lastmod;
	}

	private long lastmod;
}
