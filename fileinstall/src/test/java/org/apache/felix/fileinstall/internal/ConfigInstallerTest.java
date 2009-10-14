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
package org.apache.felix.fileinstall.internal;

import java.io.File;
import java.util.Hashtable;
import java.util.Dictionary;

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.easymock.ArgumentsMatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.Configuration;
import org.apache.felix.fileinstall.internal.ConfigInstaller;
import org.apache.felix.fileinstall.internal.FileInstall;

/**
 * Tests for ConfigInstaller
 */
public class ConfigInstallerTest extends TestCase {

    MockControl mockBundleContextControl;
    BundleContext mockBundleContext;
    MockControl mockBundleControl;
    Bundle mockBundle;
    MockControl mockConfigurationAdminControl;
    ConfigurationAdmin mockConfigurationAdmin;
    MockControl mockConfigurationControl;
    Configuration mockConfiguration;

    protected void setUp() throws Exception
    {
        super.setUp();
        mockBundleContextControl = MockControl.createControl( BundleContext.class );
        mockBundleContext = ( BundleContext ) mockBundleContextControl.getMock();
        mockBundleControl = MockControl.createControl( Bundle.class );
        mockBundle = ( Bundle ) mockBundleControl.getMock();
        mockConfigurationAdminControl = MockControl.createControl( ConfigurationAdmin.class );
        mockConfigurationAdmin = ( ConfigurationAdmin ) mockConfigurationAdminControl.getMock();
        mockConfigurationControl = MockControl.createControl( Configuration.class );
        mockConfiguration = ( Configuration ) mockConfigurationControl.getMock();
    }


    public void testParsePidWithoutFactoryPid()
    {
        mockBundleContextControl.replay();
        ConfigInstaller ci = new ConfigInstaller(null, null);

        String path = "pid.cfg";
        assertEquals( "Pid without Factory Pid calculated", "pid", ci.parsePid( path )[0] );
        assertEquals( "Pid without Factory Pid calculated", null, ci.parsePid( path )[1] );
    }


    public void testParsePidWithFactoryPid()
    {
        mockBundleContextControl.replay();
        ConfigInstaller ci = new ConfigInstaller(null, null);

        String path = "factory-pid.cfg";
        assertEquals( "Pid with Factory Pid calculated", "factory", ci.parsePid( path )[0] );
        assertEquals( "Pid with Factory Pid calculated", "pid", ci.parsePid( path )[1] );
    }

    public void testGetNewFactoryConfiguration() throws Exception
    {
        mockConfigurationControl.replay();
        mockConfigurationAdmin.listConfigurations( null );
        mockConfigurationAdminControl.setMatcher( MockControl.ALWAYS_MATCHER );
        mockConfigurationAdminControl.setReturnValue( null );
        mockConfigurationAdmin.createFactoryConfiguration( "pid", null );
        mockConfigurationAdminControl.setReturnValue( mockConfiguration );
        mockConfigurationAdminControl.replay();
        mockBundleContextControl.replay();

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin );

        assertEquals( "Factory configuration retrieved", mockConfiguration, ci.getConfiguration( "pid", "factoryPid" ) );

        mockConfigurationAdminControl.verify();
        mockConfigurationControl.verify();
        mockBundleContextControl.verify();
    }


    public void testGetExistentFactoryConfiguration() throws Exception
    {
        mockConfigurationControl.replay();
        mockConfigurationAdmin.listConfigurations( null );
        mockConfigurationAdminControl.setMatcher( MockControl.ALWAYS_MATCHER );
        mockConfigurationAdminControl.setReturnValue( null );
        mockConfigurationAdmin.createFactoryConfiguration( "pid", null );
        mockConfigurationAdminControl.setReturnValue( mockConfiguration );
        mockConfigurationAdminControl.replay();
        mockBundleContextControl.replay();

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin );

        assertEquals( "Factory configuration retrieved", mockConfiguration, ci.getConfiguration( "pid", "factoryPid" ) );

        mockConfigurationAdminControl.verify();
        mockConfigurationControl.verify();
        mockBundleContextControl.verify();
    }


    public void testGetExistentNoFactoryConfiguration() throws Exception
    {
        mockConfigurationControl.replay();
        mockConfigurationAdmin.listConfigurations( null );
        mockConfigurationAdminControl.setMatcher( MockControl.ALWAYS_MATCHER );
        mockConfigurationAdminControl.setReturnValue( null );
        mockConfigurationAdmin.getConfiguration( "pid", null );
        mockConfigurationAdminControl.setReturnValue( mockConfiguration );
        mockConfigurationAdminControl.replay();
        mockBundleContextControl.replay();

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin );

        assertEquals( "Factory configuration retrieved", mockConfiguration, ci.getConfiguration( "pid", null ) );

        mockConfigurationAdminControl.verify();
        mockConfigurationControl.verify();
        mockBundleContextControl.verify();
    }


    public void testDeleteConfig() throws Exception
    {
        mockConfiguration.delete();
        mockConfigurationControl.replay();
        mockConfigurationAdmin.listConfigurations( null );
        mockConfigurationAdminControl.setMatcher( MockControl.ALWAYS_MATCHER );
        mockConfigurationAdminControl.setReturnValue( null );
        mockConfigurationAdmin.getConfiguration( "pid", null );
        mockConfigurationAdminControl.setReturnValue( mockConfiguration );
        mockConfigurationAdminControl.replay();
        mockBundleContextControl.replay();

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin );

        assertTrue( ci.deleteConfig( new File( "pid.cfg" ) ) );

        mockConfigurationAdminControl.verify();
        mockConfigurationControl.verify();
        mockBundleContextControl.verify();
    }


    public void testSetConfiguration() throws Exception
    {
        mockConfiguration.getBundleLocation();
        mockConfigurationControl.setReturnValue( null );
        mockConfiguration.update( new Hashtable() );
        mockConfigurationControl.setMatcher( new ArgumentsMatcher()
        {
            public boolean matches( Object[] expected, Object[] actual )
            {
                return ( actual.length == 1 ) && ( (Dictionary) actual[0] ).get( "testkey" ).equals( "testvalue" );
            }


            public String toString( Object[] arg0 )
            {
                return arg0.toString();
            }
        } );
        mockConfigurationControl.replay();
        mockConfigurationAdmin.listConfigurations( null );
        mockConfigurationAdminControl.setMatcher( MockControl.ALWAYS_MATCHER );
        mockConfigurationAdminControl.setReturnValue( null );
        mockConfigurationAdmin.getConfiguration( "firstcfg", null );
        mockConfigurationAdminControl.setReturnValue( mockConfiguration );
        mockConfigurationAdminControl.replay();
        mockBundleContextControl.replay();

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin );

        assertTrue( ci.setConfig( new File( "src/test/resources/watched/firstcfg.cfg" ) ) );

        mockConfigurationAdminControl.verify();
        mockConfigurationControl.verify();
        mockBundleContextControl.verify();
    }


}
