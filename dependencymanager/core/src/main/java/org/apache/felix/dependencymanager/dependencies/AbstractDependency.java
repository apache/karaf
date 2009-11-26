package org.apache.felix.dependencymanager.dependencies;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.felix.dependencymanager.Dependency;
import org.apache.felix.dependencymanager.impl.Logger;

public abstract class AbstractDependency implements Dependency {
    private boolean m_isRequired;
    protected final Logger m_logger;

    public AbstractDependency(Logger logger) {
        m_logger = logger;
    }

    public synchronized boolean isRequired() {
        return m_isRequired;
    }
    
    protected synchronized void setIsRequired(boolean isRequired) {
        m_isRequired = isRequired;
    }
    
    protected void invokeCallbackMethod(Object[] instances, String methodName, Class[][] signatures, Object[][] parameters) {
        for (int i = 0; i < instances.length; i++) {
            try {
                invokeCallbackMethod(instances[i], methodName, signatures, parameters);
            }
            catch (NoSuchMethodException e) {
                m_logger.log(Logger.LOG_DEBUG, "Method '" + methodName + "' does not exist on " + instances[i] + ". Callback skipped.");
            }
        }
    }

    protected void invokeCallbackMethod(Object instance, String methodName, Class[][] signatures, Object[][] parameters) throws NoSuchMethodException {
        Class currentClazz = instance.getClass();
        boolean done = false;
        while (!done && currentClazz != null) {
            done = invokeMethod(instance, currentClazz, methodName, signatures, parameters, false);
            if (!done) {
                currentClazz = currentClazz.getSuperclass();
            }
        }
        if (!done && currentClazz == null) {
            throw new NoSuchMethodException(methodName);
        }
    }

    protected boolean invokeMethod(Object object, Class clazz, String name, Class[][] signatures, Object[][] parameters, boolean isSuper) {
        Method m = null;
        for (int i = 0; i < signatures.length; i++) {
            Class[] signature = signatures[i];
            try {
                m = clazz.getDeclaredMethod(name, signature);
                if (!(isSuper && Modifier.isPrivate(m.getModifiers()))) {
                    m.setAccessible(true);
                    try {
                        m.invoke(object, parameters[i]);
                    }
                    catch (InvocationTargetException e) {
                        m_logger.log(Logger.LOG_ERROR, "Exception while invoking method " + m + ".", e);
                    }
                    // we did find and invoke the method, so we return true
                    return true;
                }
            }
            catch (NoSuchMethodException e) {
                // ignore this and keep looking
            }
            catch (Exception e) {
                // could not even try to invoke the method
                m_logger.log(Logger.LOG_ERROR, "Exception while trying to invoke method " + m + ".", e);
            }
        }
        return false;
    }
}
