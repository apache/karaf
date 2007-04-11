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


import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.apache.felix.metatype.MockBundleContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;


/**
 * The <code>MetaTypeServiceImplTest</code> class tests the
 * {@link MetaTypeServiceImpl}.
 *
 * @author fmeschbe
 */
public class MetaTypeServiceImplTest extends TestCase
{

    BundleContext bundleContext;


    protected void setUp() throws Exception
    {
        super.setUp();
        bundleContext = new MockBundleContext( 10, "org.apache.felix.metatype.Mock" );
        bundleContext.getBundle().start();
    }


    protected void tearDown() throws Exception
    {
        bundleContext.getBundle().stop();
        bundleContext = null;

        super.tearDown();
    }


    public void testEmpty()
    {
        MetaTypeService mts = new MetaTypeServiceImpl( bundleContext );
        MetaTypeInformation mti = mts.getMetaTypeInformation( bundleContext.getBundle() );
        checkEmpty( mti );
    }


    public void testAfterCretionManagedService()
    {
        MetaTypeService mts = new MetaTypeServiceImpl( bundleContext );
        MetaTypeInformation mti = mts.getMetaTypeInformation( bundleContext.getBundle() );

        // assert still empty
        checkEmpty( mti );

        // register a service with PID
        String pid = "testAfterCreation";
        MockManagedService service = new MockManagedService();
        Dictionary props = new Hashtable();
        props.put( Constants.SERVICE_PID, pid );
        ServiceRegistration sr = bundleContext.registerService( ManagedService.class.getName(), service, props );

        // locales should contain MockMetaTypeProvider.LOCALES
        assertNotNull( mti.getLocales() );
        assertTrue( mti.getLocales().length == 1 );
        assertEquals( MockMetaTypeProvider.LOCALES[0], mti.getLocales()[0] );

        // pids must contain pid
        assertNotNull( mti.getPids() );
        assertTrue( mti.getPids().length == 1 );
        assertEquals( pid, mti.getPids()[0] );

        // factoryPids must be empty
        assertTrue( mti.getFactoryPids() == null || mti.getFactoryPids().length == 0 );

        // unregister the service
        sr.unregister();

        // ensure everything is clear now again
        checkEmpty( mti );
    }


    public void testAfterCretionManagedServiceFactory()
    {
        MetaTypeService mts = new MetaTypeServiceImpl( bundleContext );
        MetaTypeInformation mti = mts.getMetaTypeInformation( bundleContext.getBundle() );

        // assert still empty
        checkEmpty( mti );

        // register a service with PID
        String pid = "testAfterCreation";
        String factoryPid = "testAfterCreation_factory";
        MockManagedServiceFactory service = new MockManagedServiceFactory();
        Dictionary props = new Hashtable();
        props.put( Constants.SERVICE_PID, pid );
        props.put( ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid );
        ServiceRegistration sr = bundleContext.registerService( ManagedService.class.getName(), service, props );

        // locales should contain MockMetaTypeProvider.LOCALES
        assertNotNull( mti.getLocales() );
        assertTrue( mti.getLocales().length == 1 );
        assertEquals( MockMetaTypeProvider.LOCALES[0], mti.getLocales()[0] );

        // pids must be empty
        assertTrue( mti.getPids() == null || mti.getPids().length == 0 );

        // pids must contain pid
        assertNotNull( mti.getFactoryPids() );
        assertTrue( mti.getFactoryPids().length == 1 );
        assertEquals( factoryPid, mti.getFactoryPids()[0] );

        // unregister the service
        sr.unregister();

        // ensure everything is clear now again
        checkEmpty( mti );
    }


    private void checkEmpty( MetaTypeInformation mti )
    {
        assertEquals( bundleContext.getBundle().getBundleId(), mti.getBundle().getBundleId() );
        assertTrue( mti.getLocales() == null || mti.getLocales().length == 0 );
        assertTrue( mti.getPids() == null || mti.getPids().length == 0 );
        assertTrue( mti.getFactoryPids() == null || mti.getFactoryPids().length == 0 );
    }

    private static class MockMetaTypeProvider implements MetaTypeProvider
    {

        static String[] LOCALES =
            { "en_US" };


        public String[] getLocales()
        {
            return LOCALES;
        }


        public ObjectClassDefinition getObjectClassDefinition( String arg0, String arg1 )
        {
            return null;
        }
    }

    private static class MockManagedService extends MockMetaTypeProvider implements ManagedService
    {

        public void updated( Dictionary arg0 )
        {
        }

    }

    private static class MockManagedServiceFactory extends MockMetaTypeProvider implements ManagedServiceFactory
    {

        public void deleted( String arg0 )
        {
        }


        public String getName()
        {
            return null;
        }


        public void updated( String arg0, Dictionary arg1 )
        {
        }

    }
}
