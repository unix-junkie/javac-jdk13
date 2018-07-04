package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public class IncDecExpression extends UnaryExpression {

	IncDecExpression(final int i, final long l, final Expression expression) {
		super(i, l, expression.type, expression);
		this.updater = null;
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		vset = this.right.checkAssignOp(environment, context, vset, hashtable, this);
		if (this.right.type.inMask(254)) {
			this.type = this.right.type;
		} else {
			if (!this.right.type.isType(13)) {
				environment.error(this.where, "invalid.arg.type", this.right.type, Constants.opNames[this.op]);
			}
			this.type = Type.tError;
		}
		this.updater = this.right.getUpdater(environment, context);
		return vset;
	}

	public Vset check(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		return this.checkValue(environment, context, vset, hashtable);
	}

	public Expression inline(final Environment environment, final Context context) {
		return this.inlineValue(environment, context);
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		this.right = this.right.inlineValue(environment, context);
		if (this.updater != null) {
			this.updater = this.updater.inline(environment, context);
		}
		return this;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		if (this.updater == null) {
			return ((Node) this.right).op == 60 && this.type.isType(4) && ((IdentifierExpression) this.right).field.isLocal() ? 3 : this.right.costInline(i, environment, context) + 4;
		}
		return this.updater.costInline(i, environment, context, true) + 1;
	}

	private void codeIncDecOp(final Assembler assembler, final boolean flag) {
		switch (this.type.getTypeCode()) {
		case 1: // '\001'
			assembler.add(this.where, 18, new Integer(1));
			assembler.add(this.where, flag ? 96 : 100);
			assembler.add(this.where, 145);
			break;

		case 3: // '\003'
			assembler.add(this.where, 18, new Integer(1));
			assembler.add(this.where, flag ? 96 : 100);
			assembler.add(this.where, 147);
			break;

		case 2: // '\002'
			assembler.add(this.where, 18, new Integer(1));
			assembler.add(this.where, flag ? 96 : 100);
			assembler.add(this.where, 146);
			break;

		case 4: // '\004'
			assembler.add(this.where, 18, new Integer(1));
			assembler.add(this.where, flag ? 96 : 100);
			break;

		case 5: // '\005'
			assembler.add(this.where, 20, new Long(1L));
			assembler.add(this.where, flag ? 97 : 101);
			break;

		case 6: // '\006'
			assembler.add(this.where, 18, new Float(1.0F));
			assembler.add(this.where, flag ? 98 : 102);
			break;

		case 7: // '\007'
			assembler.add(this.where, 20, new Double(1.0D));
			assembler.add(this.where, flag ? 99 : 103);
			break;

		default:
			throw new CompilerError("invalid type");
		}
	}

	void codeIncDec(final Environment environment, final Context context, final Assembler assembler, final boolean flag, final boolean flag1, final boolean flag2) {
		if (((Node) this.right).op == 60 && this.type.isType(4) && ((IdentifierExpression) this.right).field.isLocal() && this.updater == null) {
			if (flag2 && !flag1) {
				this.right.codeLoad(environment, context, assembler);
			}
			final int i = ((LocalMember) ((IdentifierExpression) this.right).field).number;
			final int ai[] = { i, flag ? 1 : -1 };
			assembler.add(this.where, 132, ai);
			if (flag2 && flag1) {
				this.right.codeLoad(environment, context, assembler);
			}
			return;
		}
		if (this.updater == null) {
			final int j = this.right.codeLValue(environment, context, assembler);
			this.codeDup(environment, context, assembler, j, 0);
			this.right.codeLoad(environment, context, assembler);
			if (flag2 && !flag1) {
				this.codeDup(environment, context, assembler, this.type.stackSize(), j);
			}
			this.codeIncDecOp(assembler, flag);
			if (flag2 && flag1) {
				this.codeDup(environment, context, assembler, this.type.stackSize(), j);
			}
			this.right.codeStore(environment, context, assembler);
		} else {
			this.updater.startUpdate(environment, context, assembler, flag2 && !flag1);
			this.codeIncDecOp(assembler, flag);
			this.updater.finishUpdate(environment, context, assembler, flag2 && flag1);
		}
	}

	private FieldUpdater updater;
}
