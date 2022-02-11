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
package org.apache.karaf.http.core.internal.osgi;

import org.apache.karaf.http.core.BalancingPolicy;
import org.apache.karaf.http.core.ProxyService;
import org.apache.karaf.http.core.ServletService;
import org.apache.karaf.http.core.internal.HttpMBeanImpl;
import org.apache.karaf.http.core.internal.ProxyServiceImpl;
import org.apache.karaf.http.core.internal.ServletServiceImpl;
import org.apache.karaf.http.core.internal.proxy.RandomBalancingPolicy;
import org.apache.karaf.http.core.internal.proxy.RoundRobinBalancingPolicy;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.Managed;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;

import java.util.Dictionary;
import java.util.Hashtable;

@Services(
        requires = {
                @RequireService(HttpService.class),
                @RequireService(WebContainer.class),
                @RequireService(ConfigurationAdmin.class)
        },
        provides = {
                @ProvideService(ServletService.class),
                @ProvideService(ProxyService.class)
        }
)
@Managed("org.apache.karaf.http")
public class Activator extends BaseActivator implements ManagedService {

    private ProxyService proxyService;

    @Override
    protected void doStart() throws Exception {
        HttpService httpService = getTrackedService(HttpService.class);
        if (httpService == null) {
            return;
        }

        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin == null) {
            return;
        }

        WebContainer webContainer = getTrackedService(WebContainer.class);
        ServletServiceImpl servletService = null;
        if (webContainer != null) {
            servletService = new ServletServiceImpl(webContainer);
            register(ServletService.class, servletService);
        }

        RandomBalancingPolicy randomBalancingPolicy = new RandomBalancingPolicy();
        Hashtable<String, String> randomBalancingPolicyProperties = new Hashtable<>();
        randomBalancingPolicyProperties.put("type", "random");
        register(BalancingPolicy.class, randomBalancingPolicy, randomBalancingPolicyProperties);

        RoundRobinBalancingPolicy roundRobinBalancingPolicy = new RoundRobinBalancingPolicy();
        Hashtable<String, String> roundRobinBalancingPolicyProperties = new Hashtable<>();
        roundRobinBalancingPolicyProperties.put("type", "round-robin");
        register(BalancingPolicy.class, roundRobinBalancingPolicy, roundRobinBalancingPolicyProperties);

        proxyService = new ProxyServiceImpl(httpService, configurationAdmin, bundleContext);
        register(ProxyService.class, proxyService);

        if (servletService != null) {
            HttpMBeanImpl httpMBean = new HttpMBeanImpl(servletService, proxyService);
            registerMBean(httpMBean, "type=http");
        }
    }

    @Override
    public void updated(Dictionary<String, ?> properties) {
        if (proxyService != null) {
            proxyService.update(properties);
        }
    }
}
