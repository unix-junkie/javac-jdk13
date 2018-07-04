package sun.tools.java;

public final class MethodType extends Type {

	MethodType(final String s, final Type type, final Type atype[]) {
		super(12, s);
		this.returnType = type;
		this.argTypes = atype;
	}

	public Type getReturnType() {
		return this.returnType;
	}

	public Type[] getArgumentTypes() {
		return this.argTypes;
	}

	public boolean equalArguments(final Type type) {
		if (type.typeCode != 12) {
			return false;
		}
		final MethodType methodtype = (MethodType) type;
		if (this.argTypes.length != methodtype.argTypes.length) {
			return false;
		}
		for (int i = this.argTypes.length - 1; i >= 0; i--) {
			if (this.argTypes[i] != methodtype.argTypes[i]) {
				return false;
			}
		}

		return true;
	}

	public int stackSize() {
		int i = 0;
		for (int j = 0; j < this.argTypes.length; j++) {
			i += this.argTypes[j].stackSize();
		}

		return i;
	}

	public String typeString(final String s, final boolean flag, final boolean flag1) {
		final StringBuffer stringbuffer = new StringBuffer();
		stringbuffer.append(s);
		stringbuffer.append('(');
		for (int i = 0; i < this.argTypes.length; i++) {
			if (i > 0) {
				stringbuffer.append(", ");
			}
			stringbuffer.append(this.argTypes[i].typeString("", flag, flag1));
		}

		stringbuffer.append(')');
		return flag1 ? this.getReturnType().typeString(stringbuffer.toString(), flag, flag1) : stringbuffer.toString();
	}

	private final Type returnType;
	private final Type[] argTypes;
}
