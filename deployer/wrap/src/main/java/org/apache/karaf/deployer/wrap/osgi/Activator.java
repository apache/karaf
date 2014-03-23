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
package org.apache.karaf.deployer.wrap.osgi;

import java.util.Hashtable;

import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.apache.karaf.deployer.wrap.WrapDeploymentListener;
import org.apache.karaf.util.tracker.SingleServiceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLStreamHandlerService;

public class Activator implements BundleActivator, SingleServiceTracker.SingleServiceListener {

    private BundleContext bundleContext;
    private ServiceRegistration<ArtifactUrlTransformer> urlTransformerRegistration;
    private SingleServiceTracker<URLStreamHandlerService> urlHandlerTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        urlHandlerTracker = new SingleServiceTracker<URLStreamHandlerService>(
                context, URLStreamHandlerService.class,
                "(url.handler.protocol=wrap)", this);
        urlHandlerTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        urlHandlerTracker.close();
    }

    @Override
    public void serviceFound() {
        if (urlTransformerRegistration == null) {
            Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put("service.ranking", -1);
            urlTransformerRegistration = bundleContext.registerService(
                    ArtifactUrlTransformer.class,
                    new WrapDeploymentListener(),
                    props);
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
    }

}
