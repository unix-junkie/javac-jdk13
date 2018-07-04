package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;

import sun.tools.asm.Assembler;
import sun.tools.asm.CatchData;
import sun.tools.asm.TryData;
import sun.tools.java.ClassDeclaration;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassNotFound;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Type;

public final class TryStatement extends Statement {

	public TryStatement(final long l, final Statement statement, final Statement astatement[]) {
		super(101, l);
		this.body = statement;
		this.args = astatement;
	}

	Vset check(final Environment environment, final Context context, Vset vset, final Hashtable hashtable) {
		this.checkLabel(environment, context);
		try {
			vset = this.reach(environment, vset);
			final Hashtable hashtable1 = new Hashtable();
			final CheckContext checkcontext = new CheckContext(context, this);
			Vset vset1 = this.body.check(environment, checkcontext, vset.copy(), hashtable1);
			final Vset vset2 = Vset.firstDAandSecondDU(vset, vset1.copy().join(checkcontext.vsTryExit));
			for (int i = 0; i < this.args.length; i++) {
				vset1 = vset1.join(this.args[i].check(environment, checkcontext, vset2.copy(), hashtable));
			}

			for (int j = 1; j < this.args.length; j++) {
				final CatchStatement catchstatement = (CatchStatement) this.args[j];
				if (catchstatement.field != null) {
					final Type type = catchstatement.field.getType();
					final ClassDefinition classdefinition = environment.getClassDefinition(type);
					for (int l = 0; l < j; l++) {
						final CatchStatement catchstatement2 = (CatchStatement) this.args[l];
						if (catchstatement2.field == null) {
							continue;
						}
						final Type type2 = catchstatement2.field.getType();
						final ClassDeclaration classdeclaration3 = environment.getClassDeclaration(type2);
						if (!classdefinition.subClassOf(environment, classdeclaration3)) {
							continue;
						}
						environment.error(((Node) this.args[j]).where, "catch.not.reached");
						break;
					}

				}
			}

			final ClassDeclaration classdeclaration = environment.getClassDeclaration(Constants.idJavaLangError);
			final ClassDeclaration classdeclaration1 = environment.getClassDeclaration(Constants.idJavaLangRuntimeException);
			for (int k = 0; k < this.args.length; k++) {
				final CatchStatement catchstatement1 = (CatchStatement) this.args[k];
				if (catchstatement1.field != null) {
					final Type type1 = catchstatement1.field.getType();
					if (type1.isType(10)) {
						final ClassDefinition classdefinition1 = environment.getClassDefinition(type1);
						if (!classdefinition1.subClassOf(environment, classdeclaration) && !classdefinition1.superClassOf(environment, classdeclaration) && !classdefinition1.subClassOf(environment, classdeclaration1) && !classdefinition1.superClassOf(environment, classdeclaration1)) {
							boolean flag = false;
							for (final Iterator iterator = hashtable1.keySet().iterator(); iterator.hasNext();) {
								final ClassDeclaration classdeclaration4 = (ClassDeclaration) iterator.next();
								if (classdefinition1.superClassOf(environment, classdeclaration4) || classdefinition1.subClassOf(environment, classdeclaration4)) {
									flag = true;
									break;
								}
							}

							if (!flag && this.arrayCloneWhere != 0L && classdefinition1.getName().toString().equals("java.lang.CloneNotSupportedException")) {
								environment.error(this.arrayCloneWhere, "warn.array.clone.supported", classdefinition1.getName());
							}
							if (!flag) {
								environment.error(((Node) catchstatement1).where, "catch.not.thrown", classdefinition1.getName());
							}
						}
					}
				}
			}

			for (final Iterator iterator = hashtable1.keySet().iterator(); iterator.hasNext();) {
				final ClassDeclaration classdeclaration2 = (ClassDeclaration) iterator.next();
				final ClassDefinition classdefinition2 = classdeclaration2.getClassDefinition(environment);
				boolean flag1 = true;
				for (int i1 = 0; i1 < this.args.length; i1++) {
					final CatchStatement catchstatement3 = (CatchStatement) this.args[i1];
					if (catchstatement3.field == null) {
						continue;
					}
					final Type type3 = catchstatement3.field.getType();
					if (type3.isType(13) || !classdefinition2.subClassOf(environment, environment.getClassDeclaration(type3))) {
						continue;
					}
					flag1 = false;
					break;
				}

				if (flag1) {
					hashtable.put(classdeclaration2, hashtable1.get(classdeclaration2));
				}
			}

			return context.removeAdditionalVars(vset1.join(checkcontext.vsBreak));
		} catch (final ClassNotFound classnotfound) {
			environment.error(this.where, "class.not.found", classnotfound.name, Constants.opNames[this.op]);
		}
		return vset;
	}

	public Statement inline(final Environment environment, final Context context) {
		if (this.body != null) {
			this.body = this.body.inline(environment, new Context(context, this));
		}
		if (this.body == null) {
			return null;
		}
		for (int i = 0; i < this.args.length; i++) {
			if (this.args[i] != null) {
				this.args[i] = this.args[i].inline(environment, new Context(context, this));
			}
		}

		return this.args.length != 0 ? this : this.eliminate(environment, this.body);
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final TryStatement trystatement = (TryStatement) this.clone();
		if (this.body != null) {
			trystatement.body = this.body.copyInline(context, flag);
		}
		trystatement.args = new Statement[this.args.length];
		for (int i = 0; i < this.args.length; i++) {
			if (this.args[i] != null) {
				trystatement.args[i] = this.args[i].copyInline(context, flag);
			}
		}

		return trystatement;
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		final CodeContext codecontext = new CodeContext(context, this);
		final TryData trydata = new TryData();
		for (int i = 0; i < this.args.length; i++) {
			final Type type = ((CatchStatement) this.args[i]).field.getType();
			if (type.isType(10)) {
				trydata.add(environment.getClassDeclaration(type));
			} else {
				trydata.add(type);
			}
		}

		assembler.add(this.where, -3, trydata);
		if (this.body != null) {
			this.body.code(environment, codecontext, assembler);
		}
		assembler.add(trydata.getEndLabel());
		assembler.add(this.where, 167, codecontext.breakLabel);
		for (int j = 0; j < this.args.length; j++) {
			final CatchData catchdata = trydata.getCatch(j);
			assembler.add(catchdata.getLabel());
			this.args[j].code(environment, codecontext, assembler);
			assembler.add(this.where, 167, codecontext.breakLabel);
		}

		assembler.add(codecontext.breakLabel);
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("try ");
		if (this.body != null) {
			this.body.print(printstream, i);
		} else {
			printstream.print("<empty>");
		}
		for (int j = 0; j < this.args.length; j++) {
			printstream.print(" ");
			this.args[j].print(printstream, i);
		}

	}

	private Statement body;
	private Statement[] args;
	long arrayCloneWhere;
}
