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

import org.ops4j.pax.web.service.spi.WebEvent;

public class ServletInfo {
    private String name;
    private long bundleId;
    private String className;
    private String alias;
    private int state;
    private String[] urls;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public long getBundleId() {
        return bundleId;
    }
    public void setBundleId(long bundleId) {
        this.bundleId = bundleId;
    }
    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    public int getState() {
        return state;
    }
    public void setState(int state) {
        this.state = state;
    }
    public String getAlias() {
        return alias;
    }
    public void setAlias(String alias) {
        this.alias = alias;
    }
    public String[] getUrls() {
        return urls;
    }
    public void setUrls(String[] urls) {
        this.urls = urls;
    }
    
    public String getStateString() {
        switch (state) {
            case WebEvent.DEPLOYING:
                return "Deploying  ";
            case WebEvent.DEPLOYED:
                return "Deployed   ";
            case WebEvent.UNDEPLOYING:
                return "Undeploying";
            case WebEvent.UNDEPLOYED:
                return "Undeployed ";
            case WebEvent.FAILED:
                return "Failed     ";
            case WebEvent.WAITING:
                return "Waiting    ";
            default:
                return "Failed     ";
        }
    }
}
