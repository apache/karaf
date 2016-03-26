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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.List;

@Command(scope = "service", name = "get", description = "Get an OSGi service.")
@Service
public class GetService implements Action {

    @Argument(index = 0, name = "serviceName", description = "Name of the service", required = true,
            multiValued = false)
    @Completion(ObjectClassCompleter.class)
    String objectClass;

    @Reference
    BundleContext bundleContext;

    @Override
    public Object execute() throws Exception {

        List<ServiceReference<?>> serviceRefs = new ArrayList<ServiceReference<?>>();
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            ServiceReference<?>[] services = bundle.getRegisteredServices();
            if (services != null) {
                for (ServiceReference<?> ref : services) {
                    String[] objectClasses = (String[])ref.getProperty(Constants.OBJECTCLASS);
                    if (ObjectClassMatcher.matchesAtLeastOneName(objectClasses, objectClass)) {
                        serviceRefs.add(ref);
                    }
                }
            }
        }

        for (ServiceReference<?> serviceRef : serviceRefs) {
            ListServices.printServiceRef(serviceRef);
        }
        return null;
    }

    public void setObjectClass(String objectClass){
        this.objectClass = objectClass;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
