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
package org.apache.felix.scr.integration.components;


import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;


public class SimpleServiceImpl implements SimpleService
{

    private String m_value;

    private int m_ranking;

    private String m_filterProp;

    private ServiceRegistration m_registration;


    public static SimpleServiceImpl create( BundleContext bundleContext, String value )
    {
        return create( bundleContext, value, 0 );
    }


    public static SimpleServiceImpl create( BundleContext bundleContext, String value, int ranking )
    {
        SimpleServiceImpl instance = new SimpleServiceImpl( value, ranking );
        Properties props = instance.getProperties();
        instance.setRegistration( bundleContext.registerService( SimpleService.class.getName(), instance, props ) );
        return instance;
    }


    SimpleServiceImpl( final String value, final int ranking )
    {
        this.m_value = value;
        this.m_ranking = ranking;
        this.m_filterProp = "match";
    }


    private Properties getProperties()
    {
        final Properties props = new Properties();
        props.put( "value", m_value );
        props.put( "filterprop", m_filterProp );
        if ( m_ranking != 0 )
        {
            props.put( Constants.SERVICE_RANKING, Integer.valueOf( m_ranking ) );
        }
        return props;
    }


    public void update( String value )
    {
        if ( this.m_registration != null )
        {
            this.m_value = value;
            this.m_registration.setProperties( getProperties() );
        }
    }


    public void setFilterProperty( String filterProp )
    {
        if ( this.m_registration != null )
        {
            this.m_filterProp = filterProp;
            this.m_registration.setProperties( getProperties() );
        }
    }


    public void drop()
    {
        ServiceRegistration sr = getRegistration();
        if ( sr != null )
        {
            setRegistration( null );
            sr.unregister();
        }
    }


    public String getValue()
    {
        return m_value;
    }


    public void setRegistration( ServiceRegistration registration )
    {
        m_registration = registration;
    }


    public ServiceRegistration getRegistration()
    {
        return m_registration;
    }


    @Override
    public String toString()
    {
        return getClass().getSimpleName() + ": value=" + getValue() + ", filterprop=" + m_filterProp;
    }
}
