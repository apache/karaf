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
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

public class InspectCommandImpl implements Command
{
    public static final String PACKAGE_TYPE = "package";
    public static final String BUNDLE_TYPE = "bundle";
    public static final String FRAGMENT_TYPE = "fragment";
    public static final String SERVICE_TYPE = "service";

    public static final String CAPABILITY = "capability";
    public static final String REQUIREMENT = "requirement";

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
        return "inspect (package|bundle|fragment|service) (capability|requirement) [<id> ...]";
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

        if (st.countTokens() < 2)
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
            if (isValidType(type) && isValidDirection(direction))
            {
                // Now determine what needs to be printed.
                if (PACKAGE_TYPE.startsWith(type))
                {
                    if (CAPABILITY.startsWith(direction))
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
                    if (CAPABILITY.startsWith(direction))
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
                    if (CAPABILITY.startsWith(direction))
                    {
                        printFragmentHosts(ids, out, err);
                    }
                    else
                    {
                        printHostedFragments(ids, out, err);
                    }
                }
                else
                {
                    if (CAPABILITY.startsWith(direction))
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
                if (!isValidType(type))
                {
                    out.println("Invalid argument: " + type);
                }
                if (!isValidDirection(direction))
                {
                    out.println("Invalid argument: " + direction);
                }
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

            Bundle[] bundles = null;
            if ((ids == null) || (ids.length == 0))
            {
                bundles = m_context.getBundles();
            }
            else
            {
                bundles = new Bundle[ids.length];
                for (int idIdx = 0; idIdx < ids.length; idIdx++)
                {
                    try
                    {
                        long l = Long.parseLong(ids[idIdx]);
                        Bundle b = m_context.getBundle(l);
                        if (b == null)
                        {
                            err.println("Bundle ID " + ids[idIdx] + " is invalid.");
                        }
                        bundles[idIdx] = b;
                    }
                    catch (NumberFormatException ex)
                    {
                        err.println("Unable to parse id '" + ids[idIdx] + "'.");
                    }
                }
            }

            for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
            {
                try
                {
                    if (bundles[bundleIdx] != null)
                    {
                        ExportedPackage[] exports = pa.getExportedPackages(bundles[bundleIdx]);
                        if (separatorNeeded)
                        {
                            out.println("");
                        }
                        String title = bundles[bundleIdx] + " exports packages:";
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

        Bundle[] bundles = null;
        if ((ids == null) || (ids.length == 0))
        {
            bundles = m_context.getBundles();
        }
        else
        {
            bundles = new Bundle[ids.length];
            for (int idIdx = 0; idIdx < ids.length; idIdx++)
            {
                try
                {
                    long l = Long.parseLong(ids[idIdx]);
                    Bundle b = m_context.getBundle(l);
                    if (b == null)
                    {
                        err.println("Bundle ID " + ids[idIdx] + " is invalid.");
                    }
                    bundles[idIdx] = b;
                }
                catch (NumberFormatException ex)
                {
                    err.println("Unable to parse id '" + ids[idIdx] + "'.");
                }
            }
        }

        for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
        {
            try
            {
                if (bundles[bundleIdx] != null)
                {
                    if (separatorNeeded)
                    {
                        out.println("");
                    }
                    _printImportedPackages(bundles[bundleIdx], out, err);
                    separatorNeeded = true;
                }
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
            String title = bundle + " imports packages:";
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

            Bundle[] bundles = null;
            if ((ids == null) || (ids.length == 0))
            {
                bundles = m_context.getBundles();
            }
            else
            {
                bundles = new Bundle[ids.length];
                for (int idIdx = 0; idIdx < ids.length; idIdx++)
                {
                    try
                    {
                        long l = Long.parseLong(ids[idIdx]);
                        Bundle b = m_context.getBundle(l);
                        if (b == null)
                        {
                            err.println("Bundle ID " + ids[idIdx] + " is invalid.");
                        }
                        bundles[idIdx] = b;
                    }
                    catch (NumberFormatException ex)
                    {
                        err.println("Unable to parse id '" + ids[idIdx] + "'.");
                    }
                }
            }

            for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
            {
                try
                {
                    if (bundles[bundleIdx] != null)
                    {
                        RequiredBundle[] rbs = pa.getRequiredBundles(
                            bundles[bundleIdx].getSymbolicName());
                        for (int rbIdx = 0; (rbs != null) && (rbIdx < rbs.length); rbIdx++)
                        {
                            if (rbs[rbIdx].getBundle() == bundles[bundleIdx])
                            {
                                if (separatorNeeded)
                                {
                                    out.println("");
                                }
                                String title = bundles[bundleIdx] + " is required by:";
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

        Bundle[] bundles = null;
        if ((ids == null) || (ids.length == 0))
        {
            bundles = m_context.getBundles();
        }
        else
        {
            bundles = new Bundle[ids.length];
            for (int idIdx = 0; idIdx < ids.length; idIdx++)
            {
                try
                {
                    long l = Long.parseLong(ids[idIdx]);
                    Bundle b = m_context.getBundle(l);
                    if (b == null)
                    {
                        err.println("Bundle ID " + ids[idIdx] + " is invalid.");
                    }
                    bundles[idIdx] = b;
                }
                catch (NumberFormatException ex)
                {
                    err.println("Unable to parse id '" + ids[idIdx] + "'.");
                }
            }
        }

        for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
        {
            try
            {
                if (bundles[bundleIdx] != null)
                {
                    if (separatorNeeded)
                    {
                        out.println("");
                    }
                    _printRequiredBundles(bundles[bundleIdx], out, err);
                    separatorNeeded = true;
                }
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

    private void printFragmentHosts(String[] ids, PrintStream out, PrintStream err)
    {
        PackageAdmin pa = getPackageAdmin();
        if (pa == null)
        {
            out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            Bundle[] bundles = null;
            if ((ids == null) || (ids.length == 0))
            {
                bundles = m_context.getBundles();
            }
            else
            {
                bundles = new Bundle[ids.length];
                for (int idIdx = 0; idIdx < ids.length; idIdx++)
                {
                    try
                    {
                        long l = Long.parseLong(ids[idIdx]);
                        Bundle b = m_context.getBundle(l);
                        if (b == null)
                        {
                            err.println("Bundle ID " + ids[idIdx] + " is invalid.");
                        }
                        bundles[idIdx] = b;
                    }
                    catch (NumberFormatException ex)
                    {
                        err.println("Unable to parse id '" + ids[idIdx] + "'.");
                    }
                }
            }

            for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
            {
                // Print a separator for some whitespace.
                if (bundleIdx > 0)
                {
                    out.println("");
                }

                try
                {
                    if ((bundles[bundleIdx] != null) && isFragment(bundles[bundleIdx]))
                    {
                        String title = bundles[bundleIdx] + " is attached to:";
                        out.println(title);
                        out.println(Util.getUnderlineString(title));
                        Bundle[] hosts = pa.getHosts(bundles[bundleIdx]);
                        for (int hostIdx = 0;
                            (hosts != null) && (hostIdx < hosts.length);
                            hostIdx++)
                        {
                            out.println(hosts[hostIdx]);
                        }
                        if ((hosts == null) || (hosts.length == 0))
                        {
                            out.println("Nothing");
                        }
                    }
                    else if ((bundles[bundleIdx] != null) && !isFragment(bundles[bundleIdx]))
                    {
                        out.println("Bundle " + bundles[bundleIdx] + " is not a fragment.");
                    }
                }
                catch (Exception ex)
                {
                    err.println(ex.toString());
                }
            }
        }
    }

    private void printHostedFragments(String[] ids, PrintStream out, PrintStream err)
    {
        PackageAdmin pa = getPackageAdmin();
        if (pa == null)
        {
            out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            Bundle[] bundles = null;
            if ((ids == null) || (ids.length == 0))
            {
                bundles = m_context.getBundles();
            }
            else
            {
                bundles = new Bundle[ids.length];
                for (int idIdx = 0; idIdx < ids.length; idIdx++)
                {
                    try
                    {
                        long l = Long.parseLong(ids[idIdx]);
                        Bundle b = m_context.getBundle(l);
                        if (b == null)
                        {
                            err.println("Bundle ID " + ids[idIdx] + " is invalid.");
                        }
                        bundles[idIdx] = b;
                    }
                    catch (NumberFormatException ex)
                    {
                        err.println("Unable to parse id '" + ids[idIdx] + "'.");
                    }
                }
            }

            for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
            {
                // Print a separator for some whitespace.
                if (bundleIdx > 0)
                {
                    out.println("");
                }

                try
                {
                    if ((bundles[bundleIdx] != null) && !isFragment(bundles[bundleIdx]))
                    {
                        String title = bundles[bundleIdx] + " hosts:";
                        out.println(title);
                        out.println(Util.getUnderlineString(title));
                        Bundle[] fragments = pa.getFragments(bundles[bundleIdx]);
                        for (int fragIdx = 0;
                            (fragments != null) && (fragIdx < fragments.length);
                            fragIdx++)
                        {
                            out.println(fragments[fragIdx]);
                        }
                        if ((fragments == null) || (fragments.length == 0))
                        {
                            out.println("Nothing");
                        }
                    }
                    else if ((bundles[bundleIdx] != null) && isFragment(bundles[bundleIdx]))
                    {
                        out.println("Bundle " + bundles[bundleIdx] + " is a fragment.");
                    }
                }
                catch (Exception ex)
                {
                    err.println(ex.toString());
                }
            }
        }
    }

    public void printExportedServices(String[] ids, PrintStream out, PrintStream err)
    {
        Bundle[] bundles = null;
        if ((ids == null) || (ids.length == 0))
        {
            bundles = m_context.getBundles();
        }
        else
        {
            bundles = new Bundle[ids.length];
            for (int idIdx = 0; idIdx < ids.length; idIdx++)
            {
                try
                {
                    long l = Long.parseLong(ids[idIdx]);
                    Bundle b = m_context.getBundle(l);
                    if (b == null)
                    {
                        err.println("Bundle ID " + ids[idIdx] + " is invalid.");
                    }
                    bundles[idIdx] = b;
                }
                catch (NumberFormatException ex)
                {
                    err.println("Unable to parse id '" + ids[idIdx] + "'.");
                }
            }
        }

        for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
        {
            // Print a separator for some whitespace.
            if (bundleIdx > 0)
            {
                out.println("");
            }

            try
            {
                if (bundles[bundleIdx] != null)
                {
                    ServiceReference[] refs = bundles[bundleIdx].getRegisteredServices();

                    // Print header if we have not already done so.
                    String title = Util.getBundleName(bundles[bundleIdx]) + " provides services:";
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
            }
            catch (Exception ex)
            {
                err.println(ex.toString());
            }
        }
    }

    public void printImportedServices(String[] ids, PrintStream out, PrintStream err)
    {
        Bundle[] bundles = null;
        if ((ids == null) || (ids.length == 0))
        {
            bundles = m_context.getBundles();
        }
        else
        {
            bundles = new Bundle[ids.length];
            for (int idIdx = 0; idIdx < ids.length; idIdx++)
            {
                try
                {
                    long l = Long.parseLong(ids[idIdx]);
                    Bundle b = m_context.getBundle(l);
                    if (b == null)
                    {
                        err.println("Bundle ID " + ids[idIdx] + " is invalid.");
                    }
                    bundles[idIdx] = b;
                }
                catch (NumberFormatException ex)
                {
                    err.println("Unable to parse id '" + ids[idIdx] + "'.");
                }
            }
        }

        for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
        {
            // Print a separator for some whitespace.
            if (bundleIdx > 0)
            {
                out.println("");
            }

            try
            {
                if (bundles[bundleIdx] != null)
                {
                    ServiceReference[] refs = bundles[bundleIdx].getServicesInUse();

                    // Print header if we have not already done so.
                    String title = Util.getBundleName(bundles[bundleIdx]) + " requires services:";
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
                        // Print the registering bundle.
                        out.println("Registering bundle = " + refs[refIdx].getBundle());
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

    private static boolean isValidType(String type)
    {
        return (PACKAGE_TYPE.startsWith(type) || BUNDLE_TYPE.startsWith(type)
            || FRAGMENT_TYPE.startsWith(type) || SERVICE_TYPE.startsWith(type));
    }

    private static boolean isValidDirection(String direction)
    {
        return (CAPABILITY.startsWith(direction) || REQUIREMENT.startsWith(direction));
    }

    private static boolean isFragment(Bundle bundle)
    {
        return bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
    }
}