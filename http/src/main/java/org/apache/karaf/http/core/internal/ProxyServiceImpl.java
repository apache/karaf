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

import org.apache.karaf.http.core.ProxyService;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class ProxyServiceImpl implements ProxyService {

    private static Logger LOG = LoggerFactory.getLogger(ProxyServiceImpl.class.getName());

    protected static final String CONFIGURATION_PID = "org.apache.karaf.http";
    protected static final String CONFIGURATION_KEY = "proxies";

    private ConfigurationAdmin configurationAdmin;
    private HttpService httpService;
    private Map<String, String> proxies;

    public ProxyServiceImpl(HttpService httpService, ConfigurationAdmin configurationAdmin) {
        this.httpService = httpService;
        this.configurationAdmin = configurationAdmin;
        this.proxies = new HashMap<>();
        initProxies();
    }

    @Override
    public Map<String, String> getProxies() {
        return proxies;
    }

    @Override
    public void addProxy(String url, String proxyTo) throws Exception {
        addProxyInternal(url, proxyTo);
        updateConfiguration();
    }

    @Override
    public void removeProxy(String url) throws Exception {
        LOG.debug("removing proxy alias: " + url);
        httpService.unregister(url);
        proxies.remove(url);
        updateConfiguration();
    }

    @Override
    public void initProxies() {
        LOG.debug("unregistering and registering all configured proxies");
        unregisterAllProxies();
        initProxiesInternal();
    }

    private void initProxiesInternal() {
        try {
            Configuration configuration = getConfiguration();
            Dictionary<String, Object> configurationProperties = configuration.getProperties();
            String[] proxiesArray = getConfiguredProxyArray(configurationProperties);
            if (proxiesArray != null) {
                for (String proxyPair : proxiesArray) {
                    String[] split = proxyPair.split(" ", 2);
                    if (split.length == 2) {
                        String from = split[0].trim();
                        String to = split[1].trim();
                        if (from.length() > 0 && to.length() > 0) {
                            addProxyInternal(from, to);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("unable to initialize proxies: " + e.getMessage());
        }
    }

    private void addProxyInternal(String url, String proxyTo) {
        LOG.debug("adding proxy alias: " + url + ", proxied to: " + proxyTo);
        try {
            ProxyServlet proxyServlet = new ProxyServlet();
            proxyServlet.setProxyTo(proxyTo);
            httpService.registerServlet(url, proxyServlet, new Hashtable(), null);
            proxies.put(url, proxyTo);
        } catch (Exception e) {
            LOG.error("could not add proxy alias: " + url + ", proxied to: " + proxyTo + ", reason: " + e.getMessage());
        }
    }


    private void updateConfiguration() {

        try {
            Configuration configuration = getConfiguration();
            Dictionary<String, Object> configurationProperties = configuration.getProperties();
            if (configurationProperties == null) {
                configurationProperties = new Hashtable<String, Object>();
            }
            configurationProperties.put(CONFIGURATION_KEY, mapToProxyArray(this.proxies));
            configuration.update(configurationProperties);
        } catch (Exception e) {
            LOG.error("unable to update http proxy from configuration: " + e.getMessage());
        }

    }

    private Configuration getConfiguration() {

        try {
            return configurationAdmin.getConfiguration(CONFIGURATION_PID, null);
        } catch (IOException e) {
            throw new RuntimeException("Error retrieving http proxy information from config admin", e);
        }
    }

    private String[] mapToProxyArray(Map<String, String> proxies) {
        List<String> proxyList = new ArrayList<String>();
        Iterator<Map.Entry<String, String>> entries = proxies.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, String> entry = entries.next();
            proxyList.add(entry.getKey() + " " + entry.getValue());
        }
        return proxyList.stream().toArray(String[]::new);
    }

    private String[] getConfiguredProxyArray(Dictionary<String, Object> configurationProperties) {
        Object val = null;
        if (configurationProperties != null) {
            val = configurationProperties.get(CONFIGURATION_KEY);
        }

        if (val instanceof String[]) {
            return (String[]) val;
        } else {
            return null;
        }
    }

    private void unregisterAllProxies() {
        for (String url : proxies.keySet()) {
            LOG.debug("removing proxy alias: " + url);
            httpService.unregister(url);
        }
        proxies.clear();
    }


}
