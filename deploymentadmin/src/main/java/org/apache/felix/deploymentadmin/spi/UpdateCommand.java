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
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.deploymentadmin.AbstractDeploymentPackage;
import org.apache.felix.deploymentadmin.AbstractInfo;
import org.apache.felix.deploymentadmin.BundleInfoImpl;
import org.apache.felix.deploymentadmin.Constants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentException;

/**
 * Command that installs all bundles described in the source deployment package of a deployment
 * session. If a bundle was already defined in the target deployment package of the same session
 * it is updated, otherwise the bundle is simply installed.
 */
public class UpdateCommand extends Command {

    public void execute(DeploymentSessionImpl session) throws DeploymentException {
        AbstractDeploymentPackage source = session.getSourceAbstractDeploymentPackage();
        AbstractDeploymentPackage targetPackage = session.getTargetAbstractDeploymentPackage();
        BundleContext context = session.getBundleContext();

        Map expectedBundles = new HashMap();
        AbstractInfo[] bundleInfos = (AbstractInfo[]) source.getBundleInfos();
        for (int i = 0; i < bundleInfos.length; i++) {
            AbstractInfo bundleInfo = bundleInfos[i];
            if(!bundleInfo.isMissing()) {
                expectedBundles.put(bundleInfo.getPath(), bundleInfo);
            }
        }

        try {
            while (!expectedBundles.isEmpty()) {
            	AbstractInfo entry = source.getNextEntry();
            	if (entry == null) {
                	throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Expected more bundles in the stream: " + expectedBundles.keySet());
            	}
            	
            	String name = entry.getPath();
                BundleInfoImpl bundleInfo = (BundleInfoImpl) expectedBundles.remove(name);
                if (bundleInfo == null) {
                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Resource '" + name + "' is not described in the manifest.");
                }

                Bundle bundle = source.getBundle(bundleInfo.getSymbolicName());
                try {
                    if (bundle == null) {
                        // new bundle, install it
                        bundle = context.installBundle(Constants.BUNDLE_LOCATION_PREFIX + bundleInfo.getSymbolicName(), new BundleInputStream(source.getCurrentEntryStream()));
                        addRollback(new UninstallBundleRunnable(bundle));
                    } else {
                        // existing bundle, update it
                        Version sourceVersion = bundleInfo.getVersion();
                        Version targetVersion = Version.parseVersion((String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION));
                        if (!sourceVersion.equals(targetVersion)) {
                            bundle.update(new BundleInputStream(source.getCurrentEntryStream()));
                            addRollback(new UpdateBundleRunnable(bundle, targetPackage, bundleInfo.getSymbolicName()));
                        }
                    }
                }
                catch (BundleException be) {
                    if (isCancelled()) {
                        return;
                    }
                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Could not install new bundle '" + name + "'", be);
                }
                if (!bundle.getSymbolicName().equals(bundleInfo.getSymbolicName()) || !Version.parseVersion((String)bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION)).equals(bundleInfo.getVersion())) {
                    throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Installed/updated bundle version and/or symbolicnames do not match what was installed/updated");
                }
            }
        }
        catch (IOException e) {
            throw new DeploymentException(DeploymentException.CODE_OTHER_ERROR, "Problem while reading stream", e);
        }
    }

    private static class UninstallBundleRunnable implements Runnable {

        private final Bundle m_bundle;

        public UninstallBundleRunnable(Bundle bundle) {
            m_bundle = bundle;
        }

        public void run() {
            try {
                m_bundle.uninstall();
            }
            catch (BundleException e) {
                // TODO: log this
                e.printStackTrace();
            }
        }
    }

    private static class UpdateBundleRunnable implements Runnable {

        private final Bundle m_bundle;
        private final AbstractDeploymentPackage m_targetPackage;
        private final String m_symbolicName;

        public UpdateBundleRunnable(Bundle bundle, AbstractDeploymentPackage targetPackage, String symbolicName) {
            m_bundle = bundle;
            m_targetPackage = targetPackage;
            m_symbolicName = symbolicName;
        }

        public void run() {
            try {
                m_bundle.update(m_targetPackage.getBundleStream(m_symbolicName));
            }
            catch (Exception e) {
                // TODO: log this
                e.printStackTrace();
            }
        }
    }

    private final class BundleInputStream extends InputStream {
        private final InputStream m_inputStream;

        private BundleInputStream(InputStream jarInputStream) {
            m_inputStream = jarInputStream;
        }

        public int read() throws IOException {
            checkCancel();
            return m_inputStream.read();
        }

        public int read(byte[] buffer) throws IOException {
            checkCancel();
            return m_inputStream.read(buffer);
        }

        public int read(byte[] buffer, int off, int len) throws IOException {
            checkCancel();
            return m_inputStream.read(buffer, off, len);
        }

        private void checkCancel() throws IOException {
            if (isCancelled()) {
                throw new IOException("Stream was cancelled");
            }
        }
    }

}
