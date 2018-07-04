package sun.tools.tree;

import sun.tools.asm.Label;

final class CodeContext extends Context {

	CodeContext(final Context context, final Node node) {
		super(context, node);
		switch (node.op) {
		case 92: // '\\'
		case 93: // ']'
		case 94: // '^'
		case 103: // 'g'
		case 126: // '~'
			this.breakLabel = new Label();
			this.contLabel = new Label();
			break;

		case 95: // '_'
		case 101: // 'e'
		case 150:
		case 151:
			this.breakLabel = new Label();
			break;

		default:
			if (node instanceof Statement && ((Statement) node).labels != null) {
				this.breakLabel = new Label();
			}
			break;
		}
	}

	Label breakLabel;
	Label contLabel;
}
