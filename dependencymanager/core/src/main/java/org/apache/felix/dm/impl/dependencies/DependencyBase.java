package org.apache.felix.dm.impl.dependencies;

import org.apache.felix.dm.dependencies.Dependency;
import org.apache.felix.dm.impl.InvocationUtil;
import org.apache.felix.dm.impl.Logger;

public abstract class DependencyBase implements Dependency, DependencyActivation {
    private boolean m_isRequired;
    private boolean m_isInstanceBound;
    protected final Logger m_logger;

    public DependencyBase(Logger logger) {
        m_logger = logger;
    }

    public synchronized boolean isRequired() {
        return m_isRequired;
    }
    
    protected synchronized void setIsRequired(boolean isRequired) {
        m_isRequired = isRequired;
    }
    
    public final boolean isInstanceBound() {
        return m_isInstanceBound;
    }

    public final void setIsInstanceBound(boolean isInstanceBound) {
        m_isInstanceBound = isInstanceBound;
    }
    
    protected void invokeCallbackMethod(Object[] instances, String methodName, Class[][] signatures, Object[][] parameters) {
        for (int i = 0; i < instances.length; i++) {
        	try {
                InvocationUtil.invokeCallbackMethod(instances[i], methodName, signatures, parameters);
            }
            catch (NoSuchMethodException e) {
                // if the method does not exist, ignore it
            }
            catch (Exception e) {
                m_logger.log(Logger.LOG_WARNING, "Invocation of '" + methodName + "' failed.", e);
            }
        }
    }
}
