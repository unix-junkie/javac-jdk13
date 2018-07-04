package sun.tools.java;

final class ImportEnvironment extends Environment {

	ImportEnvironment(final Environment environment, final Imports imports1) {
		super(environment, environment.getSource());
		this.imports = imports1;
	}

	public Identifier resolve(final Identifier identifier) throws ClassNotFound {
		return this.imports.resolve(this, identifier);
	}

	public Imports getImports() {
		return this.imports;
	}

	private final Imports imports;
}
