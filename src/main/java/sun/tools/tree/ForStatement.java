package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.Instruction;
import sun.tools.asm.Label;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class ForStatement extends Statement {

	public ForStatement(final long l, final Statement statement, final Expression expression, final Expression expression1, final Statement statement1) {
		super(92, l);
		this.init = statement;
		this.cond = expression;
		this.inc = expression1;
		this.body = statement1;
	}

	Vset check(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		this.checkLabel(environment, context);
		vset = this.reach(environment, vset);
		final Context context1 = new Context(context, this);
		if (this.init != null) {
			vset = this.init.checkBlockStatement(environment, context1, vset, hashtable);
		}
		final CheckContext checkcontext = new CheckContext(context1, this);
		final Vset vset1 = vset.copy();
		final ConditionVars conditionvars;
		if (this.cond != null) {
			conditionvars = this.cond.checkCondition(environment, checkcontext, vset, hashtable);
			this.cond = this.convert(environment, checkcontext, Type.tBoolean, this.cond);
		} else {
			conditionvars = new ConditionVars();
			conditionvars.vsFalse = Vset.DEAD_END;
			conditionvars.vsTrue = vset;
		}
		vset = this.body.check(environment, checkcontext, conditionvars.vsTrue, hashtable);
		vset = vset.join(checkcontext.vsContinue);
		if (this.inc != null) {
			vset = this.inc.check(environment, checkcontext, vset, hashtable);
		}
		context1.checkBackBranch(environment, this, vset1, vset);
		vset = checkcontext.vsBreak.join(conditionvars.vsFalse);
		return context.removeAdditionalVars(vset);
	}

	public Statement inline(final Environment environment, Context context) {
		context = new Context(context, this);
		if (this.init != null) {
			final Statement astatement[] = { this.init, this };
			this.init = null;
			return new CompoundStatement(this.where, astatement).inline(environment, context);
		}
		if (this.cond != null) {
			this.cond = this.cond.inlineValue(environment, context);
		}
		if (this.body != null) {
			this.body = this.body.inline(environment, context);
		}
		if (this.inc != null) {
			this.inc = this.inc.inline(environment, context);
		}
		return this;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final ForStatement forstatement = (ForStatement) this.clone();
		if (this.init != null) {
			forstatement.init = this.init.copyInline(context, flag);
		}
		if (this.cond != null) {
			forstatement.cond = this.cond.copyInline(context);
		}
		if (this.body != null) {
			forstatement.body = this.body.copyInline(context, flag);
		}
		if (this.inc != null) {
			forstatement.inc = this.inc.copyInline(context);
		}
		return forstatement;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		int j = 2;
		if (this.init != null) {
			j += this.init.costInline(i, environment, context);
		}
		if (this.cond != null) {
			j += this.cond.costInline(i, environment, context);
		}
		if (this.body != null) {
			j += this.body.costInline(i, environment, context);
		}
		if (this.inc != null) {
			j += this.inc.costInline(i, environment, context);
		}
		return j;
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		final CodeContext codecontext = new CodeContext(context, this);
		if (this.init != null) {
			this.init.code(environment, codecontext, assembler);
		}
		final Label label = new Label();
		final Instruction label1 = new Label();
		assembler.add(this.where, 167, label1);
		assembler.add(label);
		if (this.body != null) {
			this.body.code(environment, codecontext, assembler);
		}
		assembler.add(codecontext.contLabel);
		if (this.inc != null) {
			this.inc.code(environment, codecontext, assembler);
		}
		assembler.add(label1);
		if (this.cond != null) {
			this.cond.codeBranch(environment, codecontext, assembler, label, true);
		} else {
			assembler.add(this.where, 167, label);
		}
		assembler.add(codecontext.breakLabel);
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("for (");
		if (this.init != null) {
			this.init.print(printstream, i);
			printstream.print(" ");
		} else {
			printstream.print("; ");
		}
		if (this.cond != null) {
			this.cond.print(printstream);
			printstream.print(" ");
		}
		printstream.print("; ");
		if (this.inc != null) {
			this.inc.print(printstream);
		}
		printstream.print(") ");
		if (this.body != null) {
			this.body.print(printstream, i);
		} else {
			printstream.print(";");
		}
	}

	private Statement init;
	private Expression cond;
	private Expression inc;
	private Statement body;
}
