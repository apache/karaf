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
package org.apache.karaf.service.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.ShellUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

@Command(scope = "service", name = "list", description = "Lists OSGi services.")
@Service
public class ListServices implements Action {

    @Argument(index = 0, name = "objectClass", description = "Name of service objectClass to filter for", required = false,
        multiValued = false)
    @Completion(ObjectClassCompleter.class)
    String objectClass;
    
    @Option(name = "-a", aliases = {}, description = "Shows all services. (By default Karaf commands are hidden)", required = false, multiValued = false)
    boolean showAll;
    
    @Option(name = "-n", aliases = {}, description = "Shows only service class names", required = false, multiValued = false)
    boolean onlyNames;

    @Reference
    BundleContext bundleContext;

    @Override
    public Object execute() throws Exception {
        if (onlyNames) {
            listNames();
            return null;
        }
        List<ServiceReference<?>> serviceRefs = new ArrayList<ServiceReference<?>>();
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            ServiceReference<?>[] services = bundle.getRegisteredServices();
            if (services != null) {
                for (ServiceReference<?> ref : services) {
                    String[] objectClasses = (String[])ref.getProperty(Constants.OBJECTCLASS);
                    if (objectClass == null || ObjectClassMatcher.matchesAtLeastOneName(objectClasses, objectClass)) {
                        serviceRefs.add(ref);
                    }
                } 
            }
        }
        
        Collections.sort(serviceRefs, new ServiceClassComparator());
        
        for (ServiceReference<?> serviceRef : serviceRefs) {
            if (showAll || !isCommand((String[])serviceRef.getProperty(Constants.OBJECTCLASS))) {
                printServiceRef(serviceRef);
            }
        }
        return null;
    }
    
    private void listNames() {
        Map<String, Integer> serviceNames = getServiceNamesMap(bundleContext);
        ArrayList<String> serviceNamesList = new ArrayList<String>(serviceNames.keySet());
        Collections.sort(serviceNamesList);
        for (String name : serviceNamesList) {
            System.out.println(name + " (" + serviceNames.get(name) + ")");
        }
    }
    
    public static Map<String, Integer> getServiceNamesMap(BundleContext bundleContext) {
        Map<String, Integer> serviceNames = new HashMap<String, Integer>();
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            ServiceReference<?>[] services = bundle.getRegisteredServices();
            if (services != null) {
                for (ServiceReference<?> serviceReference : services) {
                    String[] names = (String[])serviceReference.getProperty(Constants.OBJECTCLASS);
                    for (String name : names) {
                        int curCount = (serviceNames.containsKey(name)) ? serviceNames.get(name) : 0;
                        serviceNames.put(name, curCount + 1);
                    }
                }
            }
        }
        return serviceNames;
    }

    private void printServiceRef(ServiceReference<?> serviceRef) {
        String[] objectClass = (String[]) serviceRef.getProperty(Constants.OBJECTCLASS);
        String serviceClasses = ShellUtil.getValueString(objectClass);
        System.out.println(serviceClasses);
        System.out.println(ShellUtil.getUnderlineString(serviceClasses));
        
        printProperties(serviceRef);
        
        String bundleName = ShellUtil.getBundleName(serviceRef.getBundle());
        System.out.println("Provided by : ");
        System.out.println(" " + bundleName);
        Bundle[] usingBundles = serviceRef.getUsingBundles();
        if (usingBundles != null) {
            System.out.println("Used by: ");
            for (Bundle bundle : usingBundles) {
                System.out.println(" " + ShellUtil.getBundleName(bundle));
            }
        }
        System.out.println();
    }

    private boolean isCommand(String[] objectClasses) {
        for (String objectClass : objectClasses) {
            if (objectClass.equals("org.apache.felix.service.command.Function")) {
                return true;
            }
        }
        return false;
    }

    private void printProperties(ServiceReference<?> serviceRef) {
        for (String key : serviceRef.getPropertyKeys()) {
            if (!Constants.OBJECTCLASS.equals(key)) {
                System.out.println(" " + key + " = " + ShellUtil.getValueString(serviceRef.getProperty(key)));
            }
        }
    }

    public final class ServiceClassComparator implements Comparator<ServiceReference<?>> {
        @Override
        public int compare(ServiceReference<?> o1, ServiceReference<?> o2) {
            String[] classes1 = (String[])o1.getProperty(Constants.OBJECTCLASS);
            String[] classes2 = (String[])o2.getProperty(Constants.OBJECTCLASS);
            return classes1[0].compareTo(classes2[0]);
        }
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
