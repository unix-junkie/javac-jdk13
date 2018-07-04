package sun.tools.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class MethodSet {
	public int size() {
		return this.count;
	}

	public void add(final MemberDefinition memberdefinition) {
		if (this.frozen) {
			throw new CompilerError("add()");
		}
		final Identifier identifier = memberdefinition.getName();
		Object obj = this.lookupMap.get(identifier);
		if (obj == null) {
			obj = new ArrayList();
			this.lookupMap.put(identifier, obj);
		}
		final int i = ((Collection) obj).size();
		for (int j = 0; j < i; j++) {
			if (((MemberDefinition) ((List) obj).get(j)).getType().equalArguments(memberdefinition.getType())) {
				throw new CompilerError("duplicate addition");
			}
		}

		((Collection) obj).add(memberdefinition);
		this.count++;
	}

	public void replace(final MemberDefinition memberdefinition) {
		if (this.frozen) {
			throw new CompilerError("replace()");
		}
		final Identifier identifier = memberdefinition.getName();
		Object obj = this.lookupMap.get(identifier);
		if (obj == null) {
			obj = new ArrayList();
			this.lookupMap.put(identifier, obj);
		}
		final int i = ((Collection) obj).size();
		for (int j = 0; j < i; j++) {
			if (((MemberDefinition) ((List) obj).get(j)).getType().equalArguments(memberdefinition.getType())) {
				((List) obj).set(j, memberdefinition);
				return;
			}
		}

		((Collection) obj).add(memberdefinition);
		this.count++;
	}

	MemberDefinition lookupSig(final Identifier identifier, final Type type) {
		for (final Iterator iterator1 = this.lookupName(identifier); iterator1.hasNext();) {
			final MemberDefinition memberdefinition = (MemberDefinition) iterator1.next();
			if (memberdefinition.getType().equalArguments(type)) {
				return memberdefinition;
			}
		}

		return null;
	}

	Iterator lookupName(final Identifier identifier) {
		final Collection list = (Collection) this.lookupMap.get(identifier);
		return list == null ? Collections.EMPTY_LIST.iterator() : list.iterator();
	}

	public Iterator iterator() {
		class MethodIterator implements Iterator {

			public boolean hasNext() {
				if (this.listIter.hasNext()) {
					return true;
				}
				if (this.hashIter.hasNext()) {
					this.listIter = ((Collection) this.hashIter.next()).iterator();
					if (this.listIter.hasNext()) {
						return true;
					}
					throw new CompilerError("iterator() in MethodSet");
				}
				return false;
			}

			public Object next() {
				return this.listIter.next();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			Iterator hashIter;
			Iterator listIter;

			MethodIterator() {
				this.hashIter = MethodSet.this.lookupMap.values().iterator();
				this.listIter = Collections.EMPTY_LIST.iterator();
			}
		}

		return new MethodIterator();
	}

	void freeze() {
		this.frozen = true;
	}

	boolean isFrozen() {
		return this.frozen;
	}

	public String toString() {
		int i = this.size();
		final StringBuffer stringbuffer = new StringBuffer();
		final Iterator iterator1 = this.iterator();
		stringbuffer.append('{');
		while (iterator1.hasNext()) {
			stringbuffer.append(iterator1.next());
			if (--i > 0) {
				stringbuffer.append(", ");
			}
		}
		stringbuffer.append('}');
		return stringbuffer.toString();
	}

	final Map lookupMap = new HashMap();
	private int count;
	private boolean frozen;

}
