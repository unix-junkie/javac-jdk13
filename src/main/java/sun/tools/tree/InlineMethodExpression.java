package sun.tools.tree;

import java.io.PrintStream;

import sun.tools.asm.Assembler;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Type;

public final class InlineMethodExpression extends Expression {

	InlineMethodExpression(final long l, final Type type, final MemberDefinition memberdefinition, final Statement statement) {
		super(150, l, type);
		this.field = memberdefinition;
		this.body = statement;
	}

	public Expression inline(final Environment environment, final Context context) {
		this.body = this.body.inline(environment, new Context(context, this));
		if (this.body == null) {
			return null;
		}
		if (((Node) this.body).op == 149) {
			final Expression expression = ((InlineReturnStatement) this.body).expr;
			if (expression != null && this.type.isType(11)) {
				throw new CompilerError("value on inline-void return");
			}
			return expression;
		}
		return this;
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		return this.inline(environment, context);
	}

	public Expression copyInline(final Context context) {
		final InlineMethodExpression inlinemethodexpression = (InlineMethodExpression) this.clone();
		if (this.body != null) {
			inlinemethodexpression.body = this.body.copyInline(context, true);
		}
		return inlinemethodexpression;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		final CodeContext codecontext = new CodeContext(context, this);
		this.body.code(environment, codecontext, assembler);
		assembler.add(codecontext.breakLabel);
	}

	public void print(final PrintStream printstream) {
		printstream.print('(' + Constants.opNames[this.op] + '\n');
		this.body.print(printstream, 1);
		printstream.print(")");
	}

	final MemberDefinition field;
	private Statement body;
}
