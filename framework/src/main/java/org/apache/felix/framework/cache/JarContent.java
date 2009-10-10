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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.JarFileX;
import org.apache.felix.framework.util.Util;
import org.apache.felix.moduleloader.IContent;
import org.osgi.framework.Constants;

public class JarContent implements IContent
{
    private static final int BUFSIZE = 4096;
    private static final transient String EMBEDDED_DIRECTORY = "-embedded";
    private static final transient String LIBRARY_DIRECTORY = "-lib";

    private final Logger m_logger;
    private final Map m_configMap;
    private final Object m_revisionLock;
    private final File m_rootDir;
    private final File m_file;
    private final JarFileX m_jarFile;
    private final boolean m_isJarFileOwner;
    private Map m_nativeLibMap;

    public JarContent(Logger logger, Map configMap, Object revisionLock, File rootDir,
        File file, JarFileX jarFile)
    {
        m_logger = logger;
        m_configMap = configMap;
        m_revisionLock = revisionLock;
        m_rootDir = rootDir;
        m_file = file;
        m_jarFile = (jarFile == null) ? openJarFile(m_file) : jarFile;
        m_isJarFileOwner = (jarFile == null);
    }

    protected void finalize()
    {
        close();
    }

    public void close()
    {
        try
        {
            if (m_isJarFileOwner)
            {
                m_jarFile.close();
            }
        }
        catch (Exception ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                "JarContent: Unable to close JAR file.", ex);
        }
    }

    public boolean hasEntry(String name) throws IllegalStateException
    {
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

    public Enumeration getEntries()
    {
        // Wrap entries enumeration to filter non-matching entries.
        Enumeration e = new EntriesEnumeration(m_jarFile.entries());

        // Spec says to return null if there are no entries.
        return (e.hasMoreElements()) ? e : null;
    }

    public byte[] getEntryAsBytes(String name) throws IllegalStateException
    {
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

    public InputStream getEntryAsStream(String name)
        throws IllegalStateException, IOException
    {
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

    public IContent getEntryAsContent(String entryName)
    {
        // If the entry name refers to the content itself, then
        // just return it immediately.
        if (entryName.equals(FelixConstants.CLASS_PATH_DOT))
        {
            return new JarContent(m_logger, m_configMap, m_revisionLock,
                m_rootDir, m_file, m_jarFile);
        }

        // Remove any leading slash.
        entryName = (entryName.startsWith("/")) ? entryName.substring(1) : entryName;

        // Any embedded JAR files will be extracted to the embedded directory.
        // Since embedded JAR file names may clash when extracting from multiple
        // embedded JAR files, the embedded directory is per embedded JAR file.
        File embedDir = new File(m_rootDir, m_file.getName() + EMBEDDED_DIRECTORY);

        // Find the entry in the JAR file and create the
        // appropriate content type for it.

        // Determine if the entry is an emdedded JAR file or
        // directory in the bundle JAR file. Ignore any entries
        // that do not exist per the spec.
        ZipEntry ze = m_jarFile.getEntry(entryName);
        if ((ze != null) && ze.isDirectory())
        {
            File extractDir = new File(embedDir, entryName);

            // Extracting an embedded directory file impacts all other existing
            // contents for this revision, so we have to grab the revision
            // lock first before trying to create a directory for an embedded
            // directory to avoid a race condition.
            synchronized (m_revisionLock)
            {
                if (!BundleCache.getSecureAction().fileExists(extractDir))
                {
                    if (!BundleCache.getSecureAction().mkdirs(extractDir))
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
            File extractJar = new File(embedDir, entryName);

            // Extracting the embedded JAR file impacts all other existing
            // contents for this revision, so we have to grab the revision
            // lock first before trying to extract the embedded JAR file
            // to avoid a race condition.
            synchronized (m_revisionLock)
            {
                if (!BundleCache.getSecureAction().fileExists(extractJar))
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
                m_logger, m_configMap, m_revisionLock,
                extractJar.getParentFile(), extractJar, null);
        }

        // The entry could not be found, so return null.
        return null;
    }

// TODO: This will need to consider security.
    public String getEntryAsNativeLibrary(String entryName)
    {
        // Return result.
        String result = null;

        // Remove any leading slash.
        entryName = (entryName.startsWith("/")) ? entryName.substring(1) : entryName;

        // Any embedded native libraries will be extracted to the lib directory.
        // Since embedded library file names may clash when extracting from multiple
        // embedded JAR files, the embedded lib directory is per embedded JAR file.
        File libDir = new File(m_rootDir, m_file.getName() + LIBRARY_DIRECTORY);

        // The entry name must refer to a file type, since it is
        // a native library, not a directory.
        ZipEntry ze = m_jarFile.getEntry(entryName);
        if ((ze != null) && !ze.isDirectory())
        {
            // Extracting the embedded native library file impacts all other
            // existing contents for this revision, so we have to grab the
            // revision lock first before trying to extract the embedded JAR
            // file to avoid a race condition.
            synchronized (m_revisionLock)
            {
                // Since native libraries cannot be shared, we must extract a
                // separate copy per request, so use the request library counter
                // as part of the extracted path.
                if (m_nativeLibMap == null)
                {
                    m_nativeLibMap = new HashMap();
                }
                Integer libCount = (Integer) m_nativeLibMap.get(entryName);
                // Either set or increment the library count.
                libCount = (libCount == null) ? new Integer(0) : new Integer(libCount.intValue() + 1);
                m_nativeLibMap.put(entryName, libCount);
                File libFile = new File(
                    libDir, libCount.toString() + File.separatorChar + entryName);

                if (!BundleCache.getSecureAction().fileExists(libFile))
                {
                    if (!BundleCache.getSecureAction().fileExists(libFile.getParentFile())
                        && !BundleCache.getSecureAction().mkdirs(libFile.getParentFile()))
                    {
                        m_logger.log(
                            Logger.LOG_ERROR,
                            "Unable to create library directory.");
                    }
                    else
                    {
                        InputStream is = null;

                        try
                        {
                            is = new BufferedInputStream(
                                m_jarFile.getInputStream(ze),
                                BundleCache.BUFSIZE);
                            if (is == null)
                            {
                                throw new IOException("No input stream: " + entryName);
                            }

                            // Create the file.
                            BundleCache.copyStreamToFile(is, libFile);

                            // Perform exec permission command on extracted library
                            // if one is configured.
                            String command = (String) m_configMap.get(
                                Constants.FRAMEWORK_EXECPERMISSION);
                            if (command != null)
                            {
                                Properties props = new Properties();
                                props.setProperty("abspath", libFile.toString());
                                command = Util.substVars(command, "command", null, props);
                                Process p = BundleCache.getSecureAction().exec(command);
                                // We have to make sure we read stdout and stderr because
                                // otherwise we will block on certain unbuffered os's
                                // (like eg. windows)
                                Thread stdOut = new Thread(
                                    new DevNullRunnable(p.getInputStream()));
                                Thread stdErr = new Thread(
                                    new DevNullRunnable(p.getErrorStream()));
                                stdOut.setDaemon(true);
                                stdErr.setDaemon(true);
                                stdOut.start();
                                stdErr.start();
                                p.waitFor();
                                stdOut.join();
                                stdErr.join();
                            }

                            // Return the path to the extracted native library.
                            result = BundleCache.getSecureAction().getAbsolutePath(libFile);
                        }
                        catch (Exception ex)
                        {
                            m_logger.log(
                                Logger.LOG_ERROR,
                                "Extracting native library.", ex);
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
                }
                else
                {
                    // Return the path to the extracted native library.
                    result = BundleCache.getSecureAction().getAbsolutePath(libFile);
                }
            }
        }

        return result;
    }

    public String toString()
    {
        return "JAR " + m_file.getPath();
    }

    public File getFile()
    {
        return m_file;
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
        File embedDir = new File(m_rootDir, m_file.getName() + EMBEDDED_DIRECTORY);
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

    private static JarFileX openJarFile(File file) throws RuntimeException
    {
        try
        {
            return BundleCache.getSecureAction().openJAR(file, false);
        }
        catch (IOException ex)
        {
            throw new RuntimeException(
                "Unable to open JAR file, probably deleted: " + ex.getMessage());
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

    private static class DevNullRunnable implements Runnable 
    {
        private final InputStream m_in;

        public DevNullRunnable(InputStream in) 
        {
            m_in = in;
        }

        public void run()
        {
            try
            {
                try
                {
                    while (m_in.read() != -1){}
                }
                finally 
                {
                    m_in.close();
                }
            }
            catch (Exception ex) 
            {
                // Not much we can do - maybe we should log it?
            }
        }
    }
}