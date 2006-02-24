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
package org.apache.felix.moduleloader;

import java.io.*;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public class DirectoryContent implements IContent
{
    private static final int BUFSIZE = 4096;

    private File m_dir = null;
    private boolean m_opened = false;

    public DirectoryContent(File dir)
    {
        m_dir = dir;
    }

    protected void finalize()
    {
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

    public synchronized byte[] getEntry(String name) throws IllegalStateException
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

    public synchronized Enumeration getEntryPaths(String path)
    {
        if (!m_opened)
        {
            throw new IllegalStateException("DirectoryContent is not open");
        }

        if ((path.length() > 0) && (path.charAt(0) == '/'))
        {
            path = path.substring(1);
        }

        return new FileEnumeration(m_dir, path);
    }


    private static class FileEnumeration implements Enumeration
    {
        private File m_refDir = null;
        private File m_listDir = null;
        private File[] m_children = null;
        private int m_counter = 0;

        public FileEnumeration(File refDir, String path)
        {
            m_refDir = refDir;
            m_listDir = new File(refDir, path);
            if (m_listDir.isDirectory())
            {
                m_children = m_listDir.listFiles();
            }
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
            // Remove the leading path of the reference directory, since the
            // entry paths are supposed to be relative to the root.
            StringBuffer sb = new StringBuffer(m_children[m_counter].getAbsolutePath());
            sb.delete(0, m_refDir.getAbsolutePath().length() + 1);
            // Add a '/' to the end of directory entries.
            if (m_children[m_counter].isDirectory())
            {
                sb.append('/');
            }
            m_counter++;
            return sb.toString();
        }
    }
}