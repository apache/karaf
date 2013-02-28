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
package org.apache.karaf.management.mbeans.packages;

import java.util.List;

public interface PackagesMBean {

    /**
     * Get the exported packages.
     *
     * @return the list of exported packages.
     * @throws Exception
     */
    List<String> getExports() throws Exception;

    /**
     * Get the exported packages of a given bundle.
     *
     * @param bundleId the bundle ID.
     * @return the exported packages of the bundle.
     * @throws Exception
     */
    List<String> getExports(long bundleId) throws Exception;

    /**
     * Get the imported packages.
     *
     * @return the list of imported packages.
     * @throws Exception
     */
    List<String> getImports() throws Exception;

    /**
     * Get the imported packages of a given bundle.
     *
     * @param bundleId the bundle ID.
     * @return the list of imported packages of the bundle.
     * @throws Exception
     */
    List<String> getImports(long bundleId) throws Exception;

    /* for backward compatibility */

    /**
     * @deprecated use getExports() instead
     */
    List<String> exportedPackages() throws Exception;

    /**
     * @deprecated use getExports() instead
     */
    List<String> exportedPackages(long bundleId) throws Exception;

    /**
     * @deprecated use getImports(bundleId) instead
     */
    List<String> importedPackages() throws Exception;

    /**
     * @deprecated use getImports(bundleId) instead
     */
    List<String> importedPackages(long bundleId) throws Exception;

}
