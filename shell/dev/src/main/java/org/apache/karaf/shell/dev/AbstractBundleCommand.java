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
package org.apache.karaf.shell.dev;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Base class for a dev: command that takes a bundle id as an argument
 *
 * It also provides convient access to the PackageAdmin service
 */
public abstract class AbstractBundleCommand extends OsgiCommandSupport {

    @Argument(index = 0, name = "id", description = "The bundle ID", required = true)
    Long id;

    @Override
    protected Object doExecute() throws Exception {
        Bundle bundle = getBundleContext().getBundle(id);
        if (bundle == null) {
            System.err.println("Bundle ID " + id + " is invalid");
            return null;
        }

        doExecute(bundle);
        
        return null;
    }

    protected abstract void doExecute(Bundle bundle) throws Exception;

    /*
     * Get the list of bundles from which the given bundle imports packages
     */
    protected Map<String, Bundle> getWiredBundles(Bundle bundle) {
        // the set of bundles from which the bundle imports packages
        Map<String, Bundle> exporters = new HashMap<String, Bundle>();

        for (BundleRevision revision : bundle.adapt(BundleRevisions.class).getRevisions()) {
            BundleWiring wiring = revision.getWiring();
            if (wiring != null) {
                List<BundleWire> wires = wiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
                if (wires != null) {
                    for (BundleWire wire : wires) {
                        if (wire.getProviderWiring().getBundle().getBundleId() != 0) {
                            exporters.put(wire.getCapability().getAttributes().get(BundleRevision.PACKAGE_NAMESPACE).toString(),
                                          wire.getProviderWiring().getBundle());
                        }
                    }
                }
            }
        }
        return exporters;
    }

}
