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
package org.apache.felix.scr.impl.config;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.manager.ImmediateComponentManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.XmlHandler;


public class ConfiguredComponentHolderTest extends TestCase
{

    public void test_none()
    {
        // setup a holder
        final String name = "test.none";
        final ComponentMetadata cm = createComponentMetadata( name );
        final TestingConfiguredComponentHolder holder = new TestingConfiguredComponentHolder( cm );

        // assert single component and no map
        final ImmediateComponentManager cmgr = getSingleManager( holder );
        assertNotNull( "Expect single component manager", cmgr );
        assertNull( "Expect no component manager list", getComponentManagers( holder ) );

        // assert no configuration of single component
        assertFalse( "Expect no configuration", cmgr.hasConfiguration() );
    }


    public void test_singleton()
    {
        // setup a holder
        final String name = "test.singleton";
        final ComponentMetadata cm = createComponentMetadata( name );
        final TestingConfiguredComponentHolder holder = new TestingConfiguredComponentHolder( cm );

        // assert single component and no map
        final ImmediateComponentManager cmgr = getSingleManager( holder );
        assertNotNull( "Expect single component manager", cmgr );
        assertNull( "Expect no component manager list", getComponentManagers( holder ) );

        // assert no configuration of single component
        assertFalse( "Expect no configuration", cmgr.hasConfiguration() );

        // configure with the singleton configuration
        final Dictionary config = new Hashtable();
        config.put( "value", name );
        holder.configurationUpdated( name, config );

        // assert single component and no map
        final ImmediateComponentManager cmgrAfterConfig = getSingleManager( holder );
        assertNotNull( "Expect single component manager", cmgrAfterConfig );
        assertNull( "Expect no component manager list", getComponentManagers( holder ) );

        // assert configuration of single component
        assertTrue( "Expect configuration after updating it", cmgrAfterConfig.hasConfiguration() );
        final Dictionary componentConfig = ( ( MockImmediateComponentManager ) cmgrAfterConfig ).getConfiguration();
        assertEquals( "Expect exact configuration set", config, componentConfig );

        // unconfigure singleton
        holder.configurationDeleted( name );

        // assert single component and no map
        final ImmediateComponentManager cmgrAfterUnconfig = getSingleManager( holder );
        assertNotNull( "Expect single component manager", cmgrAfterUnconfig );
        assertNull( "Expect no component manager list", getComponentManagers( holder ) );

        // assert no configuration of single component
        assertFalse( "Expect no configuration", cmgrAfterUnconfig.hasConfiguration() );
    }


    public void test_factory()
    {
        // setup a holder
        final String name = "test.factory";
        final ComponentMetadata cm = createComponentMetadata( name );
        final TestingConfiguredComponentHolder holder = new TestingConfiguredComponentHolder( cm );

        // assert single component and no map
        final ImmediateComponentManager cmgr = getSingleManager( holder );
        assertNotNull( "Expect single component manager", cmgr );
        assertNull( "Expect no component manager list", getComponentManagers( holder ) );

        // assert no configuration of single component
        assertFalse( "Expect no configuration", cmgr.hasConfiguration() );

        // configure with configuration
        final String pid1 = "test.factory.0001";
        final Dictionary config1 = new Hashtable();
        config1.put( "value", pid1 );
        holder.configurationUpdated( pid1, config1 );

        // assert single component and single-entry map
        final ImmediateComponentManager cmgrAfterConfig = getSingleManager( holder );
        final ImmediateComponentManager[] cmgrsAfterConfig = getComponentManagers( holder );
        assertNotNull( "Expect single component manager", cmgrAfterConfig );
        assertNotNull( "Expect component manager list", cmgrsAfterConfig );
        assertEquals( "Expect one component manager in list", 1, cmgrsAfterConfig.length );

        // add another configuration
        final String pid2 = "test.factory.0002";
        final Dictionary config2 = new Hashtable();
        config1.put( "value", pid2 );
        holder.configurationUpdated( pid2, config2 );

        // assert single component and single-entry map
        final ImmediateComponentManager cmgrAfterConfig2 = getSingleManager( holder );
        final ImmediateComponentManager[] cmgrsAfterConfig2 = getComponentManagers( holder );
        assertNotNull( "Expect single component manager", cmgrAfterConfig2 );
        assertNotNull( "Expect component manager list", cmgrsAfterConfig2 );
        assertEquals( "Expect two component manager in list", 2, cmgrsAfterConfig2.length );

        // remove second configuration
        holder.configurationDeleted( pid2 );

        // assert single component and single-entry map
        final ImmediateComponentManager cmgrAfterUnConfig2 = getSingleManager( holder );
        final ImmediateComponentManager[] cmgrsAfterUnConfig2 = getComponentManagers( holder );
        assertNotNull( "Expect single component manager", cmgrAfterUnConfig2 );
        assertNotNull( "Expect component manager list", cmgrsAfterUnConfig2 );
        assertEquals( "Expect one component manager in list", 1, cmgrsAfterUnConfig2.length );

        // add second config again and remove first config -> replace singleton component
        holder.configurationUpdated( pid2, config2 );
        holder.configurationDeleted( pid1 );

        // assert single component and single-entry map
        final ImmediateComponentManager cmgrAfterConfigUnconfig = getSingleManager( holder );
        final ImmediateComponentManager[] cmgrsAfterConfigUnconfig = getComponentManagers( holder );
        assertNotNull( "Expect single component manager", cmgrAfterConfigUnconfig );
        assertNotNull( "Expect component manager list", cmgrsAfterConfigUnconfig );
        assertEquals( "Expect one component manager in list", 1, cmgrsAfterConfigUnconfig.length );

        // remove second configuration (leaving no configurations)
        holder.configurationDeleted( pid2 );

        // assert single component and single-entry map
        final ImmediateComponentManager cmgrAfterAllUnconfig = getSingleManager( holder );
        final ImmediateComponentManager[] cmgrsAfterAllUnconfig = getComponentManagers( holder );
        assertNotNull( "Expect single component manager", cmgrAfterAllUnconfig );
        assertNull( "Expect no component manager list", cmgrsAfterAllUnconfig );

    }


    private static ComponentMetadata createComponentMetadata( String name )
    {
        final ComponentMetadata metadata = new ComponentMetadata( XmlHandler.DS_VERSION_1_1 );
        metadata.setName( name );

        return metadata;
    }


    private static ImmediateComponentManager getSingleManager( ConfiguredComponentHolder holder )
    {
        try
        {
            final Field f = ConfiguredComponentHolder.class.getDeclaredField( "m_singleComponent" );
            f.setAccessible( true );
            return ( ImmediateComponentManager ) f.get( holder );
        }
        catch ( Throwable t )
        {
            fail( "Cannot access getComponentManagers method: " + t );
            return null; // compiler does not know about "fail" throwing
        }
    }


    private static ImmediateComponentManager[] getComponentManagers( ConfiguredComponentHolder holder )
    {
        try
        {
            final Method m = ConfiguredComponentHolder.class.getDeclaredMethod( "getComponentManagers", new Class[]
                { Boolean.TYPE } );
            m.setAccessible( true );
            return ( ImmediateComponentManager[] ) m.invoke( holder, new Object[]
                { Boolean.FALSE } );
        }
        catch ( Throwable t )
        {
            fail( "Cannot access getComponentManagers method: " + t );
            return null; // compiler does not know about "fail" throwing
        }
    }

    private static class TestingConfiguredComponentHolder extends ConfiguredComponentHolder
    {
        TestingConfiguredComponentHolder( ComponentMetadata metadata )
        {
            super( null, metadata );
        }


        protected ImmediateComponentManager createComponentManager()
        {
            return new MockImmediateComponentManager( getActivator(), this, getComponentMetadata() );
        }
    }

    private static class MockImmediateComponentManager extends ImmediateComponentManager
    {

        private Dictionary m_configuration;


        public MockImmediateComponentManager( BundleComponentActivator activator, ComponentHolder componentHolder, ComponentMetadata metadata )
        {
            super( activator, componentHolder, metadata );
        }


        Dictionary getConfiguration()
        {
            return m_configuration;
        }


        public boolean hasConfiguration()
        {
            return m_configuration != null;
        }


        public void reconfigure( Dictionary configuration )
        {
            this.m_configuration = configuration;
        }
    }
}
