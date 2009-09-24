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
package org.apache.felix.webconsole.plugins.event.internal;


import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.*;
import org.osgi.service.cm.ManagedService;


public class ConfigurationListener implements ManagedService
{
    private final PluginServlet plugin;

    private final String pid;

    static ServiceRegistration create( final BundleContext context, final PluginServlet plugin )
    {
        ConfigurationListener cl = new ConfigurationListener( plugin );

        Dictionary props = new Hashtable();
        props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );
        props.put( Constants.SERVICE_DESCRIPTION, "Event plugin for the Felix Web Console Configuration Receiver" );
        props.put( Constants.SERVICE_PID, cl.pid );

        return context.registerService( ManagedService.class.getName(), cl, props );
    }


    private ConfigurationListener( PluginServlet plugin )
    {
        this.plugin = plugin;
        this.pid = plugin.getClass().getName();
    }

    //---------- ManagedService

    public void updated( Dictionary config )
    {
        plugin.updateConfiguration( config );
    }
}