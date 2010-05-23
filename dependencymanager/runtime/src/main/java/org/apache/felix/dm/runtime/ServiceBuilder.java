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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.dependencies.Dependency;
import org.apache.felix.dm.service.Service;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

public class ServiceBuilder extends ServiceComponentBuilder
{
    private final static String TYPE = "Service";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public void buildService(MetaData srvMeta, List<MetaData> srvDeps, Bundle b, DependencyManager dm)
        throws Exception
    {
        Log.instance().log(LogService.LOG_DEBUG, "building service: service metadata=" + srvMeta
            + ", dependencyMetaData=" + srvDeps);

        // Get service parameters (lifecycle callbacks, composition, factory, etc ...)

        Service service = dm.createService();
        String factory = srvMeta.getString(Params.factory, null);
        String factoryConfigure = srvMeta.getString(Params.factoryConfigure, null);
        String impl = srvMeta.getString(Params.impl);
        String init = srvMeta.getString(Params.init, null);
        String start = srvMeta.getString(Params.start, null);
        String stop = srvMeta.getString(Params.stop, null);
        String destroy = srvMeta.getString(Params.destroy, null);
        String composition = srvMeta.getString(Params.composition, null);
        Dictionary<String, Object> serviceProperties = srvMeta.getDictionary(Params.properties, null);
        String[] provide = srvMeta.getStrings(Params.provide, null);

        // Check if we must provide a Set Factory.
        if (factory == null)
        {
            // No: instantiate the service.
            service.setImplementation(b.loadClass(impl));
            if (composition != null)
            {
                service.setComposition(composition);
            }
            if (provide != null)
            {
                service.setInterface(provide, serviceProperties);
            }

            // Creates a ServiceHandler, which will filter all service lifecycle callbacks.
            /*
            ServiceLifecycleHandler lfcleHandler = new ServiceLifecycleHandler(service, b, dm, srvMeta,
                                                                               srvDeps);
            service.setCallbacks(lfcleHandler, "init", "start", "stop", "destroy");
            String confDependency = DependencyBuilder.DependencyType.ConfigurationDependency.toString();
            */
            // TODO REMOVE
            for (MetaData depMeta: srvDeps)
            {
                Dependency dp = new DependencyBuilder(depMeta).build(b, dm);
                service.add(dp);
            }
        }
        else
        {
            // We don't instantiate the service, but instead we provide a Set in the registry.
            // This Set will act as a factory and another component may use registers some
            // service configurations into it in order to fire some service instantiations.

            ServiceFactory serviceFactory =  new ServiceFactory(b.loadClass(impl), init, start, stop, destroy, 
                                                                composition, serviceProperties, provide, 
                                                                factoryConfigure);
            for (MetaData dependencyMetaData: srvDeps)
            {
                Dependency dp = new DependencyBuilder(dependencyMetaData).build(b, dm);
                serviceFactory.addDependency(dp);
            }
            service.setImplementation(serviceFactory);
            service.setCallbacks(null, "start", "stop", null);
            Hashtable<String, String> props = new Hashtable<String, String>();
            props.put("dm.factory.name", factory);
            service.setInterface(Set.class.getName(), props);
        }

        dm.add(service);
    }
}
