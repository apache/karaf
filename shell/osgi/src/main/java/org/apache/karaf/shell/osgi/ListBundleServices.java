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
package org.apache.karaf.shell.osgi;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.service.command.Function;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Command(scope = "osgi", name = "bundle-services", description = "List OSGi services per bundle")
public class ListBundleServices extends BundlesCommand {

    @Option(name = "-a", aliases = {}, description = "Shows all services. (By default Karaf commands are hidden)", required = false, multiValued = false)
    boolean showAll;

    @Option(name = "-u", aliases = {}, description = "Shows the services each bundle uses. (By default the provided services are shown)", required = false, multiValued = false)
    boolean inUse;

    @Option(name = "-p", aliases = {}, description = "Shows the properties of the services", required = false, multiValued = false)
    boolean showProperties = false;

    @Override
    protected void doExecute(List<Bundle> bundles) throws Exception {
        for (Bundle bundle : bundles) {
            ServiceReference[] refs = (inUse) ? bundle.getServicesInUse() : bundle.getRegisteredServices();
            printServices(bundle, refs, showProperties);
        }
    }

    private void printServices(Bundle bundle, ServiceReference[] refs, boolean showProperties) {
        boolean headerPrinted = false;
        boolean needSeparator = false;

        if (refs == null) {
            return;
        }

        for (ServiceReference serviceRef : refs) {
            String[] objectClass = (String[]) serviceRef.getProperty(Constants.OBJECTCLASS);

            boolean print = showAll || !isCommand(objectClass);

            // Print header if we have not already done so.
            if (!headerPrinted) {
                headerPrinted = true;
                System.out.println("");
                String title = bundle.getSymbolicName() + ((inUse) ? " uses:" : " provides:");
                System.out.println(title);
                System.out.println("----------------------------------");
            }

            if (print) {
                // Print service separator if necessary.
                if (needSeparator && showProperties) {
                    System.out.println("----");
                }

                if (showProperties) {
                    printProperties(serviceRef);
                } else {
                    System.out.println(Util.getValueString(objectClass));
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

    private void printProperties(ServiceReference serviceRef) {
        for (String key : serviceRef.getPropertyKeys()) {
            System.out.println(key + " = " + Util.getValueString(serviceRef.getProperty(key)));
        }
    }

}
