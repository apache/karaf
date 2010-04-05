package org.apache.felix.dm.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class InvocationUtil {
    public static void invokeCallbackMethod(Object instance, String methodName, Class[][] signatures, Object[][] parameters) throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Class currentClazz = instance.getClass();
        while (currentClazz != null) {
            try {
                invokeMethod(instance, currentClazz, methodName, signatures, parameters, false);
                return;
            }
            catch (NoSuchMethodException nsme) {
                // ignore
            }
            currentClazz = currentClazz.getSuperclass();
        }
        throw new NoSuchMethodException(methodName);
    }

    public static void invokeMethod(Object object, Class clazz, String name, Class[][] signatures, Object[][] parameters, boolean isSuper) throws NoSuchMethodException, InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        Method m = null;
        for (int i = 0; i < signatures.length; i++) {
            Class[] signature = signatures[i];
            try {
                m = clazz.getDeclaredMethod(name, signature);
                if (!(isSuper && Modifier.isPrivate(m.getModifiers()))) {
                    m.setAccessible(true);
                    m.invoke(object, parameters[i]);
                    return;
                }
            }
            catch (NoSuchMethodException e) {
                // ignore this and keep looking
            }
        }
        throw new NoSuchMethodException(name);
    }
}
