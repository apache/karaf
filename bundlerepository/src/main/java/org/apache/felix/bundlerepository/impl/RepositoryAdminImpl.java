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
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.utils.collections.MapToDictionary;
import org.apache.felix.utils.log.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;

public class RepositoryAdminImpl implements RepositoryAdmin
{
    private final BundleContext m_context;
    private final Logger m_logger;
    private final SystemRepositoryImpl m_system;
    private final LocalRepositoryImpl m_local;
    private final DataModelHelper m_helper = new DataModelHelperImpl();
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

    public DataModelHelper getHelper()
    {
        return m_helper;
    }

    public Repository getLocalRepository()
    {
        return m_local;
    }

    public Repository getSystemRepository()
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

    public synchronized RepositoryImpl addRepository(final URL url, int hopCount) throws Exception
    {
        initialize();

        // If the repository URL is a duplicate, then we will just
        // replace the existing repository object with a new one,
        // which is effectively the same as refreshing the repository.
        try
        {
            RepositoryImpl repository = (RepositoryImpl) AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws Exception
                {
                    return m_helper.repository(url);
                }
            });
            m_repoMap.put(url.toExternalForm(), repository);

            // resolve referrals
            hopCount--;
            if (hopCount > 0 && repository.getReferrals() != null)
            {
                for (int i = 0; i < repository.getReferrals().length; i++)
                {
                    Referral referral = repository.getReferrals()[i];

                    URL referralUrl = new URL(url, referral.getUrl());
                    hopCount = (referral.getDepth() > hopCount) ? hopCount : referral.getDepth();

                    addRepository(referralUrl, hopCount);
                }
            }

            return repository;
        }
        catch (PrivilegedActionException ex)
        {
            throw (Exception) ex.getCause();
        }

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

        Filter filter = filterExpr != null ? m_helper.filter(filterExpr) : null;
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
