package com.sun.tools.javac.v8.util;

public final class Names {

	private Names() {
	}

	public static final Name slash = Name.fromString("/");
	public static final Name hyphen = Name.fromString("-");
	public static final Name slashequals = Name.fromString("/=");
	public static final Name deprecated = Name.fromString("deprecated");
	public static final Name init = Name.fromString("<init>");
	public static final Name clinit = Name.fromString("<clinit>");
	public static final Name error = Name.fromString("<error>");
	public static final Name any = Name.fromString("<any>");
	public static final Name empty = Name.fromString("");
	public static final Name period = Name.fromString(".");
	public static final Name dollar = Name.fromString("$");
	public static final Name comma = Name.fromString(",");
	public static final Name semicolon = Name.fromString(";");
	public static final Name asterisk = Name.fromString("*");
	public static final Name _this = Name.fromString("this");
	public static final Name _super = Name.fromString("super");
	public static final Name __input = Name.fromString("__input");
	public static final Name _null = Name.fromString("null");
	public static final Name _false = Name.fromString("false");
	public static final Name _true = Name.fromString("true");
	public static final Name _class = Name.fromString("class");
	public static final Name dot_class = Name.fromString(".class");
	public static final Name emptyPackage = Name.fromString("empty package");
	public static final Name java_lang = Name.fromString("java.lang");
	public static final Name java_lang_Object = Name.fromString("java.lang.Object");
	public static final Name java_lang_Cloneable = Name.fromString("java.lang.Cloneable");
	public static final Name java_io_Serializable = Name.fromString("java.io.Serializable");
	public static final Name ConstantValue = Name.fromString("ConstantValue");
	public static final Name LineNumberTable = Name.fromString("LineNumberTable");
	public static final Name LocalVariableTable = Name.fromString("LocalVariableTable");
	public static final Name Code = Name.fromString("Code");
	public static final Name Exceptions = Name.fromString("Exceptions");
	public static final Name SourceFile = Name.fromString("SourceFile");
	public static final Name InnerClasses = Name.fromString("InnerClasses");
	public static final Name Synthetic = Name.fromString("Synthetic");
	public static final Name Deprecated = Name.fromString("Deprecated");
	public static final Name append = Name.fromString("append");
	public static final Name forName = Name.fromString("forName");
	public static final Name toString = Name.fromString("toString");
	public static final Name valueOf = Name.fromString("valueOf");
	public static final Name classDollar = Name.fromString("class$");
	public static final Name thisDollar = Name.fromString("this$");
	public static final Name valDollar = Name.fromString("val$");
	public static final Name accessDollar = Name.fromString("access$");
	public static final Name getMessage = Name.fromString("getMessage");
	public static final Name TYPE = Name.fromString("TYPE");

}
