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
package org.apache.felix.cm.impl;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;

import junit.framework.TestCase;

import org.apache.felix.cm.MockBundleContext;
import org.apache.felix.cm.MockLogService;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


public class ConfigurationManagerTest extends TestCase
{

    private PrintStream replacedStdErr;

    private ByteArrayOutputStream output;


    protected void setUp() throws Exception
    {
        super.setUp();

        replacedStdErr = System.err;

        output = new ByteArrayOutputStream();
        System.setErr( new PrintStream( output ) );
    }


    protected void tearDown() throws Exception
    {
        System.setErr( replacedStdErr );

        super.tearDown();
    }


    public void testLogNoLogService()
    {
        ConfigurationManager configMgr = createConfigurationManager( null );

        setLogLevel( configMgr, LogService.LOG_WARNING );
        assertNoLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertNoLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( configMgr, LogService.LOG_ERROR );
        assertNoLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertNoLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertNoLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        // lower than error -- no output
        setLogLevel( configMgr, LogService.LOG_ERROR - 1 );
        assertNoLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertNoLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertNoLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertNoLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        // minimal log level -- no output
        setLogLevel( configMgr, Integer.MIN_VALUE );
        assertNoLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertNoLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertNoLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertNoLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( configMgr, LogService.LOG_INFO );
        assertNoLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( configMgr, LogService.LOG_DEBUG );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        // maximal log level -- all output
        setLogLevel( configMgr, Integer.MAX_VALUE );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );
    }


    // this test always expects output since when using a LogService, the log
    // level property is ignored
    public void testLogWithLogService()
    {
        LogService logService = new MockLogService();
        ConfigurationManager configMgr = createConfigurationManager( logService );

        setLogLevel( configMgr, LogService.LOG_WARNING );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( configMgr, LogService.LOG_ERROR );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( configMgr, LogService.LOG_ERROR - 1 );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( configMgr, Integer.MIN_VALUE );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( configMgr, LogService.LOG_INFO );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( configMgr, LogService.LOG_DEBUG );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );

        setLogLevel( configMgr, Integer.MAX_VALUE );
        assertLog( configMgr, LogService.LOG_DEBUG, "Debug Test Message", null );
        assertLog( configMgr, LogService.LOG_INFO, "Info Test Message", null );
        assertLog( configMgr, LogService.LOG_WARNING, "Warning Test Message", null );
        assertLog( configMgr, LogService.LOG_ERROR, "Error Test Message", null );
    }


    public void testLogSetup()
    {
        final MockBundleContext bundleContext = new MockBundleContext();
        ConfigurationManager configMgr = createConfigurationManager( null );

        // ensure the configuration data goes to target
        bundleContext.setProperty( "felix.cm.dir", "target/config" );
        
        // default value is 2
        bundleContext.setProperty( "felix.cm.loglevel", null );
        configMgr.start( bundleContext );
        assertEquals( 2, getLogLevel( configMgr ) );
        configMgr.stop( bundleContext );

        // illegal number yields default value
        bundleContext.setProperty( "felix.cm.loglevel", "not-a-number" );
        configMgr.start( bundleContext );
        assertEquals( 2, getLogLevel( configMgr ) );
        configMgr.stop( bundleContext );

        bundleContext.setProperty( "felix.cm.loglevel", "-100" );
        configMgr.start( bundleContext );
        assertEquals( -100, getLogLevel( configMgr ) );
        configMgr.stop( bundleContext );

        bundleContext.setProperty( "felix.cm.loglevel", "4" );
        configMgr.start( bundleContext );
        assertEquals( 4, getLogLevel( configMgr ) );
        configMgr.stop( bundleContext );
    }


    private void assertNoLog( ConfigurationManager configMgr, int level, String message, Throwable t )
    {
        try
        {
            configMgr.log( level, message, t );
            assertTrue( "Expecting no log output", output.size() == 0 );
        }
        finally
        {
            // clear the output for future data
            output.reset();
        }
    }


    private void assertLog( ConfigurationManager configMgr, int level, String message, Throwable t )
    {
        try
        {
            configMgr.log( level, message, t );
            assertTrue( "Expecting log output", output.size() > 0 );

            final String expectedLog = MockLogService.toMessageLine( level, message );
            final String actualLog = new String( output.toByteArray() );
            assertEquals( "Log Message not correct", expectedLog, actualLog );

        }
        finally
        {
            // clear the output for future data
            output.reset();
        }
    }


    private static void setLogLevel( ConfigurationManager configMgr, int level )
    {
        final String fieldName = "logLevel";
        try
        {
            Field field = configMgr.getClass().getDeclaredField( fieldName );
            field.setAccessible( true );
            field.setInt( configMgr, level );
        }
        catch ( Throwable ignore )
        {
            throw ( IllegalArgumentException ) new IllegalArgumentException( "Cannot set logLevel field value" )
                .initCause( ignore );
        }
    }


    private static int getLogLevel( ConfigurationManager configMgr )
    {
        final String fieldName = "logLevel";
        try
        {
            Field field = configMgr.getClass().getDeclaredField( fieldName );
            field.setAccessible( true );
            return field.getInt( configMgr );
        }
        catch ( Throwable ignore )
        {
            throw ( IllegalArgumentException ) new IllegalArgumentException( "Cannot get logLevel field value" )
                .initCause( ignore );
        }
    }


    private static ConfigurationManager createConfigurationManager( final LogService logService )
    {
        ConfigurationManager configMgr = new ConfigurationManager();

        try
        {
            Field field = configMgr.getClass().getDeclaredField( "logTracker" );
            field.setAccessible( true );
            field.set( configMgr, new ServiceTracker( new MockBundleContext(), "", null )
            {
                public Object getService()
                {
                    return logService;
                }
            } );
        }
        catch ( Throwable ignore )
        {
            throw ( IllegalArgumentException ) new IllegalArgumentException( "Cannot set logTracker field value" )
                .initCause( ignore );
        }

        return configMgr;
    }
}
