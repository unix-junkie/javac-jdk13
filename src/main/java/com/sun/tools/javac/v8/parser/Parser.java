package com.sun.tools.javac.v8.parser;

import com.sun.tools.javac.v8.tree.Tree;
import com.sun.tools.javac.v8.tree.Tree.Block;
import com.sun.tools.javac.v8.tree.Tree.Catch;
import com.sun.tools.javac.v8.tree.Tree.ClassDef;
import com.sun.tools.javac.v8.tree.Tree.Erroneous;
import com.sun.tools.javac.v8.tree.Tree.Exec;
import com.sun.tools.javac.v8.tree.Tree.Ident;
import com.sun.tools.javac.v8.tree.Tree.Literal;
import com.sun.tools.javac.v8.tree.Tree.MethodDef;
import com.sun.tools.javac.v8.tree.Tree.Operation;
import com.sun.tools.javac.v8.tree.Tree.TopLevel;
import com.sun.tools.javac.v8.tree.Tree.TypeIdent;
import com.sun.tools.javac.v8.tree.Tree.TypeParameter;
import com.sun.tools.javac.v8.tree.Tree.VarDef;
import com.sun.tools.javac.v8.tree.TreeInfo;
import com.sun.tools.javac.v8.tree.TreeMaker;
import com.sun.tools.javac.v8.util.Convert;
import com.sun.tools.javac.v8.util.Hashtable;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.ListBuffer;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;
import com.sun.tools.javac.v8.util.Position;
import com.sun.tools.javac.v8.util.Util;

public final class Parser {

	public Parser(final Scanner scanner, final TreeMaker treemaker, final Log log1, final boolean flag, final boolean flag1) {
		this.mode = 0;
		this.lastmode = 0;
		this.odStackSupply = new ListBuffer();
		this.opStackSupply = new ListBuffer();
		this.S = scanner;
		this.F = treemaker;
		this.log = log1;
		this.keepDocComments = flag1;
		if (flag1) {
			this.docComments = new Hashtable();
		}
	}

	private void skip() {
		int i = 0;
		int j = 0;
		do {
			switch (this.S.token) {
			default:
				break;

			case 0: // '\0'
			case 10: // '\n'
			case 28: // '\034'
				return;

			case 66: // 'B'
				if (i == 0 && j == 0) {
					return;
				}
				break;

			case 63: // '?'
				if (i == 0) {
					return;
				}
				i--;
				break;

			case 61: // '='
				if (j > 0) {
					j--;
				}
				break;

			case 62: // '>'
				i++;
				break;

			case 60: // '<'
				j++;
				break;
			}
			this.S.nextToken();
		} while (true);
	}

	private Tree syntaxError(final int i, final String s, final String s1) {
		if (i != this.S.errPos) {
			this.log.error(i, s, s1);
		}
		this.skip();
		this.S.errPos = i;
		return errorTree;
	}

	private Tree syntaxError(final int i, final String s) {
		return this.syntaxError(i, s, null);
	}

	private Tree syntaxError(final String s) {
		return this.syntaxError(this.S.pos, s, null);
	}

	private void syntaxError(final String s, final String s1) {
		this.syntaxError(this.S.pos, s, s1);
	}

	private void accept(final int i) {
		if (this.S.token == i) {
			this.S.nextToken();
		} else {
			final int j = Position.line(this.S.pos) <= Position.line(this.S.lastPos) ? this.S.pos : this.S.lastPos;
			this.syntaxError(j, "expected", Scanner.token2string(i));
			if (this.S.token == i) {
				this.S.nextToken();
			}
		}
	}

	private Tree illegal(final int i) {
		return (this.mode & 1) != 0 ? this.syntaxError(i, "illegal.start.of.expr") : this.syntaxError(i, "illegal.start.of.type");
	}

	private Tree illegal() {
		return this.illegal(this.S.pos);
	}

	private void attach(final Tree tree, final String s) {
		if (this.keepDocComments && s != null) {
			this.docComments.put(tree, s);
		}
	}

	private Name ident() {
		if (this.S.token == 2) {
			final Name name = this.S.name;
			this.S.nextToken();
			return name;
		}
		this.accept(2);
		return Names.error;
	}

	private Tree qualident() {
		Object obj;
		int i;
		for (obj = this.F.at(this.S.pos).Ident(this.ident()); this.S.token == 68; obj = this.F.at(i).Select((Tree) obj, this.ident())) {
			i = this.S.pos;
			this.S.nextToken();
		}

		return (Tree) obj;
	}

	private Tree literal(final Name name) {
		final int i = this.S.pos;
		Object obj = errorTree;
		switch (this.S.token) {
		case 51: // '3'
			try {
				obj = this.F.at(i).Literal(4, new Integer(Convert.string2int(this.strval(name), this.S.radix)));
			} catch (final NumberFormatException ignored) {
				this.log.error(this.S.pos, "int.number.too.large", this.strval(name));
			}
			break;

		case 52: // '4'
			try {
				obj = this.F.at(i).Literal(5, new Long(Convert.string2long(this.strval(name), this.S.radix)));
			} catch (final NumberFormatException ignored) {
				this.log.error(this.S.pos, "int.number.too.large", this.strval(name));
			}
			break;

		case 53: // '5'
			final Float float1 = Float.valueOf(this.S.stringVal());
			if (float1.floatValue() == 0.0F && !isZero(this.S.stringVal())) {
				this.log.error(this.S.pos, "fp.number.too.small");
				break;
			}
			if (float1.floatValue() == Float.POSITIVE_INFINITY) {
				this.log.error(this.S.pos, "fp.number.too.large");
			} else {
				obj = this.F.at(i).Literal(6, float1);
			}
			break;

		case 54: // '6'
			final Double double1 = Double.valueOf(this.S.stringVal());
			if (double1.doubleValue() == 0.0D && !isZero(this.S.stringVal())) {
				this.log.error(this.S.pos, "fp.number.too.small");
				break;
			}
			if (double1.doubleValue() == Double.POSITIVE_INFINITY) {
				this.log.error(this.S.pos, "fp.number.too.large");
			} else {
				obj = this.F.at(i).Literal(7, double1);
			}
			break;

		case 55: // '7'
			obj = this.F.at(i).Literal(2, new Integer(this.S.stringVal().charAt(0)));
			break;

		case 56: // '8'
			obj = this.F.at(i).Literal(10, this.S.stringVal());
			break;

		case 57: // '9'
		case 58: // ':'
		case 59: // ';'
			obj = this.F.at(i).Ident(this.S.name);
			break;

		default:
			throw new InternalError();
		}
		this.S.nextToken();
		return (Tree) obj;
	}

	private static boolean isZero(final String s) {
		final char ac[] = s.toCharArray();
		int i;
		for (i = 0; i < ac.length && (ac[i] == '0' || ac[i] == '.'); i++) {
		}
		return i >= ac.length || ac[i] < '1' || ac[i] > '9';
	}

	private String strval(final Name name) {
		final String s = this.S.stringVal();
		return name.len != 0 ? name + s : s;
	}

	private Tree expression() {
		return this.term(1);
	}

	private Tree type() {
		return this.term(2);
	}

	private Tree term(final int i) {
		final int j = this.mode;
		this.mode = i;
		final Tree tree = this.term();
		this.lastmode = this.mode;
		this.mode = j;
		return tree;
	}

	private Tree term() {
		final Tree tree = this.term1();
		return (this.mode & 1) != 0 && this.S.token == 69 || this.S.token >= 95 && this.S.token <= 105 ? this.termRest(tree) : tree;
	}

	private Tree termRest(final Tree tree) {
		switch (this.S.token) {
		case 69: // 'E'
			final int i = this.S.pos;
			this.S.nextToken();
			this.mode = 1;
			final Tree tree1 = this.term();
			return this.F.at(i).Assign(tree, tree1);

		case 95: // '_'
		case 96: // '`'
		case 97: // 'a'
		case 98: // 'b'
		case 99: // 'c'
		case 100: // 'd'
		case 101: // 'e'
		case 102: // 'f'
		case 103: // 'g'
		case 104: // 'h'
		case 105: // 'i'
			final int j = this.S.pos;
			final int k = this.S.token;
			this.S.nextToken();
			this.mode = 1;
			final Tree tree2 = this.term();
			return this.F.at(j).Assignop(optag(k), tree, tree2);

		case 70: // 'F'
		case 71: // 'G'
		case 72: // 'H'
		case 73: // 'I'
		case 74: // 'J'
		case 75: // 'K'
		case 76: // 'L'
		case 77: // 'M'
		case 78: // 'N'
		case 79: // 'O'
		case 80: // 'P'
		case 81: // 'Q'
		case 82: // 'R'
		case 83: // 'S'
		case 84: // 'T'
		case 85: // 'U'
		case 86: // 'V'
		case 87: // 'W'
		case 88: // 'X'
		case 89: // 'Y'
		case 90: // 'Z'
		case 91: // '['
		case 92: // '\\'
		case 93: // ']'
		case 94: // '^'
		default:
			return tree;
		}
	}

	private Tree term1() {
		final Tree tree = this.term2();
		if ((this.mode & 1) != 0 & this.S.token == 74) {
			this.mode = 1;
			return this.term1Rest(tree);
		}
		return tree;
	}

	private Tree term1Rest(final Tree tree) {
		if (this.S.token == 74) {
			final int i = this.S.pos;
			this.S.nextToken();
			final Tree tree1 = this.term();
			this.accept(75);
			final Tree tree2 = this.term1();
			return this.F.at(i).Conditional(16, tree, tree1, tree2);
		}
		return tree;
	}

	private Tree term2() {
		final Tree tree = this.term3();
		if ((this.mode & 1) != 0 && prec(this.S.token) >= 4) {
			this.mode = 1;
			return this.term2Rest(tree, 4);
		}
		return tree;
	}

	private Tree term2Rest(Tree tree, final int i) {
		final List list = this.odStackSupply.elems;
		final Tree atree[] = this.newOdStack();
		final List list1 = this.opStackSupply.elems;
		final int ai[] = this.newOpStack();
		atree[0] = tree;
		final int k = this.S.pos;
		int l = 1;
		int j = 0;
		while (prec(this.S.token) >= i) {
			ai[j] = l;
			j++;
			l = this.S.token;
			final int i1 = this.S.pos;
			this.S.nextToken();
			atree[j] = l != 26 ? this.term3() : this.type();
			for (; j > 0 && prec(l) >= prec(this.S.token); l = ai[j]) {
				atree[j - 1] = this.makeOp(i1, l, atree[j - 1], atree[j]);
				j--;
			}

		}
		Util.assertTrue(j == 0);
		tree = atree[0];
		if (tree.tag == 60) {
			final StringBuffer stringbuffer = foldStrings(tree);
			if (stringbuffer != null) {
				tree = this.F.at(k).Literal(10, stringbuffer.toString());
			}
		}
		this.odStackSupply.elems = list;
		this.opStackSupply.elems = list1;
		return tree;
	}

	private Tree makeOp(final int i, final int j, final Tree tree, final Tree tree1) {
		return j == 26 ? (Tree) this.F.at(i).TypeTest(tree, tree1) : this.F.at(i).Operation(optag(j), List.make(tree, tree1));
	}

	private static StringBuffer foldStrings(final Tree tree) {
		if (tree.tag == 32) {
			final Literal literal1 = (Literal) tree;
			if (literal1.typetag == 10) {
				return new StringBuffer((String) literal1.value);
			}
		} else if (tree.tag == 60) {
			final Operation operation = (Operation) tree;
			if (operation.args.tail.nonEmpty()) {
				final Tree tree1 = (Tree) operation.args.head;
				final Tree tree2 = (Tree) operation.args.tail.head;
				if (tree2.tag == 32) {
					final Literal literal2 = (Literal) tree2;
					if (literal2.typetag == 10) {
						final StringBuffer stringbuffer = foldStrings(tree1);
						if (stringbuffer != null) {
							return stringbuffer.append(literal2.value);
						}
					}
				}
			}
		}
		return null;
	}

	private Tree[] newOdStack() {
		if (this.odStackSupply.elems == this.odStackSupply.last) {
			this.odStackSupply.append(new Tree[11]);
		}
		final Tree atree[] = (Tree[]) this.odStackSupply.elems.head;
		this.odStackSupply.elems = this.odStackSupply.elems.tail;
		return atree;
	}

	private int[] newOpStack() {
		if (this.opStackSupply.elems == this.opStackSupply.last) {
			this.opStackSupply.append(new int[11]);
		}
		final int ai[] = (int[]) this.opStackSupply.elems.head;
		this.opStackSupply.elems = this.opStackSupply.elems.tail;
		return ai;
	}

	private Tree term3() {
		int i = this.S.pos;
		Object obj;
		label0: switch (this.S.token) {
		case 72: // 'H'
		case 73: // 'I'
		case 82: // 'R'
		case 83: // 'S'
		case 84: // 'T'
		case 85: // 'U'
			if ((this.mode & 1) != 0) {
				this.mode = 1;
				final int j = this.S.token;
				this.S.nextToken();
				if (j == 85 && (this.S.token == 51 || this.S.token == 52) && this.S.radix == 10) {
					obj = this.literal(Names.hyphen);
				} else {
					obj = this.term3();
					return this.F.at(i).Operation(unoptag(j), List.make(obj));
				}
			} else {
				return this.illegal();
			}
			break;

		case 60: // '<'
			if ((this.mode & 1) != 0) {
				this.S.nextToken();
				this.mode = 7;
				obj = this.term3();
				obj = this.termRest(this.term1Rest(this.term2Rest((Tree) obj, 4)));
				this.accept(61);
				this.lastmode = this.mode;
				this.mode = 1;
				if ((this.lastmode & 1) == 0) {
					final Tree tree = this.term3();
					return this.F.at(i).TypeCast((Tree) obj, tree);
				}
				if ((this.lastmode & 2) == 0) {
					break;
				}
				switch (this.S.token) {
				case 2: // '\002'
				case 4: // '\004'
				case 6: // '\006'
				case 9: // '\t'
				case 15: // '\017'
				case 20: // '\024'
				case 27: // '\033'
				case 29: // '\035'
				case 31: // '\037'
				case 37: // '%'
				case 40: // '('
				case 43: // '+'
				case 48: // '0'
				case 51: // '3'
				case 52: // '4'
				case 53: // '5'
				case 54: // '6'
				case 55: // '7'
				case 56: // '8'
				case 57: // '9'
				case 58: // ':'
				case 59: // ';'
				case 60: // '<'
				case 72: // 'H'
				case 73: // 'I'
					final Tree tree1 = this.term3();
					return this.F.at(i).TypeCast((Tree) obj, tree1);
				}
			} else {
				return this.illegal();
			}
			break;

		case 43: // '+'
			if ((this.mode & 1) != 0) {
				this.mode = 1;
				obj = this.F.at(i).Ident(Names._this);
				this.S.nextToken();
				obj = this.argumentsOpt((Tree) obj);
			} else {
				return this.illegal();
			}
			break;

		case 40: // '('
			if ((this.mode & 1) != 0) {
				this.mode = 1;
				obj = this.superSuffix(this.F.at(i).Ident(Names._super));
			} else {
				return this.illegal();
			}
			break;

		case 51: // '3'
		case 52: // '4'
		case 53: // '5'
		case 54: // '6'
		case 55: // '7'
		case 56: // '8'
		case 57: // '9'
		case 58: // ':'
		case 59: // ';'
			if ((this.mode & 1) != 0) {
				this.mode = 1;
				obj = this.literal(Names.empty);
			} else {
				return this.illegal();
			}
			break;

		case 31: // '\037'
			if ((this.mode & 1) != 0) {
				this.mode = 1;
				this.S.nextToken();
				obj = this.creator(i);
			} else {
				return this.illegal();
			}
			break;

		case 2: // '\002'
			obj = this.F.at(this.S.pos).Ident(this.ident());
			do {
				i = this.S.pos;
				switch (this.S.token) {
				default:
					break label0;

				case 64: // '@'
					this.S.nextToken();
					if (this.S.token == 65) {
						this.S.nextToken();
						obj = this.bracketsSuffix(this.bracketsOpt(this.F.at(i).TypeArray((Tree) obj)));
						break label0;
					}
					if ((this.mode & 1) != 0) {
						this.mode = 1;
						final Tree tree2 = this.term();
						obj = this.F.at(i).Indexed((Tree) obj, tree2);
					}
					this.accept(65);
					break label0;

				case 60: // '<'
					if ((this.mode & 1) != 0) {
						this.mode = 1;
						obj = this.arguments((Tree) obj);
					}
					break label0;

				case 68: // 'D'
					this.S.nextToken();
					if ((this.mode & 1) != 0) {
						switch (this.S.token) {
						case 10: // '\n'
							this.mode = 1;
							obj = this.F.at(i).Select((Tree) obj, Names._class);
							this.S.nextToken();
							break label0;

						case 43: // '+'
							this.mode = 1;
							obj = this.F.at(i).Select((Tree) obj, Names._this);
							this.S.nextToken();
							break label0;

						case 40: // '('
							this.mode = 1;
							obj = this.superSuffix(this.F.at(i).Select((Tree) obj, Names._super));
							break label0;

						case 31: // '\037'
							this.mode = 1;
							final int k = this.S.pos;
							this.S.nextToken();
							obj = this.innerCreator(k, (Tree) obj);
							break label0;
						}
					}
					obj = this.F.at(i).Select((Tree) obj, this.ident());
					break;
				}
			} while (true);

		case 4: // '\004'
		case 6: // '\006'
		case 9: // '\t'
		case 15: // '\017'
		case 20: // '\024'
		case 27: // '\033'
		case 29: // '\035'
		case 37: // '%'
			obj = this.bracketsSuffix(this.bracketsOpt(this.basicType()));
			break;

		case 48: // '0'
			if ((this.mode & 1) != 0) {
				this.S.nextToken();
				if (this.S.token == 68) {
					obj = this.bracketsSuffix(this.F.at(i).TypeIdent(9));
				} else {
					return this.illegal(i);
				}
			} else {
				return this.illegal();
			}
			break;

		case 3: // '\003'
		case 5: // '\005'
		case 7: // '\007'
		case 8: // '\b'
		case 10: // '\n'
		case 11: // '\013'
		case 12: // '\f'
		case 13: // '\r'
		case 14: // '\016'
		case 16: // '\020'
		case 17: // '\021'
		case 18: // '\022'
		case 19: // '\023'
		case 21: // '\025'
		case 22: // '\026'
		case 23: // '\027'
		case 24: // '\030'
		case 25: // '\031'
		case 26: // '\032'
		case 28: // '\034'
		case 30: // '\036'
		case 32: // ' '
		case 33: // '!'
		case 34: // '"'
		case 35: // '#'
		case 36: // '$'
		case 38: // '&'
		case 39: // '\''
		case 41: // ')'
		case 42: // '*'
		case 44: // ','
		case 45: // '-'
		case 46: // '.'
		case 47: // '/'
		case 49: // '1'
		case 50: // '2'
		case 61: // '='
		case 62: // '>'
		case 63: // '?'
		case 64: // '@'
		case 65: // 'A'
		case 66: // 'B'
		case 67: // 'C'
		case 68: // 'D'
		case 69: // 'E'
		case 70: // 'F'
		case 71: // 'G'
		case 74: // 'J'
		case 75: // 'K'
		case 76: // 'L'
		case 77: // 'M'
		case 78: // 'N'
		case 79: // 'O'
		case 80: // 'P'
		case 81: // 'Q'
		default:
			return this.illegal();
		}
		do {
			final int l = this.S.pos;
			switch (this.S.token) {
			case 64:
				this.S.nextToken();
				if (this.S.token == 65 && (this.mode & 2) != 0) {
					this.mode = 2;
					this.S.nextToken();
					return this.bracketsOpt(this.F.at(l).TypeArray((Tree) obj));
				}
				if ((this.mode & 1) != 0) {
					this.mode = 1;
					final Tree tree3 = this.term();
					obj = this.F.at(l).Indexed((Tree) obj, tree3);
				}
				this.accept(65);
				break;
			case 68:
				this.S.nextToken();
				if (this.S.token == 40 && (this.mode & 1) != 0) {
					this.mode = 1;
					obj = this.F.at(i).Select((Tree) obj, Names._super);
					this.S.nextToken();
					obj = this.arguments((Tree) obj);
				} else if (this.S.token == 31 && (this.mode & 1) != 0) {
					this.mode = 1;
					final int i1 = this.S.pos;
					this.S.nextToken();
					obj = this.innerCreator(i1, (Tree) obj);
				} else {
					obj = this.argumentsOpt(this.F.at(i).Select((Tree) obj, this.ident()));
				}
				break;
			default:
				while ((this.S.token == 82 || this.S.token == 83) && (this.mode & 1) != 0) {
					this.mode = 1;
					obj = this.F.at(this.S.pos).Operation(this.S.token != 82 ? 45 : 44, List.make(obj));
					this.S.nextToken();
				}
				return (Tree) obj;
			}
		} while (true);
	}

	private Tree superSuffix(final Tree tree) {
		this.S.nextToken();
		if (this.S.token == 60) {
			return this.arguments(tree);
		}
		final int i = this.S.pos;
		this.accept(68);
		return this.argumentsOpt(this.F.at(i).Select(tree, this.ident()));
	}

	private Tree basicType() {
		final TypeIdent typeident = this.F.at(this.S.pos).TypeIdent(typetag(this.S.token));
		this.S.nextToken();
		return typeident;
	}

	private Tree argumentsOpt(final Tree tree) {
		if ((this.mode & 1) != 0 && this.S.token == 60) {
			this.mode = 1;
			return this.arguments(tree);
		}
		return tree;
	}

	private List arguments() {
		final ListBuffer listbuffer = new ListBuffer();
		if (this.S.token == 60) {
			this.S.nextToken();
			if (this.S.token != 61) {
				listbuffer.append(this.expression());
				while (this.S.token == 67) {
					this.S.nextToken();
					listbuffer.append(this.expression());
				}
			}
			this.accept(61);
		} else {
			this.syntaxError(this.S.pos, "expected", Scanner.token2string(60));
		}
		return listbuffer.toList();
	}

	private Tree arguments(final Tree tree) {
		final int i = this.S.pos;
		final List list = this.arguments();
		return this.F.at(i).Apply(tree, list);
	}

	private Tree bracketsOpt(Tree tree) {
		while (this.S.token == 64) {
			final int i = this.S.pos;
			this.S.nextToken();
			this.accept(65);
			tree = this.F.at(i).TypeArray(tree);
		}
		return tree;
	}

	private Tree bracketsSuffix(final Tree tree) {
		if ((this.mode & 1) != 0 && this.S.token == 68) {
			this.mode = 1;
			final int i = this.S.pos;
			this.S.nextToken();
			this.accept(10);
			return this.F.at(i).Select(tree, Names._class);
		}
		if ((this.mode & 2) != 0) {
			this.mode = 2;
		} else {
			this.syntaxError(this.S.pos, "dot.class.expected");
		}
		return tree;
	}

	private Tree creator(final int i) {
		switch (this.S.token) {
		case 4: // '\004'
		case 6: // '\006'
		case 9: // '\t'
		case 15: // '\017'
		case 20: // '\024'
		case 27: // '\033'
		case 29: // '\035'
		case 37: // '%'
			return this.arrayCreatorRest(i, this.basicType());
		}
		final Tree tree = this.qualident();
		if (this.S.token == 64) {
			return this.arrayCreatorRest(i, tree);
		}
		return this.S.token == 60 ? this.classCreatorRest(i, null, tree) : this.syntaxError("left-paren.or.left-square-bracket.expected");
	}

	private Tree innerCreator(final int i, final Tree tree) {
		final Ident ident1 = this.F.at(this.S.pos).Ident(this.ident());
		return this.classCreatorRest(i, tree, ident1);
	}

	private Tree arrayCreatorRest(final int i, Tree tree) {
		this.accept(64);
		if (this.S.token == 65) {
			this.S.nextToken();
			tree = this.bracketsOpt(tree);
			if (this.S.token == 62) {
				return this.arrayInitializer(tree);
			}
			this.syntaxError(this.S.pos, "expected", Scanner.token2string(62));
			return errorTree;
		}
		final ListBuffer listbuffer = new ListBuffer();
		listbuffer.append(this.expression());
		this.accept(65);
		while (this.S.token == 64) {
			final int k = this.S.pos;
			this.S.nextToken();
			if (this.S.token == 65) {
				this.S.nextToken();
				tree = this.bracketsOpt(this.F.at(k).TypeArray(tree));
			} else {
				listbuffer.append(this.expression());
				this.accept(65);
			}
		}
		return this.F.at(i).NewArray(tree, listbuffer.toList(), null);
	}

	private Tree classCreatorRest(final int i, final Tree tree, final Tree tree1) {
		final List list = this.arguments();
		ClassDef classdef = null;
		if (this.S.token == 62) {
			classdef = this.F.at(this.S.pos).ClassDef(0, Names.empty, TypeParameter.emptyList, null, Tree.emptyList, this.classOrInterfaceBody(Names.empty, false));
		}
		return this.F.at(i).NewClass(tree, tree1, list, classdef);
	}

	private Tree arrayInitializer(final Tree tree) {
		final int i = this.S.pos;
		this.accept(62);
		final ListBuffer listbuffer = new ListBuffer();
		if (this.S.token != 63) {
			listbuffer.append(this.variableInitializer());
			while (this.S.token == 67) {
				this.S.nextToken();
				if (this.S.token == 63) {
					break;
				}
				listbuffer.append(this.variableInitializer());
			}
		}
		this.accept(63);
		return this.F.at(i).NewArray(tree, Tree.emptyList, listbuffer.toList());
	}

	private Tree variableInitializer() {
		return this.S.token != 62 ? this.expression() : this.arrayInitializer(null);
	}

	private Tree parExpression() {
		this.accept(60);
		final Tree tree = this.expression();
		this.accept(61);
		return tree;
	}

	private Block block(final int i) {
		final int j = this.S.pos;
		this.accept(62);
		final List list = this.blockStatements();
		final Block block1 = this.F.at(j).Block(i, list);
		while (this.S.token == 7 || this.S.token == 13) {
			this.syntaxError("orphaned", Scanner.token2string(this.S.token));
			this.blockStatements();
		}
		block1.endpos = this.S.pos;
		this.accept(63);
		return block1;
	}

	private Block block() {
		return this.block(0);
	}

	private List blockStatements() {
		final ListBuffer listbuffer = new ListBuffer();
		do {
			final int i = this.S.pos;
			switch (this.S.token) {
			case 0: // '\0'
			case 7: // '\007'
			case 13: // '\r'
			case 63: // '?'
				return listbuffer.toList();

			case 5: // '\005'
			case 8: // '\b'
			case 12: // '\f'
			case 14: // '\016'
			case 16: // '\020'
			case 19: // '\023'
			case 21: // '\025'
			case 23: // '\027'
			case 36: // '$'
			case 41: // ')'
			case 42: // '*'
			case 44: // ','
			case 47: // '/'
			case 50: // '2'
			case 62: // '>'
			case 66: // 'B'
				listbuffer.append(this.statement());
				break;

			case 18: // '\022'
				final String s = this.S.docComment;
				final int j = this.modifiersOpt();
				if (this.S.token == 28 || this.S.token == 10) {
					listbuffer.append(this.classOrInterfaceDeclaration(j, s));
				} else {
					final Tree tree2 = this.type();
					listbuffer.appendList(this.variableDeclarators(j, tree2));
					this.accept(66);
				}
				break;

			case 3: // '\003'
			case 39: // '\''
				final String s1 = this.S.docComment;
				final int k = this.modifiersOpt();
				listbuffer.append(this.classOrInterfaceDeclaration(k, s1));
				break;

			case 10: // '\n'
			case 28: // '\034'
				listbuffer.append(this.classOrInterfaceDeclaration(0, this.S.docComment));
				break;

			case 1: // '\001'
			case 2: // '\002'
			case 4: // '\004'
			case 6: // '\006'
			case 9: // '\t'
			case 11: // '\013'
			case 15: // '\017'
			case 17: // '\021'
			case 20: // '\024'
			case 22: // '\026'
			case 24: // '\030'
			case 25: // '\031'
			case 26: // '\032'
			case 27: // '\033'
			case 29: // '\035'
			case 30: // '\036'
			case 31: // '\037'
			case 32: // ' '
			case 33: // '!'
			case 34: // '"'
			case 35: // '#'
			case 37: // '%'
			case 38: // '&'
			case 40: // '('
			case 43: // '+'
			case 45: // '-'
			case 46: // '.'
			case 48: // '0'
			case 49: // '1'
			case 51: // '3'
			case 52: // '4'
			case 53: // '5'
			case 54: // '6'
			case 55: // '7'
			case 56: // '8'
			case 57: // '9'
			case 58: // ':'
			case 59: // ';'
			case 60: // '<'
			case 61: // '='
			case 64: // '@'
			case 65: // 'A'
			default:
				final Name name = this.S.name;
				final Tree tree = this.term(3);
				if (this.S.token == 75 && tree.tag == 31) {
					this.S.nextToken();
					final Tree tree1 = this.statement();
					listbuffer.append(this.F.at(i).Labelled(name, tree1));
				} else if ((this.lastmode & 2) != 0 && this.S.token == 2) {
					listbuffer.appendList(this.variableDeclarators(0, tree));
					this.accept(66);
				} else {
					listbuffer.append(this.F.at(i).Exec(this.checkExprStat(tree)));
					this.accept(66);
				}
				break;
			}
		} while (true);
	}

	private Tree statement() {
		final int i = this.S.pos;
		final Name name2;
		final Tree tree11;
		switch (this.S.token) {
		case 62: // '>'
			return this.block();

		case 23: // '\027'
			this.S.nextToken();
			final Tree tree = this.parExpression();
			final Tree tree7 = this.statement();
			Tree tree12 = null;
			if (this.S.token == 16) {
				this.S.nextToken();
				tree12 = this.statement();
			}
			return this.F.at(i).Conditional(17, tree, tree7, tree12);

		case 21: // '\025'
			this.S.nextToken();
			this.accept(60);
			final List list = this.S.token != 66 ? this.forInit() : Tree.emptyList;
			this.accept(66);
			final Tree tree8 = this.S.token != 66 ? this.expression() : null;
			this.accept(66);
			final List list2 = this.S.token != 61 ? this.forUpdate() : Tree.emptyList;
			this.accept(61);
			final Tree tree14 = this.statement();
			return this.F.at(i).ForLoop(list, tree8, list2, tree14);

		case 50: // '2'
			this.S.nextToken();
			final Tree tree1 = this.parExpression();
			final Tree tree9 = this.statement();
			return this.F.at(i).WhileLoop(tree1, tree9);

		case 14: // '\016'
			this.S.nextToken();
			final Tree tree2 = this.statement();
			this.accept(50);
			final Tree tree10 = this.parExpression();
			this.accept(66);
			return this.F.at(i).DoLoop(tree2, tree10);

		case 47: // '/'
			this.S.nextToken();
			final Block block1 = this.block();
			final ListBuffer listbuffer = new ListBuffer();
			Block block3 = null;
			if (this.S.token == 8 || this.S.token == 19) {
				while (this.S.token == 8) {
					listbuffer.append(this.catchClause());
				}
				if (this.S.token == 19) {
					this.S.nextToken();
					block3 = this.block();
				}
			} else {
				this.log.error(i, "try.without.catch.or.finally");
			}
			return this.F.at(i).Try(block1, listbuffer.toList(), block3);

		case 41: // ')'
			this.S.nextToken();
			final Tree tree3 = this.parExpression();
			this.accept(62);
			final List list1 = this.switchBlockStatementGroups();
			this.accept(63);
			return this.F.at(i).Switch(tree3, list1);

		case 42: // '*'
			this.S.nextToken();
			final Tree tree4 = this.parExpression();
			final Block block2 = this.block();
			return this.F.at(i).Synchronized(tree4, block2);

		case 36: // '$'
			this.S.nextToken();
			final Tree tree5 = this.S.token != 66 ? this.expression() : null;
			this.accept(66);
			return this.F.at(i).Return(tree5);

		case 44: // ','
			this.S.nextToken();
			final Tree tree6 = this.expression();
			this.accept(66);
			return this.F.at(i).Throw(tree6);

		case 5: // '\005'
			this.S.nextToken();
			final Name name = this.S.token != 2 ? null : this.ident();
			this.accept(66);
			return this.F.at(i).Break(name);

		case 12: // '\f'
			this.S.nextToken();
			final Name name1 = this.S.token != 2 ? null : this.ident();
			this.accept(66);
			return this.F.at(i).Continue(name1);

		case 66: // 'B'
			this.S.nextToken();
			return this.F.at(i).Block(0, Tree.emptyList);

		case 16: // '\020'
			return this.syntaxError("else.without.if");

		case 19: // '\023'
			return this.syntaxError("finally.without.try");

		case 8: // '\b'
			return this.syntaxError("catch.without.try");

		case 6: // '\006'
		case 7: // '\007'
		case 9: // '\t'
		case 10: // '\n'
		case 11: // '\013'
		case 13: // '\r'
		case 15: // '\017'
		case 17: // '\021'
		case 18: // '\022'
		case 20: // '\024'
		case 22: // '\026'
		case 24: // '\030'
		case 25: // '\031'
		case 26: // '\032'
		case 27: // '\033'
		case 28: // '\034'
		case 29: // '\035'
		case 30: // '\036'
		case 31: // '\037'
		case 32: // ' '
		case 33: // '!'
		case 34: // '"'
		case 35: // '#'
		case 37: // '%'
		case 38: // '&'
		case 39: // '\''
		case 40: // '('
		case 43: // '+'
		case 45: // '-'
		case 46: // '.'
		case 48: // '0'
		case 49: // '1'
		case 51: // '3'
		case 52: // '4'
		case 53: // '5'
		case 54: // '6'
		case 55: // '7'
		case 56: // '8'
		case 57: // '9'
		case 58: // ':'
		case 59: // ';'
		case 60: // '<'
		case 61: // '='
		case 63: // '?'
		case 64: // '@'
		case 65: // 'A'
		default:
			name2 = this.S.name;
			tree11 = this.expression();
			break;
		}
		if (this.S.token == 75 && tree11.tag == 31) {
			this.S.nextToken();
			final Tree tree13 = this.statement();
			return this.F.at(i).Labelled(name2, tree13);
		}
		final Exec exec = this.F.at(i).Exec(this.checkExprStat(tree11));
		this.accept(66);
		return exec;
	}

	private Catch catchClause() {
		final int i = this.S.pos;
		this.accept(8);
		this.accept(60);
		final VarDef vardef = this.variableDeclaratorId(this.optFinal(), this.qualident());
		this.accept(61);
		final Block block1 = this.block();
		return this.F.at(i).Catch(vardef, block1);
	}

	private List switchBlockStatementGroups() {
		final ListBuffer listbuffer = new ListBuffer();
		do {
			final int i = this.S.pos;
			switch (this.S.token) {
			case 7: // '\007'
				this.S.nextToken();
				final Tree tree = this.expression();
				this.accept(75);
				final List list1 = this.blockStatements();
				listbuffer.append(this.F.at(i).Case(tree, list1));
				break;

			case 13: // '\r'
				this.S.nextToken();
				this.accept(75);
				final List list = this.blockStatements();
				listbuffer.append(this.F.at(i).Case(null, list));
				break;

			case 0: // '\0'
			case 63: // '?'
				return listbuffer.toList();

			default:
				this.S.nextToken();
				this.syntaxError(i, "case.default.or.right-brace.expected");
				break;
			}
		} while (true);
	}

	private List moreStatementExpressions(int i, final Tree tree) {
		final ListBuffer listbuffer = new ListBuffer();
		listbuffer.append(this.F.at(i).Exec(this.checkExprStat(tree)));
		while (this.S.token == 67) {
			this.S.nextToken();
			i = this.S.pos;
			final Tree tree1 = this.expression();
			listbuffer.append(this.F.at(i).Exec(this.checkExprStat(tree1)));
		}
		return listbuffer.toList();
	}

	private List forInit() {
		final int i = this.S.pos;
		if (this.S.token == 18) {
			this.S.nextToken();
			return this.variableDeclarators(16, this.type());
		}
		final Tree tree = this.term(3);
		return (this.lastmode & 2) != 0 && this.S.token == 2 ? this.variableDeclarators(0, tree) : this.moreStatementExpressions(i, tree);
	}

	private List forUpdate() {
		return this.moreStatementExpressions(this.S.pos, this.expression());
	}

	private int modifiersOpt() {
		int i = 0;
		if (this.S.deprecatedFlag) {
			i = 0x20000;
			this.S.deprecatedFlag = false;
		}
		do {
			final char c;
			switch (this.S.token) {
			case 33: // '!'
				c = '\002';
				break;

			case 34: // '"'
				c = '\004';
				break;

			case 35: // '#'
				c = '\001';
				break;

			case 38: // '&'
				c = '\b';
				break;

			case 46: // '.'
				c = '\200';
				break;

			case 18: // '\022'
				c = '\020';
				break;

			case 3: // '\003'
				c = '\u0400';
				break;

			case 30: // '\036'
				c = '\u0100';
				break;

			case 49: // '1'
				c = '@';
				break;

			case 42: // '*'
				c = ' ';
				break;

			case 39: // '\''
				c = '\u0800';
				break;

			default:
				return i;
			}
			if ((i & c) != 0) {
				this.log.error(this.S.pos, "repeated.modifier");
			}
			i |= c;
			this.S.nextToken();
		} while (true);
	}

	private List variableDeclarators(final int i, final Tree tree) {
		return this.variableDeclaratorsRest(this.S.pos, i, tree, this.ident(), false, null);
	}

	private List variableDeclaratorsRest(final int i, final int j, final Tree tree, final Name name, final boolean flag, final String s) {
		final ListBuffer listbuffer = new ListBuffer();
		listbuffer.append(this.variableDeclaratorRest(i, j, tree, name, flag, s));
		while (this.S.token == 67) {
			this.S.nextToken();
			listbuffer.append(this.variableDeclarator(j, tree, flag, s));
		}
		return listbuffer.toList();
	}

	private VarDef variableDeclarator(final int i, final Tree tree, final boolean flag, final String s) {
		return this.variableDeclaratorRest(this.S.pos, i, tree, this.ident(), flag, s);
	}

	private VarDef variableDeclaratorRest(final int i, final int j, Tree tree, final Name name, final boolean flag, final String s) {
		tree = this.bracketsOpt(tree);
		Tree tree1 = null;
		if (this.S.token == 69) {
			this.S.nextToken();
			tree1 = this.variableInitializer();
		} else if (flag) {
			this.syntaxError(this.S.pos, "expected", Scanner.token2string(69));
		}
		final VarDef vardef = this.F.at(i).VarDef(j, name, tree, tree1);
		this.attach(vardef, s);
		return vardef;
	}

	private VarDef variableDeclaratorId(final int i, Tree tree) {
		final int j = this.S.pos;
		final Name name = this.ident();
		tree = this.bracketsOpt(tree);
		return this.F.at(j).VarDef(i, name, tree, null);
	}

	public TopLevel compilationUnit() {
		final int i = this.S.pos;
		Tree tree = null;
		final String s = this.S.docComment;
		if (this.S.token == 32) {
			this.S.nextToken();
			tree = this.qualident();
			this.accept(66);
		}
		final ListBuffer listbuffer = new ListBuffer();
		while (this.S.token == 25) {
			listbuffer.append(this.importDeclaration());
		}
		while (this.S.token != 0) {
			listbuffer.append(this.typeDeclaration());
		}
		final TopLevel toplevel = this.F.at(i).TopLevel(tree, listbuffer.toList());
		this.attach(toplevel, s);
		if (this.keepDocComments) {
			toplevel.docComments = this.docComments;
		}
		return toplevel;
	}

	private Tree importDeclaration() {
		final int i = this.S.pos;
		this.S.nextToken();
		Object obj = this.F.at(this.S.pos).Ident(this.ident());
		for (boolean flag = false; this.S.token == 68 && !flag;) {
			this.S.nextToken();
			if (this.S.token == 86) {
				obj = this.F.at(this.S.pos).Select((Tree) obj, Names.asterisk);
				this.S.nextToken();
				flag = true;
			} else {
				obj = this.F.at(this.S.pos).Select((Tree) obj, this.ident());
			}
		}

		this.accept(66);
		return this.F.at(i).Import((Tree) obj);
	}

	private Tree typeDeclaration() {
		int i = 0;
		if (this.S.pos == this.S.errPos) {
			for (i = this.modifiersOpt(); this.S.token != 10 && this.S.token != 28 && this.S.token != 0; i = this.modifiersOpt()) {
				this.S.nextToken();
			}
		}

		final int j = this.S.pos;
		if (this.S.token == 66) {
			this.S.nextToken();
			return this.F.at(j).Block(0, Tree.emptyList);
		}
		final String s = this.S.docComment;
		i |= this.modifiersOpt();
		return this.classOrInterfaceDeclaration(i, s);
	}

	private Tree classOrInterfaceDeclaration(int i, final String s) {
		i |= this.modifiersOpt();
		if (this.S.token == 10) {
			return this.classDeclaration(i, s);
		}
		return this.S.token == 28 ? this.interfaceDeclaration(i, s) : this.syntaxError("class.or.intf.expected");
	}

	private Tree classDeclaration(final int i, final String s) {
		final int j = this.S.pos;
		this.accept(10);
		final Name name = this.ident();
		final List list = TypeParameter.emptyList;
		Tree tree = null;
		if (this.S.token == 17) {
			this.S.nextToken();
			tree = this.type();
		}
		List list1 = Tree.emptyList;
		if (this.S.token == 24) {
			this.S.nextToken();
			list1 = this.typeList();
		}
		final List list2 = this.classOrInterfaceBody(name, false);
		final ClassDef classdef = this.F.at(j).ClassDef(i, name, list, tree, list1, list2);
		this.attach(classdef, s);
		return classdef;
	}

	private Tree interfaceDeclaration(final int i, final String s) {
		final int j = this.S.pos;
		this.accept(28);
		final Name name = this.ident();
		final List list = TypeParameter.emptyList;
		List list1 = Tree.emptyList;
		if (this.S.token == 17) {
			this.S.nextToken();
			list1 = this.typeList();
		}
		final List list2 = this.classOrInterfaceBody(name, true);
		final ClassDef classdef = this.F.at(j).ClassDef(i | 0x200, name, list, null, list1, list2);
		this.attach(classdef, s);
		return classdef;
	}

	private List typeList() {
		final ListBuffer listbuffer = new ListBuffer();
		listbuffer.append(this.type());
		while (this.S.token == 67) {
			this.S.nextToken();
			listbuffer.append(this.type());
		}
		return listbuffer.toList();
	}

	private List classOrInterfaceBody(final Name name, final boolean flag) {
		this.accept(62);
		final ListBuffer listbuffer = new ListBuffer();
		while (this.S.token != 63 && this.S.token != 0) {
			listbuffer.appendList(this.classOrInterfaceBodyDeclaration(name, flag));
		}
		this.accept(63);
		return listbuffer.toList();
	}

	private List classOrInterfaceBodyDeclaration(final Name name, final boolean flag) {
		int i = this.S.pos;
		if (this.S.token == 66) {
			this.S.nextToken();
			return Tree.emptyList.prepend(this.F.at(i).Block(0, Tree.emptyList));
		}
		final String s = this.S.docComment;
		final int j = this.modifiersOpt();
		if (this.S.token == 10 || this.S.token == 28) {
			return Tree.emptyList.prepend(this.classOrInterfaceDeclaration(j, s));
		}
		if (this.S.token == 62 && !flag && (j & 0xfff & -9) == 0) {
			return Tree.emptyList.prepend(this.block(j));
		}
		final List list = TypeParameter.emptyList;
		Name name1 = this.S.name;
		i = this.S.pos;
		final boolean flag1 = this.S.token == 48;
		final Object obj;
		if (flag1) {
			obj = this.F.at(i).TypeIdent(9);
			this.S.nextToken();
		} else {
			obj = this.type();
		}
		if (this.S.token == 60 && !flag && ((Tree) obj).tag == 31) {
			if (flag || name1 != name) {
				this.log.error(i, "invalid.meth.decl.ret.type.req");
			}
			return Tree.emptyList.prepend(this.methodDeclaratorRest(i, j, null, Names.init, list, flag, true, s));
		}
		i = this.S.pos;
		name1 = this.ident();
		if (this.S.token == 60) {
			return Tree.emptyList.prepend(this.methodDeclaratorRest(i, j, (Tree) obj, name1, list, flag, flag1, s));
		}
		if (!flag1 && list.isEmpty()) {
			final List list1 = this.variableDeclaratorsRest(i, j, (Tree) obj, name1, flag, s);
			this.accept(66);
			return list1;
		}
		this.syntaxError(this.S.pos, "expected", Scanner.token2string(60));
		return Tree.emptyList;
	}

	private Tree methodDeclaratorRest(final int i, final int j, Tree tree, final Name name, final List list, final boolean flag, final boolean flag1, final String s) {
		final List list1 = this.formalParameters();
		if (!flag1) {
			tree = this.bracketsOpt(tree);
		}
		List list2 = Tree.emptyList;
		if (this.S.token == 45) {
			this.S.nextToken();
			list2 = this.qualidentList();
		}
		final Block block1;
		if (this.S.token == 62) {
			if (flag) {
				this.log.error(this.S.pos, "intf.meth.cant.have.body");
			}
			block1 = this.block();
		} else {
			this.accept(66);
			block1 = null;
		}
		final MethodDef methoddef = this.F.at(i).MethodDef(j, name, tree, list, list1, list2, block1);
		this.attach(methoddef, s);
		return methoddef;
	}

	private List qualidentList() {
		final ListBuffer listbuffer = new ListBuffer();
		listbuffer.append(this.qualident());
		while (this.S.token == 67) {
			this.S.nextToken();
			listbuffer.append(this.qualident());
		}
		return listbuffer.toList();
	}

	private List formalParameters() {
		final ListBuffer listbuffer = new ListBuffer();
		this.accept(60);
		if (this.S.token != 61) {
			listbuffer.append(this.formalParameter());
			while (this.S.token == 67) {
				this.S.nextToken();
				listbuffer.append(this.formalParameter());
			}
		}
		this.accept(61);
		return listbuffer.toList();
	}

	private int optFinal() {
		if (this.S.token == 18) {
			this.S.nextToken();
			return 16;
		}
		return 0;
	}

	private VarDef formalParameter() {
		return this.variableDeclaratorId(this.optFinal(), this.type());
	}

	private Tree checkExprStat(final Tree tree) {
		switch (tree.tag) {
		case 23: // '\027'
		case 24: // '\030'
		case 26: // '\032'
		case 37: // '%'
		case 42: // '*'
		case 43: // '+'
		case 44: // ','
		case 45: // '-'
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
			return tree;

		case 25: // '\031'
		case 27: // '\033'
		case 28: // '\034'
		case 29: // '\035'
		case 30: // '\036'
		case 31: // '\037'
		case 32: // ' '
		case 33: // '!'
		case 34: // '"'
		case 35: // '#'
		case 36: // '$'
		case 38: // '&'
		case 39: // '\''
		case 40: // '('
		case 41: // ')'
		case 46: // '.'
		case 47: // '/'
		case 48: // '0'
		case 49: // '1'
		case 50: // '2'
		case 51: // '3'
		case 52: // '4'
		case 53: // '5'
		case 54: // '6'
		case 55: // '7'
		case 56: // '8'
		case 57: // '9'
		case 58: // ':'
		case 59: // ';'
		case 60: // '<'
		case 61: // '='
		case 62: // '>'
		case 63: // '?'
		case 64: // '@'
		case 68: // 'D'
		case 69: // 'E'
		case 70: // 'F'
		case 71: // 'G'
		case 72: // 'H'
		case 73: // 'I'
		default:
			this.log.error(tree.pos, "not.stmt");
			break;
		}
		return errorTree;
	}

	private static int prec(final int i) {
		final int j = optag(i);
		return j < 0 ? -1 : TreeInfo.opPrec(j);
	}

	private static int optag(final int i) {
		switch (i) {
		case 81: // 'Q'
			return 46;

		case 80: // 'P'
			return 47;

		case 89: // 'Y'
			return 48;

		case 100: // 'd'
			return 65;

		case 90: // 'Z'
			return 49;

		case 101: // 'e'
			return 66;

		case 88: // 'X'
			return 50;

		case 99: // 'c'
			return 67;

		case 76: // 'L'
			return 51;

		case 79: // 'O'
			return 52;

		case 71: // 'G'
			return 53;

		case 70: // 'F'
			return 54;

		case 77: // 'M'
			return 55;

		case 78: // 'N'
			return 56;

		case 92: // '\\'
			return 57;

		case 103: // 'g'
			return 74;

		case 93: // ']'
			return 58;

		case 104: // 'h'
			return 75;

		case 94: // '^'
			return 59;

		case 105: // 'i'
			return 76;

		case 84: // 'T'
			return 60;

		case 95: // '_'
			return 77;

		case 85: // 'U'
			return 61;

		case 96: // '`'
			return 78;

		case 86: // 'V'
			return 62;

		case 97: // 'a'
			return 79;

		case 87: // 'W'
			return 63;

		case 98: // 'b'
			return 80;

		case 91: // '['
			return 64;

		case 102: // 'f'
			return 81;

		case 26: // '\032'
			return 28;

		case 27: // '\033'
		case 28: // '\034'
		case 29: // '\035'
		case 30: // '\036'
		case 31: // '\037'
		case 32: // ' '
		case 33: // '!'
		case 34: // '"'
		case 35: // '#'
		case 36: // '$'
		case 37: // '%'
		case 38: // '&'
		case 39: // '\''
		case 40: // '('
		case 41: // ')'
		case 42: // '*'
		case 43: // '+'
		case 44: // ','
		case 45: // '-'
		case 46: // '.'
		case 47: // '/'
		case 48: // '0'
		case 49: // '1'
		case 50: // '2'
		case 51: // '3'
		case 52: // '4'
		case 53: // '5'
		case 54: // '6'
		case 55: // '7'
		case 56: // '8'
		case 57: // '9'
		case 58: // ':'
		case 59: // ';'
		case 60: // '<'
		case 61: // '='
		case 62: // '>'
		case 63: // '?'
		case 64: // '@'
		case 65: // 'A'
		case 66: // 'B'
		case 67: // 'C'
		case 68: // 'D'
		case 69: // 'E'
		case 72: // 'H'
		case 73: // 'I'
		case 74: // 'J'
		case 75: // 'K'
		case 82: // 'R'
		case 83: // 'S'
		default:
			return -1;
		}
	}

	private static int unoptag(final int i) {
		switch (i) {
		case 84: // 'T'
			return 38;

		case 85: // 'U'
			return 39;

		case 72: // 'H'
			return 40;

		case 73: // 'I'
			return 41;

		case 82: // 'R'
			return 42;

		case 83: // 'S'
			return 43;

		case 74: // 'J'
		case 75: // 'K'
		case 76: // 'L'
		case 77: // 'M'
		case 78: // 'N'
		case 79: // 'O'
		case 80: // 'P'
		case 81: // 'Q'
		default:
			return -1;
		}
	}

	private static int typetag(final int i) {
		switch (i) {
		case 6: // '\006'
			return 1;

		case 9: // '\t'
			return 2;

		case 37: // '%'
			return 3;

		case 27: // '\033'
			return 4;

		case 29: // '\035'
			return 5;

		case 20: // '\024'
			return 6;

		case 15: // '\017'
			return 7;

		case 4: // '\004'
			return 8;
		}
		return -1;
	}

	private final Scanner S;
	private final TreeMaker F;
	private final Log log;
	private final boolean keepDocComments;
	static final int EXPR = 1;
	static final int TYPE = 2;
	static final int NOPARAMS = 4;
	private int mode;
	private int lastmode;
	private static final Tree errorTree = new Erroneous();
	private Hashtable docComments;
	private final ListBuffer odStackSupply;
	private final ListBuffer opStackSupply;

}
