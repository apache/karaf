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

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.cache.BundleCache;
import org.osgi.framework.Constants;

public class R4Library
{
    private Logger m_logger = null;
    private BundleCache m_cache = null;
    private long m_bundleId = -1;
    private int m_revision = -1;
    private String m_os = null;
    private String m_processor = null;
    private R4LibraryHeader m_header = null;

    public R4Library(
        Logger logger, BundleCache cache, long bundleId, int revision,
        String os, String processor, R4LibraryHeader header)
    {
        m_logger = logger;
        m_cache = cache;
        m_bundleId = bundleId;
        m_revision = revision;
        m_os = normalizePropertyValue(Constants.FRAMEWORK_OS_NAME, os);
        m_processor = normalizePropertyValue(Constants.FRAMEWORK_PROCESSOR, processor);
        m_header = header;
    }

    /**
     * <p>
     * Returns a file system path to the specified library.
     * </p>
     * @param name the name of the library that is being requested.
     * @return a file system path to the specified library.
    **/
    public String getPath(String name)
    {
        if (m_header != null)
        {
            String libname = System.mapLibraryName(name);

            // Check to see if the library matches.
            boolean osOkay = checkOS(m_header.getOSNames());
            boolean procOkay = checkProcessor(m_header.getProcessors());
            if (m_header.getName().endsWith(libname) && osOkay && procOkay)
            {
                try {
                    return m_cache.getArchive(m_bundleId)
                        .getRevision(m_revision).findLibrary(m_header.getName());
                } catch (Exception ex) {
                    m_logger.log(Logger.LOG_ERROR, "R4Library: Error finding library.", ex);
                }
            }
        }

        return null;
    }

    private boolean checkOS(String[] osnames)
    {
        for (int i = 0; (osnames != null) && (i < osnames.length); i++)
        {
            String osname =
                normalizePropertyValue(Constants.FRAMEWORK_OS_NAME, osnames[i]);
            if (m_os.equals(osname))
            {
                return true;
            }
        }
        return false;
    }

    private boolean checkProcessor(String[] processors)
    {
        for (int i = 0; (processors != null) && (i < processors.length); i++)
        {
            String processor =
                normalizePropertyValue(Constants.FRAMEWORK_PROCESSOR, processors[i]);
            if (m_processor.equals(processor))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * This is simply a hack to try to create some standardized
     * property values, since there seems to be many possible
     * values for each JVM implementation.  Currently, this
     * focuses on Windows and Linux and will certainly need
     * to be changed in the future or at least edited.
    **/
    public static String normalizePropertyValue(String prop, String value)
    {
        prop = prop.toLowerCase();
        value = value.toLowerCase();

        if (prop.equals(Constants.FRAMEWORK_OS_NAME))
        {
            if (value.startsWith("linux"))
            {
                return "linux";
            }
            else if (value.startsWith("win"))
            {
                String os = "win";
                if (value.indexOf("95") >= 0)
                {
                    os = "win95";
                }
                else if (value.indexOf("98") >= 0)
                {
                    os = "win98";
                }
                else if (value.indexOf("NT") >= 0)
                {
                    os = "winnt";
                }
                else if (value.indexOf("2000") >= 0)
                {
                    os = "win2000";
                }
                else if (value.indexOf("xp") >= 0)
                {
                    os = "winxp";
                }
                return os;
            }
        }
        else if (prop.equals(Constants.FRAMEWORK_PROCESSOR))
        {
            if (value.endsWith("86"))
            {
                return "x86";
            }
        }

        return value;
    }
}