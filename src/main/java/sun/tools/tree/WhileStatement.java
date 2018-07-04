package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.Label;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class WhileStatement extends Statement {

	public WhileStatement(final long l, final Expression expression, final Statement statement) {
		super(93, l);
		this.cond = expression;
		this.body = statement;
	}

	Vset check(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		this.checkLabel(environment, context);
		final CheckContext checkcontext = new CheckContext(context, this);
		final Vset vset1 = vset.copy();
		final ConditionVars conditionvars = this.cond.checkCondition(environment, checkcontext, this.reach(environment, vset), hashtable);
		this.cond = this.convert(environment, checkcontext, Type.tBoolean, this.cond);
		vset = this.body.check(environment, checkcontext, conditionvars.vsTrue, hashtable);
		vset = vset.join(checkcontext.vsContinue);
		context.checkBackBranch(environment, this, vset1, vset);
		vset = checkcontext.vsBreak.join(conditionvars.vsFalse);
		return context.removeAdditionalVars(vset);
	}

	public Statement inline(final Environment environment, Context context) {
		context = new Context(context, this);
		this.cond = this.cond.inlineValue(environment, context);
		if (this.body != null) {
			this.body = this.body.inline(environment, context);
		}
		return this;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return 1 + this.cond.costInline(i, environment, context) + (this.body == null ? 0 : this.body.costInline(i, environment, context));
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final WhileStatement whilestatement = (WhileStatement) this.clone();
		whilestatement.cond = this.cond.copyInline(context);
		if (this.body != null) {
			whilestatement.body = this.body.copyInline(context, flag);
		}
		return whilestatement;
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		final CodeContext codecontext = new CodeContext(context, this);
		assembler.add(this.where, 167, codecontext.contLabel);
		final Label label = new Label();
		assembler.add(label);
		if (this.body != null) {
			this.body.code(environment, codecontext, assembler);
		}
		assembler.add(codecontext.contLabel);
		this.cond.codeBranch(environment, codecontext, assembler, label, true);
		assembler.add(codecontext.breakLabel);
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("while ");
		this.cond.print(printstream);
		if (this.body != null) {
			printstream.print(" ");
			this.body.print(printstream, i);
		} else {
			printstream.print(";");
		}
	}

	private Expression cond;
	private Statement body;
}
