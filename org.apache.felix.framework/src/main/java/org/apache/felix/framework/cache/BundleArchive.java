/*
 *   Copyright 2006 The Apache Software Foundation
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

import java.io.File;
import java.util.Map;

import org.apache.felix.moduleloader.IContent;
import org.apache.felix.moduleloader.IContentLoader;
import org.osgi.framework.BundleActivator;

/**
 * <p>
 * This interface represents an individual cached bundle in the
 * bundle cache. Felix uses this interface to access all information
 * about the associated bundle's cached information. Classes that implement
 * this interface will be related to a specific implementation of the
 * <tt>BundleCache</tt> interface.
 * </p>
 * @see org.apache.felix.framework.BundleCache
**/
public interface BundleArchive
{
    /**
     * <p>
     * Returns the identifier of the bundle associated with this archive.
     * </p>
     * @return the identifier of the bundle associated with this archive.
    **/
    public long getId();
    
    /**
     * <p>
     * Returns the location string of the bundle associated with this archive.
     * </p>
     * @return the location string of the bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public String getLocation()
        throws Exception;

    /**
     * <p>
     * Returns the persistent state of the bundle associated with the archive;
     * this value will be either <tt>Bundle.INSTALLED</tt> or <tt>Bundle.ACTIVE</tt>.
     * </p>
     * @return the persistent state of the bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public int getPersistentState()
        throws Exception;

    /**
     * <p>
     * Sets the persistent state of the bundle associated with this archive;
     * this value will be either <tt>Bundle.INSTALLED</tt> or <tt>Bundle.ACTIVE</tt>.
     * </p>
     * @param state the new bundle state to write to the archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public void setPersistentState(int state)
        throws Exception;

    /**
     * <p>
     * Returns the start level of the bundle associated with this archive.
     * </p>
     * @return the start level of the bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public int getStartLevel()
        throws Exception;

    /**
     * <p>
     * Sets the start level of the bundle associated with this archive.
     * </p>
     * @param level the new bundle start level to write to the archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public void setStartLevel(int level)
        throws Exception;

    /**
     * <p>
     * Returns an appropriate data file for the bundle associated with the
     * archive using the supplied file name.
     * </p>
     * @return a <tt>File</tt> corresponding to the requested data file for
     *         the bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public File getDataFile(String fileName)
        throws Exception;

    /**
     * <p>
     * Returns the persistent bundle activator of the bundle associated with
     * this archive; this is a non-standard OSGi method that is only called
     * when Felix is running in non-strict OSGi mode.
     * </p>
     * @param loader the class loader to use when trying to instantiate
     *        the bundle activator.
     * @return the persistent bundle activator of the bundle associated with
     *         this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public BundleActivator getActivator(IContentLoader contentLoader)
        throws Exception;

    /**
     * <p>
     * Sets the persistent bundle activator of the bundle associated with
     * this archive; this is a non-standard OSGi method that is only called
     * when Felix is running in non-strict OSGi mode.
     * </p>
     * @param obj the new persistent bundle activator to write to the archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public void setActivator(Object obj)
        throws Exception;

    /**
     * <p>
     * Returns the number of revisions of the bundle associated with the
     * archive. When a bundle is updated, the previous version of the bundle
     * is maintained along with the new revision until old revisions are
     * purged. The revision count reflects how many revisions of the bundle
     * are currently available in the cache.
     * </p>
     * @return the number of revisions of the bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public int getRevisionCount()
        throws Exception;

    /**
     * <p>
     * Returns the main attributes of the JAR file manifest header of the
     * specified revision of the bundle associated with this archive. The
     * returned map should be case insensitive.
     * </p>
     * @param revision the specified revision.
     * @return the case-insensitive JAR file manifest header of the specified
     *         revision of the bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public Map getManifestHeader(int revision)
        throws Exception;

    /**
     * <p>
     * Returns a content object that is associated with the specified bundle
     * revision's JAR file.
     * </p>
     * @param revision the specified revision.
     * @return A content object for the specified bundle revision's JAR file.
     * @throws java.lang.Exception if any error occurs.
    **/
    public IContent getContent(int revision)
        throws Exception;

    /**
     * <p>
     * Returns an array of content objects that are associated with the
     * specified bundle revision's class path.
     * </p>
     * @param revision the specified revision.
     * @return An array of content objects for the specified bundle revision's
     *         class path.
     * @throws java.lang.Exception if any error occurs.
    **/
    public IContent[] getContentPath(int revision)
        throws Exception;

    /**
     * <p>
     * Returns the absolute file path for the specified native library of the
     * specified revision of the bundle associated with this archive.
     * </p>
     * @param revision the specified revision.
     * @param libName the name of the library.
     * @return a <tt>String</tt> that contains the absolute path name to
     *         the requested native library of the specified revision of the
     *         bundle associated with this archive.
     * @throws java.lang.Exception if any error occurs.
    **/
    public String findLibrary(int revision, String libName)
        throws Exception;
}