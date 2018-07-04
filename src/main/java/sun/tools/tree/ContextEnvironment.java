package sun.tools.tree;

import sun.tools.java.Environment;
import sun.tools.java.Identifier;

final class ContextEnvironment extends Environment {

	ContextEnvironment(final Environment environment, final Context context) {
		super(environment, environment.getSource());
		this.ctx = context;
		this.innerEnv = environment;
	}

	public Identifier resolveName(final Identifier identifier) {
		return this.ctx.resolveName(this.innerEnv, identifier);
	}

	private final Context ctx;
	private final Environment innerEnv;
}
