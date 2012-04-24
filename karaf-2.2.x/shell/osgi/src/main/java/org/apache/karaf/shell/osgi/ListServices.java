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

import java.util.List;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.service.command.Function;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

@Command(scope = "osgi", name = "ls", description = "Lists OSGi services.")
public class ListServices extends BundlesCommandOptional {

    @Option(name = "-a", aliases = {}, description = "Shows all services", required = false, multiValued = false)
    boolean showAll;

    @Option(name = "-u", aliases = {}, description = "Shows services which are in use", required = false, multiValued = false)
    boolean inUse;

    protected void doExecute(List<Bundle> bundles) throws Exception {
        if (bundles == null) {
            Bundle[] allBundles = getBundleContext().getBundles();
            for (int i = 0; i < allBundles.length; i++) {
                printServicesShort(allBundles[i]);
            }
        } else {
            for (Bundle bundle : bundles) {
                printServices(bundle);
            }
        }
    }

    private void printServices(Bundle bundle) {
        boolean headerPrinted = false;
        boolean needSeparator = false;
        ServiceReference[] refs = null;

        // Get registered or in-use services.
        if (inUse) {
            refs = bundle.getServicesInUse();
        } else {
            refs = bundle.getRegisteredServices();
        }

        // Print properties for each service.
        for (int refIdx = 0; (refs != null) && (refIdx < refs.length); refIdx++) {
            String[] objectClass = (String[]) refs[refIdx].getProperty("objectClass");

            // Determine if we need to print the service, depending
            // on whether it is a command service or not.
            boolean print = true;
            for (int ocIdx = 0; !showAll && (ocIdx < objectClass.length); ocIdx++) {
                if (objectClass[ocIdx].equals(Function.class.getName())) {
                    print = false;
                }
            }

            // Print header if we have not already done so.
            if (!headerPrinted) {
                headerPrinted = true;
                String title = Util.getBundleName(bundle);
                title = (inUse) ? title + " uses:" : title + " provides:";
                System.out.println("");
                System.out.println(title);
                System.out.println(Util.getUnderlineString(title));
            }

            if (showAll || print) {
                // Print service separator if necessary.
                if (needSeparator) {
                    System.out.println("----");
                }

                // Print service properties.
                String[] keys = refs[refIdx].getPropertyKeys();
                for (int keyIdx = 0; (keys != null) && (keyIdx < keys.length); keyIdx++) {
                    Object v = refs[refIdx].getProperty(keys[keyIdx]);
                    System.out.println(keys[keyIdx] + " = " + Util.getValueString(v));
                }

                needSeparator = true;
            }
        }
    }

    private void printServicesShort(Bundle bundle) {
        boolean headerPrinted = false;
        ServiceReference[] refs = null;

        // Get registered or in-use services.
        if (inUse) {
            refs = bundle.getServicesInUse();
        } else {
            refs = bundle.getRegisteredServices();
        }

        for (int refIdx = 0; (refs != null) && (refIdx < refs.length); refIdx++) {
            String[] objectClass = (String[]) refs[refIdx].getProperty("objectClass");

            // Determine if we need to print the service, depending
            // on whether it is a command service or not.
            boolean print = true;
            for (int ocIdx = 0; !showAll && (ocIdx < objectClass.length); ocIdx++) {
                if (objectClass[ocIdx].equals(Function.class.getName())) {
                    print = false;
                }
            }

            // Print the service if necessary.
            if (showAll || print) {
                if (!headerPrinted) {
                    headerPrinted = true;
                    String title = Util.getBundleName(bundle);
                    title = (inUse) ? title + " uses:" : title + " provides:";
                    System.out.println("\n" + title);
                    System.out.println(Util.getUnderlineString(title));
                }
                System.out.println(Util.getValueString(objectClass));
            }
        }

    }

}
