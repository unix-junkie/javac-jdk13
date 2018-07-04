package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.AmbiguousMember;
import sun.tools.java.ClassDeclaration;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassNotFound;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Type;

public final class AddExpression extends BinaryArithmeticExpression {

	public AddExpression(final long l, final Expression expression, final Expression expression1) {
		super(29, l, expression, expression1);
	}

	void selectType(final Environment environment, final Context context, final int i) {
		if (this.left.type == Type.tString && !this.right.type.isType(11)) {
			this.type = Type.tString;
			return;
		}
		if (this.right.type == Type.tString && !this.left.type.isType(11)) {
			this.type = Type.tString;
			return;
		}
		super.selectType(environment, context, i);
	}

	public boolean isNonNull() {
		return true;
	}

	Expression eval(final int i, final int j) {
		return new IntExpression(this.where, i + j);
	}

	Expression eval(final long l, final long l1) {
		return new LongExpression(this.where, l + l1);
	}

	Expression eval(final float f, final float f1) {
		return new FloatExpression(this.where, f + f1);
	}

	Expression eval(final double d, final double d1) {
		return new DoubleExpression(this.where, d + d1);
	}

	Expression eval(final String s, final String s1) {
		return new StringExpression(this.where, s + s1);
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		if (this.type == Type.tString && this.isConstant()) {
			final StringBuffer stringbuffer = this.inlineValueSB(environment, context, new StringBuffer());
			if (stringbuffer != null) {
				return new StringExpression(this.where, stringbuffer.toString());
			}
		}
		return super.inlineValue(environment, context);
	}

	StringBuffer inlineValueSB(final Environment environment, final Context context, StringBuffer stringbuffer) {
		if (this.type != Type.tString) {
			return super.inlineValueSB(environment, context, stringbuffer);
		}
		stringbuffer = this.left.inlineValueSB(environment, context, stringbuffer);
		if (stringbuffer != null) {
			stringbuffer = this.right.inlineValueSB(environment, context, stringbuffer);
		}
		return stringbuffer;
	}

	Expression simplify() {
		if (!this.type.isType(10)) {
			if (this.type.inMask(62)) {
				if (this.left.equals(0)) {
					return this.right;
				}
				if (this.right.equals(0)) {
					return this.left;
				}
			}
		} else if (this.right.type.isType(8)) {
			this.right = new StringExpression(((Node) this.right).where, "null");
		} else if (this.left.type.isType(8)) {
			this.left = new StringExpression(((Node) this.left).where, "null");
		}
		return this;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return (this.type.isType(10) ? 12 : 1) + this.left.costInline(i, environment, context) + this.right.costInline(i, environment, context);
	}

	void codeOperation(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 96 + this.type.getTypeCodeOffset());
	}

	void codeAppend(final Environment environment, final Context context, final Assembler assembler, final ClassDeclaration classdeclaration, final boolean flag) throws ClassNotFound, AmbiguousMember {
		if (this.type.isType(10)) {
			this.left.codeAppend(environment, context, assembler, classdeclaration, flag);
			this.right.codeAppend(environment, context, assembler, classdeclaration, false);
		} else {
			super.codeAppend(environment, context, assembler, classdeclaration, flag);
		}
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		if (this.type.isType(10)) {
			try {
				if (this.left.equals("")) {
					this.right.codeValue(environment, context, assembler);
					this.right.ensureString(environment, context, assembler);
					return;
				}
				if (this.right.equals("")) {
					this.left.codeValue(environment, context, assembler);
					this.left.ensureString(environment, context, assembler);
					return;
				}
				final ClassDeclaration classdeclaration = environment.getClassDeclaration(Constants.idJavaLangStringBuffer);
				final ClassDefinition classdefinition = context.field.getClassDefinition();
				this.codeAppend(environment, context, assembler, classdeclaration, true);
				final MemberDefinition memberdefinition = classdeclaration.getClassDefinition(environment).matchMethod(environment, classdefinition, Constants.idToString);
				assembler.add(this.where, 182, memberdefinition);
			} catch (final ClassNotFound classnotfound) {
				throw new CompilerError(classnotfound);
			} catch (final AmbiguousMember ambiguousmember) {
				throw new CompilerError(ambiguousmember);
			}
		} else {
			super.codeValue(environment, context, assembler);
		}
	}
}
