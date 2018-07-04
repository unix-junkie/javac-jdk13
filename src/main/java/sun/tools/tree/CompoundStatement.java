package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;

public final class CompoundStatement extends Statement {

	public CompoundStatement(final long l, final Statement astatement[]) {
		super(105, l);
		this.args = astatement;
		for (int i = 0; i < astatement.length; i++) {
			if (astatement[i] == null) {
				astatement[i] = new CompoundStatement(l, new Statement[0]);
			}
		}

	}

	public void insertStatement(final Statement statement) {
		final Statement astatement[] = new Statement[1 + this.args.length];
		astatement[0] = statement;
		System.arraycopy(this.args, 0, astatement, 1, this.args.length);

		this.args = astatement;
	}

	Vset check(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		this.checkLabel(environment, context);
		if (this.args.length > 0) {
			vset = this.reach(environment, vset);
			final CheckContext checkcontext = new CheckContext(context, this);
			final Environment environment1 = Context.newEnvironment(environment, checkcontext);
			for (int i = 0; i < this.args.length; i++) {
				vset = this.args[i].checkBlockStatement(environment1, checkcontext, vset, hashtable);
			}

			vset = vset.join(checkcontext.vsBreak);
		}
		return context.removeAdditionalVars(vset);
	}

	public Statement inline(final Environment environment, Context context) {
		context = new Context(context, this);
		boolean flag = false;
		int i = 0;
		for (int j = 0; j < this.args.length; j++) {
			Statement statement = this.args[j];
			if (statement != null) {
				if ((statement = statement.inline(environment, context)) != null) {
					if (((Node) statement).op == 105 && statement.labels == null) {
						i += ((CompoundStatement) statement).args.length;
					} else {
						i++;
					}
					flag = true;
				}
				this.args[j] = statement;
			}
		}

		switch (i) {
		case 0: // '\0'
			return null;

		case 1: // '\001'
			for (int k = this.args.length; k-- > 0;) {
				if (this.args[k] != null) {
					return this.eliminate(environment, this.args[k]);
				}
			}

			break;
		}
		if (flag || i != this.args.length) {
			final Statement astatement[] = new Statement[i];
			for (int l = this.args.length; l-- > 0;) {
				final Statement statement1 = this.args[l];
				if (statement1 != null) {
					if (((Node) statement1).op == 105 && statement1.labels == null) {
						final Statement astatement1[] = ((CompoundStatement) statement1).args;
						for (int i1 = astatement1.length; i1-- > 0;) {
							astatement[--i] = astatement1[i1];
						}

					} else {
						astatement[--i] = statement1;
					}
				}
			}

			this.args = astatement;
		}
		return this;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final CompoundStatement compoundstatement = (CompoundStatement) this.clone();
		compoundstatement.args = new Statement[this.args.length];
		for (int i = 0; i < this.args.length; i++) {
			compoundstatement.args[i] = this.args[i].copyInline(context, flag);
		}

		return compoundstatement;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		int j = 0;
		for (int k = 0; k < this.args.length && j < i; k++) {
			j += this.args[k].costInline(i, environment, context);
		}

		return j;
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		final CodeContext codecontext = new CodeContext(context, this);
		for (int i = 0; i < this.args.length; i++) {
			this.args[i].code(environment, codecontext, assembler);
		}

		assembler.add(codecontext.breakLabel);
	}

	public Expression firstConstructor() {
		return this.args.length <= 0 ? null : this.args[0].firstConstructor();
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("{\n");
		for (int j = 0; j < this.args.length; j++) {
			printIndent(printstream, i + 1);
			if (this.args[j] != null) {
				this.args[j].print(printstream, i + 1);
			} else {
				printstream.print("<empty>");
			}
			printstream.print("\n");
		}

		printIndent(printstream, i);
		printstream.print("}");
	}

	private Statement[] args;
}
