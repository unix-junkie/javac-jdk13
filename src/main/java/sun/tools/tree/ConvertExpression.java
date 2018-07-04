package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.java.ClassNotFound;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class ConvertExpression extends UnaryExpression {

	ConvertExpression(final long l, final Type type, final Expression expression) {
		super(55, l, type, expression);
	}

	public Vset checkValue(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		return this.right.checkValue(environment, context, vset, hashtable);
	}

	Expression simplify() {
		switch (((Node) this.right).op) {
		default:
			break;

		case 62: // '>'
		case 63: // '?'
		case 64: // '@'
		case 65: // 'A'
			final int i = ((IntegerExpression) this.right).value;
			switch (this.type.getTypeCode()) {
			case 1: // '\001'
				return new ByteExpression(((Node) this.right).where, (byte) i);

			case 2: // '\002'
				return new CharExpression(((Node) this.right).where, (char) i);

			case 3: // '\003'
				return new ShortExpression(((Node) this.right).where, (short) i);

			case 4: // '\004'
				return new IntExpression(((Node) this.right).where, i);

			case 5: // '\005'
				return new LongExpression(((Node) this.right).where, i);

			case 6: // '\006'
				return new FloatExpression(((Node) this.right).where, i);

			case 7: // '\007'
				return new DoubleExpression(((Node) this.right).where, i);
			}
			break;

		case 66: // 'B'
			final long l = ((LongExpression) this.right).value;
			switch (this.type.getTypeCode()) {
			case 1: // '\001'
				return new ByteExpression(((Node) this.right).where, (byte) (int) l);

			case 2: // '\002'
				return new CharExpression(((Node) this.right).where, (char) (int) l);

			case 3: // '\003'
				return new ShortExpression(((Node) this.right).where, (short) (int) l);

			case 4: // '\004'
				return new IntExpression(((Node) this.right).where, (int) l);

			case 6: // '\006'
				return new FloatExpression(((Node) this.right).where, l);

			case 7: // '\007'
				return new DoubleExpression(((Node) this.right).where, l);
			}
			break;

		case 67: // 'C'
			final float f = ((FloatExpression) this.right).value;
			switch (this.type.getTypeCode()) {
			case 1: // '\001'
				return new ByteExpression(((Node) this.right).where, (byte) (int) f);

			case 2: // '\002'
				return new CharExpression(((Node) this.right).where, (char) (int) f);

			case 3: // '\003'
				return new ShortExpression(((Node) this.right).where, (short) (int) f);

			case 4: // '\004'
				return new IntExpression(((Node) this.right).where, (int) f);

			case 5: // '\005'
				return new LongExpression(((Node) this.right).where, (long) f);

			case 7: // '\007'
				return new DoubleExpression(((Node) this.right).where, f);
			}
			break;

		case 68: // 'D'
			final double d = ((DoubleExpression) this.right).value;
			switch (this.type.getTypeCode()) {
			case 1: // '\001'
				return new ByteExpression(((Node) this.right).where, (byte) (int) d);

			case 2: // '\002'
				return new CharExpression(((Node) this.right).where, (char) (int) d);

			case 3: // '\003'
				return new ShortExpression(((Node) this.right).where, (short) (int) d);

			case 4: // '\004'
				return new IntExpression(((Node) this.right).where, (int) d);

			case 5: // '\005'
				return new LongExpression(((Node) this.right).where, (long) d);

			case 6: // '\006'
				return new FloatExpression(((Node) this.right).where, (float) d);
			}
			break;
		}
		return this;
	}

	public boolean equals(final int i) {
		return this.right.equals(i);
	}

	public boolean equals(final boolean flag) {
		return this.right.equals(flag);
	}

	public Expression inline(final Environment environment, final Context context) {
		if (this.right.type.inMask(1792) && this.type.inMask(1792)) {
			try {
				if (!environment.implicitCast(this.right.type, this.type)) {
					return this.inlineValue(environment, context);
				}
			} catch (final ClassNotFound classnotfound) {
				throw new CompilerError(classnotfound);
			}
		}
		return super.inline(environment, context);
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		this.right.codeValue(environment, context, assembler);
		this.codeConversion(environment, context, assembler, this.right.type, this.type);
	}

	public void print(final PrintStream printstream) {
		printstream.print('(' + Constants.opNames[this.op] + ' ' + this.type + ' ');
		this.right.print(printstream);
		printstream.print(")");
	}
}
