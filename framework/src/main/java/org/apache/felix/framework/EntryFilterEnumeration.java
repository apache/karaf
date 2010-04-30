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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.apache.felix.framework.capabilityset.SimpleFilter;
import org.apache.felix.framework.resolver.Module;

class EntryFilterEnumeration implements Enumeration
{
    private final BundleImpl m_bundle;
    private final List<Enumeration> m_enumerations;
    private final List<Module> m_modules;
    private int m_moduleIndex = 0;
    private final String m_path;
    private final List<String> m_filePattern;
    private final boolean m_recurse;
    private final boolean m_isURLValues;
    private final Set<String> m_dirEntries = new HashSet();
    private final List<Object> m_nextEntries = new ArrayList(2);

    public EntryFilterEnumeration(
        BundleImpl bundle, boolean includeFragments, String path,
        String filePattern, boolean recurse, boolean isURLValues)
    {
        m_bundle = bundle;
        Module bundleModule = m_bundle.getCurrentModule();
        List<Module> fragmentModules = ((ModuleImpl) bundleModule).getFragments();
        if (includeFragments && (fragmentModules != null))
        {
            m_modules = new ArrayList(fragmentModules.size() + 1);
            m_modules.addAll(fragmentModules);
        }
        else
        {
            m_modules = new ArrayList(1);
        }
        m_modules.add(0, bundleModule);
        m_enumerations = new ArrayList(m_modules.size());
        for (int i = 0; i < m_modules.size(); i++)
        {
            m_enumerations.add(m_modules.get(i).getContent() != null ?
                m_modules.get(i).getContent().getEntries() : null);
        }
        m_recurse = recurse;
        m_isURLValues = isURLValues;

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

        m_filePattern = SimpleFilter.parseSubstring(filePattern);

        findNext();
    }

    public synchronized boolean hasMoreElements()
    {
        return (m_nextEntries.size() != 0);
    }

    public synchronized Object nextElement()
    {
        if (m_nextEntries.size() == 0)
        {
            throw new NoSuchElementException("No more entries.");
        }
        Object last = m_nextEntries.remove(0);
        findNext();
        return last;
    }

    private void findNext()
    {
        // This method filters the content entry enumeration, such that
        // it only displays the contents of the directory specified by
        // the path argument either recursively or not; much like using
        // "ls -R" or "ls" to list the contents of a directory, respectively.
        if (m_enumerations == null)
        {
            return;
        }
        while ((m_moduleIndex < m_enumerations.size()) && (m_nextEntries.size() == 0))
        {
            while (m_enumerations.get(m_moduleIndex) != null
                && m_enumerations.get(m_moduleIndex).hasMoreElements()
                && m_nextEntries.size() == 0)
            {
                // Get the current entry to determine if it should be filtered or not.
                String entryName = (String) m_enumerations.get(m_moduleIndex).nextElement();
                // Check to see if the current entry is a descendent of the specified path.
                if (!entryName.equals(m_path) && entryName.startsWith(m_path))
                {
                    // Cached entry URL. If we are returning URLs, we use this
                    // cached URL to avoid doing multiple URL lookups from a module
                    // when synthesizing directory URLs.
                    URL entryURL = null;

                    // If the current entry is in a subdirectory of the specified path,
                    // get the index of the slash character.
                    int dirSlashIdx = entryName.indexOf('/', m_path.length());

                    // JAR files are supposed to contain entries for directories,
                    // but not all do. So calculate the directory for this entry
                    // and see if we've already seen an entry for the directory.
                    // If not, synthesize an entry for the directory. If we are
                    // doing a recursive match, we need to synthesize each matching
                    // subdirectory of the entry.
                    if (dirSlashIdx >= 0)
                    {
                        // Start synthesizing directories for the current entry
                        // at the subdirectory after the initial path.
                        int subDirSlashIdx = dirSlashIdx;
                        String dir;
                        do
                        {
                            // Calculate the subdirectory name.
                            dir = entryName.substring(0, subDirSlashIdx + 1);
                            // If we have not seen this directory before, then record
                            // it and potentially synthesize an entry for it.
                            if (!m_dirEntries.contains(dir))
                            {
                                // Record it.
                                m_dirEntries.add(dir);
                                // If the entry is actually a directory entry (i.e.,
                                // it ends with a slash), then we don't need to
                                // synthesize an entry since it exists; otherwise,
                                // synthesize an entry if it matches the file pattern.
                                if (entryName.length() != (subDirSlashIdx + 1))
                                {
                                    // See if the file pattern matches the last
                                    // element of the path.
                                    if (SimpleFilter.compareSubstring(
                                        m_filePattern, getLastPathElement(dir)))
                                    {
                                        if (m_isURLValues)
                                        {
                                            entryURL = (entryURL == null)
                                                ? m_modules.get(m_moduleIndex).getEntry(entryName)
                                                : entryURL;
                                            try
                                            {
                                                m_nextEntries.add(new URL(entryURL, "/" + dir));
                                            }
                                            catch (MalformedURLException ex)
                                            {
                                            }
                                        }
                                        else
                                        {
                                            m_nextEntries.add(dir);
                                        }
                                    }
                                }
                            }
                            // Now prepare to synthesize the next subdirectory
                            // if we are matching recursively.
                            subDirSlashIdx = entryName.indexOf('/', dir.length());
                        }
                        while (m_recurse && (subDirSlashIdx >= 0));
                    }

                    // Now we actually need to check if the current entry itself should
                    // be filtered or not. If we are recursive or the current entry
                    // is a child (not a grandchild) of the initial path, then we need
                    // to check if it matches the file pattern.
                    if (m_recurse || (dirSlashIdx < 0) || (dirSlashIdx == entryName.length() - 1))
                    {
                        // See if the file pattern matches the last element of the path.
                        if (SimpleFilter.compareSubstring(
                            m_filePattern, getLastPathElement(entryName)))
                        {
                            if (m_isURLValues)
                            {
                                entryURL = (entryURL == null)
                                    ? m_modules.get(m_moduleIndex).getEntry(entryName)
                                    : entryURL;
                                m_nextEntries.add(entryURL);
                            }
                            else
                            {
                                m_nextEntries.add(entryName);
                            }
                        }
                    }
                }
            }
            if (m_nextEntries.size() == 0)
            {
                m_moduleIndex++;
            }
        }
    }

    private static String getLastPathElement(String entryName)
    {
        int endIdx = (entryName.charAt(entryName.length() - 1) == '/')
            ? entryName.length() - 1
            : entryName.length();
        int startIdx = (entryName.charAt(entryName.length() - 1) == '/')
            ? entryName.lastIndexOf('/', endIdx - 1) + 1
            : entryName.lastIndexOf('/', endIdx) + 1;
        return entryName.substring(startIdx, endIdx);
    }
}