package com.sun.tools.javac.v8.comp;

import com.sun.tools.javac.v8.code.Code;
import com.sun.tools.javac.v8.code.Code.Chain;
import com.sun.tools.javac.v8.code.Pool;
import com.sun.tools.javac.v8.code.Symbol;
import com.sun.tools.javac.v8.code.Symbol.VarSymbol;
import com.sun.tools.javac.v8.code.Type;
import com.sun.tools.javac.v8.code.Type.MethodType;
import com.sun.tools.javac.v8.util.Util;

final class Items {
	class CondItem extends Item {

		Item load() {
			Chain chain = null;
			final Chain chain1 = this.jumpFalse();
			if (this.trueJumps != null || this.opcode != 168) {
				Items.this.code.resolve(this.trueJumps);
				Items.this.code.emitop(4);
				chain = Items.this.code.branch(167);
			}
			if (chain1 != null) {
				Items.this.code.resolve(chain1);
				Items.this.code.emitop(3);
			}
			Items.this.code.resolve(chain);
			return Items.this.stackItem[this.typecode];
		}

		void duplicate() {
			this.load().duplicate();
		}

		void drop() {
			this.load().drop();
		}

		void stash(final int i) {
			throw new InternalError("stash");
		}

		CondItem mkCond() {
			return this;
		}

		Chain jumpTrue() {
			return Code.mergeChains(this.trueJumps, Items.this.code.branch(this.opcode));
		}

		Chain jumpFalse() {
			return Code.mergeChains(this.falseJumps, Items.this.code.branch(Code.negate(this.opcode)));
		}

		Item negate() {
			return new CondItem(Code.negate(this.opcode), this.falseJumps, this.trueJumps);
		}

		int width() {
			return -Code.stackdiff[this.opcode];
		}

		final Chain trueJumps;
		final Chain falseJumps;
		final int opcode;

		CondItem(final int i, final Chain chain, final Chain chain1) {
			super(5);
			this.opcode = i;
			this.trueJumps = chain;
			this.falseJumps = chain1;
		}
	}

	class AssignItem extends Item {

		Item load() {
			this.lhs.stash(this.typecode);
			this.lhs.store();
			return Items.this.stackItem[this.typecode];
		}

		void duplicate() {
			this.load().duplicate();
		}

		void drop() {
			this.lhs.store();
		}

		void stash(final int i) {
			throw new InternalError("stash");
		}

		int width() {
			return this.lhs.width() + Code.width(this.typecode);
		}

		final Item lhs;

		AssignItem(final Item item) {
			super(item.typecode);
			this.lhs = item;
		}
	}

	class ImmediateItem extends Item {

		private void ldc() {
			final int i = Items.this.pool.put(this.value);
			if (this.typecode == 1 || this.typecode == 3) {
				Items.this.code.emitop(20, 2);
				Items.this.code.emit2(i);
			} else if (i <= 255) {
				Items.this.code.emitop(18, 1);
				Items.this.code.emit1(i);
			} else {
				Items.this.code.emitop(19, 1);
				Items.this.code.emit2(i);
			}
		}

		Item load() {
			switch (this.typecode) {
			case 0: // '\0'
			case 5: // '\005'
			case 6: // '\006'
			case 7: // '\007'
				final int i = ((Number) this.value).intValue();
				if (i >= -1 && i <= 5) {
					Items.this.code.emitop(3 + i);
					break;
				}
				if (i >= -128 && i <= 127) {
					Items.this.code.emitop1(16, i);
					break;
				}
				if (i >= -32768 && i <= 32767) {
					Items.this.code.emitop2(17, i);
				} else {
					this.ldc();
				}
				break;

			case 1: // '\001'
				final long l = ((Number) this.value).longValue();
				if (l == 0L || l == 1L) {
					Items.this.code.emitop(9 + (int) l);
				} else {
					this.ldc();
				}
				break;

			case 2: // '\002'
				final float f = ((Number) this.value).floatValue();
				if (this.isPosZero(f) || f == 1.0D || f == 2D) {
					Items.this.code.emitop(11 + (int) f);
				} else {
					this.ldc();
				}
				break;

			case 3: // '\003'
				final double d = ((Number) this.value).doubleValue();
				if (this.isPosZero(d) || d == 1.0D) {
					Items.this.code.emitop(14 + (int) d);
				} else {
					this.ldc();
				}
				break;

			case 4: // '\004'
				this.ldc();
				break;

			default:
				throw new InternalError("load");
			}
			return Items.this.stackItem[this.typecode];
		}

		private boolean isPosZero(final float f) {
			return f == 0.0F && 1.0F / f > 0.0F;
		}

		private boolean isPosZero(final double d) {
			return d == 0.0D && 1.0D / d > 0.0D;
		}

		CondItem mkCond() {
			final int i = ((Number) this.value).intValue();
			return Items.this.makeCondItem(i == 0 ? 168 : 167);
		}

		Item coerce(final int i) {
			if (this.typecode == i) {
				return this;
			}
			switch (i) {
			case 0: // '\0'
				return Code.truncate(this.typecode) == 0 ? this : new ImmediateItem(Type.intType, new Integer(((Number) this.value).intValue()));

			case 1: // '\001'
				return new ImmediateItem(Type.longType, new Long(((Number) this.value).longValue()));

			case 2: // '\002'
				return new ImmediateItem(Type.floatType, new Float(((Number) this.value).floatValue()));

			case 3: // '\003'
				return new ImmediateItem(Type.doubleType, new Double(((Number) this.value).doubleValue()));

			case 5: // '\005'
				return new ImmediateItem(Type.byteType, new Integer((byte) ((Number) this.value).intValue()));

			case 6: // '\006'
				return new ImmediateItem(Type.charType, new Integer((char) ((Number) this.value).intValue()));

			case 7: // '\007'
				return new ImmediateItem(Type.shortType, new Integer((short) ((Number) this.value).intValue()));

			case 4: // '\004'
			default:
				return super.coerce(i);
			}
		}

		final Object value;

		ImmediateItem(final Type type, final Object obj) {
			super(Code.typecode(type));
			this.value = obj;
		}
	}

	class MemberItem extends Item {

		Item load() {
			Items.this.code.emitop(180, Code.width(this.typecode) - 1);
			Items.this.code.emit2(Items.this.pool.put(this.member));
			return Items.this.stackItem[this.typecode];
		}

		void store() {
			Items.this.code.emitop(181, -Code.width(this.typecode) - 1);
			Items.this.code.emit2(Items.this.pool.put(this.member));
		}

		Item invoke() {
			final MethodType methodtype = (MethodType) this.member.externalType();
			final int i = Code.width(methodtype.argtypes);
			final int j = Code.typecode(methodtype.restype);
			final int k = Code.width(j) - i;
			if ((this.member.owner.flags() & 0x200) != 0) {
				Items.this.code.emitop(185, k - 1);
				Items.this.code.emit2(Items.this.pool.put(this.member));
				Items.this.code.emit1(i + 1);
				Items.this.code.emit1(0);
			} else if (this.nonvirtual) {
				Items.this.code.emitop(183, k - 1);
				Items.this.code.emit2(Items.this.pool.put(this.member));
			} else {
				Items.this.code.emitop(182, k - 1);
				Items.this.code.emit2(Items.this.pool.put(this.member));
			}
			return Items.this.stackItem[j];
		}

		void duplicate() {
			Items.this.stackItem[4].duplicate();
		}

		void drop() {
			Items.this.stackItem[4].drop();
		}

		void stash(final int i) {
			Items.this.stackItem[4].stash(i);
		}

		int width() {
			return 1;
		}

		final Symbol member;
		final boolean nonvirtual;

		MemberItem(final Symbol symbol, final boolean flag) {
			super(Code.typecode(symbol.erasure()));
			this.member = symbol;
			this.nonvirtual = flag;
		}
	}

	class StaticItem extends Item {

		Item load() {
			Items.this.code.emitop(178, Code.width(this.typecode));
			Items.this.code.emit2(Items.this.pool.put(this.member));
			return Items.this.stackItem[this.typecode];
		}

		void store() {
			Items.this.code.emitop(179, -Code.width(this.typecode));
			Items.this.code.emit2(Items.this.pool.put(this.member));
		}

		Item invoke() {
			final MethodType methodtype = (MethodType) this.member.erasure();
			final int i = Code.width(methodtype.argtypes);
			final int j = Code.typecode(methodtype.restype);
			final int k = Code.width(j) - i;
			Items.this.code.emitop(184, k);
			Items.this.code.emit2(Items.this.pool.put(this.member));
			return Items.this.stackItem[j];
		}

		final Symbol member;

		StaticItem(final Symbol symbol) {
			super(Code.typecode(symbol.erasure()));
			this.member = symbol;
		}
	}

	class LocalItem extends Item {

		Item load() {
			final int i = Items.this.code.regOf(this.adr);
			if (i <= 3) {
				Items.this.code.emitop(26 + Code.truncate(this.typecode) * 4 + i);
			} else {
				Items.this.code.emitop1w(21 + Code.truncate(this.typecode), i);
			}
			return Items.this.stackItem[this.typecode];
		}

		void store() {
			final int i = Items.this.code.regOf(this.adr);
			if (i <= 3) {
				Items.this.code.emitop(59 + Code.truncate(this.typecode) * 4 + i);
			} else {
				Items.this.code.emitop1w(54 + Code.truncate(this.typecode), i);
			}
			Items.this.code.setDefined(this.adr);
		}

		void incr(final int i) {
			if (this.typecode == 0) {
				final int j = Items.this.code.regOf(this.adr);
				Items.this.code.emitop1w(132, j);
				if (j > 255) {
					Items.this.code.emit2(i);
				} else {
					Items.this.code.emit1(i);
				}
			} else {
				this.load();
				if (i >= 0) {
					Items.this.makeImmediateItem(Type.intType, new Integer(i)).load();
					Items.this.code.emitop(96);
				} else {
					Items.this.makeImmediateItem(Type.intType, new Integer(-i)).load();
					Items.this.code.emitop(100);
				}
				Items.this.makeStackItem(Type.intType).coerce(this.typecode);
				this.store();
			}
		}

		final int adr;
		final Type type;

		LocalItem(final Type type1, final int i) {
			super(Code.typecode(type1));
			Util.assertTrue(i >= 0);
			this.type = type1;
			this.adr = i;
		}
	}

	class SelfItem extends Item {

		Item load() {
			Items.this.code.emitop(42);
			return Items.this.stackItem[this.typecode];
		}

		final boolean isSuper;

		SelfItem(final boolean flag) {
			super(4);
			this.isSuper = flag;
		}
	}

	class IndexedItem extends Item {

		Item load() {
			Items.this.code.emitop(46 + this.typecode);
			return Items.this.stackItem[this.typecode];
		}

		void store() {
			Items.this.code.emitop(79 + this.typecode);
		}

		void duplicate() {
			Items.this.code.emitop(92);
		}

		void drop() {
			Items.this.code.emitop(88);
		}

		void stash(final int i) {
			Items.this.code.emitop(91 + 3 * (Code.width(i) - 1));
		}

		int width() {
			return 2;
		}

		IndexedItem(final Type type) {
			super(Code.typecode(type));
		}
	}

	class StackItem extends Item {

		Item load() {
			return this;
		}

		void duplicate() {
			Items.this.code.emitop(this.width() != 2 ? 89 : 92);
		}

		void drop() {
			Items.this.code.emitop(this.width() != 2 ? 87 : 88);
		}

		void stash(final int i) {
			Items.this.code.emitop((this.width() != 2 ? 90 : 91) + 3 * (Code.width(i) - 1));
		}

		int width() {
			return Code.width(this.typecode);
		}

		StackItem(final int i) {
			super(i);
		}
	}

	class Item {

		Item load() {
			throw new InternalError();
		}

		void store() {
			throw new InternalError("store unsupported: " + this);
		}

		Item invoke() {
			throw new InternalError(this.toString());
		}

		void duplicate() {
		}

		void drop() {
		}

		void stash(final int i) {
			Items.this.stackItem[i].duplicate();
		}

		CondItem mkCond() {
			this.load();
			return Items.this.makeCondItem(154);
		}

		Item coerce(final int i) {
			if (this.typecode == i) {
				return this;
			}
			this.load();
			final int j = Code.truncate(this.typecode);
			final int k = Code.truncate(i);
			if (j != k) {
				final int l = k <= j ? k : k - 1;
				Items.this.code.emitop(133 + j * 3 + l);
			}
			if (i != k) {
				Items.this.code.emitop(145 + i - 5);
			}
			return Items.this.stackItem[i];
		}

		Item coerce(final Type type) {
			return this.coerce(Code.typecode(type));
		}

		int width() {
			return 0;
		}

		final int typecode;

		Item(final int i) {
			this.typecode = i;
		}
	}

	Items(final Pool pool1, final Code code1) {
		this.code = code1;
		this.pool = pool1;
		for (int i = 0; i < 8; i++) {
			this.stackItem[i] = new StackItem(i);
		}

		this.stackItem[8] = this.voidItem;
	}

	Item makeVoidItem() {
		return this.voidItem;
	}

	Item makeThisItem() {
		return this.thisItem;
	}

	Item makeSuperItem() {
		return this.superItem;
	}

	Item makeStackItem(final Type type) {
		return this.stackItem[Code.typecode(type)];
	}

	Item makeIndexedItem(final Type type) {
		return new IndexedItem(type);
	}

	Item makeLocalItem(final VarSymbol varsymbol) {
		return new LocalItem(varsymbol.erasure(), varsymbol.adr);
	}

	Item makeLocalItem(final Type type, final int i) {
		return new LocalItem(type, i);
	}

	Item makeStaticItem(final Symbol symbol) {
		return new StaticItem(symbol);
	}

	Item makeMemberItem(final Symbol symbol, final boolean flag) {
		return new MemberItem(symbol, flag);
	}

	Item makeImmediateItem(final Type type, final Object obj) {
		return new ImmediateItem(type, obj);
	}

	Item makeAssignItem(final Item item) {
		return new AssignItem(item);
	}

	CondItem makeCondItem(final int i, final Chain chain, final Chain chain1) {
		return new CondItem(i, chain, chain1);
	}

	CondItem makeCondItem(final int i) {
		return this.makeCondItem(i, null, null);
	}

	final Pool pool;
	final Code code;
	final Item voidItem = new Item(8);
	private final Item thisItem = new SelfItem(false);
	private final Item superItem = new SelfItem(true);
	final Item[] stackItem = new Item[9];
}
