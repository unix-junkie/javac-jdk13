package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.Instruction;
import sun.tools.asm.Label;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class IfStatement extends Statement {

	public IfStatement(final long l, final Expression expression, final Statement statement, final Statement statement1) {
		super(90, l);
		this.cond = expression;
		this.ifTrue = statement;
		this.ifFalse = statement1;
	}

	Vset check(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		this.checkLabel(environment, context);
		final CheckContext checkcontext = new CheckContext(context, this);
		final ConditionVars conditionvars = this.cond.checkCondition(environment, checkcontext, this.reach(environment, vset), hashtable);
		this.cond = this.convert(environment, checkcontext, Type.tBoolean, this.cond);
		Vset vset1 = conditionvars.vsTrue.clearDeadEnd();
		Vset vset2 = conditionvars.vsFalse.clearDeadEnd();
		vset1 = this.ifTrue.check(environment, checkcontext, vset1, hashtable);
		if (this.ifFalse != null) {
			vset2 = this.ifFalse.check(environment, checkcontext, vset2, hashtable);
		}
		vset = vset1.join(vset2.join(checkcontext.vsBreak));
		return context.removeAdditionalVars(vset);
	}

	public Statement inline(final Environment environment, Context context) {
		context = new Context(context, this);
		this.cond = this.cond.inlineValue(environment, context);
		if (this.ifTrue != null) {
			this.ifTrue = this.ifTrue.inline(environment, context);
		}
		if (this.ifFalse != null) {
			this.ifFalse = this.ifFalse.inline(environment, context);
		}
		if (this.cond.equals(true)) {
			return this.eliminate(environment, this.ifTrue);
		}
		if (this.cond.equals(false)) {
			return this.eliminate(environment, this.ifFalse);
		}
		if (this.ifTrue == null && this.ifFalse == null) {
			return this.eliminate(environment, new ExpressionStatement(this.where, this.cond).inline(environment, context));
		}
		if (this.ifTrue == null) {
			this.cond = new NotExpression(((Node) this.cond).where, this.cond).inlineValue(environment, context);
			return this.eliminate(environment, new IfStatement(this.where, this.cond, this.ifFalse, null));
		}
		return this;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final IfStatement ifstatement = (IfStatement) this.clone();
		ifstatement.cond = this.cond.copyInline(context);
		if (this.ifTrue != null) {
			ifstatement.ifTrue = this.ifTrue.copyInline(context, flag);
		}
		if (this.ifFalse != null) {
			ifstatement.ifFalse = this.ifFalse.copyInline(context, flag);
		}
		return ifstatement;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		int j = 1 + this.cond.costInline(i, environment, context);
		if (this.ifTrue != null) {
			j += this.ifTrue.costInline(i, environment, context);
		}
		if (this.ifFalse != null) {
			j += this.ifFalse.costInline(i, environment, context);
		}
		return j;
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		final CodeContext codecontext = new CodeContext(context, this);
		final Label label = new Label();
		this.cond.codeBranch(environment, codecontext, assembler, label, false);
		this.ifTrue.code(environment, codecontext, assembler);
		if (this.ifFalse != null) {
			final Instruction label1 = new Label();
			assembler.add(true, this.where, 167, label1);
			assembler.add(label);
			this.ifFalse.code(environment, codecontext, assembler);
			assembler.add(label1);
		} else {
			assembler.add(label);
		}
		assembler.add(codecontext.breakLabel);
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("if ");
		this.cond.print(printstream);
		printstream.print(" ");
		this.ifTrue.print(printstream, i);
		if (this.ifFalse != null) {
			printstream.print(" else ");
			this.ifFalse.print(printstream, i);
		}
	}

	private Expression cond;
	private Statement ifTrue;
	private Statement ifFalse;
}
