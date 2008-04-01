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

import java.io.IOException;
import java.io.InputStream;

import org.apache.felix.deploymentadmin.AbstractDeploymentPackage;
import org.apache.felix.deploymentadmin.BundleInfoImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.log.LogService;

/**
 * Command that uninstalls bundles, if rolled back the bundles are restored.
 */
public class DropBundleCommand extends Command {

    public void execute(DeploymentSessionImpl session) throws DeploymentException {
        AbstractDeploymentPackage target = session.getTargetAbstractDeploymentPackage();
        AbstractDeploymentPackage source = session.getSourceAbstractDeploymentPackage();
        LogService log = session.getLog();

        BundleInfoImpl[] orderedTargetBundles = target.getOrderedBundleInfos();
        for (int i = orderedTargetBundles.length - 1; i >= 0; i--) {
            BundleInfoImpl bundleInfo = orderedTargetBundles[i];
            if (!bundleInfo.isCustomizer() && source.getBundleInfoByName(bundleInfo.getSymbolicName()) == null) {
                // stale bundle, save a copy for rolling back and uninstall it
                String symbolicName = bundleInfo.getSymbolicName();
                try {
                    Bundle bundle = target.getBundle(symbolicName);
                    bundle.uninstall();
                    addRollback(new InstallBundleRunnable(bundle, target.getBundleStream(symbolicName), log));
                }
                catch (BundleException be) {
                    log.log(LogService.LOG_WARNING, "Bundle '" + symbolicName + "' could not be uninstalled", be);
                }
                catch (IOException e) {
                    log.log(LogService.LOG_WARNING, "Could not get bundle data stream for bundle '" + symbolicName + "'", e);
                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Could not prepare rollback for uninstalling bundle '" + symbolicName + "'");
                }
            }
        }
    }

    private static class InstallBundleRunnable implements Runnable {

        private final InputStream m_bundleStream;
        private final Bundle m_bundle;
        private final LogService m_log;

        public InstallBundleRunnable(Bundle bundle, InputStream bundleStream, LogService log) {
            m_bundle = bundle;
            m_bundleStream = bundleStream;
            m_log = log;
        }

        public void run() {
            try {
                m_bundle.update(m_bundleStream);
            }
            catch (BundleException e) {
                m_log.log(LogService.LOG_WARNING, "Could not rollback uninstallation of bundle '" + m_bundle.getSymbolicName() + "'", e);
            }
        }
    }
}
