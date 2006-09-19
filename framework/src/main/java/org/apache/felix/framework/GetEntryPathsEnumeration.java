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
package org.apache.felix.framework;

import java.util.Enumeration;
import java.util.NoSuchElementException;

class GetEntryPathsEnumeration implements Enumeration
{
    private BundleImpl m_bundle = null;
    private Enumeration m_enumeration = null;
    private String m_path = null;
    private Object m_next = null;

    public GetEntryPathsEnumeration(BundleImpl bundle, String path)
    {
        m_bundle = bundle;
        m_path = path;
        m_enumeration = m_bundle.getInfo().getCurrentModule()
            .getContentLoader().getContent().getEntries();

        // Sanity check the parameters.
        if (m_path == null)
        {
            throw new IllegalArgumentException("The path for findEntries() cannot be null.");
        }
        // Strip leading '/' if present.
        if ((m_path.length() > 0) && (m_path.charAt(0) == '/'))
        {
            m_path = m_path.substring(1);
        }
        // Add a '/' to the end if not present.
        if ((m_path.length() > 0) && (m_path.charAt(m_path.length() - 1) != '/'))
        {
            m_path = m_path + "/";
        }

        m_next = findNext();
    }

    public boolean hasMoreElements()
    {
        return (m_next != null);
    }

    public Object nextElement()
    {
        if (m_next == null)
        {
            throw new NoSuchElementException("No more entry paths.");
        }
        Object last = m_next;
        m_next = findNext();
        return last;
    }

    private Object findNext()
    {
        // This method filters the content entry enumeration, such that
        // it only displays the contents of the directory specified by
        // the path argument; much like using "ls" to list the contents
        // of a directory.
        while (m_enumeration.hasMoreElements())
        {
            // Get the next entry name.
            String entryName = (String) m_enumeration.nextElement();
            // Check to see if it is a descendent of the specified path.
            if (!entryName.equals(m_path) && entryName.startsWith(m_path))
            {
                // Verify that it is a child of the path and not a
                // grandchild by examining its remaining path length.
                // This code uses the knowledge that content entries
                // corresponding to directories end in '/'. It checks
                // to see if the next occurrence of '/' is also the
                // end of the string, which means that this entry
                // represents a child directory of the path.
                int idx = entryName.indexOf('/', m_path.length());
                if ((idx < 0) || (idx == (entryName.length() - 1)))
                {
                    return entryName;
                }
            }
        }
        return null;
    }
}