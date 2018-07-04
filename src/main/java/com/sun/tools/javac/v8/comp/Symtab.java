package com.sun.tools.javac.v8.comp;

import com.sun.tools.javac.v8.code.ClassReader;
import com.sun.tools.javac.v8.code.ClassWriter;
import com.sun.tools.javac.v8.code.Scope;
import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.MethodSymbol;
import com.sun.tools.javac.v8.code.Symbol.OperatorSymbol;
import com.sun.tools.javac.v8.code.Symbol.VarSymbol;
import com.sun.tools.javac.v8.code.Type;
import com.sun.tools.javac.v8.code.Type.ArrayType;
import com.sun.tools.javac.v8.code.Type.ClassType;
import com.sun.tools.javac.v8.code.Type.MethodType;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;

public final class Symtab {

	private VarSymbol enterConstant(final String s, final Type type) {
		final VarSymbol varsymbol = new VarSymbol(25, Name.fromString(s), type, this.predefClass);
		varsymbol.constValue = type.constValue;
		this.predefClass.members().enter(varsymbol);
		return varsymbol;
	}

	private void enterBinop(final String s, final Type type, final Type type1, final Type type2, final int i) {
		this.predefClass.members().enter(new OperatorSymbol(Name.fromString(s), new MethodType(List.make(type, type1), type2, ClassSymbol.emptyList), i, this.predefClass));
	}

	private void enterBinop(final String s, final Type type, final Type type1, final Type type2, final int i, final int j) {
		this.enterBinop(s, type, type1, type2, i << 9 | j);
	}

	private void enterUnop(final String s, final Type type, final Type type1, final int i) {
		this.predefClass.members().enter(new OperatorSymbol(Name.fromString(s), new MethodType(List.make(type), type1, ClassSymbol.emptyList), i, this.predefClass));
	}

	private Type enterClass(final String s) {
		return ((Symbol) this.reader.enterClass(Name.fromString(s))).type;
	}

	public Symtab(final ClassReader reader, final ClassWriter writer) {
		this.predefClass = new ClassSymbol(1, Names.empty, Symbol.rootPackage);
		final Scope scope = new Scope(this.predefClass);
		this.predefClass.members_field = scope;
		this.reader = reader;
		this.writer = writer;
		reader.classes.put(this.predefClass.fullname, this.predefClass);
		scope.enter(Type.byteType.tsym);
		scope.enter(Type.shortType.tsym);
		scope.enter(Type.charType.tsym);
		scope.enter(Type.intType.tsym);
		scope.enter(Type.longType.tsym);
		scope.enter(Type.floatType.tsym);
		scope.enter(Type.doubleType.tsym);
		scope.enter(Type.booleanType.tsym);
		scope.enter(Type.errType.tsym);
		this.objectType = this.enterClass("java.lang.Object");
		this.classType = this.enterClass("java.lang.Class");
		this.stringType = this.enterClass("java.lang.String");
		this.stringBufferType = this.enterClass("java.lang.StringBuffer");
		final Type cloneableType = this.enterClass("java.lang.Cloneable");
		this.throwableType = this.enterClass("java.lang.Throwable");
		final Type serializableType = this.enterClass("java.io.Serializable");
		this.errorType = this.enterClass("java.lang.Error");
		this.exceptionType = this.enterClass("java.lang.Exception");
		this.runtimeExceptionType = this.enterClass("java.lang.RuntimeException");
		this.classNotFoundExceptionType = this.enterClass("java.lang.ClassNotFoundException");
		this.noClassDefFoundErrorType = this.enterClass("java.lang.NoClassDefFoundError");
		final ClassType classtype = (ClassType) ((Symbol) ArrayType.arrayClass).type;
		classtype.supertype_field = this.objectType;
		classtype.interfaces_field = List.make(cloneableType, serializableType);
		ArrayType.arrayClass.members_field = new Scope(ArrayType.arrayClass);
		this.lengthVar = new VarSymbol(17, Name.fromString("length"), Type.intType, ArrayType.arrayClass);
		ArrayType.arrayClass.members().enter(this.lengthVar);
		final Symbol methodsymbol = new MethodSymbol(1, Name.fromString("clone"), new MethodType(Type.emptyList, this.objectType, ClassSymbol.emptyList), this.objectType.tsym);
		ArrayType.arrayClass.members().enter(methodsymbol);
		this.nullConst = this.enterConstant("null", Type.botType);
		this.trueConst = this.enterConstant("true", Type.booleanType.constType(new Integer(1)));
		this.falseConst = this.enterConstant("false", Type.booleanType.constType(new Integer(0)));
		this.enterUnop("+", Type.intType, Type.intType, 0);
		this.enterUnop("+", Type.longType, Type.longType, 0);
		this.enterUnop("+", Type.floatType, Type.floatType, 0);
		this.enterUnop("+", Type.doubleType, Type.doubleType, 0);
		this.enterUnop("-", Type.intType, Type.intType, 116);
		this.enterUnop("-", Type.longType, Type.longType, 117);
		this.enterUnop("-", Type.floatType, Type.floatType, 118);
		this.enterUnop("-", Type.doubleType, Type.doubleType, 119);
		this.enterUnop("~", Type.intType, Type.intType, 130);
		this.enterUnop("~", Type.longType, Type.longType, 131);
		this.enterUnop("++", Type.byteType, Type.byteType, 96);
		this.enterUnop("++", Type.shortType, Type.shortType, 96);
		this.enterUnop("++", Type.charType, Type.charType, 96);
		this.enterUnop("++", Type.intType, Type.intType, 96);
		this.enterUnop("++", Type.longType, Type.longType, 97);
		this.enterUnop("++", Type.floatType, Type.floatType, 98);
		this.enterUnop("++", Type.doubleType, Type.doubleType, 99);
		this.enterUnop("--", Type.byteType, Type.byteType, 100);
		this.enterUnop("--", Type.shortType, Type.shortType, 100);
		this.enterUnop("--", Type.charType, Type.charType, 100);
		this.enterUnop("--", Type.intType, Type.intType, 100);
		this.enterUnop("--", Type.longType, Type.longType, 101);
		this.enterUnop("--", Type.floatType, Type.floatType, 102);
		this.enterUnop("--", Type.doubleType, Type.doubleType, 103);
		this.enterUnop("!", Type.booleanType, Type.booleanType, 257);
		this.enterBinop("+", this.stringType, this.stringType, this.stringType, 256);
		this.enterBinop("+", this.stringType, Type.intType, this.stringType, 256);
		this.enterBinop("+", this.stringType, Type.longType, this.stringType, 256);
		this.enterBinop("+", this.stringType, Type.floatType, this.stringType, 256);
		this.enterBinop("+", this.stringType, Type.doubleType, this.stringType, 256);
		this.enterBinop("+", this.stringType, Type.booleanType, this.stringType, 256);
		this.enterBinop("+", this.stringType, this.objectType, this.stringType, 256);
		this.enterBinop("+", Type.intType, this.stringType, this.stringType, 256);
		this.enterBinop("+", Type.longType, this.stringType, this.stringType, 256);
		this.enterBinop("+", Type.floatType, this.stringType, this.stringType, 256);
		this.enterBinop("+", Type.doubleType, this.stringType, this.stringType, 256);
		this.enterBinop("+", Type.booleanType, this.stringType, this.stringType, 256);
		this.enterBinop("+", this.objectType, this.stringType, this.stringType, 256);
		this.enterBinop("+", Type.intType, Type.intType, Type.intType, 96);
		this.enterBinop("+", Type.longType, Type.longType, Type.longType, 97);
		this.enterBinop("+", Type.floatType, Type.floatType, Type.floatType, 98);
		this.enterBinop("+", Type.doubleType, Type.doubleType, Type.doubleType, 99);
		this.enterBinop("-", Type.intType, Type.intType, Type.intType, 100);
		this.enterBinop("-", Type.longType, Type.longType, Type.longType, 101);
		this.enterBinop("-", Type.floatType, Type.floatType, Type.floatType, 102);
		this.enterBinop("-", Type.doubleType, Type.doubleType, Type.doubleType, 103);
		this.enterBinop("*", Type.intType, Type.intType, Type.intType, 104);
		this.enterBinop("*", Type.longType, Type.longType, Type.longType, 105);
		this.enterBinop("*", Type.floatType, Type.floatType, Type.floatType, 106);
		this.enterBinop("*", Type.doubleType, Type.doubleType, Type.doubleType, 107);
		this.enterBinop("/", Type.intType, Type.intType, Type.intType, 108);
		this.enterBinop("/", Type.longType, Type.longType, Type.longType, 109);
		this.enterBinop("/", Type.floatType, Type.floatType, Type.floatType, 110);
		this.enterBinop("/", Type.doubleType, Type.doubleType, Type.doubleType, 111);
		this.enterBinop("%", Type.intType, Type.intType, Type.intType, 112);
		this.enterBinop("%", Type.longType, Type.longType, Type.longType, 113);
		this.enterBinop("%", Type.floatType, Type.floatType, Type.floatType, 114);
		this.enterBinop("%", Type.doubleType, Type.doubleType, Type.doubleType, 115);
		this.enterBinop("&", Type.intType, Type.intType, Type.intType, 126);
		this.enterBinop("&", Type.longType, Type.longType, Type.longType, 127);
		this.enterBinop("&", Type.booleanType, Type.booleanType, Type.booleanType, 126);
		this.enterBinop("|", Type.intType, Type.intType, Type.intType, 128);
		this.enterBinop("|", Type.longType, Type.longType, Type.longType, 129);
		this.enterBinop("|", Type.booleanType, Type.booleanType, Type.booleanType, 128);
		this.enterBinop("^", Type.intType, Type.intType, Type.intType, 130);
		this.enterBinop("^", Type.longType, Type.longType, Type.longType, 131);
		this.enterBinop("^", Type.booleanType, Type.booleanType, Type.booleanType, 130);
		this.enterBinop("<<", Type.intType, Type.intType, Type.intType, 120);
		this.enterBinop("<<", Type.longType, Type.intType, Type.longType, 121);
		this.enterBinop("<<", Type.intType, Type.longType, Type.intType, 270);
		this.enterBinop("<<", Type.longType, Type.longType, Type.longType, 271);
		this.enterBinop(">>", Type.intType, Type.intType, Type.intType, 122);
		this.enterBinop(">>", Type.longType, Type.intType, Type.longType, 123);
		this.enterBinop(">>", Type.intType, Type.longType, Type.intType, 272);
		this.enterBinop(">>", Type.longType, Type.longType, Type.longType, 273);
		this.enterBinop(">>>", Type.intType, Type.intType, Type.intType, 124);
		this.enterBinop(">>>", Type.longType, Type.intType, Type.longType, 125);
		this.enterBinop(">>>", Type.intType, Type.longType, Type.intType, 274);
		this.enterBinop(">>>", Type.longType, Type.longType, Type.longType, 275);
		this.enterBinop("<", Type.intType, Type.intType, Type.booleanType, 161);
		this.enterBinop("<", Type.longType, Type.longType, Type.booleanType, 148, 155);
		this.enterBinop("<", Type.floatType, Type.floatType, Type.booleanType, 150, 155);
		this.enterBinop("<", Type.doubleType, Type.doubleType, Type.booleanType, 152, 155);
		this.enterBinop(">", Type.intType, Type.intType, Type.booleanType, 163);
		this.enterBinop(">", Type.longType, Type.longType, Type.booleanType, 148, 157);
		this.enterBinop(">", Type.floatType, Type.floatType, Type.booleanType, 149, 157);
		this.enterBinop(">", Type.doubleType, Type.doubleType, Type.booleanType, 151, 157);
		this.enterBinop("<=", Type.intType, Type.intType, Type.booleanType, 164);
		this.enterBinop("<=", Type.longType, Type.longType, Type.booleanType, 148, 158);
		this.enterBinop("<=", Type.floatType, Type.floatType, Type.booleanType, 150, 158);
		this.enterBinop("<=", Type.doubleType, Type.doubleType, Type.booleanType, 152, 158);
		this.enterBinop(">=", Type.intType, Type.intType, Type.booleanType, 162);
		this.enterBinop(">=", Type.longType, Type.longType, Type.booleanType, 148, 156);
		this.enterBinop(">=", Type.floatType, Type.floatType, Type.booleanType, 149, 156);
		this.enterBinop(">=", Type.doubleType, Type.doubleType, Type.booleanType, 151, 156);
		this.enterBinop("==", Type.intType, Type.intType, Type.booleanType, 159);
		this.enterBinop("==", Type.longType, Type.longType, Type.booleanType, 148, 153);
		this.enterBinop("==", Type.floatType, Type.floatType, Type.booleanType, 149, 153);
		this.enterBinop("==", Type.doubleType, Type.doubleType, Type.booleanType, 151, 153);
		this.enterBinop("==", Type.booleanType, Type.booleanType, Type.booleanType, 159);
		this.enterBinop("==", this.objectType, this.objectType, Type.booleanType, 165);
		this.enterBinop("!=", Type.intType, Type.intType, Type.booleanType, 160);
		this.enterBinop("!=", Type.longType, Type.longType, Type.booleanType, 148, 154);
		this.enterBinop("!=", Type.floatType, Type.floatType, Type.booleanType, 149, 154);
		this.enterBinop("!=", Type.doubleType, Type.doubleType, Type.booleanType, 151, 154);
		this.enterBinop("!=", Type.booleanType, Type.booleanType, Type.booleanType, 160);
		this.enterBinop("!=", this.objectType, this.objectType, Type.booleanType, 166);
		this.enterBinop("&&", Type.booleanType, Type.booleanType, Type.booleanType, 258);
		this.enterBinop("||", Type.booleanType, Type.booleanType, Type.booleanType, 259);
	}

	public final ClassReader reader;
	public final ClassWriter writer;
	final Type objectType;
	final Type classType;
	final Type stringType;
	final Type stringBufferType;
	final Type throwableType;
	final Type errorType;
	final Type exceptionType;
	final Type runtimeExceptionType;
	final Type classNotFoundExceptionType;
	final Type noClassDefFoundErrorType;
	final VarSymbol lengthVar;
	final VarSymbol nullConst;
	final VarSymbol trueConst;
	final VarSymbol falseConst;
	final ClassSymbol predefClass;
}
