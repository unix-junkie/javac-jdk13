package com.sun.tools.javac.v8.util;

public final class FatalError extends Error {
	private static final long serialVersionUID = 5218295620954080066L;

	public FatalError(final String s) {
		super(s);
	}
}
