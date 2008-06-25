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


import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.Action;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;


public class RefreshRepoAction extends AbstractObrPlugin implements Action
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


    public boolean performAction( HttpServletRequest request, HttpServletResponse response )
    {

        RepositoryAdmin repoAdmin = getRepositoryAdmin();
        if ( repoAdmin != null )
        {
            String repositoryURL = request.getParameter( "repository" );
            Repository[] repos = repoAdmin.listRepositories();
            Repository repo = this.getRepository( repos, repositoryURL );

            URL repoURL = null;
            if ( repo != null )
            {
                repoURL = repo.getURL();
            }
            else
            {
                try
                {
                    repoURL = new URL( repositoryURL );
                }
                catch ( Throwable t )
                {
                    // don't care, just ignore
                }
            }

            // log.log(LogService.LOG_DEBUG, "Refreshing " + repo.getURL());
            if ( repoURL != null )
            {
                if ( request.getParameter( "remove" ) != null )
                {
                    try
                    {
                        repoAdmin.removeRepository( repoURL );
                    }
                    catch ( Exception e )
                    {
                        // TODO: log.log(LogService.LOG_ERROR, "Cannot refresh
                        // Repository " + repo.getURL());
                    }
                }
                else
                {
                    try
                    {
                        repoAdmin.addRepository( repoURL );
                    }
                    catch ( Exception e )
                    {
                        // TODO: log.log(LogService.LOG_ERROR, "Cannot refresh
                        // Repository " + repo.getURL());
                    }
                }
            }
        }

        return true;
    }


    // ---------- internal -----------------------------------------------------

    private Repository getRepository( Repository[] repos, String repositoryUrl )
    {
        if ( repositoryUrl == null || repositoryUrl.length() == 0 )
        {
            return null;
        }

        for ( int i = 0; i < repos.length; i++ )
        {
            if ( repositoryUrl.equals( repos[i].getURL().toString() ) )
            {
                return repos[i];
            }
        }

        return null;
    }
}
