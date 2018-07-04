package sun.tools.java;

public final class AmbiguousClass extends ClassNotFound {
	private static final long serialVersionUID = 3960234615156172663L;

	AmbiguousClass(final Identifier identifier, final Identifier identifier1) {
		super(identifier.getName());
		this.name1 = identifier;
		this.name2 = identifier1;
	}

	public final Identifier name1;
	public final Identifier name2;
}
