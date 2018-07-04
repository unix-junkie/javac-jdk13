package com.sun.tools.javac.v8.comp;

import com.sun.tools.javac.v8.code.Scope;
import com.sun.tools.javac.v8.code.Scope.Entry;
import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.MethodSymbol;
import com.sun.tools.javac.v8.code.Symbol.OperatorSymbol;
import com.sun.tools.javac.v8.code.Symbol.TypeSymbol;
import com.sun.tools.javac.v8.code.Symbol.VarSymbol;
import com.sun.tools.javac.v8.code.Type;
import com.sun.tools.javac.v8.code.Type.ClassType;
import com.sun.tools.javac.v8.code.Type.MethodType;
import com.sun.tools.javac.v8.tree.Tree;
import com.sun.tools.javac.v8.tree.Tree.Apply;
import com.sun.tools.javac.v8.tree.Tree.Assign;
import com.sun.tools.javac.v8.tree.Tree.Assignop;
import com.sun.tools.javac.v8.tree.Tree.ClassDef;
import com.sun.tools.javac.v8.tree.Tree.Ident;
import com.sun.tools.javac.v8.tree.Tree.MethodDef;
import com.sun.tools.javac.v8.tree.Tree.NewClass;
import com.sun.tools.javac.v8.tree.Tree.Operation;
import com.sun.tools.javac.v8.tree.Tree.Select;
import com.sun.tools.javac.v8.tree.Tree.TypeParameter;
import com.sun.tools.javac.v8.tree.Tree.VarDef;
import com.sun.tools.javac.v8.tree.TreeInfo;
import com.sun.tools.javac.v8.tree.TreeMaker;
import com.sun.tools.javac.v8.tree.TreeTranslator;
import com.sun.tools.javac.v8.util.Convert;
import com.sun.tools.javac.v8.util.Hashtable;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.ListBuffer;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;
import com.sun.tools.javac.v8.util.Util;

public final class TransInner extends TreeTranslator {
	class FreeVarCollector extends TreeTranslator {

		private void addFreeVar(final VarSymbol varsymbol) {
			for (List list = this.fvs; list.nonEmpty(); list = list.tail) {
				if (list.head == varsymbol) {
					return;
				}
			}

			this.fvs = this.fvs.prepend(varsymbol);
		}

		private void addFreeVars(final ClassSymbol classsymbol) {
			final List list = (List) TransInner.this.freevarCache.get(classsymbol);
			if (list != null) {
				for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
					this.addFreeVar((VarSymbol) list1.head);
				}

			}
		}

		public void _case(final Ident ident) {
			this.result = ident;
			if (ident.sym.kind == 4 && ident.sym.owner == this.owner) {
				final VarSymbol varsymbol = (VarSymbol) ident.sym;
				if (varsymbol.constValue == null && (((Symbol) varsymbol).flags_field & 0x80000) != 0) {
					this.addFreeVar(varsymbol);
				}
			}
		}

		public void _case(final NewClass newclass) {
			this.addFreeVars((ClassSymbol) newclass.constructor.owner);
			super._case(newclass);
		}

		public void _case(final Apply apply) {
			if (TreeInfo.name(apply.meth) == Names._super) {
				this.addFreeVars((ClassSymbol) TreeInfo.symbol(apply.meth).owner);
			}
			super._case(apply);
		}

		final Symbol owner;
		List fvs;

		FreeVarCollector(final Symbol symbol) {
			this.owner = symbol;
			this.fvs = VarSymbol.emptyList;
		}
	}

	class ClassMap extends TreeTranslator {

		public void _case(final ClassDef classdef) {
			TransInner.this.classdefs.put(classdef.sym, classdef);
			super._case(classdef);
		}

	}

	public TransInner(final Log log1, final Symtab symtab, final Resolve resolve, final Check check, final Attr attr1, final TreeMaker treemaker) {
		this.log = log1;
		this.syms = symtab;
		this.rs = resolve;
		this.chk = check;
		this.attr = attr1;
		this.make = treemaker;
	}

	private ClassDef classDef(final ClassSymbol classsymbol) {
		ClassDef classdef = (ClassDef) this.classdefs.get(classsymbol);
		if (classdef == null && this.outermostMemberDef != null) {
			new ClassMap().translate(this.outermostMemberDef);
			classdef = (ClassDef) this.classdefs.get(classsymbol);
		}
		if (classdef == null) {
			new ClassMap().translate(this.outermostClassDef);
			return (ClassDef) this.classdefs.get(classsymbol);
		}
		return classdef;
	}

	private List freevars(final ClassSymbol classsymbol) {
		if ((((Symbol) classsymbol).owner.kind & 0x14) != 0) {
			List list = (List) this.freevarCache.get(classsymbol);
			if (list == null) {
				final FreeVarCollector freevarcollector = new FreeVarCollector(((Symbol) classsymbol).owner);
				freevarcollector.translate(this.classDef(classsymbol));
				list = freevarcollector.fvs;
				this.freevarCache.put(classsymbol, list);
			}
			return list;
		}
		return VarSymbol.emptyList;
	}

	private Tree makeLit(final Type type, final Object obj) {
		return type.tag == 8 ? this.make.Ident(((Number) obj).intValue() != 0 ? (Symbol) this.syms.trueConst : (Symbol) this.syms.falseConst) : this.make.Literal(type.tag, obj).setType(type.constType(obj));
	}

	private Tree makeNewClass(final Type type, final List list) {
		final NewClass newclass = this.make.NewClass(null, this.make.QualIdent(type.tsym), list, null);
		newclass.constructor = this.rs.resolveConstructor(this.make.pos, this.attrEnv, type, TreeInfo.types(list));
		newclass.type = type;
		return newclass;
	}

	private Tree makeOperation(final int i, final List list) {
		final Operation operation = this.make.Operation(i, list);
		operation.operator = this.rs.resolveOperator(this.make.pos, i, this.attrEnv, TreeInfo.types(list));
		operation.type = operation.operator.type.restype();
		return operation;
	}

	private Tree makeString(final Tree tree) {
		if (tree.type.tag >= 10) {
			return tree;
		}
		final Symbol symbol = this.rs.resolveInternalMethod(tree.pos, this.attrEnv, this.syms.stringType, Names.valueOf, Type.emptyList, Type.emptyList.prepend(tree.type));
		return this.make.App(this.make.QualIdent(symbol), List.make(tree));
	}

	private ClassSymbol makeEmptyClass(final int i, final ClassSymbol classsymbol) {
		final ClassSymbol classsymbol1 = this.syms.reader.defineClass(Names.empty, classsymbol);
		classsymbol1.flatname = this.chk.localClassName(classsymbol1, 1);
		classsymbol1.sourcefile = classsymbol.sourcefile;
		classsymbol1.completer = null;
		classsymbol1.members_field = new Scope(classsymbol1);
		final ClassType classtype = (ClassType) ((Symbol) classsymbol1).type;
		classtype.supertype_field = this.syms.objectType;
		classtype.interfaces_field = Type.emptyList;
		classsymbol.members().enter(classsymbol1);
		this.chk.compiled.put(classsymbol1.flatname, classsymbol1);
		final ClassDef classdef = this.make.ClassDef(i, Names.empty, TypeParameter.emptyList, null, Tree.emptyList, Tree.emptyList);
		classdef.sym = classsymbol1;
		final ClassDef classdef1 = this.classDef(classsymbol);
		classdef1.defs = classdef1.defs.prepend(classdef);
		return classsymbol1;
	}

	private static int accessCode(final int i) {
		if (i >= 96 && i <= 131) {
			return (i - 96) * 2 + 12;
		}
		if (i == 256) {
			return 84;
		}
		return i >= 270 && i <= 275 ? (i - 270 + 131 + 2 - 96) * 2 + 12 : -1;
	}

	private static int accessCode(final Tree tree, final Tree tree1) {
		if (tree1 == null) {
			return 0;
		}
		if (tree1.tag == 26 && tree == ((Assign) tree1).lhs) {
			return 2;
		}
		if (tree1.tag >= 42 && tree1.tag <= 45 && tree == ((Operation) tree1).args.head) {
			return (tree1.tag - 42) * 2 + 4;
		}
		return tree1.tag >= 65 && tree1.tag <= 81 && tree == ((Assignop) tree1).lhs ? accessCode(((OperatorSymbol) ((Assignop) tree1).operator).opcode) : 0;
	}

	private OperatorSymbol binaryAccessOperator(final int i) {
		for (Entry entry = this.syms.predefClass.members().elems; entry != null; entry = entry.sibling) {
			if (entry.sym instanceof OperatorSymbol) {
				final OperatorSymbol operatorsymbol = (OperatorSymbol) entry.sym;
				if (accessCode(operatorsymbol.opcode) == i) {
					return operatorsymbol;
				}
			}
		}

		return null;
	}

	private static int treeTag(final OperatorSymbol operatorsymbol) {
		switch (operatorsymbol.opcode) {
		case 128:
		case 129:
			return 65;

		case 130:
		case 131:
			return 66;

		case 126: // '~'
		case 127: // '\177'
			return 67;

		case 120: // 'x'
		case 121: // 'y'
		case 270:
		case 271:
			return 74;

		case 122: // 'z'
		case 123: // '{'
		case 272:
		case 273:
			return 75;

		case 124: // '|'
		case 125: // '}'
		case 274:
		case 275:
			return 76;

		case 96: // '`'
		case 97: // 'a'
		case 98: // 'b'
		case 99: // 'c'
		case 256:
			return 77;

		case 100: // 'd'
		case 101: // 'e'
		case 102: // 'f'
		case 103: // 'g'
			return 78;

		case 104: // 'h'
		case 105: // 'i'
		case 106: // 'j'
		case 107: // 'k'
			return 79;

		case 108: // 'l'
		case 109: // 'm'
		case 110: // 'n'
		case 111: // 'o'
			return 80;

		case 112: // 'p'
		case 113: // 'q'
		case 114: // 'r'
		case 115: // 's'
			return 81;

		case 116: // 't'
		case 117: // 'u'
		case 118: // 'v'
		case 119: // 'w'
		case 132:
		case 133:
		case 134:
		case 135:
		case 136:
		case 137:
		case 138:
		case 139:
		case 140:
		case 141:
		case 142:
		case 143:
		case 144:
		case 145:
		case 146:
		case 147:
		case 148:
		case 149:
		case 150:
		case 151:
		case 152:
		case 153:
		case 154:
		case 155:
		case 156:
		case 157:
		case 158:
		case 159:
		case 160:
		case 161:
		case 162:
		case 163:
		case 164:
		case 165:
		case 166:
		case 167:
		case 168:
		case 169:
		case 170:
		case 171:
		case 172:
		case 173:
		case 174:
		case 175:
		case 176:
		case 177:
		case 178:
		case 179:
		case 180:
		case 181:
		case 182:
		case 183:
		case 184:
		case 185:
		case 186:
		case 187:
		case 188:
		case 189:
		case 190:
		case 191:
		case 192:
		case 193:
		case 194:
		case 195:
		case 196:
		case 197:
		case 198:
		case 199:
		case 200:
		case 201:
		case 202:
		case 203:
		case 204:
		case 205:
		case 206:
		case 207:
		case 208:
		case 209:
		case 210:
		case 211:
		case 212:
		case 213:
		case 214:
		case 215:
		case 216:
		case 217:
		case 218:
		case 219:
		case 220:
		case 221:
		case 222:
		case 223:
		case 224:
		case 225:
		case 226:
		case 227:
		case 228:
		case 229:
		case 230:
		case 231:
		case 232:
		case 233:
		case 234:
		case 235:
		case 236:
		case 237:
		case 238:
		case 239:
		case 240:
		case 241:
		case 242:
		case 243:
		case 244:
		case 245:
		case 246:
		case 247:
		case 248:
		case 249:
		case 250:
		case 251:
		case 252:
		case 253:
		case 254:
		case 255:
		case 257:
		case 258:
		case 259:
		case 260:
		case 261:
		case 262:
		case 263:
		case 264:
		case 265:
		case 266:
		case 267:
		case 268:
		case 269:
		default:
			throw new InternalError();
		}
	}

	private static Name accessName(final int i, final int j) {
		return Name.fromString("access$" + i + j / 10 + j % 10);
	}

	private Symbol accessSymbol(final Symbol symbol, final Tree tree, final Tree tree1, final boolean flag) {
		final ClassSymbol classsymbol = this.accessClass(symbol, flag);
		Symbol symbol1 = symbol;
		if (symbol.owner != classsymbol) {
			symbol1 = symbol.clone(classsymbol);
			this.actualSymbols.put(symbol1, symbol);
		}
		Integer integer = (Integer) this.accessNums.get(symbol1);
		if (integer == null) {
			integer = new Integer(this.accessed.length());
			this.accessNums.put(symbol1, integer);
			this.accessSyms.put(symbol1, new MethodSymbol[NCODES]);
			this.accessed.append(symbol1);
		}
		int i;
		List list;
		final Type type;
		final List list1;
		switch (symbol1.kind) {
		case 4:
			i = accessCode(tree, tree1);
			if (i >= 12) {
				final OperatorSymbol operatorsymbol = this.binaryAccessOperator(i);
				list = operatorsymbol.opcode == 256 ? List.make(this.syms.objectType) : ((Symbol) operatorsymbol).type.argtypes().tail;
			} else if (i == 2) {
				list = Type.emptyList.prepend(symbol1.erasure());
			} else {
				list = Type.emptyList;
			}
			type = symbol1.erasure();
			list1 = ClassSymbol.emptyList;
			break;
		case 16:
			i = 0;
			list = symbol1.erasure().argtypes();
			type = symbol1.erasure().restype();
			list1 = symbol1.type.thrown();
			break;
		default:
			throw new InternalError();
		}
		if (flag) {
			i++;
		}
		if ((symbol1.flags() & 8) == 0) {
			list = list.prepend(symbol1.owner.erasure());
		}
		final MethodSymbol amethodsymbol[] = (MethodSymbol[]) this.accessSyms.get(symbol1);
		MethodSymbol methodsymbol = amethodsymbol[i];
		if (methodsymbol == null) {
			methodsymbol = new MethodSymbol(0x10008, accessName(integer.intValue(), i), new MethodType(list, type, list1), classsymbol);
			classsymbol.members().enter(methodsymbol);
			amethodsymbol[i] = methodsymbol;
		}
		return methodsymbol;
	}

	private Tree accessBase(final int i, final Symbol symbol) {
		return (symbol.flags() & 8) == 0 ? this.makeOwnerThis(i, symbol) : this.access(this.make.at(i).QualIdent(symbol.owner));
	}

	private boolean needsPrivateAccess(final Symbol symbol) {
		return (symbol.flags() & 2) != 0 && symbol.owner != this.currentClass;
	}

	private boolean needsProtectedAccess(final Symbol symbol) {
		return (symbol.flags() & 4) != 0 && symbol.owner.owner != ((Symbol) this.currentClass).owner && symbol.packge() != this.currentClass.packge() && !this.currentClass.isSubClass(symbol.owner);
	}

	private ClassSymbol accessClass(final Symbol symbol, final boolean flag) {
		if (flag) {
			ClassSymbol classsymbol;
			for (classsymbol = this.currentClass; !classsymbol.isSubClass(symbol.owner); classsymbol = ((Symbol) classsymbol).owner.enclClass()) {
			}
			return classsymbol;
		}
		return symbol.owner.enclClass();
	}

	private Tree access(Symbol symbol, Tree tree, final Tree tree1, final boolean flag) {
		if (symbol.kind == 4 && (symbol.flags() & 0x80000) != 0 && symbol.owner.enclClass() != this.currentClass) {
			this.make.at(tree.pos);
			final Object obj = ((VarSymbol) symbol).constValue;
			if (obj != null) {
				return this.makeLit(symbol.type, obj);
			}
			symbol = this.proxies.lookup(proxyName(symbol.name)).sym;
			tree = this.access(this.make.at(tree.pos).Ident(symbol));
		}
		Tree tree2 = tree.tag != 30 ? null : ((Select) tree).selected;
		switch (symbol.kind) {
		default:
			return tree;

		case 2: // '\002'
			if (symbol.owner.kind != 1) {
				final Name name = Convert.shortName(symbol.flatName());
				for (; tree2 != null && TreeInfo.symbol(tree2).kind != 1; tree2 = tree2.tag != 30 ? null : ((Select) tree2).selected) {
				}
				if (tree.tag == 31) {
					((Ident) tree).name = name;
				} else if (tree2 == null) {
					tree = this.make.at(tree.pos).Ident(symbol);
					((Ident) tree).name = name;
				} else {
					((Select) tree).selected = tree2;
					((Select) tree).name = name;
				}
			}
			return tree;

		case 4: // '\004'
		case 16: // '\020'
			if (symbol.owner.kind != 2) {
				return tree;
			}
			final boolean flag1 = flag || this.needsProtectedAccess(symbol);
			final boolean flag2 = flag1 || this.needsPrivateAccess(symbol);
			final boolean flag3 = tree2 == null && symbol.owner != this.syms.predefClass && !this.currentClass.isSubClass(symbol.owner);
			if (!flag2 && !flag3) {
				return tree;
			}
			this.make.at(tree.pos);
			if (symbol.kind == 4) {
				final Object obj1 = ((VarSymbol) symbol).constValue;
				if (obj1 != null) {
					return this.makeLit(symbol.type, obj1);
				}
			}
			if (flag2) {
				List list = Tree.emptyList;
				if ((symbol.flags() & 8) == 0) {
					if (tree2 == null) {
						tree2 = this.makeOwnerThis(tree.pos, symbol);
					}
					list = list.prepend(tree2);
					tree2 = null;
				}
				final Tree tree3 = this.make.Select(tree2 == null ? this.make.QualIdent(symbol.owner) : tree2, this.accessSymbol(symbol, tree, tree1, flag1));
				return this.make.App(tree3, list);
			}
			if (flag3) {
				return this.make.at(tree.pos).Select(this.accessBase(tree.pos, symbol), symbol);
			}
			return tree;
		}
	}

	private Tree access(final Tree tree) {
		final Symbol symbol = TreeInfo.symbol(tree);
		return symbol != null ? this.access(symbol, tree, null, false) : tree;
	}

	private Symbol accessConstructor(final Symbol symbol) {
		if (this.needsPrivateAccess(symbol)) {
			final ClassSymbol classsymbol = this.accessClass(symbol, false);
			MethodSymbol methodsymbol = (MethodSymbol) this.accessConstrs.get(symbol);
			if (methodsymbol == null) {
				methodsymbol = new MethodSymbol(0x10000, Names.init, new MethodType(symbol.type.argtypes().append(((Symbol) this.accessConstructorTag()).type), symbol.type.restype(), symbol.type.thrown()), classsymbol);
				classsymbol.members().enter(methodsymbol);
				this.accessConstrs.put(symbol, methodsymbol);
				this.accessed.append(symbol);
			}
			return methodsymbol;
		}
		return symbol;
	}

	private ClassSymbol accessConstructorTag() {
		final ClassSymbol classsymbol = this.currentClass.outermostClass();
		final Name name = Name.fromString(classsymbol.fullName() + "$" + 1);
		final ClassSymbol classsymbol1 = (ClassSymbol) this.chk.compiled.get(name);
		if (classsymbol1 == null) {
			return this.makeEmptyClass(520, classsymbol);
		}
		return classsymbol1;
	}

	private void makeAccessible(final Symbol symbol) {
		final ClassDef classdef = this.classDef(symbol.owner.enclClass());
		Util.assertTrue(classdef != null, "class def not found: " + symbol + " in " + symbol.owner);
		if (symbol.name == Names.init) {
			classdef.defs = classdef.defs.prepend(this.accessConstructorDef(((Tree) classdef).pos, symbol, (MethodSymbol) this.accessConstrs.get(symbol)));
		} else {
			final MethodSymbol amethodsymbol[] = (MethodSymbol[]) this.accessSyms.get(symbol);
			for (int i = 0; i < NCODES; i++) {
				if (amethodsymbol[i] != null) {
					classdef.defs = classdef.defs.prepend(this.accessDef(((Tree) classdef).pos, symbol, amethodsymbol[i], i));
				}
			}

		}
	}

	private Tree accessDef(final int i, final Symbol symbol, final MethodSymbol methodsymbol, final int j) {
		this.currentClass = symbol.owner.enclClass();
		this.make.at(i);
		final MethodDef methoddef = this.make.MethodDef(methodsymbol, null);
		Symbol symbol1 = (Symbol) this.actualSymbols.get(symbol);
		if (symbol1 == null) {
			symbol1 = symbol;
		}
		final Tree tree;
		final List list;
		if ((symbol1.flags() & 8) != 0) {
			tree = this.make.Ident(symbol1);
			list = this.make.Idents(methoddef.params);
		} else {
			tree = this.make.Select(this.make.Ident((VarDef) methoddef.params.head), symbol1);
			list = this.make.Idents(methoddef.params.tail);
		}
		final Object obj;
		if (symbol1.kind == 4) {
			final int k = j - (j & 1);
			final Object obj1;
			switch (k) {
			case 0: // '\0'
				obj1 = tree;
				break;

			case 2: // '\002'
				obj1 = this.make.Assign(tree, (Tree) list.head);
				break;

			case 4: // '\004'
			case 6: // '\006'
			case 8: // '\b'
			case 10: // '\n'
				obj1 = this.makeOperation((k - 4 >> 1) + 42, List.make(tree));
				break;

			case 1: // '\001'
			case 3: // '\003'
			case 5: // '\005'
			case 7: // '\007'
			case 9: // '\t'
			default:
				obj1 = this.make.Assignop(treeTag(this.binaryAccessOperator(k)), tree, (Tree) list.head);
				((Assignop) obj1).operator = this.binaryAccessOperator(k);
				break;
			}
			obj = this.make.Return(((Tree) obj1).setType(symbol1.type));
		} else {
			obj = this.make.Call(this.make.App(tree, list));
		}
		methoddef.body = this.make.Block(0, List.make(obj));
		for (List list1 = methoddef.params; list1.nonEmpty(); list1 = list1.tail) {
			((VarDef) list1.head).vartype = this.access(((VarDef) list1.head).vartype);
		}

		methoddef.restype = this.access(methoddef.restype);
		for (List list2 = methoddef.thrown; list2.nonEmpty(); list2 = list2.tail) {
			list2.head = this.access((Tree) list2.head);
		}

		return methoddef;
	}

	private Tree accessConstructorDef(final int i, final Symbol symbol, final MethodSymbol methodsymbol) {
		this.make.at(i);
		final MethodDef methoddef = this.make.MethodDef(methodsymbol, methodsymbol.externalType(), null);
		final Ident ident = this.make.Ident(Names._this);
		ident.sym = symbol;
		ident.type = symbol.type;
		methoddef.body = this.make.Block(0, List.make(this.make.Call(this.make.App(ident, this.make.Idents(methoddef.params.reverse().tail.reverse())))));
		return methoddef;
	}

	private static Name proxyName(final Name name) {
		return Name.fromString("val$" + name);
	}

	private List freevarDefs(final int i, final List list, final Symbol symbol) {
		int j = 0x10010;
		if (symbol.kind == 2) {
			j |= 2;
		}
		List list1 = VarDef.emptyList;
		for (List list2 = list; list2.nonEmpty(); list2 = list2.tail) {
			final Symbol varsymbol = (Symbol) list2.head;
			final VarSymbol varsymbol1 = new VarSymbol(j, proxyName(varsymbol.name), varsymbol.erasure(), symbol);
			this.proxies.enter(varsymbol1);
			final VarDef vardef = this.make.at(i).VarDef(varsymbol1, null);
			vardef.vartype = this.access(vardef.vartype);
			list1 = list1.prepend(vardef);
		}

		return list1;
	}

	private static Name outerThisName(final Type type) {
		Type type1 = type.outer();
		int i;
		for (i = 0; type1.tag == 10; i++) {
			type1 = type1.outer();
		}

		return Name.fromString("this$" + i);
	}

	private VarDef outerThisDef(final int i, final Symbol symbol) {
		int j = 0x10010;
		if (symbol.kind == 2) {
			j |= 2;
		}
		final Type type = ((Symbol) symbol.enclClass()).type.outer().erasure();
		final VarSymbol varsymbol = new VarSymbol(j, outerThisName(type), type, symbol);
		this.outerThisStack = this.outerThisStack.prepend(varsymbol);
		final VarDef vardef = this.make.at(i).VarDef(varsymbol, null);
		vardef.vartype = this.access(vardef.vartype);
		return vardef;
	}

	private List loadFreevars(final int i, final List list) {
		List list1 = Tree.emptyList;
		for (List list2 = list; list2.nonEmpty(); list2 = list2.tail) {
			list1 = list1.prepend(this.loadFreevar(i, (Symbol) list2.head));
		}

		return list1;
	}

	private Tree loadFreevar(final int i, final Symbol varsymbol) {
		return this.access(varsymbol, this.make.at(i).Ident(varsymbol), null, false);
	}

	private Tree makeThis(final int i, final Symbol typesymbol, final boolean flag) {
		return this.currentClass == typesymbol || !flag && this.currentClass.isSubClass(typesymbol) ? this.make.at(i).This(typesymbol.erasure()) : this.makeOuterThis(i, typesymbol, flag);
	}

	private Tree makeOuterThis(final int i, final Symbol typesymbol, final boolean flag) {
		List list = this.outerThisStack;
		if (list.isEmpty()) {
			this.log.error(i, "no.encl.instance.of.type.in.scope", typesymbol.toJava());
			return this.make.Ident(this.syms.nullConst);
		}
		VarSymbol varsymbol = (VarSymbol) list.head;
		Tree tree = this.access(this.make.at(i).Ident(varsymbol));
		for (TypeSymbol typesymbol1 = ((Symbol) varsymbol).type.tsym; typesymbol1 != typesymbol && (flag || !typesymbol1.isSubClass(typesymbol)); typesymbol1 = ((Symbol) varsymbol).type.tsym) {
			do {
				list = list.tail;
				if (list.isEmpty()) {
					this.log.error(i, "no.encl.instance.of.type.in.scope", typesymbol.toJava());
					return tree;
				}
				varsymbol = (VarSymbol) list.head;
			} while (((Symbol) varsymbol).owner != typesymbol1);
			tree = this.access(this.make.at(i).Select(tree, varsymbol));
		}

		return tree;
	}

	private Tree makeOwnerThis(final int i, final Symbol symbol) {
		final Symbol symbol1 = symbol.owner;
		if (this.currentClass.isSubClass(symbol1) && symbol.isMemberOf(this.currentClass)) {
			return this.make.at(i).This(symbol1.erasure());
		}
		List list = this.outerThisStack;
		if (list.isEmpty()) {
			this.log.error(i, "no.encl.instance.of.type.in.scope", symbol1.toJava());
			return this.make.Ident(this.syms.nullConst);
		}
		VarSymbol varsymbol = (VarSymbol) list.head;
		Tree tree = this.access(this.make.at(i).Ident(varsymbol));
		for (TypeSymbol typesymbol = ((Symbol) varsymbol).type.tsym; !typesymbol.isSubClass(symbol1) || !symbol.isMemberOf(typesymbol); typesymbol = ((Symbol) varsymbol).type.tsym) {
			do {
				list = list.tail;
				if (list.isEmpty()) {
					this.log.error(i, "no.encl.instance.of.type.in.scope", symbol1.toJava());
					return tree;
				}
				varsymbol = (VarSymbol) list.head;
			} while (((Symbol) varsymbol).owner != typesymbol);
			tree = this.access(this.make.at(i).Select(tree, varsymbol));
		}

		return tree;
	}

	private Tree initField(final int i, final Name name) {
		final Entry entry = this.proxies.lookup(name);
		final Symbol symbol = entry.sym;
		Util.assertTrue(symbol.owner.kind == 16);
		final Symbol symbol1 = entry.next().sym;
		Util.assertTrue(symbol.owner.owner == symbol1.owner);
		this.make.at(i);
		return this.make.Exec(this.make.Assign(this.make.Select(this.make.This(symbol1.owner.erasure()), symbol1), this.make.Ident(symbol)).setType(symbol1.erasure()));
	}

	private Tree initOuterThis(final int i) {
		final Symbol varsymbol = (Symbol) this.outerThisStack.head;
		Util.assertTrue(varsymbol.owner.kind == 16);
		final Symbol varsymbol1 = (Symbol) this.outerThisStack.tail.head;
		Util.assertTrue(varsymbol.owner.owner == varsymbol1.owner);
		this.make.at(i);
		return this.make.Exec(this.make.Assign(this.make.Select(this.make.This(varsymbol1.owner.erasure()), varsymbol1), this.make.Ident(varsymbol)).setType(varsymbol1.erasure()));
	}

	private ClassSymbol currentTrueClassSym() {
		if ((this.currentClass.flags() & 0x200) == 0) {
			return this.currentClass;
		}
		final Scope scope = this.currentClass.members();
		for (Entry entry = scope.elems; entry != null; entry = entry.sibling) {
			if (entry.sym.kind == 2 && entry.sym.name == Names.empty && (entry.sym.flags() & 0x200) == 0) {
				return (ClassSymbol) entry.sym;
			}
		}

		return this.makeEmptyClass(9, this.currentClass);
	}

	private Symbol classDollarSym() {
		final ClassSymbol classsymbol = this.currentTrueClassSym();
		MethodSymbol methodsymbol = (MethodSymbol) classsymbol.members().lookup(Names.classDollar).sym;
		if (methodsymbol == null) {
			methodsymbol = new MethodSymbol(0x10008, Names.classDollar, new MethodType(Type.emptyList.prepend(this.syms.stringType), this.syms.classType, ClassSymbol.emptyList), classsymbol);
			classsymbol.members().enter(methodsymbol);
			final MethodDef methoddef = this.make.MethodDef(methodsymbol, null);
			final Symbol symbol = this.rs.resolveInternalMethod(this.make.pos, this.attrEnv, this.syms.classType, Names.forName, Type.emptyList, Type.emptyList.prepend(this.syms.stringType));
			final VarSymbol varsymbol = new VarSymbol(0, TreeMaker.paramName(1), this.syms.classNotFoundExceptionType, methodsymbol);
			final Symbol symbol1 = this.rs.resolveInternalMethod(this.make.pos, this.attrEnv, this.syms.classNotFoundExceptionType, Names.getMessage, Type.emptyList, Type.emptyList);
			methoddef.body = this.make.Block(0, Tree.emptyList.prepend(this.make.Try(this.make.Call(this.make.App(this.make.QualIdent(symbol), List.make(this.make.Ident(((VarDef) methoddef.params.head).sym)))), List.make(this.make.Catch(this.make.VarDef(varsymbol, null), this.make.Throw(this.makeNewClass(this.syms.noClassDefFoundErrorType, List.make(this.make.App(this.make.Select(this.make.Ident(varsymbol), symbol1), Tree.emptyList)))))), null)));
			final ClassDef classdef = this.classDef(classsymbol);
			classdef.defs = classdef.defs.prepend(methoddef);
		}
		return methodsymbol;
	}

	private static Name cacheName(String s) {
		StringBuffer stringbuffer = new StringBuffer();
		if (s.length() > 0 && s.charAt(0) == '[') {
			stringbuffer = stringbuffer.append("array");
			for (; s.length() > 0 && s.charAt(0) == '['; s = s.substring(1)) {
				stringbuffer = stringbuffer.append('$');
			}

			if (s.length() > 0 && s.charAt(0) == 'L') {
				s = s.substring(0, s.length() - 1);
			}
		} else {
			stringbuffer = stringbuffer.append("class$");
		}
		stringbuffer = stringbuffer.append(s.replace('.', '$'));
		return Name.fromString(stringbuffer.toString());
	}

	private VarSymbol cacheSym(final String s) {
		final ClassSymbol classsymbol = this.currentTrueClassSym();
		final Name name = cacheName(s);
		VarSymbol varsymbol = (VarSymbol) classsymbol.members().lookup(name).sym;
		if (varsymbol == null) {
			varsymbol = new VarSymbol(0x10008, name, this.syms.classType, classsymbol);
			classsymbol.members().enter(varsymbol);
			final VarDef vardef = this.make.VarDef(varsymbol, null);
			final ClassDef classdef = this.classDef(classsymbol);
			classdef.defs = classdef.defs.prepend(vardef);
		}
		return varsymbol;
	}

	private Tree classOf(final Tree tree) {
		switch (tree.type.tag) {
		case 1: // '\001'
		case 2: // '\002'
		case 3: // '\003'
		case 4: // '\004'
		case 5: // '\005'
		case 6: // '\006'
		case 7: // '\007'
		case 8: // '\b'
		case 9: // '\t'
			final Name name = Type.boxedName[tree.type.tag];
			final ClassSymbol classsymbol = this.syms.reader.enterClass(name);
			final Symbol symbol = this.rs.access(this.rs.findIdentInType(this.attrEnv, ((Symbol) classsymbol).type, Names.TYPE, 4), tree.pos, ((Symbol) classsymbol).type, Names.TYPE);
			if (symbol.kind == 4) {
				this.attr.evalInit((VarSymbol) symbol);
			}
			return this.make.QualIdent(symbol);

		case 10: // '\n'
		case 11: // '\013'
			final String s = this.syms.writer.xClassName(tree.type).toString().replace('/', '.');
			final VarSymbol varsymbol = this.cacheSym(s);
			return this.make.Conditional(16, this.makeOperation(51, List.make(this.make.Ident(varsymbol), this.make.Ident(this.syms.nullConst))), this.make.Assign(this.make.Ident(varsymbol), this.make.App(this.make.Ident(this.classDollarSym()), List.make(this.make.Literal(10, s).setType(this.syms.stringType)))).setType(this.syms.classType), this.make.Ident(varsymbol)).setType(this.syms.classType);
		}
		throw new InternalError();
	}

	private Tree translate(final Tree tree, final Tree tree1) {
		final Tree tree2 = this.enclOp;
		this.enclOp = tree1;
		final Tree tree3 = this.translate(tree);
		this.enclOp = tree2;
		return tree3;
	}

	private List translate(final List list, final Tree tree) {
		final Tree tree1 = this.enclOp;
		this.enclOp = tree;
		final List list1 = this.translate(list);
		this.enclOp = tree1;
		return list1;
	}

	public void _case(final ClassDef classdef) {
		final ClassSymbol classsymbol = this.currentClass;
		this.currentClass = classdef.sym;
		this.classdefs.put(this.currentClass, classdef);
		this.proxies = this.proxies.dup();
		final List list = this.outerThisStack;
		VarDef vardef = null;
		if (this.currentClass.hasOuterInstance()) {
			vardef = this.outerThisDef(((Tree) classdef).pos, this.currentClass);
		}
		final List list1 = this.freevarDefs(((Tree) classdef).pos, this.freevars(this.currentClass), this.currentClass);
		classdef.extending = this.translate(classdef.extending);
		classdef.implementing = this.translate(classdef.implementing);
		List list3;
		for (List list2 = Tree.emptyList; classdef.defs != list2; list2 = list3) {
			list3 = classdef.defs;
			for (List list5 = list3; list5.nonEmpty() && list5 != list2; list5 = list5.tail) {
				final Tree tree = this.outermostMemberDef;
				if (tree == null) {
					this.outermostMemberDef = (Tree) list5.head;
				}
				list5.head = this.translate((Tree) list5.head);
				this.outermostMemberDef = tree;
			}

		}

		if ((classdef.flags & 4) != 0) {
			classdef.flags |= 1;
		}
		classdef.flags &= 0xe11;
		classdef.name = Convert.shortName(this.currentClass.flatName());
		for (List list4 = list1; list4.nonEmpty(); list4 = list4.tail) {
			classdef.defs = classdef.defs.prepend(list4.head);
			this.currentClass.members().enter(((VarDef) list4.head).sym);
		}

		if (this.currentClass.hasOuterInstance()) {
			classdef.defs = classdef.defs.prepend(vardef);
			this.currentClass.members().enter(vardef.sym);
		}
		this.proxies = this.proxies.leave();
		this.outerThisStack = list;
		this.translated.append(classdef);
		this.currentClass = classsymbol;
		this.result = this.make.at(((Tree) classdef).pos).Block(0, Tree.emptyList);
	}

	public void _case(final MethodDef methoddef) {
		if (methoddef.name == Names.init && (this.currentClass.isInner() || (((Symbol) this.currentClass).owner.kind & 0x14) != 0)) {
			final MethodSymbol methodsymbol = methoddef.sym;
			this.proxies = this.proxies.dup();
			final List list = this.outerThisStack;
			final List list1 = this.freevars(this.currentClass);
			VarDef vardef = null;
			if (this.currentClass.hasOuterInstance()) {
				vardef = this.outerThisDef(((Tree) methoddef).pos, methodsymbol);
			}
			final List list2 = this.freevarDefs(((Tree) methoddef).pos, list1, methodsymbol);
			super._case(methoddef);
			methoddef.params = methoddef.params.prepend(list2);
			if (this.currentClass.hasOuterInstance()) {
				methoddef.params = methoddef.params.prepend(vardef);
			}
			final Tree tree = (Tree) methoddef.body.stats.head;
			List list3 = methoddef.body.stats.tail;
			if (list1.nonEmpty()) {
				List list4 = methodsymbol.erasure().argtypes();
				for (List list5 = list1; list5.nonEmpty(); list5 = list5.tail) {
					if (TreeInfo.isInitialConstructor(methoddef)) {
						list3 = list3.prepend(this.initField(((Tree) methoddef.body).pos, proxyName(((Symbol) (VarSymbol) list5.head).name)));
					}
					list4 = list4.prepend(((Symbol) list5.head).erasure());
				}

				methodsymbol.erasure_field = new MethodType(list4, methodsymbol.erasure().restype(), methodsymbol.erasure().thrown());
			}
			if (this.currentClass.hasOuterInstance() && TreeInfo.isInitialConstructor(methoddef)) {
				list3 = list3.prepend(this.initOuterThis(((Tree) methoddef.body).pos));
			}
			methoddef.body.stats = list3.prepend(tree);
			this.proxies = this.proxies.leave();
			this.outerThisStack = list;
		} else {
			super._case(methoddef);
		}
		this.result = methoddef;
	}

	public void _case(final NewClass newclass) {
		final ClassSymbol classsymbol = (ClassSymbol) newclass.constructor.owner;
		newclass.args = this.translate(newclass.args);
		if ((((Symbol) classsymbol).owner.kind & 0x14) != 0) {
			newclass.args = newclass.args.prepend(this.loadFreevars(((Tree) newclass).pos, this.freevars(classsymbol)));
		}
		final Symbol symbol = this.accessConstructor(newclass.constructor);
		if (symbol != newclass.constructor) {
			newclass.args = newclass.args.append(this.make.Ident(this.syms.nullConst));
			newclass.constructor = symbol;
		}
		if (classsymbol.hasOuterInstance()) {
			final Tree tree = newclass.encl != null ? this.translate(newclass.encl) : this.makeThis(((Tree) newclass).pos, ((Symbol) classsymbol).type.outer().tsym, false);
			newclass.args = newclass.args.prepend(tree);
		}
		newclass.encl = null;
		if (newclass.def != null) {
			this.translate(newclass.def);
			newclass.clazz = this.access(this.make.at(newclass.clazz.pos).Ident(newclass.def.sym));
			newclass.def = null;
		} else {
			newclass.clazz = this.access(classsymbol, newclass.clazz, this.enclOp, false);
		}
		this.result = newclass;
	}

	public void _case(final Apply apply) {
		apply.args = this.translate(apply.args);
		final Name name = TreeInfo.name(apply.meth);
		if (name == Names._this || name == Names._super) {
			final Symbol symbol = this.accessConstructor(TreeInfo.symbol(apply.meth));
			if (symbol != TreeInfo.symbol(apply.meth)) {
				apply.args = apply.args.append(this.make.Ident(this.syms.nullConst));
				TreeInfo.setSymbol(apply.meth, symbol);
			}
			final ClassSymbol classsymbol = (ClassSymbol) symbol.owner;
			if ((((Symbol) classsymbol).owner.kind & 0x14) != 0) {
				apply.args = apply.args.prepend(this.loadFreevars(((Tree) apply).pos, this.freevars(classsymbol)));
			}
			if (classsymbol.hasOuterInstance()) {
				final Tree tree;
				if (apply.meth.tag == 30) {
					tree = this.translate(((Select) apply.meth).selected);
					apply.meth = this.make.Ident(symbol);
					((Ident) apply.meth).name = name;
				} else {
					tree = this.makeOuterThis(((Tree) apply).pos, ((Symbol) classsymbol).type.outer().tsym, false);
				}
				apply.args = apply.args.prepend(tree);
			}
		} else {
			apply.meth = this.translate(apply.meth);
			if (apply.meth.tag == 23) {
				final Apply apply1 = (Apply) apply.meth;
				apply1.args = apply.args.prepend(apply1.args);
				this.result = apply1;
				return;
			}
		}
		this.result = apply;
	}

	public void _case(final Assign assign) {
		assign.lhs = this.translate(assign.lhs, assign);
		assign.rhs = this.translate(assign.rhs);
		if (assign.lhs.tag == 23) {
			final Apply apply = (Apply) assign.lhs;
			apply.args = List.make(assign.rhs).prepend(apply.args);
			this.result = apply;
		} else {
			this.result = assign;
		}
	}

	public void _case(final Assignop assignop) {
		assignop.lhs = this.translate(assignop.lhs, assignop);
		assignop.rhs = this.translate(assignop.rhs);
		if (assignop.lhs.tag == 23) {
			final Apply apply = (Apply) assignop.lhs;
			final Tree tree = ((OperatorSymbol) assignop.operator).opcode != 256 ? assignop.rhs : this.makeString(assignop.rhs);
			apply.args = List.make(tree).prepend(apply.args);
			this.result = apply;
		} else {
			this.result = assignop;
		}
	}

	public void _case(final Operation operation) {
		operation.args = this.translate(operation.args, operation);
		this.result = ((Tree) operation).tag >= 42 && ((Tree) operation).tag <= 45 && ((Tree) operation.args.head).tag == 23 ? (Tree) operation.args.head : operation;
	}

	public void _case(final Ident ident) {
		this.result = this.access(ident.sym, ident, this.enclOp, false);
	}

	public void _case(final Select select) {
		final boolean flag = select.selected.tag == 30 && TreeInfo.name(select.selected) == Names._super;
		select.selected = this.translate(select.selected);
		if (select.name == Names._class) {
			this.result = this.classOf(select.selected);
		} else if (select.name == Names._this || select.name == Names._super) {
			this.result = this.makeThis(((Tree) select).pos, select.selected.type.tsym, true);
		} else {
			this.result = this.access(select.sym, select, this.enclOp, flag);
		}
	}

	public List translateTopLevelClass(final Env env, final Tree tree) {
		this.attrEnv = env;
		this.currentClass = null;
		this.outermostClassDef = tree;
		this.outermostMemberDef = null;
		this.translated = new ListBuffer();
		this.classdefs = Hashtable.make();
		this.actualSymbols = Hashtable.make();
		this.freevarCache = Hashtable.make();
		this.proxies = new Scope(null);
		this.outerThisStack = VarSymbol.emptyList;
		this.accessNums = Hashtable.make();
		this.accessSyms = Hashtable.make();
		this.accessConstrs = Hashtable.make();
		this.accessed = new ListBuffer();
		this.translate(tree, null);
		for (List list = this.accessed.toList(); list.nonEmpty(); list = list.tail) {
			this.makeAccessible((Symbol) list.head);
		}

		return this.translated.toList();
	}

	private final Log log;
	private final Symtab syms;
	private final Resolve rs;
	private final Check chk;
	private final Attr attr;
	private final TreeMaker make;
	private ClassSymbol currentClass;
	private ListBuffer translated;
	private Env attrEnv;
	Hashtable classdefs;
	private Hashtable actualSymbols;
	private Tree outermostClassDef;
	private Tree outermostMemberDef;
	Hashtable freevarCache;
	private static final int NCODES = accessCode(275) + 2;
	private Hashtable accessNums;
	private Hashtable accessSyms;
	private Hashtable accessConstrs;
	private ListBuffer accessed;
	private Scope proxies;
	private List outerThisStack;
	private Tree enclOp;

}
