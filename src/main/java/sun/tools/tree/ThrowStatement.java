package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.ClassDeclaration;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassNotFound;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class ThrowStatement extends Statement {

	public ThrowStatement(final long l, final Expression expression) {
		super(104, l);
		this.expr = expression;
	}

	Vset check(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		this.checkLabel(environment, context);
		try {
			vset = this.reach(environment, vset);
			this.expr.checkValue(environment, context, vset, hashtable);
			if (this.expr.type.isType(10)) {
				final ClassDeclaration classdeclaration = environment.getClassDeclaration(this.expr.type);
				if (hashtable.get(classdeclaration) == null) {
					hashtable.put(classdeclaration, this);
				}
				final ClassDefinition classdefinition = classdeclaration.getClassDefinition(environment);
				final ClassDeclaration classdeclaration1 = environment.getClassDeclaration(Constants.idJavaLangThrowable);
				if (!classdefinition.subClassOf(environment, classdeclaration1)) {
					environment.error(this.where, "throw.not.throwable", classdefinition);
				}
				this.expr = this.convert(environment, context, Type.tObject, this.expr);
			} else if (!this.expr.type.isType(13)) {
				environment.error(((Node) this.expr).where, "throw.not.throwable", this.expr.type);
			}
		} catch (final ClassNotFound classnotfound) {
			environment.error(this.where, "class.not.found", classnotfound.name, Constants.opNames[this.op]);
		}
		final CheckContext checkcontext = context.getTryExitContext();
		if (checkcontext != null) {
			checkcontext.vsTryExit = checkcontext.vsTryExit.join(vset);
		}
		return Statement.DEAD_END;
	}

	public Statement inline(final Environment environment, final Context context) {
		this.expr = this.expr.inlineValue(environment, context);
		return this;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final ThrowStatement throwstatement = (ThrowStatement) this.clone();
		throwstatement.expr = this.expr.copyInline(context);
		return throwstatement;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return 1 + this.expr.costInline(i, environment, context);
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		this.expr.codeValue(environment, context, assembler);
		assembler.add(this.where, 191);
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("throw ");
		this.expr.print(printstream);
		printstream.print(":");
	}

	private Expression expr;
}
