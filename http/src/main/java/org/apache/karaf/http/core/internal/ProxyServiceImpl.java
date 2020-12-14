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
package org.apache.karaf.http.core.internal;

import org.apache.karaf.http.core.BalancingPolicy;
import org.apache.karaf.http.core.Proxy;
import org.apache.karaf.http.core.ProxyService;
import org.apache.karaf.http.core.internal.proxy.ProxyServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ProxyServiceImpl implements ProxyService {

    private static Logger LOG = LoggerFactory.getLogger(ProxyServiceImpl.class.getName());

    protected static final String CONFIGURATION_PID = "org.apache.karaf.http";
    protected static final String CONFIGURATION_KEY = "proxies";

    private ConfigurationAdmin configurationAdmin;
    private HttpService httpService;
    private BundleContext bundleContext;
    private Map<String, Proxy> proxies;

    public ProxyServiceImpl(HttpService httpService, ConfigurationAdmin configurationAdmin, BundleContext bundleContext) {
        this.httpService = httpService;
        this.configurationAdmin = configurationAdmin;
        this.bundleContext = bundleContext;
        this.proxies = new HashMap<>();
        try {
            Configuration configuration = configurationAdmin.getConfiguration(CONFIGURATION_PID, null);
            if (configuration != null) {
                update(configuration.getProcessedProperties(null));
            }
        } catch (Exception e) {
            LOG.error("Can't load proxies", e);
        }
    }

    @Override
    public Map<String, Proxy> getProxies() {
        return proxies;
    }

    @Override
    public Collection<String> getBalancingPolicies() throws Exception {
        ArrayList<String> balancingPolicies = new ArrayList<>();
        Collection<ServiceReference<BalancingPolicy>> serviceReferences = bundleContext.getServiceReferences(BalancingPolicy.class, null);
        for (ServiceReference serviceReference : serviceReferences) {
            if (serviceReference.getProperty("type") != null) {
                balancingPolicies.add(serviceReference.getProperty("type").toString());
            }
        }
        return balancingPolicies;
    }

    @Override
    public void addProxy(String url, String proxyTo, String balancingProxy) throws Exception {
        Proxy proxy = new Proxy(url, proxyTo, balancingProxy);
        addProxyInternal(proxy);
        updateConfiguration();
    }

    @Override
    public void removeProxy(String url) throws Exception {
        LOG.debug("removing proxy {}", url);
        httpService.unregister(url);
        proxies.remove(url);
        updateConfiguration();
    }

    @Override
    public void update(Dictionary<String, ?> properties) {
        LOG.debug("update proxies");
        if (properties == null) {
            return;
        }
        if (properties.get(CONFIGURATION_KEY) != null && (properties.get(CONFIGURATION_KEY) instanceof String[])) {
            String[] proxiesArray = (String[]) properties.get(CONFIGURATION_KEY);
            for (String proxyString : proxiesArray) {
                String[] proxySplit = proxyString.split(" ");
                if (proxySplit.length == 3) {
                    String url = proxySplit[0].trim();
                    String proxyTo = proxySplit[1].trim();
                    String balancingPolicy = proxySplit[2].trim();
                    if (!proxies.containsKey(url)) {
                        if (balancingPolicy.equals("null")) {
                            balancingPolicy = null;
                        }
                        Proxy proxy = new Proxy(url, proxyTo, balancingPolicy);
                        addProxyInternal(proxy);
                    }
                }
            }
        }
    }

    private void addProxyInternal(Proxy proxy) {
        if (proxy.getBalancingPolicy() != null) {
            LOG.debug("Adding {} proxy to {} ({})", proxy.getUrl(), proxy.getProxyTo(), proxy.getBalancingPolicy());
        } else {
            LOG.debug("Adding {} proxy to {}", proxy.getUrl(), proxy.getProxyTo());
        }
        try {
            ProxyServlet proxyServlet = new ProxyServlet();
            proxyServlet.setProxyTo(proxy.getProxyTo());
            if (proxy.getBalancingPolicy() != null) {
                Collection<ServiceReference<BalancingPolicy>> serviceReferences = bundleContext.getServiceReferences(BalancingPolicy.class, "(type=" + proxy.getBalancingPolicy() + ")");
                if (serviceReferences != null && serviceReferences.size() == 1) {
                    BalancingPolicy balancingPolicy = bundleContext.getService(serviceReferences.iterator().next());
                    proxyServlet.setBalancingPolicy(balancingPolicy);
                }
            }
            httpService.registerServlet(proxy.getUrl(), proxyServlet, new Hashtable(), null);
            proxies.put(proxy.getUrl(), proxy);
        } catch (Exception e) {
            LOG.error("Can't add {} proxy to {}", proxy.getUrl(), proxy.getProxyTo(), e);
        }
    }

    private void updateConfiguration() {
        try {
            // get configuration
            Configuration configuration = configurationAdmin.getConfiguration(CONFIGURATION_PID);
            Dictionary<String, Object> configurationProperties = configuration.getProcessedProperties(null);
            if (configurationProperties == null) {
                configurationProperties = new Hashtable<>();
            }
            // convert proxies map to String array
            String[] proxyArray = new String[proxies.size()];
            int i = 0;
            for (Map.Entry<String, Proxy> entry : proxies.entrySet()) {
                proxyArray[i] = entry.getKey() + " " + entry.getValue().getProxyTo() + " " + entry.getValue().getBalancingPolicy();
                i++;
            }
            configurationProperties.put(CONFIGURATION_KEY, proxyArray);
            configuration.update(configurationProperties);
        } catch (Exception e) {
            LOG.error("unable to update http proxy from configuration", e);
        }
    }

}
