package sun.tools.asm;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

import sun.tools.java.ClassDeclaration;
import sun.tools.java.Environment;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Type;
import sun.tools.tree.StringExpression;

public final class ConstantPool {

	public ConstantPool() {
		this.hash = new Hashtable(101);
	}

	public int index(final Object obj) {
		return ((ConstantPoolData) this.hash.get(obj)).index;
	}

	public void put(final Object obj) {
		Object obj1 = this.hash.get(obj);
		if (obj1 == null) {
			if (obj instanceof String) {
				obj1 = new StringConstantData(this, (String) obj);
			} else if (obj instanceof StringExpression) {
				obj1 = new StringExpressionConstantData(this, (StringExpression) obj);
			} else if (obj instanceof ClassDeclaration) {
				obj1 = new ClassConstantData(this, (ClassDeclaration) obj);
			} else if (obj instanceof Type) {
				obj1 = new ClassConstantData(this, (Type) obj);
			} else if (obj instanceof MemberDefinition) {
				obj1 = new FieldConstantData(this, (MemberDefinition) obj);
			} else if (obj instanceof NameAndTypeData) {
				obj1 = new NameAndTypeConstantData(this, (NameAndTypeData) obj);
			} else if (obj instanceof Number) {
				obj1 = new NumberConstantData(this, (Number) obj);
			}
			this.hash.put(obj, obj1);
		}
	}

	public void write(final Environment environment, final DataOutputStream dataoutputstream) throws IOException {
		final ConstantPoolData aconstantpooldata[] = new ConstantPoolData[this.hash.size()];
		final String as[] = new String[aconstantpooldata.length];
		int j = 0;
		for (int k = 0; k < 5; k++) {
			final int l = j;
			for (final Iterator iterator = this.hash.values().iterator(); iterator.hasNext();) {
				final ConstantPoolData constantpooldata1 = (ConstantPoolData) iterator.next();
				if (constantpooldata1.order() == k) {
					as[j] = sortKey(constantpooldata1);
					aconstantpooldata[j++] = constantpooldata1;
				}
			}

			xsort(aconstantpooldata, as, l, j - 1);
		}

		int i = 1;
		for (int i1 = 0; i1 < aconstantpooldata.length; i1++) {
			final ConstantPoolData constantpooldata = aconstantpooldata[i1];
			constantpooldata.index = i;
			i += constantpooldata.width();
		}

		dataoutputstream.writeShort(i);
		for (int j1 = 0; j1 < j; j1++) {
			aconstantpooldata[j1].write(environment, dataoutputstream, this);
		}

	}

	private static String sortKey(final ConstantPoolData constantpooldata) {
		if (constantpooldata instanceof NumberConstantData) {
			final Number number = ((NumberConstantData) constantpooldata).num;
			final String s = number.toString();
			byte byte0 = 3;
			if (number instanceof Integer) {
				byte0 = 0;
			} else if (number instanceof Float) {
				byte0 = 1;
			} else if (number instanceof Long) {
				byte0 = 2;
			}
			return "\0" + (char) (s.length() + byte0 << 8) + s;
		}
		if (constantpooldata instanceof StringExpressionConstantData) {
			return (String) ((StringExpressionConstantData) constantpooldata).str.getValue();
		}
		if (constantpooldata instanceof FieldConstantData) {
			final MemberDefinition memberdefinition = ((FieldConstantData) constantpooldata).field;
			return memberdefinition.getName() + " " + memberdefinition.getType().getTypeSignature() + ' ' + memberdefinition.getClassDeclaration().getName();
		}
		if (constantpooldata instanceof NameAndTypeConstantData) {
			return ((NameAndTypeConstantData) constantpooldata).name + ' ' + ((NameAndTypeConstantData) constantpooldata).type;
		}
		return constantpooldata instanceof ClassConstantData ? ((ClassConstantData) constantpooldata).name : ((StringConstantData) constantpooldata).str;
	}

	private static void xsort(final ConstantPoolData aconstantpooldata[], final String as[], final int i, final int j) {
		if (i >= j) {
			return;
		}
		final String s = as[i];
		int k = i;
		int l;
		for (l = j; k < l;) {
			while (k <= j && as[k].compareTo(s) <= 0) {
				k++;
			}
			for (; l >= i && as[l].compareTo(s) > 0; l--) {
			}
			if (k < l) {
				final ConstantPoolData constantpooldata = aconstantpooldata[k];
				final String s1 = as[k];
				aconstantpooldata[k] = aconstantpooldata[l];
				aconstantpooldata[l] = constantpooldata;
				as[k] = as[l];
				as[l] = s1;
			}
		}

		final int i1 = l;
		final ConstantPoolData constantpooldata1 = aconstantpooldata[i];
		final String s2 = as[i];
		aconstantpooldata[i] = aconstantpooldata[i1];
		aconstantpooldata[i1] = constantpooldata1;
		as[i] = as[i1];
		as[i1] = s2;
		xsort(aconstantpooldata, as, i, i1 - 1);
		xsort(aconstantpooldata, as, i1 + 1, j);
	}

	private final Hashtable hash;
}
