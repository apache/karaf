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

public class DirectoryContent implements IContent
{
    private static final int BUFSIZE = 4096;
    private static final transient String EMBEDDED_DIRECTORY = "-embedded";
    private static final transient String LIBRARY_DIRECTORY = "lib";

    private Logger m_logger;
    private Object m_revisionLock;
    private File m_rootDir;
    private File m_dir;
    private boolean m_opened = false;

    public DirectoryContent(Logger logger, Object revisionLock, File rootDir, File dir)
    {
        m_logger = logger;
        m_revisionLock = revisionLock;
        m_rootDir = rootDir;
        m_dir = dir;
    }

    public void open()
    {
        m_opened = true;
    }

    public synchronized void close()
    {
        m_opened = false;
    }

    public synchronized boolean hasEntry(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("DirectoryContent is not open");
        }

        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        return new File(m_dir, name).exists();
    }

    public synchronized Enumeration getEntries()
    {
        if (!m_opened)
        {
            throw new IllegalStateException("JarContent is not open");
        }

        // Wrap entries enumeration to filter non-matching entries.
        Enumeration e = new EntriesEnumeration(m_dir);

        // Spec says to return null if there are no entries.
        return (e.hasMoreElements()) ? e : null;
    }

    public synchronized byte[] getEntryAsBytes(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("DirectoryContent is not open");
        }

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
        if (!m_opened)
        {
            throw new IllegalStateException("DirectoryContent is not open");
        }

        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        return new FileInputStream(new File(m_dir, name));
    }

    public synchronized IContent getEntryAsContent(String entryName)
    {
        if (!m_opened)
        {
            throw new IllegalStateException("DirectoryContent is not open");
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
            return new DirectoryContent(m_logger, m_revisionLock, m_rootDir, file);
        }
        else if (BundleCache.getSecureAction().fileExists(file)
            && entryName.endsWith(".jar"))
        {
            File extractedDir = new File(embedDir,
                (entryName.lastIndexOf('/') >= 0)
                    ? entryName.substring(0, entryName.lastIndexOf('/'))
                    : entryName);
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
            System.out.println("+++ EXTRACTED JAR DIR " + extractedDir);
            return new JarContent(m_logger, m_revisionLock, extractedDir, file);
        }

        // The entry could not be found, so return null.
        return null;
    }

// TODO: This will need to consider security.
    public synchronized String getEntryAsNativeLibrary(String name)
    {
        if (!m_opened)
        {
            throw new IllegalStateException("DirectoryContent is not open");
        }

        return BundleCache.getSecureAction().getAbsolutePath(new File(m_rootDir, name));
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