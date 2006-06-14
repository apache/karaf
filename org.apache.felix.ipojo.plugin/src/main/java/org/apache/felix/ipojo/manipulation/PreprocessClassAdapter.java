/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.ipojo.manipulation;

import java.util.HashMap;
import java.util.logging.Level;

import org.apache.felix.ipojo.plugin.IpojoPluginConfiguration;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;


/**
 * Manipulate the class.
 * - Add a static component manager field (_cm) ok
 * - Create getter and setter for each fields ok
 * - Store information about field ok
 * - Store information about implemented interfaces ok
 * - Change GETFIELD and PUTFIELD to called the getter and setter method
 * @author Clement Escoffier
 *
 */
public class PreprocessClassAdapter extends ClassAdapter implements Opcodes {

        /**
         * The owner.
         * m_owner : String
         */
        private String  m_owner;

        /**
         * Interfaces implemented by the component.
         */
        private String[] m_itfs = new String[0];

		/**
		 * Field hashmap [field name, type] discovered in the component class.
		 */
		private HashMap m_fields = new HashMap();


        /**
         * Constructor.
         * @param cv : Class visitor
         */
        public PreprocessClassAdapter(final ClassVisitor cv) {
            super(cv);
        }

        /** The visit method.
         * - Insert the _cm field
         * - Create the _initialize method
         * - Create the _cm setter method
         * @see org.objectweb.asm.ClassVisitor#visit(int, int, String, String, String, String[])
         * @param version : Version
         * @param access : Access modifier
         * @param name : name of the visited element
         * @param signature : singature of the visited element
         * @param superName : superclasses (extend clause)
         * @param interfaces : implement clause
         */
        public void visit(
                final int version,
                final int access,
                final String name,
                final String signature,
                final String superName,
                final String[] interfaces) {

            m_owner = name;

            // Insert _cm field
            super.visitField(ACC_PUBLIC + ACC_STATIC, "_cm", ManipulationProperty.IPOJO_INTERNAL_DESCRIPTOR + "ComponentManager;", null, null);

            // Create the _cmSetter(ComponentManager cm) method
            createComponentManagerSetter();

            //Store the interfaces :
            m_itfs = interfaces;

            super.visit(version, access, name, signature, superName, interfaces);
        }

        /** visit method method :-).
         * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
         * @param access : access modifier
         * @param name : name of the method
         * @param desc : descriptor of the method
         * @param signature : signature of the method
         * @param exceptions : exception launched by the method
         * @return MethodVisitor
         */
        public MethodVisitor visitMethod(
                final int access,
                final String name,
                final String desc,
                final String signature,
                final String[] exceptions) {
            MethodVisitor mv = cv.visitMethod(access,
                    name,
                    desc,
                    signature,
                    exceptions);

            if (mv == null) { return null; }
            else { return new PreprocessCodeAdapter(mv, m_owner); }

        }

        /**
         * Create the setter method for the _cm field.
         * The generated method must be called only one time.
         */
        private void createComponentManagerSetter() {
            MethodVisitor mv = super.visitMethod(ACC_PUBLIC + ACC_STATIC, "setComponentManager", "(" + ManipulationProperty.IPOJO_INTERNAL_DESCRIPTOR + "ComponentManager;)V", null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(PUTSTATIC, m_owner, "_cm", ManipulationProperty.IPOJO_INTERNAL_DESCRIPTOR + "ComponentManager;");
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        /** visit Field method.
         * @see org.objectweb.asm.ClassVisitor#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
         * @param access : acces modifier
         * @param name : name of the field
         * @param desc : description of the field
         * @param signature : signature of the field
         * @param value : value of the field
         * @return FieldVisitor
         */
        public FieldVisitor visitField(
                final int access,
                final String name,
                final String desc,
                final String signature,
                final Object value) {
            if ((access & ACC_STATIC) == 0) {
            		IpojoPluginConfiguration.getLogger().log(Level.INFO, "Manipulate the field declaration of " + name);
                    Type type = Type.getType(desc);

                    // Keep the field in the code
                    FieldVisitor fv =  cv.visitField(access, name, desc, signature, value);

                    if (type.getSort() == Type.ARRAY) {
                    	if (type.getInternalName().startsWith("L")) {
                    		String internalType = type.getInternalName().substring(1);
                    		String nameType = internalType.replace('/', '.');
                            m_fields.put(name, nameType + "[]");
                    	}
                    	else {
                    		String nameType = type.getClassName().substring(0, type.getClassName().length() - 2);
                    		m_fields.put(name, nameType);
                    	}

                    	// TODO : Getter & SETTER on array :
                        String gDesc = "()" + desc;
                    	createArrayGetter(name, gDesc, type);

                    	// Generates setter method
                        String sDesc = "(" + desc + ")V";
                        createArraySetter(name, sDesc, type);

                    }
                    else {
                    	// Store information on the fields
                    	m_fields.put(name, type.getClassName());

                    	// Generate the getter method
                    	String gDesc = "()" + desc;
                    	createSimpleGetter(name, gDesc, type);

                    	// Generates setter method
                        String sDesc = "(" + desc + ")V";
                        createSimpleSetter(name, sDesc, type);
                    }

                    return fv;

            }
            return super.visitField(access, name, desc, signature, value);
        }

        private void createArraySetter(String name, String desc, Type type) {
        	MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "_set" + name, desc, null, null);

        	String internalType = desc.substring(1);
        	internalType = internalType.substring(0, internalType.length() - 2);

        	mv.visitVarInsn(ALOAD, 0);
        	mv.visitVarInsn(ALOAD, 1);
        	mv.visitFieldInsn(PUTFIELD, m_owner, name, internalType);

        	mv.visitFieldInsn(GETSTATIC, m_owner, "_cm", "Lorg/apache/felix/ipojo/ComponentManager;");
        	mv.visitLdcInsn(name);
        	mv.visitVarInsn(ALOAD, 1);
        	mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/ComponentManager", "setterCallback", "(Ljava/lang/String;Ljava/lang/Object;)V");

        	mv.visitInsn(RETURN);

        	// End
        	mv.visitMaxs(0, 0);
            mv.visitEnd();
		}

		private void createArrayGetter(String name, String desc, Type type) {

//			try {
//            	System.out.println("Field Name : " + name);
//            	System.out.println("Desc : " + desc);
//            	System.out.println("ClassName : " + type.getClassName());
//            	System.out.println("Descriptor of the type : " + type.getDescriptor());
//            	System.out.println("Internal Name : " + type.getInternalName());
//            } catch(Exception e) {}
//

			String methodName = "_get" + name;
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, desc, null, null);

            String internalType = desc.substring(2);

            	mv.visitVarInsn(ALOAD, 0);
            	//mv.visitFieldInsn(GETFIELD, m_owner, name, "["+type.getInternalName()+";");
            	mv.visitFieldInsn(GETFIELD, m_owner, name, internalType);
            	mv.visitVarInsn(ASTORE, 1);

            	mv.visitFieldInsn(GETSTATIC, m_owner, "_cm", "Lorg/apache/felix/ipojo/ComponentManager;");
            	mv.visitLdcInsn(name);
            	mv.visitVarInsn(ALOAD, 1);
            	mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/ComponentManager", "getterCallback", "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
            	mv.visitVarInsn(ASTORE, 2);

            	mv.visitVarInsn(ALOAD, 2);
            	mv.visitTypeInsn(CHECKCAST, internalType);
            	mv.visitVarInsn(ASTORE, 3);

            	Label l3a = new Label();
            	mv.visitLabel(l3a);

            	mv.visitVarInsn(ALOAD, 1);
            	Label l4a = new Label();
            	mv.visitJumpInsn(IFNULL, l4a);
            	mv.visitVarInsn(ALOAD, 1);
            	mv.visitVarInsn(ALOAD, 3);
            	mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z");

            	Label l5a = new Label();
            	mv.visitJumpInsn(IFNE, l5a);
            	mv.visitLabel(l4a);

            	mv.visitVarInsn(ALOAD, 0);
            	mv.visitVarInsn(ALOAD, 3);
            	mv.visitMethodInsn(INVOKEVIRTUAL, m_owner, "_set" + name, "(" + internalType + ")V");
            	mv.visitLabel(l5a);

            	mv.visitVarInsn(ALOAD, 3);
            	mv.visitInsn(ARETURN);

            	//End
            	mv.visitMaxs(0, 0);
                mv.visitEnd();
		}


		/**
         * Create the getter for service dependency.
         * @param name : field of the dependency
         * @param desc : description of the getter method
         * @param type : type to return
         */
        private void createSimpleGetter(String name, String desc, Type type) {

            String methodName = "_get" + name;
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, methodName, desc, null, null);

            switch (type.getSort()) {
            	case Type.BOOLEAN :
            	case Type.CHAR :
            	case Type.BYTE :
            	case Type.SHORT :
           		case Type.INT :
           		case Type.FLOAT :
           		case Type.LONG :
            	case Type.DOUBLE :

            		String internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
            		String boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];
            		String unboxingMethod = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][2];

            		mv.visitVarInsn(ALOAD, 0);
            		mv.visitFieldInsn(GETFIELD, m_owner, name, internalName);
            		mv.visitVarInsn(type.getOpcode(ISTORE), 1);

            		mv.visitTypeInsn(NEW, boxingType);
            		mv.visitInsn(DUP);
            		mv.visitVarInsn(type.getOpcode(ILOAD), 1);
            		mv.visitMethodInsn(INVOKESPECIAL, boxingType, "<init>", "(" + internalName + ")V");
            		mv.visitVarInsn(ASTORE, 2);

            		mv.visitFieldInsn(GETSTATIC, m_owner, "_cm", "Lorg/apache/felix/ipojo/ComponentManager;");
            		mv.visitLdcInsn(name);
            		mv.visitVarInsn(ALOAD, 2);
            		mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/ComponentManager", "getterCallback", "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
            		mv.visitVarInsn(ASTORE, 3);

            		mv.visitVarInsn(ALOAD, 3);
            		mv.visitTypeInsn(CHECKCAST, boxingType);
            		mv.visitVarInsn(ASTORE, 4);

            		mv.visitVarInsn(ALOAD, 4);
            		mv.visitMethodInsn(INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + internalName);
            		mv.visitVarInsn(type.getOpcode(ISTORE), 5);

            		Label l5 = new Label();
            		mv.visitLabel(l5);

            		mv.visitVarInsn(type.getOpcode(ILOAD), 1);
            		mv.visitVarInsn(type.getOpcode(ILOAD), 5);
            		Label l6 = new Label();
            		mv.visitJumpInsn(type.getOpcode(IF_ICMPEQ), l6);

            		Label l7 = new Label();
            		mv.visitLabel(l7);
            			mv.visitVarInsn(ALOAD, 0);
            			mv.visitVarInsn(type.getOpcode(ILOAD), 5);
            			mv.visitMethodInsn(INVOKEVIRTUAL, m_owner, "_set" + name, "(" + internalName + ")V");
            		mv.visitLabel(l6);

            		mv.visitVarInsn(type.getOpcode(ILOAD), 5);
            		mv.visitInsn(type.getOpcode(IRETURN));
            		break;

            	case  Type.OBJECT :

            		mv.visitVarInsn(ALOAD, 0);
            		mv.visitFieldInsn(GETFIELD, m_owner, name, "L" + type.getInternalName() + ";");
            		mv.visitVarInsn(ASTORE, 1);

            		mv.visitFieldInsn(GETSTATIC, m_owner, "_cm", "Lorg/apache/felix/ipojo/ComponentManager;");
            		mv.visitLdcInsn(name);
            		mv.visitVarInsn(ALOAD, 1);
            		mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/ComponentManager", "getterCallback", "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;");
            		mv.visitVarInsn(ASTORE, 2);

            		mv.visitVarInsn(ALOAD, 2);
            		mv.visitTypeInsn(CHECKCAST, type.getInternalName());
            		mv.visitVarInsn(ASTORE, 3);

                	Label l3b = new Label();
                	mv.visitLabel(l3b);

                	mv.visitVarInsn(ALOAD, 1);
                	Label l4b = new Label();
                	mv.visitJumpInsn(IFNULL, l4b);
                	mv.visitVarInsn(ALOAD, 1);
                	mv.visitVarInsn(ALOAD, 3);
                	mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z");

                	Label l5b = new Label();
                	mv.visitJumpInsn(IFNE, l5b);
                	mv.visitLabel(l4b);

                	mv.visitVarInsn(ALOAD, 0);
                	mv.visitVarInsn(ALOAD, 3);
                	mv.visitMethodInsn(INVOKEVIRTUAL, m_owner, "_set" + name, "(L" + type.getInternalName() + ";)V");
                	mv.visitLabel(l5b);

                	mv.visitVarInsn(ALOAD, 3);
                	mv.visitInsn(ARETURN);

            	break;

                default :
                	IpojoPluginConfiguration.getLogger().log(Level.SEVERE, "Manipulation problem in " + m_owner + " : a type is not implemented : " + type);
                    break;
            }

            mv.visitMaxs(0, 0);
            mv.visitEnd();
    }

        /**
         * Create the setter method for one property.
         * The name of the method is _set+name of the field
         * @param name : name of the field representing a property
         * @param desc : description of the setter method
         * @param type : type of the property
         */
        private void createSimpleSetter(String name, String desc, Type type) {
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "_set" + name, desc, null, null);

            switch(type.getSort()) {
        		case Type.BOOLEAN :
        		case Type.CHAR :
        		case Type.BYTE :
        		case Type.SHORT :
       			case Type.INT :
       			case Type.FLOAT :
       			case Type.LONG :
       			case Type.DOUBLE :

        		String internalName = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][0];
        		String boxingType = ManipulationProperty.PRIMITIVE_BOXING_INFORMATION[type.getSort()][1];

        			mv.visitVarInsn(ALOAD, 0);
                	mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                	mv.visitFieldInsn(PUTFIELD, m_owner, name, internalName);
                	Label l1 = new Label();
                	mv.visitLabel(l1);

                	mv.visitTypeInsn(NEW, boxingType);
                	mv.visitInsn(DUP);
                	mv.visitVarInsn(type.getOpcode(ILOAD), 1);
                	mv.visitMethodInsn(INVOKESPECIAL, boxingType, "<init>", "(" + internalName + ")V");
                	mv.visitVarInsn(ASTORE, 2);

                	Label l2 = new Label();
                	mv.visitLabel(l2);
                	mv.visitFieldInsn(GETSTATIC, m_owner, "_cm", "Lorg/apache/felix/ipojo/ComponentManager;");
                	mv.visitLdcInsn(name);
                	mv.visitVarInsn(ALOAD, 2);
                	mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/ComponentManager", "setterCallback", "(Ljava/lang/String;Ljava/lang/Object;)V");

                	Label l3 = new Label();
                	mv.visitLabel(l3);
                	mv.visitInsn(RETURN);
                    break;
                case (Type.OBJECT) :

                	mv.visitVarInsn(ALOAD, 0);
                	mv.visitVarInsn(ALOAD, 1);
                	mv.visitFieldInsn(PUTFIELD, m_owner, name, "L" + type.getInternalName() + ";");

                	mv.visitFieldInsn(GETSTATIC, m_owner, "_cm", "Lorg/apache/felix/ipojo/ComponentManager;");
                	mv.visitLdcInsn(name);
                	mv.visitVarInsn(ALOAD, 1);
                	mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/ComponentManager", "setterCallback", "(Ljava/lang/String;Ljava/lang/Object;)V");

                	mv.visitInsn(RETURN);
                	break;
                default :
                	IpojoPluginConfiguration.getLogger().log(Level.SEVERE, "Manipulation Error : Cannot create the setter method for the field : " + name + " (" + type + ")");
                    break;
            }

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        /**
         * @return the interfaces implemented by the component class.
         */
        public String[] getInterfaces() {
            return m_itfs;
        }

		/**
		 * @return the field hashmap [field_name, type]
		 */
		public HashMap getFields() {
			return m_fields;
		}
}

