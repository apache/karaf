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
package org.apache.felix.framework.searchpolicy;

import java.util.*;

import org.apache.felix.framework.Logger;
import org.osgi.framework.Constants;

public class R4LibraryHeader
{
    private String m_name = null;
    private String[] m_osnames = null;
    private String[] m_osversions = null;
    private String[] m_processors = null;
    private String[] m_languages = null;

    public R4LibraryHeader(String name, String[] osnames, String[] osversions,
        String[] processors, String[] languages)
    {
        m_name = name;
        m_osnames = osnames;
        m_osversions = osversions;
        m_processors = processors;
        m_languages = languages;
    }

    public R4LibraryHeader(R4LibraryHeader library)
    {
        m_name = library.m_name;
        m_osnames = library.m_osnames;
        m_osversions = library.m_osversions;
        m_processors = library.m_processors;
        m_languages = library.m_languages;
    }

    public String getName()
    {
        return m_name;
    }

    public String[] getOSNames()
    {
        return m_osnames;
    }

    public String[] getOSVersions()
    {
        return m_osversions;
    }

    public String[] getProcessors()
    {
        return m_processors;
    }

    public static R4LibraryHeader[] parse(Logger logger, String s)
    {
        try
        {
            if ((s == null) || (s.length() == 0))
            {
                return null;
            }

            // The tokens are separated by semicolons and may include
            // any number of libraries (whose name starts with a "/")
            // along with one set of associated properties.
            StringTokenizer st = new StringTokenizer(s, ";");
            String[] libs = new String[st.countTokens()];
            List osNameList = new ArrayList();
            List osVersionList = new ArrayList();
            List processorList = new ArrayList();
            List languageList = new ArrayList();
            int libCount = 0;
            while (st.hasMoreTokens())
            {
                String token = st.nextToken().trim();
                if (token.indexOf('=') < 0)
                {
                    // Remove the slash, if necessary.
                    libs[libCount] = (token.charAt(0) == '/')
                        ? token.substring(1)
                        : token;
                    libCount++;
                }
                else
                {
                    // Check for valid native library properties; defined as
                    // a property name, an equal sign, and a value.
                    StringTokenizer stProp = new StringTokenizer(token, "=");
                    if (stProp.countTokens() != 2)
                    {
                        throw new IllegalArgumentException(
                            "Bundle manifest native library entry malformed: " + token);
                    }
                    String property = stProp.nextToken().trim().toLowerCase();
                    String value = stProp.nextToken().trim();
                    
                    // Values may be quoted, so remove quotes if present.
                    if (value.charAt(0) == '"')
                    {
                        // This should always be true, otherwise the
                        // value wouldn't be properly quoted, but we
                        // will check for safety.
                        if (value.charAt(value.length() - 1) == '"')
                        {
                            value = value.substring(1, value.length() - 1);
                        }
                        else
                        {
                            value = value.substring(1);
                        }
                    }
                    // Add the value to its corresponding property list.
                    if (property.equals(Constants.BUNDLE_NATIVECODE_OSNAME))
                    {
                        osNameList.add(value);
                    }
                    else if (property.equals(Constants.BUNDLE_NATIVECODE_OSVERSION))
                    {
                        osVersionList.add(value);
                    }
                    else if (property.equals(Constants.BUNDLE_NATIVECODE_PROCESSOR))
                    {
                        processorList.add(value);
                    }
                    else if (property.equals(Constants.BUNDLE_NATIVECODE_LANGUAGE))
                    {
                        languageList.add(value);
                    }
                }
            }

            if (libCount == 0)
            {
                return null;
            }

            R4LibraryHeader[] libraries = new R4LibraryHeader[libCount];
            for (int i = 0; i < libCount; i++)
            {
                libraries[i] =
                    new R4LibraryHeader(
                        libs[i],
                        (String[]) osNameList.toArray(new String[0]),
                        (String[]) osVersionList.toArray(new String[0]),
                        (String[]) processorList.toArray(new String[0]),
                        (String[]) languageList.toArray(new String[0]));
            }

            return libraries;

        }
        catch (RuntimeException ex)
        {
            logger.log(
                Logger.LOG_ERROR,
                "Error parsing native library header.",
                ex);
            throw ex;
        }
    }

    public String toString()
    {
        return m_name;
    }
}