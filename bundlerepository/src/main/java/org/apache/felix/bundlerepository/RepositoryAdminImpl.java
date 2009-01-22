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
package org.apache.felix.bundlerepository;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;

public class RepositoryAdminImpl implements RepositoryAdmin
{
    static BundleContext m_context = null;
    private final Logger m_logger;
    private final LocalRepositoryImpl m_local;
    private List m_urlList = new ArrayList();
    private Map m_repoMap = new HashMap();
    private boolean m_initialized = false;

    // Reusable comparator for sorting resources by name.
    private Comparator m_nameComparator = new ResourceComparator();

    public static final String REPOSITORY_URL_PROP = "obr.repository.url";
    public static final String EXTERN_REPOSITORY_TAG = "extern-repositories";

    public RepositoryAdminImpl(BundleContext context, Logger logger)
    {
        m_context = context;
        m_logger = logger;
        m_local = new LocalRepositoryImpl(context, logger);
    }

    LocalRepositoryImpl getLocalRepository()
    {
        return m_local;
    }

    public void dispose()
    {
        m_local.dispose();
    }

    public Repository addRepository(URL url) throws Exception
    {
        return addRepository(url, Integer.MAX_VALUE);
    }

    public synchronized Repository addRepository(URL url, int hopCount) throws Exception
    {
        if (!m_urlList.contains(url))
        {
            m_urlList.add(url);
        }

        // If the repository URL is a duplicate, then we will just
        // replace the existing repository object with a new one,
        // which is effectively the same as refreshing the repository.
        Repository repo = new RepositoryImpl(this, url, hopCount, m_logger);
        m_repoMap.put(url, repo);
        return repo;
    }

    public synchronized boolean removeRepository(URL url)
    {
        m_repoMap.remove(url);
        return (m_urlList.remove(url)) ? true : false;
    }

    public synchronized Repository[] listRepositories()
    {
        if (!m_initialized)
        {
            initialize();
        }
        return (Repository[]) m_repoMap.values().toArray(new Repository[m_repoMap.size()]);
    }

    public synchronized Resource getResource(String respositoryId)
    {
        // TODO: OBR - Auto-generated method stub
        return null;
    }

    public synchronized Resolver resolver()
    {
        if (!m_initialized)
        {
            initialize();
        }

        return new ResolverImpl(m_context, this, m_logger);
    }

    public synchronized Resource[] discoverResources(String filterExpr)
    {
        if (!m_initialized)
        {
            initialize();
        }

        Filter filter = null;
        try
        {
            filter = m_context.createFilter(filterExpr);
        }
        catch (InvalidSyntaxException ex)
        {
            m_logger.log(
                Logger.LOG_WARNING,
                "Error while discovering resources for " + filterExpr,
                ex);
            return new Resource[0];
        }

        Resource[] resources = null;
        MapToDictionary dict = new MapToDictionary(null);
        Repository[] repos = listRepositories();
        List matchList = new ArrayList();
        for (int repoIdx = 0; (repos != null) && (repoIdx < repos.length); repoIdx++)
        {
            resources = repos[repoIdx].getResources();
            for (int resIdx = 0; (resources != null) && (resIdx < resources.length); resIdx++)
            {
                dict.setSourceMap(resources[resIdx].getProperties());
                if (filter.match(dict))
                {
                    matchList.add(resources[resIdx]);
                }
            }
        }

        // Convert matching resources to an array an sort them by name.
        resources = (Resource[]) matchList.toArray(new Resource[matchList.size()]);
        Arrays.sort(resources, m_nameComparator);
        return resources;
    }

    private void initialize()
    {
        m_initialized = true;

        // Initialize the repository URL list if it is currently empty.
        if (m_urlList.size() == 0)
        {
            // First check the repository URL config property.
            String urlStr = m_context.getProperty(REPOSITORY_URL_PROP);
            if (urlStr != null)
            {
                StringTokenizer st = new StringTokenizer(urlStr);
                if (st.countTokens() > 0)
                {
                    while (st.hasMoreTokens())
                    {
                        final String token = st.nextToken();
                        try
                        {
                            m_urlList.add(new URL(token));
                        }
                        catch (MalformedURLException ex)
                        {
                            m_logger.log(
                                Logger.LOG_WARNING,
                                "Repository url " + token + " cannot be used. Skipped.",
                                ex);
                        }
                    }
                }
            }
        }

        m_repoMap.clear();

        for (int i = 0; i < m_urlList.size(); i++)
        {
            URL url = (URL) m_urlList.get(i);
            try
            {
                Repository repo = new RepositoryImpl(this, url, m_logger);
                if (repo != null)
                {
                    m_repoMap.put(url, repo);
                }
            }
            catch (Exception ex)
            {
                m_logger.log(
                    Logger.LOG_WARNING,
                    "RepositoryAdminImpl: Exception creating repository " + url.toExternalForm()
                        + ". Repository is skipped.",
                    ex);
            }
        }
    }
}