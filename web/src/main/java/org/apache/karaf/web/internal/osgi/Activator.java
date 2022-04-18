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
package org.apache.karaf.web.internal.osgi;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.apache.karaf.web.WebContainerService;
import org.apache.karaf.web.internal.WebContainerServiceImpl;
import org.apache.karaf.web.management.internal.WebMBeanImpl;
import org.ops4j.pax.web.service.WebContainer;

@Services(
        requires = @RequireService(WebContainer.class),
        provides = @ProvideService(WebContainerService.class)
)
public class Activator extends BaseActivator {

    private WebContainerServiceImpl webContainerService;

    @Override
    protected void doStart() throws Exception {
        WebContainer webContainer = getTrackedService(WebContainer.class);
        if (webContainer == null) {
            return;
        }

        webContainerService = new WebContainerServiceImpl();
        webContainerService.setBundleContext(bundleContext);
        webContainerService.setWebContainer(webContainer);
        register(WebContainerService.class, webContainerService);

        WebMBeanImpl webMBean = new WebMBeanImpl();
        webMBean.setWebContainerService(webContainerService);
        registerMBean(webMBean, "type=web");
    }

    @Override
    protected void doStop() {
        if (webContainerService != null) {
            webContainerService = null;
        }
        super.doStop();
    }

}
