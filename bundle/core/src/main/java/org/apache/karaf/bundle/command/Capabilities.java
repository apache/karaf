/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.bundle.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.ShellUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

@Command(scope = "bundle", name = "capabilities", description = "Displays OSGi capabilities of a given bundles.")
@Service
public class Capabilities extends BundlesCommand {

    public static final String NONSTANDARD_SERVICE_NAMESPACE = "service";

    private static final String EMPTY_MESSAGE = "[EMPTY]";
    private static final String UNUSED_MESSAGE = "[UNUSED]";

    @Option(name = "--namespace")
    String namespace = "*";

    @Override
    protected Object doExecute(List<Bundle> bundles) throws Exception {
        boolean separatorNeeded = false;
        Pattern ns = Pattern.compile(namespace.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*"));
        for (Bundle b : bundles)
        {
            if (separatorNeeded)
            {
                System.out.println();
            }

            // Print out any matching generic capabilities.
            BundleWiring wiring = b.adapt(BundleWiring.class);
            if (wiring != null)
            {
                String title = b + " provides:";
                System.out.println(title);
                System.out.println(ShellUtil.getUnderlineString(title));

                // Print generic capabilities for matching namespaces.
                boolean matches = printMatchingCapabilities(wiring, ns);

                // Handle service capabilities separately, since they aren't part
                // of the generic model in OSGi.
                if (matchNamespace(ns, NONSTANDARD_SERVICE_NAMESPACE))
                {
                    matches |= printServiceCapabilities(b);
                }

                // If there were no capabilities for the specified namespace,
                // then say so.
                if (!matches)
                {
                    System.out.println(namespace + " " + EMPTY_MESSAGE);
                }
            }
            else
            {
                System.out.println("Bundle " + b.getBundleId() + " is not resolved.");
            }
            separatorNeeded = true;
        }
        return null;
    }

    @Override
    protected void executeOnBundle(Bundle bundle) throws Exception {
    }

    private static boolean printMatchingCapabilities(BundleWiring wiring, Pattern namespace)
    {
        List<BundleWire> wires = wiring.getProvidedWires(null);
        Map<BundleCapability, List<BundleWire>> aggregateCaps =
            aggregateCapabilities(namespace, wires);
        List<BundleCapability> allCaps = wiring.getCapabilities(null);
        boolean matches = false;
        for (BundleCapability cap : allCaps)
        {
            if (matchNamespace(namespace, cap.getNamespace()))
            {
                matches = true;
                List<BundleWire> dependents = aggregateCaps.get(cap);
                Object keyAttr =
                    cap.getAttributes().get(cap.getNamespace());
                if (dependents != null)
                {
                    String msg;
                    if (keyAttr != null)
                    {
                        msg = cap.getNamespace()
                            + "; "
                            + keyAttr
                            + " "
                            + getVersionFromCapability(cap);
                    }
                    else
                    {
                        msg = cap.toString();
                    }
                    msg = msg + " required by:";
                    System.out.println(msg);
                    for (BundleWire wire : dependents)
                    {
                        System.out.println("   " + wire.getRequirerWiring().getBundle());
                    }
                }
                else if (keyAttr != null)
                {
                    System.out.println(cap.getNamespace()
                        + "; "
                        + cap.getAttributes().get(cap.getNamespace())
                        + " "
                        + getVersionFromCapability(cap)
                        + " "
                        + UNUSED_MESSAGE);
                }
                else
                {
                    System.out.println(cap + " " + UNUSED_MESSAGE);
                }
            }
        }
        return matches;
    }

    private static Map<BundleCapability, List<BundleWire>> aggregateCapabilities(
        Pattern namespace, List<BundleWire> wires)
    {
        // Aggregate matching capabilities.
        Map<BundleCapability, List<BundleWire>> map =
                new HashMap<>();
        for (BundleWire wire : wires)
        {
            if (matchNamespace(namespace, wire.getCapability().getNamespace()))
            {
                map.computeIfAbsent(wire.getCapability(), k -> new ArrayList<>()).add(wire);
            }
        }
        return map;
    }

    static boolean printServiceCapabilities(Bundle b)
    {
        boolean matches = false;

        try
        {
            ServiceReference<?>[] refs = b.getRegisteredServices();

            if ((refs != null) && (refs.length > 0))
            {
                matches = true;
                // Print properties for each service.
                for (ServiceReference<?> ref : refs)
                {
                    // Print object class with "namespace".
                    System.out.println(
                        NONSTANDARD_SERVICE_NAMESPACE
                        + "; "
                        + ShellUtil.getValueString(ref.getProperty("objectClass"))
                        + " with properties:");
                    // Print service properties.
                    String[] keys = ref.getPropertyKeys();
                    for (String key : keys)
                    {
                        if (!key.equalsIgnoreCase(Constants.OBJECTCLASS))
                        {
                            Object v = ref.getProperty(key);
                            System.out.println("   "
                                + key + " = " + ShellUtil.getValueString(v));
                        }
                    }
                    Bundle[] users = ref.getUsingBundles();
                    if ((users != null) && (users.length > 0))
                    {
                        System.out.println("   Used by:");
                        for (Bundle user : users)
                        {
                            System.out.println("      " + user);
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            System.err.println(ex.toString());
        }

        return matches;
    }

    private static String getVersionFromCapability(BundleCapability c)
    {
        Object o = c.getAttributes().get(Constants.VERSION_ATTRIBUTE);
        if (o == null)
        {
            o = c.getAttributes().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
        }
        return (o == null) ? "" : o.toString();
    }

    private static boolean matchNamespace(Pattern namespace, String actual)
    {
        return namespace.matcher(actual).matches();
    }

}
