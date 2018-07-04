package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.Label;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class BooleanExpression extends ConstantExpression {

	public BooleanExpression(final long l, final boolean flag) {
		super(61, l, Type.tBoolean);
		this.value = flag;
	}

	public Object getValue() {
		return new Integer(this.value ? 1 : 0);
	}

	public boolean equals(final boolean flag) {
		return this.value == flag;
	}

	public boolean equalsDefault() {
		return !this.value;
	}

	public void checkCondition(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final ConditionVars conditionvars) {
		if (this.value) {
			conditionvars.vsFalse = Vset.DEAD_END;
			conditionvars.vsTrue = vset;
		} else {
			conditionvars.vsFalse = vset;
			conditionvars.vsTrue = Vset.DEAD_END;
		}
	}

	void codeBranch(final Environment environment, final Context context, final Assembler assembler, final Label label, final boolean flag) {
		if (this.value == flag) {
			assembler.add(this.where, 167, label);
		}
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 18, new Integer(this.value ? 1 : 0));
	}

	public void print(final PrintStream printstream) {
		printstream.print(this.value ? "true" : "false");
	}

	final boolean value;
}
