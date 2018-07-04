package sun.tools.java;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;
import java.util.zip.ZipFile;

final class ClassPathEntry {
	String[] getFiles(final String s) {
		String as[] = (String[]) this.subdirs.get(s);
		if (as == null) {
			final File file = new File(this.dir.getPath(), s);
			if (file.isDirectory()) {
				as = file.list();
				if (as == null) {
					as = new String[0];
				}
				if (as.length == 0) {
					as = new String[] { "" };
				}
			} else {
				as = new String[0];
			}
			this.subdirs.put(s, as);
		}
		return as;
	}

	File dir;
	ZipFile zip;
	private final Map subdirs = new Hashtable(29);
}
