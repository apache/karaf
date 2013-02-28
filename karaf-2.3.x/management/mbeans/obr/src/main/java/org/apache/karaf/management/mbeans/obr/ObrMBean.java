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
package org.apache.karaf.management.mbeans.obr;

import javax.management.openmbean.TabularData;
import java.util.List;

/**
 * OBR MBean.
 */
public interface ObrMBean {

    /**
     * Get the URLs registered in the OBR service.
     *
     * @return the list of URLs in the OBR service.
     * @throws Exception
     */
    List<String> getUrls() throws Exception;

    /**
     * @deprecated use getUrls() instead.
     */
    List<String> listUrls() throws Exception;

    /**
     * Add a new URL in the OBR service.
     *
     * @param url the URL to add in the OBR service
     * @throws Exception
     */
    void addUrl(String url) throws Exception;

    /**
     * Remove an URL from the OBR service.
     *
     * @param url the URL to remove from the OBR service.
     * @throws Exception
     */
    void removeUrl(String url) throws Exception;

    /**
     * Refresh an URL in the OBR service.
     *
     * @param url the URL to refresh in the OBR service.
     * @throws Exception
     */
    void refreshUrl(String url) throws Exception;

    /**
     * Get the bundles available in the OBR service.
     *
     * @return the list of bundles available in the OBR service.
     * @throws Exception
     */
    TabularData getBundles() throws Exception;

    /**
     * @deprecated use getBundles() instead.
     */
    TabularData list() throws Exception;

    /**
     * Deploy a bundle available in the OBR service.
     *
     * @param bundle the bundle to deploy.
     * @throws Exception
     */
    void deploy(String bundle) throws Exception;

    /**
     * Deploy a bundle available in the OBR service and eventually start it.
     *
     * @param bundle the bundle to deploy.
     * @param start true to start the bundle, false else.
     * @param deployOptional true to deploy optional bundles, false else.
     * @throws Exception
     */
    void deploy(String bundle, boolean start, boolean deployOptional) throws Exception;

}
