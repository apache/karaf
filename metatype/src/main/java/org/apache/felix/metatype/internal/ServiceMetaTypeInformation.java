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
package org.apache.felix.metatype.internal;


import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeProvider;


/**
 * The <code>ServiceMetaTypeInformation</code> extends the
 * {@link MetaTypeInformationImpl} adding support to register and unregister
 * <code>ManagedService</code>s and <code>ManagedServiceFactory</code>s
 * also implementing the <code>MetaTypeProvider</code> interface.
 *
 * @author fmeschbe
 */
public class ServiceMetaTypeInformation extends MetaTypeInformationImpl implements ServiceListener
{

    /**
     * The filter specification to find <code>ManagedService</code>s and
     * <code>ManagedServiceFactory</code>s as well as to register a service
     * listener for those services (value is
     * "(|(objectClass=org.osgi.service.cm.ManagedService)(objectClass=org.osgi.service.cm.ManagedServiceFactory))").
     * We use the hard coded class name here to not create a dependency on the
     * ConfigurationAdmin service, which may not be available.
     */
    private static final String FILTER = "(|(objectClass=org.osgi.service.cm.ManagedService)(objectClass=org.osgi.service.cm.ManagedServiceFactory))";

    /**
     * The <code>BundleContext</code> used to get and unget services which
     * have to be registered and unregistered with the base class.
     */
    private final BundleContext bundleContext;


    /**
     * Creates an instance of this class handling services of the given
     * <code>bundle</code>.
     *
     * @param bundleContext The <code>BundleContext</code> used to get and
     *            unget services.
     * @param bundle The <code>Bundle</code> whose services are handled by
     *            this class.
     */
    public ServiceMetaTypeInformation( BundleContext bundleContext, Bundle bundle )
    {
        super( bundle );

        this.bundleContext = bundleContext;

        // register for service events for the bundle
        try
        {
            bundleContext.addServiceListener( this, FILTER );
        }
        catch ( InvalidSyntaxException ise )
        {
            Activator.log( LogService.LOG_ERROR, "ServiceMetaTypeInformation: Cannot register for service events", ise );
        }

        // prepare the filter to select existing services
        Filter filter;
        try
        {
            filter = bundleContext.createFilter( FILTER );
        }
        catch ( InvalidSyntaxException ise )
        {
            Activator.log( LogService.LOG_ERROR, "ServiceMetaTypeInformation: Cannot create filter '" + FILTER + "'",
                ise );
            return;
        }

        // add current services of the bundle
        ServiceReference[] sr = bundle.getRegisteredServices();
        if ( sr != null )
        {
            for ( int i = 0; i < sr.length; i++ )
            {
                if ( filter.match( sr[i] ) )
                {
                    addService( sr[i] );
                }
            }
        }
    }


    // ---------- ServiceListener ----------------------------------------------

    /**
     * Handles service registration and unregistration events ignoring all
     * services not belonging to the <code>Bundle</code> which is handled by
     * this instance.
     *
     * @param event The <code>ServiceEvent</code>
     */
    public void serviceChanged( ServiceEvent event )
    {
        // only care for services of our bundle
        if ( !getBundle().equals( event.getServiceReference().getBundle() ) )
        {
            return;
        }

        if ( event.getType() == ServiceEvent.REGISTERED )
        {
            addService( event.getServiceReference() );
        }
        else if ( event.getType() == ServiceEvent.UNREGISTERING )
        {
            removeService( event.getServiceReference() );
        }
    }


    /**
     * Registers the service described by the <code>serviceRef</code> with
     * this instance if the service is a <code>MetaTypeProvider</code>
     * instance and either a <code>service.factoryPid</code> or
     * <code>service.pid</code> property is set in the service registration
     * properties.
     * <p>
     * If the service is registered, this bundle keeps a reference, which is
     * ungot when the service is unregistered or this bundle is stopped.
     *
     * @param serviceRef The <code>ServiceReference</code> describing the
     *            service to be checked and handled.
     */
    protected void addService( ServiceReference serviceRef )
    {
        Object srv = bundleContext.getService( serviceRef );

        boolean ungetService = true;

        if ( srv instanceof MetaTypeProvider )
        {
            MetaTypeProvider mtp = ( MetaTypeProvider ) srv;

            // 1. check for a service factory PID
            String factoryPid = ( String ) serviceRef.getProperty( SERVICE_FACTORYPID );
            if ( factoryPid != null )
            {
                addFactoryPids( new String[]
                    { factoryPid } );
                addMetaTypeProvider( factoryPid, mtp );
                ungetService = false;
            }
            else
            {
                // 2. check for a service PID
                String pid = ( String ) serviceRef.getProperty( Constants.SERVICE_PID );
                if ( pid != null )
                {
                    addPids( new String[]
                        { pid } );
                    addMetaTypeProvider( pid, mtp );
                    ungetService = false;
                }
            }
        }

        if ( ungetService )
        {
            bundleContext.ungetService( serviceRef );
        }
    }


    /**
     * Unregisters the service described by the <code>serviceRef</code> from
     * this instance. Unregistration just checks for the
     * <code>service.factoryPid</code> and <code>service.pid</code> service
     * properties but does not care whether the service implements the
     * <code>MetaTypeProvider</code> interface. If the service is registered
     * it is simply unregistered.
     * <p>
     * If the service is actually unregistered the reference retrieved by the
     * registration method is ungotten.
     *
     * @param serviceRef The <code>ServiceReference</code> describing the
     *            service to be unregistered.
     */
    protected void removeService( ServiceReference serviceRef )
    {
        boolean ungetService = false;

        // 1. check for a service factory PID
        String factoryPid = ( String ) serviceRef.getProperty( SERVICE_FACTORYPID );
        if ( factoryPid != null )
        {
            ungetService = removeMetaTypeProvider( factoryPid ) != null;
            removeFactoryPid( factoryPid );
        }
        else
        {
            // 2. check for a service PID
            String pid = ( String ) serviceRef.getProperty( Constants.SERVICE_PID );
            if ( pid != null )
            {
                ungetService = removeMetaTypeProvider( pid ) != null;
                removePid( pid );
            }
        }

        // 3. drop the service reference
        if ( ungetService )
        {
            bundleContext.ungetService( serviceRef );
        }
    }
}
