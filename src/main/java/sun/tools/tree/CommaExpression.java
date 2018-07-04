package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class CommaExpression extends BinaryExpression {

	public CommaExpression(final long l, final Expression expression, final Expression expression1) {
		super(0, l, expression1 == null ? Type.tVoid : expression1.type, expression, expression1);
	}

	public Vset check(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		vset = this.left.check(environment, context, vset, hashtable);
		vset = this.right.check(environment, context, vset, hashtable);
		return vset;
	}

	void selectType(final Environment environment, final Context context, final int i) {
		this.type = this.right.type;
	}

	Expression simplify() {
		if (this.left == null) {
			return this.right;
		}
		return this.right == null ? this.left : this;
	}

	public Expression inline(final Environment environment, final Context context) {
		if (this.left != null) {
			this.left = this.left.inline(environment, context);
		}
		if (this.right != null) {
			this.right = this.right.inline(environment, context);
		}
		return this.simplify();
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		if (this.left != null) {
			this.left = this.left.inline(environment, context);
		}
		if (this.right != null) {
			this.right = this.right.inlineValue(environment, context);
		}
		return this.simplify();
	}

	int codeLValue(final Environment environment, final Context context, final Assembler assembler) {
		if (this.right == null) {
			return super.codeLValue(environment, context, assembler);
		}
		if (this.left != null) {
			this.left.code(environment, context, assembler);
		}
		return this.right.codeLValue(environment, context, assembler);
	}

	void codeLoad(final Environment environment, final Context context, final Assembler assembler) {
		if (this.right == null) {
			super.codeLoad(environment, context, assembler);
		} else {
			this.right.codeLoad(environment, context, assembler);
		}
	}

	void codeStore(final Environment environment, final Context context, final Assembler assembler) {
		if (this.right == null) {
			super.codeStore(environment, context, assembler);
		} else {
			this.right.codeStore(environment, context, assembler);
		}
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		if (this.left != null) {
			this.left.code(environment, context, assembler);
		}
		this.right.codeValue(environment, context, assembler);
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		if (this.left != null) {
			this.left.code(environment, context, assembler);
		}
		if (this.right != null) {
			this.right.code(environment, context, assembler);
		}
	}
}
