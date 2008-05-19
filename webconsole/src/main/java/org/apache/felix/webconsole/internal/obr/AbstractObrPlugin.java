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
package org.apache.felix.webconsole.internal.obr;


import org.apache.felix.webconsole.internal.BaseManagementPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;


public class AbstractObrPlugin extends BaseManagementPlugin
{

    // track the optional installer service manually
    private ServiceTracker installerService;


    public void setBundleContext( BundleContext bundleContext )
    {
        super.setBundleContext( bundleContext );

    }
    /*
        protected InstallerService getInstallerService() {
            if (installerService == null) {
                try {
                    installerService = new ServiceTracker(getBundleContext(),
                        InstallerService.class.getName(), null);
                    installerService.open();
                } catch (Throwable t) {
                    // missing InstallerService class ??
                    return null;
                }

            }

            return (InstallerService) installerService.getService();
        }

        protected BundleRepositoryAdmin getBundleRepositoryAdmin() {
            InstallerService is = getInstallerService();
            return (is != null) ? is.getBundleRepositoryAdmin() : null;
        }*/
}
