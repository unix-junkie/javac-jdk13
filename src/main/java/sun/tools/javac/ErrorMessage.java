package sun.tools.javac;

final class ErrorMessage {

	ErrorMessage(final long l, final String s) {
		this.where = l;
		this.message = s;
	}

	final long where;
	final String message;
	ErrorMessage next;
}
