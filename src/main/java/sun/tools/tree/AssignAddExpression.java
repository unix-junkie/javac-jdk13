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

public final class AssignAddExpression extends AssignOpExpression {

	public AssignAddExpression(final long l, final Expression expression, final Expression expression1) {
		super(5, l, expression, expression1);
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		return this.type.isType(10) ? 25 : super.costInline(i, environment, context);
	}

	void code(final Environment environment, final Context context, final Assembler assembler, final boolean flag) {
		if (this.itype.isType(10)) {
			try {
				final Type atype[] = { Type.tString };
				final ClassDeclaration classdeclaration = environment.getClassDeclaration(Constants.idJavaLangStringBuffer);
				if (this.updater == null) {
					assembler.add(this.where, 187, classdeclaration);
					assembler.add(this.where, 89);
					final int i = this.left.codeLValue(environment, context, assembler);
					this.codeDup(environment, context, assembler, i, 2);
					this.left.codeLoad(environment, context, assembler);
					this.left.ensureString(environment, context, assembler);
					final ClassDefinition classdefinition1 = context.field.getClassDefinition();
					MemberDefinition memberdefinition1 = classdeclaration.getClassDefinition(environment).matchMethod(environment, classdefinition1, Constants.idInit, atype);
					assembler.add(this.where, 183, memberdefinition1);
					this.right.codeAppend(environment, context, assembler, classdeclaration, false);
					memberdefinition1 = classdeclaration.getClassDefinition(environment).matchMethod(environment, classdefinition1, Constants.idToString);
					assembler.add(this.where, 182, memberdefinition1);
					if (flag) {
						this.codeDup(environment, context, assembler, Type.tString.stackSize(), i);
					}
					this.left.codeStore(environment, context, assembler);
				} else {
					this.updater.startUpdate(environment, context, assembler, false);
					this.left.ensureString(environment, context, assembler);
					assembler.add(this.where, 187, classdeclaration);
					assembler.add(this.where, 90);
					assembler.add(this.where, 95);
					final ClassDefinition classdefinition = context.field.getClassDefinition();
					MemberDefinition memberdefinition = classdeclaration.getClassDefinition(environment).matchMethod(environment, classdefinition, Constants.idInit, atype);
					assembler.add(this.where, 183, memberdefinition);
					this.right.codeAppend(environment, context, assembler, classdeclaration, false);
					memberdefinition = classdeclaration.getClassDefinition(environment).matchMethod(environment, classdefinition, Constants.idToString);
					assembler.add(this.where, 182, memberdefinition);
					this.updater.finishUpdate(environment, context, assembler, flag);
				}
			} catch (final ClassNotFound classnotfound) {
				throw new CompilerError(classnotfound);
			} catch (final AmbiguousMember ambiguousmember) {
				throw new CompilerError(ambiguousmember);
			}
		} else {
			super.code(environment, context, assembler, flag);
		}
	}

	void codeOperation(final Environment environment, final Context context, final Assembler assembler) {
		assembler.add(this.where, 96 + this.itype.getTypeCodeOffset());
	}
}
