package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.CompilerError;
import sun.tools.java.Environment;
import sun.tools.java.MemberDefinition;

final class FieldUpdater {

	FieldUpdater(final long l, final MemberDefinition memberdefinition, final Expression expression, final MemberDefinition memberdefinition1, final MemberDefinition memberdefinition2) {
		this.where = l;
		this.field = memberdefinition;
		this.base = expression;
		this.getter = memberdefinition1;
		this.setter = memberdefinition2;
	}

	public FieldUpdater inline(final Environment environment, final Context context) {
		if (this.base != null) {
			this.base = this.field.isStatic() ? this.base.inline(environment, context) : this.base.inlineValue(environment, context);
		}
		return this;
	}

	public FieldUpdater copyInline(final Context context) {
		return new FieldUpdater(this.where, this.field, this.base.copyInline(context), this.getter, this.setter);
	}

	public int costInline(final int i, final Environment environment, final Context context, final boolean flag) {
		int j = flag ? 7 : 3;
		if (!this.field.isStatic() && this.base != null) {
			j += this.base.costInline(i, environment, context);
		}
		return j;
	}

	private void codeDup(final Assembler assembler, final int i, final int j) {
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

	public void startUpdate(final Environment environment, final Context context, final Assembler assembler, final boolean flag) {
		if (!this.getter.isStatic() || !this.setter.isStatic()) {
			throw new CompilerError("startUpdate isStatic");
		}
		if (!this.field.isStatic()) {
			this.base.codeValue(environment, context, assembler);
			this.depth = 1;
		} else {
			if (this.base != null) {
				this.base.code(environment, context, assembler);
			}
			this.depth = 0;
		}
		this.codeDup(assembler, this.depth, 0);
		assembler.add(this.where, 184, this.getter);
		if (flag) {
			this.codeDup(assembler, this.field.getType().stackSize(), this.depth);
		}
	}

	public void finishUpdate(final Environment environment, final Context context, final Assembler assembler, final boolean flag) {
		if (flag) {
			this.codeDup(assembler, this.field.getType().stackSize(), this.depth);
		}
		assembler.add(this.where, 184, this.setter);
	}

	public void startAssign(final Environment environment, final Context context, final Assembler assembler) {
		if (!this.setter.isStatic()) {
			throw new CompilerError("startAssign isStatic");
		}
		if (!this.field.isStatic()) {
			this.base.codeValue(environment, context, assembler);
			this.depth = 1;
		} else {
			if (this.base != null) {
				this.base.code(environment, context, assembler);
			}
			this.depth = 0;
		}
	}

	public void finishAssign(final Environment environment, final Context context, final Assembler assembler, final boolean flag) {
		if (flag) {
			this.codeDup(assembler, this.field.getType().stackSize(), this.depth);
		}
		assembler.add(this.where, 184, this.setter);
	}

	private final long where;
	private final MemberDefinition field;
	private Expression base;
	private final MemberDefinition getter;
	private final MemberDefinition setter;
	private int depth;
}
