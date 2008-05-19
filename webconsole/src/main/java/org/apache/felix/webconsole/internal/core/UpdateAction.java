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
package org.apache.felix.webconsole.internal.core;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>UpdateAction</code> TODO
 */
public class UpdateAction extends BundleAction
{

    public static final String NAME = "update";

    public static final String LABEL = "Update";

    private static final String INSTALLER_SERVICE_NAME = "org.apache.sling.osgi.assembly.installer.InstallerService";

    // track the optional installer service manually
    private ServiceTracker installerService;


    public void setBundleContext( BundleContext bundleContext )
    {
        super.setBundleContext( bundleContext );

        installerService = new ServiceTracker( bundleContext, INSTALLER_SERVICE_NAME, null );
        installerService.open();
    }


    public String getName()
    {
        return NAME;
    }


    public String getLabel()
    {
        return LABEL;
    }


    public boolean performAction( HttpServletRequest request, HttpServletResponse response )
    {

        long bundleId = this.getBundleId( request );
        if ( bundleId > 0 )
        { // cannot stop system bundle !!
            Bundle bundle = this.getBundleContext().getBundle( bundleId );
            if ( bundle != null )
            {
                try
                {
                    this.updateFromRepo( bundle );
                }
                catch ( Throwable t )
                {
                    getLog().log( LogService.LOG_ERROR, "Uncaught Problem", t );
                }

            }
        }

        return true;
    }


    private void updateFromRepo( final Bundle bundle )
    {
        /*
                final InstallerService is = (InstallerService) installerService.getService();
                if (is == null) {
                    return;
                }

                final String name = bundle.getSymbolicName();
                final String version = (String) bundle.getHeaders().get(
                    Constants.BUNDLE_VERSION);

                // the name is required, otherwise we can do nothing
                if (name == null) {
                    return;
                }

                // TODO: Should be restrict to same major.micro ??

                Thread t = new Thread("Background Update") {
                    public void run() {
                        // wait some time for the request to settle
                        try {
                            sleep(500L);
                        } catch (InterruptedException ie) {
                            // don't care
                        }

                        Installer installer = is.getInstaller();
                        installer.addBundle(name, new VersionRange(version), -1);
                        try {
                            installer.install(false);
                        } catch (InstallerException ie) {
                            Throwable cause = (ie.getCause() != null)
                                    ? ie.getCause()
                                    : ie;
                            getLog().log(LogService.LOG_ERROR, "Cannot update", cause);
                        } finally {
                            installer.dispose();
                        }
                    }
                };

                t.setDaemon(true); // make a daemon thread (detach from current thread)
                t.start();
                */
    }

}
