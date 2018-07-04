package com.sun.tools.javac.v8.tree;

import com.sun.tools.javac.v8.code.Scope.Entry;
import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.MethodSymbol;
import com.sun.tools.javac.v8.code.Symbol.VarSymbol;
import com.sun.tools.javac.v8.code.Type;
import com.sun.tools.javac.v8.tree.Tree.Apply;
import com.sun.tools.javac.v8.tree.Tree.Assign;
import com.sun.tools.javac.v8.tree.Tree.Assignop;
import com.sun.tools.javac.v8.tree.Tree.Block;
import com.sun.tools.javac.v8.tree.Tree.Break;
import com.sun.tools.javac.v8.tree.Tree.Case;
import com.sun.tools.javac.v8.tree.Tree.Catch;
import com.sun.tools.javac.v8.tree.Tree.ClassDef;
import com.sun.tools.javac.v8.tree.Tree.Conditional;
import com.sun.tools.javac.v8.tree.Tree.Continue;
import com.sun.tools.javac.v8.tree.Tree.DoLoop;
import com.sun.tools.javac.v8.tree.Tree.Erroneous;
import com.sun.tools.javac.v8.tree.Tree.Exec;
import com.sun.tools.javac.v8.tree.Tree.Factory;
import com.sun.tools.javac.v8.tree.Tree.ForLoop;
import com.sun.tools.javac.v8.tree.Tree.Ident;
import com.sun.tools.javac.v8.tree.Tree.Import;
import com.sun.tools.javac.v8.tree.Tree.Indexed;
import com.sun.tools.javac.v8.tree.Tree.Labelled;
import com.sun.tools.javac.v8.tree.Tree.Literal;
import com.sun.tools.javac.v8.tree.Tree.MethodDef;
import com.sun.tools.javac.v8.tree.Tree.NewArray;
import com.sun.tools.javac.v8.tree.Tree.NewClass;
import com.sun.tools.javac.v8.tree.Tree.Operation;
import com.sun.tools.javac.v8.tree.Tree.Return;
import com.sun.tools.javac.v8.tree.Tree.Select;
import com.sun.tools.javac.v8.tree.Tree.Switch;
import com.sun.tools.javac.v8.tree.Tree.Synchronized;
import com.sun.tools.javac.v8.tree.Tree.Throw;
import com.sun.tools.javac.v8.tree.Tree.TopLevel;
import com.sun.tools.javac.v8.tree.Tree.Try;
import com.sun.tools.javac.v8.tree.Tree.TypeArray;
import com.sun.tools.javac.v8.tree.Tree.TypeCast;
import com.sun.tools.javac.v8.tree.Tree.TypeIdent;
import com.sun.tools.javac.v8.tree.Tree.TypeParameter;
import com.sun.tools.javac.v8.tree.Tree.TypeTest;
import com.sun.tools.javac.v8.tree.Tree.VarDef;
import com.sun.tools.javac.v8.tree.Tree.WhileLoop;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.ListBuffer;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;

public final class TreeMaker implements Factory {

	public TreeMaker() {
		this.pos = 0;
		this.pos = 0;
		this.toplevel = null;
	}

	public TreeMaker(final TopLevel toplevel1) {
		this.pos = 0;
		this.pos = 1025;
		this.toplevel = toplevel1;
	}

	public TreeMaker at(final int i) {
		this.pos = i;
		return this;
	}

	public TopLevel TopLevel(final Tree tree, final List list) {
		final TopLevel toplevel1 = new TopLevel(tree, list, null, null, null, null);
		toplevel1.pos = this.pos;
		return toplevel1;
	}

	public Import Import(final Tree tree) {
		final Import import1 = new Import(tree);
		import1.pos = this.pos;
		return import1;
	}

	public ClassDef ClassDef(final int i, final Name name, final List list, final Tree tree, final List list1, final List list2) {
		final ClassDef classdef = new ClassDef(i, name, list, tree, list1, list2, null);
		classdef.pos = this.pos;
		return classdef;
	}

	public MethodDef MethodDef(final int i, final Name name, final Tree tree, final List list, final List list1, final List list2, final Block block) {
		final MethodDef methoddef = new MethodDef(i, name, tree, list, list1, list2, block, null);
		methoddef.pos = this.pos;
		return methoddef;
	}

	public VarDef VarDef(final int i, final Name name, final Tree tree, final Tree tree1) {
		final VarDef vardef = new VarDef(i, name, tree, tree1, null);
		vardef.pos = this.pos;
		return vardef;
	}

	public Block Block(final int i, final List list) {
		final Block block = new Block(i, list);
		block.pos = this.pos;
		return block;
	}

	public DoLoop DoLoop(final Tree tree, final Tree tree1) {
		final DoLoop doloop = new DoLoop(tree, tree1);
		doloop.pos = this.pos;
		return doloop;
	}

	public WhileLoop WhileLoop(final Tree tree, final Tree tree1) {
		final WhileLoop whileloop = new WhileLoop(tree, tree1);
		whileloop.pos = this.pos;
		return whileloop;
	}

	public ForLoop ForLoop(final List list, final Tree tree, final List list1, final Tree tree1) {
		final ForLoop forloop = new ForLoop(list, tree, list1, tree1);
		forloop.pos = this.pos;
		return forloop;
	}

	public Labelled Labelled(final Name name, final Tree tree) {
		final Labelled labelled = new Labelled(name, tree);
		labelled.pos = this.pos;
		return labelled;
	}

	public Switch Switch(final Tree tree, final List list) {
		final Switch switch1 = new Switch(tree, list);
		switch1.pos = this.pos;
		return switch1;
	}

	public Case Case(final Tree tree, final List list) {
		final Case case1 = new Case(tree, list);
		case1.pos = this.pos;
		return case1;
	}

	public Synchronized Synchronized(final Tree tree, final Tree tree1) {
		final Synchronized synchronized1 = new Synchronized(tree, tree1);
		synchronized1.pos = this.pos;
		return synchronized1;
	}

	public Try Try(final Tree tree, final List list, final Tree tree1) {
		final Try try1 = new Try(tree, list, tree1);
		try1.pos = this.pos;
		return try1;
	}

	public Catch Catch(final VarDef vardef, final Tree tree) {
		final Catch catch1 = new Catch(vardef, tree);
		catch1.pos = this.pos;
		return catch1;
	}

	public Conditional Conditional(final int i, final Tree tree, final Tree tree1, final Tree tree2) {
		final Conditional conditional = new Conditional(i, tree, tree1, tree2);
		conditional.pos = this.pos;
		return conditional;
	}

	public Exec Exec(final Tree tree) {
		final Exec exec = new Exec(tree);
		exec.pos = this.pos;
		return exec;
	}

	public Break Break(final Name name) {
		final Break break1 = new Break(name, null);
		break1.pos = this.pos;
		return break1;
	}

	public Continue Continue(final Name name) {
		final Continue continue1 = new Continue(name, null);
		continue1.pos = this.pos;
		return continue1;
	}

	public Return Return(final Tree tree) {
		final Return return1 = new Return(tree);
		return1.pos = this.pos;
		return return1;
	}

	public Throw Throw(final Tree tree) {
		final Throw throw1 = new Throw(tree);
		throw1.pos = this.pos;
		return throw1;
	}

	public Apply Apply(final Tree tree, final List list) {
		final Apply apply = new Apply(tree, list);
		apply.pos = this.pos;
		return apply;
	}

	public NewClass NewClass(final Tree tree, final Tree tree1, final List list, final ClassDef classdef) {
		final NewClass newclass = new NewClass(tree, tree1, list, classdef, null);
		newclass.pos = this.pos;
		return newclass;
	}

	public NewArray NewArray(final Tree tree, final List list, final List list1) {
		final NewArray newarray = new NewArray(tree, list, list1);
		newarray.pos = this.pos;
		return newarray;
	}

	public Assign Assign(final Tree tree, final Tree tree1) {
		final Assign assign = new Assign(tree, tree1);
		assign.pos = this.pos;
		return assign;
	}

	public Assignop Assignop(final int i, final Tree tree, final Tree tree1) {
		final Assignop assignop = new Assignop(i, tree, tree1, null);
		assignop.pos = this.pos;
		return assignop;
	}

	public Operation Operation(final int i, final List list) {
		final Operation operation = new Operation(i, list, null);
		operation.pos = this.pos;
		return operation;
	}

	public TypeCast TypeCast(final Tree tree, final Tree tree1) {
		final TypeCast typecast = new TypeCast(tree, tree1);
		typecast.pos = this.pos;
		return typecast;
	}

	public TypeTest TypeTest(final Tree tree, final Tree tree1) {
		final TypeTest typetest = new TypeTest(tree, tree1);
		typetest.pos = this.pos;
		return typetest;
	}

	public Indexed Indexed(final Tree tree, final Tree tree1) {
		final Indexed indexed = new Indexed(tree, tree1);
		indexed.pos = this.pos;
		return indexed;
	}

	public Select Select(final Tree tree, final Name name) {
		final Select select = new Select(tree, name, null);
		select.pos = this.pos;
		return select;
	}

	public Ident Ident(final Name name) {
		final Ident ident = new Ident(name, null);
		ident.pos = this.pos;
		return ident;
	}

	public Literal Literal(final int i, final Object obj) {
		final Literal literal = new Literal(i, obj);
		literal.pos = this.pos;
		return literal;
	}

	public TypeIdent TypeIdent(final int i) {
		final TypeIdent typeident = new TypeIdent(i);
		typeident.pos = this.pos;
		return typeident;
	}

	public TypeArray TypeArray(final Tree tree) {
		final TypeArray typearray = new TypeArray(tree);
		typearray.pos = this.pos;
		return typearray;
	}

	public Erroneous Erroneous() {
		final Erroneous erroneous = new Erroneous();
		erroneous.pos = this.pos;
		return erroneous;
	}

	public Tree Ident(final Symbol symbol) {
		return new Ident(symbol.name, symbol).setPos(this.pos).setType(symbol.type);
	}

	public Tree Select(final Tree tree, final Symbol symbol) {
		return new Select(tree, symbol.name, symbol).setPos(this.pos).setType(symbol.type);
	}

	public Tree QualIdent(final Symbol symbol) {
		return this.isUnqualifiable(symbol) ? this.Ident(symbol) : this.Select(this.QualIdent(symbol.owner), symbol);
	}

	public Tree Ident(final VarDef vardef) {
		return this.Ident(vardef.sym);
	}

	public List Idents(final List list) {
		final ListBuffer listbuffer = new ListBuffer();
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			listbuffer.append(this.Ident((VarDef) list1.head));
		}

		return listbuffer.toList();
	}

	public Tree This(final Type type) {
		return this.Ident(new VarSymbol(16, Names._this, type, type.tsym));
	}

	public Tree Super(final Type type) {
		return this.Ident(new VarSymbol(16, Names._super, type, type.tsym));
	}

	public Tree App(final Tree tree, final List list) {
		final Type type = tree.type;
		tree.type = type.restype();
		return this.Apply(tree, list).setType(type.restype());
	}

	public Tree Type(final Type type) {
		if (type == null) {
			return null;
		}
		final Object obj;
		switch (type.tag) {
		case 1: // '\001'
		case 2: // '\002'
		case 3: // '\003'
		case 4: // '\004'
		case 5: // '\005'
		case 6: // '\006'
		case 7: // '\007'
		case 8: // '\b'
		case 9: // '\t'
			obj = this.TypeIdent(type.tag);
			break;

		case 14: // '\016'
			obj = this.Ident(((Symbol) type.tsym).name);
			break;

		case 10: // '\n'
			final Type type1 = type.outer();
			obj = type1.tag != 10 || ((Symbol) type.tsym).owner.kind != 2 ? this.QualIdent(type.tsym) : this.Select(this.Type(type1), type.tsym);
			break;

		case 11: // '\013'
			obj = this.TypeArray(this.Type(type.elemtype()));
			break;

		case 18: // '\022'
			obj = this.TypeIdent(18);
			break;

		case 12: // '\f'
		case 13: // '\r'
		case 15: // '\017'
		case 16: // '\020'
		case 17: // '\021'
		default:
			throw new InternalError("unexpected type: " + type);
		}
		return ((Tree) obj).setType(type);
	}

	public List Types(final List list) {
		final ListBuffer listbuffer = new ListBuffer();
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			listbuffer.append(this.Type((Type) list1.head));
		}

		return listbuffer.toList();
	}

	private List Classes(final List list) {
		final ListBuffer listbuffer = new ListBuffer();
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			listbuffer.append(this.Type(((Symbol) (ClassSymbol) list1.head).type.erasure()));
		}

		return listbuffer.toList();
	}

	public VarDef VarDef(final VarSymbol varsymbol, final Tree tree) {
		return (VarDef) new VarDef(varsymbol.flags(), ((Symbol) varsymbol).name, this.Type(((Symbol) varsymbol).type), tree, varsymbol).setPos(this.pos).setType(((Symbol) varsymbol).type);
	}

	public MethodDef MethodDef(final MethodSymbol methodsymbol, final Block block) {
		return this.MethodDef(methodsymbol, ((Symbol) methodsymbol).type, block);
	}

	public MethodDef MethodDef(final MethodSymbol methodsymbol, final Type type, final Block block) {
		return (MethodDef) new MethodDef(methodsymbol.flags(), ((Symbol) methodsymbol).name, this.Type(type.restype()), TypeParameter.emptyList, this.Params(type.argtypes(), methodsymbol), this.Classes(type.thrown()), block, methodsymbol).setPos(this.pos).setType(type);
	}

	private VarDef Param(final Name name, final Type type, final Symbol symbol) {
		return this.VarDef(new VarSymbol(0, name, type, symbol), null);
	}

	public List Params(final List list, final Symbol symbol) {
		final ListBuffer listbuffer = new ListBuffer();
		int i = 0;
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			listbuffer.append(this.Param(paramName(i++), (Type) list1.head, symbol));
		}

		return listbuffer.toList();
	}

	public Tree Call(final Tree tree) {
		return tree.type.tag != 9 ? (Tree) this.Return(tree) : (Tree) this.Exec(tree);
	}

	public Tree Assignment(final Symbol symbol, final Tree tree) {
		return this.Exec(this.Assign(this.Ident(symbol), tree).setType(symbol.type));
	}

	private boolean isUnqualifiable(final Symbol symbol) {
		if (symbol.owner == null || symbol.owner.kind == 16 || symbol.owner.kind == 4 || symbol.owner.name == Names.empty) {
			return true;
		}
		if (symbol.kind == 2 && this.toplevel != null) {
			Entry entry = this.toplevel.namedImportScope.lookup(symbol.name);
			if (entry.scope != null) {
				return entry.scope.owner == entry.sym.owner && entry.sym == symbol && entry.next().scope == null;
			}
			entry = this.toplevel.packge.members().lookup(symbol.name);
			if (entry.scope != null) {
				return entry.scope.owner == entry.sym.owner && entry.sym == symbol && entry.next().scope == null;
			}
			entry = this.toplevel.starImportScope.lookup(symbol.name);
			if (entry.scope != null) {
				return entry.scope.owner == entry.sym.owner && entry.sym == symbol && entry.next().scope == null;
			}
		}
		return false;
	}

	public static Name paramName(final int i) {
		return Name.fromString("x" + i);
	}

	public int pos;
	private final TopLevel toplevel;
}
