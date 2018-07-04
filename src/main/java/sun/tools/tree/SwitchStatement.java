package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Map;

import sun.tools.asm.Assembler;
import sun.tools.asm.Label;
import sun.tools.asm.SwitchData;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class SwitchStatement extends Statement {

	public SwitchStatement(final long l, final Expression expression, final Statement astatement[]) {
		super(95, l);
		this.expr = expression;
		this.args = astatement;
	}

	Vset check(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		this.checkLabel(environment, context);
		final CheckContext checkcontext = new CheckContext(context, this);
		vset = this.expr.checkValue(environment, checkcontext, this.reach(environment, vset), hashtable);
		final Type type = this.expr.type;
		this.expr = this.convert(environment, checkcontext, Type.tInt, this.expr);
		final Map hashtable1 = new Hashtable();
		boolean flag = false;
		Vset vset1 = Statement.DEAD_END;
		for (int i = 0; i < this.args.length; i++) {
			final Statement statement = this.args[i];
			if (((Node) statement).op == 96) {
				vset1 = statement.check(environment, checkcontext, vset1.join(vset.copy()), hashtable);
				final Expression expression = ((CaseStatement) statement).expr;
				if (expression != null) {
					if (expression instanceof IntegerExpression) {
						final Number integer = (Number) ((IntegerExpression) expression).getValue();
						final int j = integer.intValue();
						if (hashtable1.get(expression) != null) {
							environment.error(((Node) statement).where, "duplicate.label", integer);
						} else {
							hashtable1.put(expression, statement);
							final boolean flag1;
							switch (type.getTypeCode()) {
							case 1: // '\001'
								flag1 = j != (byte) j;
								break;

							case 3: // '\003'
								flag1 = j != (short) j;
								break;

							case 2: // '\002'
								flag1 = j != (char) j;
								break;

							default:
								flag1 = false;
								break;
							}
							if (flag1) {
								environment.error(((Node) statement).where, "switch.overflow", integer, type);
							}
						}
					} else if (!expression.isConstant() || expression.getType() != Type.tInt) {
						environment.error(((Node) statement).where, "const.expr.required");
					}
				} else {
					if (flag) {
						environment.error(((Node) statement).where, "duplicate.default");
					}
					flag = true;
				}
			} else {
				vset1 = statement.checkBlockStatement(environment, checkcontext, vset1, hashtable);
			}
		}

		if (!vset1.isDeadEnd()) {
			checkcontext.vsBreak = checkcontext.vsBreak.join(vset1);
		}
		if (flag) {
			vset = checkcontext.vsBreak;
		}
		return context.removeAdditionalVars(vset);
	}

	public Statement inline(final Environment environment, Context context) {
		context = new Context(context, this);
		this.expr = this.expr.inlineValue(environment, context);
		for (int i = 0; i < this.args.length; i++) {
			if (this.args[i] != null) {
				this.args[i] = this.args[i].inline(environment, context);
			}
		}

		return this;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final SwitchStatement switchstatement = (SwitchStatement) this.clone();
		switchstatement.expr = this.expr.copyInline(context);
		switchstatement.args = new Statement[this.args.length];
		for (int i = 0; i < this.args.length; i++) {
			if (this.args[i] != null) {
				switchstatement.args[i] = this.args[i].copyInline(context, flag);
			}
		}

		return switchstatement;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		int j = this.expr.costInline(i, environment, context);
		for (int k = 0; k < this.args.length && j < i; k++) {
			if (this.args[k] != null) {
				j += this.args[k].costInline(i, environment, context);
			}
		}

		return j;
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		final CodeContext codecontext = new CodeContext(context, this);
		this.expr.codeValue(environment, codecontext, assembler);
		final SwitchData switchdata = new SwitchData();
		boolean flag = false;
		for (int i = 0; i < this.args.length; i++) {
			final Statement statement = this.args[i];
			if (statement != null && ((Node) statement).op == 96) {
				final Expression expression = ((CaseStatement) statement).expr;
				if (expression != null) {
					switchdata.add(((IntegerExpression) expression).value, new Label());
				} else {
					flag = true;
				}
			}
		}

		if (environment.coverage()) {
			switchdata.initTableCase();
		}
		assembler.add(this.where, 170, switchdata);
		for (int j = 0; j < this.args.length; j++) {
			final Statement statement1 = this.args[j];
			if (statement1 != null) {
				if (((Node) statement1).op == 96) {
					final Expression expression1 = ((CaseStatement) statement1).expr;
					if (expression1 != null) {
						assembler.add(switchdata.get(((IntegerExpression) expression1).value));
						switchdata.addTableCase(((IntegerExpression) expression1).value, ((Node) statement1).where);
					} else {
						assembler.add(switchdata.getDefaultLabel());
						switchdata.addTableDefault(((Node) statement1).where);
					}
				} else {
					statement1.code(environment, codecontext, assembler);
				}
			}
		}

		if (!flag) {
			assembler.add(switchdata.getDefaultLabel());
		}
		assembler.add(codecontext.breakLabel);
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("switch (");
		this.expr.print(printstream);
		printstream.print(") {\n");
		for (int j = 0; j < this.args.length; j++) {
			if (this.args[j] != null) {
				printIndent(printstream, i + 1);
				this.args[j].print(printstream, i + 1);
				printstream.print("\n");
			}
		}

		printIndent(printstream, i);
		printstream.print("}");
	}

	private Expression expr;
	private Statement[] args;
}
