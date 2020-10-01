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
package org.apache.karaf.example.config.listener;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator {

    private ServiceRegistration<ConfigurationListener> registration;

    @Override
    public void start(BundleContext bundleContext) {
        ConfigurationListener listener = event ->  {
            if (event.getType() == ConfigurationEvent.CM_DELETED) {
                System.out.println("Configuration " + event.getPid() + " has been deleted");
            }
            if (event.getType() == ConfigurationEvent.CM_UPDATED) {
                System.out.println("Configuration " + event.getPid() + " has been updated");
            }
            if (event.getType() == ConfigurationEvent.CM_LOCATION_CHANGED) {
                System.out.println("Configuration " + event.getPid() + " location has been changed");
            }
        };
        registration = bundleContext.registerService(ConfigurationListener.class, listener, null);
    }

    @Override
    public void stop(BundleContext bundleContext) {
        if (registration != null) {
            registration.unregister();
        }
    }

}
