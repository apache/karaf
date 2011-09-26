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
package org.apache.karaf.management.mbeans.bundles;

import javax.management.openmbean.TabularData;

/**
 * Bundles MBean.
 */
public interface BundlesMBean {

    TabularData list() throws Exception;

    int getStartLevel(long bundleId) throws Exception;
    void setStartLevel(long bundleId, int bundleStartLevel) throws Exception;

    void refresh() throws Exception;
    void refresh(long bundleId) throws Exception;

    void update(long bundleId) throws Exception;
    void update(long bundleId, String location) throws Exception;

    void resolve() throws Exception;
    void resolve(long bundleId) throws Exception;

    void restart(long bundleId) throws Exception;

    long install(String url) throws Exception;
    long install(String url, boolean start) throws Exception;

    void start(long bundleId) throws Exception;

    void stop(long bundleId) throws Exception;

    void uninstall(long bundleId) throws Exception;

}
