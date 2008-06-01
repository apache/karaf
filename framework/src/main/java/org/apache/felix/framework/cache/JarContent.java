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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.JarFileX;
import org.apache.felix.moduleloader.IContent;

public class JarContent implements IContent
{
    private static final int BUFSIZE = 4096;
    private static final transient String LEGACY_EMBEDDED_DIRECTORY = "embedded";
    private static final transient String EMBEDDED_DIRECTORY = "-embedded";
    private static final transient String LIBRARY_DIRECTORY = "lib";

    private final Logger m_logger;
    private final Object m_revisionLock;
    private final File m_rootDir;
    private final File m_file;
    private JarFileX m_jarFile = null;
    // TODO: CACHE - It would be nice to eventually remove this legacy flag.
    private final boolean m_legacy;

    public JarContent(Logger logger, Object revisionLock, File rootDir, File file)
    {
        m_logger = logger;
        m_revisionLock = revisionLock;
        m_rootDir = rootDir;
        m_file = file;
        m_legacy = false;
    }

    // This is only used by JarRevision.
    public JarContent(Logger logger, Object revisionLock, File rootDir, File file, boolean legacy)
    {
        m_logger = logger;
        m_revisionLock = revisionLock;
        m_rootDir = rootDir;
        m_file = file;
        m_legacy = legacy;
    }

    protected void finalize()
    {
        if (m_jarFile != null)
        {
            try
            {
                m_jarFile.close();
            }
            catch (IOException ex)
            {
                // Not much we can do, so ignore it.
            }
        }
    }

    public synchronized void close()
    {
        try
        {
            if (m_jarFile != null)
            {
                m_jarFile.close();
            }
        }
        catch (Exception ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                "JarContent: Unable to open JAR file.", ex);
        }

        m_jarFile = null;
    }

    public synchronized boolean hasEntry(String name) throws IllegalStateException
    {
        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "JarContent: Unable to open JAR file.", ex);
                return false;
            }
        }

        try
        {
            ZipEntry ze = m_jarFile.getEntry(name);
            return ze != null;
        }
        catch (Exception ex)
        {
            return false;
        }
        finally
        {
        }
    }

    public synchronized Enumeration getEntries()
    {
        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "JarContent: Unable to open JAR file.", ex);
                return null;
            }
        }

        // Wrap entries enumeration to filter non-matching entries.
        Enumeration e = new EntriesEnumeration(m_jarFile.entries());

        // Spec says to return null if there are no entries.
        return (e.hasMoreElements()) ? e : null;
    }

    public synchronized byte[] getEntryAsBytes(String name) throws IllegalStateException
    {
        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "JarContent: Unable to open JAR file.", ex);
                return null;
            }
        }

        // Get the embedded resource.
        InputStream is = null;
        ByteArrayOutputStream baos = null;

        try
        {
            ZipEntry ze = m_jarFile.getEntry(name);
            if (ze == null)
            {
                return null;
            }
            is = m_jarFile.getInputStream(ze);
            if (is == null)
            {
                return null;
            }
            baos = new ByteArrayOutputStream(BUFSIZE);
            byte[] buf = new byte[BUFSIZE];
            int n = 0;
            while ((n = is.read(buf, 0, buf.length)) >= 0)
            {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();

        }
        catch (Exception ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                "JarContent: Unable to read bytes.", ex);
            return null;
        }
        finally
        {
            try
            {
                if (baos != null) baos.close();
            }
            catch (Exception ex)
            {
            }
            try
            {
                if (is != null) is.close();
            }
            catch (Exception ex)
            {
            }
        }
    }

    public synchronized InputStream getEntryAsStream(String name)
        throws IllegalStateException, IOException
    {
        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "JarContent: Unable to open JAR file.", ex);
                return null;
            }
        }

        // Get the embedded resource.
        InputStream is = null;

        try
        {
            ZipEntry ze = m_jarFile.getEntry(name);
            if (ze == null)
            {
                return null;
            }
            is = m_jarFile.getInputStream(ze);
            if (is == null)
            {
                return null;
            }
        }
        catch (Exception ex)
        {
            return null;
        }

        return is;
    }

    public synchronized IContent getEntryAsContent(String entryName)
    {
        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "JarContent: Unable to open JAR file.", ex);
                return null;
            }

        }

        // If the entry name refers to the content itself, then
        // just return it immediately.
        if (entryName.equals(FelixConstants.CLASS_PATH_DOT))
        {
            return new JarContent(m_logger, m_revisionLock, m_rootDir, m_file, m_legacy);
        }

        // Remove any leading slash.
        entryName = (entryName.startsWith("/")) ? entryName.substring(1) : entryName;

        // Any embedded JAR files will be extracted to the embedded directory.
        // Since embedded JAR file names may clash when extracting from multiple
        // embedded JAR files, the embedded directory is per embedded JAR file.
        // For backwards compatibility purposes, don't use the file cache name
        // for the root bundle JAR file.
        File embedDir;
        if (m_legacy)
        {
            embedDir = new File(m_rootDir, LEGACY_EMBEDDED_DIRECTORY);
        }
        else
        {
            embedDir = new File(m_rootDir, m_file.getName() + EMBEDDED_DIRECTORY);
        }

        // Find the entry in the JAR file and create the
        // appropriate content type for it.

        // Determine if the entry is an emdedded JAR file or
        // directory in the bundle JAR file. Ignore any entries
        // that do not exist per the spec.
        ZipEntry ze = m_jarFile.getEntry(entryName);
        if ((ze != null) && ze.isDirectory())
        {
            File extractedDir = new File(embedDir, entryName);

            // Extracting an embedded directory file impacts all other existing
            // contents for this revision, so we have to grab the revision
            // lock first before trying to create a directory for an embedded
            // directory to avoid a race condition.
            synchronized (m_revisionLock)
            {
                if (!BundleCache.getSecureAction().fileExists(extractedDir))
                {
                    if (!BundleCache.getSecureAction().mkdirs(extractedDir))
                    {
                        m_logger.log(
                            Logger.LOG_ERROR,
                            "Unable to extract embedded directory.");
                    }
                }
            }
            return new ContentDirectoryContent(this, entryName);
        }
        else if ((ze != null) && ze.getName().endsWith(".jar"))
        {
            File extractedJar = new File(embedDir, entryName);

            // Extracting the embedded JAR file impacts all other existing
            // contents for this revision, so we have to grab the revision
            // lock first before trying to extract the embedded JAR file
            // to avoid a race condition.
            synchronized (m_revisionLock)
            {
                if (!BundleCache.getSecureAction().fileExists(extractedJar))
                {
                    try
                    {
                        extractEmbeddedJar(entryName);
                    }
                    catch (Exception ex)
                    {
                        m_logger.log(
                            Logger.LOG_ERROR,
                            "Unable to extract embedded JAR file.", ex);
                    }
                }
            }
            return new JarContent(
                m_logger, m_revisionLock, extractedJar.getParentFile(), extractedJar);
        }

        // The entry could not be found, so return null.
        return null;
    }

// TODO: This will need to consider security.
    public synchronized String getEntryAsNativeLibrary(String name)
    {
        // Open JAR file if not already opened.
        if (m_jarFile == null)
        {
            try
            {
                openJarFile();
            }
            catch (IOException ex)
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "JarContent: Unable to open JAR file.", ex);
                return null;
            }
        }

        // Get bundle lib directory.
        File libDir = new File(m_rootDir, LIBRARY_DIRECTORY);
        // Get lib file.
        File libFile = new File(libDir, File.separatorChar + name);
        // Make sure that the library's parent directory exists;
        // it may be in a sub-directory.
        libDir = libFile.getParentFile();
        if (!BundleCache.getSecureAction().fileExists(libDir))
        {
            if (!BundleCache.getSecureAction().mkdirs(libDir))
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "JarContent: Unable to create library directory.");
                return null;
            }
        }
        // Extract the library from the JAR file if it does not
        // already exist.
        if (!BundleCache.getSecureAction().fileExists(libFile))
        {
            InputStream is = null;

            try
            {
                ZipEntry ze = m_jarFile.getEntry(name);
                if (ze == null)
                {
                    return null;
                }
                is = new BufferedInputStream(
                    m_jarFile.getInputStream(ze), BundleCache.BUFSIZE);
                if (is == null)
                {
                    throw new IOException("No input stream: " + name);
                }

                // Create the file.
                BundleCache.copyStreamToFile(is, libFile);
            }
            catch (Exception ex)
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "JarContent: Extracting native library.", ex);
            }
            finally
            {
                try
                {
                    if (is != null) is.close();
                }
                catch (IOException ex)
                {
                    // Not much we can do.
                }
            }
        }

        return BundleCache.getSecureAction().getAbsolutePath(libFile);
    }

    public String toString()
    {
        return "JAR " + m_file.getPath();
    }

    public synchronized File getFile()
    {
        return m_file;
    }

    private void openJarFile() throws IOException
    {
        if (m_jarFile == null)
        {
            m_jarFile = BundleCache.getSecureAction().openJAR(m_file);
        }
    }

    /**
     * This method extracts an embedded JAR file from the bundle's
     * JAR file.
     * @param id the identifier of the bundle that owns the embedded JAR file.
     * @param jarPath the path to the embedded JAR file inside the bundle JAR file.
    **/
    private void extractEmbeddedJar(String jarPath)
        throws Exception
    {
        // Remove leading slash if present.
        jarPath = (jarPath.length() > 0) && (jarPath.charAt(0) == '/')
            ? jarPath.substring(1) : jarPath;

        // Any embedded JAR files will be extracted to the embedded directory.
        // Since embedded JAR file names may clash when extracting from multiple
        // embedded JAR files, the embedded directory is per embedded JAR file.
        // For backwards compatibility purposes, don't use the file cache name
        // for the root bundle JAR file.
        File embedDir;
        if (m_legacy)
        {
            embedDir = new File(m_rootDir, LEGACY_EMBEDDED_DIRECTORY);
        }
        else
        {
            embedDir = new File(m_rootDir, m_file.getName() + EMBEDDED_DIRECTORY);
        }
        File jarFile = new File(embedDir, jarPath);

        if (!BundleCache.getSecureAction().fileExists(jarFile))
        {
            InputStream is = null;
            try
            {
                // Make sure class path entry is a JAR file.
                ZipEntry ze = m_jarFile.getEntry(jarPath);
                if (ze == null)
                {
                    return;
                }
                // If the zip entry is a directory, then ignore it since
                // we don't need to extact it; otherwise, it points to an
                // embedded JAR file, so extract it.
                else if (!ze.isDirectory())
                {
                    // Make sure that the embedded JAR's parent directory exists;
                    // it may be in a sub-directory.
                    File jarDir = jarFile.getParentFile();
                    if (!BundleCache.getSecureAction().fileExists(jarDir))
                    {
                        if (!BundleCache.getSecureAction().mkdirs(jarDir))
                        {
                            throw new IOException("Unable to create embedded JAR directory.");
                        }
                    }

                    // Extract embedded JAR into its directory.
                    is = new BufferedInputStream(m_jarFile.getInputStream(ze), BundleCache.BUFSIZE);
                    if (is == null)
                    {
                        throw new IOException("No input stream: " + jarPath);
                    }
                    // Copy the file.
                    BundleCache.copyStreamToFile(is, jarFile);
                }
            }
            finally
            {
                if (is != null) is.close();
            }
        }
    }

    private static class EntriesEnumeration implements Enumeration
    {
        private Enumeration m_enumeration = null;

        public EntriesEnumeration(Enumeration enumeration)
        {
            m_enumeration = enumeration;
        }

        public boolean hasMoreElements()
        {
            return m_enumeration.hasMoreElements();
        }

        public Object nextElement()
        {
            return ((ZipEntry) m_enumeration.nextElement()).getName();
        }
    }
}