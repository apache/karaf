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
package org.apache.karaf.packages.core;

import java.util.List;

public interface PackageService {

	/**
	 * Get the simplified package exports of a bundle. This does not show the
	 * package versions.
	 * 
	 * @param bundleId The bundle ID.
	 * @return The {@link List} of package exports in the given bundle.
	 */
    List<String> getExports(long bundleId);

    List<String> getImports(long bundleId);

	/**
	 * Get all package exports with their version, and the bundles exporting them.
	 * 
	 * @return A {@link List} containing all package exports (as {@link PackageVersion}).
	 */
    List<PackageVersion> getExports();

    /**
	 * Get all package imports with their requirement.
     *  
     * @return A {@link List} containing all package imports (as {@link PackageRequirement}).
     */
    List<PackageRequirement> getImports();

}
