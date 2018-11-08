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
package org.apache.karaf.examples.servlet.registration;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private ServiceTracker httpServiceTracker;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        httpServiceTracker =
                new ServiceTracker(bundleContext, HttpService.class.getName(), null) {
                    @Override
                    public Object addingService(ServiceReference ref) {
                        HttpService httpService = (HttpService) bundleContext.getService(ref);
                        try {
                            httpService.registerServlet(
                                    "/servlet-example", new ExampleServlet(), null, null);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return httpService;
                    }

                    public void removedService(ServiceReference ref, Object service) {
                        try {
                            ((HttpService) service).unregister("/servlet-example");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
        httpServiceTracker.open();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        httpServiceTracker.close();
    }
}
