package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class TypeExpression extends Expression {

	public TypeExpression(final long l, final Type type) {
		super(147, l, type);
	}

	Type toType(final Environment environment, final Context context) {
		return this.type;
	}

	public Vset checkValue(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		environment.error(this.where, "invalid.term");
		this.type = Type.tError;
		return vset;
	}

	public Vset checkAmbigName(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final UnaryExpression unaryexpression) {
		return vset;
	}

	public void print(final PrintStream printstream) {
		printstream.print(this.type);
	}
}
