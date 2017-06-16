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
package org.apache.karaf.bundle.core;

import javax.management.MBeanException;
import javax.management.openmbean.TabularData;

/**
 * Bundles MBean.
 */
public interface BundlesMBean {

    TabularData getBundles() throws MBeanException;

    int getStartLevel(String bundleId) throws MBeanException;
    void setStartLevel(String bundleId, int bundleStartLevel) throws MBeanException;

    void refresh() throws MBeanException;
    void refresh(String bundleId) throws MBeanException;

    void update(String bundleId) throws MBeanException;
    void update(String bundleId, boolean refresh) throws MBeanException;
    void update(String bundleId, String location) throws MBeanException;
    void update(String bundleId, String location, boolean refresh) throws MBeanException;

    void resolve() throws MBeanException;
    void resolve(String bundleId) throws MBeanException;

    void restart(String bundleId) throws MBeanException;

    long install(String url) throws MBeanException;
    long install(String url, boolean start) throws MBeanException;

    void start(String bundleId) throws MBeanException;

    void stop(String bundleId) throws MBeanException;

    void uninstall(String bundleId) throws MBeanException;

    TabularData getDiag() throws MBeanException;

    String getDiag(long bundleId);

    String getStatus(String bundleId) throws MBeanException;

}
