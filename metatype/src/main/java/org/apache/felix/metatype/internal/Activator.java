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


import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.felix.metatype.internal.l10n.BundleResources;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>Activator</code> class is the <code>BundleActivator</code> of
 * this bundle and provides abstract logging functionality: If a
 * <code>LogService</code> is available, that service is used, otherwise
 * logging goes to standard output or standard error (in case of level ERROR
 * messages).
 *
 * @author fmeschbe
 */
public class Activator implements BundleActivator
{

    /** The name of the log service. */
    private static final String NAME_LOG_SERVICE = LogService.class.getName();

    /**
     * A <code>SimpleDateFormat</code> object to format the a log message time
     * stamp in case of logging to standard output/error (value of format
     * pattern is "dd.MM.yyyy HH:mm:ss").
     */
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat( "dd.MM.yyyy HH:mm:ss" );

    /**
     * The (singleton) instance of this activator. Used by the log methods to
     * access the {@link #logService} field.
     */
    private static Activator INSTANCE;

    /**
     * The <code>LogService</code> used to log messages. If a log service is
     * not available in the framework, this field is <code>null</code>.
     *
     * @see #start(BundleContext)
     * @see #serviceChanged(ServiceEvent)
     */
    private ServiceTracker logService;

    /*
     * Set the static INSTANCE field to this new instance
     */
    {
        INSTANCE = this;
    }


    /**
     * Starts this bundle doing the following:
     * <ol>
     * <li>Register as listener for service events concerning the
     *      <code>LogService</code>
     * <li>Try to get the <code>LogService</code>
     * <li>Registers the <code>MetaTypeService</code> implementation provided
     *      by this bundle.
     * </ol>
     *
     * @param context The <code>BundleContext</code> of this activator's bundle
     */
    public void start( BundleContext context )
    {
        // register for log service events
        logService = new ServiceTracker( context, NAME_LOG_SERVICE, null );
        logService.open();

        // register the MetaTypeService now, that we are ready
        Dictionary props = new Hashtable();
        props.put( Constants.SERVICE_PID, "org.apache.felix.metatype.MetaTypeService" );
        props.put( Constants.SERVICE_DESCRIPTION, "MetaTypeService Specification 1.1 Implementation" );
        props.put( Constants.SERVICE_VENDOR, "Apache Software Foundation" );
        MetaTypeService metaTypeService = new MetaTypeServiceImpl( context );
        context.registerService( MetaTypeService.class.getName(), metaTypeService, props );
    }


    /**
     * Stops this bundle by just unregistering as a service listener.
     * <p>
     * The framework will take care of ungetting the <code>LogService</code> and
     * unregistering the <code>MetaTypeService</code> registered by the
     * {@link #start(BundleContext)} method.
     *
     * @param context The <code>BundleContext</code> of this activator's bundle
     */
    public void stop( BundleContext context )
    {
        logService.close();

        // make sure the static BundleResources cache does not block the class laoder
        BundleResources.clearResourcesCache();
    }

    //---------- Logging Support ----------------------------------------------
    // log to stdout or use LogService

    public static void log( int level, String message )
    {
        LogService log = ( LogService ) INSTANCE.logService.getService();
        if ( log == null )
        {
            _log( null, level, message, null );
        }
        else
        {
            log.log( level, message );
        }
    }


    public static void log( int level, String message, Throwable exception )
    {
        LogService log = ( LogService ) INSTANCE.logService.getService();
        if ( log == null )
        {
            _log( null, level, message, exception );
        }
        else
        {
            log.log( level, message, exception );
        }
    }


    public static void log( ServiceReference sr, int level, String message )
    {
        LogService log = ( LogService ) INSTANCE.logService.getService();
        if ( log == null )
        {
            _log( sr, level, message, null );
        }
        else
        {
            log.log( sr, level, message );
        }
    }


    public static void log( ServiceReference sr, int level, String message, Throwable exception )
    {
        LogService log = ( LogService ) INSTANCE.logService.getService();
        if ( log == null )
        {
            _log( sr, level, message, exception );
        }
        else
        {
            log.log( sr, level, message, exception );
        }
    }


    //---------- Helper Methods -----------------------------------------------

    private static void _log( ServiceReference sr, int level, String message, Throwable exception )
    {
        String time = getTimeStamp();

        StringBuffer buf = new StringBuffer( time );
        buf.append( ' ' ).append( toLevelString( level ) ).append( ' ' );
        buf.append( message );

        if ( sr != null )
        {
            String name = ( String ) sr.getProperty( Constants.SERVICE_PID );
            if ( name == null )
            {
                name = ( ( String[] ) sr.getProperty( Constants.OBJECTCLASS ) )[0];
            }
            buf.append( " (" ).append( name ).append( ", service.id=" ).append( sr.getProperty( Constants.SERVICE_ID ) )
                .append( ')' );
        }

        PrintStream dst = ( level == LogService.LOG_ERROR ) ? System.err : System.out;
        dst.println( buf );

        if ( exception != null )
        {
            buf = new StringBuffer( time );
            buf.append( ' ' ).append( toLevelString( level ) ).append( ' ' );
            dst.print( buf );
            exception.printStackTrace( dst );
        }
    }


    private static String getTimeStamp()
    {
        synchronized ( FORMAT )
        {
            return FORMAT.format( new Date() );
        }
    }


    private static String toLevelString( int level )
    {
        switch ( level )
        {
            case LogService.LOG_DEBUG:
                return "*DEBUG*";
            case LogService.LOG_INFO:
                return "*INFO *";
            case LogService.LOG_WARNING:
                return "*WARN *";
            case LogService.LOG_ERROR:
                return "*ERROR*";
            default:
                return "*" + level + "*";
        }

    }
}
