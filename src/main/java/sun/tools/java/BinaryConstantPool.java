package sun.tools.java;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

public final class BinaryConstantPool {

	BinaryConstantPool(final DataInput datainputstream) throws IOException {
		this.types = new byte[datainputstream.readUnsignedShort()];
		this.cpool = new Object[this.types.length];
		for (int i = 1; i < this.cpool.length; i++) {
			switch (this.types[i] = datainputstream.readByte()) {
			case 1: // '\001'
				this.cpool[i] = datainputstream.readUTF();
				break;

			case 3: // '\003'
				this.cpool[i] = new Integer(datainputstream.readInt());
				break;

			case 4: // '\004'
				this.cpool[i] = new Float(datainputstream.readFloat());
				break;

			case 5: // '\005'
				this.cpool[i++] = new Long(datainputstream.readLong());
				break;

			case 6: // '\006'
				this.cpool[i++] = new Double(datainputstream.readDouble());
				break;

			case 7: // '\007'
			case 8: // '\b'
				this.cpool[i] = new Integer(datainputstream.readUnsignedShort());
				break;

			case 9: // '\t'
			case 10: // '\n'
			case 11: // '\013'
			case 12: // '\f'
				this.cpool[i] = new Integer(datainputstream.readUnsignedShort() << 16 | datainputstream.readUnsignedShort());
				break;

			case 0: // '\0'
			case 2: // '\002'
			default:
				throw new ClassFormatError("invalid constant type: " + this.types[i]);
			}
		}

	}

	private int getInteger(final int i) {
		return i != 0 ? ((Number) this.cpool[i]).intValue() : 0;
	}

	public Object getValue(final int i) {
		return i != 0 ? this.cpool[i] : null;
	}

	public String getString(final int i) {
		return i != 0 ? (String) this.cpool[i] : null;
	}

	public Identifier getIdentifier(final int i) {
		return i != 0 ? Identifier.lookup(this.getString(i)) : null;
	}

	private ClassDeclaration getDeclarationFromName(final Environment environment, final int i) {
		return i != 0 ? environment.getClassDeclaration(Identifier.lookup(this.getString(i).replace('/', '.'))) : null;
	}

	public ClassDeclaration getDeclaration(final Environment environment, final int i) {
		return i != 0 ? this.getDeclarationFromName(environment, this.getInteger(i)) : null;
	}

	public Type getType(final int i) {
		return Type.tType(this.getString(i));
	}

	private int getConstantType(final int i) {
		return this.types[i];
	}

	private Object getConstant(final int i, final Environment environment) {
		final int j = this.getConstantType(i);
		switch (j) {
		case 3: // '\003'
		case 4: // '\004'
		case 5: // '\005'
		case 6: // '\006'
			return this.getValue(i);

		case 7: // '\007'
			return this.getDeclaration(environment, i);

		case 8: // '\b'
			return this.getString(this.getInteger(i));

		case 9: // '\t'
		case 10: // '\n'
		case 11: // '\013'
			try {
				final int k = this.getInteger(i);
				final ClassDefinition classdefinition = this.getDeclaration(environment, k >> 16).getClassDefinition(environment);
				final int l = this.getInteger(k & 0xffff);
				final Identifier identifier = this.getIdentifier(l >> 16);
				final Type type = this.getType(l & 0xffff);
				for (MemberDefinition memberdefinition = classdefinition.getFirstMatch(identifier); memberdefinition != null; memberdefinition = memberdefinition.getNextMatch()) {
					final Type type1 = memberdefinition.getType();
					if (j != 9 ? type1.equalArguments(type) : type1 == type) {
						return memberdefinition;
					}
				}

			} catch (final ClassNotFound ignored) {
			}
			return null;
		}
		throw new ClassFormatError("invalid constant type: " + j);
	}

	public List getDependencies(final Environment environment) {
		final List dependencies = new Vector();
		for (int i = 1; i < this.cpool.length;) {
			switch (this.types[i]) {
			case 7: // '\007'
				dependencies.add(this.getDeclarationFromName(environment, this.getInteger(i)));
				// fall through

			default:
				i++;
				break;
			}
		}

		return dependencies;
	}

	public int indexObject(final Object obj, final Environment environment) {
		if (this.indexHashObject == null) {
			this.createIndexHash(environment);
		}
		final Number integer = (Number) this.indexHashObject.get(obj);
		if (integer == null) {
			throw new IndexOutOfBoundsException("Cannot find object " + obj + " of type " + obj.getClass() + " in constant pool");
		}
		return integer.intValue();
	}

	public int indexString(final String s, final Environment environment) {
		if (this.indexHashObject == null) {
			this.createIndexHash(environment);
		}
		Integer integer = (Integer) this.indexHashAscii.get(s);
		if (integer == null) {
			if (this.MoreStuff == null) {
				this.MoreStuff = new Vector();
			}
			integer = new Integer(this.cpool.length + this.MoreStuff.size());
			this.MoreStuff.addElement(s);
			this.indexHashAscii.put(s, integer);
		}
		return integer.intValue();
	}

	private void createIndexHash(final Environment environment) {
		this.indexHashObject = new Hashtable();
		this.indexHashAscii = new Hashtable();
		for (int i = 1; i < this.cpool.length; i++) {
			if (this.types[i] == 1) {
				this.indexHashAscii.put(this.cpool[i], new Integer(i));
			} else {
				try {
					this.indexHashObject.put(this.getConstant(i, environment), new Integer(i));
				} catch (final ClassFormatError ignored) {
				}
			}
		}

	}

	public void write(final DataOutput dataoutputstream, final Environment environment) throws IOException {
		int i = this.cpool.length;
		if (this.MoreStuff != null) {
			i += this.MoreStuff.size();
		}
		dataoutputstream.writeShort(i);
		for (int j = 1; j < this.cpool.length; j++) {
			final byte byte0 = this.types[j];
			final Object obj = this.cpool[j];
			dataoutputstream.writeByte(byte0);
			switch (byte0) {
			case 1: // '\001'
				dataoutputstream.writeUTF((String) obj);
				break;

			case 3: // '\003'
				dataoutputstream.writeInt(((Number) obj).intValue());
				break;

			case 4: // '\004'
				dataoutputstream.writeFloat(((Number) obj).floatValue());
				break;

			case 5: // '\005'
				dataoutputstream.writeLong(((Number) obj).longValue());
				j++;
				break;

			case 6: // '\006'
				dataoutputstream.writeDouble(((Number) obj).doubleValue());
				j++;
				break;

			case 7: // '\007'
			case 8: // '\b'
				dataoutputstream.writeShort(((Number) obj).intValue());
				break;

			case 9: // '\t'
			case 10: // '\n'
			case 11: // '\013'
			case 12: // '\f'
				final int l = ((Number) obj).intValue();
				dataoutputstream.writeShort(l >> 16);
				dataoutputstream.writeShort(l & 0xffff);
				break;

			case 2: // '\002'
			default:
				throw new ClassFormatError("invalid constant type: " + this.types[j]);
			}
		}

		for (int k = this.cpool.length; k < i; k++) {
			final String s = (String) this.MoreStuff.elementAt(k - this.cpool.length);
			dataoutputstream.writeByte(1);
			dataoutputstream.writeUTF(s);
		}

	}

	private final byte types[];
	private final Object cpool[];
	private Hashtable indexHashObject;
	private Hashtable indexHashAscii;
	private Vector MoreStuff;
}
