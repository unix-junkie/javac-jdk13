package sun.tools.java;

public final class IdentifierToken {

	public IdentifierToken(final long l, final Identifier identifier) {
		this.where = l;
		this.id = identifier;
	}

	public IdentifierToken(final Identifier identifier) {
		this.where = 0L;
		this.id = identifier;
	}

	public IdentifierToken(final long l, final Identifier identifier, final int i) {
		this.where = l;
		this.id = identifier;
		this.modifiers = i;
	}

	public long getWhere() {
		return this.where;
	}

	public Identifier getName() {
		return this.id;
	}

	public int getModifiers() {
		return this.modifiers;
	}

	public String toString() {
		return this.id.toString();
	}

	public static long getWhere(final IdentifierToken identifiertoken, final long l) {
		return identifiertoken == null || identifiertoken.where == 0L ? l : identifiertoken.where;
	}

	private final long where;
	int modifiers;
	Identifier id;
}
