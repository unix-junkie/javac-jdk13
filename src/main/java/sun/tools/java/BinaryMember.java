package sun.tools.java;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import sun.tools.tree.BooleanExpression;
import sun.tools.tree.DoubleExpression;
import sun.tools.tree.FloatExpression;
import sun.tools.tree.IntExpression;
import sun.tools.tree.LocalMember;
import sun.tools.tree.LongExpression;
import sun.tools.tree.Node;
import sun.tools.tree.StringExpression;

public final class BinaryMember extends MemberDefinition {

	public BinaryMember(final ClassDefinition classdefinition, final int i, final Type type, final Identifier identifier, final BinaryAttribute binaryattribute) {
		super(0L, classdefinition, i, type, identifier, null, null);
		this.isConstantCache = false;
		this.isConstantCached = false;
		this.atts = binaryattribute;
		if (this.getAttribute(Constants.idDeprecated) != null) {
			this.modifiers |= Constants.M_DEPRECATED;
		}
		if (this.getAttribute(Constants.idSynthetic) != null) {
			this.modifiers |= 0x80000;
		}
	}

	public BinaryMember(final ClassDefinition classdefinition) {
		super(classdefinition);
		this.isConstantCache = false;
		this.isConstantCached = false;
	}

	public boolean isInlineable(final Environment environment, final boolean flag) {
		return this.isConstructor() && this.getClassDefinition().getSuperClass() == null;
	}

	public List getArguments() {
		if (this.isConstructor() && this.getClassDefinition().getSuperClass() == null) {
			final Vector vector = new Vector();
			vector.addElement(new LocalMember(0L, this.getClassDefinition(), 0, this.getClassDefinition().getType(), Constants.idThis));
			return vector;
		}
		return null;
	}

	public ClassDeclaration[] getExceptions(final Environment environment) {
		if (!this.isMethod() || this.exp != null) {
			return this.exp;
		}
		final byte abyte0[] = this.getAttribute(Constants.idExceptions);
		if (abyte0 == null) {
			return new ClassDeclaration[0];
		}
		try {
			final BinaryConstantPool binaryconstantpool = ((BinaryClass) this.getClassDefinition()).getConstants();
			final DataInput datainputstream = new DataInputStream(new ByteArrayInputStream(abyte0));
			final int i = datainputstream.readUnsignedShort();
			this.exp = new ClassDeclaration[i];
			for (int j = 0; j < i; j++) {
				this.exp[j] = binaryconstantpool.getDeclaration(environment, datainputstream.readUnsignedShort());
			}

			return this.exp;
		} catch (final IOException ioexception) {
			throw new CompilerError(ioexception);
		}
	}

	public String getDocumentation() {
		if (this.documentation != null) {
			return this.documentation;
		}
		final byte abyte0[] = this.getAttribute(Constants.idDocumentation);
		if (abyte0 == null) {
			return null;
		}
		try {
			return this.documentation = new DataInputStream(new ByteArrayInputStream(abyte0)).readUTF();
		} catch (final IOException ioexception) {
			throw new CompilerError(ioexception);
		}
	}

	public boolean isConstant() {
		if (!this.isConstantCached) {
			this.isConstantCache = this.isFinal() && this.isVariable() && this.getAttribute(Constants.idConstantValue) != null;
			this.isConstantCached = true;
		}
		return this.isConstantCache;
	}

	public Node getValue(final Environment environment) {
		if (this.isMethod()) {
			return null;
		}
		if (!this.isFinal()) {
			return null;
		}
		if (this.getValue() != null) {
			return this.getValue();
		}
		final byte abyte0[] = this.getAttribute(Constants.idConstantValue);
		if (abyte0 == null) {
			return null;
		}
		try {
			final BinaryConstantPool binaryconstantpool = ((BinaryClass) this.getClassDefinition()).getConstants();
			final Object obj = binaryconstantpool.getValue(new DataInputStream(new ByteArrayInputStream(abyte0)).readUnsignedShort());
			switch (this.getType().getTypeCode()) {
			case 0: // '\0'
				this.setValue(new BooleanExpression(0L, ((Number) obj).intValue() != 0));
				break;

			case 1: // '\001'
			case 2: // '\002'
			case 3: // '\003'
			case 4: // '\004'
				this.setValue(new IntExpression(0L, ((Number) obj).intValue()));
				break;

			case 5: // '\005'
				this.setValue(new LongExpression(0L, ((Number) obj).longValue()));
				break;

			case 6: // '\006'
				this.setValue(new FloatExpression(0L, ((Number) obj).floatValue()));
				break;

			case 7: // '\007'
				this.setValue(new DoubleExpression(0L, ((Number) obj).doubleValue()));
				break;

			case 10: // '\n'
				this.setValue(new StringExpression(0L, (String) binaryconstantpool.getValue(((Number) obj).intValue())));
				break;
			}
			return this.getValue();
		} catch (final IOException ioexception) {
			throw new CompilerError(ioexception);
		}
	}

	public byte[] getAttribute(final Identifier identifier) {
		for (BinaryAttribute binaryattribute = this.atts; binaryattribute != null; binaryattribute = binaryattribute.next) {
			if (binaryattribute.name.equals(identifier)) {
				return binaryattribute.data;
			}
		}

		return null;
	}

	public boolean deleteAttribute(final Identifier identifier) {
		boolean flag;
		for (flag = false; this.atts.name.equals(identifier); flag = true) {
			this.atts = this.atts.next;
		}

		BinaryAttribute binaryattribute2;
		for (BinaryAttribute binaryattribute = this.atts; binaryattribute != null; binaryattribute = binaryattribute2) {
			binaryattribute2 = binaryattribute.next;
			if (binaryattribute2 != null && binaryattribute2.name.equals(identifier)) {
				binaryattribute.next = binaryattribute2.next;
				binaryattribute2 = binaryattribute2.next;
				flag = true;
			}
		}

		for (BinaryAttribute binaryattribute1 = this.atts; binaryattribute1 != null; binaryattribute1 = binaryattribute1.next) {
			if (binaryattribute1.name.equals(identifier)) {
				throw new InternalError("Found attribute " + identifier);
			}
		}

		return flag;
	}

	BinaryAttribute atts;
	private boolean isConstantCache;
	private boolean isConstantCached;
}
