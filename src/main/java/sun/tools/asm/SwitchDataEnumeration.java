package sun.tools.asm;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

final class SwitchDataEnumeration implements Enumeration {

	SwitchDataEnumeration(final Map m) {
		this.current_index = 0;
		this.table = new Integer[m.size()];
		int i = 0;
		for (final Iterator it = m.keySet().iterator(); it.hasNext();) {
			this.table[i++] = (Integer) it.next();
		}

		Arrays.sort(this.table);
		this.current_index = 0;
	}

	public boolean hasMoreElements() {
		return this.current_index < this.table.length;
	}

	public Object nextElement() {
		return this.table[this.current_index++];
	}

	private final Integer table[];
	private int current_index;
}
