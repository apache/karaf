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
package org.apache.felix.dm.test;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * Base class for all test cases.
 */
public class Base implements LogService
{
    /**
     * Register us as a LogService
     * @param context
     */
    @Before
    public void startup(BundleContext context)
    {
        context.registerService(LogService.class.getName(), this, null);
    }

    /**
     * Always cleanup our bundle location file (because pax seems to forget to cleanup it)
     * @param context
     */
    
    @After
    public void tearDown(BundleContext context)
    {
        // The following code forces the temporary bundle files (from /tmp/tb/*) to be deleted when jvm exits
        // (this patch seems to be only required with pax examp 2.0.0)

        try
        {
            File f = new File(new URL(context.getBundle().getLocation()).getPath());
            f.deleteOnExit();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    public void log(int level, String message)
    {
        System.out.println("[LogService/" + level + "] " + message);
    }

    public void log(int level, String message, Throwable exception)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[LogService/" + level + "] ");
        sb.append(message);
        parse(sb, exception);
        System.out.println(sb.toString());
    }

    public void log(ServiceReference sr, int level, String message)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[LogService/" + level + "] ");
        sb.append(message);
        System.out.println(sb.toString());
    }

    public void log(ServiceReference sr, int level, String message, Throwable exception)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[LogService/" + level + "] ");
        sb.append(message);
        parse(sb, exception);
        System.out.println(sb.toString());
    }

    private void parse(StringBuilder sb, Throwable t)
    {
        if (t != null)
        {
            sb.append(" - ");
            StringWriter buffer = new StringWriter();
            PrintWriter pw = new PrintWriter(buffer);
            t.printStackTrace(pw);
            sb.append(buffer.toString());
        }
    }
}
