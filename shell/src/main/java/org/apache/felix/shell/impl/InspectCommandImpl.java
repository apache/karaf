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
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

public class InspectCommandImpl implements Command
{
    public static final String PACKAGE_TYPE = "package";
    public static final String BUNDLE_TYPE = "bundle";
    public static final String FRAGMENT_TYPE = "fragment";
    public static final String SERVICE_TYPE = "service";

    public static final String PROVIDE_DIRECTION = "provide";
    public static final String REQUIRE_DIRECTION = "require";

    private final BundleContext m_context;
    private ServiceReference m_ref = null;

    public InspectCommandImpl(BundleContext context)
    {
        m_context = context;
    }

    public String getName()
    {
        return "inspect";
    }

    public String getUsage()
    {
        return "inspect (package|bundle|fragment|service) (require|provide) <id> ...";
    }

    public String getShortDescription()
    {
        return "inspects dependency information.";
    }

    public void execute(String s, PrintStream out, PrintStream err)
    {
        StringTokenizer st = new StringTokenizer(s, " ");

        // Ignore the command name.
        st.nextToken();

        if (st.countTokens() < 3)
        {
            out.println("Too few arguments.");
            out.println(getUsage());
        }
        else
        {
            // Get dependency type.
            String type = st.nextToken();
            // Get dependency direction.
            String direction = st.nextToken();
            // Get target bundle identifiers.
            String[] ids = new String[st.countTokens()];
            for (int i = 0; st.hasMoreTokens(); i++)
            {
                ids[i] = st.nextToken().trim();
            }
            // Verify arguments.
            if (validTypeAndDirection(type, direction))
            {
                // Now determine what needs to be printed.
                if (PACKAGE_TYPE.startsWith(type))
                {
                    if (PROVIDE_DIRECTION.startsWith(direction))
                    {
                        printExportedPackages(ids, out, err);
                    }
                    else
                    {
                        printImportedPackages(ids, out, err);
                    }
                }
                else if (BUNDLE_TYPE.startsWith(type))
                {
                    if (PROVIDE_DIRECTION.startsWith(direction))
                    {
                        printRequiringBundles(ids, out, err);
                    }
                    else
                    {
                        printRequiredBundles(ids, out, err);
                    }
                }
                else if (FRAGMENT_TYPE.startsWith(type))
                {
                    if (PROVIDE_DIRECTION.startsWith(direction))
                    {
                        out.println("Not yet implemented.");
                    }
                    else
                    {
                        out.println("Not yet implemented.");
                    }
                }
                else
                {
                    if (PROVIDE_DIRECTION.startsWith(direction))
                    {
                        printExportedServices(ids, out, err);
                    }
                    else
                    {
                        printImportedServices(ids, out, err);
                    }
                }
            }
            else
            {
                out.println("Invalid arguments.");
            }
        }
    }

    private void printExportedPackages(String[] ids, PrintStream out, PrintStream err)
    {
        PackageAdmin pa = getPackageAdmin();
        if (pa == null)
        {
            out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            boolean separatorNeeded = false;
            for (int idIdx = 0; idIdx < ids.length; idIdx++)
            {
                try
                {
                    long l = Long.parseLong(ids[idIdx]);
                    Bundle bundle = m_context.getBundle(l);
                    if (bundle != null)
                    {
                        ExportedPackage[] exports = pa.getExportedPackages(bundle);
                        if (separatorNeeded)
                        {
                            out.println("");
                        }
                        String title = bundle + " provides packages:";
                        out.println(title);
                        out.println(Util.getUnderlineString(title));
                        if ((exports != null) && (exports.length > 0))
                        {
                            for (int expIdx = 0; expIdx < exports.length; expIdx++)
                            {
                                out.println(exports[expIdx]);
                            }
                        }
                        else
                        {
                            out.println("Nothing");
                        }
                        separatorNeeded = true;
                    }
                    else
                    {
                        err.println("Bundle ID " + ids[idIdx] + " is invalid.");
                    }
                }
                catch (NumberFormatException ex)
                {
                    err.println("Unable to parse id '" + ids[idIdx] + "'.");
                }
                catch (Exception ex)
                {
                    err.println(ex.toString());
                }
            }
        }
        ungetPackageAdmin();
    }

    private void printImportedPackages(String[] ids, PrintStream out, PrintStream err)
    {
        boolean separatorNeeded = false;
        for (int idIdx = 0; idIdx < ids.length; idIdx++)
        {
            try
            {
                long l = Long.parseLong(ids[idIdx]);
                Bundle bundle = m_context.getBundle(l);
                if (bundle != null)
                {
                    if (separatorNeeded)
                    {
                        out.println("");
                    }
                    _printImportedPackages(bundle, out, err);
                    separatorNeeded = true;
                }
                else
                {
                    err.println("Bundle ID " + ids[idIdx] + " is invalid.");
                }
            }
            catch (NumberFormatException ex)
            {
                err.println("Unable to parse id '" + ids[idIdx] + "'.");
            }
            catch (Exception ex)
            {
                err.println(ex.toString());
            }
        }
    }

    private void _printImportedPackages(Bundle bundle, PrintStream out, PrintStream err)
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
            String title = bundle + " requires packages:";
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

    private void printRequiringBundles(String[] ids, PrintStream out, PrintStream err)
    {
        PackageAdmin pa = getPackageAdmin();
        if (pa == null)
        {
            out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            boolean separatorNeeded = false;
            for (int idIdx = 0; idIdx < ids.length; idIdx++)
            {
                try
                {
                    long l = Long.parseLong(ids[idIdx]);
                    Bundle bundle = m_context.getBundle(l);
                    if (bundle != null)
                    {
                        RequiredBundle[] rbs = pa.getRequiredBundles(bundle.getSymbolicName());
                        for (int rbIdx = 0; (rbs != null) && (rbIdx < rbs.length); rbIdx++)
                        {
                            if (rbs[rbIdx].getBundle() == bundle)
                            {
                                if (separatorNeeded)
                                {
                                    out.println("");
                                }
                                String title = bundle + " required by bundles:";
                                out.println(title);
                                out.println(Util.getUnderlineString(title));
                                if ((rbs[rbIdx].getRequiringBundles() != null)
                                    && (rbs[rbIdx].getRequiringBundles().length > 0))
                                {
                                    for (int reqIdx = 0;
                                        reqIdx < rbs[rbIdx].getRequiringBundles().length;
                                        reqIdx++)
                                    {
                                        out.println(rbs[rbIdx].getRequiringBundles()[reqIdx]);
                                    }
                                }
                                else
                                {
                                    out.println("Nothing");
                                }
                                separatorNeeded = true;
                            }
                        }
                    }
                    else
                    {
                        err.println("Bundle ID " + ids[idIdx] + " is invalid.");
                    }
                }
                catch (NumberFormatException ex)
                {
                    err.println("Unable to parse id '" + ids[idIdx] + "'.");
                }
                catch (Exception ex)
                {
                    err.println(ex.toString());
                }
            }
        }
    }

    private void printRequiredBundles(String[] ids, PrintStream out, PrintStream err)
    {
        boolean separatorNeeded = false;
        for (int idIdx = 0; idIdx < ids.length; idIdx++)
        {
            try
            {
                long l = Long.parseLong(ids[idIdx]);
                Bundle bundle = m_context.getBundle(l);
                if (bundle != null)
                {
                    if (separatorNeeded)
                    {
                        out.println("");
                    }
                    _printRequiredBundles(bundle, out, err);
                    separatorNeeded = true;
                }
                else
                {
                    err.println("Bundle ID " + ids[idIdx] + " is invalid.");
                }
            }
            catch (NumberFormatException ex)
            {
                err.println("Unable to parse id '" + ids[idIdx] + "'.");
            }
            catch (Exception ex)
            {
                err.println(ex.toString());
            }
        }
    }

    private void _printRequiredBundles(Bundle bundle, PrintStream out, PrintStream err)
    {
        // Get package admin service.
        PackageAdmin pa = getPackageAdmin();
        if (pa == null)
        {
            out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            RequiredBundle[] rbs = pa.getRequiredBundles(null);
            String title = bundle + " requires bundles:";
            out.println(title);
            out.println(Util.getUnderlineString(title));
            boolean found = false;
            for (int rbIdx = 0; rbIdx < rbs.length; rbIdx++)
            {
                Bundle[] requirers = rbs[rbIdx].getRequiringBundles();
                for (int reqIdx = 0; (requirers != null) && (reqIdx < requirers.length); reqIdx++)
                {
                    if (requirers[reqIdx] == bundle)
                    {
                        out.println(rbs[reqIdx]);
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

    public void printExportedServices(String[] ids, PrintStream out, PrintStream err)
    {
        for (int i = 0; i < ids.length; i++)
        {
            try
            {
                long l = Long.parseLong(ids[i]);
                Bundle bundle = m_context.getBundle(l);
                if (bundle != null)
                {
                    ServiceReference[] refs = bundle.getRegisteredServices();

                    // Print header if we have not already done so.
                    String title = Util.getBundleName(bundle) + " provides services:";
                    out.println(title);
                    out.println(Util.getUnderlineString(title));

                    if ((refs == null) || (refs.length == 0))
                    {
                        out.println("Nothing");
                    }

                    // Print properties for each service.
                    for (int refIdx = 0;
                        (refs != null) && (refIdx < refs.length);
                        refIdx++)
                    {
                        // Print service properties.
                        String[] keys = refs[refIdx].getPropertyKeys();
                        for (int keyIdx = 0;
                            (keys != null) && (keyIdx < keys.length);
                            keyIdx++)
                        {
                            Object v = refs[refIdx].getProperty(keys[keyIdx]);
                            out.println(
                                keys[keyIdx] + " = " + Util.getValueString(v));
                        }

                        // Print service separator if necessary.
                        if ((refIdx + 1) < refs.length)
                        {
                            out.println("----");
                        }
                    }
                }
                else
                {
                    err.println("Bundle ID " + ids[i] + " is invalid.");
                }
            }
            catch (NumberFormatException ex)
            {
                err.println("Unable to parse id '" + ids[i] + "'.");
            }
            catch (Exception ex)
            {
                err.println(ex.toString());
            }
        }
    }

    public void printImportedServices(String[] ids, PrintStream out, PrintStream err)
    {
        for (int i = 0; i < ids.length; i++)
        {
            try
            {
                long l = Long.parseLong(ids[i]);
                Bundle bundle = m_context.getBundle(l);
                if (bundle != null)
                {
                    ServiceReference[] refs = bundle.getServicesInUse();

                    // Print header if we have not already done so.
                    String title = Util.getBundleName(bundle) + " requires services:";
                    out.println(title);
                    out.println(Util.getUnderlineString(title));

                    if ((refs == null) || (refs.length == 0))
                    {
                        out.println("Nothing");
                    }

                    // Print properties for each service.
                    for (int refIdx = 0;
                        (refs != null) && (refIdx < refs.length);
                        refIdx++)
                    {
                        // Print service properties.
                        String[] keys = refs[refIdx].getPropertyKeys();
                        for (int keyIdx = 0;
                            (keys != null) && (keyIdx < keys.length);
                            keyIdx++)
                        {
                            Object v = refs[refIdx].getProperty(keys[keyIdx]);
                            out.println(
                                keys[keyIdx] + " = " + Util.getValueString(v));
                        }

                        // Print service separator if necessary.
                        if ((refIdx + 1) < refs.length)
                        {
                            out.println("----");
                        }
                    }
                }
                else
                {
                    err.println("Bundle ID " + ids[i] + " is invalid.");
                }
            }
            catch (NumberFormatException ex)
            {
                err.println("Unable to parse id '" + ids[i] + "'.");
            }
            catch (Exception ex)
            {
                err.println(ex.toString());
            }
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

    private boolean validTypeAndDirection(String type, String direction)
    {
        return
            (((PACKAGE_TYPE.startsWith(type) || BUNDLE_TYPE.startsWith(type)
                || FRAGMENT_TYPE.startsWith(type) || SERVICE_TYPE.startsWith(type)))
            && (PROVIDE_DIRECTION.startsWith(direction)
                || REQUIRE_DIRECTION.startsWith(direction)));
    }
}