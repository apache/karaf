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

import java.util.*;

import org.apache.felix.framework.util.Util;
import org.apache.felix.moduleloader.IModule;

class FindEntriesEnumeration implements Enumeration
{
    private final BundleImpl m_bundle;
    private final Enumeration[] m_enumerations;
    private final IModule[] m_modules;
    private int m_moduleIndex = 0;
    private final String m_path;
    private final String[] m_filePattern;
    private final boolean m_recurse;
    private Object m_next = null;

    public FindEntriesEnumeration(
        BundleImpl bundle, String path, String filePattern, boolean recurse)
    {
        m_bundle = bundle;
        IModule bundleModule = m_bundle.getCurrentModule();
        IModule[] fragmentModules = ((ModuleImpl) bundleModule).getFragments();
        if (fragmentModules == null)
        {
            fragmentModules = new IModule[0];
        }
        m_modules = new IModule[fragmentModules.length + 1];
        m_modules[0] = bundleModule;
        for (int i = 0; i < fragmentModules.length; i++)
        {
            m_modules[i + 1] = fragmentModules[i];
        }
        m_enumerations = new Enumeration[m_modules.length];
        for (int i = 0; i < m_modules.length; i++)
        {
            m_enumerations[i] = m_modules[i].getContent() != null ?
                m_modules[i].getContent().getEntries() : null;
        }
        m_recurse = recurse;

        // Sanity check the parameters.
        if (path == null)
        {
            throw new IllegalArgumentException("The path for findEntries() cannot be null.");
        }
        // Strip leading '/' if present.
        if ((path.length() > 0) && (path.charAt(0) == '/'))
        {
            path = path.substring(1);
        }
        // Add a '/' to the end if not present.
        if ((path.length() > 0) && (path.charAt(path.length() - 1) != '/'))
        {
            path = path + "/";
        }
        m_path = path;

        // File pattern defaults to "*" if not specified.
        filePattern = (filePattern == null) ? "*" : filePattern;

        m_filePattern = Util.parseSubstring(filePattern);

        m_next = findNext();
    }

    public synchronized boolean hasMoreElements()
    {
        return (m_next != null);
    }

    public synchronized Object nextElement()
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
        // the path argument either recursively or not; much like using
        // "ls -R" or "ls" to list the contents of a directory, respectively.
        if (m_enumerations == null)
        {
            return null;
        }
        while (m_moduleIndex < m_enumerations.length)
        {
            while (m_enumerations[m_moduleIndex] != null
                &&  m_enumerations[m_moduleIndex].hasMoreElements())
            {
                // Get the next entry name.
                String entryName = (String) m_enumerations[m_moduleIndex].nextElement();
                // Check to see if it is a descendent of the specified path.
                if (!entryName.equals(m_path) && entryName.startsWith(m_path))
                {
                    // If this is recursive search, then try to match any
                    // entry path that starts with the specified path;
                    // otherwise, only try to match children of the specified
                    // path and not any grandchild. This code uses the knowledge
                    // that content entries corresponding to directories end in '/'.
                    int idx = entryName.indexOf('/', m_path.length());
                    if (m_recurse || (idx < 0) || (idx == (entryName.length() - 1)))
                    {
                        // Get the last element of the entry path, not including
                        // the '/' if it is a directory.
                        int endIdx = (entryName.charAt(entryName.length() - 1) == '/')
                            ? entryName.length() - 1
                            : entryName.length();
                        int startIdx = (entryName.charAt(entryName.length() - 1) == '/')
                            ? entryName.lastIndexOf('/', endIdx - 1) + 1
                            : entryName.lastIndexOf('/', endIdx) + 1;
                        String lastElement = entryName.substring(startIdx, endIdx);

                        // See if the file pattern matches the last element of the path.
                        if (Util.checkSubstring(m_filePattern, lastElement))
                        {
                            // Convert entry name into an entry URL.
                            return m_modules[m_moduleIndex].getEntry(entryName);
                        }
                    }
                }
            }
            m_moduleIndex++;
        }

        return null;
    }
}