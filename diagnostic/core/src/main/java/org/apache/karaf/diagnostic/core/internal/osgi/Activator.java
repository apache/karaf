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
package org.apache.karaf.diagnostic.core.internal.osgi;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.karaf.diagnostic.core.DumpProvider;
import org.apache.karaf.diagnostic.core.internal.BundleDumpProvider;
import org.apache.karaf.diagnostic.core.internal.DiagnosticDumpMBeanImpl;
import org.apache.karaf.diagnostic.core.internal.EnvironmentDumpProvider;
import org.apache.karaf.diagnostic.core.internal.FeaturesDumpProvider;
import org.apache.karaf.diagnostic.core.internal.HeapDumpProvider;
import org.apache.karaf.diagnostic.core.internal.LogDumpProvider;
import org.apache.karaf.diagnostic.core.internal.MemoryDumpProvider;
import org.apache.karaf.diagnostic.core.internal.ThreadDumpProvider;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.util.tracker.SingleServiceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

    private List<ServiceRegistration<DumpProvider>> registrations;
    private ServiceRegistration<DumpProvider> featuresProviderRegistration;
    private ServiceRegistration mbeanRegistration;
    private SingleServiceTracker<FeaturesService> featuresServiceTracker;
    private ServiceTracker<DumpProvider, DumpProvider> providersTracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        registrations = new ArrayList<ServiceRegistration<DumpProvider>>();
        registrations.add(context.registerService(DumpProvider.class, new BundleDumpProvider(context), null));
        registrations.add(context.registerService(DumpProvider.class, new EnvironmentDumpProvider(context), null));
        registrations.add(context.registerService(DumpProvider.class, new HeapDumpProvider(), null));
        registrations.add(context.registerService(DumpProvider.class, new LogDumpProvider(context), null));
        registrations.add(context.registerService(DumpProvider.class, new MemoryDumpProvider(), null));
        registrations.add(context.registerService(DumpProvider.class, new ThreadDumpProvider(), null));

        featuresServiceTracker = new SingleServiceTracker<FeaturesService>(context, FeaturesService.class, new SingleServiceTracker.SingleServiceListener() {
            @Override
            public void serviceFound() {
                featuresProviderRegistration =
                        context.registerService(
                                DumpProvider.class,
                                new FeaturesDumpProvider(featuresServiceTracker.getService()),
                                null);
            }
            @Override
            public void serviceLost() {
            }
            @Override
            public void serviceReplaced() {
                featuresProviderRegistration.unregister();
            }
        });

        final DiagnosticDumpMBeanImpl diagnostic = new DiagnosticDumpMBeanImpl();
        providersTracker = new ServiceTracker<DumpProvider, DumpProvider>(
                context, DumpProvider.class, new ServiceTrackerCustomizer<DumpProvider, DumpProvider>() {
            @Override
            public DumpProvider addingService(ServiceReference<DumpProvider> reference) {
                DumpProvider service = context.getService(reference);
                diagnostic.registerProvider(service);
                return service;
            }
            @Override
            public void modifiedService(ServiceReference<DumpProvider> reference, DumpProvider service) {
            }
            @Override
            public void removedService(ServiceReference<DumpProvider> reference, DumpProvider service) {
                diagnostic.unregisterProvider(service);
                context.ungetService(reference);
            }
        });
        providersTracker.open();

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("jmx.objectname", "org.apache.karaf:type=diagnostic,name=" + System.getProperty("karaf.name"));
        mbeanRegistration = context.registerService(
                getInterfaceNames(diagnostic),
                diagnostic,
                props
        );
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        mbeanRegistration.unregister();
        featuresServiceTracker.close();
        providersTracker.close();
        for (ServiceRegistration<DumpProvider> reg : registrations) {
            reg.unregister();
        }
    }

    private String[] getInterfaceNames(Object object) {
        List<String> names = new ArrayList<String>();
        for (Class cl = object.getClass(); cl != Object.class; cl = cl.getSuperclass()) {
            addSuperInterfaces(names, cl);
        }
        return names.toArray(new String[names.size()]);
    }

    private void addSuperInterfaces(List<String> names, Class clazz) {
        for (Class cl : clazz.getInterfaces()) {
            names.add(cl.getName());
            addSuperInterfaces(names, cl);
        }
    }

}
