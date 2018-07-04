package sun.tools.tree;

import java.io.PrintStream;

import sun.tools.asm.Assembler;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Type;

public final class InlineNewInstanceExpression extends Expression {

	InlineNewInstanceExpression(final long l, final Type type, final MemberDefinition memberdefinition, final Statement statement) {
		super(151, l, type);
		this.field = memberdefinition;
		this.body = statement;
	}

	public Expression inline(final Environment environment, final Context context) {
		return this.inlineValue(environment, context);
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		if (this.body != null) {
			final LocalMember localmember = (LocalMember) this.field.getArguments().get(0);
			final Context context1 = new Context(context, this);
			context1.declare(environment, localmember);
			this.body = this.body.inline(environment, context1);
		}
		if (this.body != null && ((Node) this.body).op == 149) {
			this.body = null;
		}
		return this;
	}

	public Expression copyInline(final Context context) {
		final InlineNewInstanceExpression inlinenewinstanceexpression = (InlineNewInstanceExpression) this.clone();
		inlinenewinstanceexpression.body = this.body.copyInline(context, true);
		return inlinenewinstanceexpression;
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		this.codeCommon(environment, context, assembler, false);
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.codeCommon(environment, context, assembler, true);
	}

	private void codeCommon(final Environment environment, final Context context, final Assembler assembler, final boolean flag) {
		assembler.add(this.where, 187, this.field.getClassDeclaration());
		if (this.body != null) {
			final LocalMember localmember = (LocalMember) this.field.getArguments().get(0);
			final CodeContext codecontext = new CodeContext(context, this);
			codecontext.declare(environment, localmember);
			assembler.add(this.where, 58, new Integer(localmember.number));
			this.body.code(environment, codecontext, assembler);
			assembler.add(codecontext.breakLabel);
			if (flag) {
				assembler.add(this.where, 25, new Integer(localmember.number));
			}
		}
	}

	public void print(final PrintStream printstream) {
		final LocalMember localmember = (LocalMember) this.field.getArguments().get(0);
		printstream.println('(' + Constants.opNames[this.op] + '#' + localmember.hashCode() + '=' + this.field.hashCode());
		if (this.body != null) {
			this.body.print(printstream, 1);
		} else {
			printstream.print("<empty>");
		}
		printstream.print(")");
	}

	final MemberDefinition field;
	private Statement body;
}
