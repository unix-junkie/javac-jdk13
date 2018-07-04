package sun.tools.tree;

import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.CompilerError;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class ArrayExpression extends NaryExpression {

	public ArrayExpression(final long l, final Expression aexpression[]) {
		super(57, l, Type.tError, null, aexpression);
	}

	public Vset checkValue(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		environment.error(this.where, "invalid.array.expr");
		return vset;
	}

	public Vset checkInitializer(final Environment environment, final Context context, Vset vset, Type type, final Hashtable hashtable) {
		if (!type.isType(9)) {
			if (!type.isType(13)) {
				environment.error(this.where, "invalid.array.init", type);
			}
			return vset;
		}
		super.type = type;
		type = type.getElementType();
		for (int i = 0; i < this.args.length; i++) {
			vset = this.args[i].checkInitializer(environment, context, vset, type, hashtable);
			this.args[i] = this.convert(environment, context, type, this.args[i]);
		}

		return vset;
	}

	public Expression inline(final Environment environment, final Context context) {
		Object obj = null;
		for (int i = 0; i < this.args.length; i++) {
			this.args[i] = this.args[i].inline(environment, context);
			if (this.args[i] != null) {
				obj = obj != null ? (Object) new CommaExpression(this.where, (Expression) obj, this.args[i]) : (Object) this.args[i];
			}
		}

		return (Expression) obj;
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		for (int i = 0; i < this.args.length; i++) {
			this.args[i] = this.args[i].inlineValue(environment, context);
		}

		return this;
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 18, new Integer(this.args.length));
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
		for (int i = 0; i < this.args.length; i++) {
			if (!this.args[i].equalsDefault()) {
				assembler.add(this.where, 89);
				assembler.add(this.where, 18, new Integer(i));
				this.args[i].codeValue(environment, context, assembler);
				switch (this.type.getElementType().getTypeCode()) {
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
					assembler.add(this.where, 79 + this.type.getElementType().getTypeCodeOffset());
					break;
				}
			}
		}

	}
}
