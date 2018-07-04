package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.CatchData;
import sun.tools.asm.Instruction;
import sun.tools.asm.Label;
import sun.tools.asm.TryData;
import sun.tools.java.ClassDefinition;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class SynchronizedStatement extends Statement {

	public SynchronizedStatement(final long l, final Expression expression, final Statement statement) {
		super(126, l);
		this.expr = expression;
		this.body = statement;
	}

	Vset check(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		this.checkLabel(environment, context);
		final CheckContext checkcontext = new CheckContext(context, this);
		vset = this.reach(environment, vset);
		vset = this.expr.checkValue(environment, checkcontext, vset, hashtable);
		if (this.expr.type.equals(Type.tNull)) {
			environment.error(((Node) this.expr).where, "synchronized.null");
		}
		this.expr = this.convert(environment, checkcontext, Type.tClass(Constants.idJavaLangObject), this.expr);
		vset = this.body.check(environment, checkcontext, vset, hashtable);
		return context.removeAdditionalVars(vset.join(checkcontext.vsBreak));
	}

	public Statement inline(final Environment environment, final Context context) {
		if (this.body != null) {
			this.body = this.body.inline(environment, context);
		}
		this.expr = this.expr.inlineValue(environment, context);
		return this;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final SynchronizedStatement synchronizedstatement = (SynchronizedStatement) this.clone();
		synchronizedstatement.expr = this.expr.copyInline(context);
		if (this.body != null) {
			synchronizedstatement.body = this.body.copyInline(context, flag);
		}
		return synchronizedstatement;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		int j = 1;
		if (this.expr != null) {
			j += this.expr.costInline(i, environment, context);
			if (j >= i) {
				return j;
			}
		}
		if (this.body != null) {
			j += this.body.costInline(i, environment, context);
		}
		return j;
	}

	public void code(final Environment environment, Context context, final Assembler assembler) {
		final ClassDefinition classdefinition = context.field.getClassDefinition();
		this.expr.codeValue(environment, context, assembler);
		context = new Context(context);
		if (this.needReturnSlot) {
			final Type type = context.field.getType().getReturnType();
			final LocalMember localmember1 = new LocalMember(0L, classdefinition, 0, type, Constants.idFinallyReturnValue);
			context.declare(environment, localmember1);
			Environment.debugOutput("Assigning return slot to " + localmember1.number);
		}
		final LocalMember localmember = new LocalMember(this.where, classdefinition, 0, Type.tObject, null);
		final LocalMember localmember2 = new LocalMember(this.where, classdefinition, 0, Type.tInt, null);
		final Integer integer = new Integer(context.declare(environment, localmember));
		final Integer integer1 = new Integer(context.declare(environment, localmember2));
		final Instruction label = new Label();
		final TryData trydata = new TryData();
		trydata.add(null);
		assembler.add(this.where, 58, integer);
		assembler.add(this.where, 25, integer);
		assembler.add(this.where, 194);
		final CodeContext codecontext = new CodeContext(context, this);
		assembler.add(this.where, -3, trydata);
		if (this.body != null) {
			this.body.code(environment, codecontext, assembler);
		} else {
			assembler.add(this.where, 0);
		}
		assembler.add(codecontext.breakLabel);
		assembler.add(trydata.getEndLabel());
		assembler.add(this.where, 25, integer);
		assembler.add(this.where, 195);
		assembler.add(this.where, 167, label);
		final CatchData catchdata = trydata.getCatch(0);
		assembler.add(catchdata.getLabel());
		assembler.add(this.where, 25, integer);
		assembler.add(this.where, 195);
		assembler.add(this.where, 191);
		assembler.add(codecontext.contLabel);
		assembler.add(this.where, 58, integer1);
		assembler.add(this.where, 25, integer);
		assembler.add(this.where, 195);
		assembler.add(this.where, 169, integer1);
		assembler.add(label);
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("synchronized ");
		this.expr.print(printstream);
		printstream.print(" ");
		if (this.body != null) {
			this.body.print(printstream, i);
		} else {
			printstream.print("{}");
		}
	}

	private Expression expr;
	private Statement body;
	boolean needReturnSlot;
}
