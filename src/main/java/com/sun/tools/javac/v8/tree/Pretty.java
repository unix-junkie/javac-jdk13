package com.sun.tools.javac.v8.tree;

import java.io.PrintStream;

import com.sun.tools.javac.v8.code.Symbol;
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
import com.sun.tools.javac.v8.tree.Tree.TypeTest;
import com.sun.tools.javac.v8.tree.Tree.VarDef;
import com.sun.tools.javac.v8.tree.Tree.Visitor;
import com.sun.tools.javac.v8.tree.Tree.WhileLoop;
import com.sun.tools.javac.v8.util.Convert;
import com.sun.tools.javac.v8.util.Hashtable;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.Name;
import com.sun.tools.javac.v8.util.Names;

public final class Pretty extends Visitor {

	public Pretty(final PrintStream printstream) {
		this.width = 3;
		this.lmargin = 0;
		this.enclClassName = Names.empty;
		this.docComments = null;
		this.out = printstream;
	}

	public Pretty() {
		this(System.out);
	}

	private void align() {
		for (int i = 0; i < this.lmargin; i++) {
			this.out.print(" ");
		}

	}

	private void indent() {
		this.lmargin += this.width;
	}

	private void undent() {
		this.lmargin -= this.width;
	}

	private void open(final int i, final int j) {
		if (j < i) {
			this.out.print("(");
		}
	}

	private void close(final int i, final int j) {
		if (j < i) {
			this.out.print(")");
		}
	}

	private void print(final String s) {
		this.out.print(Convert.escapeUnicode(s));
	}

	private void println() {
		this.out.println();
	}

	private void printExpr(final Tree tree, final int i) {
		if (tree == null) {
			this.print("/*missing*/");
		} else {
			final int j = this.prec;
			this.prec = i;
			tree.visit(this);
			this.prec = j;
		}
	}

	public void printExpr(final Tree tree) {
		this.printExpr(tree, 0);
	}

	private void printStat(final Tree tree) {
		this.printExpr(tree, -1);
	}

	private void printExprs(final List list) {
		if (list.nonEmpty()) {
			this.printExpr((Tree) list.head);
			for (List list1 = list.tail; list1.nonEmpty(); list1 = list1.tail) {
				this.print(", ");
				this.printExpr((Tree) list1.head);
			}

		}
	}

	private void printStats(final List list) {
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			this.align();
			this.printStat((Tree) list1.head);
			this.println();
		}

	}

	private void printFlags(final int i) {
		if ((i & 0x10000) != 0) {
			this.print("/*synthetic*/ ");
		}
		this.print(TreeInfo.flagNames(i));
		if ((i & 0xfff) != 0) {
			this.print(" ");
		}
	}

	private void printDocComment(final Tree tree) {
		final String s = (String) this.docComments.get(tree);
		if (s != null) {
			this.print("/**");
			this.println();
			int i = 0;
			for (int j = lineEndPos(s, i); i < s.length(); j = lineEndPos(s, i)) {
				this.align();
				this.print(" *");
				if (i < s.length() && s.charAt(i) > ' ') {
					this.print(" ");
				}
				this.print(s.substring(i, j));
				this.println();
				i = j + 1;
			}

			this.align();
			this.print(" */");
			this.println();
			this.align();
		}
	}

	private static int lineEndPos(final String s, final int i) {
		final int j = s.indexOf('\n', i);
		if (j < 0) {
			return s.length();
		}
		return j;
	}

	private void printBlock(final List list) {
		this.print("{");
		this.println();
		this.indent();
		this.printStats(list);
		this.undent();
		this.align();
		this.print("}");
	}

	public void printUnit(final TopLevel toplevel, final Tree classdef) {
		this.docComments = toplevel.docComments;
		this.printDocComment(toplevel);
		if (toplevel.pid != null) {
			this.print("package ");
			this.printExpr(toplevel.pid);
			this.print(";");
			this.println();
		}
		for (List list = toplevel.defs; list.nonEmpty() && (classdef == null || ((Tree) list.head).tag == 2); list = list.tail) {
			this.printStat((Tree) list.head);
			this.println();
		}

		if (classdef != null) {
			this.printStat(classdef);
			this.println();
		}
	}

	public void _case(final TopLevel toplevel) {
		this.printUnit(toplevel, null);
	}

	public void _case(final Import import1) {
		this.print("import ");
		this.printExpr(import1.qualid);
		this.print(";");
		this.println();
	}

	public void _case(final ClassDef classdef) {
		this.println();
		this.align();
		this.printDocComment(classdef);
		this.printFlags(classdef.flags & 0xfffffdff);
		final Name name = this.enclClassName;
		this.enclClassName = classdef.name;
		if ((classdef.flags & 0x200) != 0) {
			this.print("interface " + classdef.name);
			if (classdef.implementing.nonEmpty()) {
				this.print(" extends ");
				this.printExprs(classdef.implementing);
			}
		} else {
			this.print("class " + classdef.name);
			if (classdef.extending != null) {
				this.print(" extends ");
				this.printExpr(classdef.extending);
			}
			if (classdef.implementing.nonEmpty()) {
				this.print(" implements ");
				this.printExprs(classdef.implementing);
			}
		}
		this.print(" ");
		this.printBlock(classdef.defs);
		this.enclClassName = name;
	}

	public void _case(final MethodDef methoddef) {
		if (methoddef.name != Names.init || this.enclClassName != null) {
			this.println();
			this.align();
			this.printDocComment(methoddef);
			this.printFlags(methoddef.flags);
			if (methoddef.name == Names.init) {
				this.print(this.enclClassName.toString());
			} else {
				this.printExpr(methoddef.restype);
				this.print(" " + methoddef.name);
			}
			this.print("(");
			this.printExprs(methoddef.params);
			this.print(")");
			if (methoddef.thrown.nonEmpty()) {
				this.print(" throws ");
				this.printExprs(methoddef.thrown);
			}
			if (methoddef.body != null) {
				this.print(" ");
				this.printStat(methoddef.body);
			} else {
				this.print(";");
			}
		}
	}

	public void _case(final VarDef vardef) {
		if (this.docComments.get(vardef) != null) {
			this.println();
			this.align();
		}
		this.printDocComment(vardef);
		this.printFlags(vardef.flags);
		this.printExpr(vardef.vartype);
		this.print(" " + vardef.name);
		if (vardef.init != null) {
			this.print(" = ");
			this.printExpr(vardef.init);
		}
		if (this.prec == -1) {
			this.print(";");
		}
	}

	public void _case(final Block block) {
		this.printFlags(block.flags);
		this.printBlock(block.stats);
	}

	public void _case(final DoLoop doloop) {
		this.print("do ");
		this.printStat(doloop.body);
		this.align();
		this.print(" while (");
		this.printExpr(doloop.cond);
		this.print(");");
	}

	public void _case(final WhileLoop whileloop) {
		this.print("while (");
		this.printExpr(whileloop.cond);
		this.print(") ");
		this.printStat(whileloop.body);
	}

	public void _case(final ForLoop forloop) {
		this.print("for (");
		if (forloop.init.nonEmpty()) {
			if (((Tree) forloop.init.head).tag == 5) {
				this.printExpr((Tree) forloop.init.head);
				for (List list = forloop.init.tail; list.nonEmpty(); list = list.tail) {
					final VarDef vardef = (VarDef) list.head;
					this.print(", " + vardef.name + " = ");
					this.printExpr(vardef.init);
				}

			} else {
				this.printExprs(forloop.init);
			}
		}
		this.print("; ");
		if (forloop.cond != null) {
			this.printExpr(forloop.cond);
		}
		this.print("; ");
		this.printExprs(forloop.step);
		this.print(") ");
		this.printStat(forloop.body);
	}

	public void _case(final Labelled labelled) {
		this.print(labelled.label + ": ");
		this.printStat(labelled.body);
	}

	public void _case(final Switch switch1) {
		this.print("switch (");
		this.printExpr(switch1.selector);
		this.print(") {\n");
		this.printStats(switch1.cases);
		this.align();
		this.print("}");
	}

	public void _case(final Case case1) {
		if (case1.pat == null) {
			this.print("default");
		} else {
			this.print("case ");
			this.printExpr(case1.pat);
		}
		this.print(": \n");
		this.indent();
		this.printStats(case1.stats);
		this.undent();
		this.align();
	}

	public void _case(final Synchronized synchronized1) {
		this.print("synchronized (");
		this.printExpr(synchronized1.lock);
		this.print(") ");
		this.printStat(synchronized1.body);
	}

	public void _case(final Try try1) {
		this.print("try ");
		this.printBlock(Tree.emptyList.prepend(try1.body));
		this.print(" ");
		for (List list = try1.catchers; list.nonEmpty(); list = list.tail) {
			this.printStat((Tree) list.head);
		}

		if (try1.finalizer != null) {
			this.print(" finally ");
			this.printBlock(Tree.emptyList.prepend(try1.finalizer));
		}
	}

	public void _case(final Catch catch1) {
		this.print(" catch (");
		this.printExpr(catch1.param);
		this.print(") ");
		this.printBlock(Tree.emptyList.prepend(catch1.body));
	}

	public void _case(final Conditional conditional) {
		if (((Tree) conditional).tag == 17) {
			this.print("if (");
			this.printExpr(conditional.cond);
			this.print(") ");
			this.printStat(conditional.thenpart);
			if (conditional.elsepart != null) {
				this.print(" else ");
				this.printStat(conditional.elsepart);
			}
		} else {
			this.open(this.prec, 3);
			this.printExpr(conditional.cond, 3);
			this.print(" ? ");
			this.printExpr(conditional.thenpart, 3);
			this.print(" : ");
			this.printExpr(conditional.elsepart, 3);
			this.close(this.prec, 3);
		}
	}

	public void _case(final Exec exec) {
		this.printExpr(exec.expr);
		if (this.prec == -1) {
			this.print(";");
		}
	}

	public void _case(final Break break1) {
		this.print("break");
		if (break1.label != null) {
			this.print(" " + break1.label);
		}
		this.print(";");
	}

	public void _case(final Continue continue1) {
		this.print("continue");
		if (continue1.label != null) {
			this.print(" " + continue1.label);
		}
		this.print(";");
	}

	public void _case(final Return return1) {
		this.print("return");
		if (return1.expr != null) {
			this.print(" ");
			this.printExpr(return1.expr);
		}
		this.print(";");
	}

	public void _case(final Throw throw1) {
		this.print("throw ");
		this.printExpr(throw1.expr);
		this.print(";");
	}

	public void _case(final Apply apply) {
		this.printExpr(apply.meth);
		this.print("(");
		this.printExprs(apply.args);
		this.print(")");
	}

	public void _case(final NewClass newclass) {
		if (newclass.encl != null) {
			this.printExpr(newclass.encl);
			this.print(".");
		}
		this.print("new ");
		this.printExpr(newclass.clazz);
		this.print("(");
		this.printExprs(newclass.args);
		this.print(")");
		if (newclass.def != null) {
			final Name name = this.enclClassName;
			this.enclClassName = null;
			this.printBlock(newclass.def.defs);
			this.enclClassName = name;
		}
	}

	public void _case(final NewArray newarray) {
		if (newarray.elemtype != null) {
			this.print("new ");
			int i = 0;
			Tree tree;
			for (tree = newarray.elemtype; tree.tag == 34; tree = ((TypeArray) tree).elemtype) {
				i++;
			}

			this.printExpr(tree);
			for (List list = newarray.dims; list.nonEmpty(); list = list.tail) {
				this.print("[");
				this.printExpr((Tree) list.head);
				this.print("]");
			}

			for (int j = 0; j < i; j++) {
				this.print("[]");
			}

			if (newarray.elems != null) {
				this.print("[]");
			}
		}
		if (newarray.elems != null) {
			this.print("{");
			this.printExprs(newarray.elems);
			this.print("}");
		}
	}

	public void _case(final Assign assign) {
		this.open(this.prec, 1);
		this.printExpr(assign.lhs, 2);
		this.print(" = ");
		this.printExpr(assign.rhs, 1);
		this.close(this.prec, 1);
	}

	public void _case(final Assignop assignop) {
		this.open(this.prec, 2);
		this.printExpr(assignop.lhs, 3);
		this.print(" " + TreeInfo.operatorName(((Tree) assignop).tag - 17) + "= ");
		this.printExpr(assignop.rhs, 2);
		this.close(this.prec, 2);
	}

	public void _case(final Operation operation) {
		final int i = TreeInfo.opPrec(((Tree) operation).tag);
		final String s = TreeInfo.operatorName(((Tree) operation).tag).toString();
		this.open(this.prec, i);
		if (((Tree) operation).tag <= 43) {
			this.print(s);
			this.printExpr((Tree) operation.args.head, i);
		} else if (((Tree) operation).tag <= 45) {
			this.printExpr((Tree) operation.args.head, i);
			this.print(s);
		} else {
			this.printExpr((Tree) operation.args.head, i);
			this.print(' ' + s + ' ');
			this.printExpr((Tree) operation.args.tail.head, i + 1);
		}
		this.close(this.prec, i);
	}

	public void _case(final TypeCast typecast) {
		this.open(this.prec, 14);
		this.print("(");
		this.printExpr(typecast.clazz);
		this.print(")");
		this.printExpr(typecast.expr, 14);
		this.close(this.prec, 14);
	}

	public void _case(final TypeTest typetest) {
		this.open(this.prec, 10);
		this.printExpr(typetest.expr, 10);
		this.print(" instanceof ");
		this.printExpr(typetest.clazz, 11);
		this.close(this.prec, 10);
	}

	public void _case(final Indexed indexed) {
		this.printExpr(indexed.indexed, 15);
		this.print("[");
		this.printExpr(indexed.index);
		this.print("]");
	}

	public void _case(final Select select) {
		this.printExpr(select.selected, 15);
		this.print("." + select.name);
	}

	public void _case(final Ident ident) {
		this.print(ident.name.toString());
	}

	public void _case(final Literal literal) {
		switch (literal.typetag) {
		case 4: // '\004'
			this.print(literal.value.toString());
			break;

		case 5: // '\005'
			this.print(literal.value + "L");
			break;

		case 6: // '\006'
			this.print(literal.value + "F");
			break;

		case 7: // '\007'
			this.print(literal.value.toString());
			break;

		case 2: // '\002'
			this.print('\'' + Convert.quote(String.valueOf((char) ((Number) literal.value).intValue())) + '\'');
			break;

		case 10: // '\n'
			this.print('"' + Convert.quote((String) literal.value) + '"');
			break;

		case 3: // '\003'
		case 8: // '\b'
		case 9: // '\t'
		default:
			this.print(literal.value.toString());
			break;
		}
	}

	public void _case(final TypeIdent typeident) {
		this.print(((Symbol) Type.typeOfTag[typeident.typetag].tsym).name.toString());
	}

	public void _case(final TypeArray typearray) {
		this.printExpr(typearray.elemtype);
		this.print("[]");
	}

	public void _case(final Erroneous erroneous) {
		this.print("(ERROR)");
	}

	public void _case(final Tree tree) {
		this.print("(UNKNOWN: " + tree + ')');
		this.println();
	}

	private final PrintStream out;
	private final int width;
	private int lmargin;
	private Name enclClassName;
	private Hashtable docComments;
	private int prec;
}
