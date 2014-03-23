/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.deployer.kar.osgi;

import java.util.Hashtable;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.fileinstall.ArtifactListener;
import org.apache.karaf.deployer.kar.KarArtifactInstaller;
import org.apache.karaf.kar.KarService;
import org.apache.karaf.util.tracker.SingleServiceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator, SingleServiceTracker.SingleServiceListener {

    private BundleContext bundleContext;
    private ServiceRegistration urlTransformerRegistration;
    private SingleServiceTracker<KarService> karServiceTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        karServiceTracker = new SingleServiceTracker<KarService>(
                context, KarService.class, this);
        karServiceTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        karServiceTracker.close();
    }

    @Override
    public void serviceFound() {
        KarService service = karServiceTracker.getService();
        if (urlTransformerRegistration == null && service != null) {
            KarArtifactInstaller installer = new KarArtifactInstaller();
            installer.setKarService(service);
            Hashtable<String, Object> props = new Hashtable<String, Object>();
            urlTransformerRegistration = bundleContext.registerService(
                    new String[] {
                            ArtifactInstaller.class.getName(),
                            ArtifactListener.class.getName()
                    },
                    installer,
                    null);
        }
    }

    @Override
    public void serviceLost() {
        if (urlTransformerRegistration != null) {
            urlTransformerRegistration.unregister();
            urlTransformerRegistration = null;
        }
    }

    @Override
    public void serviceReplaced() {
        serviceLost();
        serviceFound();
    }

}
