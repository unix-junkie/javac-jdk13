package sun.tools.java;

public final class ClassDeclaration {

	public ClassDeclaration(final Identifier identifier) {
		this.found = false;
		this.type = Type.tClass(identifier);
	}

	public int getStatus() {
		return this.status;
	}

	public Identifier getName() {
		return this.type.getClassName();
	}

	public Type getType() {
		return this.type;
	}

	public boolean isDefined() {
		switch (this.status) {
		case 2: // '\002'
		case 4: // '\004'
		case 5: // '\005'
		case 6: // '\006'
			return true;

		case 3: // '\003'
		default:
			return false;
		}
	}

	public ClassDefinition getClassDefinition() {
		return this.definition;
	}

	public ClassDefinition getClassDefinition(final Environment environment) throws ClassNotFound {
		Environment.dtEvent("getClassDefinition: " + this.getName() + ", status " + this.getStatus());
		if (this.found) {
			return this.definition;
		}
		do {
			switch (this.status) {
			case 0: // '\0'
			case 1: // '\001'
			case 3: // '\003'
				environment.loadDefinition(this);
				break;

			case 2: // '\002'
			case 4: // '\004'
				if (!this.definition.isInsideLocal()) {
					this.definition.basicCheck(environment);
				}
				this.found = true;
				return this.definition;

			case 5: // '\005'
			case 6: // '\006'
				this.found = true;
				return this.definition;

			default:
				throw new ClassNotFound(this.getName());
			}
		} while (true);
	}

	public ClassDefinition getClassDefinitionNoCheck(final Environment environment) throws ClassNotFound {
		Environment.dtEvent("getClassDefinition: " + this.getName() + ", status " + this.getStatus());
		do {
			switch (this.status) {
			case 0: // '\0'
			case 1: // '\001'
			case 3: // '\003'
				environment.loadDefinition(this);
				break;

			case 2: // '\002'
			case 4: // '\004'
			case 5: // '\005'
			case 6: // '\006'
				return this.definition;

			default:
				throw new ClassNotFound(this.getName());
			}
		} while (true);
	}

	public void setDefinition(final ClassDefinition classdefinition, final int i) {
		if (classdefinition != null && !this.getName().equals(classdefinition.getName())) {
			throw new CompilerError("setDefinition: name mismatch: " + this + ", " + classdefinition);
		}
		this.definition = classdefinition;
		this.status = i;
	}

	public boolean equals(final Object obj) {
		return obj instanceof ClassDeclaration && this.type.equals(((ClassDeclaration) obj).type);
	}

	public String toString() {
		String s = this.getName().toString();
		String s1 = "type ";
		String s2 = this.getName().isInner() ? "nested " : "";
		if (this.getClassDefinition() != null) {
			s1 = this.getClassDefinition().isInterface() ? "interface " : "class ";
			if (!this.getClassDefinition().isTopLevel()) {
				s2 = "inner ";
				if (this.getClassDefinition().isLocal()) {
					s2 = "local ";
					if (!this.getClassDefinition().isAnonymous()) {
						s = this.getClassDefinition().getLocalName() + " (" + s + ')';
					}
				}
			}
		}
		return s2 + s1 + s;
	}

	private int status;
	private final Type type;
	private ClassDefinition definition;
	private boolean found;
}
