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


public abstract class RefreshRepoAction extends AbstractObrPlugin implements Action
{

    public static final String NAME = "refreshOBR";

    public static final String PARAM_REPO = "repository";


    public String getName()
    {
        return NAME;
    }


    public String getLabel()
    {
        return NAME;
    }
    /*
        public boolean performAction(HttpServletRequest request,
                HttpServletResponse response) {

            BundleRepositoryAdmin repoAdmin = getBundleRepositoryAdmin();
            if (repoAdmin != null) {
                String repositoryURL = request.getParameter("repository");
                Iterator<Repository> repos = repoAdmin.getRepositories();
                Repository repo = this.getRepository(repos, repositoryURL);

                URL repoURL = null;
                if (repo != null) {
                    repoURL = repo.getURL();
                } else {
                    try {
                        repoURL = new URL(repositoryURL);
                    } catch (Throwable t) {
                        // don't care, just ignore
                    }
                }

                // log.log(LogService.LOG_DEBUG, "Refreshing " + repo.getURL());
                if (repoURL != null) {
                    try {
                        repoAdmin.addRepository(repoURL);
                    } catch (Exception e) {
                        // TODO: log.log(LogService.LOG_ERROR, "Cannot refresh
                        // Repository " + repo.getURL());
                    }
                }
            }

            return true;
        }

        // ---------- internal -----------------------------------------------------

        private Repository getRepository(Iterator<Repository> repos,
                String repositoryUrl) {
            if (repositoryUrl == null || repositoryUrl.length() == 0) {
                return null;
            }

            while (repos.hasNext()) {
                Repository repo = repos.next();
                if (repositoryUrl.equals(repo.getURL().toString())) {
                    return repo;
                }
            }

            return null;
        }
    */
}
