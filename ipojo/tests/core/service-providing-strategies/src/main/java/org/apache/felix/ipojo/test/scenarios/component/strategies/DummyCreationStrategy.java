package org.apache.felix.ipojo.test.scenarios.component.strategies;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.IPOJOServiceFactory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.handlers.providedservice.CreationStrategy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class DummyCreationStrategy extends CreationStrategy implements InvocationHandler {

    /**
     * Map [ComponentInstance->ServiceObject] storing created service objects.
     */
    private Map/*<ComponentInstance, ServiceObject>*/ m_instances = new HashMap();
    
    private InstanceManager m_manager;

    private String[] m_specs;

    /**
     * A method is invoked on the proxy object.
     * If the method is the {@link IPOJOServiceFactory#getService(ComponentInstance)} 
     * method, this method creates a service object if no already created for the asking
     * component instance. 
     * If the method is {@link IPOJOServiceFactory#ungetService(ComponentInstance, Object)}
     * the service object is unget (i.e. removed from the map and deleted).
     * In all other cases, a {@link UnsupportedOperationException} is thrown as this policy
     * requires to use  the {@link IPOJOServiceFactory} interaction pattern.  
     * @param arg0 the proxy object
     * @param arg1 the called method
     * @param arg2 the arguments
     * @return the service object attached to the asking instance for 'get', 
     * <code>null</code> for 'unget',
     * a {@link UnsupportedOperationException} for all other methods.
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object arg0, Method arg1, Object[] arg2) {
        if (isGetServiceMethod(arg1)) {
            return getService((ComponentInstance) arg2[0]);
        }
        
        if (isUngetServiceMethod(arg1)) {
            ungetService((ComponentInstance) arg2[0], arg2[1]);
            return null;
        }
        
        throw new UnsupportedOperationException("This service requires an advanced creation policy. "
                + "Before calling the service, call the getService(ComponentInstance) method to get "
                + "the service object. " + arg1.getName()); // TODO DEBUG
    }

    /**
     * A service object is required.
     * This policy returns a service object per asking instance.
     * @param instance the instance requiring the service object
     * @return the service object for this instance
     * @see org.apache.felix.ipojo.IPOJOServiceFactory#getService(org.apache.felix.ipojo.ComponentInstance)
     */
    public Object getService(ComponentInstance instance) {
        Object obj = m_instances.get(instance);
        if (obj == null) {
            obj = m_manager.createPojoObject();
            m_instances.put(instance, obj);
        }
        return obj;
    }

    /**
     * A service object is unget.
     * The service object is removed from the map and deleted.
     * @param instance the instance releasing the service
     * @param svcObject the service object
     * @see org.apache.felix.ipojo.IPOJOServiceFactory#ungetService(org.apache.felix.ipojo.ComponentInstance, java.lang.Object)
     */
    public void ungetService(ComponentInstance instance, Object svcObject) {
        Object pojo = m_instances.remove(instance);
        m_manager.deletePojoObject(pojo);
    }

    /**
     * The service is going to be registered.
     * @param im the instance manager
     * @param interfaces the published interfaces
     * @param props the properties
     * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onPublication(org.osgi.framework.BundleContext, java.lang.String[], java.util.Properties)
     */
    public void onPublication(InstanceManager im, String[] interfaces,
            Properties props) {
        
        m_manager = im;
        m_specs = interfaces;
        
    }

    /**
     * The service is going to be unregistered.
     * The instance map is cleared. Created object are disposed.
     * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onUnpublication()
     */
    public void onUnpublication() {
        Collection col = m_instances.values();
        Iterator it = col.iterator();
        while (it.hasNext()) {
            m_manager.deletePojoObject(it.next());
        }
        m_instances.clear();
    }

    /**
     * OSGi Service Factory getService method.
     * @param arg0 the asking bundle
     * @param arg1 the service registration
     * @return a proxy implementing the {@link IPOJOServiceFactory}
     * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
     */
    public Object getService(Bundle arg0, ServiceRegistration arg1) {
        Object proxy = Proxy.newProxyInstance(m_manager.getClazz().getClassLoader(), 
                getSpecificationsWithIPOJOServiceFactory(m_specs, m_manager.getContext()), this);
        return proxy;
    }

    /**
     * OSGi Service factory unget method.
     * Does nothing.
     * @param arg0 the asking bundle
     * @param arg1 the service registration
     * @param arg2 the service object created for this bundle.
     * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
     */
    public void ungetService(Bundle arg0, ServiceRegistration arg1,
            Object arg2) { }
    
    /**
     * Utility method returning the class array of provided service
     * specification and the {@link IPOJOServiceFactory} interface.
     * @param specs the published service interface
     * @param bc the bundle context, used to load classes
     * @return the class array containing provided service specification and
     * the {@link IPOJOServiceFactory} class.
     */
    private Class[] getSpecificationsWithIPOJOServiceFactory(String[] specs, BundleContext bc) {
        Class[] classes = new Class[specs.length + 1];
        int i = 0;
        for (i = 0; i < specs.length; i++) {
            try {
                classes[i] = bc.getBundle().loadClass(specs[i]);
            } catch (ClassNotFoundException e) {
                // Should not happen.
            }
        }
        classes[i] = IPOJOServiceFactory.class;
        return classes;
    }
    
    
}

