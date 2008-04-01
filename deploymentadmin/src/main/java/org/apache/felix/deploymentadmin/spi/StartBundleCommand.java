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
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Command that starts all bundles described in the source deployment package of a deployment session.
 */
public class StartBundleCommand extends Command {

    private final RefreshPackagesMonitor m_refreshMonitor = new RefreshPackagesMonitor();
    private static final int REFRESH_TIMEOUT = 10000;

    public void execute(DeploymentSessionImpl session) {
        AbstractDeploymentPackage source = session.getSourceAbstractDeploymentPackage();
        BundleContext context = session.getBundleContext();
        PackageAdmin packageAdmin = session.getPackageAdmin();
        RefreshPackagesListener listener = new RefreshPackagesListener();
        LogService log = session.getLog();

        context.addFrameworkListener(listener);
        packageAdmin.refreshPackages(null);
        m_refreshMonitor.waitForRefresh();
        context.removeFrameworkListener(listener);

        // start source bundles
        BundleInfoImpl[] bundleInfos = source.getOrderedBundleInfos();
        for (int i = 0; i < bundleInfos.length; i++) {
            BundleInfoImpl bundleInfoImpl = bundleInfos[i];
            if(!bundleInfoImpl.isCustomizer()) {
                Bundle bundle = source.getBundle(bundleInfoImpl.getSymbolicName());
                if (bundle != null) {
                    try {
                        bundle.start();
                    }
                    catch (BundleException be) {
                        log.log(LogService.LOG_WARNING, "Could not start bundle '" + bundle.getSymbolicName() + "'", be);
                    }
                }
                else {
                	log.log(LogService.LOG_WARNING, "Could not start bundle '" + bundleInfoImpl.getSymbolicName() + "' because it is not defined in the framework");
                }
            }
        }
    }

    /**
     * RefreshPackagesListener is only listing to FrameworkEvents of the type PACKAGES_REFRESHED. It will
     * notify any object waiting the completion of a refreshpackages() call.
     */
    private class RefreshPackagesListener implements FrameworkListener {
        public void frameworkEvent(FrameworkEvent event) {
            if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
                // TODO: m_log.log(LogService.LOG_INFO, "Packages refreshed event received");
                m_refreshMonitor.proceed();
            }
        }
    }

    /**
     * Use this monitor when its desired to wait for the completion of the asynchronous PackageAdmin.refreshPackages() call.
     */
    private class RefreshPackagesMonitor {
        private boolean m_alreadyNotified = false;

        /**
         * Waits for the completion of the PackageAdmin.refreshPackages() call. Because
         * its not sure whether all OSGi framework implementations implement this method as
         * specified we have build in a timeout. So if a event about the completion of the
         * refreshpackages() is never received, we continue after the timeout whether the refresh
         * was done or not.
         */
        public synchronized void waitForRefresh() {
            if (!m_alreadyNotified) {
             // TODO: m_log.log(LogService.LOG_DEBUG, "wait for Packages refreshed event");
                try {
                    wait(REFRESH_TIMEOUT);
                }
                catch (InterruptedException ie) {
                 // TODO: m_log.log(LogService.LOG_INFO, "interrupted while waiting for packages refreshed event", ie);
                }
                finally {
                    // just reset the misted notification variable, this Monitor object might be reused.
                    m_alreadyNotified = false;
                }
            }
            else {
                // TODO: m_log.log(LogService.LOG_DEBUG, "won't wait for Packages refreshed event, event is already received");
                // just reset the misted notification variable, this Monitor object might be reused.
                m_alreadyNotified = false;
            }
        }

        /**
         * After a PACKAGES_REFRESHED event notify all the parties interested in the completion of
         * the PackageAdmin.refreshPackages() call.
         */
        public synchronized void proceed() {
            m_alreadyNotified = true;
            notifyAll();
        }
    }

}
