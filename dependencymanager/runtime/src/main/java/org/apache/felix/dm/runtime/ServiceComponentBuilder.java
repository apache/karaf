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

import java.util.List;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.service.Service;
import org.osgi.framework.Bundle;

/**
 * The builder which creates a DependencyManager Service (a Service and all its derived classes (Aspect/Adapters).
 */
public abstract class ServiceComponentBuilder
{
    /**
     * Returns the service component type.
     */
    abstract String getType();

    /**
     * Builds the service component.
     * @param serviceMetaData the service component metadata parsed from the descriptor file.
     * @param serviceDependencies the service component dependencies metadata parsed from the descriptor file.
     */
    abstract void buildService(MetaData serviceMetaData, List<MetaData> serviceDependencies, Bundle b,
        DependencyManager dm)
        throws Exception;

    /**
     * Set common Service parameters, if provided from our Component descriptor
     */
    protected void setCommonServiceParams(Service service, MetaData serviceMetaData)
        throws Exception
    {
        String init = serviceMetaData.getString(Params.init, null);
        String start = serviceMetaData.getString(Params.start, null);
        String stop = serviceMetaData.getString(Params.stop, null);
        String destroy = serviceMetaData.getString(Params.destroy, null);
        service.setCallbacks(init, start, stop, destroy);
        String composition = serviceMetaData.getString(Params.composition, null);
        if (composition != null)
        {
            service.setComposition(composition);
        }
    }
}
