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
package org.apache.karaf.features.internal.service;

/**
 * Interface for features service implementations that support boot lifecycle management.
 */
public interface BootManaged {

    boolean isBootDone();

    void bootDone();

    /**
     * @return {@code true} if a deployment was interrupted after its bundles were installed, but before they were
     *         started (e.g. by stopping the framework), see {@link #resumeDeployment()}
     */
    default boolean isDeploymentPending() {
        return false;
    }

    /**
     * Provisions the current requirements again to complete an interrupted deployment, which in particular starts
     * the bundles of started features that were left installed or resolved only.
     */
    default void resumeDeployment() throws Exception {
    }

}
