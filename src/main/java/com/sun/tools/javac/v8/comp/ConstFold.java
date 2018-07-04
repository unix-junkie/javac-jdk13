package com.sun.tools.javac.v8.comp;

import com.sun.tools.javac.v8.code.Type;
import com.sun.tools.javac.v8.util.List;
import com.sun.tools.javac.v8.util.Log;

final class ConstFold {

	ConstFold(final Log log1, final Symtab symtab) {
		this.syms = symtab;
	}

	private static Integer b2i(final boolean flag) {
		return flag ? one : zero;
	}

	private static int intValue(final Object obj) {
		return ((Number) obj).intValue();
	}

	private static long longValue(final Object obj) {
		return ((Number) obj).longValue();
	}

	private static float floatValue(final Object obj) {
		return ((Number) obj).floatValue();
	}

	private static double doubleValue(final Object obj) {
		return ((Number) obj).doubleValue();
	}

	Type fold(final int i, final List list) {
		final int j = list.length();
		if (j == 1) {
			return fold1(i, (Type) list.head);
		}
		if (j == 2) {
			return this.fold2(i, (Type) list.head, (Type) list.tail.head);
		}
		throw new InternalError();
	}

	private static Type fold1(final int i, final Type type) {
		try {
			final Object obj = type.constValue;
			switch (i) {
			case 0: // '\0'
				return type;

			case 116: // 't'
				return Type.intType.constType(new Integer(-intValue(obj)));

			case 130:
				return Type.intType.constType(new Integer(~intValue(obj)));

			case 257:
				return Type.booleanType.constType(new Integer(~intValue(obj) & 1));

			case 153:
				return Type.booleanType.constType(b2i(intValue(obj) == 0));

			case 154:
				return Type.booleanType.constType(b2i(intValue(obj) != 0));

			case 155:
				return Type.booleanType.constType(b2i(intValue(obj) < 0));

			case 157:
				return Type.booleanType.constType(b2i(intValue(obj) > 0));

			case 158:
				return Type.booleanType.constType(b2i(intValue(obj) <= 0));

			case 156:
				return Type.booleanType.constType(b2i(intValue(obj) >= 0));

			case 117: // 'u'
				return Type.longType.constType(new Long(-longValue(obj)));

			case 131:
				return Type.longType.constType(new Long(~longValue(obj)));

			case 118: // 'v'
				return Type.floatType.constType(new Float(-floatValue(obj)));

			case 119: // 'w'
				return Type.doubleType.constType(new Double(-doubleValue(obj)));
			}
			return null;
		} catch (final ArithmeticException ignored) {
			return null;
		}
	}

	private Type fold2(final int i, final Type type, final Type type1) {
		try {
			if (i > 511) {
				final Type type2 = this.fold2(i >> 9, type, type1);
				return type2.constValue != null ? fold1(i & 0x1ff, type2) : type2;
			}
			final Object obj = type.constValue;
			final Object obj1 = type1.constValue;
			switch (i) {
			case 96: // '`'
				return Type.intType.constType(new Integer(intValue(obj) + intValue(obj1)));

			case 100: // 'd'
				return Type.intType.constType(new Integer(intValue(obj) - intValue(obj1)));

			case 104: // 'h'
				return Type.intType.constType(new Integer(intValue(obj) * intValue(obj1)));

			case 108: // 'l'
				return Type.intType.constType(new Integer(intValue(obj) / intValue(obj1)));

			case 112: // 'p'
				return Type.intType.constType(new Integer(intValue(obj) % intValue(obj1)));

			case 126: // '~'
				return Type.intType.constType(new Integer(intValue(obj) & intValue(obj1)));

			case 258:
				return Type.booleanType.constType(new Integer(intValue(obj) & intValue(obj1)));

			case 128:
				return Type.intType.constType(new Integer(intValue(obj) | intValue(obj1)));

			case 259:
				return Type.booleanType.constType(new Integer(intValue(obj) | intValue(obj1)));

			case 130:
				return Type.intType.constType(new Integer(intValue(obj) ^ intValue(obj1)));

			case 120: // 'x'
			case 270:
				return Type.intType.constType(new Integer(intValue(obj) << intValue(obj1)));

			case 122: // 'z'
			case 272:
				return Type.intType.constType(new Integer(intValue(obj) >> intValue(obj1)));

			case 124: // '|'
			case 274:
				return Type.intType.constType(new Integer(intValue(obj) >>> intValue(obj1)));

			case 159:
				return Type.booleanType.constType(b2i(intValue(obj) == intValue(obj1)));

			case 160:
				return Type.booleanType.constType(b2i(intValue(obj) != intValue(obj1)));

			case 161:
				return Type.booleanType.constType(b2i(intValue(obj) < intValue(obj1)));

			case 163:
				return Type.booleanType.constType(b2i(intValue(obj) > intValue(obj1)));

			case 164:
				return Type.booleanType.constType(b2i(intValue(obj) <= intValue(obj1)));

			case 162:
				return Type.booleanType.constType(b2i(intValue(obj) >= intValue(obj1)));

			case 97: // 'a'
				return Type.longType.constType(new Long(longValue(obj) + longValue(obj1)));

			case 101: // 'e'
				return Type.longType.constType(new Long(longValue(obj) - longValue(obj1)));

			case 105: // 'i'
				return Type.longType.constType(new Long(longValue(obj) * longValue(obj1)));

			case 109: // 'm'
				return Type.longType.constType(new Long(longValue(obj) / longValue(obj1)));

			case 113: // 'q'
				return Type.longType.constType(new Long(longValue(obj) % longValue(obj1)));

			case 127: // '\177'
				return Type.longType.constType(new Long(longValue(obj) & longValue(obj1)));

			case 129:
				return Type.longType.constType(new Long(longValue(obj) | longValue(obj1)));

			case 131:
				return Type.longType.constType(new Long(longValue(obj) ^ longValue(obj1)));

			case 121: // 'y'
			case 271:
				return Type.longType.constType(new Long(longValue(obj) << intValue(obj1)));

			case 123: // '{'
			case 273:
				return Type.longType.constType(new Long(longValue(obj) >> intValue(obj1)));

			case 125: // '}'
				return Type.longType.constType(new Long(longValue(obj) >>> intValue(obj1)));

			case 148:
				if (longValue(obj) < longValue(obj1)) {
					return Type.intType.constType(minusOne);
				}
				return longValue(obj) > longValue(obj1) ? Type.intType.constType(one) : Type.intType.constType(zero);

			case 98: // 'b'
				return Type.floatType.constType(new Float(floatValue(obj) + floatValue(obj1)));

			case 102: // 'f'
				return Type.floatType.constType(new Float(floatValue(obj) - floatValue(obj1)));

			case 106: // 'j'
				return Type.floatType.constType(new Float(floatValue(obj) * floatValue(obj1)));

			case 110: // 'n'
				return Type.floatType.constType(new Float(floatValue(obj) / floatValue(obj1)));

			case 114: // 'r'
				return Type.floatType.constType(new Float(floatValue(obj) % floatValue(obj1)));

			case 149:
			case 150:
				if (floatValue(obj) < floatValue(obj1)) {
					return Type.intType.constType(minusOne);
				}
				if (floatValue(obj) > floatValue(obj1)) {
					return Type.intType.constType(one);
				}
				if (floatValue(obj) == floatValue(obj1)) {
					return Type.intType.constType(zero);
				}
				return i == 150 ? Type.intType.constType(one) : Type.intType.constType(minusOne);

			case 99: // 'c'
				return Type.doubleType.constType(new Double(doubleValue(obj) + doubleValue(obj1)));

			case 103: // 'g'
				return Type.doubleType.constType(new Double(doubleValue(obj) - doubleValue(obj1)));

			case 107: // 'k'
				return Type.doubleType.constType(new Double(doubleValue(obj) * doubleValue(obj1)));

			case 111: // 'o'
				return Type.doubleType.constType(new Double(doubleValue(obj) / doubleValue(obj1)));

			case 115: // 's'
				return Type.doubleType.constType(new Double(doubleValue(obj) % doubleValue(obj1)));

			case 151:
			case 152:
				if (doubleValue(obj) < doubleValue(obj1)) {
					return Type.intType.constType(minusOne);
				}
				if (doubleValue(obj) > doubleValue(obj1)) {
					return Type.intType.constType(one);
				}
				if (doubleValue(obj) == doubleValue(obj1)) {
					return Type.intType.constType(zero);
				}
				return i == 152 ? Type.intType.constType(one) : Type.intType.constType(minusOne);

			case 165:
				return Type.booleanType.constType(b2i(obj.equals(obj1)));

			case 166:
				return Type.booleanType.constType(b2i(!obj.equals(obj1)));

			case 256:
				return this.syms.stringType.constType(type.stringValue() + type1.stringValue());

			case 116: // 't'
			case 117: // 'u'
			case 118: // 'v'
			case 119: // 'w'
			case 132:
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
			case 153:
			case 154:
			case 155:
			case 156:
			case 157:
			case 158:
			case 167:
			case 168:
			case 169:
			case 170:
			case 171:
			case 172:
			case 173:
			case 174:
			case 175:
			case 176:
			case 177:
			case 178:
			case 179:
			case 180:
			case 181:
			case 182:
			case 183:
			case 184:
			case 185:
			case 186:
			case 187:
			case 188:
			case 189:
			case 190:
			case 191:
			case 192:
			case 193:
			case 194:
			case 195:
			case 196:
			case 197:
			case 198:
			case 199:
			case 200:
			case 201:
			case 202:
			case 203:
			case 204:
			case 205:
			case 206:
			case 207:
			case 208:
			case 209:
			case 210:
			case 211:
			case 212:
			case 213:
			case 214:
			case 215:
			case 216:
			case 217:
			case 218:
			case 219:
			case 220:
			case 221:
			case 222:
			case 223:
			case 224:
			case 225:
			case 226:
			case 227:
			case 228:
			case 229:
			case 230:
			case 231:
			case 232:
			case 233:
			case 234:
			case 235:
			case 236:
			case 237:
			case 238:
			case 239:
			case 240:
			case 241:
			case 242:
			case 243:
			case 244:
			case 245:
			case 246:
			case 247:
			case 248:
			case 249:
			case 250:
			case 251:
			case 252:
			case 253:
			case 254:
			case 255:
			case 257:
			case 260:
			case 261:
			case 262:
			case 263:
			case 264:
			case 265:
			case 266:
			case 267:
			case 268:
			case 269:
			default:
				return null;
			}
		} catch (final ArithmeticException ignored) {
			return null;
		}
	}

	static Type coerce(final Type type, final Type type1) {
		if (type.baseType() == type1.baseType()) {
			return type;
		}
		if (type.tag <= 7) {
			final Object obj = type.constValue;
			switch (type1.tag) {
			case 1: // '\001'
				return Type.byteType.constType(new Integer((byte) intValue(obj)));

			case 2: // '\002'
				return Type.charType.constType(new Integer((char) intValue(obj)));

			case 3: // '\003'
				return Type.shortType.constType(new Integer((short) intValue(obj)));

			case 4: // '\004'
				return Type.intType.constType(new Integer(intValue(obj)));

			case 5: // '\005'
				return Type.longType.constType(new Long(longValue(obj)));

			case 6: // '\006'
				return Type.floatType.constType(new Float(floatValue(obj)));

			case 7: // '\007'
				return Type.doubleType.constType(new Double(doubleValue(obj)));
			}
		}
		return type1;
	}

	private final Symtab syms;
	private static final Integer minusOne = new Integer(-1);
	private static final Integer zero = new Integer(0);
	private static final Integer one = new Integer(1);

}
