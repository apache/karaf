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
package org.apache.karaf.http.core;

import javax.management.MBeanException;
import javax.management.openmbean.TabularData;
import java.util.Collection;
import java.util.Map;

/**
 * HTTP MBean.
 */
public interface HttpMBean {

    /**
     * List details for servlets.
     *
     * @return A {@link TabularData} containing the servlets information.
     * @throws MBeanException In case of MBean failure.
     */
    TabularData getServlets() throws MBeanException;

    /**
     * List configured HTTP proxies.
     */
    Map<String, Proxy> getProxies() throws MBeanException;

    /**
     * List the available balancing policies.
     */
    Collection<String> getProxyBalancingPolicies() throws MBeanException;

    /**
     * Add a new HTTP proxy using URL, proxyTo and prefix.
     */
    void addProxy(String url, String proxyTo, String balancingPolicy) throws MBeanException;

    /**
     * Remove an existing HTTP proxy identified by URL.
     */
    void removeProxy(String url) throws MBeanException;

}
