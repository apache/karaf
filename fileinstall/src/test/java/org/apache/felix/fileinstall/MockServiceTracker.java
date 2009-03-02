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
package org.apache.felix.fileinstall;


import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


/**
 * Mock class to simulate a ServiceTracker 
 * Use the public constructor to set the bundleContext and the 
 * service instance to be returned
 */
public class MockServiceTracker extends ServiceTracker
{
    private Object service;


    /**
     * Use this constructor
     * 
     * @param context - the bundle context
     * @param service - the service instance returned by getService()
     */
    public MockServiceTracker( BundleContext context, Object service )
    {
        super( context, service.getClass().getName(), null );
        this.service = service;
    }


    MockServiceTracker( BundleContext context, Filter filter, ServiceTrackerCustomizer customizer )
    {
        super( context, filter, customizer );
    }


    MockServiceTracker( BundleContext arg0, ServiceReference arg1, ServiceTrackerCustomizer arg2 )
    {
        super( arg0, arg1, arg2 );
    }


    MockServiceTracker( BundleContext arg0, String arg1, ServiceTrackerCustomizer arg2 )
    {
        super( arg0, arg1, arg2 );
    }


    public Object waitForService( long arg0 ) throws InterruptedException
    {
        return service;
    }


    public Object getService()
    {
        return service;
    }


    public Object getService( ServiceReference reference )
    {
        return getService();
    }

}
