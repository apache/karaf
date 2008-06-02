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
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;


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
