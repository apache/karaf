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
package org.apache.felix.scr.impl.manager;


import java.util.Map;

import junit.framework.TestCase;

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
        m_serviceReference = ( ServiceReference ) EasyMock.createNiceMock( ServiceReference.class );
        m_serviceInstance = ( FakeService ) EasyMock.createNiceMock( FakeService.class );
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
        System.out.println();
        final T1 t1 = new T1();
        createMethod( "unexistent", T1.class ).invoke( t1, m_service );
        assertNull( t1.callPerformed );
    }


    public void test_privateT1()
    {
        System.out.println();
        final T1 t1 = new T1();
        createMethod( "privateT1", T1.class ).invoke( t1, m_service );
        assertNull( t1.callPerformed );
    }


    public void test_protectedT1()
    {
        System.out.println();
        final T1 t1 = new T1();
        createMethod( "protectedT1", T1.class ).invoke( t1, m_service );
        assertNull( t1.callPerformed );
    }


    public void test_protectedT1SR()
    {
        System.out.println();
        final T1 t1 = new T1();
        createMethod( "protectedT1SR", T1.class ).invoke( t1, m_service );
        assertEquals( "protectedT1SR", t1.callPerformed );
    }


    public void test_protectedT1SI()
    {
        System.out.println();
        final T1 t1 = new T1();
        createMethod( "protectedT1SI", T1.class ).invoke( t1, m_service );
        assertEquals( "protectedT1SI", t1.callPerformed );
    }


    public void test_protectedT1SSI()
    {
        System.out.println();
        final T1 t1 = new T1();
        createMethod( "protectedT1SSI", T1.class ).invoke( t1, m_service );
        assertEquals( "protectedT1SSI", t1.callPerformed );
    }


    public void test_protectedT1SSI_onT2()
    {
        System.out.println();
        final T2 t2 = new T2();
        createMethod( "protectedT1SSI", T2.class ).invoke( t2, m_service );
        assertEquals( "protectedT1SSI", t2.callPerformed );
    }


    public void test_publicT1()
    {
        System.out.println();
        final T1 t1 = new T1();
        createMethod( "publicT1", T1.class ).invoke( t1, m_service );
        assertNull( t1.callPerformed );
    }


    public void test_publicT1SR()
    {
        System.out.println();
        final T1 t1 = new T1();
        createMethod( "publicT1SR", T1.class ).invoke( t1, m_service );
        assertEquals( "publicT1SR", t1.callPerformed );
    }


    public void test_publicT1SR_onT2()
    {
        System.out.println();
        final T2 t2 = new T2();
        createMethod( "publicT1SR", T2.class ).invoke( t2, m_service );
        assertEquals( "publicT1SR", t2.callPerformed );
    }


    public void test_publicT1SI()
    {
        System.out.println();
        final T1 t1 = new T1();
        createMethod( "publicT1SI", T1.class ).invoke( t1, m_service );
        assertEquals( "publicT1SI", t1.callPerformed );
    }


    public void test_publicT1SIMap()
    {
        System.out.println();
        final T1 t1 = new T1();
        createMethod( "publicT1SIMap", T1.class ).invoke( t1, m_service );
        assertEquals( "publicT1SIMap", t1.callPerformed );
    }


    public void test_publicT1SI_onT2()
    {
        System.out.println();
        final T2 t2 = new T2();
        createMethod( "publicT1SI", T2.class ).invoke( t2, m_service );
        assertEquals( "publicT1SI", t2.callPerformed );
    }


    public void test_publicT1SSI()
    {
        System.out.println();
        final T1 t1 = new T1();
        createMethod( "publicT1SSI", T1.class ).invoke( t1, m_service );
        assertEquals( "publicT1SSI", t1.callPerformed );
    }


    public void test_publicT1SSI_onT2()
    {
        System.out.println();
        final T2 t2 = new T2();
        createMethod( "publicT1SSI", T2.class ).invoke( t2, m_service );
        assertEquals( "publicT1SSI", t2.callPerformed );
    }

    private static interface SuperFakeService
    {

    }

    private static interface FakeService extends SuperFakeService
    {

    }

    private static class T1
    {

        String callPerformed = null;


        private void privateT1()
        {
            callPerformed = "privateT1";
        }


        protected void protectedT1()
        {
            callPerformed = "protectedT1";
        }


        protected void protectedT1SR( ServiceReference sr )
        {
            if ( sr != null )
            {
                callPerformed = "protectedT1SR";
            }
            else
            {
                callPerformed = "protectedT1SR with null param";
            }
        }


        protected void protectedT1SI( FakeService si )
        {
            if ( si != null )
            {
                callPerformed = "protectedT1SI";
            }
            else
            {
                callPerformed = "protectedT1SI with null param";
            }
        }


        protected void protectedT1SSI( SuperFakeService si )
        {
            if ( si != null )
            {
                callPerformed = "protectedT1SSI";
            }
            else
            {
                callPerformed = "protectedT1SSI with null param";
            }
        }


        protected void publicT1()
        {
            callPerformed = "publicT1";
        }


        public void publicT1SR( ServiceReference sr )
        {
            if ( sr != null )
            {
                callPerformed = "publicT1SR";
            }
            else
            {
                callPerformed = "publicT1SR with null param";
            }
        }


        public void publicT1SI( FakeService si )
        {
            if ( si != null )
            {
                callPerformed = "publicT1SI";
            }
            else
            {
                callPerformed = "publicT1SI with null param";
            }
        }


        public void publicT1SIMap( FakeService si, Map props )
        {
            if ( si != null && props != null && props.size() > 0 )
            {
                callPerformed = "publicT1SIMap";
            }
            else if ( si == null )
            {
                callPerformed = "publicT1SIMap with null service instance";
            }
            else if ( props == null )
            {
                callPerformed = "publicT1SIMap with null props";
            }
            else
            {
                callPerformed = "publicT1SIMap with empty props";
            }

        }


        public void publicT1SSI( SuperFakeService si )
        {
            if ( si != null )
            {
                callPerformed = "publicT1SSI";
            }
            else
            {
                callPerformed = "publicT1SSI with null param";
            }
        }
    }

    private class T2 extends T1
    {

    }


    public BindMethod createMethod( final String methodName, final Class componentClass )
    {
        return new BindMethod( methodName, componentClass, "reference", FakeService.class.getName(), new SysOutLogger() );
    }

    private static class SysOutLogger implements BindMethod.Logger
    {

        private static final String[] LEVELS = new String[]
            { "ERROR", "WARNING", "INFO", "DEBUG" };


        public void log( int level, String message )
        {
            log( level, message, null );
        }


        public void log( int level, String message, Throwable ex )
        {
            System.out.println( LEVELS[level - 1] + " - " + message );
            if ( ex != null )
            {
                System.out.println( ex.getClass().getName() + "-" + ex.getMessage() );
            }
        }
    }

}
