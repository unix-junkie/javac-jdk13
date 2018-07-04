package sun.tools.java;

import java.util.Hashtable;
import java.util.Map;

public class Type {

	Type(final int i, final String s) {
		this.typeCode = i;
		this.typeSig = s;
		typeHash.put(s, this);
	}

	public final String getTypeSignature() {
		return this.typeSig;
	}

	public final int getTypeCode() {
		return this.typeCode;
	}

	public final int getTypeMask() {
		return 1 << this.typeCode;
	}

	public final boolean isType(final int i) {
		return this.typeCode == i;
	}

	public boolean isVoidArray() {
		if (!this.isType(9)) {
			return false;
		}
		Type type;
		for (type = this; type.isType(9); type = type.getElementType()) {
		}
		return type.isType(11);
	}

	public final boolean inMask(final int i) {
		return (1 << this.typeCode & i) != 0;
	}

	public static synchronized Type tArray(final Type type) {
		final String s = '[' + type.getTypeSignature();
		Object obj = typeHash.get(s);
		if (obj == null) {
			obj = new ArrayType(s, type);
		}
		return (Type) obj;
	}

	public Type getElementType() {
		throw new CompilerError("getElementType");
	}

	int getArrayDimension() {
		return 0;
	}

	public static synchronized Type tClass(final Identifier identifier) {
		if (identifier.isInner()) {
			final Type type = tClass(mangleInnerType(identifier));
			if (type.getClassName() != identifier) {
				changeClassName(type.getClassName(), identifier);
			}
			return type;
		}
		if (identifier.typeObject != null) {
			return identifier.typeObject;
		}
		final String s = 'L' + identifier.toString().replace('.', '/') + ';';
		Object obj = typeHash.get(s);
		if (obj == null) {
			obj = new ClassType(s, identifier);
		}
		identifier.typeObject = (Type) obj;
		return (Type) obj;
	}

	public Identifier getClassName() {
		throw new CompilerError("getClassName:" + this);
	}

	public static Identifier mangleInnerType(final Identifier identifier) {
		if (!identifier.isInner()) {
			return identifier;
		}
		final Identifier identifier1 = Identifier.lookup(identifier.getFlatName().toString().replace('.', '$'));
		if (identifier1.isInner()) {
			throw new CompilerError("mangle " + identifier1);
		}
		return Identifier.lookup(identifier.getQualifier(), identifier1);
	}

	private static void changeClassName(final Identifier identifier, final Identifier identifier1) {
		((ClassType) tClass(identifier)).className = identifier1;
	}

	public static synchronized Type tMethod(final Type type) {
		return tMethod(type, noArgs);
	}

	public static synchronized Type tMethod(final Type type, final Type atype[]) {
		final StringBuffer stringbuffer = new StringBuffer();
		stringbuffer.append('(');
		for (int i = 0; i < atype.length; i++) {
			stringbuffer.append(atype[i].getTypeSignature());
		}

		stringbuffer.append(')');
		stringbuffer.append(type.getTypeSignature());
		final String s = stringbuffer.toString();
		Object obj = typeHash.get(s);
		if (obj == null) {
			obj = new MethodType(s, type, atype);
		}
		return (Type) obj;
	}

	public Type getReturnType() {
		throw new CompilerError("getReturnType");
	}

	public Type[] getArgumentTypes() {
		throw new CompilerError("getArgumentTypes");
	}

	public static synchronized Type tType(final String s) {
		final Type type = (Type) typeHash.get(s);
		if (type != null) {
			return type;
		}
		switch (s.charAt(0)) {
		case 91: // '['
			return tArray(tType(s.substring(1)));

		case 76: // 'L'
			return tClass(Identifier.lookup(s.substring(1, s.length() - 1).replace('/', '.')));

		case 40: // '('
			Type atype[] = new Type[8];
			int i = 0;
			int j;
			int k;
			for (j = 1; s.charAt(j) != ')'; j = k) {
				for (k = j; s.charAt(k) == '['; k++) {
				}
				if (s.charAt(k++) == 'L') {
					while (s.charAt(k++) != ';') {
					}
				}
				if (i == atype.length) {
					final Type atype1[] = new Type[i * 2];
					System.arraycopy(atype, 0, atype1, 0, i);
					atype = atype1;
				}
				atype[i++] = tType(s.substring(j, k));
			}

			final Type atype2[] = new Type[i];
			System.arraycopy(atype, 0, atype2, 0, i);
			return tMethod(tType(s.substring(j + 1)), atype2);
		}
		throw new CompilerError("invalid TypeSignature:" + s);
	}

	public boolean equalArguments(final Type type) {
		return false;
	}

	public int stackSize() {
		switch (this.typeCode) {
		case 11: // '\013'
		case 13: // '\r'
			return 0;

		case 0: // '\0'
		case 1: // '\001'
		case 2: // '\002'
		case 3: // '\003'
		case 4: // '\004'
		case 6: // '\006'
		case 9: // '\t'
		case 10: // '\n'
			return 1;

		case 5: // '\005'
		case 7: // '\007'
			return 2;

		case 8: // '\b'
		case 12: // '\f'
		default:
			throw new CompilerError("stackSize " + this);
		}
	}

	public int getTypeCodeOffset() {
		switch (this.typeCode) {
		case 0: // '\0'
		case 1: // '\001'
		case 2: // '\002'
		case 3: // '\003'
		case 4: // '\004'
			return 0;

		case 5: // '\005'
			return 1;

		case 6: // '\006'
			return 2;

		case 7: // '\007'
			return 3;

		case 8: // '\b'
		case 9: // '\t'
		case 10: // '\n'
			return 4;
		}
		throw new CompilerError("invalid typecode: " + this.typeCode);
	}

	public String typeString(final String s, final boolean flag, final boolean flag1) {
		String s1;
		switch (this.typeCode) {
		case 8: // '\b'
			s1 = "null";
			break;

		case 11: // '\013'
			s1 = "void";
			break;

		case 0: // '\0'
			s1 = "boolean";
			break;

		case 1: // '\001'
			s1 = "byte";
			break;

		case 2: // '\002'
			s1 = "char";
			break;

		case 3: // '\003'
			s1 = "short";
			break;

		case 4: // '\004'
			s1 = "int";
			break;

		case 5: // '\005'
			s1 = "long";
			break;

		case 6: // '\006'
			s1 = "float";
			break;

		case 7: // '\007'
			s1 = "double";
			break;

		case 13: // '\r'
			s1 = "<error>";
			if (this == tPackage) {
				s1 = "<package>";
			}
			break;

		case 9: // '\t'
		case 10: // '\n'
		case 12: // '\f'
		default:
			s1 = "unknown";
			break;
		}
		return s.length() <= 0 ? s1 : s1 + ' ' + s;
	}

	public String typeString(final String s) {
		return this.typeString(s, false, true);
	}

	public String toString() {
		return this.typeString("", false, true);
	}

	private static final Map typeHash = new Hashtable(231);
	int typeCode;
	private final String typeSig;
	public static final Type noArgs[] = new Type[0];
	public static final Type tError = new Type(13, "?");
	public static final Type tPackage = new Type(13, ".");
	public static final Type tNull = new Type(8, "*");
	public static final Type tVoid = new Type(11, RuntimeConstants.SIG_VOID);
	public static final Type tBoolean = new Type(0, RuntimeConstants.SIG_BOOLEAN);
	public static final Type tByte = new Type(1, "B");
	public static final Type tChar = new Type(2, "C");
	public static final Type tShort = new Type(3, "S");
	public static final Type tInt = new Type(4, "I");
	public static final Type tFloat = new Type(6, "F");
	public static final Type tLong = new Type(5, "J");
	public static final Type tDouble = new Type(7, "D");
	public static final Type tObject;
	public static final Type tClassDesc;
	public static final Type tString;
	static final Type tCloneable;
	static final Type tSerializable;

	static {
		tObject = tClass(Constants.idJavaLangObject);
		tClassDesc = tClass(Constants.idJavaLangClass);
		tString = tClass(Constants.idJavaLangString);
		tCloneable = tClass(Constants.idJavaLangCloneable);
		tSerializable = tClass(Constants.idJavaIoSerializable);
	}
}
