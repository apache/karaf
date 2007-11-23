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
package org.apache.felix.scr.impl;

import org.osgi.service.log.LogService;


/**
 * This factory allows other types of ComponentManagers to be provided.
 * 
 * 
 */
public class ManagerFactory
{

    static ComponentManager createManager( BundleComponentActivator activator, ComponentMetadata metadata,
        long componentId )
    {
        activator.log( LogService.LOG_DEBUG, "ManagerFactory.createManager", metadata, null );
        if ( metadata.isImmediate() )
        {
            return new ImmediateComponentManager( activator, metadata, componentId );
        }
        else if ( metadata.getServiceMetadata() != null )
        {
            if ( metadata.getServiceMetadata().isServiceFactory() )
            {
                return new ServiceFactoryComponentManager( activator, metadata, componentId );
            }

            return new DelayedComponentManager( activator, metadata, componentId );
        }

        // if we get here, which is not expected after all, we fail
        throw new IllegalArgumentException( "Cannot create a component manager for " + metadata.getName() );
    }
}