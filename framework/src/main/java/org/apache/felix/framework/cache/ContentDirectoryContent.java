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

import org.apache.felix.framework.resolver.Content;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.NoSuchElementException;

public class ContentDirectoryContent implements Content
{
    private Content m_content = null;
    private String m_rootPath = null;

    public ContentDirectoryContent(Content content, String path)
    {
        m_content = content;
        // Add a '/' to the end if not present.
        m_rootPath = (path.length() > 0) && (path.charAt(path.length() - 1) != '/')
            ? path + "/" : path;
    }

    public synchronized void close()
    {
        // We do not actually close the associated content
        // from which we are filtering our directory because
        // we assume that this will be close manually by
        // the owner of that content.
        m_content = null;
    }

    public synchronized boolean hasEntry(String name) throws IllegalStateException
    {
        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        return m_content.hasEntry(m_rootPath + name);
    }

    public synchronized Enumeration getEntries()
    {
        return new EntriesEnumeration(m_content.getEntries(), m_rootPath);
    }

    public synchronized byte[] getEntryAsBytes(String name) throws IllegalStateException
    {
        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        return m_content.getEntryAsBytes(m_rootPath + name);
    }

    public synchronized InputStream getEntryAsStream(String name)
        throws IllegalStateException, IOException
    {
        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        return m_content.getEntryAsStream(m_rootPath + name);
    }

    public Content getEntryAsContent(String name)
    {
        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        return m_content.getEntryAsContent(m_rootPath + name);
    }

    public String getEntryAsNativeLibrary(String name)
    {
        if ((name.length() > 0) && (name.charAt(0) == '/'))
        {
            name = name.substring(1);
        }

        return m_content.getEntryAsNativeLibrary(m_rootPath + name);
    }

    public String toString()
    {
        return "CONTENT DIR " + m_rootPath + " (" + m_content + ")";
    }

    private static class EntriesEnumeration implements Enumeration
    {
        private Enumeration m_enumeration = null;
        private String m_rootPath = null;
        private String m_nextEntry = null;

        public EntriesEnumeration(Enumeration enumeration, String rootPath)
        {
            m_enumeration = enumeration;
            m_rootPath = rootPath;
            m_nextEntry = findNextEntry();
        }

        public boolean hasMoreElements()
        {
            return (m_nextEntry != null);
        }

        public Object nextElement()
        {
            if (m_nextEntry == null)
            {
                throw new NoSuchElementException("No more elements.");
            }
            String currentEntry = m_nextEntry;
            m_nextEntry = findNextEntry();
            return currentEntry;
        }

        private String findNextEntry()
        {
            // Find next entry that is inside the root directory.
            while (m_enumeration.hasMoreElements())
            {
                String next = (String) m_enumeration.nextElement();
                if (next.startsWith(m_rootPath) && !next.equals(m_rootPath))
                {
                    // Strip off the root directory.
                    return next.substring(m_rootPath.length());
                }
            }
            return null;
        }
    }
}