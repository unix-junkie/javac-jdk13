package sun.tools.java;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public final class ClassPath {

	public ClassPath(final String s) {
		this.fileSeparatorChar = String.valueOf(File.separatorChar);
		this.init(s);
	}

	private void init(final String s) {
		this.pathstr = s;
		if (s.length() == 0) {
			this.path = new ClassPathEntry[0];
		}
		int l;
		for (int i = l = 0; (i = s.indexOf(dirSeparator, i)) != -1; i++) {
			l++;
		}

		final ClassPathEntry aclasspathentry[] = new ClassPathEntry[l + 1];
		final int i1 = s.length();
		int k;
		for (int j = l = 0; j < i1; j = k + 1) {
			if ((k = s.indexOf(dirSeparator, j)) == -1) {
				k = i1;
			}
			if (j == k) {
				aclasspathentry[l] = new ClassPathEntry();
				aclasspathentry[l++].dir = new File(".");
			} else {
				final File file = new File(s.substring(j, k));
				if (file.isFile()) {
					try {
						final ZipFile zipfile = new ZipFile(file);
						aclasspathentry[l] = new ClassPathEntry();
						aclasspathentry[l++].zip = zipfile;
					} catch (final ZipException ignored) {
					} catch (final IOException ignored) {
					}
				} else {
					aclasspathentry[l] = new ClassPathEntry();
					aclasspathentry[l++].dir = file;
				}
			}
		}

		this.path = new ClassPathEntry[l];
		System.arraycopy(aclasspathentry, 0, this.path, 0, l);
	}

	public ClassFile getDirectory(final String s) {
		return this.getFile(s, true);
	}

	public ClassFile getFile(final String s) {
		return this.getFile(s, false);
	}

	private ClassFile getFile(String s, final boolean flag) {
		String s1 = s;
		String s2 = "";
		if (!flag) {
			final int i = s.lastIndexOf(File.separatorChar);
			s1 = s.substring(0, i + 1);
			s2 = s.substring(i + 1);
		} else if (s1.length() != 0 && !s1.endsWith(this.fileSeparatorChar)) {
			s1 += File.separatorChar;
			s = s1;
		}
		for (int j = 0; j < this.path.length; j++) {
			if (this.path[j].zip != null) {
				final String s3 = s.replace(File.separatorChar, '/');
				final ZipEntry zipentry = this.path[j].zip.getEntry(s3);
				if (zipentry != null) {
					return new ClassFile(this.path[j].zip, zipentry);
				}
			} else {
				final File file = new File(this.path[j].dir.getPath(), s);
				final String as[] = this.path[j].getFiles(s1);
				if (flag) {
					if (as.length > 0) {
						return new ClassFile(file);
					}
				} else {
					for (int k = 0; k < as.length; k++) {
						if (s2.equals(as[k])) {
							return new ClassFile(file);
						}
					}

				}
			}
		}

		return null;
	}

	Iterator getFiles(final String s, final String s1) {
		final Map files = new HashMap();
		for (int i = this.path.length; --i >= 0;) {
			if (this.path[i].zip != null) {
				for (final Enumeration enumeration = this.path[i].zip.entries(); enumeration.hasMoreElements();) {
					final ZipEntry zipentry = (ZipEntry) enumeration.nextElement();
					String s2 = zipentry.getName();
					s2 = s2.replace('/', File.separatorChar);
					if (s2.startsWith(s) && s2.endsWith(s1)) {
						files.put(s2, new ClassFile(this.path[i].zip, zipentry));
					}
				}

			} else {
				final String as[] = this.path[i].getFiles(s);
				for (int j = 0; j < as.length; j++) {
					String s3 = as[j];
					if (s3.endsWith(s1)) {
						s3 = s + File.separatorChar + s3;
						final File file = new File(this.path[i].dir.getPath(), s3);
						files.put(s3, new ClassFile(file));
					}
				}

			}
		}

		return files.values().iterator();
	}

	public void close() throws IOException {
		for (int i = this.path.length; --i >= 0;) {
			if (this.path[i].zip != null) {
				this.path[i].zip.close();
			}
		}

	}

	public String toString() {
		return this.pathstr;
	}

	private static final char dirSeparator;
	private String pathstr;
	private ClassPathEntry path[];
	private final String fileSeparatorChar;

	static {
		dirSeparator = File.pathSeparatorChar;
	}
}
