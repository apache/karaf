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

import java.util.*;

import org.apache.felix.framework.FilterImpl;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.*;
import org.osgi.framework.*;

public class R4LibraryClause
{
    private String[] m_libraryFiles = null;
    private String[] m_osnames = null;
    private String[] m_processors = null;
    private String[] m_osversions = null;
    private String[] m_languages = null;
    private String m_selectionFilter = null;

    public R4LibraryClause(String[] libraryFiles, String[] osnames,
        String[] processors, String[] osversions, String[] languages,
        String selectionFilter)
    {
        m_libraryFiles = libraryFiles;
        m_osnames = osnames;
        m_processors = processors;
        m_osversions = osversions;
        m_languages = languages;
        m_selectionFilter = selectionFilter;
    }

    public R4LibraryClause(R4LibraryClause library)
    {
        m_libraryFiles = library.m_libraryFiles;
        m_osnames = library.m_osnames;
        m_osversions = library.m_osversions;
        m_processors = library.m_processors;
        m_languages = library.m_languages;
        m_selectionFilter = library.m_selectionFilter;
    }

    public String[] getLibraryFiles()
    {
        return m_libraryFiles;
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

    public boolean match(Map configMap) throws BundleException
    {
        String normal_osname = normalizeOSName((String) configMap.get(Constants.FRAMEWORK_OS_NAME));
        String normal_processor = normalizeProcessor((String) configMap.get(Constants.FRAMEWORK_PROCESSOR));
        String normal_osversion = normalizeOSVersion((String) configMap.get(Constants.FRAMEWORK_OS_VERSION));
        String normal_language = (String) configMap.get(Constants.FRAMEWORK_LANGUAGE);

        // Check library's osname.
        if (!checkOSNames(normal_osname, getOSNames()))
        {
            return false;
        }

        // Check library's processor.
        if (!checkProcessors(normal_processor, getProcessors()))
        {
            return false;
        }

        // Check library's osversion if specified.
        if ((getOSVersions() != null) &&
            (getOSVersions().length > 0) &&
            !checkOSVersions(normal_osversion, getOSVersions()))
        {
            return false;
        }

        // Check library's language if specified.
        if ((getLanguages() != null) &&
            (getLanguages().length > 0) &&
            !checkLanguages(normal_language, getLanguages()))
        {
            return false;
        }

        // Check library's selection-filter if specified.
        if ((getSelectionFilter() != null) &&
            (getSelectionFilter().length() >= 0) &&
            !checkSelectionFilter(configMap, getSelectionFilter()))
        {
            return false;
        }

        return true;
    }

    private boolean checkOSNames(String currentOSName, String[] osnames)
    {
        boolean win32 = currentOSName.startsWith("win") && 
            (currentOSName.equals("windows95") ||
            currentOSName.equals("windows98") ||
            currentOSName.equals("windowsnt") ||
            currentOSName.equals("windows2000") ||
            currentOSName.equals("windowsxp") ||
            currentOSName.equals("windowsce") ||
            currentOSName.equals("windowsvista"));

        for (int i = 0; (osnames != null) && (i < osnames.length); i++)
        {
            if (osnames[i].equals(currentOSName) || 
                ("win32".equals(osnames[i]) && win32))
            {
                return true;
            }
        }
        return false;
    }

    private boolean checkProcessors(String currentProcessor, String[] processors)
    {
        for (int i = 0; (processors != null) && (i < processors.length); i++)
        {
            if (processors[i].equals(currentProcessor))
            {
                return true;
            }
        }
        return false;
    }

    private boolean checkOSVersions(String currentOSVersion, String[] osversions)
        throws BundleException
    {
        for (int i = 0; (osversions != null) && (i < osversions.length); i++)
        {
            try
            {
                VersionRange range = VersionRange.parse(osversions[i]);
                if (range.isInRange(new Version(currentOSVersion)))
                {
                    return true;
                }
            }
            catch (Exception ex)
            {
                throw new BundleException(
                    "Error evaluating osversion: " + osversions[i], ex);
            }
        }
        return false;
    }

    private boolean checkLanguages(String currentLanguage, String[] languages)
    {
        for (int i = 0; (languages != null) && (i < languages.length); i++)
        {
            if (languages[i].equals(currentLanguage))
            {
                return true;
            }
        }
        return false;
    }

    private boolean checkSelectionFilter(Map configMap, String expr)
        throws BundleException
    {
        // Get all framework properties
        Dictionary dict = new Hashtable();
        for (Iterator i = configMap.keySet().iterator(); i.hasNext(); )
        {
            Object key = i.next();
            dict.put(key, configMap.get(key));
        }
        // Compute expression
        try
        {
            FilterImpl filter = new FilterImpl(expr);
            return filter.match(dict);
        }
        catch (Exception ex)
        {
            throw new BundleException(
                "Error evaluating filter expression: " + expr, ex);
        }
    }

    public static R4LibraryClause parse(Logger logger, String s)
    {
        try
        {
            if ((s == null) || (s.length() == 0))
            {
                return null;
            }

            if (s.equals(FelixConstants.BUNDLE_NATIVECODE_OPTIONAL))
            {
                return new R4LibraryClause(null, null, null, null, null, null);
            }

            // The tokens are separated by semicolons and may include
            // any number of libraries along with one set of associated
            // properties.
            StringTokenizer st = new StringTokenizer(s, ";");
            String[] libFiles = new String[st.countTokens()];
            List osNameList = new ArrayList();
            List osVersionList = new ArrayList();
            List processorList = new ArrayList();
            List languageList = new ArrayList();
            String selectionFilter = null;
            int libCount = 0;
            while (st.hasMoreTokens())
            {
                String token = st.nextToken().trim();
                if (token.indexOf('=') < 0)
                {
                    // Remove the slash, if necessary.
                    libFiles[libCount] = (token.charAt(0) == '/')
                        ? token.substring(1)
                        : token;
                    libCount++;
                }
                else
                {
                    // Check for valid native library properties; defined as
                    // a property name, an equal sign, and a value.
                    // NOTE: StringTokenizer can not be used here because
                    // a value can contain one or more "=" too, e.g.,
                    // selection-filter="(org.osgi.framework.windowing.system=gtk)"
                    String property = null;
                    String value = null;
                    if (!(token.indexOf("=") > 1))
                    {
                        throw new IllegalArgumentException(
                            "Bundle manifest native library entry malformed: " + token);
                    }
                    else
                    {
                        property = (token.substring(0, token.indexOf("=")))
                            .trim().toLowerCase();
                        value = (token.substring(token.indexOf("=") + 1, token
                            .length())).trim();
                    }

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
                        osNameList.add(normalizeOSName(value));
                    }
                    else if (property.equals(Constants.BUNDLE_NATIVECODE_OSVERSION))
                    {
                        osVersionList.add(normalizeOSVersion(value));
                    }
                    else if (property.equals(Constants.BUNDLE_NATIVECODE_PROCESSOR))
                    {
                        processorList.add(normalizeProcessor(value));
                    }
                    else if (property.equals(Constants.BUNDLE_NATIVECODE_LANGUAGE))
                    {
                        languageList.add(value);
                    }
                    else if (property.equals(Constants.SELECTION_FILTER_ATTRIBUTE))
                    {
// TODO: NATIVE - I believe we can have multiple selection filters too.
                        selectionFilter = value;
                    }
                }
            }

            if (libCount == 0)
            {
                return null;
            }

            // Shrink lib file array.
            String[] actualLibFiles = new String[libCount];
            System.arraycopy(libFiles, 0, actualLibFiles, 0, libCount);
            return new R4LibraryClause(
                actualLibFiles,
                (String[]) osNameList.toArray(new String[osNameList.size()]),
                (String[]) processorList.toArray(new String[processorList.size()]),
                (String[]) osVersionList.toArray(new String[osVersionList.size()]),
                (String[]) languageList.toArray(new String[languageList.size()]),
                selectionFilter);
        }
        catch (RuntimeException ex)
        {
            logger.log(Logger.LOG_ERROR,
                "Error parsing native library header.", ex);
            throw ex;
        }
    }

    public static String normalizeOSName(String value)
    {
        value = value.toLowerCase();

        if (value.startsWith("win"))
        {
            String os = "win";
            if (value.indexOf("32") >= 0 || value.indexOf("*") >= 0)
            {
                os = "win32";
            }
            else if (value.indexOf("95") >= 0)
            {
                os = "windows95";
            }
            else if (value.indexOf("98") >= 0)
            {
                os = "windows98";
            }
            else if (value.indexOf("nt") >= 0)
            {
                os = "windowsnt";
            }
            else if (value.indexOf("2000") >= 0)
            {
                os = "windows2000";
            }
            else if (value.indexOf("xp") >= 0)
            {
                os = "windowsxp";
            }
            else if (value.indexOf("ce") >= 0)
            {
                os = "windowsce";
            }
            else if (value.indexOf("vista") >= 0)
            {
                os = "windowsvista";
            }
            return os;
        }
        else if (value.startsWith("linux"))
        {
            return "linux";
        }
        else if (value.startsWith("aix"))
        {
            return "aix";
        }
        else if (value.startsWith("digitalunix"))
        {
            return "digitalunix";
        }
        else if (value.startsWith("hpux"))
        {
            return "hpux";
        }
        else if (value.startsWith("irix"))
        {
            return "irix";
        }
        else if (value.startsWith("macos") || value.startsWith("mac os"))
        {
            return "macos";
        }
        else if (value.startsWith("netware"))
        {
            return "netware";
        }
        else if (value.startsWith("openbsd"))
        {
            return "openbsd";
        }
        else if (value.startsWith("netbsd"))
        {
            return "netbsd";
        }
        else if (value.startsWith("os2") || value.startsWith("os/2"))
        {
            return "os2";
        }
        else if (value.startsWith("qnx") || value.startsWith("procnto"))
        {
            return "qnx";
        }
        else if (value.startsWith("solaris"))
        {
            return "solaris";
        }
        else if (value.startsWith("sunos"))
        {
            return "sunos";
        }
        else if (value.startsWith("vxworks"))
        {
            return "vxworks";
        }
        return value;
    }

    public static String normalizeProcessor(String value)
    {
        value = value.toLowerCase();

        if (value.startsWith("x86-64") || value.startsWith("amd64"))
        {
            return "x86-64";
        }
        else if (value.startsWith("x86") || value.startsWith("pentium")
            || value.startsWith("i386") || value.startsWith("i486")
            || value.startsWith("i586") || value.startsWith("i686"))
        {
            return "x86";
        }
        else if (value.startsWith("68k"))
        {
            return "68k";
        }
        else if (value.startsWith("arm"))
        {
            return "arm";
        }
        else if (value.startsWith("alpha"))
        {
            return "alpha";
        }
        else if (value.startsWith("ignite") || value.startsWith("psc1k"))
        {
            return "ignite";
        }
        else if (value.startsWith("mips"))
        {
            return "mips";
        }
        else if (value.startsWith("parisc"))
        {
            return "parisc";
        }
        else if (value.startsWith("powerpc") || value.startsWith("power")
            || value.startsWith("ppc"))
        {
            return "powerpc";
        }
        else if (value.startsWith("sparc"))
        {
            return "sparc";
        }
        return value;
    }

    public static String normalizeOSVersion(String value)
    {
        // Header: 'Bundle-NativeCode', Parameter: 'osversion'
        // Standardized 'osversion': major.minor.micro, only digits
        String VERSION_DELIM = ".";
        String QUALIFIER_DELIM = "-";
        int major = 0;
        int minor = 0;
        int micro = 0;
        try
        {
            StringTokenizer st = new StringTokenizer(value, VERSION_DELIM, true);
            major = Integer.parseInt(st.nextToken());

            if (st.hasMoreTokens())
            {
                st.nextToken(); // consume delimiter
                minor = Integer.parseInt(st.nextToken());

                if (st.hasMoreTokens())
                {
                    st.nextToken(); // consume delimiter
                    String microStr = st.nextToken();
                    if (microStr.indexOf(QUALIFIER_DELIM) < 0)
                    {
                        micro = Integer.parseInt(microStr);
                    }
                    else
                    {
                        micro = Integer.parseInt(microStr.substring(0, microStr
                            .indexOf(QUALIFIER_DELIM)));
                    }
                }
            }
        }
        catch (Exception ex)
        {
            return Version.emptyVersion.toString();
        }

        return major + "." + minor + "." + micro;
    }
}