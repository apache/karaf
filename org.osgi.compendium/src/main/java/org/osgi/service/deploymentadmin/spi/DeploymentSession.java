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
package org.osgi.service.deploymentadmin.spi;

import org.osgi.service.deploymentadmin.DeploymentPackage;

/**
 * The session interface represents a currently running deployment session 
 * (install/update/uninstall).<p>
 * 
 * When a deployment package is installed the target package, when uninstalled the 
 * source package is an empty deployment package. The empty deployment package is a virtual 
 * entity it doesn't appear for the outside world. It is only visible on the 
 * DeploymentSession interface used by Resource Processors. Although  the empty package 
 * is only visible for Resource Processors it has the following characteristics:<p>
 *  
 * <ul>
 *     <li>has version 0.0.0</li>
 *     <li>its name is an empty string</li>
 *     <li>it is stale</li>
 *     <li>it has no bundles
 *     		(see {@link DeploymentPackage#getBundle(String)})</li>
 *     <li>it has no resources
 *     		(see {@link DeploymentPackage#getResources()})</li>
 *     <li>it has no headers except <br/>
 *     		<code>DeploymentPackage-SymbolicName</code> and <br/>
 *     		<code>DeploymentPackage-Version</code> <br/>
 *     		(see {@link DeploymentPackage#getHeader(String)})</li>
 *     <li>it has no resource headers (see 
 *     		{@link DeploymentPackage#getResourceHeader(String, String)})</li>
 *     <li>{@link DeploymentPackage#uninstall()} throws
 *     		{@link java.lang.IllegalStateException}</li>
 *     <li>{@link DeploymentPackage#uninstallForced()} throws
 *     		{@link java.lang.IllegalStateException}</li>
 * </ul>
 *  
 */
public interface DeploymentSession {
    
    /**
     * If the deployment action is an update or an uninstall, this call returns
     * the <code>DeploymentPackage</code> instance for the installed deployment package. If the 
     * deployment action is an install, this call returns the empty deploymet package (see
     * {@link DeploymentPackage}).
     * 
     * @return the target deployment package
     * @see DeploymentPackage
     */
    DeploymentPackage getTargetDeploymentPackage();
    
    /**
     * If the deployment action is an install or an update, this call returns
     * the <code>DeploymentPackage</code> instance that corresponds to the deployment package
     * being streamed in for this session. If the deployment action is an uninstall, this call 
     * returns the empty deploymet package (see {@link DeploymentPackage}).
     * 
     * @return the source deployment package
     * @see DeploymentPackage
     */ 
    DeploymentPackage getSourceDeploymentPackage();

    /**
     * Returns the private data area of the specified bundle. The bundle must be part of 
     * either the source or the target deployment packages. The permission set the caller 
     * resource processor needs to manipulate the private area of the bundle is set by the 
     * Deployment Admin on the fly when this method is called. The permissions remain available 
     * during the deployment action only.<p>
     * 
     * The bundle and the caller Resource Processor have to be in the same Deployment Package.
     * 
     * @param bundle the bundle the private area belongs to
     * @return file representing the private area of the bundle. It cannot be null.
     * @throws SecurityException if the caller doesn't have the appropriate 
     *         {@link DeploymentCustomizerPermission}("&lt;filter&gt;", "privatearea") permission.
     * @see DeploymentPackage
     * @see DeploymentCustomizerPermission
     */     
    java.io.File getDataFile(org.osgi.framework.Bundle bundle);
     
}

