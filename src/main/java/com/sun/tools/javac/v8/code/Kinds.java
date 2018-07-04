package com.sun.tools.javac.v8.code;

public interface Kinds {

	int NIL = 0;
	int PCK = 1;
	int TYP = 2;
	int VAR = 4;
	int VAL = 12;
	int MTH = 16;
	int ERR = 31;
	int AllKinds = 31;
}
