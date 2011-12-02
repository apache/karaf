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
package org.apache.karaf.shell.services;

import java.util.List;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.bundles.BundleSelector;
import org.apache.karaf.util.ShellUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@Command(scope = "service", name = "list", description = "Lists OSGi services.")
public class ListServices extends OsgiCommandSupport {
    @Argument(index = 0, name = "ids", description = "The list of bundle (identified by IDs or name or name/version) separated by whitespaces", required = false, multiValued = true)
    List<String> ids;

    @Option(name = "-a", aliases = {}, description = "Shows all services. (By default Karaf commands are hidden)", required = false, multiValued = false)
    boolean showAll;

    @Option(name = "-u", aliases = {}, description = "Shows the services each bundle uses. (By default the provided services are shown)", required = false, multiValued = false)
    boolean inUse;

    protected Object doExecute() throws Exception {
        BundleContext bundleContext = getBundleContext();
        BundleSelector selector = new BundleSelector(bundleContext, session);
        List<Bundle> bundles = selector.selectBundles(ids, true);
        if (bundles == null || bundles.isEmpty()) {
            Bundle[] allBundles = bundleContext.getBundles();
            printBundles(allBundles, false);
        } else {
            printBundles(bundles.toArray(new Bundle[]{}), true);
        }
        return null;
    }

    private void printBundles(Bundle[] bundles, boolean showProperties) {
        for (Bundle bundle : bundles) {
            ServiceReference<?>[] refs = (inUse) ? bundle.getServicesInUse() : bundle.getRegisteredServices();
            printServices(bundle, refs, showProperties);
        }
    }
    
    private void printServices(Bundle bundle, ServiceReference<?>[] refs, boolean showProperties) {
        boolean headerPrinted = false;
        boolean needSeparator = false;
        
        if (refs == null) {
            return;
        }

        for (ServiceReference<?> serviceRef : refs) {
            String[] objectClass = (String[]) serviceRef.getProperty("objectClass");

            boolean print = showAll || !isCommand(objectClass);

            // Print header if we have not already done so.
            if (!headerPrinted) {
                headerPrinted = true;
                System.out.println("");
                String title = ShellUtil.getBundleName(bundle) + ((inUse) ? " uses:" : " provides:");
                System.out.println(title);
                System.out.println(ShellUtil.getUnderlineString(title));
            }

            if (print) {
                // Print service separator if necessary.
                if (needSeparator) {
                    System.out.println("----");
                }

                if (showProperties) {
                    printProperties(serviceRef);
                } else {
                    System.out.println(ShellUtil.getValueString(objectClass));
                }

                needSeparator = true;
            }
        }
    }

    private boolean isCommand(String[] objectClasses) {
        for (String objectClass : objectClasses) {
            if (objectClass.equals(Function.class.getName())) {
                return true;
            }
        }
        return false;
    }

    private void printProperties(ServiceReference<?> serviceRef) {
        for (String key : serviceRef.getPropertyKeys()) {
            System.out.println(key + " = " + ShellUtil.getValueString(serviceRef.getProperty(key)));
        }
    }

}
