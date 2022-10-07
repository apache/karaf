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
package org.apache.karaf.kar;

import javax.management.MBeanException;
import java.util.List;

public interface KarsMBean {

    /**
     * List the installed KAR files.
     *
     * @return the list of KAR files.
     * @throws MBeanException in case of listing failure.
     */
    List<String> getKars() throws MBeanException;

    /**
     * Create a kar file for a list of feature repos
     *
     * @param repoName the name of features repository
     * @param features the features to include in the kar
     */
    void create(String repoName, List<String> features);

    /**
     * Install a KAR file from the given URL.
     *
     * @param url the KAR URL.
     * @throws MBeanException in case of installation failure.
     */
    void install(String url) throws MBeanException;

    /**
     * Install a KAR file from a given URL and decide to start or not the bundles.
     *
     * @param url the KAR URL.
     * @param noAutoStartBundles true to not automatically start the bundles, false else.
     * @throws MBeanException in case of installation failure.
     */
    void install(String url, boolean noAutoStartBundles) throws MBeanException;

    /**
     * Install a KAR file from a given URL.
     *
     * @param url the KAR URL.
     * @param noAutoStartBundles true to not automatically start the bundles, false else.
     * @param noAutoRefreshBundles true to not automatically refresh the bundles, false else.
     * @throws MBeanException in case of installation failure.
     */
    void install(String url, boolean noAutoStartBundles, boolean noAutoRefreshBundles) throws MBeanException;

    /**
     * Uninstall a KAR file.
     * 
     * @param name the name of the KAR file.
     * @throws MBeanException in case of uninstall failure.
     */
    void uninstall(String name) throws MBeanException;

    /**
     * Uninstall a KAR file.
     *
     * @param name the name of the KAR file.
     * @param noAutoRefreshBundles true to not automatically refresh bundles, false else.
     * @throws MBeanException in case of uninstall failure.
     */
    void uninstall(String name, boolean noAutoRefreshBundles) throws MBeanException;

}
