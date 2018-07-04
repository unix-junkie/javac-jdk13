package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;

import sun.tools.asm.Assembler;
import sun.tools.asm.CatchData;
import sun.tools.asm.Label;
import sun.tools.asm.TryData;
import sun.tools.java.ClassDefinition;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class FinallyStatement extends Statement {

	public FinallyStatement(final long l, final Statement statement, final Statement statement1) {
		super(103, l);
		this.body = statement;
		this.finalbody = statement1;
	}

	Vset check(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		vset = this.reach(environment, vset);
		final Hashtable hashtable1 = new Hashtable();
		final CheckContext checkcontext = new CheckContext(context, this);
		final Vset vset1 = this.body.check(environment, checkcontext, vset.copy(), hashtable1).join(checkcontext.vsBreak);
		final CheckContext checkcontext1 = new CheckContext(context, this);
		checkcontext1.vsContinue = null;
		Vset vset2 = this.finalbody.check(environment, checkcontext1, vset, hashtable);
		this.finallyCanFinish = !vset2.isDeadEnd();
		vset2 = vset2.join(checkcontext1.vsBreak);
		if (this.finallyCanFinish) {
			Object obj;
			for (final Iterator iterator = hashtable1.keySet().iterator(); iterator.hasNext(); hashtable.put(obj, hashtable1.get(obj))) {
				obj = iterator.next();
			}

		}
		return context.removeAdditionalVars(vset1.addDAandJoinDU(vset2));
	}

	public Statement inline(final Environment environment, Context context) {
		if (this.tryTemp != null) {
			context = new Context(context, this);
			context.declare(environment, this.tryTemp);
		}
		if (this.init != null) {
			this.init = this.init.inline(environment, context);
		}
		if (this.body != null) {
			this.body = this.body.inline(environment, context);
		}
		if (this.finalbody != null) {
			this.finalbody = this.finalbody.inline(environment, context);
		}
		if (this.body == null) {
			return this.eliminate(environment, this.finalbody);
		}
		return this.finalbody == null ? this.eliminate(environment, this.body) : this;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final FinallyStatement finallystatement = (FinallyStatement) this.clone();
		if (this.tryTemp != null) {
			finallystatement.tryTemp = this.tryTemp.copyInline(context);
		}
		if (this.init != null) {
			finallystatement.init = this.init.copyInline(context, flag);
		}
		if (this.body != null) {
			finallystatement.body = this.body.copyInline(context, flag);
		}
		if (this.finalbody != null) {
			finallystatement.finalbody = this.finalbody.copyInline(context, flag);
		}
		return finallystatement;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		int j = 4;
		if (this.init != null) {
			j += this.init.costInline(i, environment, context);
			if (j >= i) {
				return j;
			}
		}
		if (this.body != null) {
			j += this.body.costInline(i, environment, context);
			if (j >= i) {
				return j;
			}
		}
		if (this.finalbody != null) {
			j += this.finalbody.costInline(i, environment, context);
		}
		return j;
	}

	public void code(final Environment environment, Context context, final Assembler assembler) {
		context = new Context(context);
		final Label label = new Label();
		if (this.tryTemp != null) {
			context.declare(environment, this.tryTemp);
		}
		if (this.init != null) {
			final Context codecontext = new CodeContext(context, this);
			this.init.code(environment, codecontext, assembler);
		}
		Integer integer1 = null;
		Integer integer = null;
		if (this.finallyCanFinish) {
			final ClassDefinition classdefinition = context.field.getClassDefinition();
			if (this.needReturnSlot) {
				final Type type = context.field.getType().getReturnType();
				final LocalMember localmember2 = new LocalMember(0L, classdefinition, 0, type, Constants.idFinallyReturnValue);
				context.declare(environment, localmember2);
				Environment.debugOutput("Assigning return slot to " + localmember2.number);
			}
			final LocalMember localmember = new LocalMember(this.where, classdefinition, 0, Type.tObject, null);
			final LocalMember localmember1 = new LocalMember(this.where, classdefinition, 0, Type.tInt, null);
			integer = new Integer(context.declare(environment, localmember));
			integer1 = new Integer(context.declare(environment, localmember1));
		}
		final TryData trydata = new TryData();
		trydata.add(null);
		final CodeContext codecontext1 = new CodeContext(context, this);
		assembler.add(this.where, -3, trydata);
		this.body.code(environment, codecontext1, assembler);
		assembler.add(codecontext1.breakLabel);
		assembler.add(trydata.getEndLabel());
		if (this.finallyCanFinish) {
			assembler.add(this.where, 168, codecontext1.contLabel);
			assembler.add(this.where, 167, label);
		} else {
			assembler.add(this.where, 167, codecontext1.contLabel);
		}
		final CatchData catchdata = trydata.getCatch(0);
		assembler.add(catchdata.getLabel());
		if (this.finallyCanFinish) {
			assembler.add(this.where, 58, integer);
			assembler.add(this.where, 168, codecontext1.contLabel);
			assembler.add(this.where, 25, integer);
			assembler.add(this.where, 191);
		} else {
			assembler.add(this.where, 87);
		}
		assembler.add(codecontext1.contLabel);
		codecontext1.contLabel = null;
		codecontext1.breakLabel = label;
		if (this.finallyCanFinish) {
			assembler.add(this.where, 58, integer1);
			this.finalbody.code(environment, codecontext1, assembler);
			assembler.add(this.where, 169, integer1);
		} else {
			this.finalbody.code(environment, codecontext1, assembler);
		}
		assembler.add(label);
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("try ");
		if (this.body != null) {
			this.body.print(printstream, i);
		} else {
			printstream.print("<empty>");
		}
		printstream.print(" finally ");
		if (this.finalbody != null) {
			this.finalbody.print(printstream, i);
		} else {
			printstream.print("<empty>");
		}
	}

	private Statement body;
	private Statement finalbody;
	boolean finallyCanFinish;
	boolean needReturnSlot;
	private Statement init;
	private LocalMember tryTemp;
}
