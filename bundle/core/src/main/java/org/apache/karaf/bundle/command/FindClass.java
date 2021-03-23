package org.apache.karaf.bundle.command;

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

import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.ShellUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

@Command(scope = "bundle", name = "find-class", description = "Locates a specified class in any deployed bundle")
@Service
public class FindClass implements Action {

    @Argument(index = 0, name = "className", description = "Class name or partial class name to be found", required = true, multiValued = false)
    String className;

    @Reference
    BundleContext bundleContext;

    @Option(name = "-v", aliases = {}, description = "Show more information about the classes/resource", required = false, multiValued = false)
    boolean verbose;

    @Override
    public Object execute() throws Exception {
        findResource();
        return null;
    }

    protected void findResource() {
        Bundle[] bundles = bundleContext.getBundles();
        String path;
        String filter;
        int idx = className.lastIndexOf('.');
        if (idx >= 0) {
            path = className.substring(0, idx).replace('.', '/');
            if (path.isEmpty() || path.charAt(0) != '/') {
                path = "/" + path;
            }
            filter = className.substring(idx + 1) + ".class";
        } else {
            path = "/";
            filter = "*" + className + "*";
        }
        if (!verbose) {
            // old behavior
            for (Bundle bundle:bundles){
                BundleWiring wiring = bundle.adapt(BundleWiring.class);
                if (wiring != null){
                    Collection<String> resources = wiring.listResources(path, filter, BundleWiring.LISTRESOURCES_RECURSE);
                    if (resources.size() > 0){
                        String title = ShellUtil.getBundleName(bundle);
                        System.out.println("\n" + title);
                    }
                    for (String resource:resources){
                        System.out.println(resource);
                    }
                } else {
                    System.out.println("Bundle " + bundle.getBundleId() + " is not resolved.");
                }
            }
        } else {
            // more information
            for (Bundle bundle : bundles) {
                BundleWiring wiring = bundle.adapt(BundleWiring.class);
                if (wiring != null) {
                    // own content and attached fragments' content
                    List<URL> entries = wiring.findEntries(path, filter, BundleWiring.FINDENTRIES_RECURSE);
                    boolean hasEntries = entries != null && !entries.isEmpty();

                    // entries visible through wires
                    Collection<String> resources = wiring.listResources(path, filter, BundleWiring.LISTRESOURCES_RECURSE);
                    boolean hasResources = resources != null && !resources.isEmpty();

                    if (hasEntries || hasResources) {
                        String title = ShellUtil.getBundleName(bundle);
                        System.out.println("\n" + title);
                    }

                    if (hasEntries) {
                        System.out.println("  Resources from this bundle (and its fragments) content:");
                        for (URL entry : entries) {
                            System.out.println("    " + entry);
                        }
                    }
                    if (hasResources) {
                        Set<String> reqBundles = new LinkedHashSet<>();
                        Set<String> importedPackages = new LinkedHashSet<>();
                        for (BundleWire bw : wiring.getRequiredWires(null)) {
                            BundleCapability cap = bw.getCapability();
                            BundleRevision rcap = cap == null ? null : cap.getResource();
                            if (cap == null || rcap == null
                                    || rcap.getWiring() == null || rcap.getWiring().getBundle() == null) {
                                continue;
                            }
                            Collection<String> res = rcap.getWiring().listResources(path, filter, BundleWiring.LISTRESOURCES_RECURSE);
                            for (String r : res) {
                                if (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE)) {
                                    importedPackages.add("    " + r + " (visible through " + BundleRevision.PACKAGE_NAMESPACE + " from " + ShellUtil.getBundleName(rcap.getWiring().getBundle()) + ")");
                                } else if (cap.getNamespace().equals(BundleRevision.BUNDLE_NAMESPACE)) {
                                    reqBundles.add("    " + r + " (visible through " + BundleRevision.BUNDLE_NAMESPACE + " from " + ShellUtil.getBundleName(rcap.getWiring().getBundle()) + ")");
                                }
                            }
                        }
                        if (!(importedPackages.isEmpty() && reqBundles.isEmpty())) {
                            System.out.println("  Resources from bundle or wired bundles:");
                            for (String v : importedPackages) {
                                System.out.println(v);
                            }
                            for (String v : reqBundles) {
                                System.out.println(v);
                            }
                        }
                    }
                } else {
                    System.out.println("Bundle " + bundle.getBundleId() + " is not resolved.");
                }
            }
        }
    }

}
