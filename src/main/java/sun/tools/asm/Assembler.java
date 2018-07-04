package sun.tools.asm;

import java.io.BufferedReader;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import sun.tools.java.ClassDefinition;
import sun.tools.java.CompilerError;
import sun.tools.java.Constants;
import sun.tools.java.Environment;
import sun.tools.java.MemberDefinition;
import sun.tools.java.Type;
import sun.tools.javac.SourceClass;

public final class Assembler {

	public Assembler() {
		this.first = new Label();
		this.last = this.first;
	}

	public void add(final Instruction instruction) {
		if (instruction != null) {
			this.last.next = instruction;
			this.last = instruction;
		}
	}

	public void add(final long l, final int i) {
		this.add(new Instruction(l, i, null));
	}

	public void add(final long l, final int i, final Object obj) {
		this.add(new Instruction(l, i, obj));
	}

	public void add(final long l, final int i, final Object obj, final boolean flag) {
		this.add(new Instruction(l, i, obj, flag));
	}

	public void add(final boolean flag, final long l, final int i, final Object obj) {
		this.add(new Instruction(flag, l, i, obj));
	}

	public void add(final long l, final int i, final boolean flag) {
		this.add(new Instruction(l, i, flag));
	}

	private void optimize(final Environment environment, final Instruction label) {
		label.pc = 1;
		for (Instruction instruction = label.next; instruction != null; instruction = instruction.next) {
			switch (instruction.pc) {
			case 1: // '\001'
				return;

			case 0: // '\0'
				instruction.optimize(environment);
				instruction.pc = 1;
				// fall through

			case 2: // '\002'
			default:
				switch (instruction.opc) {
				default:
					break;

				case -2:
				case -1:
					if (instruction.pc == 1) {
						instruction.pc = 0;
					}
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
				case 198:
				case 199:
					this.optimize(environment, (Instruction) instruction.value);
					break;

				case 167:
					this.optimize(environment, (Instruction) instruction.value);
					return;

				case 168:
					this.optimize(environment, (Instruction) instruction.value);
					break;

				case 169:
				case 172:
				case 173:
				case 174:
				case 175:
				case 176:
				case 177:
				case 191:
					return;

				case 170:
				case 171:
					final SwitchData switchdata = (SwitchData) instruction.value;
					this.optimize(environment, switchdata.defaultLabel);
					for (final Iterator iterator = switchdata.tab.values().iterator(); iterator.hasNext(); this.optimize(environment, (Instruction) iterator.next())) {
					}
					return;

				case -3:
					final TryData trydata = (TryData) instruction.value;
					trydata.getEndLabel().pc = 2;
					CatchData catchdata;
					for (final Iterator iterator = trydata.catches.iterator(); iterator.hasNext(); this.optimize(environment, catchdata.getLabel())) {
						catchdata = (CatchData) iterator.next();
					}

					break;
				}
				break;
			}
		}

	}

	private boolean eliminate() {
		boolean flag = false;
		Instruction obj = this.first;
		for (Instruction instruction = ((Instruction) this.first).next; instruction != null; instruction = instruction.next) {
			if (instruction.pc != 0) {
				obj.next = instruction;
				obj = instruction;
				instruction.pc = 0;
			} else {
				flag = true;
			}
		}

		this.first.pc = 0;
		obj.next = null;
		return flag;
	}

	public void optimize(final Environment environment) {
		do {
			this.optimize(environment, this.first);
		} while (this.eliminate() && environment.opt());
	}

	public void collect(final Environment environment, final MemberDefinition memberdefinition, final ConstantPool constantpool) {
		if (memberdefinition != null && environment.debug_vars() && memberdefinition.getArguments() != null) {
			MemberDefinition memberdefinition1;
			for (final Iterator it = memberdefinition.getArguments().iterator(); it.hasNext(); constantpool.put(memberdefinition1.getType().getTypeSignature())) {
				memberdefinition1 = (MemberDefinition) it.next();
				constantpool.put(memberdefinition1.getName().toString());
			}

		}
		for (Object obj = this.first; obj != null; obj = ((Instruction) obj).next) {
			((Instruction) obj).collect(constantpool);
		}

	}

	private void balance(Label label, int i) {
		for (Object obj = label; obj != null; obj = ((Instruction) obj).next) {
			i += ((Instruction) obj).balance();
			if (i < 0) {
				throw new CompilerError("stack under flow: " + obj + " = " + i);
			}
			if (i > this.maxdepth) {
				this.maxdepth = i;
			}
			switch (((Instruction) obj).opc) {
			default:
				break;

			case -1:
				label = (Label) obj;
				if (((Instruction) obj).pc == 1) {
					if (label.depth != i) {
						throw new CompilerError("stack depth error " + i + '/' + label.depth + ": " + obj);
					}
					return;
				}
				label.pc = 1;
				label.depth = i;
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
			case 198:
			case 199:
				this.balance((Label) ((Instruction) obj).value, i);
				break;

			case 167:
				this.balance((Label) ((Instruction) obj).value, i);
				return;

			case 168:
				this.balance((Label) ((Instruction) obj).value, i + 1);
				break;

			case 169:
			case 172:
			case 173:
			case 174:
			case 175:
			case 176:
			case 177:
			case 191:
				return;

			case 21: // '\025'
			case 23: // '\027'
			case 25: // '\031'
			case 54: // '6'
			case 56: // '8'
			case 58: // ':'
				final int j = (((Instruction) obj).value instanceof Number ? ((Number) ((Instruction) obj).value).intValue() : ((LocalVariable) ((Instruction) obj).value).slot) + 1;
				if (j > this.maxvar) {
					this.maxvar = j;
				}
				break;

			case 22: // '\026'
			case 24: // '\030'
			case 55: // '7'
			case 57: // '9'
				final int k = (((Instruction) obj).value instanceof Number ? ((Number) ((Instruction) obj).value).intValue() : ((LocalVariable) ((Instruction) obj).value).slot) + 2;
				if (k > this.maxvar) {
					this.maxvar = k;
				}
				break;

			case 132:
				final int l = ((int[]) ((Instruction) obj).value)[0] + 1;
				if (l > this.maxvar) {
					this.maxvar = l + 1;
				}
				break;

			case 170:
			case 171:
				final SwitchData switchdata = (SwitchData) ((Instruction) obj).value;
				this.balance(switchdata.defaultLabel, i);
				for (final Iterator iterator = switchdata.tab.values().iterator(); iterator.hasNext(); this.balance((Label) iterator.next(), i)) {
				}
				return;

			case -3:
				final TryData trydata = (TryData) ((Instruction) obj).value;
				CatchData catchdata;
				for (final Iterator iterator = trydata.catches.iterator(); iterator.hasNext(); this.balance(catchdata.getLabel(), i + 1)) {
					catchdata = (CatchData) iterator.next();
				}

				break;
			}
		}

	}

	public void write(final Environment environment, final DataOutputStream dataoutputstream, final MemberDefinition memberdefinition, final ConstantPool constantpool) throws IOException {
		if (memberdefinition != null && memberdefinition.getArguments() != null) {
			int i = 0;
			final List arguments = memberdefinition.getArguments();
			for (final Iterator it = arguments.iterator(); it.hasNext();) {
				final MemberDefinition memberdefinition1 = (MemberDefinition) it.next();
				i += memberdefinition1.getType().stackSize();
			}

			this.maxvar = i;
		}
		try {
			this.balance(this.first, 0);
		} catch (final CompilerError compilererror) {
			System.out.println("ERROR: " + compilererror);
			this.listing(System.out);
			throw compilererror;
		}
		int j = 0;
		int k = 0;
		for (Instruction obj = this.first; obj != null; obj = obj.next) {
			obj.pc = j;
			final int l = obj.size(constantpool);
			if (j < 0x10000 && j + l >= 0x10000) {
				environment.error(obj.where, "warn.method.too.long");
			}
			j += l;
			if (obj.opc == -3) {
				k += ((TryData) obj.value).catches.size();
			}
		}

		dataoutputstream.writeShort(this.maxdepth);
		dataoutputstream.writeShort(this.maxvar);
		dataoutputstream.writeInt(this.maxpc = j);
		for (Instruction instruction = ((Instruction) this.first).next; instruction != null; instruction = instruction.next) {
			instruction.write(dataoutputstream, constantpool);
		}

		dataoutputstream.writeShort(k);
		if (k > 0) {
			this.writeExceptions(environment, dataoutputstream, constantpool, this.first, this.last);
		}
	}

	private void writeExceptions(final Environment environment, final DataOutputStream dataoutputstream, final ConstantPool constantpool, final Instruction instruction, final Instruction instruction1) throws IOException {
		for (Object obj = instruction; obj != instruction1.next; obj = ((Instruction) obj).next) {
			if (((Instruction) obj).opc == -3) {
				final TryData trydata = (TryData) ((Instruction) obj).value;
				this.writeExceptions(environment, dataoutputstream, constantpool, ((Instruction) obj).next, trydata.getEndLabel());
				for (final Iterator iterator = trydata.catches.iterator(); iterator.hasNext();) {
					final CatchData catchdata = (CatchData) iterator.next();
					dataoutputstream.writeShort(((Instruction) obj).pc);
					dataoutputstream.writeShort(trydata.getEndLabel().pc);
					dataoutputstream.writeShort(((Instruction) catchdata.getLabel()).pc);
					if (catchdata.getType() != null) {
						dataoutputstream.writeShort(constantpool.index(catchdata.getType()));
					} else {
						dataoutputstream.writeShort(0);
					}
				}

				obj = trydata.getEndLabel();
			}
		}

	}

	public void writeCoverageTable(final Environment environment, final ClassDefinition classdefinition, final DataOutput dataoutputstream, final ConstantPool constantpool, final long l) throws IOException {
		final Vector vector = new Vector();
		boolean flag = false;
		boolean flag1 = false;
		final long l1 = ((SourceClass) classdefinition).getWhere();
		final Vector vector1 = new Vector();
		int i = 0;
		for (Object obj = this.first; obj != null; obj = ((Instruction) obj).next) {
			final long l2 = ((Instruction) obj).where >> 32;
			if (l2 > 0L && ((Instruction) obj).opc != -1) {
				if (!flag1) {
					if (l1 == ((Instruction) obj).where) {
						vector.addElement(new Cover(2, l, ((Instruction) obj).pc));
					} else {
						vector.addElement(new Cover(1, l, ((Instruction) obj).pc));
					}
					i++;
					flag1 = true;
				}
				if (!flag && !((Instruction) obj).flagNoCovered) {
					boolean flag3 = false;
					for (final Iterator iterator = vector1.iterator(); iterator.hasNext();) {
						if (((Number) iterator.next()).longValue() == ((Instruction) obj).where) {
							flag3 = true;
							break;
						}
					}

					if (!flag3) {
						vector.addElement(new Cover(3, ((Instruction) obj).where, ((Instruction) obj).pc));
						i++;
						flag = true;
					}
				}
			}
			switch (((Instruction) obj).opc) {
			case 169:
			case 172:
			case 173:
			case 174:
			case 175:
			case 176:
			case 177:
			case 191:
			default:
				break;

			case -1:
				flag = false;
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
			case 198:
			case 199:
				if (((Instruction) obj).flagCondInverted) {
					vector.addElement(new Cover(7, ((Instruction) obj).where, ((Instruction) obj).pc));
					vector.addElement(new Cover(8, ((Instruction) obj).where, ((Instruction) obj).pc));
				} else {
					vector.addElement(new Cover(8, ((Instruction) obj).where, ((Instruction) obj).pc));
					vector.addElement(new Cover(7, ((Instruction) obj).where, ((Instruction) obj).pc));
				}
				i += 2;
				flag = false;
				break;

			case 167:
				flag = false;
				break;

			case -3:
				vector1.addElement(new Long(((Instruction) obj).where));
				flag = false;
				break;

			case 170:
				final SwitchData switchdata = (SwitchData) ((Instruction) obj).value;
				for (int j = switchdata.minValue; j <= switchdata.maxValue; j++) {
					vector.addElement(new Cover(5, switchdata.whereCase(new Integer(j)), ((Instruction) obj).pc));
					i++;
				}

				if (!switchdata.getDefault()) {
					vector.addElement(new Cover(6, ((Instruction) obj).where, ((Instruction) obj).pc));
					i++;
				} else {
					vector.addElement(new Cover(5, switchdata.whereCase("default"), ((Instruction) obj).pc));
					i++;
				}
				flag = false;
				break;

			case 171:
				final SwitchData switchdata1 = (SwitchData) ((Instruction) obj).value;
				for (final Enumeration enumeration1 = switchdata1.sortedKeys(); enumeration1.hasMoreElements();) {
					final Integer integer = (Integer) enumeration1.nextElement();
					vector.addElement(new Cover(5, switchdata1.whereCase(integer), ((Instruction) obj).pc));
					i++;
				}

				if (!switchdata1.getDefault()) {
					vector.addElement(new Cover(6, ((Instruction) obj).where, ((Instruction) obj).pc));
					i++;
				} else {
					vector.addElement(new Cover(5, switchdata1.whereCase("default"), ((Instruction) obj).pc));
					i++;
				}
				flag = false;
				break;
			}
		}

		dataoutputstream.writeShort(i);
		for (int k = 0; k < i; k++) {
			final Cover cover = (Cover) vector.elementAt(k);
			final long l3 = cover.Addr >> 32;
			final long l4 = cover.Addr << 32 >> 32;
			dataoutputstream.writeShort(cover.NumCommand);
			dataoutputstream.writeShort(cover.Type);
			dataoutputstream.writeInt((int) l3);
			dataoutputstream.writeInt((int) l4);
			if (cover.Type != 5 || cover.Addr != 0L) {
				JcovClassCountArray[cover.Type]++;
			}
		}

	}

	public static void addNativeToJcovTab(final Environment environment, final ClassDefinition classdefinition) {
		JcovClassCountArray[1]++;
	}

	private static String createClassJcovElement(final Environment environment, final ClassDefinition classdefinition) {
		final String s = Type.mangleInnerType(classdefinition.getClassDeclaration().getName()).toString();
		SourceClassList.addElement(s);
		final String s1 = s.replace('.', '/');
		String s2 = JcovClassLine + s1;
		s2 += " [";
		String s3 = "";
		for (int i = 0; i < arrayModifiers.length; i++) {
			if ((classdefinition.getModifiers() & arrayModifiers[i]) != 0) {
				s2 = s2 + s3 + Constants.opNames[arrayModifiersOpc[i]];
				s3 = " ";
			}
		}

		s2 += "]";
		return s2;
	}

	public static void GenVecJCov(final Environment environment, final ClassDefinition classdefinition, final long l) {
		final String s = ((SourceClass) classdefinition).getAbsoluteName();
		TmpCovTable.addElement(createClassJcovElement(environment, classdefinition));
		TmpCovTable.addElement(JcovSrcfileLine + s);
		TmpCovTable.addElement(JcovTimestampLine + l);
		TmpCovTable.addElement(JcovDataLine + 'A');
		TmpCovTable.addElement(JcovHeadingLine);
		for (int i = 1; i <= 8; i++) {
			if (JcovClassCountArray[i] != 0) {
				TmpCovTable.addElement(i + "\t" + JcovClassCountArray[i]);
				JcovClassCountArray[i] = 0;
			}
		}

	}

	public static void GenJCov(final Environment environment) {
		try {
			final File file = environment.getcovFile();
			if (file.exists()) {
				final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				String s = in.readLine();
				if (s != null && s.startsWith(JcovMagicLine)) {
					boolean flag = true;
					while ((s = in.readLine()) != null) {
						if (s.startsWith(JcovClassLine)) {
							flag = true;
							for (final Iterator iterator = SourceClassList.iterator(); iterator.hasNext();) {
								String s2 = s.substring(JcovClassLine.length());
								final int i = s2.indexOf(' ');
								if (i != -1) {
									s2 = s2.substring(0, i);
								}
								final String s1 = (String) iterator.next();
								if (s1.compareTo(s2) == 0) {
									flag = false;
									break;
								}
							}

						}
						if (flag) {
							TmpCovTable.addElement(s);
						}
					}
				}
				in.close();
			}
			final PrintStream printstream = new PrintStream(new DataOutputStream(new FileOutputStream(file)));
			printstream.println(JcovMagicLine);
			for (final Iterator iterator = TmpCovTable.iterator(); iterator.hasNext(); printstream.println(iterator.next())) {
			}
			printstream.close();
		} catch (final FileNotFoundException filenotfoundexception) {
			System.out.println("ERROR: " + filenotfoundexception);
		} catch (final IOException ioexception) {
			System.out.println("ERROR: " + ioexception);
		}
	}

	public void writeLineNumberTable(final Environment environment, final DataOutput dataoutputstream, final ConstantPool constantpool) throws IOException {
		long l = -1L;
		int i = 0;
		for (Object obj = this.first; obj != null; obj = ((Instruction) obj).next) {
			final long l1 = ((Instruction) obj).where >> 32;
			if (l1 > 0L && l != l1) {
				l = l1;
				i++;
			}
		}

		l = -1L;
		dataoutputstream.writeShort(i);
		for (Object obj1 = this.first; obj1 != null; obj1 = ((Instruction) obj1).next) {
			final long l2 = ((Instruction) obj1).where >> 32;
			if (l2 > 0L && l != l2) {
				l = l2;
				dataoutputstream.writeShort(((Instruction) obj1).pc);
				dataoutputstream.writeShort((int) l);
			}
		}

	}

	private void flowFields(final Environment environment, final Label label, MemberDefinition amemberdefinition[]) {
		if (label.locals != null) {
			final MemberDefinition amemberdefinition1[] = label.locals;
			for (int i = 0; i < this.maxvar; i++) {
				if (amemberdefinition1[i] != amemberdefinition[i]) {
					amemberdefinition1[i] = null;
				}
			}

			return;
		}
		label.locals = new MemberDefinition[this.maxvar];
		System.arraycopy(amemberdefinition, 0, label.locals, 0, this.maxvar);
		final MemberDefinition amemberdefinition2[] = new MemberDefinition[this.maxvar];
		System.arraycopy(amemberdefinition, 0, amemberdefinition2, 0, this.maxvar);
		amemberdefinition = amemberdefinition2;
		for (Instruction instruction = ((Instruction) label).next; instruction != null; instruction = instruction.next) {
			switch (instruction.opc) {
			case -2:
			case 0: // '\0'
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
			case 16: // '\020'
			case 17: // '\021'
			case 18: // '\022'
			case 19: // '\023'
			case 20: // '\024'
			case 21: // '\025'
			case 22: // '\026'
			case 23: // '\027'
			case 24: // '\030'
			case 25: // '\031'
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
			case 148:
			case 149:
			case 150:
			case 151:
			case 152:
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
			case 192:
			case 193:
			case 194:
			case 195:
			case 196:
			case 197:
			default:
				break;

			case 54: // '6'
			case 55: // '7'
			case 56: // '8'
			case 57: // '9'
			case 58: // ':'
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
				if (instruction.value instanceof LocalVariable) {
					final LocalVariable localvariable = (LocalVariable) instruction.value;
					amemberdefinition[localvariable.slot] = localvariable.field;
				}
				break;

			case -1:
				this.flowFields(environment, (Label) instruction, amemberdefinition);
				return;

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
			case 168:
			case 198:
			case 199:
				this.flowFields(environment, (Label) instruction.value, amemberdefinition);
				break;

			case 167:
				this.flowFields(environment, (Label) instruction.value, amemberdefinition);
				return;

			case 169:
			case 172:
			case 173:
			case 174:
			case 175:
			case 176:
			case 177:
			case 191:
				return;

			case 170:
			case 171:
				final SwitchData switchdata = (SwitchData) instruction.value;
				this.flowFields(environment, switchdata.defaultLabel, amemberdefinition);
				for (final Iterator iterator = switchdata.tab.values().iterator(); iterator.hasNext(); this.flowFields(environment, (Label) iterator.next(), amemberdefinition)) {
				}
				return;

			case -3:
				final List catches = ((TryData) instruction.value).catches;
				CatchData catchdata;
				for (final Iterator iterator = catches.iterator(); iterator.hasNext(); this.flowFields(environment, catchdata.getLabel(), amemberdefinition)) {
					catchdata = (CatchData) iterator.next();
				}

				break;
			}
		}

	}

	public void writeLocalVariableTable(final Environment environment, final MemberDefinition memberdefinition, final DataOutput dataoutputstream, final ConstantPool constantpool) throws IOException {
		final MemberDefinition amemberdefinition[] = new MemberDefinition[this.maxvar];
		if (memberdefinition != null && memberdefinition.getArguments() != null) {
			int j1 = 0;
			final List arguments = memberdefinition.getArguments();
			for (final Iterator it = arguments.iterator(); it.hasNext();) {
				final MemberDefinition memberdefinition1 = (MemberDefinition) it.next();
				amemberdefinition[j1] = memberdefinition1;
				j1 += memberdefinition1.getType().stackSize();
			}

		}
		this.flowFields(environment, this.first, amemberdefinition);
		final LocalVariableTable localvariabletable = new LocalVariableTable();
		for (int i = 0; i < this.maxvar; i++) {
			amemberdefinition[i] = null;
		}

		if (memberdefinition != null && memberdefinition.getArguments() != null) {
			int k1 = 0;
			final List arguments = memberdefinition.getArguments();
			for (final Iterator iterator = arguments.iterator(); iterator.hasNext();) {
				final MemberDefinition memberdefinition2 = (MemberDefinition) iterator.next();
				amemberdefinition[k1] = memberdefinition2;
				localvariabletable.define(memberdefinition2, k1, 0, this.maxpc);
				k1 += memberdefinition2.getType().stackSize();
			}

		}
		final int ai[] = new int[this.maxvar];
		for (Object obj = this.first; obj != null; obj = ((Instruction) obj).next) {
			switch (((Instruction) obj).opc) {
			case 0: // '\0'
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
			case 16: // '\020'
			case 17: // '\021'
			case 18: // '\022'
			case 19: // '\023'
			case 20: // '\024'
			case 21: // '\025'
			case 22: // '\026'
			case 23: // '\027'
			case 24: // '\030'
			case 25: // '\031'
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
			default:
				break;

			case 54: // '6'
			case 55: // '7'
			case 56: // '8'
			case 57: // '9'
			case 58: // ':'
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
				if (((Instruction) obj).value instanceof LocalVariable) {
					final LocalVariable localvariable = (LocalVariable) ((Instruction) obj).value;
					final int i2 = ((Instruction) obj).next == null ? ((Instruction) obj).pc : ((Instruction) obj).next.pc;
					if (amemberdefinition[localvariable.slot] != null) {
						localvariabletable.define(amemberdefinition[localvariable.slot], localvariable.slot, ai[localvariable.slot], i2);
					}
					ai[localvariable.slot] = i2;
					amemberdefinition[localvariable.slot] = localvariable.field;
				}
				break;

			case -1:
				for (int j = 0; j < this.maxvar; j++) {
					if (amemberdefinition[j] != null) {
						localvariabletable.define(amemberdefinition[j], j, ai[j], ((Instruction) obj).pc);
					}
				}

				final int l1 = ((Instruction) obj).pc;
				final MemberDefinition amemberdefinition1[] = ((Label) obj).locals;
				if (amemberdefinition1 == null) {
					for (int k = 0; k < this.maxvar; k++) {
						amemberdefinition[k] = null;
					}

				} else {
					System.arraycopy(amemberdefinition1, 0, amemberdefinition, 0, this.maxvar);
				}
				for (int l = 0; l < this.maxvar; l++) {
					ai[l] = l1;
				}

				break;
			}
		}

		for (int i1 = 0; i1 < this.maxvar; i1++) {
			if (amemberdefinition[i1] != null) {
				localvariabletable.define(amemberdefinition[i1], i1, ai[i1], this.maxpc);
			}
		}

		localvariabletable.write(environment, dataoutputstream, constantpool);
	}

	public boolean empty() {
		return this.first == this.last;
	}

	public void listing(final PrintStream printstream) {
		printstream.println("-- listing --");
		for (Object obj = this.first; obj != null; obj = ((Instruction) obj).next) {
			printstream.println(obj);
		}

	}

	static final int NOTREACHED = 0;
	static final int REACHED = 1;
	static final int NEEDED = 2;
	private final Label first;
	private Instruction last;
	private int maxdepth;
	private int maxvar;
	private int maxpc;
	private static final Vector SourceClassList = new Vector();
	private static final Vector TmpCovTable = new Vector();
	private static final int[] JcovClassCountArray = new int[9];
	private static final String JcovMagicLine = "JCOV-DATA-FILE-VERSION: 2.0";
	private static final String JcovClassLine = "CLASS: ";
	private static final String JcovSrcfileLine = "SRCFILE: ";
	private static final String JcovTimestampLine = "TIMESTAMP: ";
	private static final String JcovDataLine = "DATA: ";
	private static final String JcovHeadingLine = "#kind\tcount";
	private static final int[] arrayModifiers = { 1, 2, 4, 1024, 16, 512 };
	private static final int[] arrayModifiersOpc = { 121, 120, 122, 130, 128, 114 };

}
