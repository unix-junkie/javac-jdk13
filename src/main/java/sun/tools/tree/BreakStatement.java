package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;

public final class BreakStatement extends Statement {

	public BreakStatement(final long l, final Identifier identifier) {
		super(98, l);
		this.lbl = identifier;
	}

	Vset check(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		this.reach(environment, vset);
		this.checkLabel(environment, context);
		final CheckContext checkcontext = (CheckContext) new CheckContext(context, this).getBreakContext(this.lbl);
		if (checkcontext != null) {
			if (((Context) checkcontext).frameNumber != context.frameNumber) {
				environment.error(this.where, "branch.to.uplevel", this.lbl);
			}
			checkcontext.vsBreak = checkcontext.vsBreak.join(vset);
		} else if (this.lbl != null) {
			environment.error(this.where, "label.not.found", this.lbl);
		} else {
			environment.error(this.where, "invalid.break");
		}
		final CheckContext checkcontext1 = context.getTryExitContext();
		if (checkcontext1 != null) {
			checkcontext1.vsTryExit = checkcontext1.vsTryExit.join(vset);
		}
		return Statement.DEAD_END;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return 1;
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		final CodeContext codecontext = new CodeContext(context, this);
		final CodeContext codecontext1 = (CodeContext) codecontext.getBreakContext(this.lbl);
		this.codeFinally(environment, context, assembler, codecontext1, null);
		assembler.add(this.where, 167, codecontext1.breakLabel);
		assembler.add(codecontext.breakLabel);
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("break");
		if (this.lbl != null) {
			printstream.print(" " + this.lbl);
		}
		printstream.print(";");
	}

	private final Identifier lbl;
}
