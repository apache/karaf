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
package org.apache.felix.bundlerepository.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;

public class RepositoryAdminImpl implements RepositoryAdmin
{
    static BundleContext m_context = null;
    private final Logger m_logger;
    private final SystemRepositoryImpl m_system;
    private final LocalRepositoryImpl m_local;
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
        m_system = new SystemRepositoryImpl(context, logger);
        m_local = new LocalRepositoryImpl(context, logger);
    }

    public org.apache.felix.bundlerepository.Repository getLocalRepository()
    {
        return m_local;
    }

    public org.apache.felix.bundlerepository.Repository getSystemRepository()
    {
        return m_system;
    }

    public void dispose()
    {
        m_local.dispose();
    }

    public Repository addRepository(String uri) throws Exception
    {
        return addRepository(new URL(uri));
    }

    public Repository addRepository(URL url) throws Exception
    {
        return addRepository(url, Integer.MAX_VALUE);
    }

    public synchronized RepositoryImpl addRepository(URL url, int hopCount) throws Exception
    {
        initialize();

        // If the repository URL is a duplicate, then we will just
        // replace the existing repository object with a new one,
        // which is effectively the same as refreshing the repository.
        RepositoryImpl repo = new RepositoryImpl(this, url, hopCount, m_logger);
        m_repoMap.put(url.toExternalForm(), repo);
        return repo;
    }

    public synchronized boolean removeRepository(String uri)
    {
        initialize();

        return m_repoMap.remove(uri) != null;
    }

    public synchronized Repository[] listRepositories()
    {
        initialize();

        return (Repository[]) m_repoMap.values().toArray(new Repository[m_repoMap.size()]);
    }

    public synchronized Resolver resolver()
    {
        initialize();

        List repositories = new ArrayList();
        repositories.add(m_system);
        repositories.add(m_local);
        repositories.addAll(m_repoMap.values());
        return resolver((Repository[]) repositories.toArray(new Repository[repositories.size()]));
    }

    public synchronized Resolver resolver(Repository[] repositories)
    {
        initialize();

        if (repositories == null)
        {
            return resolver();
        }
        return new ResolverImpl(m_context, repositories, m_logger);
    }

    public synchronized Resource[] discoverResources(String filterExpr) throws InvalidSyntaxException
    {
        initialize();

        Filter filter = filterExpr != null ? filter(filterExpr) : null;
        Resource[] resources;
        MapToDictionary dict = new MapToDictionary(null);
        Repository[] repos = listRepositories();
        List matchList = new ArrayList();
        for (int repoIdx = 0; (repos != null) && (repoIdx < repos.length); repoIdx++)
        {
            resources = repos[repoIdx].getResources();
            for (int resIdx = 0; (resources != null) && (resIdx < resources.length); resIdx++)
            {
                dict.setSourceMap(resources[resIdx].getProperties());
                if (filter == null || filter.match(dict))
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

    public synchronized Resource[] discoverResources(Requirement[] requirements)
    {
        initialize();

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
                boolean match = true;
                for (int reqIdx = 0; (requirements != null) && (reqIdx < requirements.length); reqIdx++)
                {
                    boolean reqMatch = false;
                    Capability[] caps = resources[resIdx].getCapabilities();
                    for (int capIdx = 0; (caps != null) && (capIdx < caps.length); capIdx++)
                    {
                        if (requirements[reqIdx].isSatisfied(caps[capIdx]))
                        {
                            reqMatch = true;
                            break;
                        }
                    }
                    match &= reqMatch;
                    if (!match)
                    {
                        break;
                    }
                }
                if (match)
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

    public Requirement requirement(String name, String filter) throws InvalidSyntaxException
    {
        RequirementImpl req = new RequirementImpl();
        req.setName(name);
        if (filter != null)
        {
            req.setFilter(filter);
        }
        return req;
    }

    public Filter filter(String filter) throws InvalidSyntaxException
    {
        return FilterImpl.newInstance(filter);
    }

    public Repository repository(URL url) throws Exception
    {
        return new RepositoryImpl(null, url, 0, m_logger);
    }

    public Repository repository(Resource[] resources) 
    {
        return new RepositoryImpl(resources);
    }

    public Capability capability(String name, Map properties)
    {
        CapabilityImpl cap = new CapabilityImpl();
        cap.setName(name);
        for (Iterator it = properties.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry e = (Map.Entry) it.next();
            cap.addP((String) e.getKey(), e.getValue());
        }
        return cap;
    }

    private void initialize()
    {
        if (m_initialized)
        {
            return;
        }
        m_initialized = true;

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
                        addRepository(token);
                    }
                    catch (Exception ex)
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
}