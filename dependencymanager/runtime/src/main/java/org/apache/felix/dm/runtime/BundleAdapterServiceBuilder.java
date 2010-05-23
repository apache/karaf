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
import java.util.List;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.dependencies.Dependency;
import org.apache.felix.dm.service.Service;
import org.osgi.framework.Bundle;

public class BundleAdapterServiceBuilder extends ServiceComponentBuilder
{
    private final static String TYPE = "BundleAdapterService";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public void buildService(MetaData serviceMetaData,
        List<MetaData> serviceDependencies,
        Bundle b, DependencyManager dm) throws Exception
    {
        int stateMask = serviceMetaData.getInt(Params.stateMask, Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE);
        String filter = serviceMetaData.getString(Params.filter, null);
        Class<?> adapterImpl = b.loadClass(serviceMetaData.getString(Params.impl));
        String[] service = serviceMetaData.getStrings(Params.service, null);
        Dictionary<String, Object> properties = serviceMetaData.getDictionary(Params.properties, null);
        boolean propagate = "true".equals(serviceMetaData.getString(Params.propagate, "false"));
        Service srv = dm.createBundleAdapterService(stateMask, filter, adapterImpl, service, properties, propagate);  
        setCommonServiceParams(srv, serviceMetaData);
        for (MetaData dependencyMetaData: serviceDependencies)
        {
            Dependency dp = new DependencyBuilder(dependencyMetaData).build(b, dm);
            srv.add(dp);
        }
        dm.add(srv);
    }    
}
