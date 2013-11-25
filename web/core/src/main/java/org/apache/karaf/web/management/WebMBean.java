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
package org.apache.karaf.web.management;

import javax.management.MBeanException;
import javax.management.openmbean.TabularData;
import java.util.List;

/**
 * Describe the Web MBean.
 */
public interface WebMBean {

    /**
     * Return the list of web bundles.
     * 
     * @return a tabular data of web bundles.
     * @throws Exception in case of lookup failure.
     */
    TabularData getWebBundles() throws MBeanException;

    /**
     * Start web context of the given web bundles (identified by ID).
     * 
     * @param bundleIds the list of bundle IDs.
     *                  TODO use a BundleSelector service
     * @throws Exception in case of start failure.
     */
    void start(List<Long> bundleIds) throws MBeanException;

    /**
     * Stop web contact of the given web bundles (identified by ID).
     *
     * @param bundleIds the list of bundle IDs.
     *                  TODO use a BundleSelector service
     * @throws Exception in case of stop failure
     */
    void stop(List<Long> bundleIds) throws MBeanException;
    
}
