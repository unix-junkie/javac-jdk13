package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.Label;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.Type;

public class Statement extends Node {

	Statement(final int i, final long l) {
		super(i, l);
		this.labels = null;
	}

	public static Statement insertStatement(final Statement statement, final Statement statement1) {
		if (statement1 == null) {
			return statement;
		}
		if (statement1 instanceof CompoundStatement) {
			((CompoundStatement) statement1).insertStatement(statement);
		} else {
			final Statement astatement[] = { statement, statement1 };
			return new CompoundStatement(statement.getWhere(), astatement);
		}
		return statement1;
	}

	public void setLabel(final Environment environment, final Expression expression) {
		if (((Node) expression).op == 60) {
			if (this.labels == null) {
				this.labels = new Identifier[1];
			} else {
				final Identifier aidentifier[] = new Identifier[this.labels.length + 1];
				System.arraycopy(this.labels, 0, aidentifier, 1, this.labels.length);
				this.labels = aidentifier;
			}
			this.labels[0] = ((IdentifierExpression) expression).id;
		} else {
			environment.error(((Node) expression).where, "invalid.label");
		}
	}

	public Vset checkMethod(final Environment environment, Context context, Vset vset, final Hashtable hashtable) {
		final CheckContext checkcontext = new CheckContext(context, new Statement(47, 0L));
		context = checkcontext;
		vset = this.check(environment, context, vset, hashtable);
		if (!context.field.getType().getReturnType().isType(11) && !vset.isDeadEnd()) {
			environment.error(context.field.getWhere(), "return.required.at.end", context.field);
		}
		vset = vset.join(checkcontext.vsBreak);
		return vset;
	}

	Vset checkDeclaration(final Environment environment, final Context context, final Vset vset, final int i, final Type type, final Hashtable hashtable) {
		throw new CompilerError("checkDeclaration");
	}

	void checkLabel(final Environment environment, final Context context) {
		if (this.labels != null) {
			label0: for (int i = 0; i < this.labels.length; i++) {
				for (int j = i + 1; j < this.labels.length; j++) {
					if (this.labels[i] != this.labels[j]) {
						continue;
					}
					environment.error(this.where, "nested.duplicate.label", this.labels[i]);
					continue label0;
				}

				final CheckContext checkcontext = (CheckContext) context.getLabelContext(this.labels[i]);
				if (checkcontext != null && ((Context) checkcontext).frameNumber == context.frameNumber) {
					environment.error(this.where, "nested.duplicate.label", this.labels[i]);
				}
			}

		}
	}

	Vset check(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		throw new CompilerError("check");
	}

	Vset checkBlockStatement(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		return this.check(environment, context, vset, hashtable);
	}

	Vset reach(final Environment environment, final Vset vset) {
		if (vset.isDeadEnd()) {
			environment.error(this.where, "stat.not.reached");
			return vset.clearDeadEnd();
		}
		return vset;
	}

	public Statement inline(final Environment environment, final Context context) {
		return this;
	}

	Statement eliminate(final Environment environment, Statement statement) {
		if (statement != null && this.labels != null) {
			final Statement astatement[] = { statement };
			statement = new CompoundStatement(this.where, astatement);
			statement.labels = this.labels;
		}
		return statement;
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		throw new CompilerError("code");
	}

	void codeFinally(final Environment environment, final Context context, final Assembler assembler, final Context context1, final Type type) {
		boolean flag = false;
		boolean flag1 = false;
		for (Context context2 = context; context2 != null && context2 != context1; context2 = context2.prev) {
			if (context2.node == null) {
				continue;
			}
			if (context2.node.op == 126) {
				flag = true;
				continue;
			}
			if (context2.node.op != 103 || ((CodeContext) context2).contLabel == null) {
				continue;
			}
			flag = true;
			final FinallyStatement finallystatement = (FinallyStatement) context2.node;
			if (finallystatement.finallyCanFinish) {
				continue;
			}
			flag1 = true;
			break;
		}

		if (!flag) {
			return;
		}
		Integer integer = null;
		if (type != null) {
			context.field.getClassDefinition();
			if (!flag1) {
				final LocalMember localmember = context.getLocalField(Constants.idFinallyReturnValue);
				integer = new Integer(localmember.number);
				assembler.add(this.where, 54 + type.getTypeCodeOffset(), integer);
			} else {
				switch (context.field.getType().getReturnType().getTypeCode()) {
				case 5: // '\005'
				case 7: // '\007'
					assembler.add(this.where, 88);
					break;

				default:
					assembler.add(this.where, 87);
					break;

				case 11: // '\013'
					break;
				}
			}
		}
		for (Context context3 = context; context3 != null && context3 != context1; context3 = context3.prev) {
			if (context3.node == null) {
				continue;
			}
			if (context3.node.op == 126) {
				assembler.add(this.where, 168, ((CodeContext) context3).contLabel);
				continue;
			}
			if (context3.node.op != 103 || ((CodeContext) context3).contLabel == null) {
				continue;
			}
			final FinallyStatement finallystatement1 = (FinallyStatement) context3.node;
			final Label label = ((CodeContext) context3).contLabel;
			if (finallystatement1.finallyCanFinish) {
				assembler.add(this.where, 168, label);
				continue;
			}
			assembler.add(this.where, 167, label);
			break;
		}

		if (integer != null) {
			assembler.add(this.where, 21 + type.getTypeCodeOffset(), integer);
		}
	}

	public boolean hasLabel(final Identifier identifier) {
		final Identifier aidentifier[] = this.labels;
		if (aidentifier != null) {
			for (int i = aidentifier.length; --i >= 0;) {
				if (aidentifier[i].equals(identifier)) {
					return true;
				}
			}

		}
		return false;
	}

	public Expression firstConstructor() {
		return null;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		return (Statement) this.clone();
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return i;
	}

	static void printIndent(final PrintStream printstream, final int i) {
		for (int j = 0; j < i; j++) {
			printstream.print("    ");
		}

	}

	public void print(final PrintStream printstream, final int i) {
		if (this.labels != null) {
			for (int j = this.labels.length; --j >= 0;) {
				printstream.print(this.labels[j] + ": ");
			}

		}
	}

	public void print(final PrintStream printstream) {
		this.print(printstream, 0);
	}

	static final Vset DEAD_END;
	Identifier labels[];
	public static final Node empty = new Statement(105, 0L);
	public static final int MAXINLINECOST = Integer.getInteger("javac.maxinlinecost", 30).intValue();

	static {
		DEAD_END = Vset.DEAD_END;
	}
}
