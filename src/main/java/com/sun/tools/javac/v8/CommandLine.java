package com.sun.tools.javac.v8;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;

import com.sun.tools.javac.v8.util.ListBuffer;

final class CommandLine {
	private CommandLine() {
	}

	static String[] parse(final String as[]) throws IOException {
		final ListBuffer listbuffer = new ListBuffer();
		for (int i = 0; i < as.length; i++) {
			String s = as[i];
			if (s.length() > 1 && s.charAt(0) == '@') {
				s = s.substring(1);
				if (s.charAt(0) == '@') {
					listbuffer.append(s);
				} else {
					loadCmdFile(s, listbuffer);
				}
			} else {
				listbuffer.append(s);
			}
		}

		return (String[]) listbuffer.toList().toArray(new String[listbuffer.length()]);
	}

	private static void loadCmdFile(final String s, final ListBuffer listbuffer) throws IOException {
		final BufferedReader bufferedreader = new BufferedReader(new FileReader(s));
		final StreamTokenizer streamtokenizer = new StreamTokenizer(bufferedreader);
		streamtokenizer.resetSyntax();
		streamtokenizer.wordChars(32, 255);
		streamtokenizer.whitespaceChars(0, 32);
		streamtokenizer.commentChar(35);
		streamtokenizer.quoteChar(34);
		streamtokenizer.quoteChar(39);
		for (; streamtokenizer.nextToken() != -1; listbuffer.append(streamtokenizer.sval)) {
		}
		bufferedreader.close();
	}
}
