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

import java.io.IOException;
import static java.lang.String.format;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.ops4j.pax.url.wrap.Handler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command for enabling/disabling debug logging on a bundle and calculating the difference in
 * wired imports.
 */
@Command(scope = "dev", name = "dynamic-import", description = "Enable/disable dynamic-import for a given bundle")
public class DynamicImport extends AbstractBundleCommand {

    private final Logger LOG = LoggerFactory.getLogger(DynamicImport.class);

    /**
     * The header key where we store the active wires when we enable DynamicImport=*
     */
    protected static final String ORIGINAL_WIRES = "Original-Wires";

    @Override
    protected void doExecute(Bundle bundle) throws Exception {
        if (bundle.getHeaders().get(ORIGINAL_WIRES) == null) {
            enableDynamicImports(bundle);
        } else {
            disableDynamicImports(bundle);
        }
    }

    /*
     * Enable DynamicImport=* on the bundle
     */
    private void enableDynamicImports(Bundle bundle) throws IOException, BundleException {
        System.out.printf("Enabling dynamic imports on bundle %s%n", bundle);

        String location =
                String.format("wrap:%s$" +
                        "Bundle-UpdateLocation=%s&" +
                        "DynamicImport-Package=*&" +
                        "%s=%s&" +
                        "overwrite=merge",
                        bundle.getLocation(),
                        bundle.getLocation(),
                        ORIGINAL_WIRES,
                        explode(getWiredBundles(bundle).keySet()));
        LOG.debug(format("Updating %s with URL %s", bundle, location));

        URL url = new URL(location);
        bundle.update(url.openStream());
        getPackageAdmin().refreshPackages(new Bundle[] {bundle});
    }

    /*
     * Disable DynamicImport=* on the bundle
     *
     * At this time, we will also calculate the difference in package wiring for the bundle compared to
     * when we enabled the DynamicImport
     */
    private void disableDynamicImports(Bundle bundle) throws BundleException {
        System.out.printf("Disabling dynamic imports on bundle %s%n", bundle);

        Set<String> current = getWiredBundles(bundle).keySet();
        for (String original : bundle.getHeaders().get(ORIGINAL_WIRES).toString().split(",")) {
            current.remove(original);
        }

        if (current.isEmpty()) {
            System.out.println("(no additional packages have been wired since dynamic import was enabled)");
        } else {
            System.out.printf("%nAdditional packages wired since dynamic import was enabled:%n");
            for (String pkg : current) {
                System.out.printf("- %s%n", pkg);
            }
        }

        bundle.update();
    }

    /*
     * Explode a set of string values in to a ,-delimited string
     */
    private String explode(Set<String> set) {
        StringBuffer result = new StringBuffer();
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            result.append(it.next());
            if (it.hasNext()) {
                result.append(",");
            }
        }
        if (result.length() == 0) {
            return "--none--";
        }
        return result.toString();
    }
}
