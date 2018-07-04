package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassNotFound;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class CastExpression extends BinaryExpression {

	public CastExpression(final long l, final Expression expression, final Expression expression1) {
		super(34, l, expression.type, expression, expression1);
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		this.type = this.left.toType(environment, context);
		vset = this.right.checkValue(environment, context, vset, hashtable);
		if (this.type.isType(13) || this.right.type.isType(13)) {
			return vset;
		}
		if (this.type.equals(this.right.type)) {
			return vset;
		}
		try {
			if (environment.explicitCast(this.right.type, this.type)) {
				this.right = new ConvertExpression(this.where, this.type, this.right);
				return vset;
			}
		} catch (final ClassNotFound classnotfound) {
			environment.error(this.where, "class.not.found", classnotfound.name, Constants.opNames[this.op]);
		}
		environment.error(this.where, "invalid.cast", this.right.type, this.type);
		return vset;
	}

	public boolean isConstant() {
		return (!this.type.inMask(1792) || this.type.equals(Type.tString)) && this.right.isConstant();
	}

	public Expression inline(final Environment environment, final Context context) {
		return this.right.inline(environment, context);
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		return this.right.inlineValue(environment, context);
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		if (context == null) {
			return 1 + this.right.costInline(i, environment, context);
		}
		final ClassDefinition classdefinition = context.field.getClassDefinition();
		try {
			if (this.left.type.isType(9) || classdefinition.permitInlinedAccess(environment, environment.getClassDeclaration(this.left.type))) {
				return 1 + this.right.costInline(i, environment, context);
			}
		} catch (final ClassNotFound ignored) {
		}
		return i;
	}

	public void print(final PrintStream printstream) {
		printstream.print('(' + Constants.opNames[this.op] + ' ');
		if (this.type.isType(13)) {
			this.left.print(printstream);
		} else {
			printstream.print(this.type);
		}
		printstream.print(" ");
		this.right.print(printstream);
		printstream.print(")");
	}
}
