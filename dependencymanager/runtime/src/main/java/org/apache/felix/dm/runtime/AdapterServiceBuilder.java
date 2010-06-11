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

/**
 * Builded called when the JSON parser find an adapter service descriptor.
 */
public class AdapterServiceBuilder extends ServiceComponentBuilder
{
    /** The type attribute specified in the JSON descriptor */
    private final static String TYPE = "AdapterService";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public void buildService(MetaData srvMeta, List<MetaData> depsMeta, Bundle b, DependencyManager dm)
        throws Exception
    {
        Class<?> adapterImpl = b.loadClass(srvMeta.getString(Params.impl));
        String[] adapterService = srvMeta.getStrings(Params.adapterService, null);
        Dictionary<String, Object> adapterProperties = srvMeta.getDictionary(Params.adapterProperties, null);
        Class<?> adapteeService = b.loadClass(srvMeta.getString(Params.adapteeService));
        String adapteeFilter = srvMeta.getString(Params.adapteeFilter, null);     
        Service service = dm.createAdapterService(adapteeService, adapteeFilter)
                            .setInterface(adapterService, adapterProperties)
                            .setImplementation(adapterImpl);
        service.setComposition(srvMeta.getString(Params.composition, null));
        ServiceLifecycleHandler lfcleHandler = new ServiceLifecycleHandler(service, b, dm, srvMeta, depsMeta);
        // The dependencies will be plugged by our lifecycle handler.
        service.setCallbacks(lfcleHandler, "init", "start", "stop", "destroy");
        dm.add(service);
    }
}
