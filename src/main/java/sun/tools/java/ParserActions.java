package sun.tools.java;

import sun.tools.tree.Node;

interface ParserActions {

	void packageDeclaration(long l, IdentifierToken identifiertoken);

	void importClass(long l, IdentifierToken identifiertoken);

	void importPackage(long l, IdentifierToken identifiertoken);

	ClassDefinition beginClass(long l, String s, int i, IdentifierToken identifiertoken, IdentifierToken identifiertoken1, IdentifierToken aidentifiertoken[]);

	void endClass(long l, ClassDefinition classdefinition);

	void defineField(long l, ClassDefinition classdefinition, String s, int i, Type type, IdentifierToken identifiertoken, IdentifierToken aidentifiertoken[], IdentifierToken aidentifiertoken1[], Node node);
}
