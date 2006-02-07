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

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

public class ContentDirectoryContent implements IContent
{
    private IContent m_content = null;
    private String m_path = null;
    private boolean m_opened = false;

    public ContentDirectoryContent(IContent content, String path)
    {
        m_content = content;
        // Add a '/' to the end if not present.
        m_path = (path.length() > 0) && (path.charAt(path.length() - 1) != '/')
            ? path + "/" : path;
    }

    protected void finalize()
    {
        if (m_content != null)
        {
            m_content.close();
        }
    }

    public void open()
    {
        m_content.open();
        m_opened = true;
    }

    public synchronized void close()
    {
        try
        {
            if (m_content != null)
            {
                m_content.close();
            }
        }
        catch (Exception ex)
        {
            System.err.println("JarContent: " + ex);
        }

        m_content = null;
        m_opened = false;
    }

    public synchronized boolean hasEntry(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("ContentDirectoryContent is not open");
        }

        if (name.charAt(0) == '/')
        {
            name = name.substring(1);
        }

        return m_content.hasEntry(m_path + name);
    }

    public synchronized byte[] getEntry(String name) throws IllegalStateException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("ContentDirectoryContent is not open");
        }

        if (name.charAt(0) == '/')
        {
            name = name.substring(1);
        }

        return m_content.getEntry(m_path + name);
    }

    public synchronized InputStream getEntryAsStream(String name)
        throws IllegalStateException, IOException
    {
        if (!m_opened)
        {
            throw new IllegalStateException("ContentDirectoryContent is not open");
        }

        if (name.charAt(0) == '/')
        {
            name = name.substring(1);
        }

        return m_content.getEntryAsStream(m_path + name);
    }

    public synchronized Enumeration getEntryPaths(String path)
    {
        if (!m_opened)
        {
            throw new IllegalStateException("ContentDirectoryContent is not open");
        }

        if (path.charAt(0) == '/')
        {
            path = path.substring(1);
        }

        return m_content.getEntryPaths(m_path + path);
    }

    public String toString()
    {
        return "CONTENT DIR " + m_path + " (" + m_content + ")";
    }
}