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
package org.apache.felix.metatype;


import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;


/**
 * The <code>MockBundleContext</code> TODO
 *
 * @author fmeschbe
 * @version $Rev:$, $Date:$
 */
public class MockBundleContext implements BundleContext
{

    private Bundle theBundle;
    private Map services;

    private Set serviceListeners;


    public MockBundleContext( long bundleId, String bundleSymbolicName )
    {
        theBundle = new MockBundle( this, bundleId, bundleSymbolicName );
        services = new HashMap();
        serviceListeners = new HashSet();
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#addBundleListener(org.osgi.framework.BundleListener)
     */
    public void addBundleListener( BundleListener arg0 )
    {
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#addFrameworkListener(org.osgi.framework.FrameworkListener)
     */
    public void addFrameworkListener( FrameworkListener arg0 )
    {
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener)
     */
    public void addServiceListener( ServiceListener listener )
    {
        serviceListeners.add( listener );
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener, java.lang.String)
     */
    public void addServiceListener( ServiceListener listener, String filter )
    {
        serviceListeners.add( listener );
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#createFilter(java.lang.String)
     */
    public Filter createFilter( String arg0 )
    {
        return new Filter()
        {

            public boolean match( ServiceReference arg0 )
            {
                return true;
            }


            public boolean match( Dictionary arg0 )
            {
                return true;
            }


            public boolean matchCase( Dictionary arg0 )
            {
                return true;
            }

        };
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getAllServiceReferences(java.lang.String, java.lang.String)
     */
    public ServiceReference[] getAllServiceReferences( String arg0, String arg1 )
    {
        return null;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getBundle()
     */
    public Bundle getBundle()
    {
        return theBundle;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getBundle(long)
     */
    public Bundle getBundle( long bundleId )
    {
        if ( bundleId == getBundle().getBundleId() )
        {
            return getBundle();
        }

        return null;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getBundles()
     */
    public Bundle[] getBundles()
    {
        return new Bundle[]
            { getBundle() };
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getDataFile(java.lang.String)
     */
    public File getDataFile( String arg0 )
    {
        return null;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getProperty(java.lang.String)
     */
    public String getProperty( String arg0 )
    {
        return null;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference)
     */
    public Object getService( ServiceReference serviceReference )
    {
        if ( serviceReference instanceof MockServiceReference )
        {
            return ( ( MockServiceReference ) serviceReference ).getServiceRegistration().getService();
        }

        return null;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getServiceReference(java.lang.String)
     */
    public ServiceReference getServiceReference( String name )
    {
        ServiceRegistration sr = ( ServiceRegistration ) services.get( name );
        return ( sr != null ) ? sr.getReference() : null;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getServiceReferences(java.lang.String, java.lang.String)
     */
    public ServiceReference[] getServiceReferences( String arg0, String arg1 )
    {
        return null;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#installBundle(java.lang.String)
     */
    public Bundle installBundle( String arg0 )
    {
        return null;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#installBundle(java.lang.String, java.io.InputStream)
     */
    public Bundle installBundle( String arg0, InputStream arg1 )
    {
        return null;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#registerService(java.lang.String[], java.lang.Object, java.util.Dictionary)
     */
    public ServiceRegistration registerService( String[] names, Object service, Dictionary props )
    {
        props.put( Constants.OBJECTCLASS, names );
        ServiceRegistration sr = new MockServiceRegistration( this, service, names, props );

        for ( int i = 0; i < names.length; i++ )
        {
            services.put( names[i], sr );
        }

        fireServiceEvent( sr.getReference(), ServiceEvent.REGISTERED );

        return sr;
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#registerService(java.lang.String, java.lang.Object, java.util.Dictionary)
     */
    public ServiceRegistration registerService( String name, Object service, Dictionary props )
    {
        return registerService( new String[]
            { name }, service, props );
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#removeBundleListener(org.osgi.framework.BundleListener)
     */
    public void removeBundleListener( BundleListener arg0 )
    {
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#removeFrameworkListener(org.osgi.framework.FrameworkListener)
     */
    public void removeFrameworkListener( FrameworkListener arg0 )
    {
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#removeServiceListener(org.osgi.framework.ServiceListener)
     */
    public void removeServiceListener( ServiceListener listener )
    {
        serviceListeners.remove( listener );
    }


    /* (non-Javadoc)
     * @see org.osgi.framework.BundleContext#ungetService(org.osgi.framework.ServiceReference)
     */
    public boolean ungetService( ServiceReference serviceReference )
    {
        if ( serviceReference instanceof MockServiceReference )
        {
            return ( ( MockServiceReference ) serviceReference ).getServiceRegistration().ungetService();

        }

        return false;
    }


    private void fireServiceEvent( ServiceReference ref, int type )
    {
        ServiceEvent se = new ServiceEvent( type, ref );
        for ( Iterator li = serviceListeners.iterator(); li.hasNext(); )
        {
            ( ( ServiceListener ) li.next() ).serviceChanged( se );
        }
    }

    private static class MockServiceRegistration implements ServiceRegistration
    {

        private MockBundleContext bundleContext;
        private Dictionary serviceProps;
        private String[] serviceNames;
        private Object service;
        private ServiceReference serviceRef;
        int refs;


        MockServiceRegistration( MockBundleContext bundleContext, Object service, String[] names, Dictionary props )
        {
            this.bundleContext = bundleContext;
            this.serviceNames = names;
            this.serviceProps = props;
            this.service = service;
            this.serviceRef = new MockServiceReference( this );
        }


        Object getService()
        {
            refs++;
            return service;
        }


        boolean ungetService()
        {
            refs--;
            return refs <= 0;
        }


        public ServiceReference getReference()
        {
            return serviceRef;
        }


        public void setProperties( Dictionary props )
        {
            serviceProps = props;
        }


        public void unregister()
        {
            bundleContext.fireServiceEvent( getReference(), ServiceEvent.UNREGISTERING );

            for ( int i = 0; i < serviceNames.length; i++ )
            {
                bundleContext.services.remove( serviceNames[i] );
            }
        }

    };

    private static class MockServiceReference implements ServiceReference
    {
        private MockServiceRegistration msr;


        MockServiceReference( MockServiceRegistration msr )
        {
            this.msr = msr;
        }


        MockServiceRegistration getServiceRegistration()
        {
            return msr;
        }


        public Bundle getBundle()
        {
            return msr.bundleContext.getBundle();
        }


        public Object getProperty( String prop )
        {
            return msr.serviceProps.get( prop );
        }


        public String[] getPropertyKeys()
        {
            List keys = new ArrayList();
            for ( Enumeration ke = msr.serviceProps.keys(); ke.hasMoreElements(); )
            {
                keys.add( ke.nextElement() );
            }
            return ( String[] ) keys.toArray( new String[keys.size()] );
        }


        public Bundle[] getUsingBundles()
        {
            return null;
        }


        public boolean isAssignableTo( Bundle arg0, String arg1 )
        {
            return false;
        }

    }
}
