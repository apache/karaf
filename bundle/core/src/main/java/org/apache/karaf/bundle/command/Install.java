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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.MultiException;
import org.apache.karaf.util.jaas.JaasHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.startlevel.BundleStartLevel;

@Command(scope = "bundle", name = "install", description = "Installs one or more bundles.")
@Service
public class Install implements Action {

    @Argument(index = 0, name = "urls", description = "Bundle URLs separated by whitespaces", required = true, multiValued = true)
    List<URI> urls;

    @Option(name = "-s", aliases={"--start"}, description="Starts the bundles after installation", required = false, multiValued = false)
    boolean start;

    @Option(name = "-l", aliases={"--start-level"}, description="Sets the start level of the bundles", required = false, multiValued = false)
    Integer level;
    
    @Option(name = "--force", aliases = {"-f"}, description = "Forces the command to execute", required = false, multiValued = false)
    boolean force;

    @Option(name = "--r3-bundles", description = "Allow OSGi R3 bundles")
    boolean allowR3;
    
    @Reference
    Session session;

    @Reference
    BundleService bundleService;

    @Reference
    BundleContext bundleContext;

    @Override
    public Object execute() throws Exception {
        if (level != null) {
            int sbsl = bundleService.getSystemBundleThreshold();
            if (level < sbsl) {
                if (!JaasHelper.currentUserHasRole(BundleService.SYSTEM_BUNDLES_ROLE)) {
                    throw new IllegalArgumentException("Insufficient privileges");
                }
            }
        }
        // install the bundles
        boolean r3warned = false;
        List<Exception> exceptions = new ArrayList<>();
        List<Bundle> bundles = new ArrayList<>();
        for (URI url : urls) {
            try {
                Bundle bundle = bundleContext.installBundle(url.toString(), null);
                if (!"2".equals(bundle.getHeaders().get(Constants.BUNDLE_MANIFESTVERSION))) {
                    if (allowR3) {
                        if (!r3warned) {
                            System.err.println("WARNING: use of OSGi r3 bundles is discouraged");
                            r3warned = true;
                        }
                    } else {
                        bundle.uninstall();
                        throw new BundleException("OSGi R3 bundle not supported");
                    }
                }
                bundles.add(bundle);
            } catch (Exception e) {
                exceptions.add(new Exception("Unable to install bundle " + url + ": " + e.toString(), e));
            }
        }
        // optionally set start level
        if (level != null) {
            for (Bundle bundle : bundles) {
                try {
                    bundle.adapt(BundleStartLevel.class).setStartLevel(level);
                } catch (Exception e) {
                    exceptions.add(new Exception("Unable to set bundle start level " + bundle.getLocation() + ": " + e.toString(), e));
                }
            }
        }
        // optionally start the bundles
        if (start) {
            for (Bundle bundle : bundles) {
                try {
                    bundle.start();
                } catch (Exception e) {
                    exceptions.add(new Exception("Unable to start bundle " + bundle.getLocation() + ": " + e.toString(), e));
                }
            }
        }
        
        // print the installed bundles
        if (bundles.size() == 1) {
            System.out.println("Bundle ID: " + bundles.get(0).getBundleId());
        } else {
            String msg = bundles.stream()
                    .map(b -> Long.toString(b.getBundleId()))
                    .collect(Collectors.joining(", ", "Bundle IDs: ", ""));
            System.out.println(msg);
        }
        MultiException.throwIf("Error installing bundles", exceptions);
        return null;
    }

}
