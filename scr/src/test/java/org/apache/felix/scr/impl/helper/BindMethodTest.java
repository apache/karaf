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
package org.apache.felix.scr.impl.helper;


import junit.framework.TestCase;

import org.apache.felix.scr.impl.helper.BindMethod;
import org.apache.felix.scr.impl.manager.ImmediateComponentManager;
import org.apache.felix.scr.impl.manager.components.FakeService;
import org.apache.felix.scr.impl.manager.components.T1;
import org.apache.felix.scr.impl.manager.components.T1a;
import org.apache.felix.scr.impl.manager.components.T3;
import org.apache.felix.scr.impl.manager.components2.T2;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.easymock.EasyMock;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;


public class BindMethodTest extends TestCase
{

    private ServiceReference m_serviceReference;
    private FakeService m_serviceInstance;
    private BindMethod.Service m_service;


    public void setUp()
    {
        m_serviceReference = (ServiceReference) EasyMock.createNiceMock( ServiceReference.class );
        m_serviceInstance = (FakeService) EasyMock.createNiceMock( FakeService.class );
        m_service = new BindMethod.Service()
        {
            public ServiceReference getReference()
            {
                return m_serviceReference;
            }


            public Object getInstance()
            {
                return m_serviceInstance;
            }
        };
        EasyMock.expect( m_serviceReference.getPropertyKeys() ).andReturn( new String[]
            { Constants.SERVICE_ID } ).anyTimes();
        EasyMock.expect( m_serviceReference.getProperty( Constants.SERVICE_ID ) ).andReturn( "Fake Service" )
            .anyTimes();
        EasyMock.replay( new Object[]
            { m_serviceReference } );
    }


    public void test_Unexistent()
    {
        testMethod( "unexistent", new T1(), false, null );
        testMethod( "unexistent", new T1(), true, null );
        testMethod( "unexistent", new T2(), false, null );
        testMethod( "unexistent", new T2(), true, null );
        testMethod( "unexistent", new T3(), false, null );
        testMethod( "unexistent", new T3(), true, null );
    }


    public void test_privateT1()
    {
        testMethod( "privateT1", new T1(), false, null );
        testMethod( "privateT1", new T1(), true, null );
        testMethod( "privateT1", new T2(), false, null );
        testMethod( "privateT1", new T2(), true, null );
        testMethod( "privateT1", new T3(), false, null );
        testMethod( "privateT1", new T3(), true, null );
    }


    public void test_privateT1SR()
    {
        testMethod( "privateT1SR", new T1(), false, null );
        testMethod( "privateT1SR", new T1(), true, "privateT1SR" );
        testMethod( "privateT1SR", new T2(), false, null );
        testMethod( "privateT1SR", new T2(), true, null );
    }


    public void test_privateT1SI()
    {
        testMethod( "privateT1SI", new T1(), false, null );
        testMethod( "privateT1SI", new T1(), true, "privateT1SI" );
        testMethod( "privateT1SI", new T2(), false, null );
        testMethod( "privateT1SI", new T2(), true, null );
    }


    public void test_privateT1SIMap()
    {
        testMethod( "privateT1SIMap", new T1(), false, null );
        testMethod( "privateT1SIMap", new T1(), true, "privateT1SIMap" );
        testMethod( "privateT1SIMap", new T2(), false, null );
        testMethod( "privateT1SIMap", new T2(), true, null );
    }


    public void test_privateT1SSI()
    {
        testMethod( "privateT1SSI", new T1(), false, null );
        testMethod( "privateT1SSI", new T1(), true, "privateT1SSI" );
        testMethod( "privateT1SSI", new T2(), false, null );
        testMethod( "privateT1SSI", new T2(), true, null );
    }


    public void test_privateT1SSIMap()
    {
        testMethod( "privateT1SSIMap", new T1(), false, null );
        testMethod( "privateT1SSIMap", new T1(), true, "privateT1SSIMap" );
        testMethod( "privateT1SSIMap", new T2(), false, null );
        testMethod( "privateT1SSIMap", new T2(), true, null );
    }


    public void test_privateT2()
    {
        testMethod( "privateT2", new T1(), false, null );
        testMethod( "privateT2", new T1(), true, null );
        testMethod( "privateT2", new T2(), false, null );
        testMethod( "privateT2", new T2(), true, null );
    }


    public void test_privateT2SR()
    {
        testMethod( "privateT2SR", new T1(), false, null );
        testMethod( "privateT2SR", new T1(), true, null );
        testMethod( "privateT2SR", new T2(), false, null );
        testMethod( "privateT2SR", new T2(), true, "privateT2SR" );
    }


    public void test_privateT2SI()
    {
        testMethod( "privateT2SI", new T1(), false, null );
        testMethod( "privateT2SI", new T1(), true, null );
        testMethod( "privateT2SI", new T2(), false, null );
        testMethod( "privateT2SI", new T2(), true, "privateT2SI" );
    }


    public void test_privateT2SIMap()
    {
        testMethod( "privateT2SIMap", new T1(), false, null );
        testMethod( "privateT2SIMap", new T1(), true, null );
        testMethod( "privateT2SIMap", new T2(), false, null );
        testMethod( "privateT2SIMap", new T2(), true, "privateT2SIMap" );
    }


    public void test_privateT2SSI()
    {
        testMethod( "privateT2SSI", new T1(), false, null );
        testMethod( "privateT2SSI", new T1(), true, null );
        testMethod( "privateT2SSI", new T2(), false, null );
        testMethod( "privateT2SSI", new T2(), true, "privateT2SSI" );
    }


    public void test_privateT2SSIMap()
    {
        testMethod( "privateT2SSIMap", new T1(), false, null );
        testMethod( "privateT2SSIMap", new T1(), true, null );
        testMethod( "privateT2SSIMap", new T2(), false, null );
        testMethod( "privateT2SSIMap", new T2(), true, "privateT2SSIMap" );
    }


    public void test_packageT1()
    {
        testMethod( "packageT1", new T1(), false, null );
        testMethod( "packageT1", new T1(), true, null );
        testMethod( "packageT1", new T2(), false, null );
        testMethod( "packageT1", new T2(), true, null );
        testMethod( "packageT1", new T3(), false, null );
        testMethod( "packageT1", new T3(), true, null );
        testMethod( "packageT1", new T1a(), false, null );
        testMethod( "packageT1", new T1a(), true, null );
    }


    public void test_packageT1SR()
    {
        testMethod( "packageT1SR", new T1(), false, null );
        testMethod( "packageT1SR", new T1(), true, "packageT1SR" );
        testMethod( "packageT1SR", new T2(), false, null );
        testMethod( "packageT1SR", new T2(), true, null );
        testMethod( "packageT1SR", new T3(), false, null );
        testMethod( "packageT1SR", new T3(), true, null );
        testMethod( "packageT1SR", new T1a(), false, null );
        testMethod( "packageT1SR", new T1a(), true, "packageT1SR" );
    }


    public void test_packageT1SI()
    {
        testMethod( "packageT1SI", new T1(), false, null );
        testMethod( "packageT1SI", new T1(), true, "packageT1SI" );
        testMethod( "packageT1SI", new T2(), false, null );
        testMethod( "packageT1SI", new T2(), true, null );
        testMethod( "packageT1SI", new T3(), false, null );
        testMethod( "packageT1SI", new T3(), true, null );
        testMethod( "packageT1SI", new T1a(), false, null );
        testMethod( "packageT1SI", new T1a(), true, "packageT1SI" );
    }


    public void test_packageT1SIMap()
    {
        testMethod( "packageT1SIMap", new T1(), false, null );
        testMethod( "packageT1SIMap", new T1(), true, "packageT1SIMap" );
        testMethod( "packageT1SIMap", new T2(), false, null );
        testMethod( "packageT1SIMap", new T2(), true, null );
        testMethod( "packageT1SIMap", new T3(), false, null );
        testMethod( "packageT1SIMap", new T3(), true, null );
        testMethod( "packageT1SIMap", new T1a(), false, null );
        testMethod( "packageT1SIMap", new T1a(), true, "packageT1SIMap" );
    }


    public void test_packageT1SSI()
    {
        testMethod( "packageT1SSI", new T1(), false, null );
        testMethod( "packageT1SSI", new T1(), true, "packageT1SSI" );
        testMethod( "packageT1SSI", new T2(), false, null );
        testMethod( "packageT1SSI", new T2(), true, null );
        testMethod( "packageT1SSI", new T3(), false, null );
        testMethod( "packageT1SSI", new T3(), true, null );
        testMethod( "packageT1SSI", new T1a(), false, null );
        testMethod( "packageT1SSI", new T1a(), true, "packageT1SSI" );
    }


    public void test_packageT1SSIMap()
    {
        testMethod( "packageT1SSIMap", new T1(), false, null );
        testMethod( "packageT1SSIMap", new T1(), true, "packageT1SSIMap" );
        testMethod( "packageT1SSIMap", new T2(), false, null );
        testMethod( "packageT1SSIMap", new T2(), true, null );
        testMethod( "packageT1SSIMap", new T3(), false, null );
        testMethod( "packageT1SSIMap", new T3(), true, null );
        testMethod( "packageT1SSIMap", new T1a(), false, null );
        testMethod( "packageT1SSIMap", new T1a(), true, "packageT1SSIMap" );
    }


    public void test_packageT2()
    {
        testMethod( "packageT2", new T1(), false, null );
        testMethod( "packageT2", new T1(), true, null );
        testMethod( "packageT2", new T2(), false, null );
        testMethod( "packageT2", new T2(), true, null );
    }


    public void test_packageT2SR()
    {
        testMethod( "packageT2SR", new T1(), false, null );
        testMethod( "packageT2SR", new T1(), true, null );
        testMethod( "packageT2SR", new T2(), false, null );
        testMethod( "packageT2SR", new T2(), true, "packageT2SR" );
    }


    public void test_packageT2SI()
    {
        testMethod( "packageT2SI", new T1(), false, null );
        testMethod( "packageT2SI", new T1(), true, null );
        testMethod( "packageT2SI", new T2(), false, null );
        testMethod( "packageT2SI", new T2(), true, "packageT2SI" );
    }


    public void test_packageT2SIMap()
    {
        testMethod( "packageT2SIMap", new T1(), false, null );
        testMethod( "packageT2SIMap", new T1(), true, null );
        testMethod( "packageT2SIMap", new T2(), false, null );
        testMethod( "packageT2SIMap", new T2(), true, "packageT2SIMap" );
    }


    public void test_packageT2SSI()
    {
        testMethod( "packageT2SSI", new T1(), false, null );
        testMethod( "packageT2SSI", new T1(), true, null );
        testMethod( "packageT2SSI", new T2(), false, null );
        testMethod( "packageT2SSI", new T2(), true, "packageT2SSI" );
    }


    public void test_packageT2SSIMap()
    {
        testMethod( "packageT2SSIMap", new T1(), false, null );
        testMethod( "packageT2SSIMap", new T1(), true, null );
        testMethod( "packageT2SSIMap", new T2(), false, null );
        testMethod( "packageT2SSIMap", new T2(), true, "packageT2SSIMap" );
    }


    public void test_protectedT1()
    {
        testMethod( "protectedT1", new T1(), false, null );
        testMethod( "protectedT1", new T1(), true, null );
        testMethod( "protectedT1", new T2(), false, null );
        testMethod( "protectedT1", new T2(), true, null );
    }


    public void test_protectedT1SR()
    {
        testMethod( "protectedT1SR", new T1(), false, "protectedT1SR" );
        testMethod( "protectedT1SR", new T1(), true, "protectedT1SR" );
        testMethod( "protectedT1SR", new T2(), false, "protectedT1SR" );
        testMethod( "protectedT1SR", new T2(), true, "protectedT1SR" );
    }


    public void test_protectedT1SI()
    {
        testMethod( "protectedT1SI", new T1(), false, "protectedT1SI" );
        testMethod( "protectedT1SI", new T1(), true, "protectedT1SI" );
        testMethod( "protectedT1SI", new T2(), false, "protectedT1SI" );
        testMethod( "protectedT1SI", new T2(), true, "protectedT1SI" );
    }


    public void test_protectedT1SSI()
    {
        testMethod( "protectedT1SSI", new T1(), false, "protectedT1SSI" );
        testMethod( "protectedT1SSI", new T1(), true, "protectedT1SSI" );
        testMethod( "protectedT1SSI", new T2(), false, "protectedT1SSI" );
        testMethod( "protectedT1SSI", new T2(), true, "protectedT1SSI" );
    }


    public void test_publicT1()
    {
        testMethod( "publicT1", new T1(), false, null );
        testMethod( "publicT1", new T1(), true, null );
        testMethod( "publicT1", new T2(), false, null );
        testMethod( "publicT1", new T2(), true, null );
    }


    public void test_publicT1SR()
    {
        testMethod( "publicT1SR", new T1(), false, "publicT1SR" );
        testMethod( "publicT1SR", new T1(), true, "publicT1SR" );
        testMethod( "publicT1SR", new T2(), false, "publicT1SR" );
        testMethod( "publicT1SR", new T2(), true, "publicT1SR" );
    }


    public void test_publicT1SI()
    {
        testMethod( "publicT1SI", new T1(), false, "publicT1SI" );
        testMethod( "publicT1SI", new T1(), true, "publicT1SI" );
        testMethod( "publicT1SI", new T2(), false, "publicT1SI" );
        testMethod( "publicT1SI", new T2(), true, "publicT1SI" );
    }


    public void test_publicT1SIMap()
    {
        testMethod( "publicT1SIMap", new T1(), false, null );
        testMethod( "publicT1SIMap", new T1(), true, "publicT1SIMap" );
        testMethod( "publicT1SIMap", new T2(), false, null );
        testMethod( "publicT1SIMap", new T2(), true, "publicT1SIMap" );
    }


    public void test_publicT1SSI()
    {
        testMethod( "publicT1SSI", new T1(), false, "publicT1SSI" );
        testMethod( "publicT1SSI", new T1(), true, "publicT1SSI" );
        testMethod( "publicT1SSI", new T2(), false, "publicT1SSI" );
        testMethod( "publicT1SSI", new T2(), true, "publicT1SSI" );
    }


    public void test_publicT1SSIMap()
    {
        testMethod( "publicT1SSIMap", new T1(), false, null );
        testMethod( "publicT1SSIMap", new T1(), true, "publicT1SSIMap" );
        testMethod( "publicT1SSIMap", new T2(), false, null );
        testMethod( "publicT1SSIMap", new T2(), true, "publicT1SSIMap" );
    }


    public void test_suitable()
    {
        // T1 should use its own public implementation
        testMethod( "suitable", new T1(), false, "suitableT1" );
        testMethod( "suitable", new T1(), true, "suitableT1" );

        // T2's private implementation is only visible for DS 1.1
        testMethod( "suitable", new T2(), false, null );
        testMethod( "suitable", new T2(), true, "suitableT2" );

        // T3 extends T2 and cannot see T2's private method
        testMethod( "suitable", new T3(), false, null );
        testMethod( "suitable", new T3(), true, null );

        // T1a extends T1 and uses T1's public method
        testMethod( "suitable", new T1a(), false, "suitableT1" );
        testMethod( "suitable", new T1a(), true, "suitableT1" );
    }


    private void testMethod( final String methodName, final T1 component, final boolean isDS11,
        final String expectCallPerformed )
    {
        ComponentMetadata metadata = new ComponentMetadata( 0 ) {
            public boolean isDS11() {
                return isDS11;
            }
        };
        ImmediateComponentManager icm = new ImmediateComponentManager( null, null, metadata );
        BindMethod bm = new BindMethod( icm, methodName, component.getClass(), "reference",
            FakeService.class.getName() );
        bm.invoke( component, m_service );
        assertEquals( expectCallPerformed, component.callPerformed );
    }
}
