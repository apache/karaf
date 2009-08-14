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
package org.apache.felix.cm.integration;


import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanDir;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.withBnd;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import junit.framework.TestCase;

import org.apache.felix.cm.integration.helper.MyTinyBundle;
import org.apache.felix.cm.integration.helper.TestActivator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;


@RunWith(JUnit4TestRunner.class)
public class ConfigurationTest
{

    @Inject
    private BundleContext bundleContext;

    private Bundle bundle;

    private ServiceTracker configAdminTracker;

    private static final String PROP_NAME = "theValue";
    private static final Dictionary<String, String> theConfig;

    static
    {
        theConfig = new Hashtable<String, String>();
        theConfig.put( PROP_NAME, PROP_NAME );
    }


    @org.ops4j.pax.exam.junit.Configuration
    public static Option[] configuration()
    {
        return options(
            provision(
                scanDir( "target" ).filter( "*.jar" ),
                mavenBundle( "org.ops4j.pax.swissbox", "pax-swissbox-tinybundles", "1.0.0" )
            )
//          , PaxRunnerOptions.vmOption( "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=30303" )
        );
    }


    @Before
    public void setUp()
    {
        configAdminTracker = new ServiceTracker( bundleContext, ConfigurationAdmin.class.getName(), null );
        configAdminTracker.open();
    }


    @After
    public void tearDown() throws BundleException
    {
        if (bundle != null) {
            bundle.uninstall();
        }

        configAdminTracker.close();
        configAdminTracker = null;
    }


    @Test
    public void test_configuration_unbound_on_uninstall() throws IOException, BundleException
    {
        String pid = "test.pid";
        configure( pid );

        // ensure configuration is unbound
        final Configuration beforeInstall = getConfiguration( pid );
        TestCase.assertNull( beforeInstall.getBundleLocation() );

        bundle = installBundle( pid );

        // ensure no configuration bound before start
        final Configuration beforeStart = getConfiguration( pid );
        TestCase.assertNull( beforeInstall.getBundleLocation() );
        TestCase.assertNull( beforeStart.getBundleLocation() );

        bundle.start();
        final TestActivator tester = TestActivator.INSTANCE;
        TestCase.assertNotNull( "Activator not started !!", tester );

        // give cm time for distribution
        delay();

        // ensure a freshly retrieved object also has the location
        final Configuration beforeStop = getConfiguration( pid );
        TestCase.assertEquals( beforeStop.getBundleLocation(), bundle.getLocation() );

        // check whether bundle context is set on first configuration
        TestCase.assertEquals( beforeInstall.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStart.getBundleLocation(), bundle.getLocation() );

        bundle.stop();

        // ensure configuration still bound
        TestCase.assertEquals( beforeInstall.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStart.getBundleLocation(), bundle.getLocation() );
        TestCase.assertEquals( beforeStop.getBundleLocation(), bundle.getLocation() );

        // ensure a freshly retrieved object also has the location
        final Configuration beforeUninstall = getConfiguration( pid );
        TestCase.assertEquals( beforeUninstall.getBundleLocation(), bundle.getLocation() );

        bundle.uninstall();
        bundle = null;

        // ensure configuration is not bound any more
        TestCase.assertNull( beforeInstall.getBundleLocation() );
        TestCase.assertNull( beforeStart.getBundleLocation() );
        TestCase.assertNull( beforeStop.getBundleLocation() );
        TestCase.assertNull( beforeUninstall.getBundleLocation() );

        // ensure a freshly retrieved object also does not have the location
        final Configuration atEnd = getConfiguration( pid );
        TestCase.assertNull( atEnd.getBundleLocation() );
    }


    private Bundle installBundle( final String pid ) throws BundleException {
        final InputStream bundleStream = new MyTinyBundle()
            .prepare(
                withBnd()
                .set( Constants.BUNDLE_SYMBOLICNAME, "simpleconfiguration" )
                .set( Constants.BUNDLE_VERSION, "0.0.11" )
                .set( Constants.IMPORT_PACKAGE, "org.apache.felix.cm.integration.helper" )
                .set( Constants.BUNDLE_ACTIVATOR, "org.apache.felix.cm.integration.helper.TestActivator" )
                .set( TestActivator.HEADER_PID, pid )
            )
            .build( TinyBundles.asStream() );

        try {
            return bundleContext.installBundle( "test:SimpleComponent", bundleStream );
        } finally {
            try {
                bundleStream.close();
            } catch (IOException ioe) {
            }
        }
    }

    private static void delay()
    {
        try
        {
            Thread.sleep( 300 );
        }
        catch ( InterruptedException ie )
        {
            // dont care
        }
    }


    private ConfigurationAdmin getConfigurationAdmin()
    {
        ConfigurationAdmin ca = ( ConfigurationAdmin ) configAdminTracker.getService();
        if ( ca == null )
        {
            TestCase.fail( "Missing ConfigurationAdmin service" );
        }
        return ca;
    }


    private void configure( String pid )
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            org.osgi.service.cm.Configuration config = ca.getConfiguration( pid, null );
            config.update( theConfig );
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed updating configuration " + pid + ": " + ioe.toString() );
        }
    }

    private Configuration getConfiguration( final String pid )
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            final String filter = "(" + Constants.SERVICE_PID + "=" + pid + ")";
            org.osgi.service.cm.Configuration[] configs = ca.listConfigurations( filter );
            if ( configs != null && configs.length > 0) {
                return configs[0];
            }
        }
        catch ( InvalidSyntaxException ise )
        {
            // unexpected
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed listing configurations " + pid + ": " + ioe.toString() );
        }

        TestCase.fail("No Configuration " + pid + " found");
        return null;
    }


    private void deleteConfig( String pid )
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            org.osgi.service.cm.Configuration config = ca.getConfiguration( pid );
            config.delete();
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed deleting configuration " + pid + ": " + ioe.toString() );
        }
    }


    private String createFactoryConfiguration( String factoryPid )
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            org.osgi.service.cm.Configuration config = ca.createFactoryConfiguration( factoryPid, null );
            config.update( theConfig );
            return config.getPid();
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed updating factory configuration " + factoryPid + ": " + ioe.toString() );
            return null;
        }
    }


    private void deleteFactoryConfigurations( String factoryPid )
    {
        ConfigurationAdmin ca = getConfigurationAdmin();
        try
        {
            final String filter = "(service.factoryPid=" + factoryPid + ")";
            org.osgi.service.cm.Configuration[] configs = ca.listConfigurations( filter );
            if ( configs != null )
            {
                for ( org.osgi.service.cm.Configuration configuration : configs )
                {
                    configuration.delete();
                }
            }
        }
        catch ( InvalidSyntaxException ise )
        {
            // unexpected
        }
        catch ( IOException ioe )
        {
            TestCase.fail( "Failed deleting configurations " + factoryPid + ": " + ioe.toString() );
        }
    }
}
