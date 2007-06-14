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
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.felix.shell.Command;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

public class RefreshCommandImpl implements Command
{
    private BundleContext m_context = null;

    public RefreshCommandImpl(BundleContext context)
    {
        m_context = context;
    }

    public String getName()
    {
        return "refresh";
    }

    public String getUsage()
    {
        return "refresh [<id> ...]";
    }

    public String getShortDescription()
    {
        return "refresh packages.";
    }

    public void execute(String s, PrintStream out, PrintStream err)
    {
        StringTokenizer st = new StringTokenizer(s, " ");

        // Ignore the command name.
        st.nextToken();

        // Refresh the specified bundles or all if none are specified.
        List bundleList = new ArrayList();
        if (st.hasMoreTokens())
        {
            while (st.hasMoreTokens())
            {
                String id = st.nextToken().trim();

                try
                {
                    long l = Long.parseLong(id);
                    Bundle bundle = m_context.getBundle(l);
                    if (bundle != null)
                    {
                        bundleList.add(bundle);
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
                catch (Exception ex)
                {
                    err.println(ex.toString());
                }
            }
        }

        // Get package admin service.
        ServiceReference ref = m_context.getServiceReference(
            org.osgi.service.packageadmin.PackageAdmin.class.getName());
        if (ref == null)
        {
            out.println("PackageAdmin service is unavailable.");
            return;
        }

        PackageAdmin pa = (PackageAdmin) m_context.getService(ref);
        if (pa == null)
        {
            out.println("PackageAdmin service is unavailable.");
            return;
        }

        pa.refreshPackages((bundleList.size() == 0)
            ? null
            : (Bundle[]) bundleList.toArray(new Bundle[bundleList.size()]));
    }
}
