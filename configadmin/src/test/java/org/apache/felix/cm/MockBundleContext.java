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
package org.apache.felix.cm;


import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;


/**
 * The <code>MockBundleContext</code> is a dummy implementation of the
 * <code>BundleContext</code> interface. No methods are implemented here, that
 * is all methods have no effect and return <code>null</code> if a return value
 * is specified.
 * <p>
 * Extensions may overwrite methods as see fit.
 */
public class MockBundleContext implements BundleContext
{

    private final Properties properties = new Properties();


    public void setProperty( String name, String value )
    {
        if ( value == null )
        {
            properties.remove( name );
        }
        else
        {
            properties.setProperty( name, value );
        }
    }


    /*
     * (non-Javadoc)
     * @see
     * org.osgi.framework.BundleContext#addBundleListener(org.osgi.framework
     * .BundleListener)
     */
    public void addBundleListener( BundleListener arg0 )
    {
    }


    /*
     * (non-Javadoc)
     * @see
     * org.osgi.framework.BundleContext#addFrameworkListener(org.osgi.framework
     * .FrameworkListener)
     */
    public void addFrameworkListener( FrameworkListener arg0 )
    {
    }


    /*
     * (non-Javadoc)
     * @see
     * org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework
     * .ServiceListener)
     */
    public void addServiceListener( ServiceListener arg0 )
    {
    }


    /*
     * (non-Javadoc)
     * @see
     * org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework
     * .ServiceListener, java.lang.String)
     */
    public void addServiceListener( ServiceListener arg0, String arg1 ) throws InvalidSyntaxException
    {
    }


    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleContext#createFilter(java.lang.String)
     */
    public Filter createFilter( String arg0 ) throws InvalidSyntaxException
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * @see
     * org.osgi.framework.BundleContext#getAllServiceReferences(java.lang.String
     * , java.lang.String)
     */
    public ServiceReference[] getAllServiceReferences( String arg0, String arg1 ) throws InvalidSyntaxException
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getBundle()
     */
    public Bundle getBundle()
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getBundle(long)
     */
    public Bundle getBundle( long arg0 )
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getBundles()
     */
    public Bundle[] getBundles()
    {
        return new Bundle[0];
    }


    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getDataFile(java.lang.String)
     */
    public File getDataFile( String arg0 )
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleContext#getProperty(java.lang.String)
     */
    public String getProperty( String name )
    {
        return properties.getProperty( name );
    }


    /*
     * (non-Javadoc)
     * @seeorg.osgi.framework.BundleContext#getService(org.osgi.framework.
     * ServiceReference)
     */
    public Object getService( ServiceReference arg0 )
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * @see
     * org.osgi.framework.BundleContext#getServiceReference(java.lang.String)
     */
    public ServiceReference getServiceReference( String arg0 )
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * @see
     * org.osgi.framework.BundleContext#getServiceReferences(java.lang.String,
     * java.lang.String)
     */
    public ServiceReference[] getServiceReferences( String arg0, String arg1 ) throws InvalidSyntaxException
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleContext#installBundle(java.lang.String)
     */
    public Bundle installBundle( String arg0 ) throws BundleException
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleContext#installBundle(java.lang.String,
     * java.io.InputStream)
     */
    public Bundle installBundle( String arg0, InputStream arg1 ) throws BundleException
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleContext#registerService(java.lang.String[],
     * java.lang.Object, java.util.Dictionary)
     */
    public ServiceRegistration registerService( String[] arg0, Object arg1, Dictionary arg2 )
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleContext#registerService(java.lang.String,
     * java.lang.Object, java.util.Dictionary)
     */
    public ServiceRegistration registerService( String arg0, Object arg1, Dictionary arg2 )
    {
        return null;
    }


    /*
     * (non-Javadoc)
     * @see
     * org.osgi.framework.BundleContext#removeBundleListener(org.osgi.framework
     * .BundleListener)
     */
    public void removeBundleListener( BundleListener arg0 )
    {
    }


    /*
     * (non-Javadoc)
     * @see
     * org.osgi.framework.BundleContext#removeFrameworkListener(org.osgi.framework
     * .FrameworkListener)
     */
    public void removeFrameworkListener( FrameworkListener arg0 )
    {
    }


    /*
     * (non-Javadoc)
     * @see
     * org.osgi.framework.BundleContext#removeServiceListener(org.osgi.framework
     * .ServiceListener)
     */
    public void removeServiceListener( ServiceListener arg0 )
    {
    }


    /*
     * (non-Javadoc)
     * @seeorg.osgi.framework.BundleContext#ungetService(org.osgi.framework.
     * ServiceReference)
     */
    public boolean ungetService( ServiceReference arg0 )
    {
        return false;
    }
}
