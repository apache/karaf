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
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.obr.*;

public class RepositoryAdminImpl implements RepositoryAdmin
{
    static BundleContext m_context = null;
    private List m_urlList = new ArrayList();
    private Map m_repoMap = new HashMap();
    private boolean m_initialized = false;

    // Reusable comparator for sorting resources by name.
    private Comparator m_nameComparator = new ResourceComparator();

    private static final String DEFAULT_REPOSITORY_URL =
        "http://oscar-osgi.sf.net/obr2/repository.xml";
    public static final String REPOSITORY_URL_PROP = "obr.repository.url";
    public static final String EXTERN_REPOSITORY_TAG = "extern-repositories";

    public RepositoryAdminImpl(BundleContext context)
    {
        m_context = context;

        // Get repository URLs.
        String urlStr = m_context.getProperty(REPOSITORY_URL_PROP);
        if (urlStr != null)
        {
            StringTokenizer st = new StringTokenizer(urlStr);
            if (st.countTokens() > 0)
            {
                while (st.hasMoreTokens())
                {
                    try
                    {
                        m_urlList.add(new URL(st.nextToken()));
                    }
                    catch (MalformedURLException ex)
                    {
                        System.err.println("RepositoryAdminImpl: " + ex);
                    }
                }
            }
        }

        // Use the default URL if none were specified.
        if (m_urlList.size() == 0)
        {
            try
            {
                m_urlList.add(new URL(DEFAULT_REPOSITORY_URL));
            }
            catch (MalformedURLException ex)
            {
                System.err.println("RepositoryAdminImpl: " + ex);
            }
        }
    }

    public synchronized Repository addRepository(URL url) throws Exception
    {
        if (!m_urlList.contains(url))
        {
            m_urlList.add(url);
        }

        // If the repository URL is a duplicate, then we will just
        // replace the existing repository object with a new one,
        // which is effectively the same as refreshing the repository.
        Repository repo = new RepositoryImpl(url);
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

        return new ResolverImpl(m_context, this);
    }

    public synchronized Resource[] discoverResources(String filterExpr)
    {
        if (!m_initialized)
        {
            initialize();
        }

        Filter filter =  null;
        try
        {
            filter = m_context.createFilter(filterExpr);
        }
        catch (InvalidSyntaxException ex)
        {
            System.err.println(ex);
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
        m_repoMap.clear();

        for (int i = 0; i < m_urlList.size(); i++)
        {
            URL url = (URL) m_urlList.get(i);
            try
            {
                Repository repo = new RepositoryImpl(url);
                if (repo != null)
                {
                    m_repoMap.put(url, repo);
                }
            }
            catch (Exception ex)
            {
                System.err.println(
                    "RepositoryAdminImpl: Exception creating repository - " + ex);
                System.err.println(
                    "RepositoryAdminImpl: Ignoring repository " + url);
            }
        }
    }
}