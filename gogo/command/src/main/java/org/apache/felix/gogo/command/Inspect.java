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
package org.apache.felix.gogo.command;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

public class Inspect
{
    public static final String PACKAGE_TYPE = "package";
    public static final String BUNDLE_TYPE = "bundle";
    public static final String FRAGMENT_TYPE = "fragment";
    public static final String SERVICE_TYPE = "service";

    public static final String CAPABILITY = "capability";
    public static final String REQUIREMENT = "requirement";

    public static void inspect(
        BundleContext bc, String type, String direction, Bundle[] bundles)
    {
        // Verify arguments.
        if (isValidType(type) && isValidDirection(direction))
        {
            // Now determine what needs to be printed.
            if (PACKAGE_TYPE.startsWith(type))
            {
                if (CAPABILITY.startsWith(direction))
                {
                    printExportedPackages(bc, bundles);
                }
                else
                {
                    printImportedPackages(bc, bundles);
                }
            }
            else if (BUNDLE_TYPE.startsWith(type))
            {
                if (CAPABILITY.startsWith(direction))
                {
                    printRequiringBundles(bc, bundles);
                }
                else
                {
                    printRequiredBundles(bc, bundles);
                }
            }
            else if (FRAGMENT_TYPE.startsWith(type))
            {
                if (CAPABILITY.startsWith(direction))
                {
                    printFragmentHosts(bc, bundles);
                }
                else
                {
                    printHostedFragments(bc, bundles);
                }
            }
            else
            {
                if (CAPABILITY.startsWith(direction))
                {
                    printExportedServices(bc, bundles);
                }
                else
                {
                    printImportedServices(bc, bundles);
                }
            }
        }
        else
        {
            if (!isValidType(type))
            {
                System.out.println("Invalid argument: " + type);
            }
            if (!isValidDirection(direction))
            {
                System.out.println("Invalid argument: " + direction);
            }
        }
    }

    public static void printExportedPackages(BundleContext bc, Bundle[] bundles)
    {
        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get package admin service.
        PackageAdmin pa = Util.getService(bc, PackageAdmin.class, refs);
        if (pa == null)
        {
            System.out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            boolean separatorNeeded = false;

            if ((bundles == null) || (bundles.length == 0))
            {
                bundles = bc.getBundles();
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
                            System.out.println("");
                        }
                        String title = bundles[bundleIdx] + " exports packages:";
                        System.out.println(title);
                        System.out.println(Util.getUnderlineString(title.length()));
                        if ((exports != null) && (exports.length > 0))
                        {
                            for (int expIdx = 0; expIdx < exports.length; expIdx++)
                            {
                                System.out.print(exports[expIdx]);
                                Bundle[] importers = exports[expIdx].getImportingBundles();
                                if ((importers == null) || (importers.length == 0))
                                {
                                    System.out.println(" UNUSED");
                                }
                                else
                                {
                                    System.out.println(" imported by:");
                                    for (int impIdx = 0; impIdx < importers.length; impIdx++)
                                    {
                                        System.out.println("   " + importers[impIdx]);
                                    }
                                }
                            }
                        }
                        else
                        {
                            System.out.println("Nothing");
                        }
                        separatorNeeded = true;
                    }
                }
                catch (Exception ex)
                {
                    System.err.println(ex.toString());
                }
            }
            Util.ungetServices(bc, refs);
        }
    }

    public static void printImportedPackages(BundleContext bc, Bundle[] bundles)
    {
        boolean separatorNeeded = false;

        if ((bundles == null) || (bundles.length == 0))
        {
            bundles = bc.getBundles();
        }

        for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
        {
            try
            {
                if (bundles[bundleIdx] != null)
                {
                    if (separatorNeeded)
                    {
                        System.out.println("");
                    }
                    _printImportedPackages(bc, bundles[bundleIdx]);
                    separatorNeeded = true;
                }
            }
            catch (Exception ex)
            {
                System.err.println(ex.toString());
            }
        }
    }

    private static void _printImportedPackages(BundleContext bc, Bundle bundle)
    {
        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get package admin service.
        PackageAdmin pa = Util.getService(bc, PackageAdmin.class, refs);
        if (pa == null)
        {
            System.out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            ExportedPackage[] exports = pa.getExportedPackages((Bundle) null);
            String title = bundle + " imports packages:";
            System.out.println(title);
            System.out.println(Util.getUnderlineString(title.length()));
            boolean found = false;
            for (int expIdx = 0; expIdx < exports.length; expIdx++)
            {
                Bundle[] importers = exports[expIdx].getImportingBundles();
                for (int impIdx = 0; (importers != null) && (impIdx < importers.length); impIdx++)
                {
                    if (importers[impIdx] == bundle)
                    {
                        System.out.println(exports[expIdx]
                            + " -> " + exports[expIdx].getExportingBundle());
                        found = true;
                    }
                }
            }
            if (!found)
            {
                System.out.println("Nothing");
            }
            Util.ungetServices(bc, refs);
        }
    }

    public static void printRequiringBundles(BundleContext bc, Bundle[] bundles)
    {
        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get package admin service.
        PackageAdmin pa = Util.getService(bc, PackageAdmin.class, refs);
        if (pa == null)
        {
            System.out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            boolean separatorNeeded = false;

            if ((bundles == null) || (bundles.length == 0))
            {
                bundles = bc.getBundles();
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
                                    System.out.println("");
                                }
                                String title = bundles[bundleIdx] + " is required by:";
                                System.out.println(title);
                                System.out.println(Util.getUnderlineString(title.length()));
                                if ((rbs[rbIdx].getRequiringBundles() != null)
                                    && (rbs[rbIdx].getRequiringBundles().length > 0))
                                {
                                    for (int reqIdx = 0;
                                        reqIdx < rbs[rbIdx].getRequiringBundles().length;
                                        reqIdx++)
                                    {
                                        System.out.println(rbs[rbIdx].getRequiringBundles()[reqIdx]);
                                    }
                                }
                                else
                                {
                                    System.out.println("Nothing");
                                }
                                separatorNeeded = true;
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    System.err.println(ex.toString());
                }
            }
            Util.ungetServices(bc, refs);
        }
    }

    public static void printRequiredBundles(BundleContext bc, Bundle[] bundles)
    {
        boolean separatorNeeded = false;

        if ((bundles == null) || (bundles.length == 0))
        {
            bundles = bc.getBundles();
        }

        for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
        {
            try
            {
                if (bundles[bundleIdx] != null)
                {
                    if (separatorNeeded)
                    {
                        System.out.println("");
                    }
                    _printRequiredBundles(bc, bundles[bundleIdx]);
                    separatorNeeded = true;
                }
            }
            catch (Exception ex)
            {
                System.err.println(ex.toString());
            }
        }
    }

    private static void _printRequiredBundles(BundleContext bc, Bundle bundle)
    {
        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get package admin service.
        PackageAdmin pa = Util.getService(bc, PackageAdmin.class, refs);
        if (pa == null)
        {
            System.out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            RequiredBundle[] rbs = pa.getRequiredBundles(null);
            String title = bundle + " requires bundles:";
            System.out.println(title);
            System.out.println(Util.getUnderlineString(title.length()));
            boolean found = false;
            for (int rbIdx = 0; rbIdx < rbs.length; rbIdx++)
            {
                Bundle[] requirers = rbs[rbIdx].getRequiringBundles();
                for (int reqIdx = 0; (requirers != null) && (reqIdx < requirers.length); reqIdx++)
                {
                    if (requirers[reqIdx] == bundle)
                    {
                        System.out.println(rbs[reqIdx]);
                        found = true;
                    }
                }
            }
            if (!found)
            {
                System.out.println("Nothing");
            }
            Util.ungetServices(bc, refs);
        }
    }

    public static void printFragmentHosts(BundleContext bc, Bundle[] bundles)
    {
        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get package admin service.
        PackageAdmin pa = Util.getService(bc, PackageAdmin.class, refs);
        if (pa == null)
        {
            System.out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            if ((bundles == null) || (bundles.length == 0))
            {
                bundles = bc.getBundles();
            }

            for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
            {
                // Print a separator for some whitespace.
                if (bundleIdx > 0)
                {
                    System.out.println("");
                }

                try
                {
                    if ((bundles[bundleIdx] != null) && isFragment(bundles[bundleIdx]))
                    {
                        String title = bundles[bundleIdx] + " is attached to:";
                        System.out.println(title);
                        System.out.println(Util.getUnderlineString(title.length()));
                        Bundle[] hosts = pa.getHosts(bundles[bundleIdx]);
                        for (int hostIdx = 0;
                            (hosts != null) && (hostIdx < hosts.length);
                            hostIdx++)
                        {
                            System.out.println(hosts[hostIdx]);
                        }
                        if ((hosts == null) || (hosts.length == 0))
                        {
                            System.out.println("Nothing");
                        }
                    }
                    else if ((bundles[bundleIdx] != null) && !isFragment(bundles[bundleIdx]))
                    {
                        System.out.println("Bundle " + bundles[bundleIdx] + " is not a fragment.");
                    }
                }
                catch (Exception ex)
                {
                    System.err.println(ex.toString());
                }
            }
            Util.ungetServices(bc, refs);
        }
    }

    public static void printHostedFragments(BundleContext bc, Bundle[] bundles)
    {
        // Keep track of service references.
        List<ServiceReference> refs = new ArrayList();

        // Get package admin service.
        PackageAdmin pa = Util.getService(bc, PackageAdmin.class, refs);
        if (pa == null)
        {
            System.out.println("PackageAdmin service is unavailable.");
        }
        else
        {
            if ((bundles == null) || (bundles.length == 0))
            {
                bundles = bc.getBundles();
            }

            for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
            {
                // Print a separator for some whitespace.
                if (bundleIdx > 0)
                {
                    System.out.println("");
                }

                try
                {
                    if ((bundles[bundleIdx] != null) && !isFragment(bundles[bundleIdx]))
                    {
                        String title = bundles[bundleIdx] + " hosts:";
                        System.out.println(title);
                        System.out.println(Util.getUnderlineString(title.length()));
                        Bundle[] fragments = pa.getFragments(bundles[bundleIdx]);
                        for (int fragIdx = 0;
                            (fragments != null) && (fragIdx < fragments.length);
                            fragIdx++)
                        {
                            System.out.println(fragments[fragIdx]);
                        }
                        if ((fragments == null) || (fragments.length == 0))
                        {
                            System.out.println("Nothing");
                        }
                    }
                    else if ((bundles[bundleIdx] != null) && isFragment(bundles[bundleIdx]))
                    {
                        System.out.println("Bundle " + bundles[bundleIdx] + " is a fragment.");
                    }
                }
                catch (Exception ex)
                {
                    System.err.println(ex.toString());
                }
            }
            Util.ungetServices(bc, refs);
        }
    }

    public static void printExportedServices(BundleContext bc, Bundle[] bundles)
    {
        if ((bundles == null) || (bundles.length == 0))
        {
            bundles = bc.getBundles();
        }

        for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
        {
            // Print a separator for some whitespace.
            if (bundleIdx > 0)
            {
                System.out.println("");
            }

            try
            {
                if (bundles[bundleIdx] != null)
                {
                    ServiceReference[] refs = bundles[bundleIdx].getRegisteredServices();

                    // Print header if we have not already done so.
                    String title = Util.getBundleName(bundles[bundleIdx]) + " provides services:";
                    System.out.println(title);
                    System.out.println(Util.getUnderlineString(title.length()));

                    if ((refs == null) || (refs.length == 0))
                    {
                        System.out.println("Nothing");
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
                            System.out.println(
                                keys[keyIdx] + " = " + Util.getValueString(v));
                        }

                        // Print service separator if necessary.
                        if ((refIdx + 1) < refs.length)
                        {
                            System.out.println("----");
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                System.err.println(ex.toString());
            }
        }
    }

    public static void printImportedServices(BundleContext bc, Bundle[] bundles)
    {
        if ((bundles == null) || (bundles.length == 0))
        {
            bundles = bc.getBundles();
        }

        for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
        {
            // Print a separator for some whitespace.
            if (bundleIdx > 0)
            {
                System.out.println("");
            }

            try
            {
                if (bundles[bundleIdx] != null)
                {
                    ServiceReference[] refs = bundles[bundleIdx].getServicesInUse();

                    // Print header if we have not already done so.
                    String title = Util.getBundleName(bundles[bundleIdx]) + " requires services:";
                    System.out.println(title);
                    System.out.println(Util.getUnderlineString(title.length()));

                    if ((refs == null) || (refs.length == 0))
                    {
                        System.out.println("Nothing");
                    }

                    // Print properties for each service.
                    for (int refIdx = 0;
                        (refs != null) && (refIdx < refs.length);
                        refIdx++)
                    {
                        // Print the registering bundle.
                        System.out.println("Registering bundle = " + refs[refIdx].getBundle());
                        // Print service properties.
                        String[] keys = refs[refIdx].getPropertyKeys();
                        for (int keyIdx = 0;
                            (keys != null) && (keyIdx < keys.length);
                            keyIdx++)
                        {
                            Object v = refs[refIdx].getProperty(keys[keyIdx]);
                            System.out.println(
                                keys[keyIdx] + " = " + Util.getValueString(v));
                        }

                        // Print service separator if necessary.
                        if ((refIdx + 1) < refs.length)
                        {
                            System.out.println("----");
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                System.err.println(ex.toString());
            }
        }
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