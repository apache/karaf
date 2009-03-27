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
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

public class ImportsCommandImpl implements Command
{
    private final BundleContext m_context;
    private ServiceReference m_ref = null;

    public ImportsCommandImpl(BundleContext context)
    {
        m_context = context;
    }

    public String getName()
    {
        return "imports";
    }

    public String getUsage()
    {
        return "imports <id> ...";
    }

    public String getShortDescription()
    {
        return "list imported packages.";
    }

    public void execute(String s, PrintStream out, PrintStream err)
    {
        StringTokenizer st = new StringTokenizer(s, " ");

        // Ignore the command name.
        st.nextToken();

        if (st.hasMoreTokens())
        {
            boolean separatorNeeded = false;
            while (st.hasMoreTokens())
            {
                String id = st.nextToken().trim();

                try
                {
                    long l = Long.parseLong(id);
                    Bundle bundle = m_context.getBundle(l);
                    if (bundle != null)
                    {
                        if (separatorNeeded)
                        {
                            out.println("");
                        }
                        getImportedPackages(bundle, out, err);
                        separatorNeeded = true;
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
    }

    private void getImportedPackages(Bundle bundle, PrintStream out, PrintStream err)
    {
        // Get package admin service.
        PackageAdmin pa = getPackageAdmin();
        if (pa == null)
        {
            out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            ExportedPackage[] exports = pa.getExportedPackages((Bundle) null);
            String title = bundle + " imports:";
            out.println(title);
            out.println(Util.getUnderlineString(title));
            boolean found = false;
            for (int expIdx = 0; expIdx < exports.length; expIdx++)
            {
                Bundle[] importers = exports[expIdx].getImportingBundles();
                for (int impIdx = 0; (importers != null) && (impIdx < importers.length); impIdx++)
                {
                    if (importers[impIdx] == bundle)
                    {
                        out.println(exports[expIdx]
                            + " -> " + exports[expIdx].getExportingBundle());
                        found = true;
                    }
                }
            }
            if (!found)
            {
                out.println("Nothing");
            }
            ungetPackageAdmin();
        }
    }

    private PackageAdmin getPackageAdmin()
    {
        PackageAdmin pa = null;
        m_ref = m_context.getServiceReference(
            org.osgi.service.packageadmin.PackageAdmin.class.getName());
        if (m_ref != null)
        {
            pa = (PackageAdmin) m_context.getService(m_ref);
        }
        return pa;
    }

    private void ungetPackageAdmin()
    {
        m_context.ungetService(m_ref);
    }
}