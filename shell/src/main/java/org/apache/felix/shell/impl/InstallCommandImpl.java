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
package org.apache.felix.shell.impl;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import org.apache.felix.shell.CdCommand;
import org.apache.felix.shell.Command;
import org.osgi.framework.*;

public class InstallCommandImpl implements Command
{
    private BundleContext m_context = null;

    public InstallCommandImpl(BundleContext context)
    {
        m_context = context;
    }

    public String getName()
    {
        return "install";
    }

    public String getUsage()
    {
        return "install <URL> [<URL> ...]";
    }

    public String getShortDescription()
    {
        return "install bundle(s).";
    }

    public void execute(String s, PrintStream out, PrintStream err)
    {
        StringTokenizer st = new StringTokenizer(s, " ");

        // Ignore the command name.
        st.nextToken();

        // There should be at least one URL.
        if (st.countTokens() >= 1)
        {
            StringBuffer sb = new StringBuffer();
            while (st.hasMoreTokens())
            {
                String location = st.nextToken().trim();
                Bundle bundle = install(location, out, err);
                if (bundle != null)
                {
                    if (sb.length() > 0)
                    {
                        sb.append(", ");
                    }
                    sb.append(bundle.getBundleId());
                }
            }
            if (sb.toString().indexOf(',') > 0)
            {
                out.println("Bundle IDs: " + sb.toString());
            }
            else if (sb.length() > 0)
            {
                out.println("Bundle ID: " + sb.toString());
            }
        }
        else
        {
            err.println("Incorrect number of arguments");
        }
    }

    protected Bundle install(String location, PrintStream out, PrintStream err)
    {
        String abs = absoluteLocation(location);
        if (abs == null)
        {
            err.println("Malformed location: " + location);
        }
        else
        {
            try
            {
                return m_context.installBundle(abs, null);
            }
            catch (IllegalStateException ex)
            {
                err.println(ex.toString());
            }
            catch (BundleException ex)
            {
                if (ex.getNestedException() != null)
                {
                    err.println(ex.getNestedException().toString());
                }
                else
                {
                    err.println(ex.toString());
                }
            }
            catch (Exception ex)
            {
                err.println(ex.toString());
            }
        }
        return null;
    }

    private String absoluteLocation(String location)
    {
        try
        {
            new URL(location);
        }
        catch (MalformedURLException ex)
        {
            // Try to create a valid URL using the base URL
            // contained in the "cd" command service.
            String baseURL = "";

            try
            {
                // Get a reference to the "cd" command service.
                ServiceReference ref = m_context.getServiceReference(
                    org.apache.felix.shell.CdCommand.class.getName());

                if (ref != null)
                {
                    CdCommand cd = (CdCommand) m_context.getService(ref);
                    baseURL = cd.getBaseURL();
                    baseURL = (baseURL == null) ? "" : baseURL;
                    m_context.ungetService(ref);
                }

                String theURL = baseURL + location;
                new URL(theURL);
                location = theURL;
            }
            catch (Exception ex2)
            {
                // Just fall through and return the original location.
            }
        }
        return location;
    }
}