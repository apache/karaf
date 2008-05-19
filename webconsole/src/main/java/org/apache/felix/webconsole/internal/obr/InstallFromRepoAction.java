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
package org.apache.felix.webconsole.internal.obr;


import org.apache.felix.webconsole.Action;


public abstract class InstallFromRepoAction extends AbstractObrPlugin implements Action
{

    public static final String NAME = "installFromOBR";


    public String getName()
    {
        return NAME;
    }


    public String getLabel()
    {
        return NAME;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.sling.manager.web.internal.Action#performAction(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
    public boolean performAction(HttpServletRequest request,
            HttpServletResponse response) {

        // check whether we have to do something
        String[] bundles = request.getParameterValues("bundle");
        if (bundles == null || bundles.length == 0) {
            getLog().log(LogService.LOG_INFO, "No resources to deploy");
            return true;
        }

        InstallerService installerService = getInstallerService();
        if (installerService != null) {
            Installer installer = installerService.getInstaller();

            // prepare the deployment
            for (int i = 0; i < bundles.length; i++) {
                String bundle = bundles[i];
                int comma = bundle.indexOf(',');
                String name = (comma > 0) ? bundle.substring(0, comma) : bundle;
                String version = (comma < bundle.length() - 1)
                        ? bundle.substring(comma + 1)
                        : null;

                if (name.length() > 0) {
                    // no name, ignore this one
                    VersionRange versionRange = new VersionRange(version);
                    installer.addBundle(name, versionRange, -1);
                }
            }

            // check whether the "deploystart" button was clicked
            boolean start = request.getParameter("deploystart") != null;

            try {
                installer.install(start);
            } catch (InstallerException ie) {
                Throwable cause = (ie.getCause() != null) ? ie.getCause() : ie;
                getLog().log(LogService.LOG_ERROR, "Cannot install bundles",
                    cause);
            } finally {
                installer.dispose();
            }
        }

        // redirect to bundle list
        return true;
    }
     */

}
