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
package org.apache.felix.cm.integration.helper;


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;


/**
 * The <code>ManagedServiceThread</code> class is a ManagedService and extends
 * the {@link TestThread} for use in the
 * {@link org.apache.felix.cm.integration.ConfigUpdateStressTest}.
 */
public class ManagedServiceThread extends TestThread implements ManagedService
{

    private final BundleContext bundleContext;

    private final Hashtable<String, Object> serviceProps;

    private ServiceRegistration service;

    private final ArrayList<Dictionary> configs;

    private boolean configured;


    public ManagedServiceThread( final BundleContext bundleContext, final String pid )
    {
        Hashtable<String, Object> serviceProps = new Hashtable<String, Object>();
        serviceProps.put( Constants.SERVICE_PID, pid );

        this.bundleContext = bundleContext;
        this.serviceProps = serviceProps;
        this.configs = new ArrayList<Dictionary>();
    }


    public ArrayList<Dictionary> getConfigs()
    {
        synchronized ( configs )
        {
            return new ArrayList<Dictionary>( configs );
        }
    }


    public boolean isConfigured()
    {
        return configured;
    }


    @Override
    public void doRun()
    {
        service = bundleContext.registerService( ManagedService.class.getName(), this, serviceProps );
    }


    @Override
    public void cleanup()
    {
        if ( service != null )
        {
            service.unregister();
            service = null;
        }
    }


    public void updated( Dictionary properties )
    {
        synchronized ( configs )
        {
            configs.add( properties );
            configured = properties != null;
        }
    }
}