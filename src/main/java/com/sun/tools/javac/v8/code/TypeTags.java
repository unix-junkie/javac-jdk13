package com.sun.tools.javac.v8.code;

public interface TypeTags {

	int BYTE = 1;
	int CHAR = 2;
	int SHORT = 3;
	int INT = 4;
	int LONG = 5;
	int FLOAT = 6;
	int DOUBLE = 7;
	int BOOLEAN = 8;
	int VOID = 9;
	int CLASS = 10;
	int ARRAY = 11;
	int METHOD = 12;
	int PACKAGE = 13;
	int TYPEVAR = 14;
	int FORALL = 15;
	int BOT = 16;
	int NONE = 17;
	int ERROR = 18;
	int TypeTagCount = 19;
	int lastBaseTag = 8;
}
