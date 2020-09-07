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
package org.apache.karaf.features.command;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@Command(scope = "feature", name = "regions", description = "Prints information about region digraph.")
@Service
public class RegionInfoCommand implements Action {

    @Option(name = "-v", aliases = "--verbose", required = false, description = "Show all info.")
    boolean verbose;

    @Option(name = "-b", aliases = "--bundles", required = false, description = "Show bundles in each region.")
    boolean bundles;

    @Option(name = "-f", aliases = "--filters", required = false, description = "Show filters.")
    boolean filters;

    @Option(name = "-n", aliases = "--namespaces", required = false, description = "Show namespaces in each filter.")
    boolean namespaces;

    @Argument(index = 0, name = "regions", description = "Regions to provide detailed info for.", required = false, multiValued = true)
    List<String> regions;

    @Reference
    RegionDigraph regionDigraph;

    @Reference
    BundleContext bundleContext;

    public Object execute() throws Exception {
        System.out.println("Regions");
        if (regions == null) {
            for (Region region : regionDigraph.getRegions()) {
                showRegion(region);
            }
        } else {
            bundles = true;
            filters = true;
            namespaces = true;
            for (String regionName : regions) {
                Region region = regionDigraph.getRegion(regionName);
                if (region == null) {
                    System.out.println("No region " + regionName);
                } else {
                    showRegion(region);
                }
            }
        }
        return null;
    }

    private void showRegion(Region region) {
        BundleContext bundleContext = this.bundleContext.getBundle(0).getBundleContext();
        System.out.println(region.getName());
        if (verbose || bundles) {
            for (Long id : new TreeSet<>(region.getBundleIds())) {
                Bundle b = bundleContext.getBundle(id);
                System.out.printf("  %3d  %s%s%n", id, getStateString(b), b);
            }
        }
        if (verbose || filters || namespaces) {
            for (RegionDigraph.FilteredRegion f : region.getEdges()) {
                System.out.println("  filter to " + f.getRegion().getName());
                if (verbose || namespaces) {
                    RegionFilter rf = f.getFilter();
                    for (Map.Entry<String, Collection<String>> policy : rf.getSharingPolicy().entrySet()) {
                        String namespace = policy.getKey();
                        System.out.println("    namespace: " + namespace);
                        for (String e : policy.getValue()) {
                            System.out.println("      " + e);
                        }
                    }
                }
            }
        }
    }

    public String getStateString(Bundle bundle) {
        if (bundle == null) {
            return "Bundle null ";
        }
        int state = bundle.getState();
        if (state == Bundle.ACTIVE) {
            return "Active      ";
        } else if (state == Bundle.INSTALLED) {
            return "Installed   ";
        } else if (state == Bundle.RESOLVED) {
            return "Resolved    ";
        } else if (state == Bundle.STARTING) {
            return "Starting    ";
        } else if (state == Bundle.STOPPING) {
            return "Stopping    ";
        } else {
            return "Unknown     ";
        }
    }

}
