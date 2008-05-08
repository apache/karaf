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

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import org.apache.felix.shell.CdCommand;
import org.apache.felix.shell.Command;
import org.osgi.framework.*;

public class UpdateCommandImpl implements Command
{
    private BundleContext m_context = null;

    public UpdateCommandImpl(BundleContext context)
    {
        m_context = context;
    }

    public String getName()
    {
        return "update";
    }

    public String getUsage()
    {
        return "update <id> [<URL>]";
    }

    public String getShortDescription()
    {
        return "update bundle.";
    }

    public void execute(String s, PrintStream out, PrintStream err)
    {
        StringTokenizer st = new StringTokenizer(s, " ");

        // Ignore the command name.
        st.nextToken();

        // There should be at least a bundle ID, but there may
        // also be a URL.
        if ((st.countTokens() == 1) || (st.countTokens() == 2))
        {
            String id = st.nextToken().trim();
            String location = st.countTokens() == 0 ? null : st.nextToken().trim();

            if (location != null)
            {
                location = absoluteLocation(location);

                if (location == null)
                {
                    err.println("Malformed location: " + location);
                }
            }

            try
            {
                // Get the bundle id.
                long l = Long.parseLong(id);

                // Get the bundle.
                Bundle bundle = m_context.getBundle(l);
                if (bundle != null)
                {
                    // Create input stream from location if present
                    // and use it to update, otherwise just update.
                    if (location != null)
                    {
                        InputStream is = new URL(location).openStream();
                        bundle.update(is);
                    }
                    else
                    {
                        bundle.update();
                    }
                }
                else
                {
                    err.println("Bundle ID " + id + " is invalid.");
                }
            }
            catch (NumberFormatException ex)
            {
                err.println("Unable to parse id '" + id + "'.");
            }
            catch (MalformedURLException ex)
            {
                err.println("Unable to parse URL.");
            }
            catch (IOException ex)
            {
                err.println("Unable to open input stream: " + ex);
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
        else
        {
            err.println("Incorrect number of arguments");
        }
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