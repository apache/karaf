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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resource;
import org.osgi.service.url.AbstractURLStreamHandlerService;

/**
 * Simple {@link URLStreamHandler} which is able to handle
 * obr urls. The urls must be conform the following schema:
 *
 *  obr://<symbolicName>/<timeStamp>
 *
 * Example:
 *
 *  obr://org.apache.felix.javax.servlet/1240305961998
 *
 *
 * Update to the bundle is done
 *
 */
public class ObrURLStreamHandlerService extends AbstractURLStreamHandlerService
{
    /**
     * The BundleContext to search for the bundles.
     */
    private final BundleContext m_bundleContext;
    /**
     * The RepositoryAdmin to query for the actual url
     * for a bundle.
     */
    private final RepositoryAdmin m_reRepositoryAdmin;
    /**
     * Logger to use.
     */
    private final Logger m_logger;
    /**
     * The update strategy to use.
     * Default: newest
     */
    private String m_updateStrategy = "newest";
    /**
     * Property defining the obr update strategy
     */
    public static final String OBR_UPDATE_STRATEGY = "obr.update.strategy";

    /**
     * Constructor
     *
     * @param context context to use
     * @param admin admin to use
     */
    public ObrURLStreamHandlerService(BundleContext context, RepositoryAdmin admin)
    {
        m_bundleContext = context;
        m_reRepositoryAdmin = admin;
        m_logger = new Logger(context);
        if (m_bundleContext.getProperty(OBR_UPDATE_STRATEGY) != null)
        {
            this.m_updateStrategy = m_bundleContext.getProperty(OBR_UPDATE_STRATEGY);
        }
    }

    /**
     * {@inheritDoc}
     *
     * This implementation looks up the bundle with the given
     * url set as location String within the current {@link BundleContext}.
     * The real url for this bundle is determined afterwards via the
     * {@link RepositoryAdmin}.
     */
    public URLConnection openConnection(URL u) throws IOException
    {
        String url = u.toExternalForm();

        URL remoteURL = null;

        Bundle[] bundles = m_bundleContext.getBundles();

        int i = 0;
        while ((remoteURL == null) && (i < bundles.length))
        {
            if (url.equals(bundles[i].getLocation()))
            {
                remoteURL = getRemoteUrlForBundle(bundles[i]);
            }
            i++;
        }

        if (remoteURL == null)
        {
            throw new IOException("could not resolve obr url to remote url! " + u);
        }

        return remoteURL.openConnection();

    }

    /**
     * Determines the remote url for the given bundle according to
     * the configured {@link ResourceSelectionStrategy}.
     *
     * @param bundle bundle
     * @return remote url
     * @throws IOException if something went wrong
     */
    private URL getRemoteUrlForBundle(Bundle bundle) throws IOException
    {
        String bundleVersion =
            (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION);
        StringBuffer buffer = new StringBuffer();
        buffer.append("(symbolicname=");
        buffer.append(bundle.getSymbolicName());
        buffer.append(")");

        Resource[] discoverResources =
            m_reRepositoryAdmin.discoverResources(buffer.toString());

        ResourceSelectionStrategy strategy = getStrategy(m_updateStrategy);
        Resource selected = strategy.selectOne(
            Version.parseVersion(bundleVersion), discoverResources);

        return selected.getURL();
    }

    private ResourceSelectionStrategy getStrategy(String strategy)
    {
        m_logger.log(Logger.LOG_DEBUG, "Using ResourceSelectionStrategy: " + strategy);

        if ("same".equals(strategy))
        {
            return new SameSelectionStrategy(m_logger);
        }
        else if ("newest".equals(strategy))
        {
            return new NewestSelectionStrategy(m_logger);
        }

        throw new RuntimeException("Could not determine obr update strategy : " + strategy);
    }

    /**
     * Abstract class for Resource Selection Strategies
     */
    private static abstract class ResourceSelectionStrategy
    {
        private final Logger m_logger;

        ResourceSelectionStrategy(Logger logger)
        {
            m_logger = logger;
        }

        Logger getLogger()
        {
            return m_logger;
        }

        final Resource selectOne(Version currentVersion, Resource[] resources)
        {
            SortedMap sortedResources = new TreeMap();
            for (int i = 0; i < resources.length; i++)
            {
                sortedResources.put(resources[i].getVersion(), resources[i]);
            }

            Version versionToUse = determineVersion(currentVersion, sortedResources);

            m_logger.log(Logger.LOG_DEBUG,
                "Using Version " + versionToUse + " for bundle "
                + resources[0].getSymbolicName());

            return (Resource) sortedResources.get(versionToUse);
        }

        abstract Version determineVersion(Version currentVersion, SortedMap sortedResources);
    }

    /**
     * Strategy returning the current version.
     */
    static class SameSelectionStrategy extends ResourceSelectionStrategy
    {
        SameSelectionStrategy(Logger logger)
        {
            super(logger);
        }

        /**
         * {@inheritDoc}
         */
        Version determineVersion(Version currentVersion, SortedMap sortedResources)
        {
            return currentVersion;
        }
    }

    /**
     * Strategy returning the newest entry.
     */
    static class NewestSelectionStrategy extends ResourceSelectionStrategy
    {
        NewestSelectionStrategy(Logger logger)
        {
            super(logger);
        }

        /**
         * {@inheritDoc}
         */
        Version determineVersion(Version currentVersion, SortedMap sortedResources)
        {
            return (Version) sortedResources.lastKey();
        }
    }
}