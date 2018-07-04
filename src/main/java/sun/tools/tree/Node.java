package sun.tools.tree;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import sun.tools.java.ClassNotFound;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public class Node implements Cloneable {

	Node(final int op, final long where) {
		this.op = op;
		this.where = where;
	}

	public int getOp() {
		return this.op;
	}

	public long getWhere() {
		return this.where;
	}

	public Expression convert(final Environment environment, final Context context, final Type type, final Expression expression) {
		if (expression.type.isType(13) || type.isType(13)) {
			return expression;
		}
		if (expression.type.equals(type)) {
			return expression;
		}
		try {
			if (expression.fitsType(environment, context, type)) {
				return new ConvertExpression(this.where, type, expression);
			}
			if (environment.explicitCast(expression.type, type)) {
				environment.error(this.where, "explicit.cast.needed", Constants.opNames[this.op], expression.type, type);
				return new ConvertExpression(this.where, type, expression);
			}
		} catch (final ClassNotFound classnotfound) {
			environment.error(this.where, "class.not.found", classnotfound.name, Constants.opNames[this.op]);
		}
		environment.error(this.where, "incompatible.type", Constants.opNames[this.op], expression.type, type);
		return new ConvertExpression(this.where, Type.tError, expression);
	}

	public void print(final PrintStream printstream) {
		throw new CompilerError("print");
	}

	public final Object clone() {
		try {
			return super.clone();
		} catch (final CloneNotSupportedException ignored) {
			throw new InternalError();
		}
	}

	public String toString() {
		final ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
		this.print(new PrintStream(bytearrayoutputstream));
		return bytearrayoutputstream.toString();
	}

	int op;
	long where;
}
