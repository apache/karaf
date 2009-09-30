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

import java.io.*;
import java.util.*;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.SecureAction;
import org.osgi.framework.Constants;

/**
 * <p>
 * This class, combined with <tt>BundleArchive</tt>, and concrete
 * <tt>BundleRevision</tt> subclasses, implement the Felix bundle cache.
 * It is possible to configure the default behavior of this class by
 * passing properties into Felix' constructor. The configuration properties
 * for this class are:
 * </p>
 * <ul>
 *   <li><tt>felix.cache.bufsize</tt> - Sets the buffer size to be used by
 *       the cache; the default value is 4096. The integer
 *       value of this string provides control over the size of the
 *       internal buffer of the disk cache for performance reasons.
 *   </li>
 *   <li><tt>felix.cache.dir</tt> - Sets the directory to be used by the
 *       cache as its cache directory. The cache directory is where all
 *       profile directories are stored and a profile directory is where a
 *       set of installed bundles are stored. By default, the cache
 *       directory is <tt>.felix</tt> in the user's home directory. If
 *       this property is specified, then its value will be used as the cache
 *       directory instead of <tt>.felix</tt>. This directory will be created
 *       if it does not exist.
 *   </li>
 *   <li><tt>felix.cache.profile</tt> - Sets the profile name that will be
 *       used to create a profile directory inside of the cache directory.
 *       The created directory will contained all installed bundles associated
 *       with the profile.
 *   </li>
 *   <li><tt>felix.cache.profiledir</tt> - Sets the directory to use as the
 *       profile directory for the bundle cache; by default the profile
 *       name is used to create a directory in the <tt>.felix</tt> cache
 *       directory. If this property is specified, then the cache directory
 *       and profile name properties are ignored. The specified value of this
 *       property is used directly as the directory to contain all cached
 *       bundles. If this property is set, it is not necessary to set the
 *       cache directory or profile name properties. This directory will be
 *       created if it does not exist.
 *   </li>
 * </ul>
 * <p>
 * For specific information on how to configure Felix using system properties,
 * refer to the Felix usage documentation.
 * </p>
 * @see org.apache.felix.framework.util.BundleArchive
**/
public class BundleCache
{
    public static final String CACHE_BUFSIZE_PROP = "felix.cache.bufsize";
    public static final String CACHE_ROOTDIR_PROP = "felix.cache.rootdir";

    protected static transient int BUFSIZE = 4096;
    protected static transient final String CACHE_DIR_NAME = "felix-cache";
    protected static transient final String CACHE_ROOTDIR_DEFAULT = ".";
    protected static transient final String BUNDLE_DIR_PREFIX = "bundle";

    private static final SecureAction m_secureAction = new SecureAction();

    private final Logger m_logger;
    private final Map m_configMap;

    public BundleCache(Logger logger, Map configMap)
    {
        m_logger = logger;
        m_configMap = configMap;
    }

    /* package */ static SecureAction getSecureAction()
    {
        return m_secureAction;
    }

    public synchronized void delete() throws Exception
    {
        // Delete the cache directory.
        File cacheDir = determineCacheDir(m_configMap);
        deleteDirectoryTree(cacheDir);
    }

    public BundleArchive[] getArchives()
        throws Exception
    {
        // Get buffer size value.
        try
        {
            String sBufSize = (String) m_configMap.get(CACHE_BUFSIZE_PROP);
            if (sBufSize != null)
            {
                BUFSIZE = Integer.parseInt(sBufSize);
            }
        }
        catch (NumberFormatException ne)
        {
            // Use the default value.
        }

        // Create the cache directory, if it does not exist.
        File cacheDir = determineCacheDir(m_configMap);
        if (!getSecureAction().fileExists(cacheDir))
        {
            if (!getSecureAction().mkdirs(cacheDir))
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "Unable to create cache directory: " + cacheDir);
                throw new RuntimeException("Unable to create cache directory.");
            }
        }

        // Create the existing bundle archives in the directory, if any exist.
        List archiveList = new ArrayList();
        File[] children = getSecureAction().listDirectory(cacheDir);
        for (int i = 0; (children != null) && (i < children.length); i++)
        {
            // Ignore directories that aren't bundle directories or
            // is the system bundle directory.
            if (children[i].getName().startsWith(BUNDLE_DIR_PREFIX) &&
                !children[i].getName().equals(BUNDLE_DIR_PREFIX + Long.toString(0)))
            {
                // Recreate the bundle archive.
                try
                {
                    archiveList.add(new BundleArchive(m_logger, m_configMap, children[i]));
                }
                catch (Exception ex)
                {
                    // Log and ignore.
                    m_logger.log(Logger.LOG_ERROR,
                        "Error creating archive.", ex);
                }
            }
        }

        return (BundleArchive[])
            archiveList.toArray(new BundleArchive[archiveList.size()]);
    }

    public BundleArchive create(long id, String location, InputStream is)
        throws Exception
    {
        File cacheDir = determineCacheDir(m_configMap);

        // Construct archive root directory.
        File archiveRootDir =
            new File(cacheDir, BUNDLE_DIR_PREFIX + Long.toString(id));

        try
        {
            // Create the archive and add it to the list of archives.
            BundleArchive ba =
                new BundleArchive(m_logger, m_configMap, archiveRootDir, id, location, is);
            return ba;
        }
        catch (Exception ex)
        {
            if (m_secureAction.fileExists(archiveRootDir))
            {
                if (!BundleCache.deleteDirectoryTree(archiveRootDir))
                {
                    m_logger.log(
                        Logger.LOG_ERROR,
                        "Unable to delete the archive directory: "
                            + archiveRootDir);
                }
            }
            throw ex;
        }
    }

    /**
     * Provides the system bundle access to its private storage area; this
     * special case is necessary since the system bundle is not really a
     * bundle and therefore must be treated in a special way.
     * @param fileName the name of the file in the system bundle's private area.
     * @return a <tt>File</tt> object corresponding to the specified file name.
     * @throws Exception if any error occurs.
    **/
    public File getSystemBundleDataFile(String fileName)
        throws Exception
    {
        // Make sure system bundle directory exists.
        File sbDir = new File(determineCacheDir(m_configMap), BUNDLE_DIR_PREFIX + Long.toString(0));

        // If the system bundle directory exists, then we don't
        // need to initialize since it has already been done.
        if (!getSecureAction().fileExists(sbDir))
        {
            // Create system bundle directory, if it does not exist.
            if (!getSecureAction().mkdirs(sbDir))
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "Unable to create system bundle directory.");
                throw new IOException("Unable to create system bundle directory.");
            }
        }

        // Do some sanity checking.
        if ((fileName.length() > 0) && (fileName.charAt(0) == File.separatorChar))
            throw new IllegalArgumentException("The data file path must be relative, not absolute.");
        else if (fileName.indexOf("..") >= 0)
            throw new IllegalArgumentException("The data file path cannot contain a reference to the \"..\" directory.");

        // Return the data file.
        return new File(sbDir, fileName);
    }

    //
    // Static file-related utility methods.
    //

    /**
     * This method copies an input stream to the specified file.
     * @param is the input stream to copy.
     * @param outputFile the file to which the input stream should be copied.
    **/
    static void copyStreamToFile(InputStream is, File outputFile)
        throws IOException
    {
        OutputStream os = null;

        try
        {
            os = getSecureAction().getFileOutputStream(outputFile);
            os = new BufferedOutputStream(os, BUFSIZE);
            byte[] b = new byte[BUFSIZE];
            int len = 0;
            while ((len = is.read(b)) != -1)
            {
                os.write(b, 0, len);
            }
        }
        finally
        {
            if (is != null) is.close();
            if (os != null) os.close();
        }
    }

    static boolean deleteDirectoryTree(File target)
    {
        if (!deleteDirectoryTreeRecursive(target))
        {
            // We might be talking windows and native libs -- hence,
            // try to trigger a gc and try again. The hope is that
            // this releases the classloader that loaded the native
            // lib and allows us to delete it because it then 
            // would not be used anymore. 
            System.gc();
            System.gc();
            return deleteDirectoryTreeRecursive(target);
        }
        return true;
    }

    //
    // Private methods.
    //

    private static File determineCacheDir(Map configMap)
    {
        File cacheDir;

        // Check to see if the cache directory is specified in the storage
        // configuration property.
        String cacheDirStr = (String) configMap.get(Constants.FRAMEWORK_STORAGE);
        // Get the cache root directory for relative paths; the default is ".".
        String rootDirStr = (String) configMap.get(CACHE_ROOTDIR_PROP);
        rootDirStr = (rootDirStr == null) ? CACHE_ROOTDIR_DEFAULT : rootDirStr;
        if (cacheDirStr != null)
        {
            // If the specified cache directory is relative, then use the
            // root directory to calculate the absolute path.
            cacheDir = new File(cacheDirStr);
            if (!cacheDir.isAbsolute())
            {
                cacheDir = new File(rootDirStr, cacheDirStr);
            }
        }
        else
        {
            // If no cache directory was specified, then use the default name
            // in the root directory.
            cacheDir = new File(rootDirStr, CACHE_DIR_NAME);
        }

        return cacheDir;
    }

    private static boolean deleteDirectoryTreeRecursive(File target)
    {
    	if (!getSecureAction().fileExists(target))
        {
            return true;
        }

        if (getSecureAction().isFileDirectory(target))
        {
            File[] files = getSecureAction().listDirectory(target);
            for (int i = 0; i < files.length; i++)
            {
                deleteDirectoryTreeRecursive(files[i]);
            }
        }

        return getSecureAction().deleteFile(target);
    }
}