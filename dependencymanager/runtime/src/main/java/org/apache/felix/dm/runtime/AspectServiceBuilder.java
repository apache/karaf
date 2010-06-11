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
import org.apache.felix.dm.service.Service;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

public class AspectServiceBuilder extends ServiceComponentBuilder
{
    private final static String TYPE = "AspectService";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public void buildService(MetaData srvMeta, List<MetaData> depsMeta, Bundle b, DependencyManager dm) 
        throws Exception
    {
        Log.instance().log(LogService.LOG_INFO,
                           "AspectServiceBuilder: building aspect service: %s with dependencies %s", 
                           srvMeta, depsMeta);

        Class<?> serviceInterface = b.loadClass(srvMeta.getString(Params.service));
        String serviceFilter = srvMeta.getString(Params.filter, null);
        Dictionary<String, Object> aspectProperties = srvMeta.getDictionary(Params.properties, null);
        int ranking = srvMeta.getInt(Params.ranking, 1);
        String implClass = srvMeta.getString(Params.impl);
        Object impl = b.loadClass(implClass);        
        String field = srvMeta.getString(Params.field, null);        
        Service service = dm.createAspectService(serviceInterface, serviceFilter, ranking, field)
                            .setImplementation(impl)
                            .setServiceProperties(aspectProperties);
        service.setComposition(srvMeta.getString(Params.composition, null));
        ServiceLifecycleHandler lfcleHandler = new ServiceLifecycleHandler(service, b, dm, srvMeta, depsMeta);
        // The dependencies will be plugged by our lifecycle handler.
        service.setCallbacks(lfcleHandler, "init", "start", "stop", "destroy");
        dm.add(service);
    }
}
