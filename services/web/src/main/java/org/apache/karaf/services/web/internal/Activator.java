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
package org.apache.karaf.services.web.internal;

import java.util.Dictionary;

import org.apache.karaf.services.web.WebService;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.Managed;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;

@Services(
        requires = @RequireService(ConfigurationAdmin.class),
        provides = @ProvideService(WebService.class)
)
@Managed(WebConfig.PID)
public class Activator extends BaseActivator implements ManagedService {

    private volatile WebContainer container;

    @Override
    protected void doStart() throws Exception {
        // read the configuration right away, rather than starting with the defaults until it is notified
        ensureStartupConfiguration(WebConfig.PID);
        WebContainer webContainer = new WebContainer(bundleContext, WebConfig.from(getConfiguration()));
        container = webContainer;
        webContainer.start();
        register(WebService.class, webContainer);
    }

    @Override
    public void updated(Dictionary<String, ?> properties) {
        WebContainer webContainer = container;
        boolean unchanged;
        try {
            unchanged = webContainer != null && webContainer.getConfig().equals(WebConfig.from(properties));
        } catch (IllegalArgumentException e) {
            // reported when starting with this configuration
            unchanged = false;
        }
        if (!unchanged) {
            // otherwise the configuration read at startup is notified afterwards: nothing to restart
            super.updated(properties);
        }
    }

    @Override
    protected void doStop() {
        super.doStop();
        WebContainer webContainer = container;
        container = null;
        if (webContainer != null) {
            webContainer.stop();
        }
    }

}
