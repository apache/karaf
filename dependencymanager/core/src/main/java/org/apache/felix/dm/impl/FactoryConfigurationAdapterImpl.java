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
package org.apache.felix.dm.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.service.Service;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * Creates, updates, or removes a service, when a ConfigAdmin factory configuration is created/updated or deleted.
 */
public class FactoryConfigurationAdapterImpl extends AbstractDecorator implements ManagedServiceFactory
{
    // The Adapter Service (we need to inherit all its dependencies).
    protected volatile Service m_service;

    // Our injected dependency manager
    protected volatile DependencyManager m_dm;
    
    // Our adapter implementation (either a Class, or an Object instance)
    protected final Object m_adapterImplementation;

    // Our adapter interface(s) (either null, a String, or a String array)
    protected final Object m_adapterInterface;
    
    // Our adapter service properties (may be null)
    protected final Dictionary m_adapterProperties;
    
    // Our Managed Service Factory PID
    protected String m_factoryPid;
    
    // The adapter "update" method used to provide the configuration
    protected String m_update;

    // Tells if the CM config must be propagated along with the adapter service properties
    protected boolean m_propagate;

    /**
     * Creates a new CM factory configuration adapter.
     * 
     * @param factoryPid
     * @param updateMethod
     * @param adapterInterface
     * @param adapterImplementation
     * @param adapterProperties
     * @param propagate
     */
    public FactoryConfigurationAdapterImpl(String factoryPid, String updateMethod, Object adapterImplementation, Object adapterInterface, Dictionary adapterProperties, boolean propagate)
    {
        m_factoryPid = factoryPid;
        m_update = updateMethod;
        m_adapterImplementation = adapterImplementation;
        m_adapterInterface = adapterInterface;
        m_adapterProperties = adapterProperties;
        m_propagate = propagate;
    }

    /**
     * Returns the managed service factory name.
     */
    public String getName()
    {
        return m_factoryPid;
    }
  
    /**
     * Method called from our superclass, when we need to create a service.
     */
    public Service createService(Object[] properties) {
        Dictionary settings = (Dictionary) properties[0];     
        Service newService = m_dm.createService();        
        Object impl;
        
        try {
            impl = (m_adapterImplementation instanceof Class) ? 
                ((Class) m_adapterImplementation).newInstance() : m_adapterImplementation;
            InvocationUtil.invokeCallbackMethod(impl, m_update, 
                new Class[][] {{ Dictionary.class }, {}}, 
                new Object[][] {{ settings }, {}});
        }
        
        catch (InvocationTargetException e)
        {
            // Our super class will check if the target exception is itself a ConfigurationException.
            // In this case, it will simply re-thrown.
            throw new RuntimeException(e.getTargetException());
        }
        
        catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }
        }

        // Merge adapter service properties, with CM settings 
        Dictionary serviceProperties = m_propagate ? mergeSettings(m_adapterProperties, settings) : m_adapterProperties;
   
        if (m_adapterInterface instanceof String)
        {
            newService.setInterface((String) m_adapterInterface, serviceProperties);
        }
        else if (m_adapterInterface instanceof String[])
        {
            newService.setInterface((String[]) m_adapterInterface, serviceProperties);
        }

        newService.setImplementation(impl);
        List dependencies = m_service.getDependencies();
        newService.add(dependencies);
        return newService;
    }

    /**
     * Method called from our superclass, when we need to update a Service, because 
     * the configuration has changed.
     */
    public void updateService(Object[] properties) 
    {
        Dictionary settings = (Dictionary) properties[0];
        Service service = (Service) properties[1];
        Object impl = service.getService();
       
        try
        {
            InvocationUtil.invokeCallbackMethod(impl, m_update, 
                new Class[][] {{ Dictionary.class }, {}}, 
                new Object[][] {{ settings }, {}});
            if (m_adapterInterface != null && m_propagate == true) {
                settings = mergeSettings(m_adapterProperties, settings);
                service.setServiceProperties(settings);
            }
        }
        
        catch (InvocationTargetException e)
        {
            // Our super class will check if the target exception is itself a ConfigurationException.
            // In this case, it will simply re-thrown.
            throw new RuntimeException(e.getTargetException());
        }
        
        catch (Throwable t) {
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }
        }
    }   

    /**
     * Merge CM factory configuration setting with the adapter service properties. The private CM factory configuration 
     * settings are ignored. A CM factory configuration property is private if its name starts with a dot (".").
     * 
     * @param adapterProperties
     * @param settings
     * @return
     */
    private Dictionary mergeSettings(Dictionary adapterProperties, Dictionary settings)
    {
        Dictionary props = new Hashtable();
        
        if (adapterProperties != null) {
            Enumeration keys = adapterProperties.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object val = adapterProperties.get(key);
                props.put(key, val);
            }
        }
        
        Enumeration keys = settings.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (! key.toString().startsWith(".")) {
                // public properties are propagated
                Object val = settings.get(key);
                props.put(key, val);
            }
        }
        return props;
    }
}
