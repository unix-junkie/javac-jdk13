package sun.tools.asm;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;

import sun.tools.java.ClassDeclaration;
import sun.tools.java.CompilerError;
import sun.tools.java.Environment;
import sun.tools.java.MemberDefinition;
import sun.tools.java.RuntimeConstants;

public class Instruction {

	public Instruction(final long l, final int i, final Object obj, final boolean flag) {
		this.flagNoCovered = false;
		this.where = l;
		this.opc = i;
		this.value = obj;
		this.flagCondInverted = flag;
	}

	public Instruction(final boolean flag, final long l, final int i, final Object obj) {
		this.flagNoCovered = false;
		this.where = l;
		this.opc = i;
		this.value = obj;
		this.flagNoCovered = flag;
	}

	public Instruction(final long l, final int i, final boolean flag) {
		this.flagNoCovered = false;
		this.where = l;
		this.opc = i;
		this.flagNoCovered = flag;
	}

	public Instruction(final long l, final int i, final Object obj) {
		this.flagNoCovered = false;
		this.where = l;
		this.opc = i;
		this.value = obj;
	}

	public int getOpcode() {
		return this.pc;
	}

	public Object getValue() {
		return this.value;
	}

	public void setValue(final Object obj) {
		this.value = obj;
	}

	void optimize(final Environment environment) {
		switch (this.opc) {
		default:
			break;

		case 54: // '6'
		case 55: // '7'
		case 56: // '8'
		case 57: // '9'
		case 58: // ':'
			if (this.value instanceof LocalVariable && !environment.debug_vars()) {
				this.value = new Integer(((LocalVariable) this.value).slot);
			}
			break;

		case 167:
			Label label = (Label) this.value;
			this.value = label = label.getDestination();
			if (label == this.next) {
				this.opc = -2;
				break;
			}
			if (((Instruction) label).next == null || !environment.opt()) {
				break;
			}
			switch (((Instruction) label).next.opc) {
			case 172:
			case 173:
			case 174:
			case 175:
			case 176:
			case 177:
				this.opc = ((Instruction) label).next.opc;
				this.value = ((Instruction) label).next.value;
				break;
			}
			break;

		case 153:
		case 154:
		case 155:
		case 156:
		case 157:
		case 158:
		case 198:
		case 199:
			this.value = ((Label) this.value).getDestination();
			if (this.value == this.next) {
				this.opc = 87;
				break;
			}
			if (this.next.opc != 167 || this.value != this.next.next) {
				break;
			}
			switch (this.opc) {
			case 153:
				this.opc = 154;
				break;

			case 154:
				this.opc = 153;
				break;

			case 155:
				this.opc = 156;
				break;

			case 158:
				this.opc = 157;
				break;

			case 157:
				this.opc = 158;
				break;

			case 156:
				this.opc = 155;
				break;

			case 198:
				this.opc = 199;
				break;

			case 199:
				this.opc = 198;
				break;
			}
			this.flagCondInverted = !this.flagCondInverted;
			this.value = this.next.value;
			this.next.opc = -2;
			break;

		case 159:
		case 160:
		case 161:
		case 162:
		case 163:
		case 164:
		case 165:
		case 166:
			this.value = ((Label) this.value).getDestination();
			if (this.value == this.next) {
				this.opc = 88;
				break;
			}
			if (this.next.opc != 167 || this.value != this.next.next) {
				break;
			}
			switch (this.opc) {
			case 165:
				this.opc = 166;
				break;

			case 166:
				this.opc = 165;
				break;

			case 159:
				this.opc = 160;
				break;

			case 160:
				this.opc = 159;
				break;

			case 163:
				this.opc = 164;
				break;

			case 162:
				this.opc = 161;
				break;

			case 161:
				this.opc = 162;
				break;

			case 164:
				this.opc = 163;
				break;
			}
			this.flagCondInverted = !this.flagCondInverted;
			this.value = this.next.value;
			this.next.opc = -2;
			break;

		case 170:
		case 171:
			final SwitchData switchdata = (SwitchData) this.value;
			switchdata.defaultLabel = switchdata.defaultLabel.getDestination();
			Integer integer;
			Label label1;
			for (final Iterator iterator = switchdata.tab.keySet().iterator(); iterator.hasNext(); switchdata.tab.put(integer, label1.getDestination())) {
				integer = (Integer) iterator.next();
				label1 = (Label) switchdata.tab.get(integer);
			}

			final long l = (long) switchdata.maxValue - switchdata.minValue + 1L;
			final long l1 = switchdata.tab.size();
			final long l2 = 4L + l;
			final long l3 = 3L + 2L * l1;
			this.opc = l2 <= l3 * SWITCHRATIO ? 170 : 171;
			break;
		}
	}

	void collect(final ConstantPool constantpool) {
		switch (this.opc) {
		case 54: // '6'
		case 55: // '7'
		case 56: // '8'
		case 57: // '9'
		case 58: // ':'
			if (this.value instanceof LocalVariable) {
				final MemberDefinition memberdefinition = ((LocalVariable) this.value).field;
				constantpool.put(memberdefinition.getName().toString());
				constantpool.put(memberdefinition.getType().getTypeSignature());
			}
			return;

		case 178:
		case 179:
		case 180:
		case 181:
		case 182:
		case 183:
		case 184:
		case 185:
		case 187:
		case 192:
		case 193:
			constantpool.put(this.value);
			return;

		case 189:
			constantpool.put(this.value);
			return;

		case 197:
			constantpool.put(((ArrayData) this.value).type);
			return;

		case 18: // '\022'
		case 19: // '\023'
			if (this.value instanceof Integer) {
				final int i = ((Number) this.value).intValue();
				if (i >= -1 && i <= 5) {
					this.opc = 3 + i;
					return;
				}
				if (i >= -128 && i < 128) {
					this.opc = 16;
					return;
				}
				if (i >= -32768 && i < 32768) {
					this.opc = 17;
					return;
				}
			} else if (this.value instanceof Float) {
				final float f = ((Number) this.value).floatValue();
				if (f == 0.0F) {
					if (Float.floatToIntBits(0.0F) == 0) {
						this.opc = 11;
						return;
					}
				} else {
					if (f == 1.0F) {
						this.opc = 12;
						return;
					}
					if (f == 2.0F) {
						this.opc = 13;
						return;
					}
				}
			}
			constantpool.put(this.value);
			return;

		case 20: // '\024'
			if (this.value instanceof Long) {
				final long l = ((Number) this.value).longValue();
				if (l == 0L) {
					this.opc = 9;
					return;
				}
				if (l == 1L) {
					this.opc = 10;
					return;
				}
			} else if (this.value instanceof Double) {
				final double d = ((Number) this.value).doubleValue();
				if (d == 0.0D) {
					if (Double.doubleToLongBits(d) == 0L) {
						this.opc = 14;
						return;
					}
				} else if (d == 1.0D) {
					this.opc = 15;
					return;
				}
			}
			constantpool.put(this.value);
			return;

		case -3:
			for (final Iterator iterator = ((TryData) this.value).catches.iterator(); iterator.hasNext();) {
				final CatchData catchdata = (CatchData) iterator.next();
				if (catchdata.getType() != null) {
					constantpool.put(catchdata.getType());
				}
			}

			return;

		case 0: // '\0'
			if (this.value instanceof ClassDeclaration) {
				constantpool.put(this.value);
			}
		}
	}

	int balance() {
		switch (this.opc) {
		case -3:
		case -2:
		case -1:
		case 0: // '\0'
		case 47: // '/'
		case 49: // '1'
		case 95: // '_'
		case 116: // 't'
		case 117: // 'u'
		case 118: // 'v'
		case 119: // 'w'
		case 132:
		case 134:
		case 138:
		case 139:
		case 143:
		case 145:
		case 146:
		case 147:
		case 167:
		case 168:
		case 169:
		case 177:
		case 188:
		case 189:
		case 190:
		case 192:
		case 193:
		case 200:
		case 201:
			return 0;

		case 1: // '\001'
		case 2: // '\002'
		case 3: // '\003'
		case 4: // '\004'
		case 5: // '\005'
		case 6: // '\006'
		case 7: // '\007'
		case 8: // '\b'
		case 11: // '\013'
		case 12: // '\f'
		case 13: // '\r'
		case 16: // '\020'
		case 17: // '\021'
		case 18: // '\022'
		case 19: // '\023'
		case 21: // '\025'
		case 23: // '\027'
		case 25: // '\031'
		case 89: // 'Y'
		case 90: // 'Z'
		case 91: // '['
		case 133:
		case 135:
		case 140:
		case 141:
		case 187:
			return 1;

		case 9: // '\t'
		case 10: // '\n'
		case 14: // '\016'
		case 15: // '\017'
		case 20: // '\024'
		case 22: // '\026'
		case 24: // '\030'
		case 92: // '\\'
		case 93: // ']'
		case 94: // '^'
			return 2;

		case 46: // '.'
		case 48: // '0'
		case 50: // '2'
		case 51: // '3'
		case 52: // '4'
		case 53: // '5'
		case 54: // '6'
		case 56: // '8'
		case 58: // ':'
		case 87: // 'W'
		case 96: // '`'
		case 98: // 'b'
		case 100: // 'd'
		case 102: // 'f'
		case 104: // 'h'
		case 106: // 'j'
		case 108: // 'l'
		case 110: // 'n'
		case 112: // 'p'
		case 114: // 'r'
		case 120: // 'x'
		case 121: // 'y'
		case 122: // 'z'
		case 123: // '{'
		case 124: // '|'
		case 125: // '}'
		case 126: // '~'
		case 128:
		case 130:
		case 136:
		case 137:
		case 142:
		case 144:
		case 149:
		case 150:
		case 153:
		case 154:
		case 155:
		case 156:
		case 157:
		case 158:
		case 170:
		case 171:
		case 172:
		case 174:
		case 176:
		case 191:
		case 194:
		case 195:
		case 198:
		case 199:
			return -1;

		case 55: // '7'
		case 57: // '9'
		case 88: // 'X'
		case 97: // 'a'
		case 99: // 'c'
		case 101: // 'e'
		case 103: // 'g'
		case 105: // 'i'
		case 107: // 'k'
		case 109: // 'm'
		case 111: // 'o'
		case 113: // 'q'
		case 115: // 's'
		case 127: // '\177'
		case 129:
		case 131:
		case 159:
		case 160:
		case 161:
		case 162:
		case 163:
		case 164:
		case 165:
		case 166:
		case 173:
		case 175:
			return -2;

		case 79: // 'O'
		case 81: // 'Q'
		case 83: // 'S'
		case 84: // 'T'
		case 85: // 'U'
		case 86: // 'V'
		case 148:
		case 151:
		case 152:
			return -3;

		case 80: // 'P'
		case 82: // 'R'
			return -4;

		case 197:
			return 1 - ((ArrayData) this.value).nargs;

		case 180:
			return ((MemberDefinition) this.value).getType().stackSize() - 1;

		case 181:
			return -1 - ((MemberDefinition) this.value).getType().stackSize();

		case 178:
			return ((MemberDefinition) this.value).getType().stackSize();

		case 179:
			return -((MemberDefinition) this.value).getType().stackSize();

		case 182:
		case 183:
		case 185:
			return ((MemberDefinition) this.value).getType().getReturnType().stackSize() - (((MemberDefinition) this.value).getType().stackSize() + 1);

		case 184:
			return ((MemberDefinition) this.value).getType().getReturnType().stackSize() - ((MemberDefinition) this.value).getType().stackSize();

		case 26: // '\032'
		case 27: // '\033'
		case 28: // '\034'
		case 29: // '\035'
		case 30: // '\036'
		case 31: // '\037'
		case 32: // ' '
		case 33: // '!'
		case 34: // '"'
		case 35: // '#'
		case 36: // '$'
		case 37: // '%'
		case 38: // '&'
		case 39: // '\''
		case 40: // '('
		case 41: // ')'
		case 42: // '*'
		case 43: // '+'
		case 44: // ','
		case 45: // '-'
		case 59: // ';'
		case 60: // '<'
		case 61: // '='
		case 62: // '>'
		case 63: // '?'
		case 64: // '@'
		case 65: // 'A'
		case 66: // 'B'
		case 67: // 'C'
		case 68: // 'D'
		case 69: // 'E'
		case 70: // 'F'
		case 71: // 'G'
		case 72: // 'H'
		case 73: // 'I'
		case 74: // 'J'
		case 75: // 'K'
		case 76: // 'L'
		case 77: // 'M'
		case 78: // 'N'
		case 186:
		case 196:
		default:
			throw new CompilerError("invalid opcode: " + this);
		}
	}

	int size(final ConstantPool constantpool) {
		switch (this.opc) {
		case -3:
		case -2:
		case -1:
			return 0;

		case 16: // '\020'
		case 188:
			return 2;

		case 17: // '\021'
		case 153:
		case 154:
		case 155:
		case 156:
		case 157:
		case 158:
		case 159:
		case 160:
		case 161:
		case 162:
		case 163:
		case 164:
		case 165:
		case 166:
		case 167:
		case 168:
		case 198:
		case 199:
			return 3;

		case 18: // '\022'
		case 19: // '\023'
			if (constantpool.index(this.value) < 256) {
				this.opc = 18;
				return 2;
			}
			this.opc = 19;
			return 3;

		case 21: // '\025'
		case 22: // '\026'
		case 23: // '\027'
		case 24: // '\030'
		case 25: // '\031'
			final int i = ((Number) this.value).intValue();
			if (i < 4) {
				if (i < 0) {
					throw new CompilerError("invalid slot: " + this + "\nThis error possibly resulted from poorly constructed class paths.");
				}
				this.opc = 26 + (this.opc - 21) * 4 + i;
				return 1;
			}
			if (i <= 255) {
				return 2;
			}
			this.opc += 256;
			return 4;

		case 132:
			final int j = ((int[]) this.value)[0];
			final int i1 = ((int[]) this.value)[1];
			if (j < 0) {
				throw new CompilerError("invalid slot: " + this);
			}
			if (j <= 255 && (byte) i1 == i1) {
				return 3;
			}
			this.opc += 256;
			return 6;

		case 54: // '6'
		case 55: // '7'
		case 56: // '8'
		case 57: // '9'
		case 58: // ':'
			final int k = this.value instanceof Number ? ((Number) this.value).intValue() : ((LocalVariable) this.value).slot;
			if (k < 4) {
				if (k < 0) {
					throw new CompilerError("invalid slot: " + this);
				}
				this.opc = 59 + (this.opc - 54) * 4 + k;
				return 1;
			}
			if (k <= 255) {
				return 2;
			}
			this.opc += 256;
			return 4;

		case 169:
			final int l = ((Number) this.value).intValue();
			if (l <= 255) {
				if (l < 0) {
					throw new CompilerError("invalid slot: " + this);
				}
				return 2;
			}
			this.opc += 256;
			return 4;

		case 20: // '\024'
		case 178:
		case 179:
		case 180:
		case 181:
		case 182:
		case 183:
		case 184:
		case 187:
		case 189:
		case 192:
		case 193:
			return 3;

		case 197:
			return 4;

		case 185:
		case 200:
		case 201:
			return 5;

		case 170:
			final SwitchData switchdata = (SwitchData) this.value;
			int j1;
			for (j1 = 1; (this.pc + j1) % 4 != 0; j1++) {
			}
			return j1 + 16 + (switchdata.maxValue - switchdata.minValue) * 4;

		case 171:
			final SwitchData switchdata1 = (SwitchData) this.value;
			int k1;
			for (k1 = 1; (this.pc + k1) % 4 != 0; k1++) {
			}
			return k1 + 8 + switchdata1.tab.size() * 8;

		case 0: // '\0'
			return this.value == null || this.value instanceof Integer ? 1 : 2;

		case 1: // '\001'
		case 2: // '\002'
		case 3: // '\003'
		case 4: // '\004'
		case 5: // '\005'
		case 6: // '\006'
		case 7: // '\007'
		case 8: // '\b'
		case 9: // '\t'
		case 10: // '\n'
		case 11: // '\013'
		case 12: // '\f'
		case 13: // '\r'
		case 14: // '\016'
		case 15: // '\017'
		case 26: // '\032'
		case 27: // '\033'
		case 28: // '\034'
		case 29: // '\035'
		case 30: // '\036'
		case 31: // '\037'
		case 32: // ' '
		case 33: // '!'
		case 34: // '"'
		case 35: // '#'
		case 36: // '$'
		case 37: // '%'
		case 38: // '&'
		case 39: // '\''
		case 40: // '('
		case 41: // ')'
		case 42: // '*'
		case 43: // '+'
		case 44: // ','
		case 45: // '-'
		case 46: // '.'
		case 47: // '/'
		case 48: // '0'
		case 49: // '1'
		case 50: // '2'
		case 51: // '3'
		case 52: // '4'
		case 53: // '5'
		case 59: // ';'
		case 60: // '<'
		case 61: // '='
		case 62: // '>'
		case 63: // '?'
		case 64: // '@'
		case 65: // 'A'
		case 66: // 'B'
		case 67: // 'C'
		case 68: // 'D'
		case 69: // 'E'
		case 70: // 'F'
		case 71: // 'G'
		case 72: // 'H'
		case 73: // 'I'
		case 74: // 'J'
		case 75: // 'K'
		case 76: // 'L'
		case 77: // 'M'
		case 78: // 'N'
		case 79: // 'O'
		case 80: // 'P'
		case 81: // 'Q'
		case 82: // 'R'
		case 83: // 'S'
		case 84: // 'T'
		case 85: // 'U'
		case 86: // 'V'
		case 87: // 'W'
		case 88: // 'X'
		case 89: // 'Y'
		case 90: // 'Z'
		case 91: // '['
		case 92: // '\\'
		case 93: // ']'
		case 94: // '^'
		case 95: // '_'
		case 96: // '`'
		case 97: // 'a'
		case 98: // 'b'
		case 99: // 'c'
		case 100: // 'd'
		case 101: // 'e'
		case 102: // 'f'
		case 103: // 'g'
		case 104: // 'h'
		case 105: // 'i'
		case 106: // 'j'
		case 107: // 'k'
		case 108: // 'l'
		case 109: // 'm'
		case 110: // 'n'
		case 111: // 'o'
		case 112: // 'p'
		case 113: // 'q'
		case 114: // 'r'
		case 115: // 's'
		case 116: // 't'
		case 117: // 'u'
		case 118: // 'v'
		case 119: // 'w'
		case 120: // 'x'
		case 121: // 'y'
		case 122: // 'z'
		case 123: // '{'
		case 124: // '|'
		case 125: // '}'
		case 126: // '~'
		case 127: // '\177'
		case 128:
		case 129:
		case 130:
		case 131:
		case 133:
		case 134:
		case 135:
		case 136:
		case 137:
		case 138:
		case 139:
		case 140:
		case 141:
		case 142:
		case 143:
		case 144:
		case 145:
		case 146:
		case 147:
		case 148:
		case 149:
		case 150:
		case 151:
		case 152:
		case 172:
		case 173:
		case 174:
		case 175:
		case 176:
		case 177:
		case 186:
		case 190:
		case 191:
		case 194:
		case 195:
		case 196:
		default:
			return 1;
		}
	}

	void write(final DataOutput dataoutputstream, final ConstantPool constantpool) throws IOException {
		switch (this.opc) {
		case -3:
		case -2:
		case -1:
			break;

		case 16: // '\020'
		case 21: // '\025'
		case 22: // '\026'
		case 23: // '\027'
		case 24: // '\030'
		case 25: // '\031'
		case 169:
		case 188:
			dataoutputstream.writeByte(this.opc);
			dataoutputstream.writeByte(((Number) this.value).intValue());
			break;

		case 277:
		case 278:
		case 279:
		case 280:
		case 281:
		case 425:
			dataoutputstream.writeByte(196);
			dataoutputstream.writeByte(this.opc - 256);
			dataoutputstream.writeShort(((Number) this.value).intValue());
			break;

		case 54: // '6'
		case 55: // '7'
		case 56: // '8'
		case 57: // '9'
		case 58: // ':'
			dataoutputstream.writeByte(this.opc);
			dataoutputstream.writeByte(this.value instanceof Number ? ((Number) this.value).intValue() : ((LocalVariable) this.value).slot);
			break;

		case 310:
		case 311:
		case 312:
		case 313:
		case 314:
			dataoutputstream.writeByte(196);
			dataoutputstream.writeByte(this.opc - 256);
			dataoutputstream.writeShort(this.value instanceof Number ? ((Number) this.value).intValue() : ((LocalVariable) this.value).slot);
			break;

		case 17: // '\021'
			dataoutputstream.writeByte(this.opc);
			dataoutputstream.writeShort(((Number) this.value).intValue());
			break;

		case 18: // '\022'
			dataoutputstream.writeByte(this.opc);
			dataoutputstream.writeByte(constantpool.index(this.value));
			break;

		case 19: // '\023'
		case 20: // '\024'
		case 178:
		case 179:
		case 180:
		case 181:
		case 182:
		case 183:
		case 184:
		case 187:
		case 192:
		case 193:
			dataoutputstream.writeByte(this.opc);
			dataoutputstream.writeShort(constantpool.index(this.value));
			break;

		case 132:
			dataoutputstream.writeByte(this.opc);
			dataoutputstream.writeByte(((int[]) this.value)[0]);
			dataoutputstream.writeByte(((int[]) this.value)[1]);
			break;

		case 388:
			dataoutputstream.writeByte(196);
			dataoutputstream.writeByte(this.opc - 256);
			dataoutputstream.writeShort(((int[]) this.value)[0]);
			dataoutputstream.writeShort(((int[]) this.value)[1]);
			break;

		case 189:
			dataoutputstream.writeByte(this.opc);
			dataoutputstream.writeShort(constantpool.index(this.value));
			break;

		case 197:
			dataoutputstream.writeByte(this.opc);
			dataoutputstream.writeShort(constantpool.index(((ArrayData) this.value).type));
			dataoutputstream.writeByte(((ArrayData) this.value).nargs);
			break;

		case 185:
			dataoutputstream.writeByte(this.opc);
			dataoutputstream.writeShort(constantpool.index(this.value));
			dataoutputstream.writeByte(((MemberDefinition) this.value).getType().stackSize() + 1);
			dataoutputstream.writeByte(0);
			break;

		case 153:
		case 154:
		case 155:
		case 156:
		case 157:
		case 158:
		case 159:
		case 160:
		case 161:
		case 162:
		case 163:
		case 164:
		case 165:
		case 166:
		case 167:
		case 168:
		case 198:
		case 199:
			dataoutputstream.writeByte(this.opc);
			dataoutputstream.writeShort(((Instruction) this.value).pc - this.pc);
			break;

		case 200:
		case 201:
			dataoutputstream.writeByte(this.opc);
			dataoutputstream.writeLong(((Instruction) this.value).pc - this.pc);
			break;

		case 170:
			final SwitchData switchdata = (SwitchData) this.value;
			dataoutputstream.writeByte(this.opc);
			for (int i = 1; (this.pc + i) % 4 != 0; i++) {
				dataoutputstream.writeByte(0);
			}

			dataoutputstream.writeInt(((Instruction) switchdata.defaultLabel).pc - this.pc);
			dataoutputstream.writeInt(switchdata.minValue);
			dataoutputstream.writeInt(switchdata.maxValue);
			for (int k = switchdata.minValue; k <= switchdata.maxValue; k++) {
				final Label label = switchdata.get(k);
				final int l = label == null ? ((Instruction) switchdata.defaultLabel).pc : ((Instruction) label).pc;
				dataoutputstream.writeInt(l - this.pc);
			}

			break;

		case 171:
			final SwitchData switchdata1 = (SwitchData) this.value;
			dataoutputstream.writeByte(this.opc);
			for (int j = this.pc + 1; j % 4 != 0; j++) {
				dataoutputstream.writeByte(0);
			}

			dataoutputstream.writeInt(((Instruction) switchdata1.defaultLabel).pc - this.pc);
			dataoutputstream.writeInt(switchdata1.tab.size());
			Integer integer;
			for (final Enumeration enumeration = switchdata1.sortedKeys(); enumeration.hasMoreElements(); dataoutputstream.writeInt(((Instruction) switchdata1.get(integer)).pc - this.pc)) {
				integer = (Integer) enumeration.nextElement();
				dataoutputstream.writeInt(integer.intValue());
			}

			break;

		case 0: // '\0'
			if (this.value != null) {
				if (this.value instanceof Integer) {
					dataoutputstream.writeByte(((Number) this.value).intValue());
				} else {
					dataoutputstream.writeShort(constantpool.index(this.value));
				}
				return;
			}
			// fall through

		default:
			dataoutputstream.writeByte(this.opc);
			break;
		}
	}

	public String toString() {
		final String s = (this.where >> 32) + ":\t";
		switch (this.opc) {
		case -3:
			return s + "try " + ((TryData) this.value).getEndLabel().hashCode();

		case -2:
			return s + "dead";

		case 132:
			final int i = ((int[]) this.value)[0];
			final int j = ((int[]) this.value)[1];
			return s + RuntimeConstants.opcNames[this.opc] + ' ' + i + ", " + j;
		}
		if (this.value != null) {
			if (this.value instanceof Label) {
				return s + RuntimeConstants.opcNames[this.opc] + ' ' + this.value;
			}
			if (this.value instanceof Instruction) {
				return s + RuntimeConstants.opcNames[this.opc] + ' ' + this.value.hashCode();
			}
			return this.value instanceof String ? s + RuntimeConstants.opcNames[this.opc] + " \"" + this.value + '"' : s + RuntimeConstants.opcNames[this.opc] + ' ' + this.value;
		}
		return s + RuntimeConstants.opcNames[this.opc];
	}

	long where;
	int pc;
	int opc;
	Object value;
	Instruction next;
	boolean flagCondInverted;
	boolean flagNoCovered;
	private static final double SWITCHRATIO;

	static {
		double d = 1.5D;
		final String s = System.getProperty("javac.switchratio");
		if (s != null) {
			try {
				final double d1 = Double.valueOf(s).doubleValue();
				if (!Double.isNaN(d1) && d1 >= 0.0D) {
					d = d1;
				}
			} catch (final NumberFormatException ignored) {
			}
		}
		SWITCHRATIO = d;
	}
}
