package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.Label;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class DoStatement extends Statement {

	public DoStatement(final long l, final Statement statement, final Expression expression) {
		super(94, l);
		this.body = statement;
		this.cond = expression;
	}

	Vset check(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		this.checkLabel(environment, context);
		final CheckContext checkcontext = new CheckContext(context, this);
		final Vset vset1 = vset.copy();
		vset = this.body.check(environment, checkcontext, this.reach(environment, vset), hashtable);
		vset = vset.join(checkcontext.vsContinue);
		final ConditionVars conditionvars = this.cond.checkCondition(environment, checkcontext, vset, hashtable);
		this.cond = this.convert(environment, checkcontext, Type.tBoolean, this.cond);
		context.checkBackBranch(environment, this, vset1, conditionvars.vsTrue);
		vset = checkcontext.vsBreak.join(conditionvars.vsFalse);
		return context.removeAdditionalVars(vset);
	}

	public Statement inline(final Environment environment, Context context) {
		context = new Context(context, this);
		if (this.body != null) {
			this.body = this.body.inline(environment, context);
		}
		this.cond = this.cond.inlineValue(environment, context);
		return this;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final DoStatement dostatement = (DoStatement) this.clone();
		dostatement.cond = this.cond.copyInline(context);
		if (this.body != null) {
			dostatement.body = this.body.copyInline(context, flag);
		}
		return dostatement;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return 1 + this.cond.costInline(i, environment, context) + (this.body == null ? 0 : this.body.costInline(i, environment, context));
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		final Label label = new Label();
		assembler.add(label);
		final CodeContext codecontext = new CodeContext(context, this);
		if (this.body != null) {
			this.body.code(environment, codecontext, assembler);
		}
		assembler.add(codecontext.contLabel);
		this.cond.codeBranch(environment, codecontext, assembler, label, true);
		assembler.add(codecontext.breakLabel);
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("do ");
		this.body.print(printstream, i);
		printstream.print(" while ");
		this.cond.print(printstream);
		printstream.print(";");
	}

	private Statement body;
	private Expression cond;
}
