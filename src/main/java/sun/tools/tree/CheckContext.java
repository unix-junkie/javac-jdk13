package sun.tools.tree;

public final class CheckContext extends Context {

	CheckContext(final Context context, final Node statement) {
		super(context, statement);
		this.vsBreak = Vset.DEAD_END;
		this.vsContinue = Vset.DEAD_END;
		this.vsTryExit = Vset.DEAD_END;
	}

	public Vset vsBreak;
	Vset vsContinue;
	Vset vsTryExit;
}
