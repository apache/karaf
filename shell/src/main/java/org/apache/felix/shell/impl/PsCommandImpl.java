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
import java.util.StringTokenizer;

import org.apache.felix.shell.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

public class PsCommandImpl implements Command
{
    private static final String LOCATION_SWITCH = "-l";
    private static final String SYMBOLIC_NAME_SWITCH = "-s";
    private static final String UPDATE_LOCATION_SWITCH = "-u";

    protected final BundleContext m_context;

    public PsCommandImpl(BundleContext context)
    {
        m_context = context;
    }

    public String getName()
    {
        return "ps";
    }

    public String getUsage()
    {
        return "ps [" + LOCATION_SWITCH
            + " | " + SYMBOLIC_NAME_SWITCH
            + " | " + UPDATE_LOCATION_SWITCH + "]";
    }

    public String getShortDescription()
    {
        return "list installed bundles.";
    }

    public void execute(String s, PrintStream out, PrintStream err)
    {
        // Get start level service.
        ServiceReference ref = m_context.getServiceReference(
            org.osgi.service.startlevel.StartLevel.class.getName());
        StartLevel sl = null;
        if (ref != null)
        {
            sl = (StartLevel) m_context.getService(ref);
        }

        if (sl == null)
        {
            out.println("StartLevel service is unavailable.");
        }

        // Parse command line.
        StringTokenizer st = new StringTokenizer(s, " ");

        // Ignore the command name.
        st.nextToken();

        // Check for optional argument.
        boolean showLoc = false;
        boolean showSymbolic = false;
        boolean showUpdate = false;
        if (st.countTokens() >= 1)
        {
            while (st.hasMoreTokens())
            {
                String token = st.nextToken().trim();
                if (token.equals(LOCATION_SWITCH))
                {
                    showLoc = true;
                }
                else if (token.equals(SYMBOLIC_NAME_SWITCH))
                {
                    showSymbolic = true;
                }
                else if (token.equals(UPDATE_LOCATION_SWITCH))
                {
                    showUpdate = true;
                }
            }
        }
        Bundle[] bundles = m_context.getBundles();
        if (bundles != null)
        {
            printBundleList(bundles, sl, out, showLoc, showSymbolic, showUpdate);
        }
        else
        {
            out.println("There are no installed bundles.");
        }
    }

    protected void printBundleList(
        Bundle[] bundles, StartLevel startLevel, PrintStream out, boolean showLoc,
        boolean showSymbolic, boolean showUpdate)
    {
        // Display active start level.
        if (startLevel != null)
        {
            out.println("START LEVEL " + startLevel.getStartLevel());
        }

        // Print column headers.
        String msg = " Name";
        if (showLoc)
        {
           msg = " Location";
        }
        else if (showSymbolic)
        {
           msg = " Symbolic name";
        }
        else if (showUpdate)
        {
           msg = " Update location";
        }
        String level = (startLevel == null) ? "" : "  Level ";
        out.println("   ID " + "  State       " + level + msg);
        for (int i = 0; i < bundles.length; i++)
        {
            // Get the bundle name or location.
            String name = (String)
                bundles[i].getHeaders().get(Constants.BUNDLE_NAME);
            // If there is no name, then default to symbolic name.
            name = (name == null) ? bundles[i].getSymbolicName() : name;
            // If there is no symbolic name, resort to location.
            name = (name == null) ? bundles[i].getLocation() : name;

            // Overwrite the default value is the user specifically
            // requested to display one or the other.
            if (showLoc)
            {
                name = bundles[i].getLocation();
            }
            else if (showSymbolic)
            {
                name = bundles[i].getSymbolicName();
                name = (name == null)
                    ? "<no symbolic name>" : name;
            }
            else if (showUpdate)
            {
                name = (String)
                    bundles[i].getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
                name = (name == null)
                    ? bundles[i].getLocation() : name;
            }
            // Show bundle version if not showing location.
            String version = (String)
                bundles[i].getHeaders().get(Constants.BUNDLE_VERSION);
            name = (!showLoc && !showUpdate && (version != null))
                ? name + " (" + version + ")" : name;
            long l = bundles[i].getBundleId();
            String id = String.valueOf(l);
            if (startLevel == null)
            {
                level = "1";
            }
            else
            {
                level = String.valueOf(startLevel.getBundleStartLevel(bundles[i]));
            }
            while (level.length() < 5)
            {
                level = " " + level;
            }
            while (id.length() < 4)
            {
                id = " " + id;
            }
            out.println("[" + id + "] ["
                + getStateString(bundles[i].getState())
                + "] [" + level + "] " + name);
        }
    }

    public String getStateString(int i)
    {
        if (i == Bundle.ACTIVE)
            return "Active     ";
        else if (i == Bundle.INSTALLED)
            return "Installed  ";
        else if (i == Bundle.RESOLVED)
            return "Resolved   ";
        else if (i == Bundle.STARTING)
            return "Starting   ";
        else if (i == Bundle.STOPPING)
            return "Stopping   ";
        return "Unknown    ";
    }
}