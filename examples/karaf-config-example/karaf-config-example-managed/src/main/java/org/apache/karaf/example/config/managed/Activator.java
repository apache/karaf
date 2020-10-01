/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.example.config.managed;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

import java.util.Enumeration;
import java.util.Hashtable;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator {

    private ServiceRegistration<ManagedService> registration;

    @Override
    public void start(BundleContext bundleContext) {
        ManagedService managedService = properties -> {
            System.out.println("Configuration changed");
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                System.out.println(key + " = " + properties.get(key));
            }
        };
        Hashtable<String, String> serviceProperties = new Hashtable<>();
        serviceProperties.put(Constants.SERVICE_PID, "org.apache.karaf.example.config");
        registration = bundleContext.registerService(ManagedService.class, managedService, serviceProperties);
    }

    @Override
    public void stop(BundleContext bundleContext) {
        if (registration != null) {
            registration.unregister();
        }
    }

}
