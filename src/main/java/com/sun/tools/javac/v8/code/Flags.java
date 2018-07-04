package com.sun.tools.javac.v8.code;

public interface Flags {

	int PUBLIC = 1;
	int PRIVATE = 2;
	int PROTECTED = 4;
	int STATIC = 8;
	int FINAL = 16;
	int SYNCHRONIZED = 32;
	int VOLATILE = 64;
	int TRANSIENT = 128;
	int NATIVE = 256;
	int INTERFACE = 512;
	int ABSTRACT = 1024;
	int STRICTFP = 2048;
	int StandardFlags = 4095;
	int ACC_SUPER = 32;
	int SYNTHETIC = 0x10000;
	int DEPRECATED = 0x20000;
	int HASINIT = 0x40000;
	int CAPTURED = 0x80000;
	int BLOCK = 0x100000;
	int IPROXY = 0x200000;
	int NOOUTERTHIS = 0x400000;
	int HASDIRECTORY = 0x800000;
	int CLASS_SEEN = 0x2000000;
	int SOURCE_SEEN = 0x4000000;
	int LOCKED = 0x10000000;
	int UNATTRIBUTED = 0x20000000;
	int AccessFlags = 7;
	int LocalClassFlags = 3088;
	int MemberClassFlags = 3607;
	int ClassFlags = 3601;
	int LocalVarFlags = 16;
	int InterfaceVarFlags = 25;
	int VarFlags = 223;
	int ConstructorFlags = 7;
	int InterfaceMethodFlags = 1025;
	int MethodFlags = 3391;
}
