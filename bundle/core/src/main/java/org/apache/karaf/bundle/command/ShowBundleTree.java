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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.bundle.command.bundletree.Node;
import org.apache.karaf.bundle.command.bundletree.Tree;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

/**
 * Command for showing the full tree of bundles that have been used to resolve
 * a given bundle.
 */
@Command(scope = "bundle", name = "tree-show", description = "Shows the tree of bundles based on the wiring information.")
@Service
public class ShowBundleTree extends BundleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShowBundleTree.class);

    @Option(name = "-v", aliases = { "--version" }, description = "Show bundle versions")
    private boolean versions;

    private Tree<Bundle> tree;

    @Override
    protected Object doExecute(Bundle bundle) throws Exception {
        long start = System.currentTimeMillis();
        // let's do the real work here
        printHeader(bundle);
        tree = new Tree<>(bundle);
        createTree(bundle);
        printTree(tree);
        printDuplicatePackages(tree);
        LOGGER.debug(format("Dependency tree calculated in %d ms",
                            System.currentTimeMillis() - start));
        return null;
    }
    
    /**
     * Return a String representation of a bundle state
     */
    private String getState(Bundle bundle) {
        switch (bundle.getState()) {
            case Bundle.UNINSTALLED : return "UNINSTALLED";
            case Bundle.INSTALLED : return "INSTALLED";
            case Bundle.RESOLVED: return "RESOLVED";
            case Bundle.STARTING : return "STARTING";
            case Bundle.STOPPING : return "STOPPING";
            case Bundle.ACTIVE : return "ACTIVE";
            default : return "UNKNOWN";
        }
    }

    /*
     * Print the header
     */
    private void printHeader(Bundle bundle) {
        System.out.printf("Bundle %s [%s] is currently %s%n",
                bundle.getSymbolicName(),
                bundle.getBundleId(),
                getState(bundle));
    }

    /*
     * Print the dependency tree
     */
    private void printTree(Tree<Bundle> tree) {
        System.out.printf("%n");
        tree.write(System.out, node -> {
            if (versions) {
                return String.format("%s / [%s] [%s]",
                        node.getValue().getSymbolicName(),
                        node.getValue().getVersion().toString(),
                        node.getValue().getBundleId());
            } else {
                return String.format("%s [%s]",
                        node.getValue().getSymbolicName(),
                        node.getValue().getBundleId());
            }
        });
    }

    /*
     * Check for bundles in the tree exporting the same package
     * as a possible cause for 'Unresolved constraint...' on a uses-conflict
     */
    private void printDuplicatePackages(Tree<Bundle> tree) {
        Set<Bundle> bundles = tree.flatten();
        Map<String, Set<Bundle>> exports = new HashMap<>();

        for (Bundle bundle : bundles) {
            for (BundleRevision revision : bundle.adapt(BundleRevisions.class).getRevisions()) {
                BundleWiring wiring = revision.getWiring();
                if (wiring != null) {
                    List<BundleWire> wires = wiring.getProvidedWires(BundleRevision.PACKAGE_NAMESPACE);
                    if (wires != null) {
                        for (BundleWire wire : wires) {
                            String name = wire.getCapability().getAttributes().get(BundleRevision.PACKAGE_NAMESPACE).toString();
                            exports.computeIfAbsent(name, k -> new HashSet<>()).add(bundle);
                        }
                    }
                }
            }
        }
        
        for(Map.Entry<String, Set<Bundle>> entry : exports.entrySet()) {
        	Set<Bundle> bundlesExportingPkg = entry.getValue();
        	if(bundlesExportingPkg.size() > 1) {
        		System.out.printf("%n");
                System.out.printf("WARNING: multiple bundles are exporting package %s%n", entry.getKey());
                for (Bundle bundle : bundlesExportingPkg) {
                    System.out.printf("- %s%n", bundle);
                }
        	}
        }
    }

    /*
     * Creates the bundle tree
     */
    protected void createTree(Bundle bundle) {
        if (bundle.getState() >= Bundle.RESOLVED) {
            createNode(tree);
        } else {
            createNodesForImports(tree, bundle);
            System.out.print("\nWarning: the below tree is a rough approximation of a possible resolution");
        }
    }

    /*
     * Creates nodes for the imports of the bundle (instead of reporting wiring information
     */
    private void createNodesForImports(Node<Bundle> node, Bundle bundle) {
        Clause[] imports = Parser.parseHeader(bundle.getHeaders().get("Import-Package"));
        Clause[] exports = Parser.parseHeader(bundle.getHeaders().get("Export-Package"));
        for (Clause i : imports) {
            boolean exported = false;
            for (Clause e : exports) {
                if (e.getName().equals(i.getName())) {
                    exported = true;
                    break;
                }
            }
            if (!exported) {
                createNodeForImport(node, bundle, i);
            }
        }
    }

    /*
     * Create a child node for a given import (by finding a matching export in the currently installed bundles)
     */
    private void createNodeForImport(Node<Bundle> node, Bundle bundle, Clause i) {
        VersionRange range = VersionRange.parseVersionRange(i.getAttribute(Constants.VERSION_ATTRIBUTE));
        boolean foundMatch = false;
        for (Bundle b : bundleContext.getBundles()) {
            BundleWiring wiring = b.adapt(BundleWiring.class);
            if (wiring != null) {
                List<BundleCapability> caps = wiring.getCapabilities(BundleRevision.PACKAGE_NAMESPACE);
                if (caps != null) {
                    for (BundleCapability cap : caps) {
                        String n = getAttribute(cap, BundleRevision.PACKAGE_NAMESPACE);
                        String v = getAttribute(cap, Constants.VERSION_ATTRIBUTE);
                        if (i.getName().equals(n) && range.contains(VersionTable.getVersion(v))) {
                            boolean existing = tree.flatten().contains(b);
                            System.out.printf("- import %s: resolved using %s%n", i, b);
                            foundMatch = true;
                            if (!node.hasChild(b)) {
                                Node<Bundle> child = node.addChild(b);
                                if (!existing) {
                                    createNode(child);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!foundMatch) {
            System.out.printf("- import %s: WARNING - unable to find matching export%n", i);
        }
    }

    private String getAttribute(BundleCapability capability, String name) {
        Object o = capability.getAttributes().get(name);
        return o != null ? o.toString() : null;
    }

    /*
    * Creates a node in the bundle tree
    */
    private void createNode(Node<Bundle> node) {
        Bundle bundle = node.getValue();
        Collection<Bundle> exporters = new HashSet<>(bundleService.getWiredBundles(bundle).values());

        for (Bundle exporter : exporters) {
            if (node.hasAncestor(exporter)) {                
                LOGGER.debug(format("Skipping %s (already exists in the current branch)", exporter));
            } else {
                boolean existing = tree.flatten().contains(exporter);
                LOGGER.debug(format("Adding %s as a dependency for %s", exporter, bundle));
                Node<Bundle> child = node.addChild(exporter);
                if (existing) {
                    LOGGER.debug(format("Skipping children of %s (already exists in another branch)", exporter));
                } else {
                    createNode(child);
                }
            }
        }
    }
}
