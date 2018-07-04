package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.Environment;

public final class ReturnStatement extends Statement {

	public ReturnStatement(final long l, final Expression expression) {
		super(100, l);
		this.expr = expression;
	}

	Vset check(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		this.checkLabel(environment, context);
		vset = this.reach(environment, vset);
		if (this.expr != null) {
			vset = this.expr.checkValue(environment, context, vset, hashtable);
		}
		if (context.field.isInitializer()) {
			environment.error(this.where, "return.inside.static.initializer");
			return Statement.DEAD_END;
		}
		if (context.field.getType().getReturnType().isType(11)) {
			if (this.expr != null) {
				if (context.field.isConstructor()) {
					environment.error(this.where, "return.with.value.constr", context.field);
				} else {
					environment.error(this.where, "return.with.value", context.field);
				}
				this.expr = null;
			}
		} else if (this.expr == null) {
			environment.error(this.where, "return.without.value", context.field);
		} else {
			this.expr = this.convert(environment, context, context.field.getType().getReturnType(), this.expr);
		}
		final CheckContext checkcontext = context.getReturnContext();
		if (checkcontext != null) {
			checkcontext.vsBreak = checkcontext.vsBreak.join(vset);
		}
		final CheckContext checkcontext1 = context.getTryExitContext();
		if (checkcontext1 != null) {
			checkcontext1.vsTryExit = checkcontext1.vsTryExit.join(vset);
		}
		if (this.expr != null) {
			Node node = null;
			for (Context context1 = context; context1 != null; context1 = context1.prev) {
				if (context1.node == null) {
					continue;
				}
				if (context1.node.op == 47) {
					break;
				}
				if (context1.node.op == 126) {
					node = context1.node;
					break;
				}
				if (context1.node.op == 103 && ((CheckContext) context1).vsContinue != null) {
					node = context1.node;
				}
			}

			if (node != null) {
				if (node.op == 103) {
					((FinallyStatement) node).needReturnSlot = true;
				} else {
					((SynchronizedStatement) node).needReturnSlot = true;
				}
			}
		}
		return Statement.DEAD_END;
	}

	public Statement inline(final Environment environment, final Context context) {
		if (this.expr != null) {
			this.expr = this.expr.inlineValue(environment, context);
		}
		return this;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return 1 + (this.expr == null ? 0 : this.expr.costInline(i, environment, context));
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final Expression expression = this.expr == null ? null : this.expr.copyInline(context);
		if (!flag && expression != null) {
			final Statement astatement[] = { new ExpressionStatement(this.where, expression), new InlineReturnStatement(this.where, null) };
			return new CompoundStatement(this.where, astatement);
		}
		return new InlineReturnStatement(this.where, expression);
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		if (this.expr == null) {
			this.codeFinally(environment, context, assembler, null, null);
			assembler.add(this.where, 177);
		} else {
			this.expr.codeValue(environment, context, assembler);
			this.codeFinally(environment, context, assembler, null, this.expr.type);
			assembler.add(this.where, 172 + this.expr.type.getTypeCodeOffset());
		}
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("return");
		if (this.expr != null) {
			printstream.print(" ");
			this.expr.print(printstream);
		}
		printstream.print(";");
	}

	private Expression expr;
}
