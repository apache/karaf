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
package org.apache.karaf.deployer.spring.osgi;

import java.util.Hashtable;

import org.apache.felix.fileinstall.ArtifactListener;
import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.apache.karaf.deployer.spring.SpringDeploymentListener;
import org.apache.karaf.deployer.spring.SpringURLHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLStreamHandlerService;

public class Activator implements BundleActivator {

    private ServiceRegistration urlHandlerRegistration;
    private ServiceRegistration urlTransformerRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("url.handler.protocol", "spring");
        urlHandlerRegistration = context.registerService(
                URLStreamHandlerService.class,
                new SpringURLHandler(),
                props);

        urlTransformerRegistration = context.registerService(
                new String[] {
                        ArtifactUrlTransformer.class.getName(),
                        ArtifactListener.class.getName()
                },
                new SpringDeploymentListener(),
                null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        urlTransformerRegistration.unregister();
        urlHandlerRegistration.unregister();
    }
}
