/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.service.interceptor.impl.runtime.proxy;

import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;
import static org.objectweb.asm.Opcodes.*;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

// forked from OWB
public class AsmProxyFactory {
    private static final Method[] EMPTY_METHODS = new Method[0];

    private static final String FIELD_INTERCEPTOR_HANDLER = "karafInterceptorProxyHandler";
    private static final String FIELD_INTERCEPTED_METHODS = "karafInterceptorProxyMethods";

    public <T> T create(final Class<?> clazz, final InterceptorHandler handler) {
        try {
            final T proxy = (T) clazz.getConstructor().newInstance();
            final Field invocationHandlerField = clazz.getDeclaredField(FIELD_INTERCEPTOR_HANDLER);
            invocationHandlerField.setAccessible(true);
            invocationHandlerField.set(proxy, handler);
            return proxy;
        } catch (final IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException ite) {
            throw new IllegalStateException(ite.getTargetException());
        }
    }

    public <T> Class<T> createProxyClass(final ProxyFactory.ProxyClassLoader classLoader,
                                         final String proxyClassName, final Class<?>[] classesToProxy,
                                         final Method[] interceptedMethods) {
        try {
            return (Class<T>) Class.forName(proxyClassName, true, classLoader);
        } catch (final ClassNotFoundException cnfe) {
            return doCreateProxyClass(classLoader, proxyClassName, classesToProxy, interceptedMethods);
        }
    }

    private <T> Class<T> doCreateProxyClass(final ProxyFactory.ProxyClassLoader classLoader, final String proxyClassName,
                                            final Class<?>[] classesToProxy, final Method[] interceptedMethods) {
        final String proxyClassFileName = proxyClassName.replace('.', '/');
        final byte[] proxyBytes = generateProxy(classesToProxy, proxyClassFileName, sortOutDuplicateMethods(interceptedMethods));
        final Class<T> proxyCLass = classLoader.getOrRegister(proxyClassName, proxyBytes, classesToProxy[0].getPackage(), classesToProxy[0].getProtectionDomain());
        try {
            final Field interceptedMethodsField = proxyCLass.getDeclaredField(FIELD_INTERCEPTED_METHODS);
            interceptedMethodsField.setAccessible(true);
            interceptedMethodsField.set(null, interceptedMethods);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        return proxyCLass;
    }

    private Method[] sortOutDuplicateMethods(final Method[] methods) {
        if (methods == null || methods.length == 0) {
            return null;
        }

        final List<Method> duplicates = new ArrayList<>();
        for (final Method outer : methods) {
            for (final Method inner : methods) {
                if (inner != outer
                        && hasSameSignature(outer, inner)
                        && !(duplicates.contains(outer) || duplicates.contains(inner))) {
                    duplicates.add(inner);
                }
            }
        }

        final List<Method> outsorted = new ArrayList<>(Arrays.asList(methods));
        outsorted.removeAll(duplicates);
        return outsorted.toArray(EMPTY_METHODS);
    }

    private boolean hasSameSignature(Method a, Method b) {
        return a.getName().equals(b.getName())
                && a.getReturnType().equals(b.getReturnType())
                && Arrays.equals(a.getParameterTypes(), b.getParameterTypes());
    }

    private void createConstructor(final ClassWriter cw, final String proxyClassFileName, final Class<?> classToProxy,
                                   final String classFileName) {
        Constructor superDefaultCt;
        String parentClassFileName = classFileName;
        String descriptor = "()V";

        try {
            if (classToProxy.isInterface()) {
                parentClassFileName = Type.getInternalName(Object.class);
                superDefaultCt = Object.class.getConstructor(null);
                descriptor = Type.getConstructorDescriptor(superDefaultCt);
            }
        } catch (final NoSuchMethodException nsme) {
            // no worries
        }

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", descriptor, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, parentClassFileName, "<init>", descriptor, false);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ACONST_NULL);
        mv.visitFieldInsn(PUTFIELD, proxyClassFileName, FIELD_INTERCEPTOR_HANDLER, Type.getDescriptor(InterceptorHandler.class));

        mv.visitInsn(RETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private byte[] generateProxy(final Class<?>[] classesToProxy, final String proxyClassFileName, final Method[] interceptedMethods) {
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final String classFileName = classesToProxy[0].getName().replace('.', '/');

        final String[] interfaces = Stream.of(classesToProxy)
                .filter(Class::isInterface)
                .map(Type::getInternalName)
                .toArray(String[]::new);
        final String superClassName;
        if (interfaces.length == classesToProxy.length) {
            superClassName = Type.getInternalName(Object.class);
        } else {
            superClassName = Type.getInternalName(classesToProxy[0]);
        }

        cw.visit(findJavaVersion(classesToProxy[0]), ACC_PUBLIC + ACC_SUPER + ACC_SYNTHETIC, proxyClassFileName, null, superClassName, interfaces);
        cw.visitSource(classFileName + ".java", null);
        createInstanceVariables(cw);
        createConstructor(cw, proxyClassFileName, classesToProxy[0], classFileName);
        if (interceptedMethods != null) {
            delegateInterceptedMethods(cw, proxyClassFileName, classesToProxy[0], interceptedMethods);
        }
        return cw.toByteArray();
    }

    private void createInstanceVariables(final ClassWriter cw) {
        cw.visitField(ACC_PRIVATE,
                FIELD_INTERCEPTOR_HANDLER, Type.getDescriptor(InterceptorHandler.class), null, null).visitEnd();
        cw.visitField(ACC_PRIVATE | ACC_STATIC,
                FIELD_INTERCEPTED_METHODS, Type.getDescriptor(Method[].class), null, null).visitEnd();
    }

    private void delegateInterceptedMethods(final ClassWriter cw,
                                            final String proxyClassFileName, final Class<?> classToProxy,
                                            final Method[] interceptedMethods) {
        for (int i = 0; i < interceptedMethods.length; i++) {
            if (!unproxyableMethod(interceptedMethods[i])) {
                generateInterceptorHandledMethod(cw, interceptedMethods[i], i, classToProxy, proxyClassFileName);
            }
        }
    }

    private void generateInterceptorHandledMethod(final ClassWriter cw, final Method method, final int methodIndex,
                                                  final Class<?> classToProxy, final String proxyClassFileName) {
        if ("<init>".equals(method.getName())) {
            return;
        }

        final Class<?> returnType = method.getReturnType();
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Class<?>[] exceptionTypes = method.getExceptionTypes();
        final int modifiers = method.getModifiers();
        if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
            throw new IllegalStateException("It's not possible to proxy a final or static method: " + classToProxy.getName() + " " + method.getName());
        }

        // push the method definition
        final int modifier = modifiers & (ACC_PUBLIC | ACC_PROTECTED | ACC_VARARGS);

        final MethodVisitor mv = cw.visitMethod(modifier, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();
        // push try/catch block, to catch declared exceptions, and to catch java.lang.Throwable
        final Label l0 = new Label();
        final Label l1 = new Label();
        final Label l2 = new Label();

        if (exceptionTypes.length > 0) {
            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/reflect/InvocationTargetException");
        }

        // push try code
        mv.visitLabel(l0);
        final String classNameToOverride = method.getDeclaringClass().getName().replace('.', '/');
        mv.visitLdcInsn(Type.getType("L" + classNameToOverride + ";"));

        // the following code generates the bytecode for this line of Java:
        // Method method = <proxy>.class.getMethod("add", new Class[] { <array of function argument classes> });

        // get the method name to invoke, and push to stack
        mv.visitLdcInsn(method.getName());

        // create the Class[]
        createArrayDefinition(mv, parameterTypes.length, Class.class);

        int length = 1;

        // push parameters into array
        for (int i = 0; i < parameterTypes.length; i++) {
            // keep copy of array on stack
            mv.visitInsn(DUP);

            final Class<?> parameterType = parameterTypes[i];

            // push number onto stack
            pushIntOntoStack(mv, i);

            if (parameterType.isPrimitive()) {
                String wrapperType = getWrapperType(parameterType);
                mv.visitFieldInsn(GETSTATIC, wrapperType, "TYPE", "Ljava/lang/Class;");
            } else {
                mv.visitLdcInsn(Type.getType(parameterType));
            }

            mv.visitInsn(AASTORE);

            if (Long.TYPE.equals(parameterType) || Double.TYPE.equals(parameterType)) {
                length += 2;
            } else {
                length++;
            }
        }

        // the following code generates bytecode equivalent to:
        // return ((<returntype>) invocationHandler.invoke(this, {methodIndex}, new Object[] { <function arguments }))[.<primitive>Value()];

        final Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, proxyClassFileName, FIELD_INTERCEPTOR_HANDLER, Type.getDescriptor(InterceptorHandler.class));
        mv.visitFieldInsn(GETSTATIC, proxyClassFileName, FIELD_INTERCEPTED_METHODS, Type.getDescriptor(Method[].class));
        if (methodIndex < 128) {
            mv.visitIntInsn(BIPUSH, methodIndex);
        } else if (methodIndex < 32267) {
            mv.visitIntInsn(SIPUSH, methodIndex);
        } else {
            throw new IllegalStateException("Sorry, we only support Classes with 2^15 methods...");
        }

        mv.visitInsn(AALOAD);
        pushMethodParameterArray(mv, parameterTypes);
        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(InterceptorHandler.class), "invoke",
                "(Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;", true);
        mv.visitTypeInsn(CHECKCAST, getCastType(returnType));
        if (returnType.isPrimitive() && (!Void.TYPE.equals(returnType))) {
            // get the primitive value
            mv.visitMethodInsn(INVOKEVIRTUAL, getWrapperType(returnType), getPrimitiveMethod(returnType),
                    "()" + Type.getDescriptor(returnType), false);
        }

        mv.visitLabel(l1);
        if (!Void.TYPE.equals(returnType)) {
            mv.visitInsn(getReturnInsn(returnType));
        } else {
            mv.visitInsn(POP);
            mv.visitInsn(RETURN);
        }

        // catch InvocationTargetException
        if (exceptionTypes.length > 0) {
            mv.visitLabel(l2);
            mv.visitVarInsn(ASTORE, length);

            Label l5 = new Label();
            mv.visitLabel(l5);

            for (int i = 0; i < exceptionTypes.length; i++) {
                Class<?> exceptionType = exceptionTypes[i];

                mv.visitLdcInsn(Type.getType("L" + exceptionType.getCanonicalName().replace('.', '/') + ";"));
                mv.visitVarInsn(ALOAD, length);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getCause",
                        "()Ljava/lang/Throwable;", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z", false);

                Label l6 = new Label();
                mv.visitJumpInsn(IFEQ, l6);

                Label l7 = new Label();
                mv.visitLabel(l7);

                mv.visitVarInsn(ALOAD, length);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getCause",
                        "()Ljava/lang/Throwable;", false);
                mv.visitTypeInsn(CHECKCAST, getCastType(exceptionType));
                mv.visitInsn(ATHROW);
                mv.visitLabel(l6);

                if (i == (exceptionTypes.length - 1)) {
                    mv.visitTypeInsn(NEW, "java/lang/reflect/UndeclaredThrowableException");
                    mv.visitInsn(DUP);
                    mv.visitVarInsn(ALOAD, length);
                    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/reflect/UndeclaredThrowableException", "<init>",
                            "(Ljava/lang/Throwable;)V", false);
                    mv.visitInsn(ATHROW);
                }
            }
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private int findJavaVersion(final Class<?> from) {
        final String resource = from.getName().replace('.', '/') + ".class";
        try (final InputStream stream = from.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                return V1_8;
            }
            final ClassReader reader = new ClassReader(stream);
            final VersionVisitor visitor = new VersionVisitor();
            reader.accept(visitor, SKIP_DEBUG + SKIP_CODE + SKIP_FRAMES);
            if (visitor.version != 0) {
                return visitor.version;
            }
        } catch (final Exception e) {
            // no-op
        }
        // mainly for JVM classes - outside the classloader, find to fallback on the JVM version
        final String javaVersionProp = System.getProperty("java.version", "1.8");
        if (javaVersionProp.startsWith("1.8")) {
            return V1_8;
        } else if (javaVersionProp.startsWith("9") || javaVersionProp.startsWith("1.9")) {
            return V9;
        } else if (javaVersionProp.startsWith("10")) {
            return V10;
        } else if (javaVersionProp.startsWith("11")) {
            return V11;
        } else if (javaVersionProp.startsWith("12")) {
            return V12;
        } else if (javaVersionProp.startsWith("13")) {
            return V13;
        } else if (javaVersionProp.startsWith("14")) {
            return V14;
        } else if (javaVersionProp.startsWith("15")) {
            return V15;
        } else if (javaVersionProp.startsWith("16")) {
            return V16;
        } else if (javaVersionProp.startsWith("17")) {
            return V17;
        } else if (javaVersionProp.startsWith("18")) {
            return V18;
        } else if (javaVersionProp.startsWith("19")) {
            return V19;
        }
        try {
            final int i = Integer.parseInt(javaVersionProp);
            if (i > 13) {
                return V13 + (i - 13);
            }
            return V1_8;
        } catch (final NumberFormatException nfe) {
            return V1_8;
        }
    }

    private boolean unproxyableMethod(final Method delegatedMethod) {
        final int modifiers = delegatedMethod.getModifiers();
        return (modifiers & (Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL | Modifier.NATIVE)) > 0 ||
                "finalize".equals(delegatedMethod.getName()) || delegatedMethod.isBridge();
    }

    /**
     * @return the wrapper type for a primitive, e.g. java.lang.Integer for int
     */
    private String getWrapperType(Class<?> type) {
        if (Integer.TYPE.equals(type)) {
            return Integer.class.getCanonicalName().replace('.', '/');
        } else if (Boolean.TYPE.equals(type)) {
            return Boolean.class.getCanonicalName().replace('.', '/');
        } else if (Character.TYPE.equals(type)) {
            return Character.class.getCanonicalName().replace('.', '/');
        } else if (Byte.TYPE.equals(type)) {
            return Byte.class.getCanonicalName().replace('.', '/');
        } else if (Short.TYPE.equals(type)) {
            return Short.class.getCanonicalName().replace('.', '/');
        } else if (Float.TYPE.equals(type)) {
            return Float.class.getCanonicalName().replace('.', '/');
        } else if (Long.TYPE.equals(type)) {
            return Long.class.getCanonicalName().replace('.', '/');
        } else if (Double.TYPE.equals(type)) {
            return Double.class.getCanonicalName().replace('.', '/');
        } else if (Void.TYPE.equals(type)) {
            return Void.class.getCanonicalName().replace('.', '/');
        }

        throw new IllegalStateException("Type: " + type.getCanonicalName() + " is not a primitive type");
    }

    /**
     * Returns the appropriate bytecode instruction to load a value from a variable to the stack
     *
     * @param type Type to load
     * @return Bytecode instruction to use
     */
    private int getVarInsn(Class<?> type) {
        if (type.isPrimitive()) {
            if (Integer.TYPE.equals(type)) {
                return ILOAD;
            } else if (Boolean.TYPE.equals(type)) {
                return ILOAD;
            } else if (Character.TYPE.equals(type)) {
                return ILOAD;
            } else if (Byte.TYPE.equals(type)) {
                return ILOAD;
            } else if (Short.TYPE.equals(type)) {
                return ILOAD;
            } else if (Float.TYPE.equals(type)) {
                return FLOAD;
            } else if (Long.TYPE.equals(type)) {
                return LLOAD;
            } else if (Double.TYPE.equals(type)) {
                return DLOAD;
            }
        }

        throw new IllegalStateException("Type: " + type.getCanonicalName() + " is not a primitive type");
    }

    /**
     * Invokes the most appropriate bytecode instruction to put a number on the stack
     *
     * @param mv
     * @param i
     */
    private void pushIntOntoStack(final MethodVisitor mv, final int i) {
        if (i == 0) {
            mv.visitInsn(ICONST_0);
        } else if (i == 1) {
            mv.visitInsn(ICONST_1);
        } else if (i == 2) {
            mv.visitInsn(ICONST_2);
        } else if (i == 3) {
            mv.visitInsn(ICONST_3);
        } else if (i == 4) {
            mv.visitInsn(ICONST_4);
        } else if (i == 5) {
            mv.visitInsn(ICONST_5);
        } else if (i > 5 && i <= 255) {
            mv.visitIntInsn(BIPUSH, i);
        } else {
            mv.visitIntInsn(SIPUSH, i);
        }
    }

    /**
     * Gets the appropriate bytecode instruction for RETURN, according to what type we need to return
     *
     * @param type Type the needs to be returned
     * @return The matching bytecode instruction
     */
    private int getReturnInsn(final Class<?> type) {
        if (type.isPrimitive()) {
            if (Void.TYPE.equals(type)) {
                return RETURN;
            }
            if (Integer.TYPE.equals(type)) {
                return IRETURN;
            } else if (Boolean.TYPE.equals(type)) {
                return IRETURN;
            } else if (Character.TYPE.equals(type)) {
                return IRETURN;
            } else if (Byte.TYPE.equals(type)) {
                return IRETURN;
            } else if (Short.TYPE.equals(type)) {
                return IRETURN;
            } else if (Float.TYPE.equals(type)) {
                return FRETURN;
            } else if (Long.TYPE.equals(type)) {
                return LRETURN;
            } else if (Double.TYPE.equals(type)) {
                return DRETURN;
            }
        }
        return ARETURN;
    }

    /**
     * Gets the string to use for CHECKCAST instruction, returning the correct value for any type, including primitives and arrays
     *
     * @param returnType The type to cast to with CHECKCAST
     * @return CHECKCAST parameter
     */
    private String getCastType(Class<?> returnType) {
        if (returnType.isPrimitive()) {
            return getWrapperType(returnType);
        } else {
            return Type.getInternalName(returnType);
        }
    }

    /**
     * Returns the name of the Java method to call to get the primitive value from an Object - e.g. intValue for java.lang.Integer
     *
     * @param type Type whose primitive method we want to lookup
     * @return The name of the method to use
     */
    private String getPrimitiveMethod(final Class<?> type) {
        if (Integer.TYPE.equals(type)) {
            return "intValue";
        } else if (Boolean.TYPE.equals(type)) {
            return "booleanValue";
        } else if (Character.TYPE.equals(type)) {
            return "charValue";
        } else if (Byte.TYPE.equals(type)) {
            return "byteValue";
        } else if (Short.TYPE.equals(type)) {
            return "shortValue";
        } else if (Float.TYPE.equals(type)) {
            return "floatValue";
        } else if (Long.TYPE.equals(type)) {
            return "longValue";
        } else if (Double.TYPE.equals(type)) {
            return "doubleValue";
        }

        throw new IllegalStateException("Type: " + type.getCanonicalName() + " is not a primitive type");
    }

    private void generateReturn(final MethodVisitor mv, final Method delegatedMethod) {
        final Class<?> returnType = delegatedMethod.getReturnType();
        mv.visitInsn(getReturnInsn(returnType));
    }

    /**
     * Create an Object[] parameter which contains all the parameters of the currently invoked method
     * and store this array for use in the call stack.
     *
     * @param mv
     * @param parameterTypes
     */
    private void pushMethodParameterArray(MethodVisitor mv, Class<?>[] parameterTypes) {
        // need to construct the array of objects passed in
        // create the Object[]
        createArrayDefinition(mv, parameterTypes.length, Object.class);

        int index = 1;
        for (int i = 0; i < parameterTypes.length; i++) {
            // keep copy of array on stack
            mv.visitInsn(DUP);

            final Class<?> parameterType = parameterTypes[i];
            pushIntOntoStack(mv, i);

            if (parameterType.isPrimitive()) {
                final String wrapperType = getWrapperType(parameterType);
                mv.visitVarInsn(getVarInsn(parameterType), index);
                mv.visitMethodInsn(INVOKESTATIC, wrapperType, "valueOf",
                        "(" + Type.getDescriptor(parameterType) + ")L" + wrapperType + ";", false);
                mv.visitInsn(AASTORE);

                if (Long.TYPE.equals(parameterType) || Double.TYPE.equals(parameterType)) {
                    index += 2;
                } else {
                    index++;
                }
            } else {
                mv.visitVarInsn(ALOAD, index);
                mv.visitInsn(AASTORE);
                index++;
            }
        }
    }

    private void createArrayDefinition(final MethodVisitor mv, final int size, final Class<?> type) {
        if (size < 0) {
            throw new IllegalStateException("Array size cannot be less than zero");
        }
        pushIntOntoStack(mv, size);
        mv.visitTypeInsn(ANEWARRAY, type.getCanonicalName().replace('.', '/'));
    }


    private static class VersionVisitor extends ClassVisitor {
        private int version;

        private VersionVisitor() {
            super(ASM7);
        }

        @Override
        public void visit(final int version, final int access, final String name,
                          final String signature, final String superName, final String[] interfaces) {
            this.version = version;
        }
    }

    public interface InterceptorHandler {
        Object invoke(Method method, Object[] args) throws Exception;
    }
}
