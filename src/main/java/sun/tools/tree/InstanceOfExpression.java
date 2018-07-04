package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.Label;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassNotFound;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class InstanceOfExpression extends BinaryExpression {

	public InstanceOfExpression(final long l, final Expression expression, final Expression expression1) {
		super(25, l, Type.tBoolean, expression, expression1);
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		vset = this.left.checkValue(environment, context, vset, hashtable);
		this.right = new TypeExpression(((Node) this.right).where, this.right.toType(environment, context));
		if (this.right.type.isType(13) || this.left.type.isType(13)) {
			return vset;
		}
		if (!this.right.type.inMask(1536)) {
			environment.error(((Node) this.right).where, "invalid.arg.type", this.right.type, Constants.opNames[this.op]);
			return vset;
		}
		try {
			if (!environment.explicitCast(this.left.type, this.right.type)) {
				environment.error(this.where, "invalid.instanceof", this.left.type, this.right.type);
			}
		} catch (final ClassNotFound classnotfound) {
			environment.error(this.where, "class.not.found", classnotfound.name, Constants.opNames[this.op]);
		}
		return vset;
	}

	public Expression inline(final Environment environment, final Context context) {
		return this.left.inline(environment, context);
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		this.left = this.left.inlineValue(environment, context);
		return this;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		if (context == null) {
			return 1 + this.left.costInline(i, environment, context);
		}
		final ClassDefinition classdefinition = context.field.getClassDefinition();
		try {
			if (this.right.type.isType(9) || classdefinition.permitInlinedAccess(environment, environment.getClassDeclaration(this.right.type))) {
				return 1 + this.left.costInline(i, environment, context);
			}
		} catch (final ClassNotFound ignored) {
		}
		return i;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.left.codeValue(environment, context, assembler);
		if (this.right.type.isType(10)) {
			assembler.add(this.where, 193, environment.getClassDeclaration(this.right.type));
		} else {
			assembler.add(this.where, 193, this.right.type);
		}
	}

	void codeBranch(final Environment environment, final Context context, final Assembler assembler, final Label label, final boolean flag) {
		this.codeValue(environment, context, assembler);
		assembler.add(this.where, flag ? 154 : 153, label, flag);
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		this.left.code(environment, context, assembler);
	}

	public void print(final PrintStream printstream) {
		printstream.print('(' + Constants.opNames[this.op] + ' ');
		this.left.print(printstream);
		printstream.print(" ");
		if (((Node) this.right).op == 147) {
			printstream.print(this.right.type);
		} else {
			this.right.print(printstream);
		}
		printstream.print(")");
	}
}
