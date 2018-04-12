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
package org.apache.karaf.jaas.config.impl;

import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.config.KeystoreInstance;
import org.apache.karaf.jaas.config.KeystoreManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

    private OsgiConfiguration osgiConfiguration;
    private ServiceRegistration<KeystoreManager> registration;
    private ServiceTracker<KeystoreInstance, KeystoreInstance> keystoreInstanceServiceTracker;
    private ServiceTracker<JaasRealm, JaasRealm> jaasRealmServiceTracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        ProxyLoginModule.init(context.getBundle(0).getBundleContext());

        final OsgiKeystoreManager keystoreManager = new OsgiKeystoreManager();

        keystoreInstanceServiceTracker = new ServiceTracker<>(
                context, KeystoreInstance.class, new ServiceTrackerCustomizer<KeystoreInstance, KeystoreInstance>() {
            @Override
            public KeystoreInstance addingService(ServiceReference<KeystoreInstance> reference) {
                KeystoreInstance service = context.getService(reference);
                keystoreManager.register(service, null);
                return service;
            }

            @Override
            public void modifiedService(ServiceReference<KeystoreInstance> reference, KeystoreInstance service) {
            }

            @Override
            public void removedService(ServiceReference<KeystoreInstance> reference, KeystoreInstance service) {
                keystoreManager.unregister(service, null);
                context.ungetService(reference);
            }
        });
        keystoreInstanceServiceTracker.open();

        osgiConfiguration = new OsgiConfiguration();
        osgiConfiguration.init();

        jaasRealmServiceTracker = new ServiceTracker<>(
                context, JaasRealm.class, new ServiceTrackerCustomizer<JaasRealm, JaasRealm>() {
            @Override
            public JaasRealm addingService(ServiceReference<JaasRealm> reference) {
                JaasRealm service = context.getService(reference);
                osgiConfiguration.register(service, null);
                return service;
            }

            @Override
            public void modifiedService(ServiceReference<JaasRealm> reference, JaasRealm service) {
            }

            @Override
            public void removedService(ServiceReference<JaasRealm> reference, JaasRealm service) {
                osgiConfiguration.unregister(service, null);
            }
        });
        jaasRealmServiceTracker.open();

        registration = context.registerService(KeystoreManager.class, keystoreManager, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        registration.unregister();
        keystoreInstanceServiceTracker.close();
        jaasRealmServiceTracker.close();
        osgiConfiguration.close();
    }
}
