package sun.tools.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Collection;

public final class CommandLine {
	private CommandLine() {
		// assert false;
	}

	public static String[] parse(final String as[]) throws IOException {
		final ArrayList arraylist = new ArrayList(as.length);
		for (int i = 0; i < as.length; i++) {
			String s = as[i];
			if (s.length() > 1 && s.charAt(0) == '@') {
				s = s.substring(1);
				if (s.charAt(0) == '@') {
					arraylist.add(s);
				} else {
					loadCmdFile(s, arraylist);
				}
			} else {
				arraylist.add(s);
			}
		}

		return (String[]) arraylist.toArray(new String[arraylist.size()]);
	}

	private static void loadCmdFile(final String s, final Collection list) throws IOException {
		final BufferedReader in = new BufferedReader(new FileReader(s));
		final StreamTokenizer streamtokenizer = new StreamTokenizer(in);
		streamtokenizer.resetSyntax();
		streamtokenizer.wordChars(32, 255);
		streamtokenizer.whitespaceChars(0, 32);
		streamtokenizer.commentChar(35);
		streamtokenizer.quoteChar(34);
		streamtokenizer.quoteChar(39);
		for (; streamtokenizer.nextToken() != -1; list.add(streamtokenizer.sval)) {
		}
		in.close();
	}
}
