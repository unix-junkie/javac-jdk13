package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class ArrayAccessExpression extends UnaryExpression {

	public ArrayAccessExpression(final long l, final Expression expression, final Expression expression1) {
		super(48, l, Type.tError, expression);
		this.index = expression1;
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		vset = this.right.checkValue(environment, context, vset, hashtable);
		if (this.index == null) {
			environment.error(this.where, "array.index.required");
			return vset;
		}
		vset = this.index.checkValue(environment, context, vset, hashtable);
		this.index = this.convert(environment, context, Type.tInt, this.index);
		if (!this.right.type.isType(9)) {
			if (!this.right.type.isType(13)) {
				environment.error(this.where, "not.array", this.right.type);
			}
			return vset;
		}
		this.type = this.right.type.getElementType();
		return vset;
	}

	public Vset checkAmbigName(final Environment environment, final Context context, Vset vset, final Hashtable hashtable, final UnaryExpression unaryexpression) {
		if (this.index == null) {
			vset = this.right.checkAmbigName(environment, context, vset, hashtable, this);
			if (this.right.type == Type.tPackage) {
				FieldExpression.reportFailedPackagePrefix(environment, this.right);
				return vset;
			}
			if (this.right instanceof TypeExpression) {
				final Type type = Type.tArray(this.right.type);
				unaryexpression.right = new TypeExpression(this.where, type);
				return vset;
			}
			environment.error(this.where, "array.index.required");
			return vset;
		}
		return super.checkAmbigName(environment, context, vset, hashtable, unaryexpression);
	}

	public Vset checkLHS(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		return this.checkValue(environment, context, vset, hashtable);
	}

	public Vset checkAssignOp(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final Expression expression) {
		return this.checkValue(environment, context, vset, hashtable);
	}

	public FieldUpdater getAssigner(final Environment environment, final Context context) {
		return null;
	}

	public FieldUpdater getUpdater(final Environment environment, final Context context) {
		return null;
	}

	Type toType(final Environment environment, final Context context) {
		return this.toType(environment, this.right.toType(environment, context));
	}

	private Type toType(final Environment environment, final Type type) {
		if (this.index != null) {
			environment.error(((Node) this.index).where, "array.dim.in.type");
		}
		return Type.tArray(type);
	}

	public Expression inline(final Environment environment, final Context context) {
		this.right = this.right.inlineValue(environment, context);
		this.index = this.index.inlineValue(environment, context);
		return this;
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		this.right = this.right.inlineValue(environment, context);
		this.index = this.index.inlineValue(environment, context);
		return this;
	}

	public Expression inlineLHS(final Environment environment, final Context context) {
		return this.inlineValue(environment, context);
	}

	public Expression copyInline(final Context context) {
		final ArrayAccessExpression arrayaccessexpression = (ArrayAccessExpression) this.clone();
		arrayaccessexpression.right = this.right.copyInline(context);
		arrayaccessexpression.index = this.index == null ? null : this.index.copyInline(context);
		return arrayaccessexpression;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return 1 + this.right.costInline(i, environment, context) + this.index.costInline(i, environment, context);
	}

	int codeLValue(final Environment environment, final Context context, final Assembler assembler) {
		this.right.codeValue(environment, context, assembler);
		this.index.codeValue(environment, context, assembler);
		return 2;
	}

	void codeLoad(final Environment environment, final Context context, final Assembler assembler) {
		switch (this.type.getTypeCode()) {
		case 0: // '\0'
		case 1: // '\001'
			assembler.add(this.where, 51);
			break;

		case 2: // '\002'
			assembler.add(this.where, 52);
			break;

		case 3: // '\003'
			assembler.add(this.where, 53);
			break;

		default:
			assembler.add(this.where, 46 + this.type.getTypeCodeOffset());
			break;
		}
	}

	void codeStore(final Environment environment, final Context context, final Assembler assembler) {
		switch (this.type.getTypeCode()) {
		case 0: // '\0'
		case 1: // '\001'
			assembler.add(this.where, 84);
			break;

		case 2: // '\002'
			assembler.add(this.where, 85);
			break;

		case 3: // '\003'
			assembler.add(this.where, 86);
			break;

		default:
			assembler.add(this.where, 79 + this.type.getTypeCodeOffset());
			break;
		}
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.codeLValue(environment, context, assembler);
		this.codeLoad(environment, context, assembler);
	}

	public void print(final PrintStream printstream) {
		printstream.print('(' + Constants.opNames[this.op] + ' ');
		this.right.print(printstream);
		printstream.print(" ");
		if (this.index != null) {
			this.index.print(printstream);
		} else {
			printstream.print("<empty>");
		}
		printstream.print(")");
	}

	Expression index;
}
