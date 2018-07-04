package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.asm.ArrayData;
import sun.tools.asm.Assembler;
import sun.tools.java.CompilerError;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class NewArrayExpression extends NaryExpression {

	public NewArrayExpression(final long l, final Expression expression, final Expression aexpression[]) {
		super(41, l, Type.tError, expression, aexpression);
	}

	public NewArrayExpression(final long l, final Expression expression, final Expression aexpression[], final Expression expression1) {
		this(l, expression, aexpression);
		this.init = expression1;
	}

	public Vset checkValue(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		this.type = this.right.toType(environment, context);
		boolean flag = this.init != null;
		for (int i = 0; i < this.args.length; i++) {
			final Expression expression = this.args[i];
			if (expression == null) {
				if (i == 0 && !flag) {
					environment.error(this.where, "array.dim.missing");
				}
				flag = true;
			} else {
				if (flag) {
					environment.error(((Node) expression).where, "invalid.array.dim");
				}
				vset = expression.checkValue(environment, context, vset, hashtable);
				this.args[i] = this.convert(environment, context, Type.tInt, expression);
			}
			this.type = Type.tArray(this.type);
		}

		if (this.init != null) {
			vset = this.init.checkInitializer(environment, context, vset, this.type, hashtable);
			this.init = this.convert(environment, context, this.type, this.init);
		}
		return vset;
	}

	public Expression copyInline(final Context context) {
		final NewArrayExpression newarrayexpression = (NewArrayExpression) super.copyInline(context);
		if (this.init != null) {
			newarrayexpression.init = this.init.copyInline(context);
		}
		return newarrayexpression;
	}

	public Expression inline(final Environment environment, final Context context) {
		Object obj = null;
		for (int i = 0; i < this.args.length; i++) {
			if (this.args[i] != null) {
				obj = obj == null ? (Object) this.args[i] : (Object) new CommaExpression(this.where, (Expression) obj, this.args[i]);
			}
		}

		if (this.init != null) {
			obj = obj == null ? (Object) this.init : (Object) new CommaExpression(this.where, (Expression) obj, this.init);
		}
		return obj == null ? null : ((Expression) obj).inline(environment, context);
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		if (this.init != null) {
			return this.init.inlineValue(environment, context);
		}
		for (int i = 0; i < this.args.length; i++) {
			if (this.args[i] != null) {
				this.args[i] = this.args[i].inlineValue(environment, context);
			}
		}

		return this;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		int i = 0;
		for (int j = 0; j < this.args.length; j++) {
			if (this.args[j] != null) {
				this.args[j].codeValue(environment, context, assembler);
				i++;
			}
		}

		if (this.args.length > 1) {
			assembler.add(this.where, 197, new ArrayData(this.type, i));
			return;
		}
		switch (this.type.getElementType().getTypeCode()) {
		case 0: // '\0'
			assembler.add(this.where, 188, new Integer(4));
			break;

		case 1: // '\001'
			assembler.add(this.where, 188, new Integer(8));
			break;

		case 3: // '\003'
			assembler.add(this.where, 188, new Integer(9));
			break;

		case 2: // '\002'
			assembler.add(this.where, 188, new Integer(5));
			break;

		case 4: // '\004'
			assembler.add(this.where, 188, new Integer(10));
			break;

		case 5: // '\005'
			assembler.add(this.where, 188, new Integer(11));
			break;

		case 6: // '\006'
			assembler.add(this.where, 188, new Integer(6));
			break;

		case 7: // '\007'
			assembler.add(this.where, 188, new Integer(7));
			break;

		case 9: // '\t'
			assembler.add(this.where, 189, this.type.getElementType());
			break;

		case 10: // '\n'
			assembler.add(this.where, 189, environment.getClassDeclaration(this.type.getElementType()));
			break;

		case 8: // '\b'
		default:
			throw new CompilerError("codeValue");
		}
	}

	private Expression init;
}
