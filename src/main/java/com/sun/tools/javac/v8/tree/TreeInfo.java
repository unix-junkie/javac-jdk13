package com.sun.tools.javac.v8.tree;

import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.tree.Tree.Apply;
import com.sun.tools.javac.v8.tree.Tree.Assign;
import com.sun.tools.javac.v8.tree.Tree.Block;
import com.sun.tools.javac.v8.tree.Tree.ClassDef;
import com.sun.tools.javac.v8.tree.Tree.Exec;
import com.sun.tools.javac.v8.tree.Tree.Ident;
import com.sun.tools.javac.v8.tree.Tree.Labelled;
import com.sun.tools.javac.v8.tree.Tree.MethodDef;
import com.sun.tools.javac.v8.tree.Tree.Select;
import com.sun.tools.javac.v8.tree.Tree.Synchronized;
import com.sun.tools.javac.v8.tree.Tree.Try;
import com.sun.tools.javac.v8.tree.Tree.VarDef;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.ListBuffer;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;

public final class TreeInfo {

	private TreeInfo() {
	}

	private static boolean isConstructor(final Tree tree) {
		return tree.tag == 4 && ((MethodDef) tree).name == Names.init;
	}

	public static boolean hasConstructors(final List list) {
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			if (isConstructor((Tree) list1.head)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isSyntheticInit(final Tree tree) {
		if (tree.tag == 18) {
			final Exec exec = (Exec) tree;
			if (exec.expr.tag == 26) {
				final Assign assign = (Assign) exec.expr;
				if (assign.lhs.tag == 30) {
					final Select select = (Select) assign.lhs;
					if ((select.sym.flags() & 0x10000) != 0 && name(select.selected) == Names._this) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean isMethodCall(final Tree tree, final Name name1) {
		if (tree.tag == 18) {
			final Exec exec = (Exec) tree;
			if (exec.expr.tag == 23) {
				final Name name2 = name(((Apply) exec.expr).meth);
				if (name2 == name1) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isSelfCall(final Tree tree) {
		return isMethodCall(tree, Names._this) || isMethodCall(tree, Names._super);
	}

	public static boolean isInitialConstructor(final Tree tree) {
		if (tree.tag != 4) {
			return false;
		}
		final MethodDef methoddef = (MethodDef) tree;
		return (methoddef.name == Names.init || methoddef.name == Names.clinit) && (methoddef.body == null || methoddef.body.stats.isEmpty() || !isMethodCall((Tree) methoddef.body.stats.head, Names._this));
	}

	public static int firstStatPos(final Tree tree) {
		return tree.tag == 6 && ((Block) tree).stats.nonEmpty() ? ((Tree) ((Block) tree).stats.head).pos : tree.pos;
	}

	public static int endPos(final Tree tree) {
		return tree.tag == 6 && ((Block) tree).endpos != 0 ? ((Block) tree).endpos : tree.pos;
	}

	public static int finalizerPos(final Tree tree) {
		if (tree.tag == 14) {
			final Try try1 = (Try) tree;
			return try1.finalizer != null ? firstStatPos(try1.finalizer) : endPos(try1.body);
		}
		return tree.tag == 13 ? endPos(((Synchronized) tree).body) : tree.pos;
	}

	public static Tree referencedStatement(final Tree labelled) {
		Object obj = labelled;
		do {
			obj = ((Labelled) obj).body;
		} while (((Tree) obj).tag == 10);
		switch (((Tree) obj).tag) {
		case 7: // '\007'
		case 8: // '\b'
		case 9: // '\t'
		case 11: // '\013'
			return (Tree) obj;

		case 10: // '\n'
		default:
			return labelled;
		}
	}

	public static List types(final List list) {
		final ListBuffer listbuffer = new ListBuffer();
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			listbuffer.append(((Tree) list1.head).type);
		}

		return listbuffer.toList();
	}

	public static Name name(final Tree tree) {
		switch (tree.tag) {
		case 31: // '\037'
			return ((Ident) tree).name;

		case 30: // '\036'
			return ((Select) tree).name;
		}
		return null;
	}

	public static Name fullName(final Tree tree) {
		switch (tree.tag) {
		case 31: // '\037'
			return ((Ident) tree).name;

		case 30: // '\036'
			final Name name1 = fullName(((Select) tree).selected);
			return name1 != null ? Name.fromString(name1 + "." + name(tree)) : null;
		}
		return null;
	}

	public static Symbol symbol(final Tree tree) {
		switch (tree.tag) {
		case 31: // '\037'
			return ((Ident) tree).sym;

		case 30: // '\036'
			return ((Select) tree).sym;
		}
		return null;
	}

	public static void setSymbol(final Tree tree, final Symbol symbol1) {
		switch (tree.tag) {
		case 31: // '\037'
			((Ident) tree).sym = symbol1;
			break;

		case 30: // '\036'
			((Select) tree).sym = symbol1;
			break;
		}
	}

	public static int flags(final Tree tree) {
		switch (tree.tag) {
		case 5: // '\005'
			return ((VarDef) tree).flags;

		case 4: // '\004'
			return ((MethodDef) tree).flags;

		case 3: // '\003'
			return ((ClassDef) tree).flags;

		case 6: // '\006'
			return ((Block) tree).flags;
		}
		return 0;
	}

	public static int firstFlag(final int i) {
		int j;
		for (j = 1; (j & 0xfff) != 0 && (j & i) == 0; j <<= 1) {
		}
		return j;
	}

	public static String flagNames(final int i) {
		final StringBuffer stringbuffer = new StringBuffer();
		int j = 0;
		for (int k = i & 0xfff; k != 0;) {
			if ((k & 1) != 0) {
				if (stringbuffer.length() != 0) {
					stringbuffer.append(' ');
				}
				stringbuffer.append(flagName[j]);
			}
			k >>= 1;
			j++;
		}

		return stringbuffer.toString();
	}

	public static Name operatorName(final int i) {
		return opname[i - 38];
	}

	public static int opPrec(final int i) {
		switch (i) {
		case 38: // '&'
			return 14;

		case 39: // '\''
			return 14;

		case 40: // '('
			return 14;

		case 41: // ')'
			return 14;

		case 42: // '*'
			return 14;

		case 43: // '+'
			return 14;

		case 44: // ','
			return 15;

		case 45: // '-'
			return 15;

		case 26: // '\032'
			return 1;

		case 65: // 'A'
		case 66: // 'B'
		case 67: // 'C'
		case 74: // 'J'
		case 75: // 'K'
		case 76: // 'L'
		case 77: // 'M'
		case 78: // 'N'
		case 79: // 'O'
		case 80: // 'P'
		case 81: // 'Q'
			return 2;

		case 46: // '.'
			return 4;

		case 47: // '/'
			return 5;

		case 51: // '3'
			return 9;

		case 52: // '4'
			return 9;

		case 53: // '5'
			return 10;

		case 54: // '6'
			return 10;

		case 55: // '7'
			return 10;

		case 56: // '8'
			return 10;

		case 48: // '0'
			return 6;

		case 49: // '1'
			return 7;

		case 50: // '2'
			return 8;

		case 57: // '9'
			return 11;

		case 58: // ':'
			return 11;

		case 59: // ';'
			return 11;

		case 60: // '<'
			return 12;

		case 61: // '='
			return 12;

		case 62: // '>'
			return 13;

		case 63: // '?'
			return 13;

		case 64: // '@'
			return 13;

		case 28: // '\034'
			return 10;

		case 27: // '\033'
		case 29: // '\035'
		case 30: // '\036'
		case 31: // '\037'
		case 32: // ' '
		case 33: // '!'
		case 34: // '"'
		case 35: // '#'
		case 36: // '$'
		case 37: // '%'
		case 68: // 'D'
		case 69: // 'E'
		case 70: // 'F'
		case 71: // 'G'
		case 72: // 'H'
		case 73: // 'I'
		default:
			throw new InternalError();
		}
	}

	private static final String flagName[] = { "public", "private", "protected", "static", "final", "synchronized", "volatile", "transient", "native", "interface", "abstract", "strictfp" };
	private static final Name[] opname;
	public static final int notExpression = -1;
	public static final int noPrec = 0;
	public static final int assignPrec = 1;
	public static final int assignopPrec = 2;
	public static final int condPrec = 3;
	public static final int orPrec = 4;
	public static final int andPrec = 5;
	public static final int bitorPrec = 6;
	public static final int bitxorPrec = 7;
	public static final int bitandPrec = 8;
	public static final int eqPrec = 9;
	public static final int ordPrec = 10;
	public static final int shiftPrec = 11;
	public static final int addPrec = 12;
	public static final int mulPrec = 13;
	public static final int prefixPrec = 14;
	public static final int postfixPrec = 15;
	public static final int precCount = 16;

	static {
		opname = new Name[27];
		opname[0] = Name.fromString("+");
		opname[1] = Name.fromString("-");
		opname[2] = Name.fromString("!");
		opname[3] = Name.fromString("~");
		opname[4] = Name.fromString("++");
		opname[5] = Name.fromString("--");
		opname[6] = Name.fromString("++");
		opname[7] = Name.fromString("--");
		opname[8] = Name.fromString("||");
		opname[9] = Name.fromString("&&");
		opname[13] = Name.fromString("==");
		opname[14] = Name.fromString("!=");
		opname[15] = Name.fromString("<");
		opname[16] = Name.fromString(">");
		opname[17] = Name.fromString("<=");
		opname[18] = Name.fromString(">=");
		opname[10] = Name.fromString("|");
		opname[11] = Name.fromString("^");
		opname[12] = Name.fromString("&");
		opname[19] = Name.fromString("<<");
		opname[20] = Name.fromString(">>");
		opname[21] = Name.fromString(">>>");
		opname[22] = Name.fromString("+");
		opname[23] = Name.fromString("-");
		opname[24] = Name.fromString("*");
		opname[25] = Name.fromString("/");
		opname[26] = Name.fromString("%");
	}
}
