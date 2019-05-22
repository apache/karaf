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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.ShellUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

@Command(scope = "bundle", name = "services", description = "Lists OSGi services per Bundle")
@Service
public class Services extends BundlesCommand {

    @Option(name = "-a", aliases = {}, description = "Shows all services. (Karaf commands and completers are hidden by default)", required = false, multiValued = false)
    boolean showAll;

    @Option(name = "-u", aliases = {}, description = "Shows the services each bundle uses. (By default the provided services are shown)", required = false, multiValued = false)
    boolean inUse;
    
    @Option(name = "-p", aliases = {}, description = "Shows the properties of the services", required = false, multiValued = false)
    boolean showProperties = false;

    Set<String> hidden = new HashSet<>(Arrays.asList(
            "org.apache.felix.service.command.Function",
            "org.apache.karaf.shell.console.Completer"
    ));

    @Override
    protected void executeOnBundle(Bundle bundle) throws Exception {
        ServiceReference<?>[] refs = (inUse) ? bundle.getServicesInUse() : bundle.getRegisteredServices();
        printServices(bundle, refs, showProperties);
    }
    
    private void printServices(Bundle bundle, ServiceReference<?>[] refs, boolean showProperties) {
        boolean headerPrinted = false;
        boolean needSeparator = false;
        
        if (refs == null) {
            return;
        }

        for (ServiceReference<?> serviceRef : refs) {
            String[] objectClass = (String[]) serviceRef.getProperty(Constants.OBJECTCLASS);

            boolean print = showAll || !isCommandOrCompleter(objectClass);

            // Print header if we have not already done so.
            if (!headerPrinted) {
                headerPrinted = true;
                System.out.println();
                String title = ShellUtil.getBundleName(bundle) + ((inUse) ? " uses:" : " provides:");
                System.out.println(title);
                System.out.println(ShellUtil.getUnderlineString(title));
            }

            if (print) {
                // Print service separator if necessary.
                if (needSeparator && showProperties) {
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

    private boolean isCommandOrCompleter(String[] objectClasses) {
        for (String objectClass : objectClasses) {
            if (hidden.contains(objectClass)) {
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
