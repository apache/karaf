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

import java.io.InputStream;

import org.osgi.framework.Bundle;

/**
  * This is the interface of the Deployment Admin service.<p>
  * 
  * The OSGi Service Platform provides mechanisms to manage the life cycle of
  * bundles, configuration objects, permission objects, etc. but the overall consistency
  * of the runtime configuration is the responsibility of the management
  * agent. In other words, the management agent decides to install, update,
  * or uninstall bundles, create or delete configuration or permission objects, as
  * well as manage other resource types, etc.<p>
  * 
  * The Deployment Admin service standardizes the access to some of the responsibilities
  * of the management agent. The service provides functionality to manage Deployment Packages 
  * (see {@link DeploymentPackage}). A Deployment Package groups resources as a unit 
  * of management. A Deployment Package is something that can be installed, updated, 
  * and uninstalled as a unit.<p> 
  * 
  * The Deployment Admin functionality is exposed as a standard OSGi service with no 
  * mandatory service parameters.
  */
public interface DeploymentAdmin {

	/**
	 * Installs a Deployment Package from an input stream. If a version of that Deployment Package
	 * is already installed and the versions are different, the installed version is updated
	 * with this new version even if it is older (downgrade). If the two versions are the same, then this 
	 * method simply returns with the old (target) Deployment Package without any action.
	 *  
	 * @param  in the input stream the Deployment Package can be read from. It mustn't be <code>null</code>.
	 * @return A DeploymentPackage object representing the newly installed/updated Deployment Package. 
	 *         It is never <code>null</code>. 
	 * @throws IllegalArgumentException if the got InputStream parameter is <code>null</code>         
	 * @throws DeploymentException if the installation was not successful. For detailed error code description 
	 *         see {@link DeploymentException}.
	 * @throws SecurityException if the caller doesn't have the appropriate
	 *         {@link DeploymentAdminPermission}("&lt;filter&gt;", "install") permission.
	 * @see DeploymentAdminPermission
	 * @see DeploymentPackage
	 * @see DeploymentPackage
	 */
    DeploymentPackage installDeploymentPackage(InputStream in) throws DeploymentException;

    /**
      * Lists the Deployment Packages currently installed on the platform.<p>
      * 
      * {@link DeploymentAdminPermission}("&lt;filter&gt;", "list") is 
      * needed for this operation to the effect that only those packages are listed in  
      * the array to which the caller has appropriate DeploymentAdminPermission. It has 
      * the consequence that the method never throws SecurityException only doesn't 
      * put certain Deployment Packages into the array.<p>
      * 
      * During an installation of an existing package (update) or during an uninstallation, 
      * the target must remain in this list until the installation (uninstallation) process 
      * is completed, after which the source (or <code>null</code> in case of uninstall) 
      * replaces the target.
      * 
      * @return the array of <code>DeploymentPackage</code> objects representing all the 
      *         installed Deployment Packages. The return value cannot be <code>null</code>. 
      *         In case of missing permissions it may give back an empty array.
      * @see DeploymentPackage
      * @see DeploymentAdminPermission
      */
    DeploymentPackage[] listDeploymentPackages();

    /**
     * Gets the currenlty installed {@link DeploymentPackage} instance which has the given 
     * symbolic name.<p>
     * 
     * During an installation of an existing package (update) or during an uninstallation, 
     * the target Deployment Package must remain the return value until the installation 
     * (uninstallation) process is completed, after which the source (or <code>null</code> 
     * in case of uninstall) is the return value.
     * 
     * @param  symbName the symbolic name of the Deployment Package to be retrieved. It mustn't be 
     *         <code>null</code>.
     * @return The <code>DeploymentPackage</code> for the given symbolic name. 
     *         If there is no Deployment Package with that symbolic name currently installed, 
     *         <code>null</code> is returned.
     * @throws IllegalArgumentException if the given <code>symbName</code> is <code>null</code>
     * @throws SecurityException if the caller doesn't have the appropriate 
     *         {@link DeploymentAdminPermission}("&lt;filter&gt;", "list") permission.
     * @see DeploymentPackage
     * @see DeploymentAdminPermission
     */
    DeploymentPackage getDeploymentPackage(String symbName);  

    /**
     * Gives back the installed {@link DeploymentPackage} that owns the bundle. Deployment Packages own their 
     * bundles by their Bundle Symbolic Name. It means that if a bundle belongs to an installed 
     * Deployment Packages (and at most to one) the Deployment Admin assigns the bundle to its owner  
     * Deployment Package by the Symbolic Name of the bundle.<p>
     * 
     * @param bundle the bundle whose owner is queried 
     * @return the Deployment Package Object that owns the bundle or <code>null</code> if the bundle doesn't 
     *         belong to any Deployment Packages (standalone bundles)
     * @throws IllegalArgumentException if the given <code>bundle</code> is <code>null</code>
     * @throws SecurityException if the caller doesn't have the appropriate 
     *         {@link DeploymentAdminPermission}("&lt;filter&gt;", "list") permission.
     * @see DeploymentPackage
     * @see DeploymentAdminPermission
     */
    DeploymentPackage getDeploymentPackage(Bundle bundle);  
  
    /**
     * This method cancels the currently active deployment session. This method addresses the need
     * to cancel the processing of excessively long running, or resource consuming install, update
     * or uninstall operations.<p>
     * 
     * @return true if there was an active session and it was successfully cancelled.
     * @throws SecurityException if the caller doesn't have the appropriate 
     *         {@link DeploymentAdminPermission}("&lt;filter&gt;", "cancel") permission.
     * @see DeploymentAdminPermission
     */
    boolean cancel();     
    
}
