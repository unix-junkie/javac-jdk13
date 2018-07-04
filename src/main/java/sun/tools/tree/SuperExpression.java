package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.java.ClassDeclaration;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class SuperExpression extends ThisExpression {

	public SuperExpression(final long where) {
		super(83, where);
	}

	public SuperExpression(final long where, final Expression outerArg) {
		super(where, outerArg);
		this.op = 83;
	}

	SuperExpression(final long where, final Context context) {
		super(where, context);
		this.op = 83;
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		vset = this.checkCommon(environment, context, vset, hashtable);
		if (this.type != Type.tError) {
			environment.error(this.where, "undef.var.super", Constants.idSuper);
		}
		return vset;
	}

	public Vset checkAmbigName(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final UnaryExpression unaryexpression) {
		return this.checkCommon(environment, context, vset, hashtable);
	}

	private Vset checkCommon(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		final ClassDeclaration classdeclaration = context.field.getClassDefinition().getSuperClass();
		if (classdeclaration == null) {
			environment.error(this.where, "undef.var", Constants.idSuper);
			this.type = Type.tError;
			return vset;
		}
		vset = super.checkValue(environment, context, vset, hashtable);
		this.type = classdeclaration.getType();
		return vset;
	}
}
