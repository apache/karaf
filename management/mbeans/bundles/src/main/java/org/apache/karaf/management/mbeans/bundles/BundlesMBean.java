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

    /**
     * List the bundles.
     *
     * @return the list of bundles.
     * @throws Exception
     */
    TabularData getBundles() throws Exception;

    /**
     * @deprecated use getBundles() instead.
     */
    TabularData list() throws Exception;

    /**
     * Get the start level of a given bundle.
     *
     * @param bundleId the bundle ID.
     * @return the start level of the bundle.
     * @throws Exception
     */
    int getStartLevel(String bundleId) throws Exception;

    /**
     * Set the start level of a given bundle.
     *
     * @param bundleId the bundle ID.
     * @param bundleStartLevel the new start level of the bundle.
     * @throws Exception
     */
    void setStartLevel(String bundleId, int bundleStartLevel) throws Exception;

    /**
     * Refresh all bundles.
     *
     * @throws Exception
     */
    void refresh() throws Exception;

    /**
     * Refresh a given bundle.
     *
     * @param bundleId the bundle ID.
     * @throws Exception
     */
    void refresh(String bundleId) throws Exception;

    /**
     * Update a given bundle from its location.
     *
     * @param bundleId the bundle ID.
     * @throws Exception
     */
    void update(String bundleId) throws Exception;

    /**
     * Update a given bundle from a given location.
     *
     * @param bundleId the bundle ID.
     * @param location the target location.
     * @throws Exception
     */
    void update(String bundleId, String location) throws Exception;

    /**
     * Resolve the bundle.
     * @throws Exception
     */
    void resolve() throws Exception;

    /**
     * Resolve a given bundle.
     *
     * @param bundleId the bundle ID.
     * @throws Exception
     */
    void resolve(String bundleId) throws Exception;

    /**
     * Restart the given bundle.
     *
     * @param bundleId the bundle ID.
     * @throws Exception
     */
    void restart(String bundleId) throws Exception;

    /**
     * Install a bundle at a given URL.
     *
     * @param url the bundle URL.
     * @return the bundle ID.
     * @throws Exception
     */
    long install(String url) throws Exception;

    /**
     * Install a bundle at a given URL and start the bundle.
     *
     * @param url the bundle URL.
     * @param start true to start the bundle, false else.
     * @return the bundle ID.
     * @throws Exception
     */
    long install(String url, boolean start) throws Exception;

    /**
     * Start a given bundle.
     *
     * @param bundleId the bundle ID.
     * @throws Exception
     */
    void start(String bundleId) throws Exception;

    /**
     * Stop a given bundle.
     *
     * @param bundleId the bundle ID.
     * @throws Exception
     */
    void stop(String bundleId) throws Exception;

    /**
     * Uninstall a given bundle.
     *
     * @param bundleId the bundle ID.
     * @throws Exception
     */
    void uninstall(String bundleId) throws Exception;

}
