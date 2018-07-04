package com.sun.tools.javac.v8.tree;

import com.sun.tools.javac.v8.code.Scope;
import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.code.Symbol.ClassSymbol;
import com.sun.tools.javac.v8.code.Symbol.MethodSymbol;
import com.sun.tools.javac.v8.code.Symbol.PackageSymbol;
import com.sun.tools.javac.v8.code.Symbol.VarSymbol;
import com.sun.tools.javac.v8.code.Type;
import com.sun.tools.javac.v8.util.Hashtable;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.Name;

public abstract class Tree {
	public abstract static class Visitor {

		protected void _case(final TopLevel toplevel) {
			this._case((Tree) toplevel);
		}

		protected void _case(final Import import1) {
			this._case((Tree) import1);
		}

		protected void _case(final ClassDef classdef) {
			this._case((Tree) classdef);
		}

		protected void _case(final MethodDef methoddef) {
			this._case((Tree) methoddef);
		}

		protected void _case(final VarDef vardef) {
			this._case((Tree) vardef);
		}

		protected void _case(final Block block) {
			this._case((Tree) block);
		}

		protected void _case(final DoLoop doloop) {
			this._case((Tree) doloop);
		}

		protected void _case(final WhileLoop whileloop) {
			this._case((Tree) whileloop);
		}

		protected void _case(final ForLoop forloop) {
			this._case((Tree) forloop);
		}

		protected void _case(final Labelled labelled) {
			this._case((Tree) labelled);
		}

		protected void _case(final Switch switch1) {
			this._case((Tree) switch1);
		}

		void _case(final Case case1) {
			this._case((Tree) case1);
		}

		protected void _case(final Synchronized synchronized1) {
			this._case((Tree) synchronized1);
		}

		protected void _case(final Try try1) {
			this._case((Tree) try1);
		}

		void _case(final Catch catch1) {
			this._case((Tree) catch1);
		}

		protected void _case(final Conditional conditional) {
			this._case((Tree) conditional);
		}

		protected void _case(final Exec exec) {
			this._case((Tree) exec);
		}

		protected void _case(final Break break1) {
			this._case((Tree) break1);
		}

		protected void _case(final Continue continue1) {
			this._case((Tree) continue1);
		}

		protected void _case(final Return return1) {
			this._case((Tree) return1);
		}

		protected void _case(final Throw throw1) {
			this._case((Tree) throw1);
		}

		protected void _case(final Apply apply) {
			this._case((Tree) apply);
		}

		protected void _case(final NewClass newclass) {
			this._case((Tree) newclass);
		}

		protected void _case(final NewArray newarray) {
			this._case((Tree) newarray);
		}

		protected void _case(final Assign assign) {
			this._case((Tree) assign);
		}

		protected void _case(final Assignop assignop) {
			this._case((Tree) assignop);
		}

		protected void _case(final Operation operation) {
			this._case((Tree) operation);
		}

		protected void _case(final TypeCast typecast) {
			this._case((Tree) typecast);
		}

		protected void _case(final TypeTest typetest) {
			this._case((Tree) typetest);
		}

		protected void _case(final Indexed indexed) {
			this._case((Tree) indexed);
		}

		public void _case(final Select select) {
			this._case((Tree) select);
		}

		protected void _case(final Ident ident) {
			this._case((Tree) ident);
		}

		protected void _case(final Literal literal) {
			this._case((Tree) literal);
		}

		protected void _case(final TypeIdent typeident) {
			this._case((Tree) typeident);
		}

		public void _case(final TypeArray typearray) {
			this._case((Tree) typearray);
		}

		protected void _case(final Erroneous erroneous) {
			this._case((Tree) erroneous);
		}

		public void _case(final Tree tree) {
			throw new InternalError("unexpected: " + tree);
		}

	}

	public interface Factory {

		TopLevel TopLevel(Tree tree, List list);

		Import Import(Tree tree);

		ClassDef ClassDef(int i, Name name, List list, Tree tree, List list1, List list2);

		MethodDef MethodDef(int i, Name name, Tree tree, List list, List list1, List list2, Block block);

		VarDef VarDef(int i, Name name, Tree tree, Tree tree1);

		Block Block(int i, List list);

		DoLoop DoLoop(Tree tree, Tree tree1);

		WhileLoop WhileLoop(Tree tree, Tree tree1);

		ForLoop ForLoop(List list, Tree tree, List list1, Tree tree1);

		Labelled Labelled(Name name, Tree tree);

		Switch Switch(Tree tree, List list);

		Case Case(Tree tree, List list);

		Synchronized Synchronized(Tree tree, Tree tree1);

		Try Try(Tree tree, List list, Tree tree1);

		Catch Catch(VarDef vardef, Tree tree);

		Conditional Conditional(int i, Tree tree, Tree tree1, Tree tree2);

		Exec Exec(Tree tree);

		Break Break(Name name);

		Continue Continue(Name name);

		Return Return(Tree tree);

		Throw Throw(Tree tree);

		Apply Apply(Tree tree, List list);

		NewClass NewClass(Tree tree, Tree tree1, List list, ClassDef classdef);

		NewArray NewArray(Tree tree, List list, List list1);

		Assign Assign(Tree tree, Tree tree1);

		Assignop Assignop(int i, Tree tree, Tree tree1);

		Operation Operation(int i, List list);

		TypeCast TypeCast(Tree tree, Tree tree1);

		TypeTest TypeTest(Tree tree, Tree tree1);

		Indexed Indexed(Tree tree, Tree tree1);

		Select Select(Tree tree, Name name);

		Ident Ident(Name name);

		Literal Literal(int i, Object obj);

		TypeIdent TypeIdent(int i);

		TypeArray TypeArray(Tree tree);

		Erroneous Erroneous();
	}

	public static class Erroneous extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Erroneous() {
			super(37);
		}
	}

	public static final class TypeParameter {
		public static final List emptyList = new List();

		private TypeParameter() {
		}
	}

	public static class TypeArray extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree elemtype;

		public TypeArray(final Tree tree) {
			super(34);
			this.elemtype = tree;
		}
	}

	public static class TypeIdent extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public final int typetag;

		public TypeIdent(final int i) {
			super(33);
			this.typetag = i;
		}
	}

	public static class Literal extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public final int typetag;
		public final Object value;

		public Literal(final int i, final Object obj) {
			super(32);
			this.typetag = i;
			this.value = obj;
		}
	}

	public static class Ident extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Name name;
		public Symbol sym;

		public Ident(final Name name1, final Symbol symbol) {
			super(31);
			this.name = name1;
			this.sym = symbol;
		}
	}

	public static class Select extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree selected;
		public Name name;
		public Symbol sym;

		public Select(final Tree tree, final Name name1, final Symbol symbol) {
			super(30);
			this.selected = tree;
			this.name = name1;
			this.sym = symbol;
		}
	}

	public static class Indexed extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree indexed;
		public Tree index;

		public Indexed(final Tree tree, final Tree tree1) {
			super(29);
			this.indexed = tree;
			this.index = tree1;
		}
	}

	public static class TypeTest extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree expr;
		public Tree clazz;

		public TypeTest(final Tree tree, final Tree tree1) {
			super(28);
			this.expr = tree;
			this.clazz = tree1;
		}
	}

	public static class TypeCast extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree clazz;
		public Tree expr;

		public TypeCast(final Tree tree, final Tree tree1) {
			super(27);
			this.clazz = tree;
			this.expr = tree1;
		}
	}

	public static class Operation extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public List args;
		public Symbol operator;

		public Operation(final int i, final List list, final Symbol symbol) {
			super(i);
			this.args = list;
			this.operator = symbol;
		}
	}

	public static class Assignop extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree lhs;
		public Tree rhs;
		public Symbol operator;

		public Assignop(final int i, final Tree tree, final Tree tree1, final Symbol symbol) {
			super(i);
			this.lhs = tree;
			this.rhs = tree1;
			this.operator = symbol;
		}
	}

	public static class Assign extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree lhs;
		public Tree rhs;

		public Assign(final Tree tree, final Tree tree1) {
			super(26);
			this.lhs = tree;
			this.rhs = tree1;
		}
	}

	public static class NewArray extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree elemtype;
		public List dims;
		public List elems;

		public NewArray(final Tree tree, final List list, final List list1) {
			super(25);
			this.elemtype = tree;
			this.dims = list;
			this.elems = list1;
		}
	}

	public static class NewClass extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree encl;
		public Tree clazz;
		public List args;
		public ClassDef def;
		public Symbol constructor;

		public NewClass(final Tree tree, final Tree tree1, final List list, final ClassDef classdef, final Symbol symbol) {
			super(24);
			this.encl = tree;
			this.clazz = tree1;
			this.args = list;
			this.def = classdef;
			this.constructor = symbol;
		}
	}

	public static class Apply extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree meth;
		public List args;

		public Apply(final Tree tree, final List list) {
			super(23);
			this.meth = tree;
			this.args = list;
		}
	}

	public static class Throw extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree expr;

		public Throw(final Tree tree) {
			super(22);
			this.expr = tree;
		}
	}

	public static class Return extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree expr;

		public Return(final Tree tree) {
			super(21);
			this.expr = tree;
		}
	}

	public static class Continue extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public final Name label;
		public Tree target;

		public Continue(final Name name, final Tree tree) {
			super(20);
			this.label = name;
			this.target = tree;
		}
	}

	public static class Break extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public final Name label;
		public Tree target;

		public Break(final Name name, final Tree tree) {
			super(19);
			this.label = name;
			this.target = tree;
		}
	}

	public static class Exec extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree expr;

		public Exec(final Tree tree) {
			super(18);
			this.expr = tree;
		}
	}

	public static class Conditional extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree cond;
		public Tree thenpart;
		public Tree elsepart;

		public Conditional(final int i, final Tree tree, final Tree tree1, final Tree tree2) {
			super(i);
			this.cond = tree;
			this.thenpart = tree1;
			this.elsepart = tree2;
		}
	}

	public static class Catch extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public VarDef param;
		public Tree body;
		public static final List emptyList = new List();

		public Catch(final VarDef vardef, final Tree tree) {
			super(15);
			this.param = vardef;
			this.body = tree;
		}
	}

	public static class Try extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree body;
		public List catchers;
		public Tree finalizer;

		public Try(final Tree tree, final List list, final Tree tree1) {
			super(14);
			this.body = tree;
			this.catchers = list;
			this.finalizer = tree1;
		}
	}

	public static class Synchronized extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree lock;
		public Tree body;

		public Synchronized(final Tree tree, final Tree tree1) {
			super(13);
			this.lock = tree;
			this.body = tree1;
		}
	}

	public static class Case extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree pat;
		public List stats;

		public Case(final Tree tree, final List list) {
			super(12);
			this.pat = tree;
			this.stats = list;
		}
	}

	public static class Switch extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree selector;
		public List cases;

		public Switch(final Tree tree, final List list) {
			super(11);
			this.selector = tree;
			this.cases = list;
		}
	}

	public static class Labelled extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public final Name label;
		public Tree body;

		public Labelled(final Name name, final Tree tree) {
			super(10);
			this.label = name;
			this.body = tree;
		}
	}

	public static class ForLoop extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public List init;
		public Tree cond;
		public List step;
		public Tree body;

		public ForLoop(final List list, final Tree tree, final List list1, final Tree tree1) {
			super(9);
			this.init = list;
			this.cond = tree;
			this.step = list1;
			this.body = tree1;
		}
	}

	public static class WhileLoop extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree cond;
		public Tree body;

		public WhileLoop(final Tree tree, final Tree tree1) {
			super(8);
			this.cond = tree;
			this.body = tree1;
		}
	}

	public static class DoLoop extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree body;
		public Tree cond;

		public DoLoop(final Tree tree, final Tree tree1) {
			super(7);
			this.body = tree;
			this.cond = tree1;
		}
	}

	public static class Block extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public final int flags;
		public List stats;
		public int endpos;

		public Block(final int i, final List list) {
			super(6);
			this.endpos = 0;
			this.stats = list;
			this.flags = i;
		}
	}

	public static class VarDef extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public final int flags;
		public final Name name;
		public Tree vartype;
		public Tree init;
		public VarSymbol sym;
		public static final List emptyList = new List();

		public VarDef(final int i, final Name name1, final Tree tree, final Tree tree1, final VarSymbol varsymbol) {
			super(5);
			this.flags = i;
			this.name = name1;
			this.vartype = tree;
			this.init = tree1;
			this.sym = varsymbol;
		}
	}

	public static class MethodDef extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public final int flags;
		public final Name name;
		public Tree restype;
		public List typarams;
		public List params;
		public List thrown;
		public Block body;
		public MethodSymbol sym;

		public MethodDef(final int i, final Name name1, final Tree tree, final List list, final List list1, final List list2, final Block block, final MethodSymbol methodsymbol) {
			super(4);
			this.flags = i;
			this.name = name1;
			this.restype = tree;
			this.typarams = list;
			this.params = list1;
			this.thrown = list2;
			this.body = block;
			this.sym = methodsymbol;
		}
	}

	public static class ClassDef extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public int flags;
		public Name name;
		public List typarams;
		public Tree extending;
		public List implementing;
		public List defs;
		public ClassSymbol sym;

		public ClassDef(final int i, final Name name1, final List list, final Tree tree, final List list1, final List list2, final ClassSymbol classsymbol) {
			super(3);
			this.flags = i;
			this.name = name1;
			this.typarams = list;
			this.extending = tree;
			this.implementing = list1;
			this.defs = list2;
			this.sym = classsymbol;
		}
	}

	public static class Import extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree qualid;

		public Import(final Tree tree) {
			super(2);
			this.qualid = tree;
		}
	}

	public static class TopLevel extends Tree {

		public void visit(final Visitor visitor) {
			visitor._case(this);
		}

		public Tree pid;
		public List defs;
		public Name sourcefile;
		public PackageSymbol packge;
		public Scope namedImportScope;
		public Scope starImportScope;
		public Hashtable docComments;

		public TopLevel(final Tree tree, final List list, final Name name, final PackageSymbol packagesymbol, final Scope scope, final Scope scope1) {
			super(1);
			this.docComments = null;
			this.pid = tree;
			this.defs = list;
			this.sourcefile = name;
			this.packge = packagesymbol;
			this.namedImportScope = scope;
			this.starImportScope = scope1;
		}
	}

	Tree(final int i) {
		this.tag = i;
	}

	public Tree setPos(final int i) {
		this.pos = i;
		return this;
	}

	public Tree setType(final Type type1) {
		this.type = type1;
		return this;
	}

	public void visit(final Visitor visitor) {
		visitor._case(this);
	}

	public static final int TOPLEVEL = 1;
	public static final int IMPORT = 2;
	public static final int CLASSDEF = 3;
	public static final int METHODDEF = 4;
	public static final int VARDEF = 5;
	public static final int BLOCK = 6;
	public static final int DOLOOP = 7;
	public static final int WHILELOOP = 8;
	public static final int FORLOOP = 9;
	public static final int LABELLED = 10;
	public static final int SWITCH = 11;
	public static final int CASE = 12;
	public static final int SYNCHRONIZED = 13;
	public static final int TRY = 14;
	public static final int CATCH = 15;
	public static final int CONDEXPR = 16;
	public static final int CONDSTAT = 17;
	public static final int EXEC = 18;
	public static final int BREAK = 19;
	public static final int CONTINUE = 20;
	public static final int RETURN = 21;
	public static final int THROW = 22;
	public static final int APPLY = 23;
	public static final int NEWCLASS = 24;
	public static final int NEWARRAY = 25;
	public static final int ASSIGN = 26;
	public static final int TYPECAST = 27;
	public static final int TYPETEST = 28;
	public static final int INDEXED = 29;
	public static final int SELECT = 30;
	public static final int IDENT = 31;
	public static final int LITERAL = 32;
	public static final int TYPEIDENT = 33;
	public static final int TYPEARRAY = 34;
	public static final int TYPEAPPLY = 35;
	public static final int TYPEPARAMETER = 36;
	public static final int ERRONEOUS = 37;
	public static final int POS = 38;
	public static final int NEG = 39;
	public static final int NOT = 40;
	public static final int COMPL = 41;
	public static final int PREINC = 42;
	public static final int PREDEC = 43;
	public static final int POSTINC = 44;
	public static final int POSTDEC = 45;
	public static final int OR = 46;
	public static final int AND = 47;
	public static final int BITOR = 48;
	public static final int BITXOR = 49;
	public static final int BITAND = 50;
	public static final int EQ = 51;
	public static final int NE = 52;
	public static final int LT = 53;
	public static final int GT = 54;
	public static final int LE = 55;
	public static final int GE = 56;
	public static final int SL = 57;
	public static final int SR = 58;
	public static final int USR = 59;
	public static final int PLUS = 60;
	public static final int MINUS = 61;
	public static final int MUL = 62;
	public static final int DIV = 63;
	public static final int MOD = 64;
	public static final int BITOR_ASG = 65;
	public static final int BITXOR_ASG = 66;
	public static final int BITAND_ASG = 67;
	public static final int SL_ASG = 74;
	public static final int SR_ASG = 75;
	public static final int USR_ASG = 76;
	public static final int PLUS_ASG = 77;
	public static final int MINUS_ASG = 78;
	public static final int MUL_ASG = 79;
	public static final int DIV_ASG = 80;
	public static final int MOD_ASG = 81;
	public static final int ASGOffset = 17;
	public int pos;
	public Type type;
	public int tag;
	public static final List emptyList = new List();
}
