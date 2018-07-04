package sun.tools.java;

import java.util.Hashtable;
import java.util.Map;

public final class Identifier {

	private Identifier(final String s) {
		this.typeObject = null;
		this.name = s;
		this.ipos = s.indexOf(' ');
	}

	int getType() {
		return !(this.value instanceof Integer) ? 60 : ((Number) this.value).intValue();
	}

	void setType(final int i) {
		this.value = new Integer(i);
	}

	public static synchronized Identifier lookup(final String s) {
		Identifier identifier = (Identifier) hash.get(s);
		if (identifier == null) {
			hash.put(s, identifier = new Identifier(s));
		}
		return identifier;
	}

	public static Identifier lookup(final Identifier identifier, final Identifier identifier1) {
		if (identifier == Constants.idNull) {
			return identifier1;
		}
		if (identifier.name.charAt(identifier.name.length() - 1) == ' ') {
			return lookup(identifier.name + identifier1.name);
		}
		final Identifier identifier2 = lookup(identifier + "." + identifier1);
		if (!identifier1.isQualified() && !identifier.isInner()) {
			identifier2.value = identifier;
		}
		return identifier2;
	}

	public static Identifier lookupInner(final Identifier identifier, final Identifier identifier1) {
		final Identifier identifier2;
		if (identifier.isInner()) {
			identifier2 = identifier.name.charAt(identifier.name.length() - 1) == ' ' ? lookup(identifier.name + identifier1) : lookup(identifier, identifier1);
		} else {
			identifier2 = lookup(identifier + "." + ' ' + identifier1);
		}
		identifier2.value = identifier.value;
		return identifier2;
	}

	public String toString() {
		return this.name;
	}

	public boolean isQualified() {
		if (this.value == null) {
			int i = this.ipos;
			if (i <= 0) {
				i = this.name.length();
			} else {
				i--;
			}
			final int j = this.name.lastIndexOf('.', i - 1);
			this.value = j >= 0 ? (Object) lookup(this.name.substring(0, j)) : (Object) Constants.idNull;
		}
		return this.value instanceof Identifier && this.value != Constants.idNull;
	}

	public Identifier getQualifier() {
		return this.isQualified() ? (Identifier) this.value : Constants.idNull;
	}

	public Identifier getName() {
		return this.isQualified() ? lookup(this.name.substring(((Identifier) this.value).name.length() + 1)) : this;
	}

	public boolean isInner() {
		return this.ipos > 0;
	}

	public Identifier getFlatName() {
		if (this.isQualified()) {
			return this.getName().getFlatName();
		}
		if (this.ipos > 0 && this.name.charAt(this.ipos - 1) == '.') {
			if (this.ipos + 1 == this.name.length()) {
				return lookup(this.name.substring(0, this.ipos - 1));
			}
			final String s = this.name.substring(this.ipos + 1);
			final String s1 = this.name.substring(0, this.ipos);
			return lookup(s1 + s);
		}
		return this;
	}

	public Identifier getTopName() {
		return !this.isInner() ? this : lookup(this.getQualifier(), this.getFlatName().getHead());
	}

	public Identifier getHead() {
		Identifier identifier;
		for (identifier = this; identifier.isQualified(); identifier = identifier.getQualifier()) {
		}
		return identifier;
	}

	public Identifier getTail() {
		final Identifier identifier = this.getHead();
		return identifier == this ? Constants.idNull : lookup(this.name.substring(identifier.name.length() + 1));
	}

	public boolean hasAmbigPrefix() {
		return this.name.startsWith("<<ambiguous>>");
	}

	Identifier addAmbigPrefix() {
		return lookup("<<ambiguous>>" + this.name);
	}

	Identifier removeAmbigPrefix() {
		return this.hasAmbigPrefix() ? lookup(this.name.substring("<<ambiguous>>".length())) : this;
	}

	private static final Map hash = new Hashtable(3001, 0.5F);
	private final String name;
	private Object value;
	Type typeObject;
	private final int ipos;
	public static final char INNERCLASS_PREFIX = 32;

}
