package com.sun.tools.javac.v8.code;

import com.sun.tools.javac.v8.code.Scope.Entry;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.TypeSymbol;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;
import com.sun.tools.javac.v8.util.Util;

public class Type {
	public static class ErrorType extends Type {

		public Type constType(final Object obj) {
			return this;
		}

		public Type outer() {
			return this;
		}

		public Type elemtype() {
			return this;
		}

		public Type restype() {
			return this;
		}

		public Type bound() {
			return this;
		}

		public Type asSuper(final Symbol symbol) {
			return this;
		}

		public Type asOuterSuper(final Symbol symbol) {
			return this;
		}

		public Type asSub(final Symbol symbol) {
			return this;
		}

		public Type memberType(final Symbol symbol) {
			return this;
		}

		public boolean isErroneous() {
			return true;
		}

		public boolean isSameType(final Type type) {
			return true;
		}

		public boolean isSubType(final Type type) {
			return true;
		}

		public boolean isCastable(final Type type) {
			return true;
		}

		public boolean hasSameArgs(final Type type) {
			return false;
		}

		ErrorType(final TypeSymbol typesymbol) {
			super(18, typesymbol);
		}
	}

	public static class TypeVar extends Type {

		Type bound() {
			return this.bound;
		}

		public Type supertype() {
			return (this.bound.tsym.flags() & 0x200) == 0 ? this.bound : null;
		}

		public List interfaces() {
			return (this.bound.tsym.flags() & 0x200) != 0 ? Type.emptyList.prepend(this.bound) : null;
		}

		public Type asSuper(final Symbol symbol) {
			return this.bound.asSuper(symbol);
		}

		public Type asOuterSuper(final Symbol symbol) {
			return this.bound.asOuterSuper(symbol);
		}

		public Type classBound() {
			return this.bound.classBound();
		}

		public Type asSub(final Symbol symbol) {
			return this.bound.asSub(symbol);
		}

		public Type memberType(final Symbol symbol) {
			return this.bound.memberType(symbol);
		}

		public Type subst(List list, List list1) {
			for (; list.length() > list1.length(); list = list.tail) {
			}
			for (; list.tail != null && list1.tail != null; list1 = list1.tail) {
				if (this == list.head) {
					return (Type) list1.head;
				}
				list = list.tail;
			}

			return this;
		}

		public Type erasure() {
			return this.bound.erasure();
		}

		public TypeSymbol memberClass(final Name name) {
			return this.bound().memberClass(name);
		}

		final Type bound;

		TypeVar(final Type type, final TypeSymbol typesymbol) {
			super(14, typesymbol);
			this.bound = type;
		}

		public TypeVar(final Type type, final Name name, final Symbol symbol) {
			this(type, null);
			this.tsym = new TypeSymbol(0, name, this, symbol);
		}
	}

	public static class PackageType extends Type {

		public String toString() {
			return this.tsym.fullName().toString();
		}

		public boolean isSameType(final Type type) {
			return this == type;
		}

		PackageType(final TypeSymbol typesymbol) {
			super(13, typesymbol);
		}
	}

	public static class MethodType extends Type {

		public String toString() {
			return "(" + this.argtypes + ')' + this.restype;
		}

		public String toJava() {
			return "(" + this.argtypes + ')' + this.restype;
		}

		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof MethodType)) {
				return false;
			}
			final MethodType methodtype = (MethodType) obj;
			List list = this.argtypes;
			List list1;
			for (list1 = methodtype.argtypes; list.tail != null && list1.tail != null && ((Type) list.head).equals(list1.head); list1 = list1.tail) {
				list = list.tail;
			}

			return list.tail == null && list1.tail == null && this.restype.equals(methodtype.restype);
		}

		public int hashCode() {
			int i = 12;
			for (List list = this.argtypes; list.tail != null; list = list.tail) {
				i = (i << 5) + ((Type) list.head).hashCode();
			}

			return (i << 5) + this.restype.hashCode();
		}

		public List argtypes() {
			return this.argtypes;
		}

		public Type restype() {
			return this.restype;
		}

		public List thrown() {
			return this.thrown;
		}

		public Type subst(final List list, final List list1) {
			final List list2 = Type.subst(this.argtypes, list, list1);
			final Type type = this.restype.subst(list, list1);
			return list2 == this.argtypes && type == this.restype ? this : new MethodType(list2, type, this.thrown);
		}

		public boolean isErroneous() {
			return Type.isErroneous(this.argtypes) || this.restype.isErroneous();
		}

		public Type erasure() {
			final List list = Type.erasure(this.argtypes);
			final Type type = this.restype.erasure();
			return list == this.argtypes && type == this.restype ? this : new MethodType(list, type, this.thrown);
		}

		public int occCount(final Type type) {
			return type == this ? 1 : Type.occCount(this.argtypes, type) + this.restype.occCount(type);
		}

		public MethodType asMethodType() {
			return this;
		}

		public boolean hasSameArgs(final Type type) {
			return type.tag == 12 && Type.isSameTypes(this.argtypes, type.argtypes());
		}

		public boolean isSameType(final Type type) {
			return this.hasSameArgs(type) && this.restype.isSameType(type.restype());
		}

		public void complete() {
			for (List list = this.argtypes; list.nonEmpty(); list = list.tail) {
				((Type) list.head).complete();
			}

			this.restype.complete();
			for (List list1 = this.thrown; list1.nonEmpty(); list1 = list1.tail) {
				((Symbol) list1.head).complete();
			}

		}

		public List argtypes;
		public Type restype;
		public List thrown;
		private static final ClassSymbol methodClass;

		static {
			methodClass = new ClassSymbol(1, Name.fromString("Method"), Symbol.noSymbol);
		}

		public MethodType(final List list, final Type type, final List list1) {
			super(12, methodClass);
			this.argtypes = list;
			this.restype = type;
			this.thrown = list1;
		}
	}

	public static class ArrayType extends Type {

		public String toString() {
			return this.elemtype + "[]";
		}

		public String toJava() {
			return this.toString();
		}

		public boolean equals(final Object obj) {
			return this == obj || obj instanceof ArrayType && this.elemtype.equals(((ArrayType) obj).elemtype);
		}

		public int hashCode() {
			return 352 + this.elemtype.hashCode();
		}

		public Type elemtype() {
			return this.elemtype;
		}

		public int dimensions() {
			int i = 0;
			for (Object obj = this; ((Type) obj).tag == 11; obj = ((Type) obj).elemtype()) {
				i++;
			}

			return i;
		}

		public List allparams() {
			return this.elemtype.allparams();
		}

		public Type subst(final List list, final List list1) {
			final Type type = this.elemtype.subst(list, list1);
			return type == this.elemtype ? this : new ArrayType(type);
		}

		public boolean isErroneous() {
			return this.elemtype.isErroneous();
		}

		public boolean isParameterized() {
			return this.elemtype.isParameterized();
		}

		public boolean isRaw() {
			return this.elemtype.isRaw();
		}

		public Type erasure() {
			final Type type = this.elemtype.erasure();
			return type == this.elemtype ? this : new ArrayType(type);
		}

		public Type unerasure() {
			final Type type = this.elemtype.unerasure();
			return type == this.elemtype ? this : new ArrayType(type);
		}

		public int occCount(final Type type) {
			return type == this ? 1 : this.elemtype.occCount(type);
		}

		public Type asSuper(final Symbol symbol) {
			return this.isSubType(symbol.type) ? symbol.type : null;
		}

		public Type asOuterSuper(final Symbol symbol) {
			return this.isSubType(symbol.type) ? symbol.type : null;
		}

		public boolean isSameType(final Type type) {
			return this == type || type.tag == 18 || type.tag == 11 && this.elemtype.isSameType(type.elemtype());
		}

		public boolean isGenType(final Type type) {
			return this == type || type.tag == 18 || type.tag == 11 && this.elemtype.isGenType(type.elemtype());
		}

		public boolean isSubType(final Type type) {
			if (this == type || type.tag == 18) {
				return true;
			}
			if (type.tag == 11) {
				return this.elemtype.tag <= 8 ? this.elemtype.isSameType(type.elemtype()) : this.elemtype.isSubType(type.elemtype());
			}
			if (type.tag == 10) {
				final Name name = type.tsym.fullName();
				return name == Names.java_lang_Object || name == Names.java_lang_Cloneable || name == Names.java_io_Serializable;
			}
			return false;
		}

		public boolean isCastable(final Type type) {
			return type.tag == 18 || type.tag == 10 && this.isSubType(type) || type.tag == 11 && (this.elemtype().tag > 8 ? this.elemtype().isCastable(type.elemtype()) : this.elemtype().tag == type.elemtype().tag);
		}

		public void complete() {
			this.elemtype.complete();
		}

		public final Type elemtype;
		public static final ClassSymbol arrayClass;

		static {
			arrayClass = new ClassSymbol(1, Name.fromString("Array"), Symbol.noSymbol);
		}

		public ArrayType(final Type type) {
			super(11, arrayClass);
			this.elemtype = type;
		}
	}

	public static class ClassType extends Type {

		public Type constType(final Object obj) {
			final Type classtype = new ClassType(this.outer_field, this.typarams_field, this.tsym);
			classtype.constValue = obj;
			return classtype;
		}

		public String toString() {
			final StringBuffer stringbuffer = new StringBuffer();
			if (this.outer().tag == 10 && ((Symbol) this.tsym).owner.kind == 2) {
				stringbuffer.append(this.outer());
				stringbuffer.append('.');
				stringbuffer.append(className(this.tsym, false));
			} else {
				stringbuffer.append(className(this.tsym, true));
			}
			if (this.typarams().nonEmpty()) {
				stringbuffer.append('<');
				stringbuffer.append(this.typarams());
				stringbuffer.append('>');
			}
			return stringbuffer.toString();
		}

		private static String className(final Symbol symbol, final boolean flag) {
			if (symbol.name.len == 0) {
				return "<anonymous " + (symbol.type.interfaces().nonEmpty() ? symbol.type.interfaces().head : symbol.type.supertype()) + '>' + (Type.moreInfo ? String.valueOf(symbol.hashCode()) : "");
			}
			return flag ? symbol.fullName().toString() : symbol.name.toString();
		}

		public String toJava() {
			final StringBuffer stringbuffer = new StringBuffer();
			if (this.outer().tag == 10 && ((Symbol) this.tsym).owner.kind == 2) {
				stringbuffer.append(this.outer());
				stringbuffer.append('.');
				stringbuffer.append(javaClassName(this.tsym, false));
			} else {
				stringbuffer.append(javaClassName(this.tsym, true));
			}
			if (this.typarams().nonEmpty()) {
				stringbuffer.append('<');
				stringbuffer.append(this.typarams());
				stringbuffer.append('>');
			}
			return stringbuffer.toString();
		}

		private static String javaClassName(final Symbol symbol, final boolean flag) {
			if (symbol.name.len == 0) {
				String s = symbol.type.interfaces().nonEmpty() ? Log.getLocalizedString("anonymous.class", ((Type) symbol.type.interfaces().head).toJava()) : Log.getLocalizedString("anonymous.class", symbol.type.supertype().toJava());
				if (Type.moreInfo) {
					s += String.valueOf(symbol.hashCode());
				}
				return s;
			}
			return flag ? symbol.fullName().toString() : symbol.name.toString();
		}

		public List typarams() {
			if (this.typarams_field == null) {
				this.tsym.complete();
				this.typarams_field = ((Symbol) this.tsym).type.typarams();
			}
			return this.typarams_field;
		}

		public Type outer() {
			if (this.outer_field == null) {
				this.tsym.complete();
				this.outer_field = ((Symbol) this.tsym).type.outer();
			}
			return this.outer_field;
		}

		public Type supertype() {
			if (this.supertype_field == null) {
				this.tsym.complete();
				final Type type = ((ClassType) ((Symbol) this.tsym).type).supertype_field;
				if (type == null) {
					this.supertype_field = Type.noType;
				} else if (this == ((Symbol) this.tsym).type) {
					this.supertype_field = type;
				} else {
					final List list = this.classBound().allparams();
					final List list1 = ((Symbol) this.tsym).type.allparams();
					this.supertype_field = list.isEmpty() ? type.erasure() : type.subst(list1, list);
				}
			}
			return this.supertype_field;
		}

		public List interfaces() {
			if (this.interfaces_field == null) {
				this.tsym.complete();
				final List list = ((ClassType) ((Symbol) this.tsym).type).interfaces_field;
				if (list == null) {
					this.interfaces_field = Type.emptyList;
				} else if (this == ((Symbol) this.tsym).type) {
					this.interfaces_field = list;
				} else {
					final List list1 = this.allparams();
					final List list2 = ((Symbol) this.tsym).type.allparams();
					this.interfaces_field = list1.isEmpty() ? Type.erasure(list) : Type.subst(list, list2, list1);
				}
			}
			return this.interfaces_field;
		}

		public List allparams() {
			if (this.allparams_field == null) {
				this.allparams_field = this.typarams().prepend(this.outer().allparams());
			}
			return this.allparams_field;
		}

		public Type asSuper(final Symbol symbol) {
			if (this.tsym == symbol) {
				return this;
			}
			final Type type = this.supertype();
			if (type.tag == 10) {
				final Type type1 = type.asSuper(symbol);
				if (type1 != null) {
					return type1;
				}
			}
			if ((symbol.flags() & 0x200) != 0) {
				for (List list = this.interfaces(); list.nonEmpty(); list = list.tail) {
					final Type type2 = ((Type) list.head).asSuper(symbol);
					if (type2 != null) {
						return type2;
					}
				}

			}
			return null;
		}

		public Type asOuterSuper(final Symbol symbol) {
			Object obj = this;
			do {
				final Type type = ((Type) obj).asSuper(symbol);
				if (type != null) {
					return type;
				}
				obj = ((Type) obj).outer();
			} while (((Type) obj).tag == 10);
			return null;
		}

		Type classBound() {
			final Type type = this.outer().classBound();
			return type != this.outer_field ? new ClassType(type, this.typarams(), this.tsym) : this;
		}

		Type asSub(final Symbol symbol) {
			if (this.tsym == symbol) {
				return this;
			}
			final Type type = symbol.type.asSuper(this.tsym);
			if (type == null) {
				return null;
			}
			final Type type1 = symbol.type.subst(type.allparams(), this.allparams());
			if (!type1.isSubType(this)) {
				return null;
			}
			for (List list = symbol.type.allparams(); list.nonEmpty(); list = list.tail) {
				if (type1.occCount((Type) list.head) != 0) {
					return type1.erasure();
				}
			}

			return type1;
		}

		public Type memberType(final Symbol symbol) {
			final Symbol symbol1 = symbol.owner;
			final int i = symbol.flags();
			if (((i & 8) == 0 || (i & 0x200) != 0) && symbol1.type.isParameterized()) {
				final Type type = this.asOuterSuper(symbol1);
				if (type != null) {
					final List list = symbol1.type.allparams();
					final List list1 = type.allparams();
					if (list.nonEmpty()) {
						return list1.isEmpty() ? symbol.type.erasure() : symbol.type.subst(list, list1);
					}
				}
			}
			return symbol.type;
		}

		public Type subst(final List list, final List list1) {
			final Type type = this.outer();
			final List list2 = this.typarams();
			final List list3 = Type.subst(list2, list, list1);
			final Type type1 = type.subst(list, list1);
			return list3 == list2 && type1 == type ? this : new ClassType(type1, list3, this.tsym);
		}

		public boolean isErroneous() {
			return this.outer().isErroneous() || Type.isErroneous(this.typarams());
		}

		public boolean isParameterized() {
			return this.allparams().tail != null;
		}

		public boolean isRaw() {
			return this != ((Symbol) this.tsym).type && ((Symbol) this.tsym).type.allparams().nonEmpty() && this.allparams().isEmpty() && (((Symbol) this.tsym).type.typarams().nonEmpty() && this.typarams().isEmpty() || this.outer().isRaw());
		}

		public Type erasure() {
			return this.tsym.erasure();
		}

		public Type unerasure() {
			if (this.isRaw()) {
				List list = this.typarams();
				if (list.isEmpty()) {
					for (List list1 = ((Symbol) this.tsym).type.typarams(); list1.nonEmpty(); list1 = list1.tail) {
						list = list.prepend(Type.botType);
					}

				}
				return new ClassType(this.outer().unerasure(), list, this.tsym);
			}
			return this;
		}

		public int occCount(final Type type) {
			if (type == this) {
				return 1;
			}
			return this.isParameterized() ? this.outer().occCount(type) + Type.occCount(this.typarams(), type) : 0;
		}

		public boolean isSameType(final Type type) {
			return this == type || type.tag == 18 || this.tsym == type.tsym && this.outer().isSameType(type.outer()) && Type.isSameTypes(this.typarams(), type.typarams());
		}

		public boolean isGenType(final Type type) {
			return this == type || type.tag == 18 || this.tsym == type.tsym && this.outer().isGenType(type.outer()) && Type.isGenTypes(this.typarams(), type.typarams());
		}

		public boolean isSubType(final Type type) {
			if (this == type || type.tag == 18) {
				return true;
			}
			if (this.tsym == type.tsym) {
				return (!type.isParameterized() || Type.isGenTypes(this.typarams(), type.typarams())) && this.outer().isSubType(type.outer());
			}
			if ((type.tsym.flags() & 0x200) != 0) {
				for (List list = this.interfaces(); list.nonEmpty(); list = list.tail) {
					if (((Type) list.head).isSubType(type)) {
						return true;
					}
				}

			}
			final Type type1 = this.supertype();
			return type1.tag == 10 && type1.isSubType(type);
		}

		public TypeSymbol memberClass(final Name name) {
			for (Object obj = this; ((Type) obj).tag == 10; obj = ((Type) obj).supertype()) {
				Entry entry;
				for (entry = ((Type) obj).tsym.members().lookup(name); entry.scope != null && entry.sym.kind != 2; entry = entry.next()) {
				}
				if (entry.scope != null) {
					return (TypeSymbol) entry.sym;
				}
			}

			return null;
		}

		public boolean isCastable(final Type type) {
			return type.tag == 18 || (type.tag == 10 || type.tag == 11) && (this.isSubType(type) || type.isSubType(this) && (type.tag == 11 || ((Symbol) type.tsym).type == type || type.isSameType(this.asSub(type.tsym))) || type.tag == 10 && type.allparams().isEmpty() && ((type.tsym.flags() & 0x200) != 0 && (this.tsym.flags() & 0x10) == 0 || (this.tsym.flags() & 0x200) != 0 && (type.tsym.flags() & 0x10) == 0));
		}

		public void complete() {
			if (((Symbol) this.tsym).completer != null) {
				this.tsym.complete();
			}
		}

		public Type outer_field;
		public List typarams_field;
		List allparams_field;
		public Type supertype_field;
		public List interfaces_field;

		public ClassType(final Type type, final List list, final TypeSymbol typesymbol) {
			super(10, typesymbol);
			this.outer_field = type;
			this.typarams_field = list;
			this.allparams_field = null;
			this.supertype_field = null;
			this.interfaces_field = null;
		}
	}

	Type(final int i, final TypeSymbol typesymbol) {
		this.constValue = null;
		this.tag = i;
		this.tsym = typesymbol;
	}

	public Type constType(final Object obj) {
		Util.assertTrue(this.tag <= 8);
		final Type type = new Type(this.tag, this.tsym);
		type.constValue = obj;
		return type;
	}

	public Type baseType() {
		return this.constValue == null ? this : ((Symbol) this.tsym).type;
	}

	public String toString() {
		String s = this.tsym != null && ((Symbol) this.tsym).name != null ? ((Symbol) this.tsym).name.toString() : "null";
		if (moreInfo && this.tag == 14) {
			s += this.hashCode();
		}
		return s;
	}

	public String toJava() {
		String s = this.tsym != null && ((Symbol) this.tsym).name != null ? ((Symbol) this.tsym).name.toJava() : "null";
		if (moreInfo && this.tag == 14) {
			s += this.hashCode();
		}
		return s;
	}

	public static String toJavaList(final List list) {
		if (list.isEmpty()) {
			return "";
		}
		final StringBuffer stringbuffer = new StringBuffer();
		stringbuffer.append(((Type) list.head).toJava());
		for (List list1 = list.tail; list1.nonEmpty(); list1 = list1.tail) {
			stringbuffer.append(',');
			stringbuffer.append(((Type) list1.head).toJava());
		}

		return stringbuffer.toString();
	}

	public String stringValue() {
		if (this.tag == 8) {
			return ((Number) this.constValue).intValue() != 0 ? "true" : "false";
		}
		return this.tag == 2 ? String.valueOf((char) ((Number) this.constValue).intValue()) : this.constValue.toString();
	}

	public List typarams() {
		return emptyList;
	}

	public Type outer() {
		return null;
	}

	public Type elemtype() {
		return null;
	}

	public int dimensions() {
		return 0;
	}

	public List argtypes() {
		return emptyList;
	}

	public Type restype() {
		return null;
	}

	public List thrown() {
		return ClassSymbol.emptyList;
	}

	Type bound() {
		return null;
	}

	public Type supertype() {
		return null;
	}

	public List interfaces() {
		return emptyList;
	}

	public List allparams() {
		return emptyList;
	}

	public Type asSuper(final Symbol symbol) {
		return null;
	}

	public Type asOuterSuper(final Symbol symbol) {
		return null;
	}

	Type classBound() {
		return this;
	}

	Type asSub(final Symbol symbol) {
		return null;
	}

	public Type memberType(final Symbol symbol) {
		return symbol.type;
	}

	public Type subst(final List list, final List list1) {
		return this;
	}

	static List subst(final List list, final List list1, final List list2) {
		if (list.tail != null) {
			final Type type = ((Type) list.head).subst(list1, list2);
			final List list3 = subst(list.tail, list1, list2);
			if (type != list.head || list3 != list.tail) {
				return list3.prepend(type);
			}
		}
		return list;
	}

	public boolean isErroneous() {
		return false;
	}

	public static boolean isErroneous(final List list) {
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			if (((Type) list1.head).isErroneous()) {
				return true;
			}
		}

		return false;
	}

	public boolean isParameterized() {
		return false;
	}

	public boolean isRaw() {
		return false;
	}

	public static boolean isRaw(final List list) {
		List list1;
		for (list1 = list; list1.nonEmpty() && !((Type) list1.head).isRaw(); list1 = list1.tail) {
		}
		return list1.nonEmpty();
	}

	public Type erasure() {
		return this;
	}

	static List erasure(final List list) {
		if (list.nonEmpty()) {
			final List list1 = erasure(list.tail);
			final Type type = ((Type) list.head).erasure();
			if (list1 != list.tail || type != list.head) {
				return list1.prepend(type);
			}
		}
		return list;
	}

	public Type unerasure() {
		return this;
	}

	public static List unerasure(final List list) {
		if (list.nonEmpty()) {
			final List list1 = unerasure(list.tail);
			final Type type = ((Type) list.head).unerasure();
			if (list1 != list.tail || type != list.head) {
				return list1.prepend(type);
			}
		}
		return list;
	}

	int occCount(final Type type) {
		return type != this ? 0 : 1;
	}

	static int occCount(final List list, final Type type) {
		int i = 0;
		for (List list1 = list; list1.tail != null; list1 = list1.tail) {
			i += ((Type) list1.head).occCount(type);
		}

		return i;
	}

	public boolean isSameType(final Type type) {
		if (this == type || type.tag == 18) {
			return true;
		}
		switch (this.tag) {
		case 1: // '\001'
		case 2: // '\002'
		case 3: // '\003'
		case 4: // '\004'
		case 5: // '\005'
		case 6: // '\006'
		case 7: // '\007'
		case 8: // '\b'
		case 9: // '\t'
		case 16: // '\020'
		case 17: // '\021'
			return this.tag == type.tag;

		case 14: // '\016'
			return false;

		case 10: // '\n'
		case 11: // '\013'
		case 12: // '\f'
		case 13: // '\r'
		case 15: // '\017'
		default:
			throw new InternalError("isSameType " + this.tag);
		}
	}

	static boolean isSameTypes(List list, List list1) {
		for (; list.tail != null && list1.tail != null && ((Type) list.head).isSameType((Type) list1.head); list1 = list1.tail) {
			list = list.tail;
		}

		return list.tail == null && list1.tail == null;
	}

	boolean isGenType(final Type type) {
		return this.isSameType(type) || this.tag == 16 && this.isSubType(type);
	}

	static boolean isGenTypes(List list, List list1) {
		for (; list.tail != null && list1.tail != null && ((Type) list.head).isGenType((Type) list1.head); list1 = list1.tail) {
			list = list.tail;
		}

		return list.tail == null && list1.tail == null;
	}

	public boolean isSubType(final Type type) {
		if (this == type || type.tag == 18) {
			return true;
		}
		switch (this.tag) {
		case 1: // '\001'
		case 2: // '\002'
			return this.tag == type.tag || this.tag + 2 <= type.tag && type.tag <= 7;

		case 3: // '\003'
		case 4: // '\004'
		case 5: // '\005'
		case 6: // '\006'
		case 7: // '\007'
			return this.tag <= type.tag && type.tag <= 7;

		case 8: // '\b'
		case 9: // '\t'
			return this.tag == type.tag;

		case 14: // '\016'
			return this.bound().isSubType(type);

		case 16: // '\020'
			return type.tag == 16 || type.tag == 10 || type.tag == 11 || type.tag == 14;

		case 10: // '\n'
		case 11: // '\013'
		case 12: // '\f'
		case 13: // '\r'
		case 15: // '\017'
		default:
			throw new InternalError("isSubType " + this.tag);
		}
	}

	public static boolean isSubTypes(List list, List list1) {
		for (; list.tail != null && list1.tail != null && ((Type) list.head).isSubType((Type) list1.head); list1 = list1.tail) {
			list = list.tail;
		}

		return list.tail == null && list1.tail == null;
	}

	public boolean isAssignable(final Type type) {
		if (this.tag <= 4 && this.constValue != null) {
			final int i = ((Number) this.constValue).intValue();
			switch (type.tag) {
			default:
				break;

			case 1: // '\001'
				if (i >= -128 && i <= 127) {
					return true;
				}
				break;

			case 2: // '\002'
				if (i >= 0 && i <= 65535) {
					return true;
				}
				break;

			case 3: // '\003'
				if (i >= -32768 && i <= 32767) {
					return true;
				}
				break;

			case 4: // '\004'
				return true;
			}
		}
		return this.isSubType(type);
	}

	public boolean isCastable(final Type type) {
		if (type.tag == 18) {
			return true;
		}
		switch (this.tag) {
		case 1: // '\001'
		case 2: // '\002'
		case 3: // '\003'
		case 4: // '\004'
		case 5: // '\005'
		case 6: // '\006'
		case 7: // '\007'
			return type.tag <= 7;

		case 8: // '\b'
			return type.tag == 8;

		case 9: // '\t'
			return false;

		case 14: // '\016'
			return this.bound().isCastable(type);

		case 16: // '\020'
			return this.isSubType(type);

		case 10: // '\n'
		case 11: // '\013'
		case 12: // '\f'
		case 13: // '\r'
		case 15: // '\017'
		default:
			throw new InternalError();
		}
	}

	TypeSymbol memberClass(final Name name) {
		return null;
	}

	public MethodType asMethodType() {
		throw new InternalError();
	}

	public boolean hasSameArgs(final Type type) {
		throw new InternalError();
	}

	void complete() {
	}

	private static void initType(final Type type, final TypeSymbol classsymbol) {
		type.tsym = classsymbol;
		typeOfTag[type.tag] = type;
	}

	private static void initType(final Type type, final String s) {
		initType(type, new ClassSymbol(1, Name.fromString(s), type, Symbol.rootPackage));
	}

	private static void initType(final Type type, final String s, final String s1) {
		initType(type, s);
		boxedName[type.tag] = Name.fromString("java.lang." + s1);
	}

	static void init() {
		initType(byteType, "byte", "Byte");
		initType(shortType, "short", "Short");
		initType(charType, "char", "Character");
		initType(intType, "int", "Integer");
		initType(longType, "long", "Long");
		initType(floatType, "float", "Float");
		initType(doubleType, "double", "Double");
		initType(booleanType, "boolean", "Boolean");
		initType(voidType, "void", "Void");
		initType(botType, "<null>");
		initType(errType, Symbol.errSymbol);
	}

	public static boolean moreInfo;
	public final int tag;
	public TypeSymbol tsym;
	public Object constValue;
	public static final List emptyList = new List();
	public static final Type[] typeOfTag = new Type[19];
	public static final Name[] boxedName = new Name[19];
	public static final Type byteType = new Type(1, null);
	public static final Type charType = new Type(2, null);
	public static final Type shortType = new Type(3, null);
	public static final Type intType = new Type(4, null);
	public static final Type longType = new Type(5, null);
	public static final Type floatType = new Type(6, null);
	public static final Type doubleType = new Type(7, null);
	public static final Type booleanType = new Type(8, null);
	public static final Type voidType = new Type(9, null);
	public static final Type noType = new Type(17, null);
	public static final Type botType = new Type(16, null);
	public static final Type errType = new ErrorType(null);
}
