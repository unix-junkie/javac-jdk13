package sun.tools.asm;

import java.io.DataOutput;
import java.io.IOException;

import sun.tools.java.Environment;
import sun.tools.java.MemberDefinition;

final class LocalVariableTable {

	LocalVariableTable() {
		this.locals = new LocalVariable[8];
	}

	void define(final MemberDefinition memberdefinition, final int i, final int j, final int k) {
		if (j >= k) {
			return;
		}
		for (int l = 0; l < this.len; l++) {
			if (this.locals[l].field == memberdefinition && this.locals[l].slot == i && j <= this.locals[l].to && k >= this.locals[l].from) {
				this.locals[l].from = Math.min(this.locals[l].from, j);
				this.locals[l].to = Math.max(this.locals[l].to, k);
				return;
			}
		}

		if (this.len == this.locals.length) {
			final LocalVariable alocalvariable[] = new LocalVariable[this.len * 2];
			System.arraycopy(this.locals, 0, alocalvariable, 0, this.len);
			this.locals = alocalvariable;
		}
		this.locals[this.len++] = new LocalVariable(memberdefinition, i, j, k);
	}

	private void trim_ranges() {
		for (int i = 0; i < this.len; i++) {
			for (int j = i + 1; j < this.len; j++) {
				if (this.locals[i].field.getName() == this.locals[j].field.getName() && this.locals[i].from <= this.locals[j].to && this.locals[i].to >= this.locals[j].from) {
					if (this.locals[i].slot < this.locals[j].slot) {
						if (this.locals[i].from < this.locals[j].from) {
							this.locals[i].to = Math.min(this.locals[i].to, this.locals[j].from);
						}
					} else if (this.locals[i].slot > this.locals[j].slot && this.locals[i].from > this.locals[j].from) {
						this.locals[j].to = Math.min(this.locals[j].to, this.locals[i].from);
					}
				}
			}

		}

	}

	void write(final Environment environment, final DataOutput dataoutputstream, final ConstantPool constantpool) throws IOException {
		this.trim_ranges();
		dataoutputstream.writeShort(this.len);
		for (int i = 0; i < this.len; i++) {
			dataoutputstream.writeShort(this.locals[i].from);
			dataoutputstream.writeShort(this.locals[i].to - this.locals[i].from);
			dataoutputstream.writeShort(constantpool.index(this.locals[i].field.getName().toString()));
			dataoutputstream.writeShort(constantpool.index(this.locals[i].field.getType().getTypeSignature()));
			dataoutputstream.writeShort(this.locals[i].slot);
		}

	}

	private LocalVariable[] locals;
	private int len;
}
