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
package org.apache.felix.webconsole.internal;


import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.felix.webconsole.Render;


/**
 * The <code>Util</code> TODO
 */
public class Util
{

    /** web apps subpage */
    public static final String PAGE_WEBAPPS = "/webapps";

    /** vm statistics subpage */
    public static final String PAGE_VM_STAT = "/vmstat";

    /** Logs subpage */
    public static final String PAGE_LOGS = "/logs";

    /** Parameter name */
    public static final String PARAM_ACTION = "action";

    /** Parameter name */
    public static final String PARAM_CONTENT = "content";

    /** Parameter name */
    public static final String PARAM_SHUTDOWN = "shutdown";

    /** Parameter value */
    public static final String VALUE_SHUTDOWN = "shutdown";

    private static final String HEADER = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">"
        + "<html>"
        + "<head>"
        + "<meta http-equiv=\"Content-Type\" content=\"text/html; utf-8\">"
        + "<link rel=\"icon\" href=\"res/imgs/favicon.ico\">"
        + "<title>{0} - {12}</title>"
        + "<script src=\"res/ui/admin.js\" language=\"JavaScript\"></script>"
        + "<script language=\"JavaScript\">"
        + "ABOUT_VERSION=''{1}'';"
        + "ABOUT_JVERSION=''{2}'';"
        + "ABOUT_JRT=''{3} (build {2})'';"
        + "ABOUT_JVM=''{4} (build {5}, {6})'';"
        + "ABOUT_MEM=\"{7} KB\";"
        + "ABOUT_USED=\"{8} KB\";"
        + "ABOUT_FREE=\"{9} KB\";"
        + "</script>"
        + "<link href=\"res/ui/admin.css\" rel=\"stylesheet\" type=\"text/css\">"
        + "</head>"
        + "<body>"
        + "<div id=\"main\">"
        + "<div id=\"lead\">"
        + "<h1>"
        + "{0}<br>{12}"
        + "</h1>"
        + "<p>"
        + "<a target=\"_blank\" href=\"{13}\" title=\"{11}\"><img src=\"res/imgs/logo.png\" width=\"165\" height=\"63\" border=\"0\"></a>"
        + "</p>" + "</div>";

    /** The name of the request attribute containig the map of FileItems from the POST request */
    public static final String ATTR_FILEUPLOAD = "org.apache.felix.webconsole.fileupload";


    public static PrintWriter startHtml( HttpServletResponse resp, String pageTitle ) throws IOException
    {
        resp.setContentType( "text/html; utf-8" );

        PrintWriter pw = resp.getWriter();

        String adminTitle = "OSGi Management Console"; // ServletEngine.VERSION.getFullProductName();
        String productName = "Felix"; // ServletEngine.VERSION.getShortProductName();
        String productWeb = "http://felix.apache.org";
        String vendorName = "http://www.apache.org"; // ServletEngine.VERSION.getVendorWeb();
        String vendorWeb = "http://www.apache.org"; // ServletEngine.VERSION.getVendorWeb();

        long freeMem = Runtime.getRuntime().freeMemory() / 1024;
        long totalMem = Runtime.getRuntime().totalMemory() / 1024;
        long usedMem = totalMem - freeMem;

        String header = MessageFormat.format( HEADER, new Object[]
            {
                adminTitle,
                "1.0.0-SNAPSHOT", // ServletEngine.VERSION.getFullVersion(),
                System.getProperty( "java.runtime.version" ), System.getProperty( "java.runtime.name" ),
                System.getProperty( "java.vm.name" ), System.getProperty( "java.vm.version" ),
                System.getProperty( "java.vm.info" ), new Long( totalMem ), new Long( usedMem ), new Long( freeMem ),
                vendorWeb, productName, pageTitle, productWeb, vendorName } );
        pw.println( header );
        return pw;
    }


    public static void navigation( PrintWriter pw, Collection renders, String current, boolean disabled )
    {
        pw.println( "<p id='technav'>" );

        SortedMap map = new TreeMap();
        for ( Iterator ri = renders.iterator(); ri.hasNext(); )
        {
            Render render = ( Render ) ri.next();
            if ( render.getLabel() == null )
            {
                // ignore renders without a label
            }
            else if ( disabled || current.equals( render.getName() ) )
            {
                map.put( render.getLabel(), "<span class='technavat'>" + render.getLabel() + "</span>" );
            }
            else
            {
                map.put( render.getLabel(), "<a href='" + render.getName() + "'>" + render.getLabel() + "</a></li>" );
            }
        }

        for ( Iterator li = map.values().iterator(); li.hasNext(); )
        {
            pw.println( li.next() );
        }

        pw.println( "</p>" );
    }


    public static void endHhtml( PrintWriter pw )
    {
        pw.println( "</body>" );
        pw.println( "</html>" );
    }


    public static void startScript( PrintWriter pw )
    {
        pw.println( "<script type='text/javascript'>" );
        pw.println( "// <![CDATA[" );
    }


    public static void endScript( PrintWriter pw )
    {
        pw.println( "// ]]>" );
        pw.println( "</script>" );
    }


    public static void spool( String res, HttpServletResponse resp ) throws IOException
    {
        InputStream ins = getResource( res );
        if ( ins != null )
        {
            try
            {
                IOUtils.copy( ins, resp.getOutputStream() );
            }
            finally
            {
                IOUtils.closeQuietly( ins );
            }
        }
    }


    private static InputStream getResource( String resource )
    {
        return Util.class.getResourceAsStream( resource );
    }
}
