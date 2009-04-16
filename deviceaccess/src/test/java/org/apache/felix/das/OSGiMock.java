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
package org.apache.felix.das;


import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;


/**
 * 
 * a very simple mock of an osgi framework. enables the registration of services.
 * automatically generates mocked service references for them.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class OSGiMock
{

	@Mock
    private BundleContext m_context;

    private Map<Object, ServiceReference> m_references;

    private Map<ServiceReference, Bundle> m_bundles;

    private int m_serviceIndex = 1;

    public OSGiMock()
    {
    	MockitoAnnotations.initMocks(this);
        m_references = new HashMap<Object, ServiceReference>();
        m_bundles = new HashMap<ServiceReference, Bundle>();
    }

    public static ServiceReference createReference(final Properties p) 
    {
        ServiceReference ref = Mockito.mock( ServiceReference.class );

        Mockito.when(ref.getProperty(Mockito.anyString())).thenAnswer(new Answer<Object>() {
        	public Object answer(InvocationOnMock invocation) throws Throwable {
        		return p.get(invocation.getArguments()[0].toString());
        	}
        });
        
        Mockito.when(ref.getPropertyKeys()).thenAnswer(new Answer<Object>() {
        	public Object answer(InvocationOnMock invocation) throws Throwable {
        		return p.keySet().toArray(new String[0]);
        	}
        });
        
        
        return ref;
    }



    public BundleContext getBundleContext()
    {
        return m_context;
    }


    @SuppressWarnings("all")
    public ServiceReference registerService( String[] ifaces, Object impl, Properties props )
    {

        ServiceReference ref = createReference( ifaces, props );

        Mockito.when( m_context.registerService( ifaces, impl, props ) )
        	.thenReturn( null );

        Mockito.when( m_context.getService( ref ) ).thenReturn( impl );

        m_references.put( impl, ref );

        return ref;
    }


    public ServiceReference getReference( Object service )
    {
        return m_references.get( service );
    }


    public Bundle getBundle( ServiceReference ref )
    {
        return m_bundles.get( ref );
    }



    @SuppressWarnings("all")
    public ServiceReference createReference( String[] ifaces, Properties props )
    {

        final ServiceReference ref = Mockito.mock( ServiceReference.class );

        RefPropAnswer answer = new RefPropAnswer( props, ifaces );

        Mockito.when( ref.getProperty( Mockito.anyString() ) )
        	.thenAnswer( answer );

        Mockito.when( ref.getPropertyKeys() )
        	.thenReturn( props.keySet().toArray( new String[0] ) );

        Bundle bundle = Mockito.mock( Bundle.class );
        
        Mockito.when( ref.getBundle() ).thenReturn( bundle );

        m_bundles.put( ref, bundle );

        return ref;
    }

    @SuppressWarnings({ "unchecked" })
    private class RefPropAnswer implements Answer<Object>
    {
        private final Dictionary m_p;


        public RefPropAnswer( Dictionary p, String[] iface )
        {
            m_p = p;
            m_p.put( Constants.OBJECTCLASS, iface );
            m_p.put( Constants.SERVICE_ID, m_serviceIndex++ );
        }


        public Object answer(InvocationOnMock invocation) throws Throwable
        {
        	String key = (String)invocation.getArguments()[0];
            return m_p.get( key );
        }

    }
}
