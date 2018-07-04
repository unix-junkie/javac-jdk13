package sun.tools.java;

public class ClassNotFound extends Exception {
	private static final long serialVersionUID = 8413272704727255399L;

	public ClassNotFound(final Identifier identifier) {
		super(identifier.toString());
		this.name = identifier;
	}

	public final Identifier name;
}
