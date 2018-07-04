package sun.tools.asm;

import sun.tools.java.MemberDefinition;

final class NameAndTypeData {

	NameAndTypeData(final MemberDefinition memberdefinition) {
		this.field = memberdefinition;
	}

	public int hashCode() {
		return this.field.getName().hashCode() * this.field.getType().hashCode();
	}

	public boolean equals(final Object obj) {
		if (obj instanceof NameAndTypeData) {
			final NameAndTypeData nameandtypedata = (NameAndTypeData) obj;
			return this.field.getName().equals(nameandtypedata.field.getName()) && this.field.getType().equals(nameandtypedata.field.getType());
		}
		return false;
	}

	public String toString() {
		return "%%" + this.field + "%%";
	}

	final MemberDefinition field;
}
