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

import org.apache.felix.fileinstall.ArtifactListener;
import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.apache.karaf.deployer.features.FeatureDeploymentListener;
import org.apache.karaf.deployer.features.FeatureURLHandler;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.service.url.URLStreamHandlerService;

@Services(requires = @RequireService(FeaturesService.class))
public class Activator extends BaseActivator {

    private FeatureDeploymentListener listener;

    @Override
    protected void doStart() throws Exception {
        FeaturesService service = getTrackedService(FeaturesService.class);
        if (service == null) {
            return;
        }

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("url.handler.protocol", "feature");
        FeatureURLHandler handler = new FeatureURLHandler();
        register(URLStreamHandlerService.class, handler, props);

        listener = new FeatureDeploymentListener();
        listener.setFeaturesService(service);
        listener.setBundleContext(bundleContext);
        listener.init();
        register(new Class[] { ArtifactUrlTransformer.class, ArtifactListener.class },
                 listener);
    }

    protected void doStop() {
        super.doStop();
        if (listener != null) {
            listener.destroy();
            listener = null;
        }
    }

}
