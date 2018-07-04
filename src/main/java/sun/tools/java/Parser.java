package sun.tools.java;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import sun.tools.tree.AddExpression;
import sun.tools.tree.AndExpression;
import sun.tools.tree.ArrayAccessExpression;
import sun.tools.tree.ArrayExpression;
import sun.tools.tree.AssignAddExpression;
import sun.tools.tree.AssignBitAndExpression;
import sun.tools.tree.AssignBitOrExpression;
import sun.tools.tree.AssignBitXorExpression;
import sun.tools.tree.AssignDivideExpression;
import sun.tools.tree.AssignExpression;
import sun.tools.tree.AssignMultiplyExpression;
import sun.tools.tree.AssignOpExpression;
import sun.tools.tree.AssignRemainderExpression;
import sun.tools.tree.AssignShiftLeftExpression;
import sun.tools.tree.AssignShiftRightExpression;
import sun.tools.tree.AssignSubtractExpression;
import sun.tools.tree.AssignUnsignedShiftRightExpression;
import sun.tools.tree.BitAndExpression;
import sun.tools.tree.BitNotExpression;
import sun.tools.tree.BitOrExpression;
import sun.tools.tree.BitXorExpression;
import sun.tools.tree.BooleanExpression;
import sun.tools.tree.BreakStatement;
import sun.tools.tree.CaseStatement;
import sun.tools.tree.CastExpression;
import sun.tools.tree.CatchStatement;
import sun.tools.tree.CharExpression;
import sun.tools.tree.CommaExpression;
import sun.tools.tree.CompoundStatement;
import sun.tools.tree.ConditionalExpression;
import sun.tools.tree.ContinueStatement;
import sun.tools.tree.DeclarationStatement;
import sun.tools.tree.DivideExpression;
import sun.tools.tree.DoStatement;
import sun.tools.tree.DoubleExpression;
import sun.tools.tree.EqualExpression;
import sun.tools.tree.ExprExpression;
import sun.tools.tree.Expression;
import sun.tools.tree.ExpressionStatement;
import sun.tools.tree.FieldExpression;
import sun.tools.tree.FinallyStatement;
import sun.tools.tree.FloatExpression;
import sun.tools.tree.ForStatement;
import sun.tools.tree.GreaterExpression;
import sun.tools.tree.GreaterOrEqualExpression;
import sun.tools.tree.IdentifierExpression;
import sun.tools.tree.IfStatement;
import sun.tools.tree.InstanceOfExpression;
import sun.tools.tree.IntExpression;
import sun.tools.tree.LessExpression;
import sun.tools.tree.LessOrEqualExpression;
import sun.tools.tree.LocalMember;
import sun.tools.tree.LongExpression;
import sun.tools.tree.MethodExpression;
import sun.tools.tree.MultiplyExpression;
import sun.tools.tree.NegativeExpression;
import sun.tools.tree.NewArrayExpression;
import sun.tools.tree.NewInstanceExpression;
import sun.tools.tree.Node;
import sun.tools.tree.NotEqualExpression;
import sun.tools.tree.NotExpression;
import sun.tools.tree.NullExpression;
import sun.tools.tree.OrExpression;
import sun.tools.tree.PositiveExpression;
import sun.tools.tree.PostDecExpression;
import sun.tools.tree.PostIncExpression;
import sun.tools.tree.PreDecExpression;
import sun.tools.tree.PreIncExpression;
import sun.tools.tree.RemainderExpression;
import sun.tools.tree.ReturnStatement;
import sun.tools.tree.ShiftLeftExpression;
import sun.tools.tree.ShiftRightExpression;
import sun.tools.tree.Statement;
import sun.tools.tree.StringExpression;
import sun.tools.tree.SubtractExpression;
import sun.tools.tree.SuperExpression;
import sun.tools.tree.SwitchStatement;
import sun.tools.tree.SynchronizedStatement;
import sun.tools.tree.ThisExpression;
import sun.tools.tree.ThrowStatement;
import sun.tools.tree.TryStatement;
import sun.tools.tree.TypeExpression;
import sun.tools.tree.UnsignedShiftRightExpression;
import sun.tools.tree.VarDeclarationStatement;
import sun.tools.tree.WhileStatement;

public class Parser extends Scanner implements ParserActions {

	protected Parser(final Environment environment, final InputStream inputstream) throws IOException {
		super(environment, inputstream);
		this.args = new Node[32];
		this.argIndex = 0;
		this.aCount = 0;
		this.aTypes = new Type[8];
		this.aNames = new IdentifierToken[this.aTypes.length];
		this.FPstate = 0;
		this.scanner = this;
		this.actions = this;
	}

	private Parser(final Scanner scanner1) {
		super(scanner1.env);
		this.args = new Node[32];
		this.argIndex = 0;
		this.aCount = 0;
		this.aTypes = new Type[8];
		this.aNames = new IdentifierToken[this.aTypes.length];
		this.FPstate = 0;
		this.scanner = scanner1;
		this.env = scanner1.env;
		this.token = scanner1.token;
		this.pos = scanner1.pos;
		this.actions = this;
	}

	/**
	 * @deprecated Method packageDeclaration is deprecated
	 */
	public void packageDeclaration(final long l, final IdentifierToken identifiertoken) {
		throw new RuntimeException("beginClass method is abstract");
	}

	/**
	 * @deprecated Method importClass is deprecated
	 */
	public void importClass(final long l, final IdentifierToken identifiertoken) {
		throw new RuntimeException("importClass method is abstract");
	}

	/**
	 * @deprecated Method importPackage is deprecated
	 */
	public void importPackage(final long l, final IdentifierToken identifiertoken) {
		throw new RuntimeException("importPackage method is abstract");
	}

	/**
	 * @deprecated Method beginClass is deprecated
	 */
	public ClassDefinition beginClass(final long l, final String s, final int i, final IdentifierToken identifiertoken, final IdentifierToken identifiertoken1, final IdentifierToken aidentifiertoken[]) {
		if (aidentifiertoken != null) {
			final Identifier[] aidentifier = new Identifier[aidentifiertoken.length];
			for (int j = 0; j < aidentifiertoken.length; j++) {
				aidentifier[j] = aidentifiertoken[j].id;
			}

		}
		throw new RuntimeException("beginClass method is abstract");
	}

	protected ClassDefinition getCurrentClass() {
		return null;
	}

	/**
	 * @deprecated Method endClass is deprecated
	 */
	public void endClass(final long l, final ClassDefinition classdefinition) {
		throw new RuntimeException("endClass method is abstract");
	}

	/**
	 * @deprecated Method defineField is deprecated
	 */
	public void defineField(final long l, final ClassDefinition classdefinition, final String s, final int i, final Type type, final IdentifierToken identifiertoken, final IdentifierToken aidentifiertoken[], final IdentifierToken aidentifiertoken1[], final Node node) {
		if (aidentifiertoken != null) {
			final Identifier[] aidentifier = new Identifier[aidentifiertoken.length];
			for (int j = 0; j < aidentifiertoken.length; j++) {
				aidentifier[j] = aidentifiertoken[j].id;
			}

		}
		if (aidentifiertoken1 != null) {
			final Identifier[] aidentifier1 = new Identifier[aidentifiertoken1.length];
			for (int k = 0; k < aidentifiertoken1.length; k++) {
				aidentifier1[k] = aidentifiertoken1[k].id;
			}

		}
		throw new RuntimeException("defineField method is abstract");
	}

	private void addArgument(final Node node) {
		if (this.argIndex == this.args.length) {
			final Node anode[] = new Node[this.args.length * 2];
			System.arraycopy(this.args, 0, anode, 0, this.args.length);
			this.args = anode;
		}
		this.args[this.argIndex++] = node;
	}

	private Expression[] exprArgs(final int i) {
		final Expression aexpression[] = new Expression[this.argIndex - i];
		System.arraycopy(this.args, i, aexpression, 0, this.argIndex - i);
		this.argIndex = i;
		return aexpression;
	}

	private Statement[] statArgs(final int i) {
		final Statement astatement[] = new Statement[this.argIndex - i];
		System.arraycopy(this.args, i, astatement, 0, this.argIndex - i);
		this.argIndex = i;
		return astatement;
	}

	private void expect(final int i) throws SyntaxError, IOException {
		if (this.token != i) {
			switch (i) {
			case 60: // '<'
				this.env.error(this.scanner.prevPos, "identifier.expected");
				break;

			default:
				this.env.error(this.scanner.prevPos, "token.expected", Constants.opNames[i]);
				break;
			}
			throw new SyntaxError();
		}
		this.scan();
	}

	private Expression parseTypeExpression() throws SyntaxError, IOException {
		switch (this.token) {
		case 77: // 'M'
			return new TypeExpression(this.scan(), Type.tVoid);

		case 78: // 'N'
			return new TypeExpression(this.scan(), Type.tBoolean);

		case 70: // 'F'
			return new TypeExpression(this.scan(), Type.tByte);

		case 71: // 'G'
			return new TypeExpression(this.scan(), Type.tChar);

		case 72: // 'H'
			return new TypeExpression(this.scan(), Type.tShort);

		case 73: // 'I'
			return new TypeExpression(this.scan(), Type.tInt);

		case 74: // 'J'
			return new TypeExpression(this.scan(), Type.tLong);

		case 75: // 'K'
			return new TypeExpression(this.scan(), Type.tFloat);

		case 76: // 'L'
			return new TypeExpression(this.scan(), Type.tDouble);

		case 60: // '<'
			Object obj = new IdentifierExpression(this.pos, this.scanner.idValue);
			this.scan();
			while (this.token == 46) {
				obj = new FieldExpression(this.scan(), (Expression) obj, this.scanner.idValue);
				this.expect(60);
			}
			return (Expression) obj;

		case 61: // '='
		case 62: // '>'
		case 63: // '?'
		case 64: // '@'
		case 65: // 'A'
		case 66: // 'B'
		case 67: // 'C'
		case 68: // 'D'
		case 69: // 'E'
		default:
			this.env.error(this.pos, "type.expected");
			throw new SyntaxError();
		}
	}

	private Expression parseMethodExpression(final Expression expression, final Identifier identifier) throws SyntaxError, IOException {
		final long l = this.scan();
		final int i = this.argIndex;
		if (this.token != 141) {
			this.addArgument(this.parseExpression());
			while (this.token == 0) {
				this.scan();
				this.addArgument(this.parseExpression());
			}
		}
		this.expect(141);
		return new MethodExpression(l, expression, identifier, this.exprArgs(i));
	}

	private Expression parseNewInstanceExpression(final long l, final Expression expression, final Expression expression1) throws SyntaxError, IOException {
		final int i = this.argIndex;
		this.expect(140);
		if (this.token != 141) {
			this.addArgument(this.parseExpression());
			while (this.token == 0) {
				this.scan();
				this.addArgument(this.parseExpression());
			}
		}
		this.expect(141);
		ClassDefinition classdefinition = null;
		if (this.token == 138 && !(expression1 instanceof TypeExpression)) {
			final long l1 = this.pos;
			final Identifier identifier = FieldExpression.toIdentifier(expression1);
			if (identifier == null) {
				this.env.error(expression1.getWhere(), "type.expected");
			}
			final Vector vector = new Vector(1);
			vector.addElement(new IdentifierToken(Constants.idNull));
			final Vector vector1 = new Vector(0);
			if (this.token == 113 || this.token == 112) {
				this.env.error(this.pos, "anonymous.extends");
				this.parseInheritance(vector, vector1);
			}
			classdefinition = this.parseClassBody(new IdentifierToken(l1, Constants.idNull), 0x30000, 56, null, vector, vector1, expression1.getWhere());
		}
		return expression == null && classdefinition == null ? new NewInstanceExpression(l, expression1, this.exprArgs(i)) : new NewInstanceExpression(l, expression1, this.exprArgs(i), expression, classdefinition);
	}

	private Expression parseTerm() throws SyntaxError, IOException {
		switch (this.token) {
		case 63: // '?'
			final char c = this.scanner.charValue;
			return new CharExpression(this.scan(), c);

		case 65: // 'A'
			final int i = this.scanner.intValue;
			final long l6 = this.scan();
			if (i < 0 && this.radix == 10) {
				this.env.error(l6, "overflow.int.dec");
			}
			return new IntExpression(l6, i);

		case 66: // 'B'
			final long l = this.scanner.longValue;
			final long l8 = this.scan();
			if (l < 0L && this.radix == 10) {
				this.env.error(l8, "overflow.long.dec");
			}
			return new LongExpression(l8, l);

		case 67: // 'C'
			final float f = this.scanner.floatValue;
			return new FloatExpression(this.scan(), f);

		case 68: // 'D'
			final double d = this.scanner.doubleValue;
			return new DoubleExpression(this.scan(), d);

		case 69: // 'E'
			final String s = this.scanner.stringValue;
			return new StringExpression(this.scan(), s);

		case 60: // '<'
			final Identifier identifier = this.scanner.idValue;
			final long l7 = this.scan();
			return this.token != 140 ? new IdentifierExpression(l7, identifier) : this.parseMethodExpression(null, identifier);

		case 80: // 'P'
			return new BooleanExpression(this.scan(), true);

		case 81: // 'Q'
			return new BooleanExpression(this.scan(), false);

		case 84: // 'T'
			return new NullExpression(this.scan());

		case 82: // 'R'
			final Expression thisexpression = new ThisExpression(this.scan());
			return this.token != 140 ? thisexpression : this.parseMethodExpression(thisexpression, Constants.idInit);

		case 83: // 'S'
			final Expression superexpression = new SuperExpression(this.scan());
			return this.token != 140 ? superexpression : this.parseMethodExpression(superexpression, Constants.idInit);

		case 70: // 'F'
		case 71: // 'G'
		case 72: // 'H'
		case 73: // 'I'
		case 74: // 'J'
		case 75: // 'K'
		case 76: // 'L'
		case 77: // 'M'
		case 78: // 'N'
			return this.parseTypeExpression();

		case 29: // '\035'
			final long l1 = this.scan();
			switch (this.token) {
			case 65: // 'A'
				final int j = this.scanner.intValue;
				final long l11 = this.scan();
				if (j < 0 && this.radix == 10) {
					this.env.error(l11, "overflow.int.dec");
				}
				return new IntExpression(l11, j);

			case 66: // 'B'
				final long l9 = this.scanner.longValue;
				final long l12 = this.scan();
				if (l9 < 0L && this.radix == 10) {
					this.env.error(l12, "overflow.long.dec");
				}
				return new LongExpression(l12, l9);

			case 67: // 'C'
				final float f1 = this.scanner.floatValue;
				return new FloatExpression(this.scan(), f1);

			case 68: // 'D'
				final double d1 = this.scanner.doubleValue;
				return new DoubleExpression(this.scan(), d1);
			}
			return new PositiveExpression(l1, this.parseTerm());

		case 30: // '\036'
			final long l2 = this.scan();
			switch (this.token) {
			case 65: // 'A'
				final int k = -this.scanner.intValue;
				return new IntExpression(this.scan(), k);

			case 66: // 'B'
				final long l10 = -this.scanner.longValue;
				return new LongExpression(this.scan(), l10);

			case 67: // 'C'
				final float f2 = -this.scanner.floatValue;
				return new FloatExpression(this.scan(), f2);

			case 68: // 'D'
				final double d2 = -this.scanner.doubleValue;
				return new DoubleExpression(this.scan(), d2);
			}
			return new NegativeExpression(l2, this.parseTerm());

		case 37: // '%'
			return new NotExpression(this.scan(), this.parseTerm());

		case 38: // '&'
			return new BitNotExpression(this.scan(), this.parseTerm());

		case 50: // '2'
			return new PreIncExpression(this.scan(), this.parseTerm());

		case 51: // '3'
			return new PreDecExpression(this.scan(), this.parseTerm());

		case 140:
			final long l3 = this.scan();
			final Expression expression = this.parseExpression();
			this.expect(141);
			if (expression.getOp() == 147) {
				return new CastExpression(l3, expression, this.parseTerm());
			}
			switch (this.token) {
			case 50: // '2'
				return new PostIncExpression(this.scan(), expression);

			case 51: // '3'
				return new PostDecExpression(this.scan(), expression);

			case 37: // '%'
			case 38: // '&'
			case 49: // '1'
			case 60: // '<'
			case 63: // '?'
			case 65: // 'A'
			case 66: // 'B'
			case 67: // 'C'
			case 68: // 'D'
			case 69: // 'E'
			case 80: // 'P'
			case 81: // 'Q'
			case 82: // 'R'
			case 83: // 'S'
			case 84: // 'T'
			case 140:
				return new CastExpression(l3, expression, this.parseTerm());
			}
			return new ExprExpression(l3, expression);

		case 138:
			final long l4 = this.scan();
			final int i1 = this.argIndex;
			if (this.token != 139) {
				this.addArgument(this.parseExpression());
				while (this.token == 0) {
					this.scan();
					if (this.token == 139) {
						break;
					}
					this.addArgument(this.parseExpression());
				}
			}
			this.expect(139);
			return new ArrayExpression(l4, this.exprArgs(i1));

		case 49: // '1'
			final long l5 = this.scan();
			final int j1 = this.argIndex;
			if (this.token == 140) {
				this.scan();
				this.parseExpression();
				this.expect(141);
				this.env.error(l5, "not.supported", "new(...)");
				return new NullExpression(l5);
			}
			final Expression expression2 = this.parseTypeExpression();
			if (this.token == 142) {
				while (this.token == 142) {
					this.scan();
					this.addArgument(this.token == 143 ? null : (Node) this.parseExpression());
					this.expect(143);
				}
				final Expression aexpression[] = this.exprArgs(j1);
				return this.token == 138 ? new NewArrayExpression(l5, expression2, aexpression, this.parseTerm()) : new NewArrayExpression(l5, expression2, aexpression);
			}
			return this.parseNewInstanceExpression(l5, null, expression2);

		case 31: // '\037'
		case 32: // ' '
		case 33: // '!'
		case 34: // '"'
		case 35: // '#'
		case 36: // '$'
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
		case 52: // '4'
		case 53: // '5'
		case 54: // '6'
		case 55: // '7'
		case 56: // '8'
		case 57: // '9'
		case 58: // ':'
		case 59: // ';'
		case 61: // '='
		case 62: // '>'
		case 64: // '@'
		case 79: // 'O'
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
		case 106: // 'j'
		case 107: // 'k'
		case 108: // 'l'
		case 109: // 'm'
		case 110: // 'n'
		case 111: // 'o'
		case 112: // 'p'
		case 113: // 'q'
		case 114: // 'r'
		case 115: // 's'
		case 116: // 't'
		case 117: // 'u'
		case 118: // 'v'
		case 119: // 'w'
		case 120: // 'x'
		case 121: // 'y'
		case 122: // 'z'
		case 123: // '{'
		case 124: // '|'
		case 125: // '}'
		case 126: // '~'
		case 127: // '\177'
		case 128:
		case 129:
		case 130:
		case 131:
		case 132:
		case 133:
		case 134:
		case 135:
		case 136:
		case 137:
		case 139:
		default:
			this.env.error(this.scanner.prevPos, "missing.term");
			return new IntExpression(this.pos, 0);
		}
	}

	private Expression parseExpression() throws SyntaxError, IOException {
		for (Expression expression = this.parseTerm(); expression != null; expression = expression.order()) {
			final Expression expression1 = this.parseBinaryExpression(expression);
			if (expression1 == null) {
				return expression;
			}
			expression = expression1;
		}

		return null;
	}

	private Expression parseBinaryExpression(Expression expression) throws SyntaxError, IOException {
		if (expression != null) {
			switch (this.token) {
			case 142:
				final long l = this.scan();
				final Expression expression1 = this.token == 143 ? null : this.parseExpression();
				this.expect(143);
				return new ArrayAccessExpression(l, expression, expression1);

			case 50: // '2'
				return new PostIncExpression(this.scan(), expression);

			case 51: // '3'
				return new PostDecExpression(this.scan(), expression);

			case 46: // '.'
				final long l1 = this.scan();
				if (this.token == 82) {
					final long l3 = this.scan();
					if (this.token == 140) {
						expression = new ThisExpression(l3, expression);
						return this.parseMethodExpression(expression, Constants.idInit);
					}
					return new FieldExpression(l1, expression, Constants.idThis);
				}
				if (this.token == 83) {
					final long l4 = this.scan();
					if (this.token == 140) {
						expression = new SuperExpression(l4, expression);
						return this.parseMethodExpression(expression, Constants.idInit);
					}
					return new FieldExpression(l1, expression, Constants.idSuper);
				}
				if (this.token == 49) {
					this.scan();
					if (this.token != 60) {
						this.expect(60);
					}
					return this.parseNewInstanceExpression(l1, expression, this.parseTypeExpression());
				}
				if (this.token == 111) {
					this.scan();
					return new FieldExpression(l1, expression, Constants.idClass);
				}
				final Identifier identifier = this.scanner.idValue;
				this.expect(60);
				return this.token == 140 ? this.parseMethodExpression(expression, identifier) : new FieldExpression(l1, expression, identifier);

			case 25: // '\031'
				return new InstanceOfExpression(this.scan(), expression, this.parseTerm());

			case 29: // '\035'
				return new AddExpression(this.scan(), expression, this.parseTerm());

			case 30: // '\036'
				return new SubtractExpression(this.scan(), expression, this.parseTerm());

			case 33: // '!'
				return new MultiplyExpression(this.scan(), expression, this.parseTerm());

			case 31: // '\037'
				return new DivideExpression(this.scan(), expression, this.parseTerm());

			case 32: // ' '
				return new RemainderExpression(this.scan(), expression, this.parseTerm());

			case 26: // '\032'
				return new ShiftLeftExpression(this.scan(), expression, this.parseTerm());

			case 27: // '\033'
				return new ShiftRightExpression(this.scan(), expression, this.parseTerm());

			case 28: // '\034'
				return new UnsignedShiftRightExpression(this.scan(), expression, this.parseTerm());

			case 24: // '\030'
				return new LessExpression(this.scan(), expression, this.parseTerm());

			case 23: // '\027'
				return new LessOrEqualExpression(this.scan(), expression, this.parseTerm());

			case 22: // '\026'
				return new GreaterExpression(this.scan(), expression, this.parseTerm());

			case 21: // '\025'
				return new GreaterOrEqualExpression(this.scan(), expression, this.parseTerm());

			case 20: // '\024'
				return new EqualExpression(this.scan(), expression, this.parseTerm());

			case 19: // '\023'
				return new NotEqualExpression(this.scan(), expression, this.parseTerm());

			case 18: // '\022'
				return new BitAndExpression(this.scan(), expression, this.parseTerm());

			case 17: // '\021'
				return new BitXorExpression(this.scan(), expression, this.parseTerm());

			case 16: // '\020'
				return new BitOrExpression(this.scan(), expression, this.parseTerm());

			case 15: // '\017'
				return new AndExpression(this.scan(), expression, this.parseTerm());

			case 14: // '\016'
				return new OrExpression(this.scan(), expression, this.parseTerm());

			case 1: // '\001'
				return new AssignExpression(this.scan(), expression, this.parseTerm());

			case 2: // '\002'
				return new AssignMultiplyExpression(this.scan(), expression, this.parseTerm());

			case 3: // '\003'
				return new AssignDivideExpression(this.scan(), expression, this.parseTerm());

			case 4: // '\004'
				return new AssignRemainderExpression(this.scan(), expression, this.parseTerm());

			case 5: // '\005'
				return new AssignAddExpression(this.scan(), expression, this.parseTerm());

			case 6: // '\006'
				return new AssignSubtractExpression(this.scan(), expression, this.parseTerm());

			case 7: // '\007'
				return new AssignShiftLeftExpression(this.scan(), expression, this.parseTerm());

			case 8: // '\b'
				return new AssignShiftRightExpression(this.scan(), expression, this.parseTerm());

			case 9: // '\t'
				return new AssignUnsignedShiftRightExpression(this.scan(), expression, this.parseTerm());

			case 10: // '\n'
				return new AssignBitAndExpression(this.scan(), expression, this.parseTerm());

			case 11: // '\013'
				return new AssignBitOrExpression(this.scan(), expression, this.parseTerm());

			case 12: // '\f'
				return new AssignBitXorExpression(this.scan(), expression, this.parseTerm());

			case 137:
				final long l2 = this.scan();
				final Expression expression2 = this.parseExpression();
				this.expect(136);
				final Expression expression3 = this.parseExpression();
				if (expression3 instanceof AssignExpression || expression3 instanceof AssignOpExpression) {
					this.env.error(expression3.getWhere(), "assign.in.conditionalexpr");
				}
				return new ConditionalExpression(l2, expression, expression2, expression3);

			case 13: // '\r'
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
			case 47: // '/'
			case 48: // '0'
			case 49: // '1'
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
			case 106: // 'j'
			case 107: // 'k'
			case 108: // 'l'
			case 109: // 'm'
			case 110: // 'n'
			case 111: // 'o'
			case 112: // 'p'
			case 113: // 'q'
			case 114: // 'r'
			case 115: // 's'
			case 116: // 't'
			case 117: // 'u'
			case 118: // 'v'
			case 119: // 'w'
			case 120: // 'x'
			case 121: // 'y'
			case 122: // 'z'
			case 123: // '{'
			case 124: // '|'
			case 125: // '}'
			case 126: // '~'
			case 127: // '\177'
			case 128:
			case 129:
			case 130:
			case 131:
			case 132:
			case 133:
			case 134:
			case 135:
			case 136:
			case 138:
			case 139:
			case 140:
			case 141:
			default:
				return null;
			}
		}
		return expression;
	}

	private boolean recoverStatement() throws SyntaxError, IOException {
		do {
			switch (this.token) {
			case -1:
			case 90: // 'Z'
			case 92: // '\\'
			case 93: // ']'
			case 94: // '^'
			case 98: // 'b'
			case 99: // 'c'
			case 100: // 'd'
			case 101: // 'e'
			case 102: // 'f'
			case 103: // 'g'
			case 138:
			case 139:
				return true;

			case 77: // 'M'
			case 111: // 'o'
			case 114: // 'r'
			case 120: // 'x'
			case 121: // 'y'
			case 124: // '|'
			case 125: // '}'
			case 126: // '~'
				this.expect(139);
				return false;

			case 140:
				this.match(140, 141);
				this.scan();
				break;

			case 142:
				this.match(142, 143);
				this.scan();
				break;

			default:
				this.scan();
				break;
			}
		} while (true);
	}

	private Statement parseDeclaration(final long l, final int i, final Expression expression) throws SyntaxError, IOException {
		final int j = this.argIndex;
		if (this.token == 60) {
			this.addArgument(new VarDeclarationStatement(this.pos, this.parseExpression()));
			while (this.token == 0) {
				this.scan();
				this.addArgument(new VarDeclarationStatement(this.pos, this.parseExpression()));
			}
		}
		return new DeclarationStatement(l, i, expression, this.statArgs(j));
	}

	private void topLevelExpression(final Node expression) {
		switch (expression.getOp()) {
		case 1: // '\001'
		case 2: // '\002'
		case 3: // '\003'
		case 4: // '\004'
		case 5: // '\005'
		case 6: // '\006'
		case 7: // '\007'
		case 8: // '\b'
		case 9: // '\t'
		case 10: // '\n'
		case 11: // '\013'
		case 12: // '\f'
		case 39: // '\''
		case 40: // '('
		case 42: // '*'
		case 44: // ','
		case 45: // '-'
		case 47: // '/'
			return;

		case 13: // '\r'
		case 14: // '\016'
		case 15: // '\017'
		case 16: // '\020'
		case 17: // '\021'
		case 18: // '\022'
		case 19: // '\023'
		case 20: // '\024'
		case 21: // '\025'
		case 22: // '\026'
		case 23: // '\027'
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
		case 36: // '$'
		case 37: // '%'
		case 38: // '&'
		case 41: // ')'
		case 43: // '+'
		case 46: // '.'
		default:
			this.env.error(expression.getWhere(), "invalid.expr");
			break;
		}
	}

	private Statement parseStatement() throws SyntaxError, IOException {
		final long l13;
		final Expression expression5;
		switch (this.token) {
		case 135:
			return new CompoundStatement(this.scan(), new Statement[0]);

		case 138:
			return this.parseBlockStatement();

		case 90: // 'Z'
			final long l = this.scan();
			this.expect(140);
			final Expression expression = this.parseExpression();
			this.expect(141);
			final Statement statement2 = this.parseStatement();
			if (this.token == 91) {
				this.scan();
				return new IfStatement(l, expression, statement2, this.parseStatement());
			}
			return new IfStatement(l, expression, statement2, null);

		case 91: // '['
			this.env.error(this.scan(), "else.without.if");
			return this.parseStatement();

		case 92: // '\\'
			final long l1 = this.scan();
			this.expect(140);
			Object obj = null;
			if (this.token != 135) {
				final long l14 = this.pos;
				final int j1 = this.parseModifiers(16);
				Object obj4 = this.parseExpression();
				if (this.token == 60) {
					obj = this.parseDeclaration(l14, j1, (Expression) obj4);
				} else {
					if (j1 != 0) {
						this.expect(60);
					}
					this.topLevelExpression((Node) obj4);
					while (this.token == 0) {
						final long l17 = this.scan();
						final Expression expression12 = this.parseExpression();
						this.topLevelExpression(expression12);
						obj4 = new CommaExpression(l17, (Expression) obj4, expression12);
					}
					obj = new ExpressionStatement(l14, (Expression) obj4);
				}
			}
			this.expect(135);
			Expression expression6 = null;
			if (this.token != 135) {
				expression6 = this.parseExpression();
			}
			this.expect(135);
			Object obj2 = null;
			if (this.token != 141) {
				obj2 = this.parseExpression();
				this.topLevelExpression((Node) obj2);
				while (this.token == 0) {
					final long l15 = this.scan();
					final Expression expression10 = this.parseExpression();
					this.topLevelExpression(expression10);
					obj2 = new CommaExpression(l15, (Expression) obj2, expression10);
				}
			}
			this.expect(141);
			return new ForStatement(l1, (Statement) obj, expression6, (Expression) obj2, this.parseStatement());

		case 93: // ']'
			final long l2 = this.scan();
			this.expect(140);
			final Expression expression1 = this.parseExpression();
			this.expect(141);
			return new WhileStatement(l2, expression1, this.parseStatement());

		case 94: // '^'
			final long l3 = this.scan();
			final Statement statement1 = this.parseStatement();
			this.expect(93);
			this.expect(140);
			final Expression expression7 = this.parseExpression();
			this.expect(141);
			this.expect(135);
			return new DoStatement(l3, statement1, expression7);

		case 98: // 'b'
			final long l4 = this.scan();
			Identifier identifier = null;
			if (this.token == 60) {
				identifier = this.scanner.idValue;
				this.scan();
			}
			this.expect(135);
			return new BreakStatement(l4, identifier);

		case 99: // 'c'
			final long l5 = this.scan();
			Identifier identifier1 = null;
			if (this.token == 60) {
				identifier1 = this.scanner.idValue;
				this.scan();
			}
			this.expect(135);
			return new ContinueStatement(l5, identifier1);

		case 100: // 'd'
			final long l6 = this.scan();
			Expression expression2 = null;
			if (this.token != 135) {
				expression2 = this.parseExpression();
			}
			this.expect(135);
			return new ReturnStatement(l6, expression2);

		case 95: // '_'
			final long l7 = this.scan();
			final int i = this.argIndex;
			this.expect(140);
			final Expression expression8 = this.parseExpression();
			this.expect(141);
			this.expect(138);
			while (this.token != -1 && this.token != 139) {
				final int i1 = this.argIndex;
				try {
					switch (this.token) {
					case 96: // '`'
						this.addArgument(new CaseStatement(this.scan(), this.parseExpression()));
						this.expect(136);
						break;

					case 97: // 'a'
						this.addArgument(new CaseStatement(this.scan(), null));
						this.expect(136);
						break;

					default:
						this.addArgument(this.parseStatement());
						break;
					}
				} catch (final SyntaxError syntaxerror) {
					this.argIndex = i1;
					if (!this.recoverStatement()) {
						throw syntaxerror;
					}
				}
			}
			this.expect(139);
			return new SwitchStatement(l7, expression8, this.statArgs(i));

		case 96: // '`'
			this.env.error(this.pos, "case.without.switch");
			while (this.token == 96) {
				this.scan();
				this.parseExpression();
				this.expect(136);
			}
			return this.parseStatement();

		case 97: // 'a'
			this.env.error(this.pos, "default.without.switch");
			this.scan();
			this.expect(136);
			return this.parseStatement();

		case 101: // 'e'
			final long l8 = this.scan();
			final Object obj1 = null;
			final int k = this.argIndex;
			Object obj3 = this.parseBlockStatement();
			if (obj1 == null) {
			}
			boolean flag = false;
			while (this.token == 102) {
				final long l16 = this.pos;
				this.expect(102);
				this.expect(140);
				final int k1 = this.parseModifiers(16);
				final Expression expression11 = this.parseExpression();
				final IdentifierToken identifiertoken = this.scanner.getIdToken();
				this.expect(60);
				identifiertoken.modifiers = k1;
				this.expect(141);
				this.addArgument(new CatchStatement(l16, expression11, identifiertoken, this.parseBlockStatement()));
				flag = true;
			}
			if (flag) {
				obj3 = new TryStatement(l8, (Statement) obj3, this.statArgs(k));
			}
			if (this.token == 103) {
				this.scan();
				return new FinallyStatement(l8, (Statement) obj3, this.parseBlockStatement());
			}
			if (flag || obj1 != null) {
				return (Statement) obj3;
			}
			this.env.error(this.pos, "try.without.catch.finally");
			return new TryStatement(l8, (Statement) obj3, null);

		case 102: // 'f'
			this.env.error(this.pos, "catch.without.try");
			Statement statement;
			do {
				this.scan();
				this.expect(140);
				this.parseModifiers(16);
				this.parseExpression();
				this.expect(60);
				this.expect(141);
				statement = this.parseBlockStatement();
			} while (this.token == 102);
			if (this.token == 103) {
				this.scan();
				statement = this.parseBlockStatement();
			}
			return statement;

		case 103: // 'g'
			this.env.error(this.pos, "finally.without.try");
			this.scan();
			return this.parseBlockStatement();

		case 104: // 'h'
			final long l9 = this.scan();
			final Expression expression3 = this.parseExpression();
			this.expect(135);
			return new ThrowStatement(l9, expression3);

		case 58: // ':'
			final long l10 = this.scan();
			this.expect(60);
			this.expect(135);
			this.env.error(l10, "not.supported", "goto");
			return new CompoundStatement(l10, new Statement[0]);

		case 126: // '~'
			final long l11 = this.scan();
			this.expect(140);
			final Expression expression4 = this.parseExpression();
			this.expect(141);
			return new SynchronizedStatement(l11, expression4, this.parseBlockStatement());

		case 111: // 'o'
		case 114: // 'r'
			return this.parseLocalClass(0);

		case 123: // '{'
		case 128:
		case 130:
		case 131:
			final long l12 = this.pos;
			int j = this.parseModifiers(0x200410);
			switch (this.token) {
			case 111: // 'o'
			case 114: // 'r'
				return this.parseLocalClass(j);

			case 60: // '<'
			case 70: // 'F'
			case 71: // 'G'
			case 72: // 'H'
			case 73: // 'I'
			case 74: // 'J'
			case 75: // 'K'
			case 76: // 'L'
			case 78: // 'N'
				if ((j & 0x200400) != 0) {
					j &= 0xffdffbff;
					this.expect(111);
				}
				final Expression expression9 = this.parseExpression();
				if (this.token != 60) {
					this.expect(60);
				}
				final Statement statement5 = this.parseDeclaration(l12, j, expression9);
				this.expect(135);
				return statement5;
			}
			this.env.error(this.pos, "type.expected");
			throw new SyntaxError();

		case 77: // 'M'
		case 120: // 'x'
		case 121: // 'y'
		case 124: // '|'
		case 125: // '}'
			this.env.error(this.pos, "statement.expected");
			throw new SyntaxError();

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
		case 70: // 'F'
		case 71: // 'G'
		case 72: // 'H'
		case 73: // 'I'
		case 74: // 'J'
		case 75: // 'K'
		case 76: // 'L'
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
		case 105: // 'i'
		case 106: // 'j'
		case 107: // 'k'
		case 108: // 'l'
		case 109: // 'm'
		case 110: // 'n'
		case 112: // 'p'
		case 113: // 'q'
		case 115: // 's'
		case 116: // 't'
		case 117: // 'u'
		case 118: // 'v'
		case 119: // 'w'
		case 122: // 'z'
		case 127: // '\177'
		case 129:
		case 132:
		case 133:
		case 134:
		case 136:
		case 137:
		default:
			l13 = this.pos;
			expression5 = this.parseExpression();
			break;
		}
		if (this.token == 60) {
			final Statement statement3 = this.parseDeclaration(l13, 0, expression5);
			this.expect(135);
			return statement3;
		}
		if (this.token == 136) {
			this.scan();
			final Statement statement4 = this.parseStatement();
			statement4.setLabel(this.env, expression5);
			return statement4;
		}
		this.topLevelExpression(expression5);
		this.expect(135);
		return new ExpressionStatement(l13, expression5);
	}

	private Statement parseBlockStatement() throws SyntaxError, IOException {
		if (this.token != 138) {
			this.env.error(this.scanner.prevPos, "token.expected", Constants.opNames[138]);
			return this.parseStatement();
		}
		final long l = this.scan();
		final int i = this.argIndex;
		while (this.token != -1 && this.token != 139) {
			final int j = this.argIndex;
			try {
				this.addArgument(this.parseStatement());
			} catch (final SyntaxError syntaxerror) {
				this.argIndex = j;
				if (!this.recoverStatement()) {
					throw syntaxerror;
				}
			}
		}
		this.expect(139);
		return new CompoundStatement(l, this.statArgs(i));
	}

	private IdentifierToken parseName(final boolean flag) throws SyntaxError, IOException {
		final IdentifierToken identifiertoken = this.scanner.getIdToken();
		this.expect(60);
		if (this.token != 46) {
			return identifiertoken;
		}
		final StringBuffer stringbuffer = new StringBuffer(identifiertoken.id.toString());
		while (this.token == 46) {
			this.scan();
			if (this.token == 33 && flag) {
				this.scan();
				stringbuffer.append(".*");
				break;
			}
			stringbuffer.append('.');
			if (this.token == 60) {
				stringbuffer.append(this.scanner.idValue);
			}
			this.expect(60);
		}
		identifiertoken.id = Identifier.lookup(stringbuffer.toString());
		return identifiertoken;
	}

	private Type parseType() throws SyntaxError, IOException {
		final Type type;
		switch (this.token) {
		case 60: // '<'
			type = Type.tClass(this.parseName(false).id);
			break;

		case 77: // 'M'
			this.scan();
			type = Type.tVoid;
			break;

		case 78: // 'N'
			this.scan();
			type = Type.tBoolean;
			break;

		case 70: // 'F'
			this.scan();
			type = Type.tByte;
			break;

		case 71: // 'G'
			this.scan();
			type = Type.tChar;
			break;

		case 72: // 'H'
			this.scan();
			type = Type.tShort;
			break;

		case 73: // 'I'
			this.scan();
			type = Type.tInt;
			break;

		case 75: // 'K'
			this.scan();
			type = Type.tFloat;
			break;

		case 74: // 'J'
			this.scan();
			type = Type.tLong;
			break;

		case 76: // 'L'
			this.scan();
			type = Type.tDouble;
			break;

		case 61: // '='
		case 62: // '>'
		case 63: // '?'
		case 64: // '@'
		case 65: // 'A'
		case 66: // 'B'
		case 67: // 'C'
		case 68: // 'D'
		case 69: // 'E'
		default:
			this.env.error(this.pos, "type.expected");
			throw new SyntaxError();
		}
		return this.parseArrayBrackets(type);
	}

	private Type parseArrayBrackets(Type type) throws SyntaxError, IOException {
		while (this.token == 142) {
			this.scan();
			if (this.token != 143) {
				this.env.error(this.pos, "array.dim.in.decl");
				this.parseExpression();
			}
			this.expect(143);
			type = Type.tArray(type);
		}
		return type;
	}

	private void addArgument(final int i, final Type type, final IdentifierToken identifiertoken) {
		identifiertoken.modifiers = i;
		if (this.aCount >= this.aTypes.length) {
			final Type atype[] = new Type[this.aCount * 2];
			System.arraycopy(this.aTypes, 0, atype, 0, this.aCount);
			this.aTypes = atype;
			final IdentifierToken aidentifiertoken[] = new IdentifierToken[this.aCount * 2];
			System.arraycopy(this.aNames, 0, aidentifiertoken, 0, this.aCount);
			this.aNames = aidentifiertoken;
		}
		this.aTypes[this.aCount] = type;
		this.aNames[this.aCount++] = identifiertoken;
	}

	private int parseModifiers(final int i) throws IOException {
		int j = 0;
		do {
			if (this.token == 123) {
				this.env.error(this.pos, "not.supported", "const");
				this.scan();
			}
			int k = 0;
			switch (this.token) {
			case 120: // 'x'
				k = 2;
				break;

			case 121: // 'y'
				k = 1;
				break;

			case 122: // 'z'
				k = 4;
				break;

			case 124: // '|'
				k = 8;
				break;

			case 125: // '}'
				k = 128;
				break;

			case 128:
				k = 16;
				break;

			case 130:
				k = 1024;
				break;

			case 127: // '\177'
				k = 256;
				break;

			case 129:
				k = 64;
				break;

			case 126: // '~'
				k = 32;
				break;

			case 131:
				k = 0x200000;
				break;
			}
			if ((k & i) != 0) {
				if ((k & j) != 0) {
					this.env.error(this.pos, "repeated.modifier");
				}
				j |= k;
				this.scan();
			} else {
				return j;
			}
		} while (true);
	}

	private void parseField() throws SyntaxError, IOException {
		if (this.token == 135) {
			this.scan();
			return;
		}
		final String s = this.scanner.docComment;
		long l = this.pos;
		int i = this.parseModifiers(0x2005ff);
		if (i == (i & 8) && this.token == 138) {
			this.actions.defineField(l, this.curClass, s, i, Type.tMethod(Type.tVoid), new IdentifierToken(Constants.idClassInit), null, null, this.parseStatement());
			return;
		}
		if (this.token == 111 || this.token == 114) {
			this.parseNamedClass(i, 111, s);
			return;
		}
		l = this.pos;
		Type type = this.parseType();
		IdentifierToken identifiertoken = null;
		switch (this.token) {
		case 60: // '<'
			identifiertoken = this.scanner.getIdToken();
			l = this.scan();
			break;

		case 140:
			identifiertoken = new IdentifierToken(Constants.idInit);
			if ((i & 0x200000) != 0) {
				this.env.error(this.pos, "bad.constructor.modifier");
			}
			break;

		default:
			this.expect(60);
			break;
		}
		if (this.token == 140) {
			this.scan();
			this.aCount = 0;
			if (this.token != 141) {
				final int j = this.parseModifiers(16);
				Type type2 = this.parseType();
				final IdentifierToken identifiertoken1 = this.scanner.getIdToken();
				this.expect(60);
				type2 = this.parseArrayBrackets(type2);
				this.addArgument(j, type2, identifiertoken1);
				while (this.token == 0) {
					this.scan();
					final int k = this.parseModifiers(16);
					Type type3 = this.parseType();
					final IdentifierToken identifiertoken2 = this.scanner.getIdToken();
					this.expect(60);
					type3 = this.parseArrayBrackets(type3);
					this.addArgument(k, type3, identifiertoken2);
				}
			}
			this.expect(141);
			type = this.parseArrayBrackets(type);
			final Type atype[] = new Type[this.aCount];
			System.arraycopy(this.aTypes, 0, atype, 0, this.aCount);
			final IdentifierToken aidentifiertoken[] = new IdentifierToken[this.aCount];
			System.arraycopy(this.aNames, 0, aidentifiertoken, 0, this.aCount);
			type = Type.tMethod(type, atype);
			IdentifierToken aidentifiertoken1[] = null;
			if (this.token == 144) {
				this.scan();
				final Vector vector = new Vector();
				vector.addElement(this.parseName(false));
				while (this.token == 0) {
					this.scan();
					vector.addElement(this.parseName(false));
				}
				aidentifiertoken1 = new IdentifierToken[vector.size()];
				vector.copyInto(aidentifiertoken1);
			}
			switch (this.token) {
			case 138:
				final int i1 = this.FPstate;
				if ((i & 0x200000) != 0) {
					this.FPstate = 0x200000;
				} else {
					i |= this.FPstate & 0x200000;
				}
				this.actions.defineField(l, this.curClass, s, i, type, identifiertoken, aidentifiertoken, aidentifiertoken1, this.parseStatement());
				this.FPstate = i1;
				break;

			case 135:
				this.scan();
				this.actions.defineField(l, this.curClass, s, i, type, identifiertoken, aidentifiertoken, aidentifiertoken1, null);
				break;

			default:
				if ((i & 0x500) == 0) {
					this.expect(138);
				} else {
					this.expect(135);
				}
				break;
			}
			return;
		}
		do {
			final long l1 = this.pos;
			final Type type1 = this.parseArrayBrackets(type);
			Expression expression = null;
			if (this.token == 1) {
				this.scan();
				expression = this.parseExpression();
			}
			this.actions.defineField(l1, this.curClass, s, i, type1, identifiertoken, null, null, expression);
			if (this.token != 0) {
				this.expect(135);
				return;
			}
			this.scan();
			identifiertoken = this.scanner.getIdToken();
			this.expect(60);
		} while (true);
	}

	private void recoverField(final ClassDefinition classdefinition) throws SyntaxError, IOException {
		do {
			switch (this.token) {
			case -1:
			case 70: // 'F'
			case 71: // 'G'
			case 72: // 'H'
			case 73: // 'I'
			case 74: // 'J'
			case 75: // 'K'
			case 76: // 'L'
			case 77: // 'M'
			case 78: // 'N'
			case 120: // 'x'
			case 121: // 'y'
			case 124: // '|'
			case 125: // '}'
			case 126: // '~'
			case 128:
				return;

			case 138:
				this.match(138, 139);
				this.scan();
				break;

			case 140:
				this.match(140, 141);
				this.scan();
				break;

			case 142:
				this.match(142, 143);
				this.scan();
				break;

			case 110: // 'n'
			case 111: // 'o'
			case 114: // 'r'
			case 115: // 's'
			case 139:
				this.actions.endClass(this.pos, classdefinition);
				throw new SyntaxError();

			default:
				this.scan();
				break;
			}
		} while (true);
	}

	private void parseClass() throws SyntaxError, IOException {
		final String s = this.scanner.docComment;
		final int i = this.parseModifiers(0x20061f);
		this.parseNamedClass(i, 115, s);
	}

	private Statement parseLocalClass(final int i) throws SyntaxError, IOException {
		final long l = this.pos;
		final ClassDefinition classdefinition = this.parseNamedClass(0x20000 | i, 105, null);
		final Statement astatement[] = { new VarDeclarationStatement(l, new LocalMember(classdefinition), null) };
		final TypeExpression typeexpression = new TypeExpression(l, classdefinition.getType());
		return new DeclarationStatement(l, 0, typeexpression, astatement);
	}

	private ClassDefinition parseNamedClass(int i, final int j, final String s) throws SyntaxError, IOException {
		switch (this.token) {
		case 114: // 'r'
			this.scan();
			i |= 0x200;
			break;

		case 111: // 'o'
			this.scan();
			break;

		default:
			this.env.error(this.pos, "class.expected");
			break;
		}
		final int k = this.FPstate;
		if ((i & 0x200000) != 0) {
			this.FPstate = 0x200000;
		} else {
			i |= this.FPstate & 0x200000;
		}
		final IdentifierToken identifiertoken = this.scanner.getIdToken();
		final long l = this.pos;
		this.expect(60);
		final Vector vector = new Vector();
		final Vector vector1 = new Vector();
		this.parseInheritance(vector, vector1);
		final ClassDefinition classdefinition = this.parseClassBody(identifiertoken, i, j, s, vector, vector1, l);
		this.FPstate = k;
		return classdefinition;
	}

	private void parseInheritance(final Vector vector, final Vector vector1) throws SyntaxError, IOException {
		if (this.token == 112) {
			this.scan();
			vector.addElement(this.parseName(false));
			while (this.token == 0) {
				this.scan();
				vector.addElement(this.parseName(false));
			}
		}
		if (this.token == 113) {
			this.scan();
			vector1.addElement(this.parseName(false));
			while (this.token == 0) {
				this.scan();
				vector1.addElement(this.parseName(false));
			}
		}
	}

	private ClassDefinition parseClassBody(final IdentifierToken identifiertoken, final int i, final int j, final String s, final Vector vector, Vector vector1, final long l) throws SyntaxError, IOException {
		IdentifierToken identifiertoken1 = null;
		if ((i & 0x200) != 0) {
			if (!vector1.isEmpty()) {
				this.env.error(((IdentifierToken) vector1.elementAt(0)).getWhere(), "intf.impl.intf");
			}
			vector1 = vector;
		} else if (!vector.isEmpty()) {
			if (vector.size() > 1) {
				this.env.error(((IdentifierToken) vector.elementAt(1)).getWhere(), "multiple.inherit");
			}
			identifiertoken1 = (IdentifierToken) vector.elementAt(0);
		}
		final ClassDefinition classdefinition = this.curClass;
		final IdentifierToken aidentifiertoken[] = new IdentifierToken[vector1.size()];
		vector1.copyInto(aidentifiertoken);
		final ClassDefinition classdefinition1 = this.actions.beginClass(l, s, i, identifiertoken, identifiertoken1, aidentifiertoken);
		this.expect(138);
		while (this.token != -1 && this.token != 139) {
			try {
				this.curClass = classdefinition1;
				this.parseField();
			} catch (final SyntaxError ignored) {
				this.recoverField(classdefinition1);
			} finally {
				this.curClass = classdefinition;
			}
		}
		this.expect(139);
		this.actions.endClass(this.scanner.prevPos, classdefinition1);
		return classdefinition1;
	}

	private void recoverFile() throws IOException {
		do {
			switch (this.token) {
			case 111: // 'o'
			case 114: // 'r'
				return;

			case 138:
				this.match(138, 139);
				this.scan();
				break;

			case 140:
				this.match(140, 141);
				this.scan();
				break;

			case 142:
				this.match(142, 143);
				this.scan();
				break;

			case -1:
				return;

			default:
				this.scan();
				break;
			}
		} while (true);
	}

	public void parseFile() {
		try {
			try {
				if (this.token == 115) {
					final long l = this.scan();
					final IdentifierToken identifiertoken = this.parseName(false);
					this.expect(135);
					this.actions.packageDeclaration(l, identifiertoken);
				}
			} catch (final SyntaxError ignored) {
				this.recoverFile();
			}
			while (this.token == 110) {
				try {
					final long l1 = this.scan();
					final IdentifierToken identifiertoken1 = this.parseName(true);
					this.expect(135);
					if (identifiertoken1.id.getName().equals(Constants.idStar)) {
						identifiertoken1.id = identifiertoken1.id.getQualifier();
						this.actions.importPackage(l1, identifiertoken1);
					} else {
						this.actions.importClass(l1, identifiertoken1);
					}
				} catch (final SyntaxError ignored) {
					this.recoverFile();
				}
			}
			while (this.token != -1) {
				try {
					switch (this.token) {
					case 111: // 'o'
					case 114: // 'r'
					case 120: // 'x'
					case 121: // 'y'
					case 128:
					case 130:
					case 131:
						this.parseClass();
						break;

					case 135:
						this.scan();
						break;

					case -1:
						return;

					default:
						this.env.error(this.pos, "toplevel.expected");
						throw new SyntaxError();
					}
				} catch (final SyntaxError ignored) {
					this.recoverFile();
				}
			}
		} catch (final IOException ignored) {
			this.env.error(this.pos, "io.exception", this.env.getSource());
		}
	}

	long scan() throws IOException {
		if (this.scanner != this && this.scanner != null) {
			final long l = this.scanner.scan();
			this.token = this.scanner.token;
			this.pos = this.scanner.pos;
			return l;
		}
		return super.scan();
	}

	void match(final int i, final int j) throws IOException {
		if (this.scanner != this) {
			this.scanner.match(i, j);
			this.token = this.scanner.token;
			this.pos = this.scanner.pos;
			return;
		}
		super.match(i, j);
	}

	private final ParserActions actions;
	private Node args[];
	private int argIndex;
	private int aCount;
	private Type aTypes[];
	private IdentifierToken aNames[];
	private ClassDefinition curClass;
	private int FPstate;
	private final Scanner scanner;
}
