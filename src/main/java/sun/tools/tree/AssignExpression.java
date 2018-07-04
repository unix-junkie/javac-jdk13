package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;

public final class AssignExpression extends BinaryAssignExpression {

	public AssignExpression(final long l, final Expression expression, final Expression expression1) {
		super(1, l, expression, expression1);
		this.updater = null;
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		if (this.left instanceof IdentifierExpression) {
			vset = this.right.checkValue(environment, context, vset, hashtable);
			vset = this.left.checkLHS(environment, context, vset, hashtable);
		} else {
			vset = this.left.checkLHS(environment, context, vset, hashtable);
			vset = this.right.checkValue(environment, context, vset, hashtable);
		}
		this.type = this.left.type;
		this.right = this.convert(environment, context, this.type, this.right);
		this.updater = this.left.getAssigner(environment, context);
		return vset;
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		if (this.implementation != null) {
			return this.implementation.inlineValue(environment, context);
		}
		this.left = this.left.inlineLHS(environment, context);
		this.right = this.right.inlineValue(environment, context);
		if (this.updater != null) {
			this.updater = this.updater.inline(environment, context);
		}
		return this;
	}

	public Expression copyInline(final Context context) {
		if (this.implementation != null) {
			return this.implementation.copyInline(context);
		}
		final AssignExpression assignexpression = (AssignExpression) this.clone();
		assignexpression.left = this.left.copyInline(context);
		assignexpression.right = this.right.copyInline(context);
		if (this.updater != null) {
			assignexpression.updater = this.updater.copyInline(context);
		}
		return assignexpression;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return this.updater == null ? this.right.costInline(i, environment, context) + this.left.costInline(i, environment, context) + 2 : this.right.costInline(i, environment, context) + this.updater.costInline(i, environment, context, false);
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		if (this.updater == null) {
			final int i = this.left.codeLValue(environment, context, assembler);
			this.right.codeValue(environment, context, assembler);
			this.codeDup(environment, context, assembler, this.right.type.stackSize(), i);
			this.left.codeStore(environment, context, assembler);
		} else {
			this.updater.startAssign(environment, context, assembler);
			this.right.codeValue(environment, context, assembler);
			this.updater.finishAssign(environment, context, assembler, true);
		}
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		if (this.updater == null) {
			this.left.codeLValue(environment, context, assembler);
			this.right.codeValue(environment, context, assembler);
			this.left.codeStore(environment, context, assembler);
		} else {
			this.updater.startAssign(environment, context, assembler);
			this.right.codeValue(environment, context, assembler);
			this.updater.finishAssign(environment, context, assembler, false);
		}
	}

	private FieldUpdater updater;
}
