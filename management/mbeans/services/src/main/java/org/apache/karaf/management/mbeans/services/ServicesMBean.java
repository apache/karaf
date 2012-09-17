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
package org.apache.karaf.management.mbeans.services;

import javax.management.openmbean.TabularData;

/**
 * Services MBean.
 */
public interface ServicesMBean {

    /**
     * Get the list of services.
     *
     * @return the list of services.
     * @throws Exception
     */
    TabularData getServices() throws Exception;

    /**
     * Get the list of services, eventually currently in use.
     *
     * @param inUse true to list only the services in use, false else.
     * @return the list of services.
     * @throws Exception
     */
    TabularData getServices(boolean inUse) throws Exception;

    /**
     * Get the list of services provided by a given bundle.
     *
     * @param bundleId the bundle ID.
     * @return the list of services provided by the given bundle.
     * @throws Exception
     */
    TabularData getServices(long bundleId) throws Exception;

    /**
     * Get the list of services provided by a given bundle and eventually in use.
     *
     * @param bundleId the bundle ID.
     * @param inUse true to list only the services in use, false else.
     * @return the list of services.
     * @throws Exception
     */
    TabularData getServices(long bundleId, boolean inUse) throws Exception;

    /**
     * @deprecated use the Services attribute instead
     */
    TabularData list() throws Exception;

    /**
     * @deprecated use getServices() instead
     */
    TabularData list(boolean inUse) throws Exception;

    /**
     * @deprecated use getServices() instead
     */
    TabularData list(long bundleId) throws Exception;

    /**
     * @deprecated use getServices() instead
     */
    TabularData list(long bundleId, boolean inUse) throws Exception;

}
