package sun.tools.java;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import sun.tools.javac.Main;

public final class BinaryClass extends ClassDefinition {

	private BinaryClass(final Object obj, final ClassDeclaration declaration, final int i, final ClassDeclaration superClass, final ClassDeclaration interfaces[], final List dependencies) {
		super(obj, 0L, declaration, i, null, null);
		this.haveLoadedNested = false;
		this.basicCheckDone = false;
		this.basicChecking = false;
		this.dependencies = dependencies;
		this.superClass = superClass;
		this.interfaces = interfaces;
	}

	protected void basicCheck(final Environment environment) throws ClassNotFound {
		Environment.dtEnter("BinaryClass.basicCheck: " + this.getName());
		if (this.basicChecking || this.basicCheckDone) {
			Environment.dtExit("BinaryClass.basicCheck: OK " + this.getName());
			return;
		}
		Environment.dtEvent("BinaryClass.basicCheck: CHECKING " + this.getName());
		this.basicChecking = true;
		super.basicCheck(environment);
		if (ClassDefinition.doInheritanceChecks) {
			this.collectInheritedMethods(environment);
		}
		this.basicCheckDone = true;
		this.basicChecking = false;
		Environment.dtExit("BinaryClass.basicCheck: " + this.getName());
	}

	public static BinaryClass load(final Environment environment, final DataInputStream datainputstream, final int fileFlags) throws IOException {
		final int j = datainputstream.readInt();
		if (j != 0xcafebabe) {
			throw new ClassFormatError("wrong magic: " + j + ", expected " + 0xcafebabe);
		}
		final int k = datainputstream.readUnsignedShort();
		final int l = datainputstream.readUnsignedShort();
		if (l < 45) {
			throw new ClassFormatError(Main.getText("javac.err.version.too.old", String.valueOf(l)));
		}
		if (l > 47 || l == 47 && k > 0) {
			throw new ClassFormatError(Main.getText("javac.err.version.too.recent", l + "." + k));
		}
		final BinaryConstantPool binaryconstantpool = new BinaryConstantPool(datainputstream);
		final List dependencies = binaryconstantpool.getDependencies(environment);
		final int i1 = datainputstream.readUnsignedShort() & 0xe31;
		final ClassDeclaration classdeclaration = binaryconstantpool.getDeclaration(environment, datainputstream.readUnsignedShort());
		final ClassDeclaration classdeclaration1 = binaryconstantpool.getDeclaration(environment, datainputstream.readUnsignedShort());
		final ClassDeclaration aclassdeclaration[] = new ClassDeclaration[datainputstream.readUnsignedShort()];
		for (int j1 = 0; j1 < aclassdeclaration.length; j1++) {
			aclassdeclaration[j1] = binaryconstantpool.getDeclaration(environment, datainputstream.readUnsignedShort());
		}

		final BinaryClass binaryclass = new BinaryClass(null, classdeclaration, i1, classdeclaration1, aclassdeclaration, dependencies);
		binaryclass.cpool = binaryconstantpool;
		binaryclass.addDependency(classdeclaration1);
		final int k1 = datainputstream.readUnsignedShort();
		for (int l1 = 0; l1 < k1; l1++) {
			final int i2 = datainputstream.readUnsignedShort() & 0xdf;
			final Identifier identifier = binaryconstantpool.getIdentifier(datainputstream.readUnsignedShort());
			final Type type = binaryconstantpool.getType(datainputstream.readUnsignedShort());
			final BinaryAttribute binaryattribute = BinaryAttribute.load(datainputstream, binaryconstantpool, fileFlags);
			binaryclass.addMember(new BinaryMember(binaryclass, i2, type, identifier, binaryattribute));
		}

		final int j2 = datainputstream.readUnsignedShort();
		for (int k2 = 0; k2 < j2; k2++) {
			final int l2 = datainputstream.readUnsignedShort() & 0xd3f;
			final Identifier identifier1 = binaryconstantpool.getIdentifier(datainputstream.readUnsignedShort());
			final Type type1 = binaryconstantpool.getType(datainputstream.readUnsignedShort());
			final BinaryAttribute binaryattribute1 = BinaryAttribute.load(datainputstream, binaryconstantpool, fileFlags);
			binaryclass.addMember(new BinaryMember(binaryclass, l2, type1, identifier1, binaryattribute1));
		}

		binaryclass.atts = BinaryAttribute.load(datainputstream, binaryconstantpool, fileFlags);
		byte abyte0[] = binaryclass.getAttribute(Constants.idSourceFile);
		if (abyte0 != null) {
			final DataInput datainputstream1 = new DataInputStream(new ByteArrayInputStream(abyte0));
			binaryclass.source = binaryconstantpool.getString(datainputstream1.readUnsignedShort());
		}
		abyte0 = binaryclass.getAttribute(Constants.idDocumentation);
		if (abyte0 != null) {
			binaryclass.documentation = new DataInputStream(new ByteArrayInputStream(abyte0)).readUTF();
		}
		if (binaryclass.getAttribute(Constants.idDeprecated) != null) {
			binaryclass.modifiers |= Constants.M_DEPRECATED;
		}
		if (binaryclass.getAttribute(Constants.idSynthetic) != null) {
			binaryclass.modifiers |= 0x80000;
		}
		return binaryclass;
	}

	public void loadNested(final Environment environment) {
		this.loadNested(environment, 0);
	}

	private void loadNested(final Environment environment, final int i) {
		if (this.haveLoadedNested) {
			Environment.dtEvent("loadNested: DUPLICATE CALL SKIPPED");
			return;
		}
		this.haveLoadedNested = true;
		try {
			final byte abyte0[] = this.getAttribute(Constants.idInnerClasses);
			if (abyte0 != null) {
				this.initInnerClasses(environment, abyte0, i);
			}
		} catch (final IOException ignored) {
			environment.error(0L, "malformed.attribute", this.getClassDeclaration(), Constants.idInnerClasses);
			Environment.dtEvent("loadNested: MALFORMED ATTRIBUTE (InnerClasses)");
		}
	}

	private void initInnerClasses(final Environment environment, final byte abyte0[], final int i) throws IOException {
		final DataInput datainputstream = new DataInputStream(new ByteArrayInputStream(abyte0));
		final int j = datainputstream.readUnsignedShort();
		for (int k = 0; k < j; k++) {
			final int l = datainputstream.readUnsignedShort();
			final ClassDeclaration classdeclaration = this.cpool.getDeclaration(environment, l);
			ClassDeclaration classdeclaration1 = null;
			final int i1 = datainputstream.readUnsignedShort();
			if (i1 != 0) {
				classdeclaration1 = this.cpool.getDeclaration(environment, i1);
			}
			Identifier identifier = Constants.idNull;
			final int j1 = datainputstream.readUnsignedShort();
			if (j1 != 0) {
				identifier = Identifier.lookup(this.cpool.getString(j1));
			}
			final int k1 = datainputstream.readUnsignedShort();
			final boolean flag = classdeclaration1 != null && !identifier.equals(Constants.idNull) && ((k1 & 2) == 0 || (i & 4) != 0);
			if (flag) {
				final Identifier identifier1 = Identifier.lookupInner(classdeclaration1.getName(), identifier);
				Type.tClass(identifier1);
				if (classdeclaration.equals(this.getClassDeclaration())) {
					try {
						final ClassDefinition classdefinition = classdeclaration1.getClassDefinition(environment);
						this.initInner(classdefinition, k1);
					} catch (final ClassNotFound ignored) {
					}
				} else if (classdeclaration1.equals(this.getClassDeclaration())) {
					try {
						final ClassDefinition classdefinition1 = classdeclaration.getClassDefinition(environment);
						this.initOuter(classdefinition1, k1);
					} catch (final ClassNotFound ignored) {
					}
				}
			}
		}

	}

	private void initInner(final ClassDefinition classdefinition, int i) {
		if (this.getOuterClass() != null) {
			return;
		}
		if ((i & 2) != 0) {
			i &= -6;
		} else if ((i & 4) != 0) {
			i &= -2;
		}
		if ((i & 0x200) != 0) {
			i |= 0x408;
		}
		if (classdefinition.isInterface()) {
			i |= 9;
			i &= -7;
		}
		this.modifiers = i;
		this.setOuterClass(classdefinition);
		for (MemberDefinition memberdefinition = this.getFirstMember(); memberdefinition != null; memberdefinition = memberdefinition.getNextMember()) {
			if (memberdefinition.isUplevelValue() && classdefinition.getType().equals(memberdefinition.getType()) && memberdefinition.getName().toString().startsWith("this$")) {
				this.setOuterMember(memberdefinition);
			}
		}

	}

	private void initOuter(final ClassDefinition classdefinition, final int i) {
		if (classdefinition instanceof BinaryClass) {
			((BinaryClass) classdefinition).initInner(this, i);
		}
		this.addMember(new BinaryMember(classdefinition));
	}

	public void write(final Environment environment, final OutputStream outputstream) throws IOException {
		final DataOutputStream dataoutputstream = new DataOutputStream(outputstream);
		dataoutputstream.writeInt(0xcafebabe);
		dataoutputstream.writeShort(environment.getMinorVersion());
		dataoutputstream.writeShort(environment.getMajorVersion());
		this.cpool.write(dataoutputstream, environment);
		dataoutputstream.writeShort(this.getModifiers() & 0xe31);
		dataoutputstream.writeShort(this.cpool.indexObject(this.getClassDeclaration(), environment));
		dataoutputstream.writeShort(this.getSuperClass() == null ? 0 : this.cpool.indexObject(this.getSuperClass(), environment));
		dataoutputstream.writeShort(this.interfaces.length);
		for (int i = 0; i < this.interfaces.length; i++) {
			dataoutputstream.writeShort(this.cpool.indexObject(this.interfaces[i], environment));
		}

		int j = 0;
		int k = 0;
		for (MemberDefinition memberdefinition = this.firstMember; memberdefinition != null; memberdefinition = memberdefinition.getNextMember()) {
			if (memberdefinition.isMethod()) {
				k++;
			} else {
				j++;
			}
		}

		dataoutputstream.writeShort(j);
		for (MemberDefinition memberdefinition1 = this.firstMember; memberdefinition1 != null; memberdefinition1 = memberdefinition1.getNextMember()) {
			if (!memberdefinition1.isMethod()) {
				dataoutputstream.writeShort(memberdefinition1.getModifiers() & 0xdf);
				final String s = memberdefinition1.getName().toString();
				final String s1 = memberdefinition1.getType().getTypeSignature();
				dataoutputstream.writeShort(this.cpool.indexString(s, environment));
				dataoutputstream.writeShort(this.cpool.indexString(s1, environment));
				BinaryAttribute.write(((BinaryMember) memberdefinition1).atts, dataoutputstream, this.cpool, environment);
			}
		}

		dataoutputstream.writeShort(k);
		for (MemberDefinition memberdefinition2 = this.firstMember; memberdefinition2 != null; memberdefinition2 = memberdefinition2.getNextMember()) {
			if (memberdefinition2.isMethod()) {
				dataoutputstream.writeShort(memberdefinition2.getModifiers() & 0xd3f);
				final String s2 = memberdefinition2.getName().toString();
				final String s3 = memberdefinition2.getType().getTypeSignature();
				dataoutputstream.writeShort(this.cpool.indexString(s2, environment));
				dataoutputstream.writeShort(this.cpool.indexString(s3, environment));
				BinaryAttribute.write(((BinaryMember) memberdefinition2).atts, dataoutputstream, this.cpool, environment);
			}
		}

		BinaryAttribute.write(this.atts, dataoutputstream, this.cpool, environment);
		dataoutputstream.flush();
	}

	public Iterator getDependencies() {
		return this.dependencies.iterator();
	}

	public void addDependency(final ClassDeclaration classdeclaration) {
		if (classdeclaration != null && !this.dependencies.contains(classdeclaration)) {
			this.dependencies.add(classdeclaration);
		}
	}

	public BinaryConstantPool getConstants() {
		return this.cpool;
	}

	private byte[] getAttribute(final Identifier identifier) {
		for (BinaryAttribute binaryattribute = this.atts; binaryattribute != null; binaryattribute = binaryattribute.next) {
			if (binaryattribute.name.equals(identifier)) {
				return binaryattribute.data;
			}
		}

		return null;
	}

	BinaryConstantPool cpool;
	private BinaryAttribute atts;
	private final List dependencies;
	private boolean haveLoadedNested;
	private boolean basicCheckDone;
	private boolean basicChecking;
}
