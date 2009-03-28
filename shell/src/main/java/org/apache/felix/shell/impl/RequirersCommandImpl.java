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
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

public class RequirersCommandImpl implements Command
{
    private BundleContext m_context = null;

    public RequirersCommandImpl(BundleContext context)
    {
        m_context = context;
    }

    public String getName()
    {
        return "requirers";
    }

    public String getUsage()
    {
        return "requirers <id> ...";
    }

    public String getShortDescription()
    {
        return "list requiring bundles.";
    }

    public void execute(String s, PrintStream out, PrintStream err)
    {
        // Get package admin service.
        ServiceReference ref = m_context.getServiceReference(
            org.osgi.service.packageadmin.PackageAdmin.class.getName());
        PackageAdmin pa = (ref == null) ? null : (PackageAdmin) m_context.getService(ref);
        if (pa == null)
        {
            out.println("PackageAdmin service is unavailable.");
            return;
        }

        // Parse command line.
        StringTokenizer st = new StringTokenizer(s, " ");

        // Ignore the command name.
        st.nextToken();

        if (st.hasMoreTokens())
        {
            boolean separatorNeeded = false;
            while (st.hasMoreTokens())
            {
                String id = st.nextToken();
                try
                {
                    long l = Long.parseLong(id);
                    Bundle bundle = m_context.getBundle(l);
                    RequiredBundle[] rbs = pa.getRequiredBundles(bundle.getSymbolicName());
                    for (int i = 0; (rbs != null) && (i < rbs.length); i++)
                    {
                        if (rbs[i].getBundle() == bundle)
                        {
                            if (separatorNeeded)
                            {
                                out.println("");
                            }
                            printRequiredBundles(out, bundle, rbs[i].getRequiringBundles());
                            separatorNeeded = true;
                        }
                    }
                }
                catch (NumberFormatException ex)
                {
                    err.println("Unable to parse id '" + id + "'.");
                }
                catch (Exception ex)
                {
                    err.println(ex.toString());
                }
            }
        }
    }

    private void printRequiredBundles(PrintStream out, Bundle target, Bundle[] requirers)
    {
        String title = target + " required by:";
        out.println(title);
        out.println(Util.getUnderlineString(title));
        if ((requirers != null) && (requirers.length > 0))
        {
            for (int i = 0; i < requirers.length; i++)
            {
                out.println(requirers[i]);
            }
        }
        else
        {
            out.println("Nothing");
        }
    }
}