/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.deploymentadmin.spi;

import org.apache.felix.deploymentadmin.AbstractDeploymentPackage;
import org.apache.felix.deploymentadmin.BundleInfoImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.log.LogService;

/**
 * Command that stops all bundles described in the target deployment package of a deployment session.
 * 
 * By spec every single bundle of the target package should be stopped, even if this is not strictly necessary 
 * because of bundles being unaffected by an update. To be able to skip the stopping of unaffected bundles the 
 * following system property can be defined: <code>org.apache.felix.deploymentadmin.stopunaffectedbundle</code>.
 * If this property has value <code>false</code> (case insensitive) then unaffected bundles will not be stopped, 
 * in all other cases the bundles will be stopped according to the OSGi specification.
 */
public class StopBundleCommand extends Command {

    public void execute(DeploymentSessionImpl session) throws DeploymentException {
        AbstractDeploymentPackage target = session.getTargetAbstractDeploymentPackage();
        BundleInfo[] bundleInfos = target.getOrderedBundleInfos();
        for (int i = 0; i < bundleInfos.length; i++) {
            if (isCancelled()) {
                throw new DeploymentException(DeploymentException.CODE_CANCELLED);
            }
            String symbolicName = bundleInfos[i].getSymbolicName();
			Bundle bundle = target.getBundle(symbolicName);
            if (bundle != null) {
            	String stopUnaffectedBundle = System.getProperty("org.apache.felix.deploymentadmin.stopunaffectedbundle", "true");
        		if (stopUnaffectedBundle.equalsIgnoreCase("false") && omitBundleStop(session, symbolicName)) {
        			continue;
        		}
                addRollback(new StartBundleRunnable(bundle));
                try {
                    bundle.stop();
                }
                catch (BundleException e) {
                	session.getLog().log(LogService.LOG_WARNING, "Could not stop bundle '" + bundle.getSymbolicName() + "'", e);
                }
            }
            else {
            	session.getLog().log(LogService.LOG_WARNING, "Could not stop bundle '" + symbolicName + "' because it was not defined int he framework");
            }
        }
    }

    /**
     * Determines whether stopping a bundle is strictly needed.
     * 
     * @param session The current deployment session.
     * @param symbolicName The symbolic name of the bundle to inspect.
     * 
     * @return Returns <code>true</code> if <code>Constants.DEPLOYMENTPACKAGE_MISSING</code> is true for the specified bundle in the
     * source deployment package or if the version of the bundle is the same in both source and target deployment package. Returns 
     * <code>false</code> otherwise.
     */
    private boolean omitBundleStop(DeploymentSessionImpl session, String symbolicName) {
    	boolean result = false;
		BundleInfoImpl sourceBundleInfo = session.getSourceAbstractDeploymentPackage().getBundleInfoByName(symbolicName);
		BundleInfoImpl targetBundleInfo = session.getSourceAbstractDeploymentPackage().getBundleInfoByName(symbolicName);
		boolean fixPackageMissing = sourceBundleInfo != null  && sourceBundleInfo.isMissing();
		boolean sameVersion = (targetBundleInfo != null && sourceBundleInfo != null && targetBundleInfo.getVersion().equals(sourceBundleInfo.getVersion()));
		if (fixPackageMissing || sameVersion) {
			result = true;
		}
		return result;
	}

	private class StartBundleRunnable implements Runnable {

        private final Bundle m_bundle;

        public StartBundleRunnable(Bundle bundle) {
            m_bundle = bundle;
        }

        public void run() {
            try {
                m_bundle.start();
            }
            catch (BundleException e) {
                // TODO: log this
                e.printStackTrace();
            }
        }

    }
}

