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

/**
 * POJO describing proxy definition.
 */
public class Proxy {

    private String url;
    private String proxyTo;
    private String balancingPolicy;

    public Proxy(String url, String proxyTo, String balancingPolicy) {
        this.url = url;
        this.proxyTo = proxyTo;
        this.balancingPolicy = balancingPolicy;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProxyTo() {
        return proxyTo;
    }

    public void setProxyTo(String proxyTo) {
        this.proxyTo = proxyTo;
    }

    public String getBalancingPolicy() {
        return balancingPolicy;
    }

    public void setBalancingPolicy(String balancingPolicy) {
        this.balancingPolicy = balancingPolicy;
    }

}
