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
package org.apache.felix.fileinstall;


import java.io.File;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import junit.framework.TestCase;

import org.easymock.ArgumentsMatcher;
import org.easymock.MockControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.packageadmin.PackageAdmin;


/**
 * Test class for the DirectoryWatcher
 */
public class DirectoryWatcherTest extends TestCase
{

    private final static String TEST = "test.key";
    Dictionary props = new Hashtable();
    DirectoryWatcher dw;
    MockControl mockBundleContextControl;
    BundleContext mockBundleContext;
    MockControl mockPackageAdminControl;
    PackageAdmin mockPackageAdmin;
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
        mockPackageAdminControl = MockControl.createControl( PackageAdmin.class );
        mockPackageAdmin = ( PackageAdmin ) mockPackageAdminControl.getMock();
        mockBundleControl = MockControl.createControl( Bundle.class );
        mockBundle = ( Bundle ) mockBundleControl.getMock();
        mockConfigurationAdminControl = MockControl.createControl( ConfigurationAdmin.class );
        mockConfigurationAdmin = ( ConfigurationAdmin ) mockConfigurationAdminControl.getMock();
        mockConfigurationControl = MockControl.createControl( Configuration.class );
        mockConfiguration = ( Configuration ) mockConfigurationControl.getMock();
    }


    public void testGetLongWithNonExistentProperty()
    {
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );
        assertEquals( "getLong gives the default value for non-existing properties", 100, dw.getLong( props, TEST, 100 ) );
    }


    public void testGetLongWithExistentProperty()
    {
        props.put( TEST, "33" );
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );
        assertEquals( "getLong retrieves the right property value", 33, dw.getLong( props, TEST, 100 ) );
    }


    public void testGetLongWithIncorrectValue()
    {
        props.put( TEST, "incorrect" );

        mockBundleContext.getServiceReference( "org.osgi.service.log.LogService" );
        mockBundleContextControl.setReturnValue( null );
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );
        assertEquals( "getLong retrieves the right property value", 100, dw.getLong( props, TEST, 100 ) );
    }


    public void testParameterAfterInitialization()
    {
        props.put( DirectoryWatcher.POLL, "500" );
        props.put( DirectoryWatcher.DEBUG, "1" );
        props.put( DirectoryWatcher.START_NEW_BUNDLES, "false" );
        props.put( DirectoryWatcher.DIR, new File( "src/test/resources" ).getAbsolutePath() );
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );

        assertEquals( "POLL parameter correctly read", 500l, dw.poll );
        assertEquals( "DEBUG parameter correctly read", 1l, dw.debug );
        assertTrue( "DIR parameter correctly read", dw.watchedDirectory.getAbsolutePath().endsWith(
            "src" + File.separatorChar + "test" + File.separatorChar + "resources" ) );
        assertEquals( "START_NEW_BUNDLES parameter correctly read", false, dw.startBundles );
    }


    public void testDefaultParametersAreSetAfterEmptyInitialization()
    {
        props.put( DirectoryWatcher.DIR, new File( "src/test/resources" ).getAbsolutePath() );
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );

        assertEquals( "Default POLL parameter correctly read", 2000l, dw.poll );
        assertEquals( "Default DEBUG parameter correctly read", -1l, dw.debug );
        assertEquals( "Default START_NEW_BUNDLES parameter correctly read", true, dw.startBundles );
    }


    public void testParsePidWithoutFactoryPid()
    {
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );
        String path = "pid.cfg";
        assertEquals( "Pid without Factory Pid calculated", "pid", dw.parsePid( path )[0] );
        assertEquals( "Pid without Factory Pid calculated", null, dw.parsePid( path )[1] );
    }


    public void testParsePidWithFactoryPid()
    {
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );

        String path = "factory-pid.cfg";
        assertEquals( "Pid with Factory Pid calculated", "factory", dw.parsePid( path )[0] );
        assertEquals( "Pid with Factory Pid calculated", "pid", dw.parsePid( path )[1] );
    }


    public void testIsFragment() throws Exception
    {
        mockBundleContext.createFilter( "" );
        mockBundleContextControl.setMatcher( MockControl.ALWAYS_MATCHER );
        mockBundleContextControl.setReturnValue( null );
        mockBundleContextControl.replay();
        mockPackageAdmin.getBundleType( mockBundle );
        mockPackageAdminControl.setReturnValue( new Long(PackageAdmin.BUNDLE_TYPE_FRAGMENT) );
        mockPackageAdminControl.replay();
        mockBundleControl.replay();

        FileInstall.padmin = new MockServiceTracker( mockBundleContext, mockPackageAdmin );
        dw = new DirectoryWatcher( props, mockBundleContext );

        assertTrue( "Fragment type correctly retrieved from Package Admin service", dw.isFragment( mockBundle ) );

        mockPackageAdminControl.verify();
        mockBundleContextControl.verify();
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
        mockBundleContext.createFilter( "" );
        mockBundleContextControl.setMatcher( MockControl.ALWAYS_MATCHER );
        mockBundleContextControl.setReturnValue( null );
        mockBundleContextControl.replay();

        FileInstall.cmTracker = new MockServiceTracker( mockBundleContext, mockConfigurationAdmin );
        dw = new DirectoryWatcher( props, mockBundleContext );

        assertEquals( "Factory configuration retrieved", mockConfiguration, dw.getConfiguration( "pid", "factoryPid" ) );

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
        mockBundleContext.createFilter( "" );
        mockBundleContextControl.setMatcher( MockControl.ALWAYS_MATCHER );
        mockBundleContextControl.setReturnValue( null );
        mockBundleContextControl.replay();

        FileInstall.cmTracker = new MockServiceTracker( mockBundleContext, mockConfigurationAdmin );
        dw = new DirectoryWatcher( props, mockBundleContext );

        assertEquals( "Factory configuration retrieved", mockConfiguration, dw.getConfiguration( "pid", "factoryPid" ) );

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
        mockBundleContext.createFilter( "" );
        mockBundleContextControl.setMatcher( MockControl.ALWAYS_MATCHER );
        mockBundleContextControl.setReturnValue( null );
        mockBundleContextControl.replay();

        FileInstall.cmTracker = new MockServiceTracker( mockBundleContext, mockConfigurationAdmin );
        dw = new DirectoryWatcher( props, mockBundleContext );

        assertEquals( "Factory configuration retrieved", mockConfiguration, dw.getConfiguration( "pid", null ) );

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
        mockBundleContext.createFilter( "" );
        mockBundleContextControl.setMatcher( MockControl.ALWAYS_MATCHER );
        mockBundleContextControl.setReturnValue( null );
        mockBundleContextControl.replay();

        FileInstall.cmTracker = new MockServiceTracker( mockBundleContext, mockConfigurationAdmin );
        dw = new DirectoryWatcher( props, mockBundleContext );

        assertTrue( dw.deleteConfig( new File( "pid.cfg" ) ) );

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
                return ( actual.length == 1 ) && ( ( Dictionary ) actual[0] ).get( "testkey" ).equals( "testvalue" );
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
        mockBundleContext.createFilter( "" );
        mockBundleContextControl.setMatcher( MockControl.ALWAYS_MATCHER );
        mockBundleContextControl.setReturnValue( null );
        mockBundleContextControl.replay();

        FileInstall.cmTracker = new MockServiceTracker( mockBundleContext, mockConfigurationAdmin );
        dw = new DirectoryWatcher( props, mockBundleContext );

        assertTrue( dw.setConfig( new File( "src/test/resources/watched/firstcfg.cfg" ) ) );

        mockConfigurationAdminControl.verify();
        mockConfigurationControl.verify();
        mockBundleContextControl.verify();
    }

}
