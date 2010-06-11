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
package org.apache.felix.dm.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.dependencies.BundleDependency;
import org.apache.felix.dm.dependencies.ConfigurationDependency;
import org.apache.felix.dm.dependencies.Dependency;
import org.apache.felix.dm.dependencies.ResourceDependency;
import org.apache.felix.dm.dependencies.ServiceDependency;
import org.apache.felix.dm.dependencies.TemporalServiceDependency;
import org.apache.felix.dm.service.Service;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

/**
 * Allow Services to configure dynamically their dependency filters from their init() method.
 * Basically, this class acts as a service implementation lifecycle handler. When we detect that the Service is
 * called in its init() method, and if its init() method returns a Map, then the Map is assumed to contain
 * dependency filters, which will be applied to all service dependencies. The Map optionally returned by
 * Service's init method has to contains the following keys:
 * <ul>
 *   <li>name.filter: the value must be a valid OSGi filter. the "name" prefix must match a ServiceDependency 
 *   name attribute</li>
 *   <li>name.required: the value is a boolean ("true"|"false") and must the "name" prefix must match a 
 *   ServiceDependency name attribute</li>
 * </ul>
 * 
 * Example of a Service whose dependency filter is configured from ConfigAdmin:
 * 
 * <blockquote><pre>
 *  &#47;**
 *    * A Service whose service dependency filter/require attribute may be configured from ConfigAdmin
 *    *&#47;
 *  &#64;Service
 *  class X {
 *      private Dictionary m_config;
 *      
 *      &#64;ConfigurationDependency(pid="MyPid")
 *      void configure(Dictionary conf) {
 *           // Initialize our service from config ...
 *           
 *           // And store the config for later usage (from our init method)
 *           m_config = config;
 *      }
 *
 *      // The returned Map will be used to configure our "dependency1" Dependency.
 *      &#64;Init
 *      Map init() {
 *          return new HashMap() {{
 *              put("dependency1.filter", m_config.get("filter"));
 *              put("dependency1.required", m_config.get("required"));
 *          }};
 *      } 
 *      
 *      &#64;ServiceDependency(name="dependency1") 
 *      void bindOtherService(OtherService other) {
 *         // the filter and required flag will be configured from our init method.
 *      }
 *  }
 *  </pre></blockquote>
 */
public class ServiceLifecycleHandler
{
    private String m_init;
    private String m_start;
    private String m_stop;
    private String m_destroy;
    private MetaData m_srvMeta;
    private List<MetaData> m_depsMeta;
    private List<Dependency> m_deps = new ArrayList<Dependency>();
    private Bundle m_bundle;

    public ServiceLifecycleHandler(Service srv, Bundle srvBundle, DependencyManager dm,
                                   MetaData srvMeta, List<MetaData> depMeta)
        throws Exception
    {
        m_srvMeta = srvMeta;
        m_init = srvMeta.getString(Params.init, null);
        m_start = srvMeta.getString(Params.start, null);
        m_stop = srvMeta.getString(Params.stop, null);
        m_destroy = srvMeta.getString(Params.destroy, null);
        m_bundle = srvBundle;

        // Plug configuration dependencies now, and remove them from the dependency list.
        // (we want these dependencies to be injected before the init method).
        
        String confDependency = DependencyBuilder.DependencyType.ConfigurationDependency.toString();
        Iterator<MetaData> dependencies = depMeta.iterator();
        while (dependencies.hasNext()) 
        {
            MetaData dependency = dependencies.next();
            if (dependency.getString(Params.type).equals(confDependency))
            {
                // Register Configuration dependency now.
                Dependency dp = new DependencyBuilder(dependency).build(m_bundle, dm);
                srv.add(dp);
                dependencies.remove();
            }
        }
        
        m_depsMeta = depMeta;
    }

    @SuppressWarnings("unchecked")
    public void init(Object serviceInstance, DependencyManager dm, Service service)
        throws Exception
    {
        // Invoke the service instance init method, and check if it returns a dependency
        // customization map. This map will be used to configure some dependency filters
        // (or required flag).
        Object o = invokeMethod(serviceInstance, m_init, dm, service);      
        Map<String, String> customization = (o != null && Map.class.isAssignableFrom(o.getClass())) ?
            (Map<String, String>) o : new HashMap<String, String>();
       
        Log.instance().log(LogService.LOG_DEBUG,
                           "ServiceLifecycleHandler.init: invoked init method from service %s " +
                           ", returned map: %s", serviceInstance, customization); 
                                 
        for (MetaData dependency : m_depsMeta) 
        {
            // Check if this dependency has a name, and if we find the name from the 
            // customization map, then apply filters and required flag from the map into it.
            
            String name = dependency.getString(Params.name, null);
            if (name != null)
            {
                String filter = customization.get(name + ".filter");
                String required = customization.get(name + ".required");
                		
                if (filter != null || required != null) {
                    dependency = (MetaData) dependency.clone();
                    if (filter != null) 
                    {
                        dependency.setString(Params.filter, filter);
                    }
                    if (required != null)
                    {
                        dependency.setString(Params.required, required);
                    }
                }
            }
            DependencyBuilder depBuilder = new DependencyBuilder(dependency);
            Log.instance().log(LogService.LOG_INFO, 
                               "ServiceLifecycleHandler.init: adding dependency %s into service %s",
                               dependency, m_srvMeta);
            Dependency d = depBuilder.build(m_bundle, dm, true);
            m_deps.add(d);
            service.add(d);
        }
    }

    public void start(Object serviceInstance, DependencyManager dm, Service service)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        // Remove "instance bound" flag from all dependencies, because we want to be deactivated
        // once we lose one of the deps ...
        Iterator it = m_deps.iterator();
        while (it.hasNext())
        {
            Dependency d = (Dependency) it.next();
            if (d instanceof ServiceDependency) {
                ((ServiceDependency)d).setInstanceBound(false);
            } else if (d instanceof BundleDependency)
            {
                ((BundleDependency)d).setInstanceBound(false);
            } else  if (d instanceof ResourceDependency) 
            {
                ((ResourceDependency) d).setInstanceBound(false);
            } else if (d instanceof ConfigurationDependency) 
            {
                ((ConfigurationDependency) d).setInstanceBound(false);
            } else if (d instanceof TemporalServiceDependency) 
            {
                ((TemporalServiceDependency) d).setInstanceBound(false);
            }
        }
        invokeMethod(serviceInstance, m_start, dm, service);
    }

    public void stop(Object serviceInstance, DependencyManager dm, Service service)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        invokeMethod(serviceInstance, m_stop, dm, service);
    }

    public void destroy(Object serviceInstance, DependencyManager dm, Service service)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        invokeMethod(serviceInstance, m_destroy, dm, service);
    }

    private Object invokeMethod(Object serviceInstance, String method, DependencyManager dm, Service service)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        return invokeMethod(serviceInstance,
                            method,
                            new Class[][] {
                                    { Object.class, DependencyManager.class, Service.class },
                                    { DependencyManager.class, Service.class },
                                    { Object.class },
                                    {}
            },
                            new Object[][] {
                                    { serviceInstance, dm, service },
                                    { dm, service },
                                    { serviceInstance },
                                    {}
            });
    }

    private Object invokeMethod(Object instance, String method, Class<?>[][] signatures, Object[][] params)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        if (method == null)
        {
            // The annotated class did not provide an annotation for this lifecycle callback.
            return null;
        }

        Class clazz = instance.getClass();

        while (clazz != null)
        {
            for (int i = 0; i < signatures.length; i++)
            {
                Class<?>[] signature = signatures[i];
                try
                {
                    // Don't use getMethod because getMethod only look for public methods !
                    Method m = instance.getClass().getDeclaredMethod(method, signature);
                    m.setAccessible(true);
                    return m.invoke(instance, params[i]);
                }
                catch (NoSuchMethodException e)
                {
                    // ignore this and keep looking
                }
            }
            clazz = clazz.getSuperclass();
        }

        return null;
    }
}
