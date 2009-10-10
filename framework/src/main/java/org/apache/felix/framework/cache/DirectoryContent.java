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

import org.apache.felix.moduleloader.*;
import java.io.*;
import java.util.*;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.Util;
import org.osgi.framework.Constants;

public class DirectoryContent implements IContent
{
    private static final int BUFSIZE = 4096;
    private static final transient String EMBEDDED_DIRECTORY = "-embedded";
    private static final transient String LIBRARY_DIRECTORY = "-lib";

    private final Logger m_logger;
    private final Map m_configMap;
    private final Object m_revisionLock;
    private final File m_rootDir;
    private final File m_dir;
    private Map m_nativeLibMap;

    public DirectoryContent(Logger logger, Map configMap, Object revisionLock,
        File rootDir, File dir)
    {
        m_logger = logger;
        m_configMap = configMap;
        m_revisionLock = revisionLock;
        m_rootDir = rootDir;
        m_dir = dir;
    }

    public void close()
    {
        // Nothing to clean up.
    }

    public synchronized boolean hasEntry(String name) throws IllegalStateException
    {
        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        return new File(m_dir, name).exists();
    }

    public synchronized Enumeration getEntries()
    {
        // Wrap entries enumeration to filter non-matching entries.
        Enumeration e = new EntriesEnumeration(m_dir);

        // Spec says to return null if there are no entries.
        return (e.hasMoreElements()) ? e : null;
    }

    public synchronized byte[] getEntryAsBytes(String name) throws IllegalStateException
    {
        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        // Get the embedded resource.
        InputStream is = null;
        ByteArrayOutputStream baos = null;

        try
        {
            is = new BufferedInputStream(new FileInputStream(new File(m_dir, name)));
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
        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        return new FileInputStream(new File(m_dir, name));
    }

    public synchronized IContent getEntryAsContent(String entryName)
    {
        // If the entry name refers to the content itself, then
        // just return it immediately.
        if (entryName.equals(FelixConstants.CLASS_PATH_DOT))
        {
            return new DirectoryContent(m_logger, m_configMap, m_revisionLock, m_rootDir, m_dir);
        }

        // Remove any leading slash, since all bundle class path
        // entries are relative to the root of the bundle.
        entryName = (entryName.startsWith("/")) ? entryName.substring(1) : entryName;

        // Any embedded JAR files will be extracted to the embedded directory.
        File embedDir = new File(m_rootDir, m_dir.getName() + EMBEDDED_DIRECTORY);

        // Determine if the entry is an emdedded JAR file or
        // directory in the bundle JAR file. Ignore any entries
        // that do not exist per the spec.
        File file = new File(m_dir, entryName);
        if (BundleCache.getSecureAction().isFileDirectory(file))
        {
            return new DirectoryContent(m_logger, m_configMap, m_revisionLock, m_rootDir, file);
        }
        else if (BundleCache.getSecureAction().fileExists(file)
            && entryName.endsWith(".jar"))
        {
            File extractDir = new File(embedDir,
                (entryName.lastIndexOf('/') >= 0)
                    ? entryName.substring(0, entryName.lastIndexOf('/'))
                    : entryName);
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
            return new JarContent(m_logger, m_configMap, m_revisionLock, extractDir, file, null);
        }

        // The entry could not be found, so return null.
        return null;
    }

// TODO: This will need to consider security.
    public synchronized String getEntryAsNativeLibrary(String entryName)
    {
        // Return result.
        String result = null;

        // Remove any leading slash, since all bundle class path
        // entries are relative to the root of the bundle.
        entryName = (entryName.startsWith("/")) ? entryName.substring(1) : entryName;

        // Any embedded native library files will be extracted to the lib directory.
        File libDir = new File(m_rootDir, m_dir.getName() + LIBRARY_DIRECTORY);

        // The entry must exist and refer to a file, not a directory,
        // since we are expecting it to be a native library.
        File entryFile = new File(m_dir, entryName);
        if (BundleCache.getSecureAction().fileExists(entryFile)
            && !BundleCache.getSecureAction().isFileDirectory(entryFile))
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
                                new FileInputStream(entryFile),
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
                                p.waitFor();
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
        return "DIRECTORY " + m_dir;
    }

    private static class EntriesEnumeration implements Enumeration
    {
        private File m_dir = null;
        private File[] m_children = null;
        private int m_counter = 0;

        public EntriesEnumeration(File dir)
        {
            m_dir = dir;
            m_children = listFilesRecursive(m_dir);
        }

        public boolean hasMoreElements()
        {
            return (m_children != null) && (m_counter < m_children.length);
        }

        public Object nextElement()
        {
            if ((m_children == null) || (m_counter >= m_children.length))
            {
                throw new NoSuchElementException("No more entry paths.");
            }

            // Convert the file separator character to slashes.
            String abs = m_children[m_counter].getAbsolutePath()
                .replace(File.separatorChar, '/');

            // Remove the leading path of the reference directory, since the
            // entry paths are supposed to be relative to the root.
            StringBuffer sb = new StringBuffer(abs);
            sb.delete(0, m_dir.getAbsolutePath().length() + 1);
            // Add a '/' to the end of directory entries.
            if (m_children[m_counter].isDirectory())
            {
                sb.append('/');
            }
            m_counter++;
            return sb.toString();
        }

        public File[] listFilesRecursive(File dir)
        {
            File[] children = dir.listFiles();
            File[] combined = children;
            for (int i = 0; i < children.length; i++)
            {
                if (children[i].isDirectory())
                {
                    File[] grandchildren = listFilesRecursive(children[i]);
                    if (grandchildren.length > 0)
                    {
                        File[] tmp = new File[combined.length + grandchildren.length];
                        System.arraycopy(combined, 0, tmp, 0, combined.length);
                        System.arraycopy(grandchildren, 0, tmp, combined.length, grandchildren.length);
                        combined = tmp;
                    }
                }
            }
            return combined;
        }
    }
}