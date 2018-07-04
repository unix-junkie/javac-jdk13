package sun.tools.java;

public final class AmbiguousMember extends Exception {
	private static final long serialVersionUID = -8728819089280198625L;

	public AmbiguousMember(final MemberDefinition memberdefinition, final MemberDefinition memberdefinition1) {
		super(memberdefinition.getName() + " + " + memberdefinition1.getName());
		this.field1 = memberdefinition;
		this.field2 = memberdefinition1;
	}

	public final MemberDefinition field1;
	public final MemberDefinition field2;
}
