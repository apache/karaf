/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.felix.tool.mangen;

import java.io.IOException;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 *
 * @version $Revision: 32 $
 * @author <A HREF="mailto:heavy@ungoverned.org">Richard S. Hall</A> 
 */
public class ASMClassScanner implements ClassScanner, ClassVisitor, MethodVisitor
{
    private static final int DEFAULT_INCREMENT = 10;

    private int m_fieldCount = 0;
    private String[] m_fieldNames = new String[DEFAULT_INCREMENT];
    private String[] m_fieldSignatures = new String[DEFAULT_INCREMENT];
    private boolean[] m_fieldSynthFlags = new boolean[DEFAULT_INCREMENT];

    private int m_methodCount = 0;
    private String[] m_methodNames = new String[DEFAULT_INCREMENT];
    private String[] m_methodSignatures = new String[DEFAULT_INCREMENT];
    private boolean[] m_methodSynthFlags = new boolean[DEFAULT_INCREMENT];

    private int m_classCount = 0;
    private String[] m_classSignatures = new String[DEFAULT_INCREMENT];

    public ASMClassScanner()
    {
    }

    //
    // Methods for ClassScanner interface.
    //

    public void scan(java.io.InputStream is, String name) throws IOException
    {
        ClassReader cr = new ClassReader(is);
        //TODO: below is ASM 3.0 form, will need to use once migrate from 2.2.1
        //cr.accept(this, ClassReader.SKIP_DEBUG + ClassReader.SKIP_FRAMES);
        cr.accept(this, false);
    }

    public int getFieldCount()
    {
        return m_fieldCount;
    }

    public String getFieldName(int index)
    {
        return m_fieldNames[index];
    }

    public String getFieldSignature(int index)
    {
        return m_fieldSignatures[index];
    }

    public boolean isSyntheticField(int index)
    {
        return m_fieldSynthFlags[index];
    }

    public int getMethodCount()
    {
        return m_methodCount;
    }

    public String getMethodName(int index)
    {
        return m_methodNames[index];
    }

    public String getMethodSignature(int index)
    {
        return m_methodSignatures[index];
    }

    public boolean isSyntheticMethod(int index)
    {
        return m_methodSynthFlags[index];
    }

    public int getConstantClassCount()
    {
        return m_classCount;
    }

    public String getConstantClassSignature(int index)
    {
        return m_classSignatures[index];
    }

    //
    // Methods for ClassVisitor interface.
    //

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
    {
        //System.out.println("visit: " + name + " : " + signature + " : " + superName);
        // Capture super type dependency.
        addConstantClass(superName);
        // Capture implemented interface type dependencies.
        for (int i = 0; (interfaces != null) && (i < interfaces.length); i++)
        {
            //System.out.println("visit interfaces: " + interfaces[i]);
            addConstantClass(interfaces[i]);
        }
        // Capture class type itself, since it depends on itself.
        addConstantClass(name);
    }

    public void visitAttribute(org.objectweb.asm.Attribute attr)
    {
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
    {
        //System.out.println("visitField: " + name + " : " + desc + " : " + signature + " : " + value);
        // Capture field type dependency.
        addField(name, desc, (access & Opcodes.ACC_SYNTHETIC) != 0);
        return null;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
    {
        //System.out.println("visitMethod: " + name + " : " + desc + " : " + signature);
        // Capture declared method exception type dependencies.
        for (int i = 0; (exceptions != null) && (i < exceptions.length); i++)
        {
        //System.out.println("visitField exceptions: " + exceptions[i]);
            addConstantClass(exceptions[i]);
        }
        // Capture declared method signature type dependencies.
        addMethod(name, desc, (access & Opcodes.ACC_SYNTHETIC) != 0);
        return this;
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access)
    {
        //System.out.println("visitInnerClass: " + name + " : " + outerName + " : " + innerName);
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible)
    {
        //System.out.println("visitAnnotation " + desc + " : " + visible);
        return null;
    }

    public void visitSource(String source, String debug)
    {
    }

    public void visitOuterClass(String owner, String name, String desc)
    {
        //System.out.println("visitOuterClass: " + name + " : " + desc);
    }

    public void visitEnd()
    {
    }

    //
    // Methods for MethodVisitor interface.
    //

    public AnnotationVisitor visitAnnotationDefault()
    {
        return null;
    }

// A method with this name is already provided in the ClassVisitor interface
// above, but since neither method does anything we can just ignore it.
//
//    public AnnotationVisitor visitAnnotation(String desc, boolean visible)
//    {
//        return null;
//    }

    public AnnotationVisitor visitParameterAnnotation(
            int parameter,
            String desc,
            boolean visible)
    {
        //System.out.println("visitParameterAnnotation: " + desc + " : " + visible);
        return null;
    }

// A method with this name is already provided in the ClassVisitor interface
// above, but since neither method does anything we can just ignore it.
//
//    public void visitAttribute(Attribute attr)
//    {
//    }

    public void visitCode()
    {
    }

    public void visitInsn(int opcode)
    {
    }

    public void visitIntInsn(int opcode, int operand)
    {
    }

    public void visitVarInsn(int opcode, int var)
    {
    }

    public void visitTypeInsn(int opcode, String desc)
    {
        //System.out.println("visitTypeInsn: " + desc);
        // This captures type operation type dependency (e.g., new, instanceof).
        addConstantClass(desc);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc)
    {
        //System.out.println("visitFieldInsn: " + owner + " : " + name + " : " + desc);
        // This captures the owner type dependency of fields we access.
        addConstantClass(owner);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc)
    {
        //System.out.println("visitMethodInsn: " + owner + " : " + name + " : " + desc);
        // Capture the owner type dependency of the method we invoke.
        // This is necessary to capture the use of static methods,
        // but it also captures the use of methods on return arguments.
        // Capturing the type of return objects is not strictly necessary,
        // since the type will be captured if assigned to a local variable.
        // However, not all returned objects are assigned to a local
        // variable, so this will capture types of return objects that we use
        // directly (e.g., obj.getFoo().getBar()).
        addConstantClass(owner);
    }

    public void visitJumpInsn(int opcode, Label label)
    {
    }

    public void visitLabel(Label label)
    {
    }

    public void visitLdcInsn(Object cst)
    {
        //System.out.println("visitLdcInsn: " + cst);
    }

    public void visitIincInsn(int var, int increment)
    {
    }

    public void visitTableSwitchInsn(int min, int max, Label dflt, Label labels[])
    {
    }

    public void visitLookupSwitchInsn(Label dflt, int keys[], Label labels[])
    {
    }

    public void visitMultiANewArrayInsn(String desc, int dims)
    {
        //System.out.println("visitMultiANewArrayInsn: " + desc);
    }

    public void visitTryCatchBlock(Label start, Label end, Label handler, String type)
    {
    }

    public void visitLocalVariable(
            String name,
            String desc,
            String signature,
            Label start,
            Label end,
            int index)
    {
        //System.out.println("visitLocalVariable: " + name + " : " + desc + " : " + signature);
        // Capture local variable type dependency, but ignore
        // primitive types.
        if (desc.startsWith("L"))
        {
            // The "desc" variable is in the form "L<class>;", so
            // extract just the class name, since mangen expects
            // a class name only or an array.
            addConstantClass(desc.substring(1, desc.length() - 1));
        }
        else if (desc.indexOf("[L") >= 0)
        {
            addConstantClass(desc);
        }
    }

    public void visitLineNumber(int line, Label start)
    {
    }

    public void visitMaxs(int maxStack, int maxLocals)
    {
    }
    
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, 
            Object[] stack)
    {
    }

// A method with this name is already provided in the ClassVisitor interface
// above, but since neither method does anything we can just ignore it.
//
//    public void visitEnd()
//    {
//    }

    //
    // Utility methods.
    //

    private void addField(String name, String signature, boolean synth)
    {
        m_fieldNames = addToStringArray(m_fieldCount, m_fieldNames, name);
        m_fieldSignatures = addToStringArray(m_fieldCount, m_fieldSignatures, signature);
        m_fieldSynthFlags = addToBooleanArray(m_fieldCount, m_fieldSynthFlags, synth);
        m_fieldCount++;
    }

    private void addMethod(String name, String signature, boolean synth)
    {
        m_methodNames = addToStringArray(m_methodCount, m_methodNames, name);
        m_methodSignatures = addToStringArray(m_methodCount, m_methodSignatures, signature);
        m_methodSynthFlags = addToBooleanArray(m_methodCount, m_methodSynthFlags, synth);
        m_methodCount++;
    }

    private void addConstantClass(String signature)
    {
        m_classSignatures = addToStringArray(m_classCount, m_classSignatures, signature);
        m_classCount++;
    }

    public static boolean[] addToBooleanArray(int count, boolean[] bs, boolean b)
    {
        if (count < bs.length)
        {
            bs[count] = b;
        }
        else
        {
            boolean[] bs2 = new boolean[bs.length + DEFAULT_INCREMENT];
            System.arraycopy(bs, 0, bs2, 0, bs.length);
            bs2[bs.length] = b;
            bs = bs2;
        }
        return bs;
    }

    public static String[] addToStringArray(int count, String[] ss, String s)
    {
        if (count < ss.length)
        {
            ss[count] = s;
        }
        else
        {
            String[] ss2 = new String[ss.length + DEFAULT_INCREMENT];
            System.arraycopy(ss, 0, ss2, 0, ss.length);
            ss2[ss.length] = s;
            ss = ss2;
        }
        return ss;
    }
}
