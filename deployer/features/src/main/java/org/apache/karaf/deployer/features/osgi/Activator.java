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
package org.apache.karaf.deployer.features.osgi;

import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.fileinstall.ArtifactListener;
import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.apache.karaf.deployer.features.FeatureDeploymentListener;
import org.apache.karaf.deployer.features.FeatureURLHandler;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.util.tracker.SingleServiceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, SingleServiceTracker.SingleServiceListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private AtomicBoolean scheduled = new AtomicBoolean();
    private BundleContext bundleContext;
    private ServiceRegistration urlHandlerRegistration;
    private ServiceRegistration urlTransformerRegistration;
    private SingleServiceTracker<FeaturesService> featuresServiceTracker;
    private FeatureDeploymentListener listener;

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        scheduled.set(true);

        featuresServiceTracker = new SingleServiceTracker<FeaturesService>(
                context, FeaturesService.class, this);
        featuresServiceTracker.open();

        scheduled.set(false);
        reconfigure();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        featuresServiceTracker.close();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }

    @Override
    public void serviceFound() {
        reconfigure();
    }

    @Override
    public void serviceLost() {
        reconfigure();
    }

    @Override
    public void serviceReplaced() {
        reconfigure();
    }

    protected void reconfigure() {
        if (scheduled.compareAndSet(false, true)) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    scheduled.set(false);
                    doStop();
                    try {
                        doStart();
                    } catch (Exception e) {
                        LOGGER.warn("Error starting features deployer", e);
                        doStop();
                    }
                }
            });
        }
    }

    protected void doStart() throws Exception {
        FeaturesService service = featuresServiceTracker.getService();
        if (service == null) {
            return;
        }

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("url.handler.protocol", "feature");
        FeatureURLHandler handler = new FeatureURLHandler();
        urlHandlerRegistration = bundleContext.registerService(
                URLStreamHandlerService.class,
                handler,
                props);

        listener = new FeatureDeploymentListener();
        listener.setFeaturesService(service);
        listener.setBundleContext(bundleContext);
        listener.init();

        urlTransformerRegistration = bundleContext.registerService(
                new String[] {
                        ArtifactUrlTransformer.class.getName(),
                        ArtifactListener.class.getName()
                },
                listener,
                null);
    }

    protected void doStop() {
        if (urlTransformerRegistration != null) {
            urlTransformerRegistration.unregister();
            urlTransformerRegistration = null;
        }
        if (urlHandlerRegistration != null) {
            urlHandlerRegistration.unregister();
            urlHandlerRegistration = null;
        }
        if (listener != null) {
            listener.destroy();
            listener = null;
        }
    }

}
