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
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
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


    protected void setUp() throws Exception
    {
        super.setUp();
        mockBundleContextControl = MockControl.createControl( BundleContext.class );
        mockBundleContext = ( BundleContext ) mockBundleContextControl.getMock();
        mockPackageAdminControl = MockControl.createControl( PackageAdmin.class );
        mockPackageAdmin = ( PackageAdmin ) mockPackageAdminControl.getMock();
        mockBundleControl = MockControl.createControl( Bundle.class );
        mockBundle = ( Bundle ) mockBundleControl.getMock();
        props.put( DirectoryWatcher.DIR, new File( "target/load" ).getAbsolutePath() );
    }


    public void testGetLongWithNonExistentProperty()
    {
        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );
        assertEquals( "getLong gives the default value for non-existing properties", 100, dw.getLong( props, TEST, 100 ) );
    }


    public void testGetLongWithExistentProperty()
    {
        props.put( TEST, "33" );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );
        assertEquals( "getLong retrieves the right property value", 33, dw.getLong( props, TEST, 100 ) );
    }


    public void testGetLongWithIncorrectValue()
    {
        props.put( TEST, "incorrect" );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        mockBundleContext.getServiceReference( "org.osgi.service.log.LogService" );
        mockBundleContextControl.setReturnValue( null );
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );
        assertEquals( "getLong retrieves the right property value", 100, dw.getLong( props, TEST, 100 ) );
    }


    public void testGetBooleanWithNonExistentProperty()
    {
        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );
        assertEquals( "getBoolean gives the default value for non-existing properties", true, dw.getBoolean( props, TEST, true ) );
    }


    public void testGetBooleanWithExistentProperty()
    {
        props.put( TEST, "true" );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );
        assertEquals( "getBoolean retrieves the right property value", true, dw.getBoolean( props, TEST, false ) );
    }


    public void testGetBooleanWithIncorrectValue()
    {
        props.put( TEST, "incorrect" );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        mockBundleContext.getServiceReference( "org.osgi.service.log.LogService" );
        mockBundleContextControl.setReturnValue( null );
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );
        assertEquals( "getBoolean retrieves the right property value", false, dw.getBoolean( props, TEST, true ) );
    }


    public void testGetFileWithNonExistentProperty()
    {
        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );
        assertEquals( "getFile gives the default value for non-existing properties", new File("tmp"), dw.getFile( props, TEST, new File("tmp") ) );
    }


    public void testGetFileWithExistentProperty()
    {
        props.put( TEST, "test" );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );
        assertEquals( "getBoolean retrieves the right property value", new File("test"), dw.getFile( props, TEST, new File("tmp") ) );
    }


    public void testParameterAfterInitialization()
    {
        props.put( DirectoryWatcher.POLL, "500" );
        props.put( DirectoryWatcher.LOG_LEVEL, "1" );
        props.put( DirectoryWatcher.START_NEW_BUNDLES, "false" );
        props.put( DirectoryWatcher.DIR, new File( "src/test/resources" ).getAbsolutePath() );
        props.put( DirectoryWatcher.TMPDIR, new File( "src/test/resources" ).getAbsolutePath() );
        props.put( DirectoryWatcher.FILTER, ".*\\.cfg" );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );

        assertEquals( "POLL parameter correctly read", 500l, dw.poll );
        assertEquals( "LOG_LEVEL parameter correctly read", 1, dw.logLevel );
        assertTrue( "DIR parameter correctly read", dw.watchedDirectory.getAbsolutePath().endsWith(
            "src" + File.separatorChar + "test" + File.separatorChar + "resources" ) );
        assertTrue( "TMPDIR parameter correctly read", dw.tmpDir.getAbsolutePath().endsWith(
            "src" + File.separatorChar + "test" + File.separatorChar + "resources" ) );
        assertEquals( "START_NEW_BUNDLES parameter correctly read", false, dw.startBundles );
        assertEquals( "FILTER parameter correctly read", ".*\\.cfg", dw.filter );
    }


    public void testDefaultParametersAreSetAfterEmptyInitialization()
    {
        props.put( DirectoryWatcher.DIR, new File( "src/test/resources" ).getAbsolutePath() );

        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
        mockBundleContextControl.replay();
        dw = new DirectoryWatcher( props, mockBundleContext );

        assertTrue( "DIR parameter correctly read", dw.watchedDirectory.getAbsolutePath().endsWith(
            "src" + File.separatorChar + "test" + File.separatorChar + "resources" ) );
        assertEquals( "Default POLL parameter correctly read", 2000l, dw.poll );
        assertEquals( "Default LOG_LEVEL parameter correctly read", 0, dw.logLevel );
        assertTrue( "Default TMPDIR parameter correctly read", dw.tmpDir.getAbsolutePath().startsWith(
                new File(System.getProperty("java.io.tmpdir")).getAbsolutePath()) );
        assertEquals( "Default START_NEW_BUNDLES parameter correctly read", true, dw.startBundles );
        assertEquals( "Default FILTER parameter correctly read", null, dw.filter );
    }


    public void testIsFragment() throws Exception
    {
        mockBundleContext.addBundleListener((BundleListener) org.easymock.EasyMock.anyObject());
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


}
