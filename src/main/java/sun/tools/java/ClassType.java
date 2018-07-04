package sun.tools.java;

public final class ClassType extends Type {

	ClassType(final String s, final Identifier identifier) {
		super(10, s);
		this.className = identifier;
	}

	public Identifier getClassName() {
		return this.className;
	}

	public String typeString(final String s, final boolean flag, final boolean flag1) {
		final String s1 = (flag ? this.getClassName().getFlatName() : Identifier.lookup(this.getClassName().getQualifier(), this.getClassName().getFlatName())).toString();
		return s.length() <= 0 ? s1 : s1 + ' ' + s;
	}

	Identifier className;
}
