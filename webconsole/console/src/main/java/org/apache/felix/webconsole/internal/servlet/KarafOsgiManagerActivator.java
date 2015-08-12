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
package org.apache.felix.webconsole.internal.servlet;


import java.util.Hashtable;

import org.apache.felix.webconsole.WebConsoleSecurityProvider;
import org.apache.felix.webconsole.WebConsoleSecurityProvider2;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;


/**
 * This is the main, starting class of the Bundle. It initializes and disposes
 * the Apache Web Console upon bundle lifecycle requests.
 */
public class KarafOsgiManagerActivator implements BundleActivator
{

    private ServiceRegistration registration;
    private KarafOsgiManager osgiManager;


    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start( BundleContext bundleContext )
    {
        Hashtable<String, String> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, "org.apache.karaf.webconsole");
        registration = bundleContext.registerService(
                new String[] {WebConsoleSecurityProvider.class.getName(), WebConsoleSecurityProvider2.class.getName(), ManagedService.class.getName() },
                new JaasSecurityProvider(),
                props
        );
        osgiManager = new KarafOsgiManager( bundleContext );
    }


    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop( BundleContext arg0 )
    {
        if ( osgiManager != null )
        {
            osgiManager.dispose();
        }
        if ( registration != null )
        {
            registration.unregister();
        }
    }

}
