package sun.tools.asm;

import java.util.Enumeration;
import java.util.Hashtable;

public final class SwitchData {

	public SwitchData() {
		this.defaultLabel = new Label();
		this.tab = new Hashtable();
		this.whereCaseTab = null;
	}

	public Label get(final int i) {
		return (Label) this.tab.get(new Integer(i));
	}

	public Label get(final Integer integer) {
		return (Label) this.tab.get(integer);
	}

	public void add(final int i, final Label label) {
		if (this.tab.isEmpty()) {
			this.minValue = i;
			this.maxValue = i;
		} else {
			if (i < this.minValue) {
				this.minValue = i;
			}
			if (i > this.maxValue) {
				this.maxValue = i;
			}
		}
		this.tab.put(new Integer(i), label);
	}

	public Instruction getDefaultLabel() {
		return this.defaultLabel;
	}

	synchronized Enumeration sortedKeys() {
		return new SwitchDataEnumeration(this.tab);
	}

	public void initTableCase() {
		this.whereCaseTab = new Hashtable();
	}

	public void addTableCase(final int i, final long l) {
		if (this.whereCaseTab != null) {
			this.whereCaseTab.put(new Integer(i), new Long(l));
		}
	}

	public void addTableDefault(final long l) {
		if (this.whereCaseTab != null) {
			this.whereCaseTab.put("default", new Long(l));
		}
	}

	public long whereCase(final Object obj) {
		final Number long1 = (Number) this.whereCaseTab.get(obj);
		return long1 != null ? long1.longValue() : 0L;
	}

	public boolean getDefault() {
		return this.whereCase("default") != 0L;
	}

	int minValue;
	int maxValue;
	Label defaultLabel;
	final Hashtable tab;
	private Hashtable whereCaseTab;
}
