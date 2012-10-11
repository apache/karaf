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
                    System.out.println(ListBundleServices.getValueString(objectClass));
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
            System.out.println(key + " = " + ListBundleServices.getValueString(serviceRef.getProperty(key)));
        }
    }

    private static String getBundleName(Bundle bundle) {
        if (bundle != null) {
            String name = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
            return (name == null)
                    ? "Bundle " + Long.toString(bundle.getBundleId())
                    : name + " (" + Long.toString(bundle.getBundleId()) + ")";
        }
        return "[STALE BUNDLE]";
    }

    private static String getUnderlineString(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            sb.append('-');
        }
        return sb.toString();
    }


    private static String getValueString(Object obj) {
        if (obj == null) {
            return "null";
        } else if (obj.getClass().isArray()) {
            Object[] array = (Object[]) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < array.length; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(getValueString(array[i]));
            }
            sb.append("]");
            return sb.toString();
        } else if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof Boolean) {
            return ((Boolean) obj).toString();
        } else if (obj instanceof Long) {
            return ((Long) obj).toString();
        } else if (obj instanceof Integer) {
            return ((Integer) obj).toString();
        } else if (obj instanceof Short) {
            return ((Short) obj).toString();
        } else if (obj instanceof Double) {
            return ((Double) obj).toString();
        } else if (obj instanceof Float) {
            return ((Float) obj).toString();
        } else if (obj instanceof URL) {
            return ((URL) obj).toExternalForm();
        } else if (obj instanceof URI) {
            try {
                return ((URI) obj).toURL().toExternalForm();
            } catch (MalformedURLException e) {
                return obj.toString();
            }
        } else {
            return obj.toString();
        }
    }


}
