package com.sun.tools.javac.v8.comp;

import com.sun.tools.javac.v8.code.Scope;

final class AttrContext {

	AttrContext() {
		this.scope = null;
		this.staticLevel = 0;
		this.isSelfCall = false;
		this.selectSuper = false;
	}

	AttrContext dup(final Scope scope1) {
		final AttrContext attrcontext = new AttrContext();
		attrcontext.scope = scope1;
		attrcontext.staticLevel = this.staticLevel;
		attrcontext.isSelfCall = this.isSelfCall;
		attrcontext.selectSuper = this.selectSuper;
		return attrcontext;
	}

	AttrContext dup() {
		return this.dup(this.scope);
	}

	Scope scope;
	int staticLevel;
	boolean isSelfCall;
	boolean selectSuper;
}
