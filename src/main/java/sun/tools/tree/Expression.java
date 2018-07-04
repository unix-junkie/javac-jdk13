package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.Instruction;
import sun.tools.asm.Label;
import sun.tools.java.AmbiguousMember;
import sun.tools.java.ClassDeclaration;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassNotFound;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Type;

public class Expression extends Node {

	Expression(final int op, final long where, final Type type1) {
		super(op, where);
		this.type = type1;
	}

	public Type getType() {
		return this.type;
	}

	int precedence() {
		return this.op >= Constants.opPrecedence.length ? 100 : Constants.opPrecedence[this.op];
	}

	public Expression order() {
		return this;
	}

	public boolean isConstant() {
		return false;
	}

	public Object getValue() {
		return null;
	}

	boolean equals(final int i) {
		return false;
	}

	public boolean equals(final boolean flag) {
		return false;
	}

	public boolean equals(final Identifier identifier) {
		return false;
	}

	boolean equals(final String s) {
		return false;
	}

	boolean isNull() {
		return false;
	}

	boolean isNonNull() {
		return false;
	}

	boolean equalsDefault() {
		return false;
	}

	Type toType(final Environment environment, final Context context) {
		environment.error(this.where, "invalid.type.expr");
		return Type.tError;
	}

	public boolean fitsType(final Environment environment, final Context context, final Type type1) {
		try {
			if (environment.isMoreSpecific(this.type, type1)) {
				return true;
			}
			if (this.type.isType(4) && this.isConstant() && context != null) {
				final Expression expression = this.inlineValue(environment, context);
				if (expression != this && expression instanceof ConstantExpression) {
					return expression.fitsType(environment, context, type1);
				}
			}
			return false;
		} catch (final ClassNotFound ignored) {
			return false;
		}
	}

	public Vset checkValue(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		return vset;
	}

	public Vset checkInitializer(final Environment environment, final Context context, final Vset vset, final Type type1, final Hashtable hashtable) {
		return this.checkValue(environment, context, vset, hashtable);
	}

	public Vset check(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		throw new CompilerError("check failed");
	}

	Vset checkLHS(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		environment.error(this.where, "invalid.lhs.assignment");
		this.type = Type.tError;
		return vset;
	}

	FieldUpdater getAssigner(final Environment environment, final Context context) {
		throw new CompilerError("getAssigner lhs");
	}

	FieldUpdater getUpdater(final Environment environment, final Context context) {
		throw new CompilerError("getUpdater lhs");
	}

	Vset checkAssignOp(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final Expression expression) {
		if (expression instanceof IncDecExpression) {
			environment.error(this.where, "invalid.arg", Constants.opNames[((Node) expression).op]);
		} else {
			environment.error(this.where, "invalid.lhs.assignment");
		}
		this.type = Type.tError;
		return vset;
	}

	Vset checkAmbigName(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final UnaryExpression unaryexpression) {
		return this.checkValue(environment, context, vset, hashtable);
	}

	public ConditionVars checkCondition(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable) {
		final ConditionVars conditionvars = new ConditionVars();
		this.checkCondition(environment, context, vset, hashtable, conditionvars);
		return conditionvars;
	}

	void checkCondition(final Environment environment, final Context context, final Vset vset, final Hashtable hashtable, final ConditionVars conditionvars) {
		conditionvars.vsTrue = conditionvars.vsFalse = this.checkValue(environment, context, vset, hashtable);
		conditionvars.vsFalse = conditionvars.vsFalse.copy();
	}

	Expression eval() {
		return this;
	}

	Expression simplify() {
		return this;
	}

	public Expression inline(final Environment environment, final Context context) {
		return null;
	}

	public Expression inlineValue(final Environment environment, final Context context) {
		return this;
	}

	StringBuffer inlineValueSB(final Environment environment, final Context context, final StringBuffer stringbuffer) {
		final Expression expression = this.inlineValue(environment, context);
		final Object obj = expression.getValue();
		if (obj == null && !expression.isNull()) {
			return null;
		}
		if (this.type == Type.tChar) {
			stringbuffer.append((char) ((Number) obj).intValue());
		} else if (this.type == Type.tBoolean) {
			stringbuffer.append(((Number) obj).intValue() != 0);
		} else {
			stringbuffer.append(obj);
		}
		return stringbuffer;
	}

	Expression inlineLHS(final Environment environment, final Context context) {
		return null;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return 1;
	}

	void codeBranch(final Environment environment, final Context context, final Assembler assembler, final Label label, final boolean flag) {
		if (this.type.isType(0)) {
			this.codeValue(environment, context, assembler);
			assembler.add(this.where, flag ? 154 : 153, label, flag);
		} else {
			throw new CompilerError("codeBranch " + Constants.opNames[this.op]);
		}
	}

	public void codeValue(final Environment environment, final Context context, final Assembler assembler) {
		if (this.type.isType(0)) {
			final Label label = new Label();
			final Instruction label1 = new Label();
			this.codeBranch(environment, context, assembler, label, true);
			assembler.add(true, this.where, 18, new Integer(0));
			assembler.add(true, this.where, 167, label1);
			assembler.add(label);
			assembler.add(true, this.where, 18, new Integer(1));
			assembler.add(label1);
		} else {
			throw new CompilerError("codeValue");
		}
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		this.codeValue(environment, context, assembler);
		switch (this.type.getTypeCode()) {
		case 5: // '\005'
		case 7: // '\007'
			assembler.add(this.where, 88);
			break;

		default:
			assembler.add(this.where, 87);
			break;

		case 11: // '\013'
			break;
		}
	}

	int codeLValue(final Environment environment, final Context context, final Assembler assembler) {
		this.print(System.out);
		throw new CompilerError("invalid lhs");
	}

	void codeLoad(final Environment environment, final Context context, final Assembler assembler) {
		this.print(System.out);
		throw new CompilerError("invalid load");
	}

	void codeStore(final Environment environment, final Context context, final Assembler assembler) {
		this.print(System.out);
		throw new CompilerError("invalid store");
	}

	void ensureString(final Environment environment, final Context context, final Assembler assembler) throws ClassNotFound, AmbiguousMember {
		if (this.type == Type.tString && this.isNonNull()) {
			return;
		}
		final ClassDefinition classdefinition = context.field.getClassDefinition();
		final ClassDeclaration classdeclaration = environment.getClassDeclaration(Type.tString);
		final ClassDefinition classdefinition1 = classdeclaration.getClassDefinition(environment);
		if (this.type.inMask(1792)) {
			if (this.type != Type.tString) {
				final Type atype[] = { Type.tObject };
				final MemberDefinition memberdefinition = classdefinition1.matchMethod(environment, classdefinition, Constants.idValueOf, atype);
				assembler.add(this.where, 184, memberdefinition);
			}
			if (!this.type.inMask(768)) {
				final Type atype1[] = { Type.tString };
				final MemberDefinition memberdefinition1 = classdefinition1.matchMethod(environment, classdefinition, Constants.idValueOf, atype1);
				assembler.add(this.where, 184, memberdefinition1);
			}
		} else {
			final Type atype2[] = { this.type };
			final MemberDefinition memberdefinition2 = classdefinition1.matchMethod(environment, classdefinition, Constants.idValueOf, atype2);
			assembler.add(this.where, 184, memberdefinition2);
		}
	}

	void codeAppend(final Environment environment, final Context context, final Assembler assembler, final ClassDeclaration classdeclaration, final boolean flag) throws ClassNotFound, AmbiguousMember {
		final ClassDefinition classdefinition = context.field.getClassDefinition();
		final ClassDefinition classdefinition1 = classdeclaration.getClassDefinition(environment);
		if (flag) {
			assembler.add(this.where, 187, classdeclaration);
			assembler.add(this.where, 89);
			final MemberDefinition memberdefinition;
			if (this.equals("")) {
				memberdefinition = classdefinition1.matchMethod(environment, classdefinition, Constants.idInit);
			} else {
				this.codeValue(environment, context, assembler);
				this.ensureString(environment, context, assembler);
				final Type atype[] = { Type.tString };
				memberdefinition = classdefinition1.matchMethod(environment, classdefinition, Constants.idInit, atype);
			}
			assembler.add(this.where, 183, memberdefinition);
		} else {
			this.codeValue(environment, context, assembler);
			final Type atype1[] = { !this.type.inMask(1792) || this.type == Type.tString ? this.type : Type.tObject };
			final MemberDefinition memberdefinition1 = classdefinition1.matchMethod(environment, classdefinition, Constants.idAppend, atype1);
			assembler.add(this.where, 182, memberdefinition1);
		}
	}

	void codeDup(final Environment environment, final Context context, final Assembler assembler, final int i, final int j) {
		switch (i) {
		default:
			break;

		case 0: // '\0'
			return;

		case 1: // '\001'
			switch (j) {
			case 0: // '\0'
				assembler.add(this.where, 89);
				return;

			case 1: // '\001'
				assembler.add(this.where, 90);
				return;

			case 2: // '\002'
				assembler.add(this.where, 91);
				return;
			}
			break;

		case 2: // '\002'
			switch (j) {
			case 0: // '\0'
				assembler.add(this.where, 92);
				return;

			case 1: // '\001'
				assembler.add(this.where, 93);
				return;

			case 2: // '\002'
				assembler.add(this.where, 94);
				return;
			}
			break;
		}
		throw new CompilerError("can't dup: " + i + ", " + j);
	}

	void codeConversion(final Environment environment, final Context context, final Assembler assembler, final Type type1, final Type type2) {
		final int i = type1.getTypeCode();
		final int j = type2.getTypeCode();
		label0: switch (j) {
		case 8: // '\b'
		default:
			break;

		case 0: // '\0'
			if (i == 0) {
				return;
			}
			break;

		case 1: // '\001'
			if (i != 1) {
				this.codeConversion(environment, context, assembler, type1, Type.tInt);
				assembler.add(this.where, 145);
			}
			return;

		case 2: // '\002'
			if (i != 2) {
				this.codeConversion(environment, context, assembler, type1, Type.tInt);
				assembler.add(this.where, 146);
			}
			return;

		case 3: // '\003'
			if (i != 3) {
				this.codeConversion(environment, context, assembler, type1, Type.tInt);
				assembler.add(this.where, 147);
			}
			return;

		case 4: // '\004'
			switch (i) {
			case 1: // '\001'
			case 2: // '\002'
			case 3: // '\003'
			case 4: // '\004'
				return;

			case 5: // '\005'
				assembler.add(this.where, 136);
				return;

			case 6: // '\006'
				assembler.add(this.where, 139);
				return;

			case 7: // '\007'
				assembler.add(this.where, 142);
				return;
			}
			break;

		case 5: // '\005'
			switch (i) {
			case 1: // '\001'
			case 2: // '\002'
			case 3: // '\003'
			case 4: // '\004'
				assembler.add(this.where, 133);
				return;

			case 5: // '\005'
				return;

			case 6: // '\006'
				assembler.add(this.where, 140);
				return;

			case 7: // '\007'
				assembler.add(this.where, 143);
				return;
			}
			break;

		case 6: // '\006'
			switch (i) {
			case 1: // '\001'
			case 2: // '\002'
			case 3: // '\003'
			case 4: // '\004'
				assembler.add(this.where, 134);
				return;

			case 5: // '\005'
				assembler.add(this.where, 137);
				return;

			case 6: // '\006'
				return;

			case 7: // '\007'
				assembler.add(this.where, 144);
				return;
			}
			break;

		case 7: // '\007'
			switch (i) {
			case 1: // '\001'
			case 2: // '\002'
			case 3: // '\003'
			case 4: // '\004'
				assembler.add(this.where, 135);
				return;

			case 5: // '\005'
				assembler.add(this.where, 138);
				return;

			case 6: // '\006'
				assembler.add(this.where, 141);
				return;

			case 7: // '\007'
				return;
			}
			break;

		case 10: // '\n'
			switch (i) {
			default:
				break label0;

			case 8: // '\b'
				return;

			case 9: // '\t'
			case 10: // '\n'
				try {
					if (!environment.implicitCast(type1, type2)) {
						assembler.add(this.where, 192, environment.getClassDeclaration(type2));
					}
				} catch (final ClassNotFound classnotfound) {
					throw new CompilerError(classnotfound);
				}
				return;
			}

		case 9: // '\t'
			switch (i) {
			default:
				break label0;

			case 8: // '\b'
				return;

			case 9: // '\t'
			case 10: // '\n'
				try {
					if (!environment.implicitCast(type1, type2)) {
						assembler.add(this.where, 192, type2);
					}
					return;
				} catch (final ClassNotFound classnotfound1) {
					throw new CompilerError(classnotfound1);
				}
			}
		}
		throw new CompilerError("codeConversion: " + i + ", " + j);
	}

	public Expression firstConstructor() {
		return null;
	}

	public Expression copyInline(final Context context) {
		return (Expression) this.clone();
	}

	public void print(final PrintStream printstream) {
		printstream.print(Constants.opNames[this.op]);
	}

	Type type;
}
