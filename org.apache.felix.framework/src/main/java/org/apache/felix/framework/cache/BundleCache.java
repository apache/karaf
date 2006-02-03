/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.framework.cache;

import java.io.InputStream;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.PropertyResolver;

/**
 * <p>
 * This interface represents the storage mechanism that Felix uses for
 * caching bundles. It is possible for multiple implementations of
 * this interface to exist for different storage technologies, such as the
 * file system, memory, or a database. Felix includes a default implementation
 * of this interface that uses the file system. Felix allows you to specify
 * alternative implementations to use by specifying a class name via the
 * <tt>felix.cache.class</tt> system property. Bundle cache implemenations
 * should implement this interface and provide a default constructor.
 * </p>
 * @see org.apache.felix.framework.BundleArchive
**/
public interface BundleCache
{
    /**
     * <p>
     * This method is called before using the BundleCache implementation
     * to initialize it and to pass it a reference to its associated
     * configuration property resolver and logger. The <tt>BundleCache</tt>
     * implementation should not use <tt>System.getProperty()</tt> directly
     * for configuration properties, it should use the property resolver
     * instance passed into this method. The property resolver
     * provides access to properties passed into the Felix instance's
     * constructor. This approach allows multiple instances of Felix to
     * exist in memory at the same time, but for
     * them to be configured differently. For example, an application may
     * want two instances of Felix, where each instance stores their cache
     * in a different location in the file system. When using multiple
     * instances of Felix in memory at the same time, system properties
     * should be avoided and all properties should be passed in to Felix's
     * constructor.
     * </p>
     * @param cfg the property resolver for obtaining configuration properties.
     * @param logger the logger to use for reporting errors.
     * @throws Exception if any error occurs.
    **/
    public void initialize(PropertyResolver cfg, Logger logger)
        throws Exception;

    /**
     * <p>
     * Returns all cached bundle archives.
     * </p>
     * @return an array of all cached bundle archives.
     * @throws Exception if any error occurs.
    **/
    public BundleArchive[] getArchives()
        throws Exception;

    /**
     * <p>
     * Returns the bundle archive associated with the specified
     * bundle indentifier.
     * </p>
     * @param id the identifier of the bundle archive to retrieve.
     * @return the bundle archive assocaited with the specified bundle identifier.
     * @throws Exception if any error occurs.
    **/
    public BundleArchive getArchive(long id)
        throws Exception;

    /**
     * <p>
     * Creates a new bundle archive for the specified bundle
     * identifier using the supplied location string and input stream. The
     * contents of the bundle JAR file should be read from the supplied
     * input stream, which will not be <tt>null</tt>. The input stream is
     * closed by the caller; the implementation is only responsible for
     * closing streams it opens. If this method completes successfully, then
     * it means that the initial bundle revision of the specified bundle was
     * successfully cached.
     * </p>
     * @param id the identifier of the bundle associated with the new archive.
     * @param location the location of the bundle associated with the new archive.
     * @param is the input stream to the bundle's JAR file.
     * @return the created bundle archive.
     * @throws Exception if any error occurs.
    **/
    public BundleArchive create(long id, String location, InputStream is)
        throws Exception;

    /**
     * <p>
     * Saves an updated revision of the specified bundle to
     * the bundle cache using the supplied input stream. The contents of the
     * updated bundle JAR file should be read from the supplied input stream,
     * which will not be <tt>null</tt>. The input stream is closed by the
     * caller; the implementation is only responsible for closing streams
     * it opens. Updating a bundle in the cache does not replace the current
     * revision of the bundle, it makes a new revision available. If this
     * method completes successfully, then it means that the number of
     * revisions of the specified bundle has increased by one.
     * </p>
     * @param ba the bundle archive of the bundle to update.
     * @param is the input stream to the bundle's updated JAR file.
     * @throws Exception if any error occurs.
    **/
    public void update(BundleArchive ba, InputStream is)
        throws Exception;

    /**
     * <p>
     * Purges all old revisions of the specified bundle from
     * the cache. If this method completes successfully, then it means that
     * only the most current revision of the bundle should exist in the cache.
     * </p>
     * @param ba the bundle archive of the bundle to purge.
     * @throws Exception if any error occurs.
    **/
    public void purge(BundleArchive ba)
        throws Exception;

    /**
     * <p>
     * Removes the specified bundle from the cache. If this method
     * completes successfully, there should be no trace of the removed bundle
     * in the cache.
     * </p>
     * @param ba the bundle archive of the bundle to remove.
     * @throws Exception if any error occurs.
    **/
    public void remove(BundleArchive ba)
        throws Exception;
}