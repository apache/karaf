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
package org.apache.felix.framework.cache;

import java.io.File;
import java.util.Map;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.resolver.Content;

/**
 * <p>
 * This class implements an abstract revision of a bundle archive. A revision
 * is an abstraction of a bundle's actual content and is associated with a
 * parent bundle archive. A bundle archive may have multiple revisions assocaited
 * with it at one time, since updating a bundle results in a new version of the
 * bundle's content until the bundle is refreshed. Upon a refresh, then old
 * revisions are then purged. This abstract class is the base class for all
 * concrete types of revisions, such as ones for a JAR file or directories. All
 * revisions are assigned a root directory into which all of their state should
 * be stored, if necessary. Clean up of this directory is the responsibility
 * of the parent bundle archive and not of the revision itself.
 * </p>
 * @see org.apache.felix.framework.cache.BundleCache
 * @see org.apache.felix.framework.cache.BundleArchive
**/
public abstract class BundleRevision
{
    private final Logger m_logger;
    private final Map m_configMap;
    private final File m_revisionRootDir;
    private final String m_location;

    /**
     * <p>
     * This class is abstract and cannot be created. It represents a revision
     * of a bundle, i.e., its content. A revision is associated with a particular
     * location string, which is typically in URL format. Subclasses of this
     * class provide particular functionality, such as a revision in the form
     * of a JAR file or a directory. Each revision subclass is expected to use
     * the root directory associated with the abstract revision instance to
     * store any state; this will ensure that resources used by the revision are
     * properly freed when the revision is no longer needed.
     * </p>
     * @param logger a logger for use by the revision.
     * @param revisionRootDir the root directory to be used by the revision
     *        subclass for storing any state.
     * @param location the location string associated with the revision.
     * @param trustedCaCerts the trusted CA certificates if any.
     * @throws Exception if any errors occur.
    **/
    public BundleRevision(Logger logger, Map configMap, File revisionRootDir, String location)
        throws Exception
    {
        m_logger = logger;
        m_configMap = configMap;
        m_revisionRootDir = revisionRootDir;
        m_location = location;
    }

    /**
     * <p>
     * Returns the logger for this revision.
     * </p>
     * @return the logger instance for this revision.
    **/
    public Logger getLogger()
    {
        return m_logger;
    }

    /**
     * <p>
     * Returns the configuration map for this revision.
     * </p>
     * @return the configuration map for this revision.
    **/
    public Map getConfig()
    {
        return m_configMap;
    }

    /**
     * <p>
     * Returns the root directory for this revision.
     * </p>
     * @return the root directory for this revision.
    **/
    public File getRevisionRootDir()
    {
        return m_revisionRootDir;
    }

    /**
     * <p>
     * Returns the location string this revision.
     * </p>
     * @return the location string for this revision.
    **/
    public String getLocation()
    {
        return m_location;
    }

    /**
     * <p>
     * Returns the main attributes of the JAR file manifest header of the
     * revision. The returned map is case insensitive.
     * </p>
     * @return the case-insensitive JAR file manifest header of the revision.
     * @throws java.lang.Exception if any error occurs.
    **/
    public abstract Map getManifestHeader() throws Exception;

    public abstract Content getContent() throws Exception;

    /**
     * <p>
     * This method is called when the revision is no longer needed. The directory
     * associated with the revision will automatically be removed for each
     * revision, so this method only needs to be concerned with other issues,
     * such as open files.
     * </p>
     * @throws Exception if any error occurs.
    **/
    protected abstract void close() throws Exception;
}