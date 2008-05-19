/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.internal.system;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.Render;
import org.apache.felix.webconsole.internal.BaseManagementPlugin;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.internal.core.SetStartLevelAction;


public class VMStatRender extends BaseManagementPlugin implements Render
{

    public static final String NAME = "vmstat";

    public static final String LABEL = "System Information";

    private static final long startDate = ( new Date() ).getTime();


    public String getName()
    {
        return NAME;
    }


    public String getLabel()
    {
        return LABEL;
    }


    public void render( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {

        PrintWriter pw = response.getWriter();

        pw.println( "" );
        boolean shutdown = false;

        String target = request.getRequestURI();
        if ( request.getParameter( Util.PARAM_SHUTDOWN ) != null )
        {
            target = ShutdownRender.NAME;
            shutdown = true;
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
        pw.println( "<td class='content'>Server</td>" );
        pw.println( "<td class='content'>" );

        if ( !shutdown )
        {
            pw.println( "<input type='hidden' name='" + Util.PARAM_SHUTDOWN + "' value='" + Util.VALUE_SHUTDOWN + "'>" );
            pw
                .println( "<input class='submit important' type='submit' value='Stop' onclick=\"return confirm('This will terminate all running applications. Do you want to stop the server?')\">" );
        }
        else
        {
            pw.println( "<input class='submit important' type='button' value='Abort' onclick=\"abort('"
                + request.getRequestURI() + "')\">&nbsp;" );
            pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + ShutdownAction.NAME + "'>" );
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

        this.infoLine( pw, "Java Runtime", "ABOUT_JRT" );
        this.infoLine( pw, "Java Virtual Machine", "ABOUT_JVM" );
        this.infoLine( pw, "Total Memory", "ABOUT_MEM" );
        this.infoLine( pw, "Used Memory", "ABOUT_USED" );
        this.infoLine( pw, "Free Memory", "ABOUT_FREE" );

        pw.println( "<tr class='content'>" );
        pw.println( "<form method='post'>" );
        pw.println( "<td class='content'>Garbage Collection</td>" );
        pw.println( "<td class='content'>" );
        pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + GCAction.NAME + "'>" );
        pw.println( "<input class='submit' type='submit' name='" + GCAction.LABEL + "' value='Run'>" );
        pw.println( "</form></td></tr>" );

        pw.println( "</table>" );
    }


    private void infoLine( PrintWriter pw, String label, String jsName )
    {
        pw.println( "<tr class='content'>" );
        pw.println( "<td class='content'>" + label + "</td>" );
        pw.println( "<td class='content'>" );
        pw.println( "<script> document.write(" + jsName + "); </script>" );
        pw.println( "</td></tr>" );
    }

}
