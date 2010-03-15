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
package org.apache.felix.webconsole.internal.system;


import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.startlevel.StartLevel;


/**
 * VMStatPlugin provides the System Information tab. This particular plugin uses
 * more than one templates.
 */
public class VMStatPlugin extends SimpleWebConsolePlugin implements OsgiManagerPlugin
{

    private static final String LABEL = "vmstat";
    private static final String TITLE = "%vmstat.pluginTitle";
    private static final String CSS[] = null;

    private static final String ATTR_TERMINATED = "terminated";

    private static final String PARAM_SHUTDOWN_TIMER = "shutdown_timer";
    private static final String PARAM_SHUTDOWN_TYPE = "shutdown_type";
    private static final String PARAM_SHUTDOWN_TYPE_RESTART = "Restart";
    //private static final String PARAM_SHUTDOWN_TYPE_STOP = "Stop";

    private static final long startDate = ( new Date() ).getTime();

    // from BaseWebConsolePlugin
    private static String START_LEVEL_NAME = StartLevel.class.getName();

    // templates
    private final String TPL_VM_MAIN;
    private final String TPL_VM_STOP;
    private final String TPL_VM_RESTART;


    /** Default constructor */
    public VMStatPlugin()
    {
        super( LABEL, TITLE, CSS );

        // load templates
        TPL_VM_MAIN = readTemplateFile(  "/templates/vmstat.html"  );
        TPL_VM_STOP = readTemplateFile( "/templates/vmstat_stop.html" );
        TPL_VM_RESTART = readTemplateFile( "/templates/vmstat_restart.html" );
    }


    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        final String action = request.getParameter( "action");

        if ( "setStartLevel".equals( action ))
        {
            StartLevel sl = getStartLevel();
            if ( sl != null )
            {
                int bundleSL = WebConsoleUtil.getParameterInt( request, "bundleStartLevel", -1 );
                if ( bundleSL > 0 && bundleSL != sl.getInitialBundleStartLevel() )
                {
                    sl.setInitialBundleStartLevel( bundleSL );
                }

                int systemSL = WebConsoleUtil.getParameterInt( request, "systemStartLevel", -1 );
                if ( systemSL > 0 && systemSL != sl.getStartLevel() )
                {
                    sl.setStartLevel( systemSL );
                }
            }
        }
        else if ( "gc".equals( action ) )
        {
            System.gc();
            System.gc(); // twice for sure
        }
        else if ( request.getParameter( PARAM_SHUTDOWN_TIMER ) == null )        
        {

            // whether to stop or restart the framework
            final boolean restart = PARAM_SHUTDOWN_TYPE_RESTART.equals( request.getParameter( PARAM_SHUTDOWN_TYPE ) );

            // simply terminate VM in case of shutdown :-)
            final Bundle systemBundle = getBundleContext().getBundle( 0 );
            Thread t = new Thread( "Stopper" )
            {
                public void run()
                {
                    try
                    {
                        Thread.sleep( 2000L );
                    }
                    catch ( InterruptedException ie )
                    {
                        // ignore
                    }

                    log( "Shutting down server now!" );

                    // stopping bundle 0 (system bundle) stops the framework
                    try
                    {
                        if ( restart )
                        {
                            systemBundle.update();
                        }
                        else
                        {
                            systemBundle.stop();
                        }
                    }
                    catch ( BundleException be )
                    {
                        log( "Problem stopping or restarting the Framework", be );
                    }
                }
            };
            t.start();

            request.setAttribute( ATTR_TERMINATED, ATTR_TERMINATED );
            request.setAttribute( PARAM_SHUTDOWN_TYPE, new Boolean( restart ) );
        }

        // render the response without redirecting
        doGet( request, response );
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        String body;

        if ( request.getAttribute( ATTR_TERMINATED ) != null )
        {
            Object restart = request.getAttribute( PARAM_SHUTDOWN_TYPE );
            if ( ( restart instanceof Boolean ) && ( ( Boolean ) restart ).booleanValue() )
            {
                body = TPL_VM_RESTART;
            }
            else
            {
                body = TPL_VM_STOP;
            }
            response.getWriter().print( body );
            return;
        }

        body = TPL_VM_MAIN;

        long freeMem = Runtime.getRuntime().freeMemory() / 1024;
        long totalMem = Runtime.getRuntime().totalMemory() / 1024;
        long usedMem = totalMem - freeMem;

        boolean shutdownTimer = request.getParameter( PARAM_SHUTDOWN_TIMER ) != null;
        String shutdownType = request.getParameter( PARAM_SHUTDOWN_TYPE );
        if ( shutdownType == null )
            shutdownType = "";

        JSONObject json = new JSONObject();
        try
        {
            json.put( "systemStartLevel", getStartLevel().getStartLevel() );
            json.put( "bundleStartLevel", getStartLevel().getInitialBundleStartLevel() );
            json.put( "lastStarted", startDate );
            json.put( "runtime", System.getProperty( "java.runtime.name" ) + "(build "
                + System.getProperty( "java.runtime.version" ) + ")" );
            json.put( "jvm", System.getProperty( "java.vm.name" ) + "(build " + System.getProperty( "java.vm.version" )
                + ", " + System.getProperty( "java.vm.info" ) + ")" );
            json.put( "shutdownTimer", shutdownTimer );
            json.put( "mem_total", totalMem );
            json.put( "mem_free", freeMem );
            json.put( "mem_used", usedMem );
            json.put( "shutdownType", shutdownType );
        }
        catch ( JSONException e )
        {
            throw new IOException( e.toString() );
        }

        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "startData", json.toString() );

        response.getWriter().print( body );
    }


    private final StartLevel getStartLevel()
    {
        return ( StartLevel ) getService( START_LEVEL_NAME );
    }
}
