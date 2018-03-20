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

import org.apache.karaf.http.core.ProxyInfo;
import org.apache.karaf.http.core.ProxyService;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.osgi.service.http.HttpService;

import java.util.*;

public class ProxyServiceImpl implements ProxyService {

    private HttpService httpService;
    private Map<String, ProxyInfo> proxies;

    public ProxyServiceImpl(HttpService httpService) {
        this.httpService = httpService;
        this.proxies = new HashMap<>();
    }

    @Override
    public Collection<ProxyInfo> getProxies() {
        return proxies.values();
    }

    @Override
    public void addProxy(String url, String prefix, String proxyTo) throws Exception {
        Dictionary<String, String> proxyConfig = new Hashtable<>();
        proxyConfig.put("prefix", prefix);
        proxyConfig.put("proxyTo", proxyTo);
        httpService.registerServlet(url, new ProxyServlet.Transparent(), proxyConfig, null);
        ProxyInfo proxyInfo = new ProxyInfo();
        proxyInfo.setPrefix(prefix);
        proxyInfo.setUrl(url);
        proxyInfo.setProxyTo(proxyTo);
        proxies.put(url, proxyInfo);
    }

    @Override
    public void removeProxy(String url) throws Exception {
        httpService.unregister(url);
        proxies.remove(url);
    }
}
