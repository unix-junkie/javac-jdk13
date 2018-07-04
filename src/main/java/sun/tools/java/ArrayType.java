package sun.tools.java;

public final class ArrayType extends Type {

	ArrayType(final String s, final Type type) {
		super(9, s);
		this.elemType = type;
	}

	public Type getElementType() {
		return this.elemType;
	}

	public int getArrayDimension() {
		return this.elemType.getArrayDimension() + 1;
	}

	public String typeString(final String s, final boolean flag, final boolean flag1) {
		return this.getElementType().typeString(s, flag, flag1) + "[]";
	}

	private final Type elemType;
}
