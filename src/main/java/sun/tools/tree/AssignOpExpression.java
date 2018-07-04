package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public abstract class AssignOpExpression extends BinaryAssignExpression {

	AssignOpExpression(final int i, final long l, final Expression expression, final Expression expression1) {
		super(i, l, expression, expression1);
		this.updater = null;
	}

	final void selectType(final Environment environment, final Context context, final int i) {
		Type type = null;
		switch (this.op) {
		case 5: // '\005'
			if (this.left.type == Type.tString) {
				if (this.right.type == Type.tVoid) {
					environment.error(this.where, "incompatible.type", Constants.opNames[this.op], Type.tVoid, Type.tString);
					super.type = Type.tError;
				} else {
					super.type = this.itype = Type.tString;
				}
				return;
			}
			// fall through

		case 2: // '\002'
		case 3: // '\003'
		case 4: // '\004'
		case 6: // '\006'
			if ((i & 0x80) != 0) {
				this.itype = Type.tDouble;
			} else if ((i & 0x40) != 0) {
				this.itype = Type.tFloat;
			} else if ((i & 0x20) != 0) {
				this.itype = Type.tLong;
			} else {
				this.itype = Type.tInt;
			}
			break;

		case 10: // '\n'
		case 11: // '\013'
		case 12: // '\f'
			if ((i & 1) != 0) {
				this.itype = Type.tBoolean;
			} else if ((i & 0x20) != 0) {
				this.itype = Type.tLong;
			} else {
				this.itype = Type.tInt;
			}
			break;

		case 7: // '\007'
		case 8: // '\b'
		case 9: // '\t'
			type = Type.tInt;
			if (this.right.type.inMask(62)) {
				this.right = new ConvertExpression(this.where, Type.tInt, this.right);
			}
			this.itype = this.left.type == Type.tLong ? Type.tLong : Type.tInt;
			break;

		default:
			throw new CompilerError("Bad assignOp type: " + this.op);
		}
		if (type == null) {
			type = this.itype;
		}
		this.right = this.convert(environment, context, type, this.right);
		super.type = this.left.type;
	}

	private int getIncrement() {
		if (((Node) this.left).op == 60 && this.type.isType(4) && ((Node) this.right).op == 65 && (this.op == 5 || this.op == 6) && ((IdentifierExpression) this.left).field.isLocal()) {
			int i = ((IntegerExpression) (IntExpression) this.right).value;
			if (this.op == 6) {
				i = -i;
			}
			if (i == (short) i) {
				return i;
			}
		}
		return 0x7fffffff;
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		vset = this.left.checkAssignOp(environment, context, vset, hashtable, this);
		vset = this.right.checkValue(environment, context, vset, hashtable);
		final int i = this.left.type.getTypeMask() | this.right.type.getTypeMask();
		if ((i & 0x2000) != 0) {
			return vset;
		}
		this.selectType(environment, context, i);
		if (!this.type.isType(13)) {
			this.convert(environment, context, this.itype, this.left);
		}
		this.updater = this.left.getUpdater(environment, context);
		return vset;
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		this.left = this.left.inlineValue(environment, context);
		this.right = this.right.inlineValue(environment, context);
		if (this.updater != null) {
			this.updater = this.updater.inline(environment, context);
		}
		return this;
	}

	public Expression copyInline(final Context context) {
		final AssignOpExpression assignopexpression = (AssignOpExpression) this.clone();
		assignopexpression.left = this.left.copyInline(context);
		assignopexpression.right = this.right.copyInline(context);
		if (this.updater != null) {
			assignopexpression.updater = this.updater.copyInline(context);
		}
		return assignopexpression;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		if (this.updater == null) {
			return this.getIncrement() == 0x7fffffff ? this.right.costInline(i, environment, context) + this.left.costInline(i, environment, context) + 4 : 3;
		}
		return this.right.costInline(i, environment, context) + this.updater.costInline(i, environment, context, true) + 1;
	}

	void code(final Environment environment, final Context context, final Assembler assembler, final boolean flag) {
		final int i = this.getIncrement();
		if (i != 0x7fffffff && this.updater == null) {
			final int j = ((LocalMember) ((IdentifierExpression) this.left).field).number;
			final int ai[] = { j, i };
			assembler.add(this.where, 132, ai);
			if (flag) {
				this.left.codeValue(environment, context, assembler);
			}
			return;
		}
		if (this.updater == null) {
			final int k = this.left.codeLValue(environment, context, assembler);
			this.codeDup(environment, context, assembler, k, 0);
			this.left.codeLoad(environment, context, assembler);
			this.codeConversion(environment, context, assembler, this.left.type, this.itype);
			this.right.codeValue(environment, context, assembler);
			this.codeOperation(environment, context, assembler);
			this.codeConversion(environment, context, assembler, this.itype, this.type);
			if (flag) {
				this.codeDup(environment, context, assembler, this.type.stackSize(), k);
			}
			this.left.codeStore(environment, context, assembler);
		} else {
			this.updater.startUpdate(environment, context, assembler, false);
			this.codeConversion(environment, context, assembler, this.left.type, this.itype);
			this.right.codeValue(environment, context, assembler);
			this.codeOperation(environment, context, assembler);
			this.codeConversion(environment, context, assembler, this.itype, this.type);
			this.updater.finishUpdate(environment, context, assembler, flag);
		}
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.code(environment, context, assembler, true);
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		this.code(environment, context, assembler, false);
	}

	public void print(final PrintStream printstream) {
		printstream.print('(' + Constants.opNames[this.op] + ' ');
		this.left.print(printstream);
		printstream.print(" ");
		this.right.print(printstream);
		printstream.print(")");
	}

	Type itype;
	static final int NOINC = 0x7fffffff;
	FieldUpdater updater;
}
