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
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

public class ExportsCommandImpl implements Command
{
    private BundleContext m_context = null;

    public ExportsCommandImpl(BundleContext context)
    {
        m_context = context;
    }

    public String getName()
    {
        return "exports";
    }

    public String getUsage()
    {
        return "exports <id> ...";
    }

    public String getShortDescription()
    {
        return "list exported packages.";
    }

    public void execute(String s, PrintStream out, PrintStream err)
    {
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
                    ExportedPackage[] exports = pa.getExportedPackages(bundle);
                    if (separatorNeeded)
                    {
                        out.println("");
                    }
                    printExports(out, bundle, exports);
                    separatorNeeded = true;
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

    private void printExports(PrintStream out, Bundle target, ExportedPackage[] exports)
    {
        String title = target + " exports:";
        out.println(title);
        out.println(Util.getUnderlineString(title));
        if ((exports != null) && (exports.length > 0))
        {
            for (int i = 0; i < exports.length; i++)
            {
                out.println(exports[i]);
            }
        }
        else
        {
            out.println("Nothing");
        }
    }
}