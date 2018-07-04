package sun.tools.javac;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import sun.tools.java.ClassDefinition;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.IdentifierToken;
import sun.tools.java.Imports;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Parser;
import sun.tools.java.Type;
import sun.tools.tree.Node;

public final class BatchParser extends Parser {

	BatchParser(final Environment environment, final InputStream inputstream) throws IOException {
		super(environment, inputstream);
		this.imports = new Imports();
		this.classes = new Vector();
		this.toplevelEnv = this.imports.newEnvironment(environment);
	}

	public void packageDeclaration(final long l, final IdentifierToken identifiertoken) {
		identifiertoken.getName();
		if (this.pkg == null) {
			this.pkg = identifiertoken.getName();
			this.imports.setCurrentPackage(identifiertoken);
		} else {
			this.env.error(l, "package.repeated");
		}
	}

	public void importClass(final long l, final IdentifierToken identifiertoken) {
		this.imports.addClass(identifiertoken);
	}

	public void importPackage(final long l, final IdentifierToken identifiertoken) {
		this.imports.addPackage(identifiertoken);
	}

	public ClassDefinition beginClass(final long l, final String s, int i, IdentifierToken identifiertoken, final IdentifierToken identifiertoken1, final IdentifierToken aidentifiertoken[]) {
		Environment.dtEnter("beginClass: " + this.sourceClass);
		final SourceClass sourceclass = this.sourceClass;
		if (sourceclass == null && this.pkg != null) {
			identifiertoken = new IdentifierToken(identifiertoken.getWhere(), Identifier.lookup(this.pkg, identifiertoken.getName()));
		}
		if ((i & 0x10000) != 0) {
			i |= 0x12;
		}
		if ((i & 0x20000) != 0) {
			i |= 2;
		}
		if ((i & 0x200) != 0) {
			i |= 0x400;
			if (sourceclass != null) {
				i |= 8;
			}
		}
		if (sourceclass != null && sourceclass.isInterface()) {
			if ((i & 6) == 0) {
				i |= 1;
			}
			i |= 8;
		}
		this.sourceClass = (SourceClass) this.toplevelEnv.makeClassDefinition(this.toplevelEnv, l, identifiertoken, s, i, identifiertoken1, aidentifiertoken, sourceclass);
		this.sourceClass.getClassDeclaration().setDefinition(this.sourceClass, 4);
		this.env = new Environment(this.toplevelEnv, this.sourceClass);
		Environment.dtEvent("beginClass: SETTING UP DEPENDENCIES");
		Environment.dtEvent("beginClass: ADDING TO CLASS LIST");
		this.classes.addElement(this.sourceClass);
		Environment.dtExit("beginClass: " + this.sourceClass);
		return this.sourceClass;
	}

	public ClassDefinition getCurrentClass() {
		return this.sourceClass;
	}

	public void endClass(final long l, final ClassDefinition classdefinition) {
		Environment.dtEnter("endClass: " + this.sourceClass);
		this.sourceClass.setEndPosition(l);
		this.sourceClass = (SourceClass) this.sourceClass.getOuterClass();
		this.env = this.toplevelEnv;
		if (this.sourceClass != null) {
			this.env = new Environment(this.env, this.sourceClass);
		}
		Environment.dtExit("endClass: " + this.sourceClass);
	}

	public void defineField(final long l, final ClassDefinition classdefinition, final String s, int i, Type type, final IdentifierToken identifiertoken, IdentifierToken aidentifiertoken[], IdentifierToken aidentifiertoken1[], final Node node) {
		final Identifier identifier = identifiertoken.getName();
		if (this.sourceClass.isInterface()) {
			if ((i & 6) == 0) {
				i |= 1;
			}
			i |= type.isType(12) ? 0x400 : 0x18;
		}
		if (identifier.equals(Constants.idInit)) {
			final Type type1 = type.getReturnType();
			final Identifier identifier1 = type1.isType(10) ? type1.getClassName() : Constants.idStar;
			final Identifier identifier2 = this.sourceClass.getLocalName();
			if (identifier2.equals(identifier1)) {
				type = Type.tMethod(Type.tVoid, type.getArgumentTypes());
			} else if (identifier2.equals(identifier1.getFlatName().getName())) {
				type = Type.tMethod(Type.tVoid, type.getArgumentTypes());
				this.env.error(l, "invalid.method.decl.qual");
			} else if (identifier1.isQualified() || identifier1.equals(Constants.idStar)) {
				this.env.error(l, "invalid.method.decl.name");
				return;
			} else {
				this.env.error(l, "invalid.method.decl");
				return;
			}
		}
		if (aidentifiertoken == null && type.isType(12)) {
			aidentifiertoken = new IdentifierToken[0];
		}
		if (aidentifiertoken1 == null && type.isType(12)) {
			aidentifiertoken1 = new IdentifierToken[0];
		}
		final MemberDefinition memberdefinition = this.env.makeMemberDefinition(this.env, l, this.sourceClass, s, i, type, identifier, aidentifiertoken, aidentifiertoken1, node);
		if (this.env.dump()) {
			memberdefinition.print(System.out);
		}
	}

	private Identifier pkg;
	Imports imports;
	Vector classes;
	private SourceClass sourceClass;
	private final Environment toplevelEnv;
}
