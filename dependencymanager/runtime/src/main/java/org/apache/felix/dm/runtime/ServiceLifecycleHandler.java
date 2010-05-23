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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.dependencies.Dependency;
import org.apache.felix.dm.service.Service;
import org.osgi.framework.Bundle;

/**
 * This class acts as a service implementation lifecycle handler. If the actual service implementation
 * init method returns a Map, then this map will is inspected for dependency filters, which will be 
 * applied to the dependencies specified in the service.
 */
public class ServiceLifecycleHandler
{
    private String m_init;
    private String m_start;
    private String m_stop;
    private String m_destroy;
    private List<MetaData> m_dependencies = new ArrayList<MetaData>();
    private Map<String, MetaData> m_namedDependencies = new HashMap<String, MetaData>();
    private Bundle m_bundle;

    public ServiceLifecycleHandler(Service srv, Bundle srvBundle, DependencyManager dm,
                                   MetaData srvMetaData, List<MetaData> srvDep)
        throws Exception
    {
        m_init = srvMetaData.getString(Params.init, null);
        m_start = srvMetaData.getString(Params.start, null);
        m_stop = srvMetaData.getString(Params.stop, null);
        m_destroy = srvMetaData.getString(Params.destroy, null);
        m_bundle = srvBundle;

        String confDependency = DependencyBuilder.DependencyType.ConfigurationDependency.toString();
        for (MetaData depMeta: srvDep)
        {
            if (depMeta.getString(Params.type).equals(confDependency))
            {
                // Register Configuration dependencies now
                Dependency dp = new DependencyBuilder(depMeta).build(m_bundle, dm);
                srv.add(dp);
            }
            else
            {
                String name = depMeta.getString(Params.name, null);
                if (name != null)
                {
                    m_namedDependencies.put(name, depMeta);
                }
                else
                {
                    m_dependencies.add(depMeta);
                }
            }
        }

    }

    @SuppressWarnings("unchecked")
    public void init(Object serviceInstance, DependencyManager dm, Service service)
        throws Exception
    {
        // Invoke the service instance actual init method.
        Object o = invokeMethod(serviceInstance, m_init, dm, service);

        // If the init method returned a Map, then apply all filters found from it into
        // the service dependencies. Keys = Dependency name / values = Dependency filter
        if (o != null && Map.class.isAssignableFrom(o.getClass()))
        {
            Map<String, String> filters = (Map<String, String>) o;
            for (Map.Entry<String, String> entry: filters.entrySet())
            {
                String name = entry.getKey();
                String filter = entry.getValue();

                MetaData dependency = m_namedDependencies.remove(name);
                if (dependency != null)
                {
                    dependency.setString(Params.filter, filter);
                    DependencyBuilder depBuilder = new DependencyBuilder(dependency);
                    Dependency d = depBuilder.build(m_bundle, dm, true);
                    service.add(d);
                }
                else
                {
                    throw new IllegalArgumentException("Invalid filter Map: the filter key " + name
                        + " does not correspond " +
                        " to a known Dependency.");
                }
            }
        }

        plugDependencies(m_namedDependencies.values(), dm, service);
        plugDependencies(m_dependencies, dm, service);
    }

    public void start(Object serviceInstance, DependencyManager dm, Service service)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
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

    private void plugDependencies(Collection<MetaData> dependencies, DependencyManager dm, Service service)
        throws Exception
    {
        for (MetaData dependency: dependencies)
        {
            DependencyBuilder depBuilder = new DependencyBuilder(dependency);
            Dependency d = depBuilder.build(m_bundle, dm, true);
            service.add(d);
        }
    }
}
