package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class DeclarationStatement extends Statement {

	public DeclarationStatement(final long l, final int i, final Expression expression, final Statement astatement[]) {
		super(107, l);
		this.mod = i;
		this.type = expression;
		this.args = astatement;
	}

	Vset check(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		environment.error(this.where, "invalid.decl");
		return this.checkBlockStatement(environment, context, vset, hashtable);
	}

	Vset checkBlockStatement(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		if (this.labels != null) {
			environment.error(this.where, "declaration.with.label", this.labels[0]);
		}
		vset = this.reach(environment, vset);
		final Type type1 = this.type.toType(environment, context);
		for (int i = 0; i < this.args.length; i++) {
			vset = this.args[i].checkDeclaration(environment, context, vset, this.mod, type1, hashtable);
		}

		return vset;
	}

	public Statement inline(final Environment environment, final Context context) {
		int i = 0;
		for (int j = 0; j < this.args.length; j++) {
			if ((this.args[j] = this.args[j].inline(environment, context)) != null) {
				i++;
			}
		}

		return i != 0 ? this : null;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final DeclarationStatement declarationstatement = (DeclarationStatement) this.clone();
		if (this.type != null) {
			declarationstatement.type = this.type.copyInline(context);
		}
		declarationstatement.args = new Statement[this.args.length];
		for (int i = 0; i < this.args.length; i++) {
			if (this.args[i] != null) {
				declarationstatement.args[i] = this.args[i].copyInline(context, flag);
			}
		}

		return declarationstatement;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		int j = 1;
		for (int k = 0; k < this.args.length; k++) {
			if (this.args[k] != null) {
				j += this.args[k].costInline(i, environment, context);
			}
		}

		return j;
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		for (int i = 0; i < this.args.length; i++) {
			if (this.args[i] != null) {
				this.args[i].code(environment, context, assembler);
			}
		}

	}

	public void print(final PrintStream printstream, final int i) {
		printstream.print("declare ");
		super.print(printstream, i);
		this.type.print(printstream);
		printstream.print(" ");
		for (int j = 0; j < this.args.length; j++) {
			if (j > 0) {
				printstream.print(", ");
			}
			if (this.args[j] != null) {
				this.args[j].print(printstream);
			} else {
				printstream.print("<empty>");
			}
		}

		printstream.print(";");
	}

	private final int mod;
	private Expression type;
	private Statement[] args;
}
