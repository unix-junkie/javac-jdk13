package sun.tools.asm;

final class Cover {
	Cover(final int i, final long l, final int j) {
		this.Type = i;
		this.Addr = l;
		this.NumCommand = j;
	}

	public final int Type;
	final long Addr;
	final int NumCommand;
}
