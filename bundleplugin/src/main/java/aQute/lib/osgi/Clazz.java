/* Copyright 2006 aQute SARL 
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0 */
package aQute.lib.osgi;

import java.io.*;
import java.nio.*;
import java.util.*;

public class Clazz {
    public static enum QUERY {
        IMPLEMENTS, EXTENDS, IMPORTS, NAMED, ANY, VERSION
    };

    static protected class Assoc {
        Assoc(byte tag, int a, int b) {
            this.tag = tag;
            this.a = a;
            this.b = b;
        }

        byte tag;
        int  a;
        int  b;
    }

    final static byte                SkipTable[] = { 0, // 0 non existent
            -1, // 1 CONSTANT_utf8 UTF 8, handled in
            // method
            -1, // 2
            4, // 3 CONSTANT_Integer
            4, // 4 CONSTANT_Float
            8, // 5 CONSTANT_Long (index +=2!)
            8, // 6 CONSTANT_Double (index +=2!)
            -1, // 7 CONSTANT_Class
            2, // 8 CONSTANT_String
            4, // 9 CONSTANT_FieldRef
            4, // 10 CONSTANT_MethodRef
            4, // 11 CONSTANT_InterfaceMethodRef
            4, // 12 CONSTANT_NameAndType
                                                 };

    String                           className;
    Object                           pool[];
    int                              intPool[];
    Map<String, Map<String, String>> imports     = new HashMap<String, Map<String, String>>();
    String                           path;

    // static String type = "([BCDFIJSZ\\[]|L[^<>]+;)";
    // static Pattern descriptor = Pattern.compile("\\(" + type + "*\\)(("
    // + type + ")|V)");
    int                              minor       = 0;
    int                              major       = 0;

    String                           sourceFile;
    Set<String>                      xref;
    Set<Integer>                     classes;
    Set<Integer>                     descriptors;
    int                              forName     = 0;
    int                              class$      = 0;
    String[]                         interfaces;
    String                           zuper;

    public Clazz(String path) {
        this.path = path;
    }

    public Clazz(String path, InputStream in) throws IOException {
        this.path = path;
        DataInputStream din = new DataInputStream(in);
        parseClassFile(din);
        din.close();
    }

    Set<String> parseClassFile(DataInputStream in) throws IOException {
        xref = new HashSet<String>();
        classes = new HashSet<Integer>();
        descriptors = new HashSet<Integer>();

        boolean crawl = false; // Crawl the byte code
        int magic = in.readInt();
        if (magic != 0xCAFEBABE)
            throw new IOException("Not a valid class file (no CAFEBABE header)");

        minor = in.readUnsignedShort(); // minor version
        major = in.readUnsignedShort(); // major version
        int count = in.readUnsignedShort();
        pool = new Object[count];
        intPool = new int[count];

        process: for (int poolIndex = 1; poolIndex < count; poolIndex++) {
            byte tag = in.readByte();
            switch (tag) {
            case 0:
                break process;
            case 1:
                constantUtf8(in, poolIndex);
                break;

            // For some insane optimization reason are
            // the long and the double two entries in the
            // constant pool. See 4.4.5
            case 5:
                constantLong(in, poolIndex);
                poolIndex++;
                break;

            case 6:
                constantDouble(in, poolIndex);
                poolIndex++;
                break;

            case 7:
                constantClass(in, poolIndex);
                break;

            case 8:
                constantString(in, poolIndex);
                break;

            case 10: // Method ref
                methodRef(in, poolIndex);
                break;

            // Name and Type
            case 12:
                nameAndType(in, poolIndex, tag);
                break;

            // We get the skip count for each record type
            // from the SkipTable. This will also automatically
            // abort when
            default:
                if (tag == 2)
                    throw new IOException("Invalid tag " + tag);
                in.skipBytes(SkipTable[tag]);
                break;
            }
        }

        pool(pool, intPool);
        /*
         * Parse after the constant pool, code thanks to Hans Christian
         * Falkenberg
         */

        /* int access_flags = */in.readUnsignedShort(); // access
        int this_class = in.readUnsignedShort();
        int super_class = in.readUnsignedShort();
        zuper = (String) pool[intPool[super_class]];
        if (zuper != null) {
            addReference(zuper);
        }
        className = (String) pool[intPool[this_class]];

        int interfacesCount = in.readUnsignedShort();
        if (interfacesCount > 0) {
            interfaces = new String[interfacesCount];
            for (int i = 0; i < interfacesCount; i++)
                interfaces[i] = (String) pool[intPool[in.readUnsignedShort()]];
        }
        
        int fieldsCount = in.readUnsignedShort();
        for (int i = 0; i < fieldsCount; i++) {
            /* access_flags = */in.readUnsignedShort(); // skip access flags
            int name_index = in.readUnsignedShort();
            int descriptor_index = in.readUnsignedShort();

            // Java prior to 1.5 used a weird
            // static variable to hold the com.X.class
            // result construct. If it did not find it
            // it would create a variable class$com$X
            // that would be used to hold the class
            // object gotten with Class.forName ...
            // Stupidly, they did not actively use the
            // class name for the field type, so bnd
            // would not see a reference. We detect
            // this case and add an artificial descriptor
            String name = pool[name_index].toString(); // name_index
            if (name.startsWith("class$")) {
                crawl = true;
            }

            descriptors.add(new Integer(descriptor_index));
            doAttributes(in, false);
        }

        //
        // Check if we have to crawl the code to find
        // the ldc(_w) <string constant> invokestatic Class.forName
        // if so, calculate the method ref index so we
        // can do this efficiently
        //
        if (crawl) {
            forName = findMethod("java/lang/Class", "forName",
                    "(Ljava/lang/String;)Ljava/lang/Class;");
            class$ = findMethod(className, "class$",
                    "(Ljava/lang/String;)Ljava/lang/Class;");
        }

        //
        // Handle the methods
        //
        int methodCount = in.readUnsignedShort();
        for (int i = 0; i < methodCount; i++) {
            /* access_flags = */in.readUnsignedShort();
            /* int name_index = */in.readUnsignedShort();
            int descriptor_index = in.readUnsignedShort();
            // String s = (String) pool[name_index];
            descriptors.add(new Integer(descriptor_index));
            doAttributes(in, crawl);
        }

        doAttributes(in, false);

        //
        // Now iterate over all classes we found and
        // parse those as well. We skip duplicates
        //

        for (Iterator<Integer> e = classes.iterator(); e.hasNext();) {
            int class_index = e.next().shortValue();
            doClassReference((String) pool[class_index]);
        }

        //
        // Parse all the descriptors we found
        //

        for (Iterator<Integer> e = descriptors.iterator(); e.hasNext();) {
            Integer index = e.next();
            String prototype = (String) pool[index.intValue()];
            if (prototype != null)
                parseDescriptor(prototype);
            else
                System.err.println("Unrecognized descriptor: " + index);
        }
        Set<String> xref = this.xref;
        reset();
        return xref;
    }

    protected void pool(Object[] pool, int[] intPool) {
    }

    /**
     * @param in
     * @param poolIndex
     * @param tag
     * @throws IOException
     */
    protected void nameAndType(DataInputStream in, int poolIndex, byte tag)
            throws IOException {
        int name_index = in.readUnsignedShort();
        int descriptor_index = in.readUnsignedShort();
        descriptors.add(new Integer(descriptor_index));
        pool[poolIndex] = new Assoc(tag, name_index, descriptor_index);
    }

    /**
     * @param in
     * @param poolIndex
     * @param tag
     * @throws IOException
     */
    private void methodRef(DataInputStream in, int poolIndex)
            throws IOException {
        int class_index = in.readUnsignedShort();
        int name_and_type_index = in.readUnsignedShort();
        pool[poolIndex] = new Assoc((byte) 10, class_index, name_and_type_index);
    }

    /**
     * @param in
     * @param poolIndex
     * @throws IOException
     */
    private void constantString(DataInputStream in, int poolIndex)
            throws IOException {
        int string_index = in.readUnsignedShort();
        intPool[poolIndex] = string_index;
    }

    /**
     * @param in
     * @param poolIndex
     * @throws IOException
     */
    protected void constantClass(DataInputStream in, int poolIndex)
            throws IOException {
        int class_index = in.readUnsignedShort();
        classes.add(new Integer(class_index));
        intPool[poolIndex] = class_index;
    }

    /**
     * @param in
     * @throws IOException
     */
    protected void constantDouble(DataInputStream in, int poolIndex)
            throws IOException {
        in.skipBytes(8);
    }

    /**
     * @param in
     * @throws IOException
     */
    protected void constantLong(DataInputStream in, int poolIndex)
            throws IOException {
        in.skipBytes(8);
    }

    /**
     * @param in
     * @param poolIndex
     * @throws IOException
     */
    protected void constantUtf8(DataInputStream in, int poolIndex)
            throws IOException {
        // CONSTANT_Utf8

        String name = in.readUTF();
        xref.add(name);
        pool[poolIndex] = name;
    }

    /**
     * Find a method reference in the pool that points to the given class,
     * methodname and descriptor.
     * 
     * @param clazz
     * @param methodname
     * @param descriptor
     * @return index in constant pool
     */
    private int findMethod(String clazz, String methodname, String descriptor) {
        for (int i = 1; i < pool.length; i++) {
            if (pool[i] instanceof Assoc) {
                Assoc methodref = (Assoc) pool[i];
                if (methodref.tag == 10) {
                    // Method ref
                    int class_index = methodref.a;
                    int class_name_index = intPool[class_index];
                    if (clazz.equals(pool[class_name_index])) {
                        int name_and_type_index = methodref.b;
                        Assoc name_and_type = (Assoc) pool[name_and_type_index];
                        if (name_and_type.tag == 12) {
                            // Name and Type
                            int name_index = name_and_type.a;
                            int type_index = name_and_type.b;
                            if (methodname.equals(pool[name_index])) {
                                if (descriptor.equals(pool[type_index])) {
                                    return i;
                                }
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }

    private void doClassReference(String next) {
        if (next != null) {
            String normalized = normalize(next);
            if (normalized != null) {
                String pack = getPackage(normalized);
                packageReference(pack);
            }
        } else
            throw new IllegalArgumentException("Invalid class, parent=");
    }

    /**
     * Called for each attribute in the class, field, or method.
     * 
     * @param in
     *            The stream
     * @throws IOException
     */
    private void doAttributes(DataInputStream in, boolean crawl)
            throws IOException {
        int attributesCount = in.readUnsignedShort();
        for (int j = 0; j < attributesCount; j++) {
            // skip name CONSTANT_Utf8 pointer
            doAttribute(in, crawl);
        }
    }

    /**
     * Process a single attribute, if not recognized, skip it.
     * 
     * @param in
     *            the data stream
     * @throws IOException
     */
    private void doAttribute(DataInputStream in, boolean crawl)
            throws IOException {
        int attribute_name_index = in.readUnsignedShort();
        String attributeName = (String) pool[attribute_name_index];
        if (attribute_name_index == 560)
            System.out.println("Index " + attribute_name_index + ":"
                    + attributeName);
        long attribute_length = in.readInt();
        attribute_length &= 0xFFFFFFFF;
        if ("RuntimeVisibleAnnotations".equals(attributeName))
            doAnnotations(in);
        else if ("RuntimeVisibleParameterAnnotations".equals(attributeName))
            doParameterAnnotations(in);
        else if ("SourceFile".equals(attributeName))
            doSourceFile(in);
        else if ("Code".equals(attributeName) && crawl)
            doCode(in);
        else {
            if (attribute_length > 0x7FFFFFFF) {
                throw new IllegalArgumentException("Attribute > 2Gb");
            }
            in.skipBytes((int) attribute_length);
        }
    }

    /**
     * <pre>
     * Code_attribute {
     * 		u2 attribute_name_index;
     * 		u4 attribute_length;
     * 		u2 max_stack;
     * 		u2 max_locals;
     * 		u4 code_length;
     * 		u1 code[code_length];
     * 		u2 exception_table_length;
     * 		{    	u2 start_pc;
     * 		      	u2 end_pc;
     * 		      	u2  handler_pc;
     * 		      	u2  catch_type;
     * 		}	exception_table[exception_table_length];
     * 		u2 attributes_count;
     * 		attribute_info attributes[attributes_count];
     * 	}
     * </pre>
     * 
     * @param in
     * @param pool
     * @throws IOException
     */
    private void doCode(DataInputStream in) throws IOException {
        /* int max_stack = */in.readUnsignedShort();
        /* int max_locals = */in.readUnsignedShort();
        int code_length = in.readInt();
        byte code[] = new byte[code_length];
        in.readFully(code);
        crawl(code);
        int exception_table_length = in.readUnsignedShort();
        in.skipBytes(exception_table_length * 8);
        doAttributes(in, false);
    }

    /**
     * We must find Class.forName references ...
     * 
     * @param code
     */
    protected void crawl(byte[] code) {
        ByteBuffer bb = ByteBuffer.wrap(code);
        bb.order(ByteOrder.BIG_ENDIAN);
        int lastReference = -1;

        while (bb.remaining() > 0) {
            int instruction = 0xFF & bb.get();
            switch (instruction) {
            case OpCodes.ldc:
                lastReference = 0xFF & bb.get();
                break;

            case OpCodes.ldc_w:
                lastReference = 0xFFFF & bb.getShort();
                break;

            case OpCodes.invokestatic:
                int methodref = 0xFFFF & bb.getShort();
                if ((methodref == forName || methodref == class$)
                        && lastReference != -1
                        && pool[intPool[lastReference]] instanceof String) {
                    String clazz = (String) pool[intPool[lastReference]];
                    doClassReference(clazz.replace('.', '/'));
                }
                break;

            case OpCodes.tableswitch:
                // Skip to place divisible by 4
                while ((bb.position() & 0x3) != 0)
                    bb.get();
                /* int deflt = */
                bb.getInt();
                int low = bb.getInt();
                int high = bb.getInt();
                bb.position(bb.position() + (high - low + 1) * 4);
                lastReference = -1;
                break;

            case OpCodes.lookupswitch:
                // Skip to place divisible by 4
                while ((bb.position() & 0x3) != 0)
                    bb.get();
                /* deflt = */
                bb.getInt();
                int npairs = bb.getInt();
                bb.position(bb.position() + npairs * 8);
                lastReference = -1;
                break;

            default:
                lastReference = -1;
                bb.position(bb.position() + OpCodes.OFFSETS[instruction]);
            }
        }
    }

    private void doSourceFile(DataInputStream in) throws IOException {
        int sourcefile_index = in.readUnsignedShort();
        this.sourceFile = pool[sourcefile_index].toString();
    }

    private void doParameterAnnotations(DataInputStream in) throws IOException {
        int num_parameters = in.readUnsignedByte();
        for (int p = 0; p < num_parameters; p++) {
            int num_annotations = in.readUnsignedShort(); // # of annotations
            for (int a = 0; a < num_annotations; a++) {
                doAnnotation(in);
            }
        }
    }

    private void doAnnotations(DataInputStream in) throws IOException {
        int num_annotations = in.readUnsignedShort(); // # of annotations
        for (int a = 0; a < num_annotations; a++) {
            doAnnotation(in);
        }
    }

    private void doAnnotation(DataInputStream in) throws IOException {
        int type_index = in.readUnsignedShort();
        descriptors.add(new Integer(type_index));
        int num_element_value_pairs = in.readUnsignedShort();
        for (int v = 0; v < num_element_value_pairs; v++) {
            /* int element_name_index = */in.readUnsignedShort();
            doElementValue(in);
        }
    }

    private void doElementValue(DataInputStream in) throws IOException {
        int tag = in.readUnsignedByte();
        switch (tag) {
        case 'B':
        case 'C':
        case 'D':
        case 'F':
        case 'I':
        case 'J':
        case 'S':
        case 'Z':
        case 's':
            /* int const_value_index = */
            in.readUnsignedShort();
            break;

        case 'e':
            int type_name_index = in.readUnsignedShort();
            descriptors.add(new Integer(type_name_index));
            /* int const_name_index = */
            in.readUnsignedShort();
            break;

        case 'c':
            int class_info_index = in.readUnsignedShort();
            descriptors.add(new Integer(class_info_index));
            break;

        case '@':
            doAnnotation(in);
            break;

        case '[':
            int num_values = in.readUnsignedShort();
            for (int i = 0; i < num_values; i++) {
                doElementValue(in);
            }
            break;

        default:
            throw new IllegalArgumentException(
                    "Invalid value for Annotation ElementValue tag " + tag);
        }
    }

    void packageReference(String pack) {
        if (pack.indexOf('<') >= 0)
            System.out.println("Oops: " + pack);
        if (!imports.containsKey(pack))
            imports.put(pack, new LinkedHashMap<String, String>());
    }

    void parseDescriptor(String prototype) {
        addReference(prototype);
        StringTokenizer st = new StringTokenizer(prototype, "(;)", true);
        while (st.hasMoreTokens()) {
            if (st.nextToken().equals("(")) {
                String token = st.nextToken();
                while (!token.equals(")")) {
                    addReference(token);
                    token = st.nextToken();
                }
                token = st.nextToken();
                addReference(token);
            }
        }
    }

    private void addReference(String token) {
        while (token.startsWith("["))
            token = token.substring(1);

        if (token.startsWith("L")) {
            String clazz = normalize(token.substring(1));
            if (clazz.startsWith("java/"))
                return;
            String pack = getPackage(clazz);
            packageReference(pack);
        }
    }

    static String normalize(String s) {
        if (s.startsWith("[L"))
            return normalize(s.substring(2));
        if (s.startsWith("["))
            if (s.length() == 2)
                return null;
            else
                return normalize(s.substring(1));
        if (s.endsWith(";"))
            return normalize(s.substring(0, s.length() - 1));
        return s + ".class";
    }

    public static String getPackage(String clazz) {
        int n = clazz.lastIndexOf('/');
        if (n < 0)
            return ".";
        return clazz.substring(0, n).replace('/', '.');
    }

    public Map<String, Map<String, String>> getReferred() {
        return imports;
    }

    String getClassName() {
        return className;
    }

    public String getPath() {
        return path;
    }

    public Set<String> xref(InputStream in) throws IOException {
        DataInputStream din = new DataInputStream(in);
        Set<String> set = parseClassFile(din);
        din.close();
        return set;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * .class construct for different compilers
     * 
     * sun 1.1 Detect static variable class$com$acme$MyClass 1.2 " 1.3 " 1.4 "
     * 1.5 ldc_w (class) 1.6 "
     * 
     * eclipse 1.1 class$0, ldc (string), invokestatic Class.forName 1.2 " 1.3 "
     * 1.5 ldc (class) 1.6 "
     * 
     * 1.5 and later is not an issue, sun pre 1.5 is easy to detect the static
     * variable that decodes the class name. For eclipse, the class$0 gives away
     * we have a reference encoded in a string.
     * compilerversions/compilerversions.jar contains test versions of all
     * versions/compilers.
     */

    public void reset() {
        pool = null;
        intPool = null;
        xref = null;
        classes = null;
        descriptors = null;
    }

    public boolean is(QUERY query, Instruction instr, Map<String, Clazz> classspace) {
        switch (query) {
        case ANY:
            return true;

        case NAMED:
            if ( instr.matches(getClassName()))
                return !instr.isNegated();
            return false;
            
        case VERSION:
            String v = major + "/" + minor;
            if ( instr.matches(v))
                return !instr.isNegated();
            return false;
            
            
        case IMPLEMENTS:
            for ( int i=0; interfaces != null && i<interfaces.length; i++ ) {
                if ( instr.matches(interfaces[i]))
                    return !instr.isNegated();
            }
            break;
        case EXTENDS:
            if ( zuper == null )
                return false;
            
            if ( instr.matches(zuper))
                return !instr.isNegated();
            break;
            
        case IMPORTS:
            for ( String imp : imports.keySet() ) {
                if ( instr.matches(imp.replace('.', '/')))
                    return !instr.isNegated();                    
            }
        }
        
        if ( zuper == null || classspace == null)
            return false;

        Clazz clazz = classspace.get(zuper);
        if (clazz == null)
            return false;

        return clazz.is(query, instr, classspace);
    }

    public String toString() {
        return getFQN();
    }

    public String getFQN() {
        return getClassName().replace('/', '.');
    }
}
