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
package org.apache.karaf.jpa.hibernate.impl;

import javax.management.MBeanServer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Track MBeanServer service and manage one StatisticsPublisher for each MBeanServer
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer<MBeanServer, MBeanServer> {

    private ServiceTracker<MBeanServer, MBeanServer> mbeanServerTracker;
    private StatisticsPublisher publisher;
    private BundleContext context;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        mbeanServerTracker = new ServiceTracker<>(context, MBeanServer.class, this);
        mbeanServerTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        mbeanServerTracker.close();
    }

    @Override
    public MBeanServer addingService(ServiceReference<MBeanServer> reference) {
        MBeanServer mbeanServer = context.getService(reference);
        publisher = new StatisticsPublisher(context, mbeanServer);
        publisher.start();
        return mbeanServer;
    }

    @Override
    public void modifiedService(ServiceReference<MBeanServer> reference, MBeanServer service) {
    }

    @Override
    public void removedService(ServiceReference<MBeanServer> reference, MBeanServer service) {
        if (publisher != null) {
            publisher.stop();
            publisher = null;
        }
    }

}
