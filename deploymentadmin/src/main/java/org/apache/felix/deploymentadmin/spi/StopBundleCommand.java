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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.log.LogService;

/**
 * Command that stops all bundles described in the target deployment package of a deployment session.
 */
public class StopBundleCommand extends Command {

    public void execute(DeploymentSessionImpl session) throws DeploymentException {
        AbstractDeploymentPackage target = session.getTargetAbstractDeploymentPackage();
        BundleInfo[] bundleInfos = target.getOrderedBundleInfos();
        for (int i = 0; i < bundleInfos.length; i++) {
            if (isCancelled()) {
                throw new DeploymentException(DeploymentException.CODE_CANCELLED);
            }
            Bundle bundle = target.getBundle(bundleInfos[i].getSymbolicName());
            if (bundle != null) {
                addRollback(new StartBundleRunnable(bundle));
                try {
                    bundle.stop();
                }
                catch (BundleException e) {
                	session.getLog().log(LogService.LOG_WARNING, "Could not stop bundle '" + bundle.getSymbolicName() + "'", e);
                }
            }
            else {
            	session.getLog().log(LogService.LOG_WARNING, "Could not stop bundle '" + bundleInfos[i].getSymbolicName() + "' because it was not defined int he framework");
            }
        }
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

