package sun.tools.tree;

import java.util.List;

import sun.tools.java.ClassDefinition;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Type;

public final class LocalMember extends MemberDefinition {

	public int getScopeNumber() {
		return this.scopeNumber;
	}

	public LocalMember(final long l, final ClassDefinition classdefinition, final int i, final Type type, final Identifier identifier) {
		super(l, classdefinition, i, type, identifier, null, null);
		this.number = -1;
	}

	public LocalMember(final ClassDefinition classdefinition) {
		super(classdefinition);
		this.number = -1;
		this.name = classdefinition.getLocalName();
	}

	LocalMember(final MemberDefinition memberdefinition) {
		this(0L, null, 0, memberdefinition.getType(), Constants.idClass);
		this.accessPeer = memberdefinition;
	}

	MemberDefinition getMember() {
		return this.name != Constants.idClass ? null : this.accessPeer;
	}

	public boolean isLocal() {
		return true;
	}

	public LocalMember copyInline(final Context context) {
		final LocalMember localmember = new LocalMember(this.where, this.clazz, this.modifiers, this.type, this.name);
		localmember.readcount = this.readcount;
		localmember.writecount = this.writecount;
		localmember.originalOfCopy = this;
		localmember.addModifiers(0x20000);
		if (this.accessPeer != null && (this.accessPeer.getModifiers() & 0x20000) == 0) {
			throw new CompilerError("local copyInline");
		}
		this.accessPeer = localmember;
		return localmember;
	}

	LocalMember getCurrentInlineCopy(final Context context) {
		final MemberDefinition memberdefinition = this.accessPeer;
		if (memberdefinition != null && (memberdefinition.getModifiers() & 0x20000) != 0) {
			return (LocalMember) memberdefinition;
		}
		return this;
	}

	static LocalMember[] copyArguments(final Context context, final MemberDefinition memberdefinition) {
		final List arguments = memberdefinition.getArguments();
		final LocalMember alocalmember[] = (LocalMember[]) arguments.toArray(new LocalMember[0]);
		for (int i = 0; i < alocalmember.length; i++) {
			alocalmember[i] = alocalmember[i].copyInline(context);
		}

		return alocalmember;
	}

	static void doneWithArguments(final Context context, final LocalMember alocalmember[]) {
		for (int i = 0; i < alocalmember.length; i++) {
			if (((MemberDefinition) alocalmember[i].originalOfCopy).accessPeer == alocalmember[i]) {
				alocalmember[i].originalOfCopy.accessPeer = null;
			}
		}

	}

	public boolean isInlineable(final Environment environment, final boolean flag) {
		return (this.getModifiers() & 0x100000) != 0;
	}

	boolean isUsed() {
		return this.readcount != 0 || this.writecount != 0;
	}

	public Node getValue(final Environment environment) {
		return this.getValue();
	}

	int number;
	int readcount;
	int writecount;
	int scopeNumber;
	private LocalMember originalOfCopy;
	LocalMember prev;
}
