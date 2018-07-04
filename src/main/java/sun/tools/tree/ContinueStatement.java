package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;

public final class ContinueStatement extends Statement {

	public ContinueStatement(final long l, final Identifier identifier) {
		super(99, l);
		this.lbl = identifier;
	}

	Vset check(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		this.checkLabel(environment, context);
		this.reach(environment, vset);
		final CheckContext checkcontext = (CheckContext) new CheckContext(context, this).getContinueContext(this.lbl);
		if (checkcontext != null) {
			switch (((Context) checkcontext).node.op) {
			case 92: // '\\'
			case 93: // ']'
			case 94: // '^'
				if (((Context) checkcontext).frameNumber != context.frameNumber) {
					environment.error(this.where, "branch.to.uplevel", this.lbl);
				}
				checkcontext.vsContinue = checkcontext.vsContinue.join(vset);
				break;

			default:
				environment.error(this.where, "invalid.continue");
				break;
			}
		} else if (this.lbl != null) {
			environment.error(this.where, "label.not.found", this.lbl);
		} else {
			environment.error(this.where, "invalid.continue");
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
		final CodeContext codecontext = (CodeContext) context.getContinueContext(this.lbl);
		this.codeFinally(environment, context, assembler, codecontext, null);
		assembler.add(this.where, 167, codecontext.contLabel);
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("continue");
		if (this.lbl != null) {
			printstream.print(" " + this.lbl);
		}
		printstream.print(";");
	}

	private final Identifier lbl;
}
