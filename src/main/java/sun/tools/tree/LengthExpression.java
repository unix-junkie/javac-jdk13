package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class LengthExpression extends UnaryExpression {

	LengthExpression(final long l, final Expression expression) {
		super(148, l, Type.tInt, expression);
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		vset = this.right.checkValue(environment, context, vset, hashtable);
		if (!this.right.type.isType(9)) {
			environment.error(this.where, "invalid.length", this.right.type);
		}
		return vset;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.right.codeValue(environment, context, assembler);
		assembler.add(this.where, 190);
	}
}
