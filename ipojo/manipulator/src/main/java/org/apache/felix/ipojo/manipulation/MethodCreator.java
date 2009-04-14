/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.manipulation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.manipulation.ClassChecker.AnnotationDescriptor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * iPOJO Class Adapter.
 * This class adapt the visited class to link the class with the container.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodCreator extends ClassAdapter implements Opcodes {

    /**
     * Instance Manager Field.
     */
    public static final  String IM_FIELD = "__IM";
    
    /**
     * All POJO method will be renamed by using this prefix.
     */
    private static final String PREFIX = "__";

    /**
     * POJO class.
     */
    private static final  String POJO = "org/apache/felix/ipojo/Pojo";

    /**
     * Filed flag prefix.
     */
    private static final  String FIELD_FLAG_PREFIX = "__F";

    /**
     * Method flag prefix.
     */
    private static final  String METHOD_FLAG_PREFIX = "__M";

    /**
     * onEntry method name.
     */
    private static final  String ENTRY = "onEntry";

    /**
     * onExit method name. 
     */
    private static final  String EXIT = "onExit";

    /**
     * on Error method name.
     */
    private static final  String ERROR = "onError";

    /**
     * onGet method name. 
     */
    private static final  String GET = "onGet";

    /**
     * onSet method name.
     */
    private static final  String SET = "onSet";

    /**
     * Name of the current manipulated class. 
     */
    private String m_owner;

    /**
     * Set of fields detected in the class.
     * (this set is given by the previous analysis)
     */
    private Set m_fields;

    /**
     * List of methods contained in the class.
     * This set contains method id.
     */
    private List m_methods = new ArrayList(); // Contains method id.
    
    /**
     * List of fields injected as method flag in the class.
     * This set contains field name generate from method id.
     */
    private List m_methodFlags = new ArrayList(); 
    
    /**
     * The list of methods visited during the previous analysis.
     * This list allows getting annotations to move to generated
     * method.
     */
    private List m_visitedMethods = new ArrayList();
    
    /**
     * Set to <code>true</code> when a suitable constructor
     * is found. If not set to <code>true</code> at the end
     * of the visit, the manipulator injects a constructor.
     */
    private boolean m_foundSuitableConstructor = false;

    /**
     * Name of the super class.
     */
    private String m_superclass;

    /**
     * Constructor.
     * @param arg0 : class visitor.
     * @param fields : fields map detected during the previous class analysis.
     * @param methods : the list of the detected method during the previous class analysis.
     */
    public MethodCreator(ClassVisitor arg0, Map fields, List methods) {
        super(arg0);
        m_fields = fields.keySet();
        m_visitedMethods = methods;
    }

    /**
     * Visit method.
     * This method store the current class name.
     * Moreover the POJO interface is added to the list of implemented interface.
     * Then the Instance manager field is added.
     * @param version : version
     * @param access : access flag
     * @param name : class name
     * @param signature : signature
     * @param superName : parent class
     * @param interfaces : implemented interface
     * @see org.objectweb.asm.ClassAdapter#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        m_owner = name;
        m_superclass = superName;
        addPOJOInterface(version, access, name, signature, superName, interfaces);
        addIMField();
    }

    /**
     * A method is visited.
     * This method does not manipulate clinit and class$ methods.
     * In the case of a constructor, this method will generate a constructor with the instance manager 
     * and will adapt the current constructor to call this constructor.
     * For standard method, this method will create method header, rename the current method and adapt it.
     * @param access : access flag.
     * @param name : name of the method
     * @param desc : method descriptor
     * @param signature : signature
     * @param exceptions : declared exceptions.
     * @return the MethodVisitor wich will visit the method code.
     * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // Avoid manipulating special methods
        if (name.equals("<clinit>") || name.equals("class$")) { return super.visitMethod(access, name, desc, signature, exceptions); }
        // The constructor is manipulated separately
        if (name.equals("<init>")) {
            MethodDescriptor md = getMethodDescriptor("$init", desc);
            // 1) change the constructor descriptor (add a component manager arg as first argument)
            String newDesc = desc.substring(1);
            newDesc = "(Lorg/apache/felix/ipojo/InstanceManager;" + newDesc;

            Type[] args = Type.getArgumentTypes(desc);
            if (args.length == 0) {
                generateEmptyConstructor(access, signature, exceptions, md.getAnnotations());
                m_foundSuitableConstructor = true;
            } else if (args.length == 1 && args[0].getClassName().equals("org.osgi.framework.BundleContext")) {
                generateBCConstructor(access, signature, exceptions, md.getAnnotations());
                m_foundSuitableConstructor = true;
            } else {
                // Do nothing, the constructor does not match.
                return cv.visitMethod(access, name, desc, signature, exceptions);
            }

            // Insert the new constructor
            MethodVisitor mv = super.visitMethod(ACC_PRIVATE, "<init>", newDesc, signature, exceptions);
            return new ConstructorCodeAdapter(mv, m_owner, m_fields, ACC_PRIVATE, name, newDesc);
        }
        
        if ((access & ACC_SYNTHETIC) == ACC_SYNTHETIC && name.startsWith("access$")) { 
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return new MethodCodeAdapter(mv, m_owner, access, name, desc, m_fields); 
        }

        if ((access & ACC_STATIC) == ACC_STATIC) { return super.visitMethod(access, name, desc, signature, exceptions); }

        MethodDescriptor md = getMethodDescriptor(name, desc);
        if (md == null) {
            generateMethodHeader(access, name, desc, signature, exceptions, new ArrayList(0));
        } else {
            generateMethodHeader(access, name, desc, signature, exceptions, md.getAnnotations());
        }
        
        String id = generateMethodFlag(name, desc);
        if (! m_methodFlags.contains(id)) {
            FieldVisitor flagField = cv.visitField(Opcodes.ACC_PRIVATE, id, "Z", null, null);
            flagField.visitEnd();
            m_methodFlags.add(id);
        }

        MethodVisitor mv = super.visitMethod(ACC_PRIVATE, PREFIX + name, desc, signature, exceptions);
        return new MethodCodeAdapter(mv, m_owner, ACC_PRIVATE, PREFIX + name, desc, m_fields);
    }
    
    /**
     * Gets the method descriptor for the specified name and descriptor.
     * The method descriptor is looked inside the 
     * {@link MethodCreator#m_visitedMethods}
     * @param name the name of the method
     * @param desc the descriptor of the method
     * @return the method descriptor or <code>null</code> if not found.
     */
    private MethodDescriptor getMethodDescriptor(String name, String desc) {
        for (int i = 0; i < m_visitedMethods.size(); i++) {
            MethodDescriptor md = (MethodDescriptor) m_visitedMethods.get(i);
            if (md.getName().equals(name) && md.getDescriptor().equals(desc)) {
                return md;
            }
        }
        return null;
    }
    
    /**
     * Visit a Field.
     * This field access is replaced by an invocation to the getter method or to the setter method.
     * (except for static field).
     * Inject the getter and the setter method for this field.
     * @see org.objectweb.asm.ClassVisitor#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
     * @param access : access modifier
     * @param name : name of the field
     * @param desc : description of the field
     * @param signature : signature of the field
     * @param value : value of the field
     * @return FieldVisitor : null
     */
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
        if ((access & ACC_STATIC) == 0) {
            FieldVisitor flag = cv.visitField(Opcodes.ACC_PRIVATE, FIELD_FLAG_PREFIX + name, "Z", null, null);
            flag.visitEnd();

            Type type = Type.getType(desc);

            if (type.getSort() == Type.ARRAY) {
                String gDesc = "()" + desc;
                createArrayGetter(name, gDesc, type);

                // Generates setter method
                String sDesc = "(" + desc + ")V";
                createArraySetter(name, sDesc, type);

            } else {
                // Generate the getter method
                String gDesc = "()" + desc;
                createSimpleGetter(name, gDesc, type);

                // Generates setter method
                String sDesc = "(" + desc + ")V";
                createSimpleSetter(name, sDesc, type);
            }

        }
        return cv.visitField(access, name, desc, signature, value);
    }

    /**
     * Create a constructor to call the manipulated constructor.
     * This constructor does not have any argument. It will call the manipulated
     * constructor with a null instance manager.
     * @param access : access flag
     * @param signature : method signature
     * @param exceptions : declared exception
     * @param annotations : the annotations to move to this constructor.
     */
    private void generateEmptyConstructor(int access, String signature, String[] exceptions, List annotations) {
        MethodVisitor mv = cv.visitMethod(access, "<init>", "()V", signature, exceptions);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ACONST_NULL);
        mv.visitMethodInsn(INVOKESPECIAL, m_owner, "<init>", "(Lorg/apache/felix/ipojo/InstanceManager;)V");
        mv.visitInsn(RETURN);
   
        // Move annotations
        if (annotations != null) {
            for (int i = 0; i < annotations.size(); i++) {
                AnnotationDescriptor ad = (AnnotationDescriptor) annotations.get(i);
                ad.visit(mv);
            }
        }
        
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Create a constructor to call the manipulated constructor.
     * This constructor has one argument (the bundle context). It will call the manipulated
     * constructor with a null instance manager.
     * @param access : access flag
     * @param signature : method signature
     * @param exceptions : declared exception
     * @param annotations : the annotations to move to this constructor.
     */
    private void generateBCConstructor(int access, String signature, String[] exceptions, List annotations) {
        MethodVisitor mv = cv.visitMethod(access, "<init>", "(Lorg/osgi/framework/BundleContext;)V", signature, exceptions);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESPECIAL, m_owner, "<init>", "(Lorg/apache/felix/ipojo/InstanceManager;Lorg/osgi/framework/BundleContext;)V");
        mv.visitInsn(RETURN);
        
        // Move annotations
        if (annotations != null) {
            for (int i = 0; i < annotations.size(); i++) {
                AnnotationDescriptor ad = (AnnotationDescriptor) annotations.get(i);
                ad.visit(mv);
            }
        }
        
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Generate the method header of a POJO method.
     * This method header encapsulate the POJO method call to
     * signal entry exit and error to the container.
     * @param access : access flag.
     * @param name : method name.
     * @param desc : method descriptor.
     * @param signature : method signature.
     * @param exceptions : declared exceptions.
     * @param annotations : the annotations to move to this method.
     */
    private void generateMethodHeader(int access, String name, String desc, String signature, String[] exceptions, List annotations) {
        GeneratorAdapter mv = new GeneratorAdapter(cv.visitMethod(access, name, desc, signature, exceptions), access, name, desc); 
        
        mv.visitCode();
        
        Type returnType = Type.getReturnType(desc);

        // Compute result and exception stack location
        int result = -1;
        int exception = -1;
        
        //int arguments = mv.newLocal(Type.getType((new Object[0]).getClass()));

        if (returnType.getSort() != Type.VOID) {
            // The method returns something 
            result = mv.newLocal(returnType);
            exception = mv.newLocal(Type.getType(Throwable.class));
        } else {
            exception = mv.newLocal(Type.getType(Throwable.class));
        }

        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();

        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Throwable");

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, generateMethodFlag(name, desc), "Z");
        mv.visitJumpInsn(IFNE, l0);

        mv.visitVarInsn(ALOAD, 0);
        mv.loadArgs();
        mv.visitMethodInsn(INVOKESPECIAL, m_owner, PREFIX + name, desc);
        mv.visitInsn(returnType.getOpcode(IRETURN));

        // end of the non intercepted method invocation.
        
        mv.visitLabel(l0);
        
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(generateMethodId(name, desc));
        mv.loadArgArray();
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", ENTRY, "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
        
        mv.visitVarInsn(ALOAD, 0);
            
        // Do not allow argument modification : just reload arguments.
        mv.loadArgs();
        mv.visitMethodInsn(INVOKESPECIAL, m_owner, PREFIX + name, desc);

        if (returnType.getSort() != Type.VOID) {
            mv.visitVarInsn(returnType.getOpcode(ISTORE), result);
        }

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(generateMethodId(name, desc));
        if (returnType.getSort() != Type.VOID) {
            mv.visitVarInsn(returnType.getOpcode(ILOAD), result);
            mv.box(returnType);
        } else {
            mv.visitInsn(ACONST_NULL);
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", EXIT, "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V");
        
        mv.visitLabel(l1);
        Label l7 = new Label();
        mv.visitJumpInsn(GOTO, l7);
        mv.visitLabel(l2);

        mv.visitVarInsn(ASTORE, exception);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(generateMethodId(name, desc));
        mv.visitVarInsn(ALOAD, exception);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", ERROR, "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Throwable;)V");
        mv.visitVarInsn(ALOAD, exception);
        mv.visitInsn(ATHROW);

        mv.visitLabel(l7);
        if (returnType.getSort() != Type.VOID) {
            mv.visitVarInsn(returnType.getOpcode(ILOAD), result);
        }
        mv.visitInsn(returnType.getOpcode(IRETURN));
        
        // Move annotations
        if (annotations != null) {
            for (int i = 0; i < annotations.size(); i++) {
                AnnotationDescriptor ad = (AnnotationDescriptor) annotations.get(i);
                ad.visit(mv);
            }
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Generate a method flag name.
     * @param name : method name.
     * @param desc : method descriptor.
     * @return the method flag name
     */
    private String generateMethodFlag(String name, String desc) {
        return METHOD_FLAG_PREFIX + generateMethodId(name, desc);
    }

    /**
     * Generate the method id based on the given method name and method descriptor.
     * The method Id is unique for this method and serves to create the flag field (so
     * must follow field name Java restrictions).
     * @param name : method name
     * @param desc : method descriptor
     * @return  method ID
     */
    private String generateMethodId(String name, String desc) {
        StringBuffer id = new StringBuffer(name);
        Type[] args = Type.getArgumentTypes(desc);
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].getClassName();
            if (arg.endsWith("[]")) {
                arg = arg.substring(0, arg.length() - 2);
                id.append("$" + arg.replace('.', '_') + "__");
            } else {
                id.append("$" + arg.replace('.', '_'));
            }
        }
        if (!m_methods.contains(id.toString())) {
            m_methods.add(id.toString());
        }
        return id.toString();
    }

    /**
     * Add the instance manager field (__im).
     */
    private void addIMField() {
        FieldVisitor fv = super.visitField(ACC_PRIVATE, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;", null, null);
        fv.visitEnd();
    }

    /**
     * Add the POJO interface to the visited class.
     * @param version : class version
     * @param access : class access
     * @param name : class name
     * @param signature : class signature
     * @param superName : super class
     * @param interfaces : implemented interfaces.
     */
    private void addPOJOInterface(int version, int access, String name, String signature, String superName, String[] interfaces) {

        // Add the POJO interface to the interface list
        // Check that the POJO interface is not already in the list
        boolean found = false;
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].equals(POJO)) {
                found = true;
            }
        }
        String[] itfs;
        if (!found) {
            itfs = new String[interfaces.length + 1];
            for (int i = 0; i < interfaces.length; i++) {
                itfs[i] = interfaces[i];
            }
            itfs[interfaces.length] = POJO;
        } else {
            itfs = interfaces;
        }

        cv.visit(version, access, name, signature, superName, itfs);
    }

    /**
     * Visit end.
     * Create helper methods.
     * @see org.objectweb.asm.ClassAdapter#visitEnd()
     */
    public void visitEnd() {
        // Create the component manager setter method
        createSetInstanceManagerMethod();

        // Add the getComponentInstance
        createGetComponentInstanceMethod();
        
        // Need to inject a constructor?
        if (! m_foundSuitableConstructor) { // No adequate constructor, create one.
            createSimpleConstructor();
        }

        m_methods.clear();
        m_methodFlags.clear();

        cv.visitEnd();
    }

    /**
     * Creates a simple constructor with an instance manager
     * in argument if no suitable constructor is found during
     * the visit.
     */
    private void createSimpleConstructor() {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>",
                "(Lorg/apache/felix/ipojo/InstanceManager;)V", null, null);
        mv.visitCode();

        // Super call
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, m_superclass, "<init>", "()V");

        // Call set instance manager
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, m_owner, "_setInstanceManager",
                "(Lorg/apache/felix/ipojo/InstanceManager;)V");

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Create the setter method for the __cm field.
     */
    private void createSetInstanceManagerMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "_setInstanceManager", "(Lorg/apache/felix/ipojo/InstanceManager;)V", null, null);
        mv.visitCode();

        // If the given instance manager is null, just returns.
        mv.visitVarInsn(ALOAD, 1);
        Label l1 = new Label();
        mv.visitJumpInsn(IFNONNULL, l1);
        mv.visitInsn(RETURN);
        mv.visitLabel(l1);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getRegistredFields", "()Ljava/util/Set;");
        mv.visitVarInsn(ASTORE, 2);

        mv.visitVarInsn(ALOAD, 2);
        Label endif = new Label();
        mv.visitJumpInsn(IFNULL, endif);
        Iterator it = m_fields.iterator();
        while (it.hasNext()) {
            String field = (String) it.next();
            mv.visitVarInsn(ALOAD, 2);
            mv.visitLdcInsn(field);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "contains", "(Ljava/lang/Object;)Z");
            Label l3 = new Label();
            mv.visitJumpInsn(IFEQ, l3);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ICONST_1);
            mv.visitFieldInsn(PUTFIELD, m_owner, FIELD_FLAG_PREFIX + field, "Z");
            mv.visitLabel(l3);
        }
        mv.visitLabel(endif);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", "getRegistredMethods", "()Ljava/util/Set;");
        mv.visitVarInsn(ASTORE, 2);

        mv.visitVarInsn(ALOAD, 2);
        Label endif2 = new Label();
        mv.visitJumpInsn(IFNULL, endif2);

        for (int i = 0; i < m_methods.size(); i++) {
            String methodId = (String) m_methods.get(i);
            if (!methodId.equals("<init>")) {
                mv.visitVarInsn(ALOAD, 2);
                mv.visitLdcInsn(methodId);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "contains", "(Ljava/lang/Object;)Z");
                Label l3 = new Label();
                mv.visitJumpInsn(IFEQ, l3);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitInsn(ICONST_1);
                mv.visitFieldInsn(PUTFIELD, m_owner, METHOD_FLAG_PREFIX + methodId, "Z");
                mv.visitLabel(l3);
            }
        }

        mv.visitLabel(endif2);

        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Create the getComponentInstance method.
     */
    private void createGetComponentInstanceMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "getComponentInstance", "()Lorg/apache/felix/ipojo/ComponentInstance;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Create a getter method for an array.
     * @param name : field name
     * @param desc : method description
     * @param type : contained type (inside the array)
     */
    private void createArraySetter(String name, String desc, Type type) {
        MethodVisitor mv = cv.visitMethod(0, "__set" + name, desc, null, null);
        mv.visitCode();

        String internalType = desc.substring(1);
        internalType = internalType.substring(0, internalType.length() - 2);

        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, FIELD_FLAG_PREFIX + name, "Z");
        Label l2 = new Label();
        mv.visitJumpInsn(IFNE, l2);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, m_owner, name, internalType);
        mv.visitInsn(RETURN);
        mv.visitLabel(l2);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(name);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", SET, "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V");

        mv.visitInsn(RETURN);

        // End
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Create a setter method for an array.
     * @param name : field name
     * @param desc : method description
     * @param type : contained type (inside the array)
     */
    private void createArrayGetter(String name, String desc, Type type) {
        String methodName = "__get" + name;
        MethodVisitor mv = cv.visitMethod(0, methodName, desc, null, null);
        mv.visitCode();

        String internalType = desc.substring(2);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, FIELD_FLAG_PREFIX + name, "Z");
        Label l1 = new Label();
        mv.visitJumpInsn(IFNE, l1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, name, internalType);
        mv.visitInsn(ARETURN);
        mv.visitLabel(l1);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(name);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", GET, "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
        mv.visitTypeInsn(CHECKCAST, internalType);
        mv.visitInsn(ARETURN);

        // End
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Create the getter for a field.
     * @param name : field of the dependency
     * @param desc : description of the getter method
     * @param type : type to return
     */
    private void createSimpleGetter(String name, String desc, Type type) {
        String methodName = "__get" + name;
        MethodVisitor mv = cv.visitMethod(0, methodName, desc, null, null);
        mv.visitCode();

        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:

                String internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                String boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];
                String unboxingMethod = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][2];

                Label l0 = new Label();
                mv.visitLabel(l0);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, FIELD_FLAG_PREFIX + name, "Z");
                Label l1 = new Label();
                mv.visitJumpInsn(IFNE, l1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, internalName);
                mv.visitInsn(IRETURN);

                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", GET, "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
                mv.visitVarInsn(ASTORE, 1);

                mv.visitVarInsn(ALOAD, 1);
                mv.visitTypeInsn(CHECKCAST, boxingType);
                mv.visitVarInsn(ASTORE, 2);

                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + internalName);
                mv.visitInsn(type.getOpcode(IRETURN));
                break;

            case Type.LONG:
                internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];
                unboxingMethod = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][2];

                l0 = new Label();
                mv.visitLabel(l0);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, FIELD_FLAG_PREFIX + name, "Z");
                l1 = new Label();
                mv.visitJumpInsn(IFNE, l1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, internalName);
                mv.visitInsn(LRETURN);
                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", GET, "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
                mv.visitVarInsn(ASTORE, 1);

                mv.visitVarInsn(ALOAD, 1);
                mv.visitTypeInsn(CHECKCAST, boxingType);
                mv.visitVarInsn(ASTORE, 2);

                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + internalName);
                mv.visitInsn(LRETURN);

                break;

            case Type.DOUBLE:
                internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];
                unboxingMethod = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][2];

                l0 = new Label();
                mv.visitLabel(l0);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, FIELD_FLAG_PREFIX + name, "Z");
                l1 = new Label();
                mv.visitJumpInsn(IFNE, l1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, internalName);
                mv.visitInsn(DRETURN);
                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", GET, "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
                mv.visitVarInsn(ASTORE, 1);

                mv.visitVarInsn(ALOAD, 1);
                mv.visitTypeInsn(CHECKCAST, boxingType);
                mv.visitVarInsn(ASTORE, 2);

                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + internalName);
                mv.visitInsn(DRETURN);

                break;

            case Type.FLOAT:
                internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];
                unboxingMethod = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][2];

                l0 = new Label();
                mv.visitLabel(l0);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, FIELD_FLAG_PREFIX + name, "Z");
                l1 = new Label();
                mv.visitJumpInsn(IFNE, l1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, internalName);
                mv.visitInsn(FRETURN);
                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", GET, "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
                mv.visitVarInsn(ASTORE, 1);

                mv.visitVarInsn(ALOAD, 1);
                mv.visitTypeInsn(CHECKCAST, boxingType);
                mv.visitVarInsn(ASTORE, 2);

                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + internalName);
                mv.visitInsn(FRETURN);

                break;

            case Type.OBJECT:
                l0 = new Label();
                mv.visitLabel(l0);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, FIELD_FLAG_PREFIX + name, "Z");
                l1 = new Label();
                mv.visitJumpInsn(IFNE, l1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, name, "L" + type.getInternalName() + ";");
                mv.visitInsn(ARETURN);
                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitLdcInsn(name);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", GET, "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
                mv.visitTypeInsn(CHECKCAST, type.getInternalName());
                mv.visitInsn(ARETURN);

                break;

            default:
                ManipulationProperty.getLogger().log(ManipulationProperty.SEVERE, "Manipulation problem in " + m_owner + " : a type is not implemented : " + type);
                break;
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Create the setter method for one property. The name of the method is _set+name of the field
     * @param name : name of the field representing a property
     * @param desc : description of the setter method
     * @param type : type of the property
     */
    private void createSimpleSetter(String name, String desc, Type type) {
        MethodVisitor mv = cv.visitMethod(0, "__set" + name, desc, null, null);
        mv.visitCode();

        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.FLOAT:
                String internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                String boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];

                Label l1 = new Label();
                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, FIELD_FLAG_PREFIX + name, "Z");
                Label l22 = new Label();
                mv.visitJumpInsn(IFNE, l22);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                mv.visitFieldInsn(PUTFIELD, m_owner, name, internalName);
                mv.visitInsn(RETURN);
                mv.visitLabel(l22);

                mv.visitTypeInsn(NEW, boxingType);
                mv.visitInsn(DUP);
                mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                mv.visitMethodInsn(INVOKESPECIAL, boxingType, "<init>", "(" + internalName + ")V");
                mv.visitVarInsn(ASTORE, 2);

                Label l2 = new Label();
                mv.visitLabel(l2);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitLdcInsn(name);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", SET, "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V");

                Label l3 = new Label();
                mv.visitLabel(l3);
                mv.visitInsn(RETURN);
                break;

            case Type.LONG:
            case Type.DOUBLE:
                internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
                boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];

                l1 = new Label();
                mv.visitLabel(l1);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, FIELD_FLAG_PREFIX + name, "Z");
                Label l23 = new Label();
                mv.visitJumpInsn(IFNE, l23);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                mv.visitFieldInsn(PUTFIELD, m_owner, name, internalName);
                mv.visitInsn(RETURN);
                mv.visitLabel(l23);

                mv.visitTypeInsn(NEW, boxingType);
                mv.visitInsn(DUP);
                mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                mv.visitMethodInsn(INVOKESPECIAL, boxingType, "<init>", "(" + internalName + ")V");
                mv.visitVarInsn(ASTORE, 3); // Double space

                l2 = new Label();
                mv.visitLabel(l2);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitLdcInsn(name);
                mv.visitVarInsn(ALOAD, 3);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", SET, "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V");

                l3 = new Label();
                mv.visitLabel(l3);
                mv.visitInsn(RETURN);
                break;

            case Type.OBJECT:
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, FIELD_FLAG_PREFIX + name, "Z");
                Label l24 = new Label();
                mv.visitJumpInsn(IFNE, l24);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, m_owner, name, "L" + type.getInternalName() + ";");
                mv.visitInsn(RETURN);
                mv.visitLabel(l24);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, m_owner, IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
                mv.visitVarInsn(ALOAD, 0);
                mv.visitLdcInsn(name);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", SET, "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V");

                mv.visitInsn(RETURN);
                break;
            default:
                ManipulationProperty.getLogger().log(ManipulationProperty.SEVERE, "Manipulation Error : Cannot create the setter method for the field : " + name + " (" + type + ")");
                break;
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

}
