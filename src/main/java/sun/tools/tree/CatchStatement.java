package sun.tools.tree;

import java.io.PrintStream;
import java.util.Hashtable;

import sun.tools.asm.Assembler;
import sun.tools.asm.LocalVariable;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassNotFound;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.IdentifierToken;
import sun.tools.java.Type;

public final class CatchStatement extends Statement {

	public CatchStatement(final long l, final Expression expression, final IdentifierToken identifiertoken, final Statement statement) {
		super(102, l);
		this.mod = identifiertoken.getModifiers();
		this.texpr = expression;
		this.id = identifiertoken.getName();
		this.body = statement;
	}

	Vset check(final Environment environment, Context context, Vset vset, final Hashtable hashtable) {
		vset = this.reach(environment, vset);
		context = new Context(context, this);
		final Type type = this.texpr.toType(environment, context);
		try {
			if (context.getLocalField(this.id) != null) {
				environment.error(this.where, "local.redefined", this.id);
			}
			if (!type.isType(13)) {
				if (!type.isType(10)) {
					environment.error(this.where, "catch.not.throwable", type);
				} else {
					final ClassDefinition classdefinition = environment.getClassDefinition(type);
					if (!classdefinition.subClassOf(environment, environment.getClassDeclaration(Constants.idJavaLangThrowable))) {
						environment.error(this.where, "catch.not.throwable", classdefinition);
					}
				}
			}
			this.field = new LocalMember(this.where, context.field.getClassDefinition(), this.mod, type, this.id);
			context.declare(environment, this.field);
			vset.addVar(this.field.number);
			return this.body.check(environment, context, vset, hashtable);
		} catch (final ClassNotFound classnotfound) {
			environment.error(this.where, "class.not.found", classnotfound.name, Constants.opNames[this.op]);
		}
		return vset;
	}

	public Statement inline(final Environment environment, Context context) {
		context = new Context(context, this);
		if (this.field.isUsed()) {
			context.declare(environment, this.field);
		}
		if (this.body != null) {
			this.body = this.body.inline(environment, context);
		}
		return this;
	}

	public Statement copyInline(final Context context, final boolean flag) {
		final CatchStatement catchstatement = (CatchStatement) this.clone();
		if (this.body != null) {
			catchstatement.body = this.body.copyInline(context, flag);
		}
		if (this.field != null) {
			catchstatement.field = this.field.copyInline(context);
		}
		return catchstatement;
	}

	public int costInline(final int i, final Environment environment, final Context context) {
		int j = 1;
		if (this.body != null) {
			j += this.body.costInline(i, environment, context);
		}
		return j;
	}

	public void code(final Environment environment, final Context context, final Assembler assembler) {
		final Context codecontext = new CodeContext(context, this);
		if (this.field.isUsed()) {
			codecontext.declare(environment, this.field);
			assembler.add(this.where, 58, new LocalVariable(this.field, this.field.number));
		} else {
			assembler.add(this.where, 87);
		}
		if (this.body != null) {
			this.body.code(environment, codecontext, assembler);
		}
	}

	public void print(final PrintStream printstream, final int i) {
		super.print(printstream, i);
		printstream.print("catch (");
		this.texpr.print(printstream);
		printstream.print(" " + this.id + ") ");
		if (this.body != null) {
			this.body.print(printstream, i);
		} else {
			printstream.print("<empty>");
		}
	}

	private final int mod;
	private final Expression texpr;
	private final Identifier id;
	private Statement body;
	LocalMember field;
}
