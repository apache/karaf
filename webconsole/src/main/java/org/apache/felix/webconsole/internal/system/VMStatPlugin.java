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
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.internal.BaseWebConsolePlugin;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.internal.core.SetStartLevelAction;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogService;


public class VMStatPlugin extends BaseWebConsolePlugin
{

    public static final String LABEL = "vmstat";

    public static final String TITLE = "System Information";

    private static final String ATTR_TERMINATED = "terminated";

    private static final String PARAM_SHUTDOWN_TIMER = "shutdown_timer";
    private static final String PARAM_SHUTDOWN_TYPE = "shutdown_type";

    private static final String PARAM_SHUTDOWN_TYPE_RESTART = "Restart";
    private static final String PARAM_SHUTDOWN_TYPE_STOP = "Stop";

    private static final long startDate = ( new Date() ).getTime();


    public String getLabel()
    {
        return LABEL;
    }


    public String getTitle()
    {
        return TITLE;
    }


    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        if ( request.getParameter( PARAM_SHUTDOWN_TIMER ) == null )
        {

            // whether to stop or restart the framework
            final boolean restart = PARAM_SHUTDOWN_TYPE_RESTART.equals( request.getParameter( PARAM_SHUTDOWN_TYPE ) );

            // simply terminate VM in case of shutdown :-)
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

                    getLog().log( LogService.LOG_INFO, "Shutting down server now!" );

                    // stopping bundle 0 (system bundle) stops the framework
                    try
                    {
                        Bundle systemBundle = getBundleContext().getBundle( 0 );
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
                        getLog().log( LogService.LOG_ERROR, "Problem stopping or restarting the Framework", be );
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


    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        PrintWriter pw = response.getWriter();

        pw.println( "" );

        if ( request.getAttribute( ATTR_TERMINATED ) != null )
        {
            pw.println( "<tr>" );
            pw.println( "<td colspan='2' class='techcontentcell'>" );
            pw.println( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );
            pw.println( "<tr class='content'>" );

            Object restart = request.getAttribute( PARAM_SHUTDOWN_TYPE );
            if ( ( restart instanceof Boolean ) && ( ( Boolean ) restart ).booleanValue() )
            {
                pw.println( "<th class='content important'>Framework is restarting. stand by ...</th>" );

                pw.println( "<td class='content'>" );
                pw.println( "<form name='reloadform' method='get'>" );
                pw.println( "<input class='submit important' type='submit' value='Reload')\">&nbsp;" );
                pw.println( "Reloading in <span id='reloadcountdowncell'>&nbsp;</span>" );
                pw.println( "<script language='JavaScript'>" );
                pw.println( "shutdown(10, 'reloadform', 'reloadcountdowncell');" );
                pw.println( "</script>" );

                pw.println( "</form>" );
                pw.println( "</td" );
            }
            else
            {
                pw.println( "<th class='content important'>Framework has been stopped.</th>" );
            }

            pw.println( "</tr>" );
            pw.println( "</table>" );
            pw.println( "</td>" );
            pw.println( "</tr>" );
            return;
        }

        boolean shutdownTimer = false;
        String target = request.getRequestURI();
        if ( request.getParameter( PARAM_SHUTDOWN_TIMER ) != null )
        {
            target = getLabel(); // ShutdownRender.NAME;
            shutdownTimer = true;
        }

        pw.println( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );

        pw.println( "<tr class='content'>" );
        pw.println( "<th colspan='2' class='content container'>Start Level Information:</th>" );
        pw.println( "</tr>" );

        pw.println( "<tr class='content'>" );
        pw.println( "<td class='content'>System Start Level</td>" );
        pw.println( "<td class='content'>" );
        pw.println( "<form method='post'>" );
        pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + SetStartLevelAction.NAME + "'>" );
        pw.println( "<input class='input' type='text' size='3' name='systemStartLevel' value='"
            + getStartLevel().getStartLevel() + "'/>" );
        pw.println( "&nbsp;&nbsp;<input class='submit' type='submit' name='" + SetStartLevelAction.LABEL
            + "' value='Change'>" );
        pw.println( "</form>" );
        pw.println( "</td>" );
        pw.println( "</tr>" );
        pw.println( "<tr class='content'>" );
        pw.println( "<td class='content'>Default Bundle Start Level</td>" );
        pw.println( "<td class='content'>" );
        pw.println( "<form method='post'>" );
        pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + SetStartLevelAction.NAME + "'>" );
        pw.println( "<input class='input' type='text' size='3' name='bundleStartLevel' value='"
            + getStartLevel().getInitialBundleStartLevel() + "'/>" );
        pw.println( "&nbsp;&nbsp;<input class='submit' type='submit' name='" + SetStartLevelAction.LABEL
            + "' value='Change'>" );
        pw.println( "</form>" );
        pw.println( "</td>" );
        pw.println( "</tr>" );

        pw.println( "<tr class='content'>" );
        pw.println( "<td colspan='2' class='content'>&nbsp;</th>" );
        pw.println( "</tr>" );

        pw.println( "<tr class='content'>" );
        pw.println( "<th colspan='2' class='content container'>Server Information:</th>" );
        pw.println( "</tr>" );

        pw.println( "<tr class='content'>" );
        pw.println( "<td class='content'>Last Started</td>" );
        pw.println( "<td class='content'>" );
        pw.println( "<script language='JavaScript'>" );
        pw.println( "localDate(" + startDate /* <%= Server.getStartTime() %> */
            + ")" );
        pw.println( "</script>" );
        pw.println( "</td>" );
        pw.println( "</tr>" );

        pw.println( "<tr class='content'>" );
        pw.println( "<form name='shutdownform' method='post' action='" + target + "'>" );
        pw.println( "<td class='content'>Framework</td>" );
        pw.println( "<td class='content'>" );

        if ( !shutdownTimer )
        {
            pw.println( "<input type='hidden' name='" + PARAM_SHUTDOWN_TIMER + "' value='" + PARAM_SHUTDOWN_TIMER
                + "'>" );
            pw
                .println( "<input class='submit important' type='submit' name='"
                    + PARAM_SHUTDOWN_TYPE
                    + "' value='Restart' onclick=\"return confirm('This will stop and restart the framework and all bundles. Please confirm to continue.')\">" );
            pw
                .println( "<input class='submit important' type='submit' name='"
                    + PARAM_SHUTDOWN_TYPE
                    + "' value='Stop' onclick=\"return confirm('This will stop the framework and all bundles. Please confirm to continue.')\">" );
        }
        else
        {
            pw.println( "<input type='hidden' name='" + PARAM_SHUTDOWN_TYPE + "' value='"
                + request.getParameter( PARAM_SHUTDOWN_TYPE ) + "'>" );
            pw.println( "<input class='submit important' type='button' value='Abort' onclick=\"abort('"
                + request.getRequestURI() + "')\">&nbsp;" );
            pw.println( "Shutdown in <span id='countdowncell'>&nbsp;</span>" );
            pw.println( "<script language='JavaScript'>" );
            pw.println( "shutdown(3, 'shutdownform', 'countdowncell');" );
            pw.println( "</script>" );
        }

        pw.println( "</td>" );
        pw.println( "</form>" );
        pw.println( "</tr>" );

        pw.println( "<tr class='content'>" );
        pw.println( "<td colspan='2' class='content'>&nbsp;</th>" );
        pw.println( "</tr>" );

        pw.println( "<tr class='content'>" );
        pw.println( "<th colspan='2' class='content container'>Java Information:</th>" );
        pw.println( "</tr>" );

        long freeMem = Runtime.getRuntime().freeMemory() / 1024;
        long totalMem = Runtime.getRuntime().totalMemory() / 1024;
        long usedMem = totalMem - freeMem;

        this.infoLine( pw, "Java Runtime", System.getProperty( "java.runtime.name" ) + "(build "
            + System.getProperty( "java.runtime.version" ) + ")" );
        this.infoLine( pw, "Java Virtual Machine", System.getProperty( "java.vm.name" ) + "(build "
            + System.getProperty( "java.vm.version" ) + ", " + System.getProperty( "java.vm.info" ) + ")" );
        this.infoLine( pw, "Total Memory", totalMem + " KB" );
        this.infoLine( pw, "Used Memory", usedMem + " KB" );
        this.infoLine( pw, "Free Memory", freeMem + " KB" );

        pw.println( "<tr class='content'>" );
        pw.println( "<form method='post'>" );
        pw.println( "<td class='content'>Garbage Collection</td>" );
        pw.println( "<td class='content'>" );
        pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + GCAction.NAME + "'>" );
        pw.println( "<input class='submit' type='submit' name='" + GCAction.LABEL + "' value='Run'>" );
        pw.println( "</form></td></tr>" );

        pw.println( "</table>" );
    }


    private void infoLine( PrintWriter pw, String label, String value )
    {
        pw.println( "<tr class='content'>" );
        pw.println( "<td class='content'>" + label + "</td>" );
        pw.println( "<td class='content'>" );
        pw.println( value );
        pw.println( "</td></tr>" );
    }

}
