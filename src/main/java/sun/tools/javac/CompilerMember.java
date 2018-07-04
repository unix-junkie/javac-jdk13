package sun.tools.javac;

import sun.tools.asm.Assembler;
import sun.tools.java.MemberDefinition;

final class CompilerMember implements Comparable {

	CompilerMember(final MemberDefinition memberdefinition, final Assembler assembler) {
		this.field = memberdefinition;
		this.asm = assembler;
		this.name = memberdefinition.getName().toString();
		this.sig = memberdefinition.getType().getTypeSignature();
	}

	public int compareTo(final Object obj) {
		final CompilerMember compilermember = (CompilerMember) obj;
		return this.getKey().compareTo(compilermember.getKey());
	}

	private String getKey() {
		if (this.key == null) {
			this.key = this.name + this.sig;
		}
		return this.key;
	}

	final MemberDefinition field;
	final Assembler asm;
	final String name;
	final String sig;
	private String key;
}
