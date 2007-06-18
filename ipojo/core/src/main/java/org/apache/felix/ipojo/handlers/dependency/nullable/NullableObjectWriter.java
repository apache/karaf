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
package org.apache.felix.ipojo.handlers.dependency.nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Create the proxy class.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class NullableObjectWriter implements Opcodes {

    /**
     * Return the proxy classname for the contract contractname on the service
     * object soc.
     * 
     * @param url URL of the needed contract
     * @param contractName String
     * @return byte[]
     */
    public static byte[] dump(URL url, String contractName) {

        ClassReader cr = null;
        InputStream is = null;
        byte[] b = null;
        try {
            is = url.openStream();
            cr = new ClassReader(is);
            MethodSignatureVisitor msv = new MethodSignatureVisitor();
            cr.accept(msv, ClassReader.SKIP_FRAMES);
            is.close();

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            // String[] segment = contractName.split("[.]");
            // String className = "org/apache/felix/ipojo/" +
            // segment[segment.length - 1] + "Nullable";
            String className = contractName.replace('.', '/') + "Nullable";

            // Create the class
            cw.visit(V1_2, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", new String[] { contractName.replace('.', '/'),
                "org/apache/felix/ipojo/Nullable" });

            // Inject a constructor <INIT>()V
            MethodVisitor cst = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            cst.visitVarInsn(ALOAD, 0);
            cst.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
            cst.visitInsn(RETURN);
            cst.visitMaxs(0, 0);
            cst.visitEnd();

            // Methods Generation :
            MethodSignature[] methods = msv.getMethods();

            for (int i = 0; i < methods.length; ++i) {
                MethodSignature method = methods[i];
                String desc = method.getDesc();
                String name = method.getName();
                String sign = method.getSignature();
                String[] exc = method.getException();

                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, name, desc, sign, exc);

                Type returnType = Type.getReturnType(desc);
                // Primitive type :
                switch (returnType.getSort()) {
                    case Type.BOOLEAN:
                    case Type.INT:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.SHORT:
                        // Integer or Boolean : return 0 ( false)
                        mv.visitInsn(ICONST_0);
                        mv.visitInsn(IRETURN);
                        break;
                    case Type.LONG:
                        mv.visitInsn(LCONST_0);
                        mv.visitInsn(LRETURN);
                        break;
                    case Type.DOUBLE:
                        // Double : return 0.0
                        mv.visitInsn(DCONST_0);
                        mv.visitInsn(DRETURN);
                        break;
                    case Type.FLOAT:
                        // Double : return 0.0
                        mv.visitInsn(FCONST_0);
                        mv.visitInsn(FRETURN);
                        break;
                    case Type.ARRAY:
                    case Type.OBJECT:
                        // Return always null for array and object
                        mv.visitInsn(ACONST_NULL);
                        mv.visitInsn(ARETURN);
                        break;
                    case Type.VOID:
                        mv.visitInsn(RETURN);
                        break;
                    default:
                        System.err.println("Type not yet managed : " + returnType);
                        break;
                }
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            // End process
            cw.visitEnd();
            b = cw.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return b;
    }
}
