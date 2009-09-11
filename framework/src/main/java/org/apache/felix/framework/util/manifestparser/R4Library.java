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
package org.apache.felix.framework.util.manifestparser;

import java.util.Map;
import org.osgi.framework.Constants;

public class R4Library
{
    private String m_libraryFile;
    private String[] m_osnames;
    private String[] m_processors;
    private String[] m_osversions;
    private String[] m_languages;
    private String m_selectionFilter;

    public R4Library(
        String libraryFile, String[] osnames, String[] processors, String[] osversions,
        String[] languages, String selectionFilter) throws Exception
    {
        m_libraryFile = libraryFile;
        m_osnames = osnames;
        m_processors = processors;
        m_osversions = osversions;
        m_languages = languages;
        m_selectionFilter = selectionFilter;
    }

    public String getEntryName()
    {
        return m_libraryFile;
    }

    public String[] getOSNames()
    {
        return m_osnames;
    }

    public String[] getProcessors()
    {
        return m_processors;
    }

    public String[] getOSVersions()
    {
        return m_osversions;
    }

    public String[] getLanguages()
    {
        return m_languages;
    }

    public String getSelectionFilter()
    {
        return m_selectionFilter;
    }

    /**
     * <p>
     * Determines if the specified native library name matches this native
     * library definition.
     * </p>
     * @param name the native library name to try to match.
     * @return <tt>true</tt> if this native library name matches this native
     *         library definition; <tt>false</tt> otherwise.
    **/
    public boolean match(Map configMap, String name)
    {
        String libname = System.mapLibraryName(name);
        String[] exts = ManifestParser.parseDelimitedString(
            (String) configMap.get(Constants.FRAMEWORK_LIBRARY_EXTENSIONS), ",");
        int extIdx = 0;

        // First try to match the default name, then try to match any additionally
        // specified library extensions.
        do
        {
            if (m_libraryFile.equals(libname) || m_libraryFile.endsWith("/" + libname))
            {
                return true;
            }
            else if (libname.endsWith(".jnilib") && m_libraryFile.endsWith(".dylib"))
            {
                libname = libname.substring(0, libname.length() - 6) + "dylib";
                if (m_libraryFile.equals(libname) || m_libraryFile.endsWith("/" + libname))
                {
                    return true;
                }
            }
            else if (m_libraryFile.equals(name) || m_libraryFile.endsWith("/" + name))
            {
                return true;
            }

            // If we have other native library extensions to try, then
            // calculate the new native library name.
            if ((exts != null) && (extIdx < exts.length))
            {
                int idx = libname.lastIndexOf(".");
                libname = (idx < 0)
                    ? libname + "." + exts[extIdx++]
                    : libname.substring(0, idx) + "." + exts[extIdx++];
            }
        }
        while ((exts != null) && (extIdx < exts.length));

        return false;
    }

    public String toString()
    {
        if (m_libraryFile != null)
        {
            StringBuffer sb = new StringBuffer();
            sb.append(m_libraryFile);
            for (int i = 0; (m_osnames != null) && (i < m_osnames.length); i++)
            {
                sb.append(';');
                sb.append(Constants.BUNDLE_NATIVECODE_OSNAME);
                sb.append('=');
                sb.append(m_osnames[i]);
            }
            for (int i = 0; (m_processors != null) && (i < m_processors.length); i++)
            {
                sb.append(';');
                sb.append(Constants.BUNDLE_NATIVECODE_PROCESSOR);
                sb.append('=');
                sb.append(m_processors[i]);
            }
            for (int i = 0; (m_osversions != null) && (i < m_osversions.length); i++)
            {
                sb.append(';');
                sb.append(Constants.BUNDLE_NATIVECODE_OSVERSION);
                sb.append('=');
                sb.append(m_osversions[i]);
            }
            for (int i = 0; (m_languages != null) && (i < m_languages.length); i++)
            {
                sb.append(';');
                sb.append(Constants.BUNDLE_NATIVECODE_LANGUAGE);
                sb.append('=');
                sb.append(m_languages[i]);
            }
            if (m_selectionFilter != null)
            {
                sb.append(';');
                sb.append(Constants.SELECTION_FILTER_ATTRIBUTE);
                sb.append('=');
                sb.append('\'');
                sb.append(m_selectionFilter);
            }

            return sb.toString();
        }
        return "*";
    }
}