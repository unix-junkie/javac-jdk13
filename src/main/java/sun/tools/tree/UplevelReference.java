package sun.tools.tree;

import sun.tools.asm.Assembler;
import sun.tools.java.ClassDefinition;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.MemberDefinition;

public final class UplevelReference {

	public UplevelReference(final ClassDefinition classdefinition, final LocalMember localmember) {
		this.client = classdefinition;
		this.target = localmember;
		Identifier identifier;
		if (localmember.getName().equals(Constants.idThis)) {
			final ClassDefinition classdefinition1 = localmember.getClassDefinition();
			int i = 0;
			for (ClassDefinition classdefinition2 = classdefinition1; !classdefinition2.isTopLevel(); classdefinition2 = classdefinition2.getOuterClass()) {
				i++;
			}

			identifier = Identifier.lookup("this$" + i);
		} else {
			identifier = Identifier.lookup("val$" + localmember.getName());
		}
		final Identifier identifier1 = identifier;
		int j = 0;
		do {
			boolean flag = classdefinition.getFirstMatch(identifier) != null;
			for (UplevelReference uplevelreference = classdefinition.getReferences(); uplevelreference != null; uplevelreference = uplevelreference.next) {
				if (uplevelreference.target.getName().equals(identifier)) {
					flag = true;
				}
			}

			if (flag) {
				identifier = Identifier.lookup(identifier1 + "$" + ++j);
			} else {
				this.localArgument = new LocalMember(localmember.getWhere(), classdefinition, 0x80010, localmember.getType(), identifier);
				return;
			}
		} while (true);
	}

	public UplevelReference insertInto(final UplevelReference uplevelreference) {
		if (uplevelreference == null || this.isEarlierThan(uplevelreference)) {
			this.next = uplevelreference;
			return this;
		}
		UplevelReference uplevelreference1;
		for (uplevelreference1 = uplevelreference; uplevelreference1.next != null && !this.isEarlierThan(uplevelreference1.next); uplevelreference1 = uplevelreference1.next) {
		}
		this.next = uplevelreference1.next;
		uplevelreference1.next = this;
		return uplevelreference;
	}

	private boolean isEarlierThan(final UplevelReference uplevelreference) {
		if (this.isClientOuterField()) {
			return true;
		}
		if (uplevelreference.isClientOuterField()) {
			return false;
		}
		final LocalMember localmember = uplevelreference.target;
		final Identifier identifier = this.target.getName();
		final Identifier identifier1 = localmember.getName();
		final int i = identifier.toString().compareTo(identifier1.toString());
		if (i != 0) {
			return i < 0;
		}
		final Identifier identifier2 = this.target.getClassDefinition().getName();
		final Identifier identifier3 = localmember.getClassDefinition().getName();
		final int j = identifier2.toString().compareTo(identifier3.toString());
		return j < 0;
	}

	public LocalMember getTarget() {
		return this.target;
	}

	public LocalMember getLocalArgument() {
		return this.localArgument;
	}

	public MemberDefinition getLocalField() {
		return this.localField;
	}

	public MemberDefinition getLocalField(final Environment environment) {
		if (this.localField == null) {
			this.makeLocalField(environment);
		}
		return this.localField;
	}

	public ClassDefinition getClient() {
		return this.client;
	}

	public UplevelReference getNext() {
		return this.next;
	}

	public boolean isClientOuterField() {
		final MemberDefinition memberdefinition = this.client.findOuterMember();
		return memberdefinition != null && this.localField == memberdefinition;
	}

	private boolean localArgumentAvailable(final Environment environment, final Context context) {
		final MemberDefinition memberdefinition = context.field;
		if (memberdefinition.getClassDefinition() != this.client) {
			throw new CompilerError("localArgumentAvailable");
		}
		return memberdefinition.isConstructor() || memberdefinition.isVariable() || memberdefinition.isInitializer();
	}

	public void noteReference(final Environment environment, final Context context) {
		if (this.localField == null && !this.localArgumentAvailable(environment, context)) {
			this.makeLocalField(environment);
		}
	}

	private void makeLocalField(final Environment environment) {
		this.client.referencesMustNotBeFrozen();
		final int i = 0x80012;
		this.localField = environment.makeMemberDefinition(environment, this.localArgument.getWhere(), this.client, null, i, this.localArgument.getType(), this.localArgument.getName(), null, null, null);
	}

	public Expression makeLocalReference(final Environment environment, final Context context) {
		if (context.field.getClassDefinition() != this.client) {
			throw new CompilerError("makeLocalReference");
		}
		return this.localArgumentAvailable(environment, context) ? new IdentifierExpression(0L, this.localArgument) : this.makeFieldReference(environment, context);
	}

	private Expression makeFieldReference(final Environment environment, final Context context) {
		final Expression expression = context.findOuterLink(environment, 0L, this.localField);
		return new FieldExpression(0L, expression, this.localField);
	}

	public void willCodeArguments(final Environment environment, final Context context) {
		if (!this.isClientOuterField()) {
			context.noteReference(environment, this.target);
		}
		if (this.next != null) {
			this.next.willCodeArguments(environment, context);
		}
	}

	public void codeArguments(final Environment environment, final Context context, final Assembler assembler, final long l, final MemberDefinition memberdefinition) {
		if (!this.isClientOuterField()) {
			final Expression expression = context.makeReference(environment, this.target);
			expression.codeValue(environment, context, assembler);
		}
		if (this.next != null) {
			this.next.codeArguments(environment, context, assembler, l, memberdefinition);
		}
	}

	public void codeInitialization(final Environment environment, final Context context, final Assembler assembler, final long l, final MemberDefinition memberdefinition) {
		if (this.localField != null && !this.isClientOuterField()) {
			Expression obj = context.makeReference(environment, this.target);
			final Expression expression = this.makeFieldReference(environment, context);
			obj = new AssignExpression(((Node) obj).getWhere(), expression, obj);
			obj.type = this.localField.getType();
			obj.code(environment, context, assembler);
		}
		if (this.next != null) {
			this.next.codeInitialization(environment, context, assembler, l, memberdefinition);
		}
	}

	public String toString() {
		return "[" + this.localArgument + " in " + this.client + ']';
	}

	private final ClassDefinition client;
	private final LocalMember target;
	private final LocalMember localArgument;
	private MemberDefinition localField;
	private UplevelReference next;
}
