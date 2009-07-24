/*
 * Copyright (c) OSGi Alliance (2005, 2008). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.service.deploymentadmin;

import org.osgi.framework.Version;

/**
 * Represents a bundle in the array given back by the {@link DeploymentPackage#getBundleInfos()}  
 * method.
 */
public interface BundleInfo {
	
	/**
	 * Returns the Bundle Symbolic Name of the represented bundle.
	 * 
	 * @return the Bundle Symbolic Name 
	 */
	String getSymbolicName();
	
	/**
	 * Returns the version of the represented bundle.
	 * 
	 * @return the version of the represented bundle
	 */
	Version getVersion();

}
