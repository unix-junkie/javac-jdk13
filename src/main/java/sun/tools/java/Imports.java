package sun.tools.java;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

public final class Imports {

	public Imports() {
		this.currentPackage = Constants.idNull;
		this.classes = new Hashtable();
		this.packages = new Vector();
		this.singles = new Vector();
		this.addPackage(Constants.idJavaLang);
	}

	public synchronized void resolve(final Environment environment) {
		if (this.checked != 0) {
			return;
		}
		this.checked = -1;
		final Vector vector = new Vector();
		for (final Iterator iterator = this.packages.iterator(); iterator.hasNext();) {
			final IdentifierToken identifiertoken = (IdentifierToken) iterator.next();
			Identifier identifier = identifiertoken.getName();
			final long l = identifiertoken.getWhere();
			if (environment.isExemptPackage(identifier)) {
				vector.addElement(identifiertoken);
			} else {
				Identifier identifier2 = environment.resolvePackageQualifiedName(identifier);
				if (importable(identifier2, environment)) {
					if (environment.getPackage(identifier2.getTopName()).exists()) {
						environment.error(l, "class.and.package", identifier2.getTopName());
					}
					if (!identifier2.isInner()) {
						identifier2 = Identifier.lookupInner(identifier2, Constants.idNull);
					}
					identifier = identifier2;
				} else if (!environment.getPackage(identifier).exists()) {
					environment.error(l, "package.not.found", identifier, "import");
				} else if (identifier2.isInner()) {
					environment.error(l, "class.and.package", identifier2.getTopName());
				}
				vector.addElement(new IdentifierToken(l, identifier));
			}
		}

		this.packages = vector;
		for (final Iterator iterator = this.singles.iterator(); iterator.hasNext();) {
			final IdentifierToken identifiertoken1 = (IdentifierToken) iterator.next();
			Identifier identifier1 = identifiertoken1.getName();
			final long l1 = identifiertoken1.getWhere();
			identifier1.getQualifier();
			identifier1 = environment.resolvePackageQualifiedName(identifier1);
			if (!environment.classExists(identifier1.getTopName())) {
				environment.error(l1, "class.not.found", identifier1, "import");
			}
			final Identifier identifier4 = identifier1.getFlatName().getName();
			final Identifier identifier5 = (Identifier) this.classes.get(identifier4);
			if (identifier5 != null) {
				final Identifier identifier6 = Identifier.lookup(identifier5.getQualifier(), identifier5.getFlatName());
				final Identifier identifier7 = Identifier.lookup(identifier1.getQualifier(), identifier1.getFlatName());
				if (!identifier6.equals(identifier7)) {
					environment.error(l1, "ambig.class", identifier1, identifier5);
				}
			}
			this.classes.put(identifier4, identifier1);
			try {
				final ClassDeclaration classdeclaration = environment.getClassDeclaration(identifier1);
				ClassDefinition classdefinition = classdeclaration.getClassDefinitionNoCheck(environment);
				final Identifier identifier8 = classdefinition.getName().getQualifier();
				for (; classdefinition != null; classdefinition = classdefinition.getOuterClass()) {
					if (!classdefinition.isPrivate() && (classdefinition.isPublic() || identifier8.equals(this.currentPackage))) {
						continue;
					}
					environment.error(l1, "cant.access.class", classdefinition);
					break;
				}

			} catch (final AmbiguousClass ambiguousclass) {
				environment.error(l1, "ambig.class", ambiguousclass.name1, ambiguousclass.name2);
			} catch (final ClassNotFound classnotfound) {
				environment.error(l1, "class.not.found", classnotfound.name, "import");
			}
		}

		this.checked = 1;
	}

	public synchronized Identifier resolve(final Environment environment, Identifier identifier) throws ClassNotFound {
		Environment.dtEnter("Imports.resolve: " + identifier);
		if (identifier.hasAmbigPrefix()) {
			identifier = identifier.removeAmbigPrefix();
		}
		if (identifier.isQualified()) {
			Environment.dtExit("Imports.resolve: QUALIFIED " + identifier);
			return identifier;
		}
		if (this.checked <= 0) {
			this.checked = 0;
			this.resolve(environment);
		}
		Identifier identifier1 = (Identifier) this.classes.get(identifier);
		if (identifier1 != null) {
			Environment.dtExit("Imports.resolve: PREVIOUSLY IMPORTED " + identifier);
			return identifier1;
		}
		final Identifier identifier2 = Identifier.lookup(this.currentPackage, identifier);
		if (importable(identifier2, environment)) {
			identifier1 = identifier2;
		} else {
			for (final Iterator iterator = this.packages.iterator(); iterator.hasNext();) {
				final IdentifierToken identifiertoken = (IdentifierToken) iterator.next();
				final Identifier identifier3 = Identifier.lookup(identifiertoken.getName(), identifier);
				if (importable(identifier3, environment)) {
					if (identifier1 == null) {
						identifier1 = identifier3;
					} else {
						Environment.dtExit("Imports.resolve: AMBIGUOUS " + identifier);
						throw new AmbiguousClass(identifier1, identifier3);
					}
				}
			}

		}
		if (identifier1 == null) {
			Environment.dtExit("Imports.resolve: NOT FOUND " + identifier);
			throw new ClassNotFound(identifier);
		}
		this.classes.put(identifier, identifier1);
		Environment.dtExit("Imports.resolve: FIRST IMPORT " + identifier);
		return identifier1;
	}

	private static boolean importable(final Identifier identifier, final Environment environment) {
		if (!identifier.isInner()) {
			return environment.classExists(identifier);
		}
		if (!environment.classExists(identifier.getTopName())) {
			return false;
		}
		try {
			final ClassDeclaration classdeclaration = environment.getClassDeclaration(identifier.getTopName());
			final ClassDefinition classdefinition = classdeclaration.getClassDefinitionNoCheck(environment);
			return classdefinition.innerClassExists(identifier.getFlatName().getTail());
		} catch (final ClassNotFound ignored) {
			return false;
		}
	}

	public synchronized Identifier forceResolve(final Environment environment, final Identifier identifier) {
		if (identifier.isQualified()) {
			return identifier;
		}
		final Identifier identifier1 = (Identifier) this.classes.get(identifier);
		if (identifier1 != null) {
			return identifier1;
		}
		final Identifier identifier2 = Identifier.lookup(this.currentPackage, identifier);
		this.classes.put(identifier, identifier2);
		return identifier2;
	}

	public synchronized void addClass(final IdentifierToken identifiertoken) {
		this.singles.addElement(identifiertoken);
	}

	public void addClass(final Identifier identifier) {
		this.addClass(new IdentifierToken(identifier));
	}

	public synchronized void addPackage(final IdentifierToken identifiertoken) {
		final Identifier identifier = identifiertoken.getName();
		if (identifier == this.currentPackage) {
			return;
		}
		final int i = this.packages.size();
		for (int j = 0; j < i; j++) {
			if (identifier == ((IdentifierToken) this.packages.elementAt(j)).getName()) {
				return;
			}
		}

		this.packages.addElement(identifiertoken);
	}

	private void addPackage(final Identifier identifier) {
		this.addPackage(new IdentifierToken(identifier));
	}

	public synchronized void setCurrentPackage(final IdentifierToken identifiertoken) {
		this.currentPackage = identifiertoken.getName();
	}

	public Identifier getCurrentPackage() {
		return this.currentPackage;
	}

	public Environment newEnvironment(final Environment environment) {
		return new ImportEnvironment(environment, this);
	}

	private Identifier currentPackage;
	private final Hashtable classes;
	private Vector packages;
	private final Vector singles;
	private int checked;
}
