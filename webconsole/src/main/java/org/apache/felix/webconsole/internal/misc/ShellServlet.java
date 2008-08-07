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
package org.apache.felix.webconsole.internal.misc;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.shell.ShellService;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;


public class ShellServlet extends AbstractWebConsolePlugin implements OsgiManagerPlugin
{
    private ServiceTracker shellTracker;


    public String getLabel()
    {
        return "shell";
    }


    public String getTitle()
    {
        return "Shell";
    }


    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        response.setCharacterEncoding( "utf-8" );
        response.setContentType( "text/html" );

        PrintWriter pw = response.getWriter();

        try
        {
            String command = request.getParameter( "command" );

            pw.print( "<span class=\"consolecommand\">-&gt; " );
            pw.print( command == null ? "" : escapeHtml( command ) );
            pw.println( "</span><br />" );

            if ( command != null && !"".equals( command ) )
            {
                ShellService shellService = getShellService();
                if ( shellService != null )
                {
                    ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
                    ByteArrayOutputStream baosErr = new ByteArrayOutputStream();

                    shellService.executeCommand( command, new PrintStream( baosOut, true ), new PrintStream( baosErr,
                        true ) );
                    if ( baosOut.size() > 0 )
                    {
                        pw.print( escapeHtml( new String( baosOut.toByteArray() ) ) );
                    }
                    if ( baosErr.size() > 0 )
                    {
                        pw.print( "<span class=\"error\">" );
                        pw.print( escapeHtml( new String( baosErr.toByteArray() ) ) );
                        pw.println( "</span>" );
                    }
                }
                else
                {
                    pw.print( "<span class=\"error\">" );
                    pw.print( "Error: No shell service available<br />" );
                    pw.println( "</span>" );
                }
            }
        }
        catch ( Throwable t )
        {
            pw.print( "<span class=\"error\">" );
            StringWriter out = new StringWriter();
            t.printStackTrace( new PrintWriter( out, true ) );
            pw.print( escapeHtml( out.toString() ) );
            pw.println( "</span>" );
        }
    }


    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        PrintWriter pw = response.getWriter();

        String appRoot = request.getContextPath() + request.getServletPath();
        pw.println( "<link href=\"" + appRoot + "/res/ui/shell.css\" rel=\"stylesheet\" type=\"text/css\" />" );
        pw.println( "<script src=\"" + appRoot + "/res/ui/shell.js\" type=\"text/javascript\"></script>" );

        pw.println( "<br />" );

        pw.println( "<form name=\"shellCommandForm\" method=\"post\" action=\"" + appRoot
            + "/shell\" title=\"Shell Command\" onsubmit=\"runShellCommand();return false;\">" );

        pw.println( "<div class=\"consolebuttons\">" );
        pw.println( "<input class=\"submit\" type=\"button\" value=\"Help\" onclick=\"executeCommand('help');\"/>" );
        pw
            .println( "&nbsp;&nbsp;<input class=\"submit\" type=\"button\" value=\"Clear\" onclick=\"clearConsole();\"/>" );
        pw.println( "</div>" );

        pw.println( "<div id=\"consoleframe\" class=\"consoleframe\" onclick=\"shellCommandFocus();\">" );
        pw.println( "<div id=\"console\" class=\"console\" onclick=\"shellCommandFocus();\">" );
        pw.println( "</div>" );

        pw.println( "<span class=\"prompt\">" );
        pw.println( "-&gt; <input type=\"text\" name=\"command\" value=\"\" class=\"command\" autocomplete=\"off\"/>" );
        pw.println( "</span>" );

        pw.println( "</div>" );

        pw.println( "</form>" );

        pw.println( "<script type=\"text/javascript\">" );
        pw.println( "shellCommandFocus();" );
        pw.println( "</script>" );
    }


    protected ShellService getShellService()
    {
        return ( ( ShellService ) shellTracker.getService() );
    }


    public void activate( BundleContext bundleContext )
    {
        super.activate( bundleContext );

        shellTracker = new ServiceTracker( bundleContext, ShellService.class.getName(), null );
        shellTracker.open();
    }


    public void deactivate()
    {
        if ( shellTracker != null )
        {
            shellTracker.close();
            shellTracker = null;
        }

        super.deactivate();
    }


    protected String escapeHtml( String text )
    {
        StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < text.length(); i++ )
        {
            char ch = text.charAt( i );
            if ( ch == '<' )
            {
                sb.append( "&lt;" );
            }
            else if ( ch == '>' )
            {
                sb.append( "&gt;" );
            }
            else if ( ch == '&' )
            {
                sb.append( "&amp;" );
            }
            else if ( ch == ' ' )
            {
                sb.append( "&nbsp;" );
            }
            else if ( ch == '\r' )
            {
            }
            else if ( ch == '\n' )
            {
                sb.append( "<br />\r\n" );
            }
            else
            {
                sb.append( ch );
            }
        }

        return ( sb.toString() );
    }
}
