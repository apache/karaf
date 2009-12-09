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
package org.apache.felix.karaf.shell.dev;

import static java.lang.String.format;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.karaf.shell.dev.util.Bundles;
import org.apache.felix.karaf.shell.dev.util.Import;
import org.apache.felix.karaf.shell.dev.util.Node;
import org.apache.felix.karaf.shell.dev.util.Tree;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command for showing the full tree of bundles that have been used to resolve
 * a given bundle.
 */
@Command(scope = "dev", name = "show-tree",
         description = "Show the tree of bundles based on the wiring information")
public class ShowBundleTree extends AbstractBundleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShowBundleTree.class);

    // a cache of all exported packages
    private ExportedPackage[] allExportedPackages;

    @Override
    protected void doExecute(Bundle bundle) throws Exception {
        // let's do the real work here
        printHeader(bundle);
        Tree<Bundle> tree = createTree(bundle);
        printTree(tree);
        printDuplicatePackages(tree);
    }

    /*
     * Print the header
     */
    private void printHeader(Bundle bundle) {
        System.out.printf("Bundle %s [%s] is currently %s%n",
                bundle.getSymbolicName(),
                bundle.getBundleId(),
                Bundles.toString(bundle.getState()));
    }

    /*
     * Print the dependency tree
     */
    private void printTree(Tree<Bundle> tree) {
        System.out.printf("%n");
        tree.write(System.out, new Tree.Converter<Bundle>() {

            public String toString(Node<Bundle> node) {
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
        Map<String, Set<Bundle>> exports = new HashMap<String, Set<Bundle>>();

        for (Bundle bundle : bundles) {
            ExportedPackage[] packages = getPackageAdmin().getExportedPackages(bundle);
            if (packages != null) {
                for (ExportedPackage p : packages) {
                    if (exports.get(p.getName()) == null) {
                        exports.put(p.getName(), new HashSet<Bundle>());
                    }
                    exports.get(p.getName()).add(bundle);
                }
            }
        }

        for (String pkg : exports.keySet()) {
            if (exports.get(pkg).size() > 1) {
                System.out.printf("%n");
                System.out.printf("WARNING: multiple bundles are exporting package %s%n", pkg);
                for (Bundle bundle : exports.get(pkg)) {
                    System.out.printf("- %s%n", bundle);
                }
            }
        }
    }

    /*
     * Creates the bundle tree
     */
    protected Tree<Bundle> createTree(Bundle bundle) {
        Tree<Bundle> tree = new Tree<Bundle>(bundle);
        Set<Bundle> trail = new HashSet<Bundle>();
        if (bundle.getState() >= Bundle.RESOLVED) {
            createNode(tree, trail);
        } else {
            for (Import i : Import.parse(String.valueOf(bundle.getHeaders().get("Import-Package")))) {
                for (ExportedPackage ep : getPackageAdmin().getExportedPackages(i.getPackage())) {
                    if (ep.getVersion().compareTo(i.getVersion()) >= 0) {
                        if (!bundle.equals(ep.getExportingBundle())) {
                            Node child = tree.addChild(ep.getExportingBundle());
                            System.out.printf("- using %s to resolve import %s%n", ep.getExportingBundle(), i);
                            createNode(child, trail);
                        }
                    }
                }
            }
        }
        return tree;
    }

    /*
     * Creates a node in the bundle tree
     */
    private void createNode(Node<Bundle> node, Set<Bundle> trail) {
        Bundle bundle = node.getValue();
        Collection<Bundle> exporters = new HashSet<Bundle>();
        exporters.addAll(getWiredBundles(bundle).values());

        for (Bundle exporter : exporters) {
            if (trail.contains(exporter)) {
                LOGGER.debug(format("Skipping %s because it already exists in the current tree branch", exporter));
            } else {
                trail.add(exporter);
                Node child = node.addChild(exporter);
                LOGGER.debug(format("Adding %s as a dependency for %s", exporter, bundle));
                createNode(child, trail);
                trail.remove(exporter);
            }
        }
    }
}
