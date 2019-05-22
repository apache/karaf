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
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

@Command(scope = "bundle", name = "requirements", description = "Displays OSGi requirements of a given bundles.")
@Service
public class Requirements extends BundlesCommand {

    public static final String NONSTANDARD_SERVICE_NAMESPACE = "service";

    private static final String EMPTY_MESSAGE = "[EMPTY]";
    private static final String UNRESOLVED_MESSAGE = "[UNRESOLVED]";

    @Option(name = "--namespace")
    String namespace = "*";

    @Override
    protected void executeOnBundle(Bundle bundle) throws Exception {
    }

    @Override
    protected Object doExecute(List<Bundle> bundles) throws Exception {
        boolean separatorNeeded = false;
        Pattern ns = Pattern.compile(namespace.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*"));
        for (Bundle b : bundles) {
            if (separatorNeeded) {
                System.out.println();
            }

            // Print out any matching generic requirements.
            BundleWiring wiring = b.adapt(BundleWiring.class);
            if (wiring != null) {
                String title = b + " requires:";
                System.out.println(title);
                System.out.println(ShellUtil.getUnderlineString(title));
                boolean matches = printMatchingRequirements(wiring, ns);

                // Handle service requirements separately, since they aren't part
                // of the generic model in OSGi.
                if (matchNamespace(ns, NONSTANDARD_SERVICE_NAMESPACE)) {
                    matches |= printServiceRequirements(b);
                }

                // If there were no requirements for the specified namespace,
                // then say so.
                if (!matches) {
                    System.out.println(namespace + " " + EMPTY_MESSAGE);
                }
            } else {
                System.out.println("Bundle " + b.getBundleId() + " is not resolved.");
            }

            separatorNeeded = true;
        }
        return null;
    }

    private static boolean printMatchingRequirements(BundleWiring wiring, Pattern namespace) {
        List<BundleWire> wires = wiring.getRequiredWires(null);
        Map<BundleRequirement, List<BundleWire>> aggregateReqs = aggregateRequirements(namespace, wires);
        List<BundleRequirement> allReqs = wiring.getRequirements(null);
        boolean matches = false;
        for (BundleRequirement req : allReqs) {
            if (matchNamespace(namespace, req.getNamespace())) {
                matches = true;
                List<BundleWire> providers = aggregateReqs.get(req);
                if (providers != null) {
                    System.out.println(req.getNamespace() + "; "
                                    + req.getDirectives().get(Constants.FILTER_DIRECTIVE) + " resolved by:");
                    for (BundleWire wire : providers) {
                        String msg;
                        Object keyAttr = wire.getCapability().getAttributes().get(wire.getCapability().getNamespace());
                        if (keyAttr != null) {
                            msg = wire.getCapability().getNamespace() + "; "
                                    + keyAttr + " " + getVersionFromCapability(wire.getCapability());
                        } else {
                            msg = wire.getCapability().toString();
                        }
                        msg = "   " + msg + " from " + wire.getProviderWiring().getBundle();
                        System.out.println(msg);
                    }
                } else {
                    System.out.println(req.getNamespace() + "; "
                                    + req.getDirectives().get(Constants.FILTER_DIRECTIVE) + " " + UNRESOLVED_MESSAGE);
                }
            }
        }
        return matches;
    }

    private static Map<BundleRequirement, List<BundleWire>> aggregateRequirements(
            Pattern namespace, List<BundleWire> wires) {
        // Aggregate matching capabilities.
        Map<BundleRequirement, List<BundleWire>> map = new HashMap<>();
        for (BundleWire wire : wires) {
            if (matchNamespace(namespace, wire.getRequirement().getNamespace())) {
                map.computeIfAbsent(wire.getRequirement(), k -> new ArrayList<>()).add(wire);
            }
        }
        return map;
    }

    static boolean printServiceRequirements(Bundle b) {
        boolean matches = false;

        try {
            ServiceReference<?>[] refs = b.getServicesInUse();

            if ((refs != null) && (refs.length > 0)) {
                matches = true;
                // Print properties for each service.
                for (ServiceReference<?> ref : refs) {
                    // Print object class with "namespace".
                    System.out.println(
                            NONSTANDARD_SERVICE_NAMESPACE
                                    + "; "
                                    + ShellUtil.getValueString(ref.getProperty("objectClass"))
                                    + " provided by:");
                    System.out.println("   " + ref.getBundle());
                }
            }
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }

        return matches;
    }

    private static String getVersionFromCapability(BundleCapability c) {
        Object o = c.getAttributes().get(Constants.VERSION_ATTRIBUTE);
        if (o == null) {
            o = c.getAttributes().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
        }
        return (o == null) ? "" : o.toString();
    }

    private static boolean matchNamespace(Pattern namespace, String actual) {
        return namespace.matcher(actual).matches();
    }

}
