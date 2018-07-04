package com.sun.tools.javac;

final class Main {
	private Main() {
		// assert false;
	}

	public static void main(final String args[]) {
		final com.sun.tools.javac.v8.Main main = new com.sun.tools.javac.v8.Main("javac");
		System.exit(main.compile(args));
	}
}
