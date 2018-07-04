package sun.tools.java;

public final class CompilerError extends Error {
	private static final long serialVersionUID = -4915506762694290892L;

	public CompilerError(final String message) {
		super(message);
		this.e = this;
	}

	public CompilerError(final Throwable cause) {
		super(cause.getMessage());
		this.e = cause;
	}

	public void printStackTrace() {
		if (this.e == this) {
			super.printStackTrace();
		} else {
			this.e.printStackTrace();
		}
	}

	private final Throwable e;
}
