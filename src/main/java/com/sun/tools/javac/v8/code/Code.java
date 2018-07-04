package com.sun.tools.javac.v8.code;

import com.sun.tools.javac.v8.code.Symbol.VarSymbol;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.ListBuffer;
import com.sun.tools.javac.v8.util.Log;
import com.sun.tools.javac.v8.util.Position;
import com.sun.tools.javac.v8.util.Util;

public final class Code {
	public static class Chain {

		final int pc;
		final int stacksize;
		final Chain next;

		public Chain(final int i, final int j, final Chain chain) {
			this.pc = i;
			this.stacksize = j;
			this.next = chain;
		}
	}

	public boolean checkLimits(final int i, final Log log) {
		if (this.cp > ClassFile.MAX_CODE) {
			log.error(i, "limit.code");
			return true;
		}
		if (this.max_locals > ClassFile.MAX_LOCALS) {
			log.error(i, "limit.locals");
			return true;
		}
		if (this.max_stack > ClassFile.MAX_STACK) {
			log.error(i, "limit.stack");
			return true;
		}
		return false;
	}

	public Code(final boolean flag, final boolean flag1, final boolean flag2) {
		this.max_stack = 0;
		this.max_locals = 0;
		this.code = new byte[64];
		this.cp = 0;
		this.catchInfo = new ListBuffer();
		this.lineInfo = new List();
		this.lvar_start_pc = new char[4];
		this.lvar_length = new char[4];
		this.lvar_reg = new char[4];
		this.lvar = new VarSymbol[4];
		this.nvars = 0;
		this.alive = true;
		this.stacksize = 0;
		this.fixedPc = false;
		this.nextadr = 0;
		this.nextreg = 0;
		this.adrmap = new int[64];
		this.pendingJumps = null;
		this.pendingStatPos = 0;
		this.fatcode = flag;
		this.lineDebugInfo = flag1;
		this.varDebugInfo = flag2;
	}

	public static int typecode(final Type type) {
		switch (type.tag) {
		case 1: // '\001'
			return 5;

		case 3: // '\003'
			return 7;

		case 2: // '\002'
			return 6;

		case 4: // '\004'
			return 0;

		case 5: // '\005'
			return 1;

		case 6: // '\006'
			return 2;

		case 7: // '\007'
			return 3;

		case 8: // '\b'
			return 5;

		case 9: // '\t'
			return 8;

		case 10: // '\n'
		case 11: // '\013'
		case 12: // '\f'
		case 14: // '\016'
		case 16: // '\020'
			return 4;

		case 13: // '\r'
		case 15: // '\017'
		default:
			throw new InternalError("typecode " + type.tag);
		}
	}

	public static int truncate(final int i) {
		switch (i) {
		case 5: // '\005'
		case 6: // '\006'
		case 7: // '\007'
			return 0;
		}
		return i;
	}

	public static int width(final int i) {
		switch (i) {
		case 1: // '\001'
		case 3: // '\003'
			return 2;

		case 8: // '\b'
			return 0;
		}
		return 1;
	}

	private static int width(final Type type) {
		return width(typecode(type));
	}

	public static int width(final List list) {
		int i = 0;
		for (List list1 = list; list1.nonEmpty(); list1 = list1.tail) {
			i += width((Type) list1.head);
		}

		return i;
	}

	public static int arraycode(final Type type) {
		switch (type.tag) {
		case 1: // '\001'
			return 8;

		case 8: // '\b'
			return 4;

		case 3: // '\003'
			return 9;

		case 2: // '\002'
			return 5;

		case 4: // '\004'
			return 10;

		case 5: // '\005'
			return 11;

		case 6: // '\006'
			return 6;

		case 7: // '\007'
			return 7;

		case 10: // '\n'
			return 0;

		case 11: // '\013'
			return 1;

		case 9: // '\t'
		default:
			throw new InternalError("arraycode " + type);
		}
	}

	public int curPc() {
		if (this.pendingJumps != null) {
			this.resolvePending();
		}
		if (this.pendingStatPos != 0) {
			this.markStatBegin();
		}
		this.fixedPc = true;
		return this.cp;
	}

	public void emit1(final int i) {
		if (this.alive) {
			if (this.cp == this.code.length) {
				final byte abyte0[] = new byte[this.cp * 2];
				System.arraycopy(this.code, 0, abyte0, 0, this.cp);
				this.code = abyte0;
			}
			this.code[this.cp++] = (byte) i;
		}
	}

	public void emit2(final int i) {
		if (this.alive) {
			if (this.cp + 2 > this.code.length) {
				this.emit1(i >> 8);
				this.emit1(i);
			} else {
				this.code[this.cp++] = (byte) (i >> 8);
				this.code[this.cp++] = (byte) i;
			}
		}
	}

	public void emit4(final int i) {
		if (this.alive) {
			if (this.cp + 4 > this.code.length) {
				this.emit1(i >> 24);
				this.emit1(i >> 16);
				this.emit1(i >> 8);
				this.emit1(i);
			} else {
				this.code[this.cp++] = (byte) (i >> 24);
				this.code[this.cp++] = (byte) (i >> 16);
				this.code[this.cp++] = (byte) (i >> 8);
				this.code[this.cp++] = (byte) i;
			}
		}
	}

	public void emitop(final int i, final int j) {
		if (this.pendingJumps != null) {
			this.resolvePending();
		}
		if (this.alive) {
			if (this.pendingStatPos != 0) {
				this.markStatBegin();
			}
			this.emit1(i);
			if (j <= -1000) {
				this.stacksize += j + 1000;
				this.alive = false;
				Util.assertTrue(this.stacksize == 0);
			} else {
				this.stacksize += j;
				Util.assertTrue(this.stacksize >= 0);
				if (this.stacksize > this.max_stack) {
					this.max_stack = this.stacksize;
				}
			}
		}
	}

	public void emitop(final int i) {
		this.emitop(i, stackdiff[i]);
	}

	public void emitop1(final int i, final int j) {
		this.emitop(i);
		this.emit1(j);
	}

	public void emitop1w(final int i, final int j) {
		if (j > 255) {
			this.emitop(196);
			this.emitop2(i, j);
		} else {
			this.emitop1(i, j);
		}
	}

	public void emitop2(final int i, final int j) {
		this.emitop(i);
		this.emit2(j);
	}

	private void emitop4(final int i, final int j) {
		this.emitop(i);
		this.emit4(j);
	}

	public void align(final int i) {
		if (this.alive) {
			while (this.cp % i != 0) {
				this.emit1(0);
			}
		}
	}

	private void put1(final int i, final int j) {
		this.code[i] = (byte) j;
	}

	private void put2(final int i, final int j) {
		this.put1(i, j >> 8);
		this.put1(i + 1, j);
	}

	public void put4(final int i, final int j) {
		this.put1(i, j >> 24);
		this.put1(i + 1, j >> 16);
		this.put1(i + 2, j >> 8);
		this.put1(i + 3, j);
	}

	private int get1(final int i) {
		return this.code[i] & 0xff;
	}

	private int get2(final int i) {
		return this.get1(i) << 8 | this.get1(i + 1);
	}

	public int get4(final int i) {
		return this.get1(i) << 24 | this.get1(i + 1) << 16 | this.get1(i + 2) << 8 | this.get1(i + 3);
	}

	public boolean isAlive() {
		return this.alive || this.pendingJumps != null;
	}

	public void markDead() {
		this.alive = false;
	}

	public int entryPoint() {
		this.alive = true;
		return this.curPc();
	}

	public int entryPoint(final int i) {
		this.alive = true;
		this.stacksize = i;
		if (this.stacksize > this.max_stack) {
			this.max_stack = this.stacksize;
		}
		return this.curPc();
	}

	public static int negate(final int i) {
		if (i == 198) {
			return 199;
		}
		return i == 199 ? 198 : (i + 1 ^ 1) - 1;
	}

	public int emitJump(final int i) {
		if (this.fatcode) {
			if (i == 167 || i == 168) {
				this.emitop4(i + 200 - 167, 0);
			} else {
				this.emitop2(negate(i), 8);
				this.emitop4(200, 0);
			}
			return this.cp - 5;
		}
		this.emitop2(i, 0);
		return this.cp - 3;
	}

	public Chain branch(final int i) {
		Chain chain = null;
		if (i == 167) {
			chain = this.pendingJumps;
			this.pendingJumps = null;
		}
		if (i != 168 && this.isAlive()) {
			chain = new Chain(this.emitJump(i), this.stacksize, chain);
			this.fixedPc = this.fatcode;
			if (i == 167) {
				this.alive = false;
			}
		}
		return chain;
	}

	public void resolve(final Chain chain, int i) {
		if (chain != null) {
			Util.assertTrue(i > chain.pc || this.stacksize == 0);
			if (i >= this.cp) {
				i = this.cp;
			} else if (this.get1(i) == 167) {
				i += this.fatcode ? this.get4(i + 1) : this.get2(i + 1);
			}
			if (this.get1(chain.pc) == 167 && chain.pc + 3 == i && i == this.cp && !this.fixedPc) {
				this.cp -= 3;
				i -= 3;
			} else {
				if (this.fatcode) {
					this.put4(chain.pc + 1, i - chain.pc);
				} else if (i - chain.pc < -32768 || i - chain.pc > 32767) {
					this.fatcode = true;
				} else {
					this.put2(chain.pc + 1, i - chain.pc);
				}
				Util.assertTrue(!this.alive || chain.stacksize == this.stacksize);
			}
			this.fixedPc = true;
			this.resolve(chain.next, i);
			if (this.cp == i) {
				this.stacksize = chain.stacksize;
				this.alive = true;
			}
		}
	}

	public void resolve(final Chain chain) {
		this.pendingJumps = mergeChains(chain, this.pendingJumps);
	}

	private void resolvePending() {
		this.resolve(this.pendingJumps, this.cp);
		this.pendingJumps = null;
	}

	public static Chain mergeChains(final Chain chain, final Chain chain1) {
		if (chain1 == null) {
			return chain;
		}
		if (chain == null) {
			return chain1;
		}
		return chain.pc < chain1.pc ? new Chain(chain1.pc, chain1.stacksize, mergeChains(chain, chain1.next)) : new Chain(chain.pc, chain.stacksize, mergeChains(chain.next, chain1));
	}

	public void addCatch(final char c, final char c1, final char c2, final char c3) {
		this.catchInfo.append(new char[] { c, c1, c2, c3 });
	}

	private void addLineNumber(final char c, final char c1) {
		if (this.lineDebugInfo) {
			if (this.lineInfo.nonEmpty() && ((char[]) this.lineInfo.head)[0] == c) {
				this.lineInfo = this.lineInfo.tail;
			}
			if (this.lineInfo.isEmpty() || ((char[]) this.lineInfo.head)[1] != c1) {
				this.lineInfo = this.lineInfo.prepend(new char[] { c, c1 });
			}
		}
	}

	public void statBegin(final int i) {
		if (i != 0) {
			this.pendingStatPos = i;
		}
	}

	public void markStatBegin() {
		final int i = Position.line(this.pendingStatPos);
		this.pendingStatPos = 0;
		if (this.alive && this.lineDebugInfo) {
			final char c = (char) this.cp;
			final char c1 = (char) i;
			if (c == this.cp && c1 == i) {
				this.addLineNumber(c, c1);
			}
		}
	}

	private void addLocalVar(final int i, final VarSymbol varsymbol) {
		for (VarSymbol[] avarsymbol; varsymbol.adr >= this.lvar.length; this.lvar = avarsymbol) {
			final char ac[] = new char[this.lvar.length * 2];
			final char ac1[] = new char[this.lvar.length * 2];
			final char ac2[] = new char[this.lvar.length * 2];
			avarsymbol = new VarSymbol[this.lvar.length * 2];
			for (int j = 0; j < this.lvar.length; j++) {
				ac[j] = this.lvar_start_pc[j];
				ac1[j] = this.lvar_length[j];
				ac2[j] = this.lvar_reg[j];
				avarsymbol[j] = this.lvar[j];
			}

			this.lvar_start_pc = ac;
			this.lvar_length = ac1;
			this.lvar_reg = ac2;
		}

		this.lvar_start_pc[varsymbol.adr] = '\uFFFF';
		this.lvar_length[varsymbol.adr] = '\uFFFF';
		this.lvar_reg[varsymbol.adr] = (char) i;
		this.lvar[varsymbol.adr] = varsymbol;
		this.nvars++;
	}

	public void setDefined(final int i) {
		if (this.varDebugInfo && this.cp < 65535 && i < this.lvar_start_pc.length && this.lvar_start_pc[i] == '\uFFFF') {
			this.lvar_start_pc[i] = (char) this.cp;
		}
	}

	private void setUndefined(final int i) {
		if (this.varDebugInfo && i < this.lvar_start_pc.length && this.lvar_start_pc[i] != '\uFFFF' && this.cp - this.lvar_start_pc[i] < 65535 && this.lvar_length[i] == '\uFFFF') {
			this.lvar_length[i] = (char) (this.curPc() - this.lvar_start_pc[i]);
		}
	}

	private int newLocal(final int i) {
		final int j = this.nextadr++;
		final int k = width(i);
		final int l = this.nextreg;
		this.nextreg = l + k;
		if (this.nextreg > this.max_locals) {
			this.max_locals = this.nextreg;
		}
		for (int[] ai; j >= this.adrmap.length; this.adrmap = ai) {
			ai = new int[this.adrmap.length * 2];
			System.arraycopy(this.adrmap, 0, ai, 0, this.adrmap.length);
		}

		this.adrmap[j] = k == 2 ? -l : l;
		return j;
	}

	public int newLocal(final Type type) {
		return this.newLocal(typecode(type));
	}

	public int newLocal(final VarSymbol varsymbol) {
		varsymbol.adr = this.newLocal(varsymbol.erasure());
		if (this.varDebugInfo) {
			this.addLocalVar(this.regOf(varsymbol.adr), varsymbol);
		}
		return varsymbol.adr;
	}

	public int regOf(final int i) {
		final int j = this.adrmap[i];
		if (j < 0) {
			return -j;
		}
		return j;
	}

	public void newRegSegment() {
		this.nextreg = this.max_locals;
	}

	public void endScopes(final int i) {
		if (i < this.nextadr) {
			for (int j = i; j < this.nextadr; j++) {
				this.setUndefined(j);
			}

			this.nextreg = this.regOf(i);
		}
	}

	public int max_stack;
	public int max_locals;
	public byte code[];
	public int cp;
	ListBuffer catchInfo;
	List lineInfo;
	char lvar_start_pc[];
	char lvar_length[];
	char lvar_reg[];
	VarSymbol lvar[];
	int nvars;
	public boolean fatcode;
	private boolean alive;
	public int stacksize;
	private boolean fixedPc;
	public int nextadr;
	private int nextreg;
	private int adrmap[];
	private Chain pendingJumps;
	private int pendingStatPos;
	private final boolean varDebugInfo;
	private final boolean lineDebugInfo;
	public static final int stackdiff[];
	static {
		stackdiff = new int[ByteCodes.ByteCodeCount];
		stackdiff[ByteCodes.nop] = 0;
		stackdiff[1] = 1;
		stackdiff[2] = 1;
		stackdiff[3] = 1;
		stackdiff[4] = 1;
		stackdiff[5] = 1;
		stackdiff[6] = 1;
		stackdiff[7] = 1;
		stackdiff[8] = 1;
		stackdiff[9] = 2;
		stackdiff[10] = 2;
		stackdiff[11] = 1;
		stackdiff[12] = 1;
		stackdiff[13] = 1;
		stackdiff[14] = 2;
		stackdiff[15] = 2;
		stackdiff[16] = 1;
		stackdiff[17] = 1;
		stackdiff[18] = -999;
		stackdiff[19] = -999;
		stackdiff[20] = -999;
		stackdiff[21] = 1;
		stackdiff[22] = 2;
		stackdiff[23] = 1;
		stackdiff[24] = 2;
		stackdiff[25] = 1;
		stackdiff[26] = 1;
		stackdiff[30] = 2;
		stackdiff[34] = 1;
		stackdiff[38] = 2;
		stackdiff[42] = 1;
		stackdiff[27] = 1;
		stackdiff[31] = 2;
		stackdiff[35] = 1;
		stackdiff[39] = 2;
		stackdiff[43] = 1;
		stackdiff[28] = 1;
		stackdiff[32] = 2;
		stackdiff[36] = 1;
		stackdiff[40] = 2;
		stackdiff[44] = 1;
		stackdiff[29] = 1;
		stackdiff[33] = 2;
		stackdiff[37] = 1;
		stackdiff[41] = 2;
		stackdiff[45] = 1;
		stackdiff[46] = -1;
		stackdiff[47] = 0;
		stackdiff[48] = -1;
		stackdiff[49] = 0;
		stackdiff[50] = -1;
		stackdiff[51] = -1;
		stackdiff[52] = -1;
		stackdiff[53] = -1;
		stackdiff[54] = -1;
		stackdiff[55] = -2;
		stackdiff[56] = -1;
		stackdiff[57] = -2;
		stackdiff[58] = -1;
		stackdiff[59] = -1;
		stackdiff[63] = -2;
		stackdiff[67] = -1;
		stackdiff[71] = -2;
		stackdiff[75] = -1;
		stackdiff[60] = -1;
		stackdiff[64] = -2;
		stackdiff[68] = -1;
		stackdiff[72] = -2;
		stackdiff[76] = -1;
		stackdiff[61] = -1;
		stackdiff[65] = -2;
		stackdiff[69] = -1;
		stackdiff[73] = -2;
		stackdiff[77] = -1;
		stackdiff[62] = -1;
		stackdiff[66] = -2;
		stackdiff[70] = -1;
		stackdiff[74] = -2;
		stackdiff[78] = -1;
		stackdiff[79] = -3;
		stackdiff[80] = -4;
		stackdiff[81] = -3;
		stackdiff[82] = -4;
		stackdiff[83] = -3;
		stackdiff[84] = -3;
		stackdiff[85] = -3;
		stackdiff[86] = -3;
		stackdiff[87] = -1;
		stackdiff[88] = -2;
		stackdiff[89] = 1;
		stackdiff[90] = 1;
		stackdiff[91] = 1;
		stackdiff[92] = 2;
		stackdiff[93] = 2;
		stackdiff[94] = 2;
		stackdiff[95] = 0;
		stackdiff[96] = -1;
		stackdiff[97] = -2;
		stackdiff[98] = -1;
		stackdiff[99] = -2;
		stackdiff[100] = -1;
		stackdiff[101] = -2;
		stackdiff[102] = -1;
		stackdiff[103] = -2;
		stackdiff[104] = -1;
		stackdiff[105] = -2;
		stackdiff[106] = -1;
		stackdiff[107] = -2;
		stackdiff[108] = -1;
		stackdiff[109] = -2;
		stackdiff[110] = -1;
		stackdiff[111] = -2;
		stackdiff[112] = -1;
		stackdiff[113] = -2;
		stackdiff[114] = -1;
		stackdiff[115] = -2;
		stackdiff[116] = 0;
		stackdiff[117] = 0;
		stackdiff[118] = 0;
		stackdiff[119] = 0;
		stackdiff[120] = -1;
		stackdiff[121] = -1;
		stackdiff[122] = -1;
		stackdiff[123] = -1;
		stackdiff[124] = -1;
		stackdiff[125] = -1;
		stackdiff[126] = -1;
		stackdiff[127] = -2;
		stackdiff[128] = -1;
		stackdiff[129] = -2;
		stackdiff[130] = -1;
		stackdiff[131] = -2;
		stackdiff[132] = 0;
		stackdiff[133] = 1;
		stackdiff[134] = 0;
		stackdiff[135] = 1;
		stackdiff[136] = -1;
		stackdiff[137] = -1;
		stackdiff[138] = 0;
		stackdiff[139] = 0;
		stackdiff[140] = 1;
		stackdiff[141] = 1;
		stackdiff[142] = -1;
		stackdiff[143] = 0;
		stackdiff[144] = -1;
		stackdiff[145] = 0;
		stackdiff[146] = 0;
		stackdiff[147] = 0;
		stackdiff[148] = -3;
		stackdiff[149] = -1;
		stackdiff[150] = -1;
		stackdiff[151] = -3;
		stackdiff[152] = -3;
		stackdiff[153] = -1;
		stackdiff[154] = -1;
		stackdiff[155] = -1;
		stackdiff[156] = -1;
		stackdiff[157] = -1;
		stackdiff[158] = -1;
		stackdiff[159] = -2;
		stackdiff[160] = -2;
		stackdiff[161] = -2;
		stackdiff[162] = -2;
		stackdiff[163] = -2;
		stackdiff[164] = -2;
		stackdiff[165] = -2;
		stackdiff[166] = -2;
		stackdiff[167] = 0;
		stackdiff[168] = 0;
		stackdiff[169] = 0;
		stackdiff[170] = -1;
		stackdiff[171] = -1;
		stackdiff[172] = -1001;
		stackdiff[173] = -1002;
		stackdiff[174] = -1001;
		stackdiff[175] = -1002;
		stackdiff[176] = -1001;
		stackdiff[177] = -1000;
		stackdiff[178] = -999;
		stackdiff[179] = -999;
		stackdiff[180] = -999;
		stackdiff[181] = -999;
		stackdiff[182] = -999;
		stackdiff[183] = -999;
		stackdiff[184] = -999;
		stackdiff[185] = -999;
		stackdiff[186] = 0;
		stackdiff[187] = 1;
		stackdiff[188] = 0;
		stackdiff[189] = 0;
		stackdiff[190] = 0;
		stackdiff[191] = -1001;
		stackdiff[192] = 0;
		stackdiff[193] = 0;
		stackdiff[194] = -1;
		stackdiff[195] = -1;
		stackdiff[196] = 0;
		stackdiff[197] = -999;
		stackdiff[198] = -1;
		stackdiff[199] = -1;
		stackdiff[200] = 0;
		stackdiff[201] = 0;
		stackdiff[ByteCodes.breakpoint] = 0;
	}
}
