package sun.tools.tree;

import sun.tools.java.AmbiguousMember;
import sun.tools.java.ClassDefinition;
import sun.tools.java.ClassNotFound;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.Identifier;
import sun.tools.java.MemberDefinition;

public class Context {

	public Context(final Context context, final MemberDefinition memberdefinition) {
		this.field = memberdefinition;
		if (context == null) {
			this.frameNumber = 1;
			this.scopeNumber = 2;
			this.varNumber = 0;
		} else {
			this.prev = context;
			this.locals = context.locals;
			this.classes = context.classes;
			if (memberdefinition != null && (memberdefinition.isVariable() || memberdefinition.isInitializer())) {
				this.frameNumber = context.frameNumber;
				this.scopeNumber = context.scopeNumber + 1;
			} else {
				this.frameNumber = context.scopeNumber + 1;
				this.scopeNumber = this.frameNumber + 1;
			}
			this.varNumber = context.varNumber;
		}
	}

	public Context(final Context context, final ClassDefinition classdefinition) {
		this(context, (MemberDefinition) null);
	}

	Context(final Context context, final Node node1) {
		if (context == null) {
			this.frameNumber = 1;
			this.scopeNumber = 2;
			this.varNumber = 0;
		} else {
			this.prev = context;
			this.locals = context.locals;
			this.classes = context.classes;
			this.varNumber = context.varNumber;
			this.field = context.field;
			this.frameNumber = context.frameNumber;
			this.scopeNumber = context.scopeNumber + 1;
			this.node = node1;
		}
	}

	public Context(final Context context) {
		this(context, (Node) null);
	}

	public int declare(final Environment environment, final LocalMember localmember) {
		localmember.scopeNumber = this.scopeNumber;
		if (this.field == null && Constants.idThis.equals(localmember.getName())) {
			localmember.scopeNumber++;
		}
		if (localmember.isInnerClass()) {
			localmember.prev = this.classes;
			this.classes = localmember;
			return 0;
		}
		localmember.prev = this.locals;
		this.locals = localmember;
		localmember.number = this.varNumber;
		this.varNumber += localmember.getType().stackSize();
		return localmember.number;
	}

	public LocalMember getLocalField(final Identifier identifier) {
		for (LocalMember localmember = this.locals; localmember != null; localmember = localmember.prev) {
			if (identifier.equals(localmember.getName())) {
				return localmember;
			}
		}

		return null;
	}

	public int getScopeNumber(final ClassDefinition classdefinition) {
		for (Context context = this; context != null; context = context.prev) {
			if (context.field != null && context.field.getClassDefinition() == classdefinition) {
				return context.frameNumber;
			}
		}

		return -1;
	}

	private MemberDefinition getFieldCommon(final Environment environment, final Identifier identifier, final boolean flag) throws AmbiguousMember, ClassNotFound {
		final LocalMember localmember = this.getLocalField(identifier);
		final int i = localmember != null ? localmember.scopeNumber : -2;
		final ClassDefinition classdefinition = this.field.getClassDefinition();
		for (ClassDefinition classdefinition1 = classdefinition; classdefinition1 != null; classdefinition1 = classdefinition1.getOuterClass()) {
			final MemberDefinition memberdefinition = classdefinition1.getVariable(environment, identifier, classdefinition);
			if (memberdefinition != null && this.getScopeNumber(classdefinition1) > i && (!flag || memberdefinition.getClassDefinition() == classdefinition1)) {
				return memberdefinition;
			}
		}

		return localmember;
	}

	public int declareFieldNumber(final MemberDefinition memberdefinition) {
		return this.declare(null, new LocalMember(memberdefinition));
	}

	public int getFieldNumber(final MemberDefinition memberdefinition) {
		for (LocalMember localmember = this.locals; localmember != null; localmember = localmember.prev) {
			if (localmember.getMember() == memberdefinition) {
				return localmember.number;
			}
		}

		return -1;
	}

	public MemberDefinition getElement(final int i) {
		for (LocalMember localmember = this.locals; localmember != null; localmember = localmember.prev) {
			if (localmember.number == i) {
				final MemberDefinition memberdefinition = localmember.getMember();
				return memberdefinition == null ? localmember : memberdefinition;
			}
		}

		return null;
	}

	public LocalMember getLocalClass(final Identifier identifier) {
		for (LocalMember localmember = this.classes; localmember != null; localmember = localmember.prev) {
			if (identifier.equals(localmember.getName())) {
				return localmember;
			}
		}

		return null;
	}

	private MemberDefinition getClassCommon(final Environment environment, final Identifier identifier, final boolean flag) throws ClassNotFound {
		final LocalMember localmember = this.getLocalClass(identifier);
		final int i = localmember != null ? localmember.scopeNumber : -2;
		for (ClassDefinition classdefinition = this.field.getClassDefinition(); classdefinition != null; classdefinition = classdefinition.getOuterClass()) {
			final MemberDefinition memberdefinition = classdefinition.getInnerClass(environment, identifier);
			if (memberdefinition != null && this.getScopeNumber(classdefinition) > i && (!flag || memberdefinition.getClassDefinition() == classdefinition)) {
				return memberdefinition;
			}
		}

		return localmember;
	}

	public final MemberDefinition getField(final Environment environment, final Identifier identifier) throws AmbiguousMember, ClassNotFound {
		return this.getFieldCommon(environment, identifier, false);
	}

	public final MemberDefinition getApparentField(final Environment environment, final Identifier identifier) throws AmbiguousMember, ClassNotFound {
		return this.getFieldCommon(environment, identifier, true);
	}

	private boolean isInScope(final LocalMember localmember) {
		for (LocalMember localmember1 = this.locals; localmember1 != null; localmember1 = localmember1.prev) {
			if (localmember == localmember1) {
				return true;
			}
		}

		return false;
	}

	public UplevelReference noteReference(final Environment environment, final LocalMember localmember) {
		final int i = this.isInScope(localmember) ? localmember.scopeNumber : -1;
		UplevelReference uplevelreference = null;
		int j = -1;
		for (Context context = this; context != null; context = context.prev) {
			if (j == context.frameNumber) {
				continue;
			}
			j = context.frameNumber;
			if (i >= j) {
				return uplevelreference;
			}
			final ClassDefinition classdefinition = context.field.getClassDefinition();
			final UplevelReference uplevelreference1 = classdefinition.getReference(localmember);
			uplevelreference1.noteReference(environment, context);
			if (uplevelreference == null) {
				uplevelreference = uplevelreference1;
			}
		}

		return uplevelreference;
	}

	public Expression makeReference(final Environment environment, final LocalMember localmember) {
		final UplevelReference uplevelreference = this.noteReference(environment, localmember);
		if (uplevelreference != null) {
			return uplevelreference.makeLocalReference(environment, this);
		}
		return Constants.idThis.equals(localmember.getName()) ? (Expression) new ThisExpression(0L, localmember) : new IdentifierExpression(0L, localmember);
	}

	public Expression findOuterLink(final Environment environment, final long l, final MemberDefinition memberdefinition) {
		final ClassDefinition classdefinition = memberdefinition.getClassDefinition();
		final ClassDefinition classdefinition1 = memberdefinition.isStatic() ? null : memberdefinition.isConstructor() ? classdefinition.isTopLevel() ? null : classdefinition.getOuterClass() : classdefinition;
		return classdefinition1 == null ? null : this.findOuterLink(environment, l, classdefinition1, memberdefinition, false);
	}

	private static boolean match(final Environment environment, final ClassDefinition classdefinition, final ClassDefinition classdefinition1) {
		try {
			return classdefinition == classdefinition1 || classdefinition1.implementedBy(environment, classdefinition.getClassDeclaration());
		} catch (final ClassNotFound ignored) {
			return false;
		}
	}

	public Expression findOuterLink(final Environment environment, final long l, final ClassDefinition classdefinition, final MemberDefinition memberdefinition, final boolean flag) {
		if (this.field.isStatic()) {
			if (memberdefinition == null) {
				final Identifier identifier = classdefinition.getName().getFlatName().getName();
				environment.error(l, "undef.var", Identifier.lookup(identifier, Constants.idThis));
			} else if (memberdefinition.isConstructor()) {
				environment.error(l, "no.outer.arg", classdefinition, memberdefinition.getClassDeclaration());
			} else if (memberdefinition.isMethod()) {
				environment.error(l, "no.static.meth.access", memberdefinition, memberdefinition.getClassDeclaration());
			} else {
				environment.error(l, "no.static.field.access", memberdefinition.getName(), memberdefinition.getClassDeclaration());
			}
			final Expression thisexpression = new ThisExpression(l, this);
			thisexpression.type = classdefinition.getType();
			return thisexpression;
		}
		LocalMember localmember = this.locals;
		ClassDefinition classdefinition2 = null;
		if (this.field.isConstructor()) {
			classdefinition2 = this.field.getClassDefinition();
		}
		ClassDefinition classdefinition1 = null;
		Object obj = null;
		if (!this.field.isMethod()) {
			classdefinition1 = this.field.getClassDefinition();
			obj = new ThisExpression(l, this);
		}
		do {
			if (obj == null) {
				for (; localmember != null && !Constants.idThis.equals(localmember.getName()); localmember = localmember.prev) {
				}
				if (localmember == null) {
					break;
				}
				obj = new ThisExpression(l, localmember);
				classdefinition1 = localmember.getClassDefinition();
				localmember = localmember.prev;
			}
			if (classdefinition1 == classdefinition || !flag && match(environment, classdefinition1, classdefinition)) {
				break;
			}
			final MemberDefinition memberdefinition1 = classdefinition1.findOuterMember();
			if (memberdefinition1 == null) {
				obj = null;
			} else {
				final ClassDefinition classdefinition3 = classdefinition1;
				classdefinition1 = classdefinition3.getOuterClass();
				if (classdefinition3 == classdefinition2) {
					final Identifier identifier2 = memberdefinition1.getName();
					final IdentifierExpression identifierexpression = new IdentifierExpression(l, identifier2);
					identifierexpression.bind(environment, this);
					obj = identifierexpression;
				} else {
					obj = new FieldExpression(l, (Expression) obj, memberdefinition1);
				}
			}
		} while (true);
		if (obj != null) {
			return (Expression) obj;
		}
		if (memberdefinition == null) {
			final Identifier identifier1 = classdefinition.getName().getFlatName().getName();
			environment.error(l, "undef.var", Identifier.lookup(identifier1, Constants.idThis));
		} else if (memberdefinition.isConstructor()) {
			environment.error(l, "no.outer.arg", classdefinition, memberdefinition.getClassDefinition());
		} else {
			environment.error(l, "no.static.field.access", memberdefinition, this.field);
		}
		final Expression thisexpression1 = new ThisExpression(l, this);
		thisexpression1.type = classdefinition.getType();
		return thisexpression1;
	}

	public static boolean outerLinkExists(final Environment environment, final ClassDefinition classdefinition, ClassDefinition classdefinition1) {
		for (; !match(environment, classdefinition1, classdefinition); classdefinition1 = classdefinition1.getOuterClass()) {
			if (classdefinition1.isTopLevel()) {
				return false;
			}
		}

		return true;
	}

	public ClassDefinition findScope(final Environment environment, final ClassDefinition classdefinition) {
		ClassDefinition classdefinition1;
		for (classdefinition1 = this.field.getClassDefinition(); classdefinition1 != null && !match(environment, classdefinition1, classdefinition); classdefinition1 = classdefinition1.getOuterClass()) {
		}
		return classdefinition1;
	}

	Identifier resolveName(final Environment environment, final Identifier identifier) {
		if (identifier.isQualified()) {
			final Identifier identifier1 = this.resolveName(environment, identifier.getHead());
			if (identifier1.hasAmbigPrefix()) {
				return identifier1;
			}
			if (!environment.classExists(identifier1)) {
				return environment.resolvePackageQualifiedName(identifier);
			}
			try {
				return environment.getClassDefinition(identifier1).resolveInnerClass(environment, identifier.getTail());
			} catch (final ClassNotFound ignored) {
				return Identifier.lookupInner(identifier1, identifier.getTail());
			}
		}
		try {
			final MemberDefinition memberdefinition = this.getClassCommon(environment, identifier, false);
			if (memberdefinition != null) {
				return memberdefinition.getInnerClass().getName();
			}
		} catch (final ClassNotFound ignored) {
		}
		return environment.resolveName(identifier);
	}

	public Identifier getApparentClassName(final Environment environment, final Identifier identifier) {
		if (identifier.isQualified()) {
			final Identifier identifier1 = this.getApparentClassName(environment, identifier.getHead());
			return identifier1 != null ? Identifier.lookup(identifier1, identifier.getTail()) : Constants.idNull;
		}
		try {
			final MemberDefinition memberdefinition = this.getClassCommon(environment, identifier, true);
			if (memberdefinition != null) {
				return memberdefinition.getInnerClass().getName();
			}
		} catch (final ClassNotFound ignored) {
		}
		final Identifier identifier2 = this.field.getClassDefinition().getTopClass().getName();
		return identifier2.getName().equals(identifier) ? identifier2 : Constants.idNull;
	}

	public void checkBackBranch(final Environment environment, final Statement statement, final Vset vset, final Vset vset1) {
		for (LocalMember localmember = this.locals; localmember != null; localmember = localmember.prev) {
			if (localmember.isBlankFinal() && vset.testVarUnassigned(localmember.number) && !vset1.testVarUnassigned(localmember.number)) {
				environment.error(((Node) statement).where, "assign.to.blank.final.in.loop", localmember.getName());
			}
		}

	}

	public boolean canReach(final Environment environment, final MemberDefinition memberdefinition) {
		return this.field.canReach(environment, memberdefinition);
	}

	public Context getLabelContext(final Identifier identifier) {
		for (Context context = this; context != null; context = context.prev) {
			if (context.node instanceof Statement && ((Statement) context.node).hasLabel(identifier)) {
				return context;
			}
		}

		return null;
	}

	public Context getBreakContext(final Identifier identifier) {
		if (identifier != null) {
			return this.getLabelContext(identifier);
		}
		for (Context context = this; context != null; context = context.prev) {
			if (context.node != null) {
				switch (context.node.op) {
				case 92: // '\\'
				case 93: // ']'
				case 94: // '^'
				case 95: // '_'
					return context;
				}
			}
		}

		return null;
	}

	public Context getContinueContext(final Identifier identifier) {
		if (identifier != null) {
			return this.getLabelContext(identifier);
		}
		for (Context context = this; context != null; context = context.prev) {
			if (context.node != null) {
				switch (context.node.op) {
				case 92: // '\\'
				case 93: // ']'
				case 94: // '^'
					return context;
				}
			}
		}

		return null;
	}

	public CheckContext getReturnContext() {
		for (Context context = this; context != null; context = context.prev) {
			if (context.node != null && context.node.op == 47) {
				return (CheckContext) context;
			}
		}

		return null;
	}

	public CheckContext getTryExitContext() {
		for (Context context = this; context != null && context.node != null && context.node.op != 47; context = context.prev) {
			if (context.node.op == 101) {
				return (CheckContext) context;
			}
		}

		return null;
	}

	Context getInlineContext() {
		for (Context context = this; context != null; context = context.prev) {
			if (context.node != null) {
				switch (context.node.op) {
				case 150:
				case 151:
					return context;
				}
			}
		}

		return null;
	}

	Context getInlineMemberContext(final MemberDefinition memberdefinition) {
		for (Context context = this; context != null; context = context.prev) {
			if (context.node != null) {
				switch (context.node.op) {
				default:
					break;

				case 150:
					if (((InlineMethodExpression) context.node).field.equals(memberdefinition)) {
						return context;
					}
					break;

				case 151:
					if (((InlineNewInstanceExpression) context.node).field.equals(memberdefinition)) {
						return context;
					}
					break;
				}
			}
		}

		return null;
	}

	public final Vset removeAdditionalVars(final Vset vset) {
		return vset.removeAdditionalVars(this.varNumber);
	}

	public final int getVarNumber() {
		return this.varNumber;
	}

	public int getThisNumber() {
		final LocalMember localmember = this.getLocalField(Constants.idThis);
		return localmember != null && localmember.getClassDefinition() == this.field.getClassDefinition() ? localmember.number : this.varNumber;
	}

	public final MemberDefinition getField() {
		return this.field;
	}

	public static Environment newEnvironment(final Environment environment, final Context context) {
		return new ContextEnvironment(environment, context);
	}

	Context prev;
	Node node;
	int varNumber;
	private LocalMember locals;
	private LocalMember classes;
	MemberDefinition field;
	private final int scopeNumber;
	final int frameNumber;
}
