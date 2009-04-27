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
package org.apache.servicemix.kernel.gshell.osgi;

import java.util.List;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.apache.geronimo.gshell.command.Command;
import org.apache.servicemix.kernel.gshell.core.OsgiCommandSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class ListServices extends OsgiCommandSupport {

    @Option(name = "-a", description = "Show all")
    boolean showAll;

    @Option(name = "-u", description = "Show in use")
    boolean inUse;

    @Argument(required = false, multiValued = true, description = "Bundles ids")
    List<Long> ids;

    protected Object doExecute() throws Exception {
        if (ids != null && !ids.isEmpty()) {
            for (long id : ids) {
                Bundle bundle = getBundleContext().getBundle(id);
                if (bundle != null) {
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
                    for (int refIdx = 0;
                         (refs != null) && (refIdx < refs.length);
                         refIdx++) {
                        String[] objectClass = (String[])
                                refs[refIdx].getProperty("objectClass");

                        // Determine if we need to print the service, depending
                        // on whether it is a command service or not.
                        boolean print = true;
                        for (int ocIdx = 0;
                             !showAll && (ocIdx < objectClass.length);
                             ocIdx++) {
                            if (objectClass[ocIdx].equals(Command.class.getName())) {
                                print = false;
                            }
                        }

                        // Print header if we have not already done so.
                        if (!headerPrinted) {
                            headerPrinted = true;
                            String title = Util.getBundleName(bundle);
                            title = (inUse)
                                    ? title + " uses:"
                                    : title + " provides:";
                            io.out.println("");
                            io.out.println(title);
                            io.out.println(Util.getUnderlineString(title));
                        }

                        if (showAll || print) {
                            // Print service separator if necessary.
                            if (needSeparator) {
                                io.out.println("----");
                            }

                            // Print service properties.
                            String[] keys = refs[refIdx].getPropertyKeys();
                            for (int keyIdx = 0;
                                 (keys != null) && (keyIdx < keys.length);
                                 keyIdx++) {
                                Object v = refs[refIdx].getProperty(keys[keyIdx]);
                                io.out.println(
                                        keys[keyIdx] + " = " + Util.getValueString(v));
                            }

                            needSeparator = true;
                        }
                    }
                } else {
                    io.err.println("Bundle ID " + id + " is invalid.");
                }
            }
        }
        else
        {
            Bundle[] bundles = getBundleContext().getBundles();
            if (bundles != null)
            {
                // TODO: Sort list.
                for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
                {
                    boolean headerPrinted = false;
                    ServiceReference[] refs = null;

                    // Get registered or in-use services.
                    if (inUse)
                    {
                        refs = bundles[bundleIdx].getServicesInUse();
                    }
                    else
                    {
                        refs = bundles[bundleIdx].getRegisteredServices();
                    }

                    for (int refIdx = 0; (refs != null) && (refIdx < refs.length); refIdx++)
                    {
                        String[] objectClass = (String[])
                            refs[refIdx].getProperty("objectClass");

                        // Determine if we need to print the service, depending
                        // on whether it is a command service or not.
                        boolean print = true;
                        for (int ocIdx = 0;
                            !showAll && (ocIdx < objectClass.length);
                            ocIdx++)
                        {
                            if (objectClass[ocIdx].equals(Command.class.getName()))
                            {
                                print = false;
                            }
                        }

                        // Print the service if necessary.
                        if (showAll || print)
                        {
                            if (!headerPrinted)
                            {
                                headerPrinted = true;
                                String title = Util.getBundleName(bundles[bundleIdx]);
                                title = (inUse)
                                    ? title + " uses:"
                                    : title + " provides:";
                                io.out.println("\n" + title);
                                io.out.println(Util.getUnderlineString(title));
                            }
                            io.out.println(Util.getValueString(objectClass));
                        }
                    }
                }
            }
            else
            {
                io.out.println("There are no registered services.");
            }
        }
        return Result.SUCCESS;
    }

}