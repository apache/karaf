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


import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;


public class SimpleComponent
{

    // component configuration property whose existence causes the
    // activate method to fail
    public static final String PROP_ACTIVATE_FAILURE = "request.activation.failure";

    public static final Map<Long, SimpleComponent> INSTANCES = new HashMap<Long, SimpleComponent>();

    public static final Set<SimpleComponent> PREVIOUS_INSTANCES = new HashSet<SimpleComponent>();

    public static SimpleComponent INSTANCE;

    private Map<?, ?> m_config;

    public long m_id;

    public ComponentContext m_activateContext;

    public SimpleService m_singleRef;

    public final Set<SimpleService> m_multiRef = new HashSet<SimpleService>();


    @SuppressWarnings("unused")
    private void activate( ComponentContext activateContext, Map<?, ?> config )
    {
        // fail activation if requested so
        if ( config.containsKey( PROP_ACTIVATE_FAILURE ) )
        {
            throw new RuntimeException( String.valueOf( config.get( PROP_ACTIVATE_FAILURE ) ) );
        }

        m_id = ( Long ) config.get( ComponentConstants.COMPONENT_ID );
        m_activateContext = activateContext;

        INSTANCE = this;
        INSTANCES.put( m_id, this );
        setConfig( config );

        if ( PREVIOUS_INSTANCES.contains( this ) )
        {
            System.err.println();
            System.err.println( "An instance has been reused !!!" );
            System.err.println( "Existing: " + PREVIOUS_INSTANCES );
            System.err.println( "New     : " + this );
            System.err.println();
        }
        else
        {
            PREVIOUS_INSTANCES.add( this );
        }
    }


    @SuppressWarnings("unused")
    private void configure( ComponentContext context )
    {
        setConfig( context.getProperties() );
    }


    @SuppressWarnings("unused")
    private void deactivate()
    {
        INSTANCES.remove( getProperty( ComponentConstants.COMPONENT_ID ) );

        m_activateContext = null;
        INSTANCE = null;
        setConfig( new HashMap<Object, Object>() );
    }


    protected void setConfig( Map<?, ?> config )
    {
        m_config = config;
    }


    protected void setConfig( Dictionary<?, ?> config )
    {
        Map<Object, Object> configMap = new HashMap<Object, Object>();
        for ( Enumeration<?> ce = config.keys(); ce.hasMoreElements(); )
        {
            Object key = ce.nextElement();
            Object value = config.get( key );
            configMap.put( key, value );
        }
        m_config = configMap;
    }


    public Object getProperty( Object name )
    {
        return m_config.get( name );
    }


    // bind method for single service binding
    public void setSimpleService( SimpleService simpleService )
    {
        this.m_singleRef = simpleService;
    }


    // unbind method for single service binding
    public void unsetSimpleService( SimpleService simpleService )
    {
        if ( this.m_singleRef == simpleService )
        {
            this.m_singleRef = null;
        }
    }


    // bind method for multi-service binding
    public void bindSimpleService( SimpleService simpleService )
    {
        this.m_multiRef.add( simpleService );
    }


    // unbind method for multi-service binding
    public void unbindSimpleService( SimpleService simpleService )
    {
        this.m_multiRef.remove( simpleService );
    }
}
