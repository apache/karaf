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
package org.apache.felix.framework.searchpolicy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import java.util.jar.Manifest;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.SecureAction;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.apache.felix.moduleloader.*;

public class ContentLoaderImpl implements IContentLoader
{
    private final Logger m_logger;
    private final IContent m_content;
    private IContent[] m_contentPath;
    private IContent[] m_fragmentContents = null;
    private ISearchPolicy m_searchPolicy = null;
    private IURLPolicy m_urlPolicy = null;
    private ContentClassLoader m_classLoader;
    private ProtectionDomain m_protectionDomain = null;
    private static SecureAction m_secureAction = new SecureAction();

    public ContentLoaderImpl(Logger logger, IContent content)
    {
        m_logger = logger;
        m_content = content;
    }

    public Logger getLogger()
    {
        return m_logger;
    }

    public synchronized void close()
    {
        m_content.close();
        for (int i = 0; (m_contentPath != null) && (i < m_contentPath.length); i++)
        {
            m_contentPath[i].close();
        }
        for (int i = 0; (m_fragmentContents != null) && (i < m_fragmentContents.length); i++)
        {
            m_fragmentContents[i].close();
        }
    }

    public IContent getContent()
    {
        return m_content;
    }

    public synchronized IContent[] getClassPath()
    {
        if (m_contentPath == null)
        {
            try
            {
                m_contentPath = initializeContentPath();
            }
            catch (Exception ex)
            {
                m_logger.log(Logger.LOG_ERROR, "Unable to get module class path.", ex);
            }
        }
        return m_contentPath;
    }

    public synchronized void attachFragmentContents(IContent[] fragmentContents)
        throws Exception
    {
        // Close existing fragment contents.
        if (m_fragmentContents != null)
        {
            for (int i = 0; i < m_fragmentContents.length; i++)
            {
                m_fragmentContents[i].close();
            }
        }
        m_fragmentContents = fragmentContents;

        if (m_contentPath != null)
        {
            for (int i = 0; i < m_contentPath.length; i++)
            {
                m_contentPath[i].close();
            }
        }
        m_contentPath = initializeContentPath();
    }

    public synchronized void setSearchPolicy(ISearchPolicy searchPolicy)
    {
        m_searchPolicy = searchPolicy;
    }

    public synchronized ISearchPolicy getSearchPolicy()
    {
        return m_searchPolicy;
    }

    public synchronized void setURLPolicy(IURLPolicy urlPolicy)
    {
        m_urlPolicy = urlPolicy;
    }

    public synchronized IURLPolicy getURLPolicy()
    {
        return m_urlPolicy;
    }

    public synchronized void setSecurityContext(Object securityContext)
    {
        m_protectionDomain = (ProtectionDomain) securityContext;
    }

    public synchronized Object getSecurityContext()
    {
        return m_protectionDomain;
    }

    public Class getClass(String name)
    {
        synchronized (this)
        {
            if (m_classLoader == null)
            {
                m_classLoader = m_secureAction.createContentClassLoader(this,
                    m_protectionDomain);
            }
        }

        try
        {
            return m_classLoader.loadClassFromModule(name);
        }
        catch (ClassNotFoundException ex)
        {
            return null;
        }
    }

    public URL getResource(String name)
    {
        URL url = null;

        // Remove leading slash, if present, but special case
        // "/" so that it returns a root URL...this isn't very
        // clean or meaninful, but the Spring guys want it.
        if (name.equals("/"))
        {
            // Just pick a class path index since it doesn't really matter.
            url = getURLPolicy().createURL(1, name);
        }
        else if (name.startsWith("/"))
        {
            name = name.substring(1);
        }

        // Check the module class path.
        IContent[] contentPath = getClassPath();
        for (int i = 0;
            (url == null) &&
            (i < contentPath.length); i++)
        {
            if (contentPath[i].hasEntry(name))
            {
                url = getURLPolicy().createURL(i + 1, name);
            }
        }

        return url;
    }

    public Enumeration getResources(String name)
    {
        Vector v = new Vector();

        // Special case "/" so that it returns a root URLs for
        // each bundle class path entry...this isn't very
        // clean or meaningful, but the Spring guys want it.
        if (name.equals("/"))
        {
            for (int i = 0; i < getClassPath().length; i++)
            {
                v.addElement(getURLPolicy().createURL(i + 1, name));
            }
        }
        else
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            // Check the module class path.
            IContent[] contentPath = getClassPath();
            for (int i = 0; i < contentPath.length; i++)
            {
                if (contentPath[i].hasEntry(name))
                {
                    // Use the class path index + 1 for creating the path so
                    // that we can differentiate between module content URLs
                    // (where the path will start with 0) and module class
                    // path URLs.
                    v.addElement(getURLPolicy().createURL(i + 1, name));
                }
            }
        }

        return v.elements();
    }

    // TODO: API: Investigate how to handle this better, perhaps we need
    // multiple URL policies, one for content -- one for class path.
    public URL getResourceFromContent(String name)
    {
        URL url = null;

        // Check for the special case of "/", which represents
        // the root of the bundle according to the spec.
        if (name.equals("/"))
        {
            url = getURLPolicy().createURL(0, "/");
        }

        if (url == null)
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            // Check the module content.
            if (getContent().hasEntry(name))
            {
                // Module content URLs start with 0, whereas module
                // class path URLs start with the index into the class
                // path + 1.
                url = getURLPolicy().createURL(0, name);
            }
        }

        return url;
    }

    public boolean hasInputStream(int index, String urlPath)
    {
        if (urlPath.startsWith("/"))
        {
            urlPath = urlPath.substring(1);
        }
        if (index == 0)
        {
            return m_content.hasEntry(urlPath);
        }
        return getClassPath()[index - 1].hasEntry(urlPath);
    }

    public InputStream getInputStream(int index, String urlPath)
        throws IOException
    {
        if (urlPath.startsWith("/"))
        {
            urlPath = urlPath.substring(1);
        }
        if (index == 0)
        {
            return m_content.getEntryAsStream(urlPath);
        }
        return getClassPath()[index - 1].getEntryAsStream(urlPath);
    }

    public synchronized String toString()
    {
        return m_searchPolicy.toString();
    }

    private IContent[] initializeContentPath() throws Exception
    {
        List contentList = new ArrayList();
        calculateContentPath(m_content, contentList, true);
        for (int i = 0; (m_fragmentContents != null) && (i < m_fragmentContents.length); i++)
        {
            calculateContentPath(m_fragmentContents[i], contentList, false);
        }
        return (IContent[]) contentList.toArray(new IContent[contentList.size()]);
    }

    private List calculateContentPath(IContent content, List contentList, boolean searchFragments)
        throws Exception
    {
        // Creating the content path entails examining the bundle's
        // class path to determine whether the bundle JAR file itself
        // is on the bundle's class path and then creating content
        // objects for everything on the class path.

        // Create a list to contain the content path for the specified content.
        List localContentList = new ArrayList();

        // Get the bundle's manifest header.
        InputStream is = null;
        Map headers = null;
        try
        {
// TODO: REFACTOR - It seems that we shouldn't have to get the manifest
//       here since we already have it in our module definition, but we
//       don't have access to the module definition here. This is something
//       to be considered when we refactor the module layer.
            is = content.getEntryAsStream("META-INF/MANIFEST.MF");
            headers = new StringMap(new Manifest(is).getMainAttributes(), false);
        }
        finally
        {
            if (is != null) is.close();
        }

        // Find class path meta-data.
        String classPath = (headers == null)
            ? null : (String) headers.get(FelixConstants.BUNDLE_CLASSPATH);
        // Parse the class path into strings.
        String[] classPathStrings = ManifestParser.parseDelimitedString(
            classPath, FelixConstants.CLASS_PATH_SEPARATOR);

        if (classPathStrings == null)
        {
            classPathStrings = new String[0];
        }

        // Create the bundles class path.
        for (int i = 0; i < classPathStrings.length; i++)
        {
            // Remove any leading slash, since all bundle class path
            // entries are relative to the root of the bundle.
            classPathStrings[i] = (classPathStrings[i].startsWith("/"))
                ? classPathStrings[i].substring(1)
                : classPathStrings[i];

            // Check for the bundle itself on the class path.
            if (classPathStrings[i].equals(FelixConstants.CLASS_PATH_DOT))
            {
                localContentList.add(content);
            }
            else
            {
                // Try to find the embedded class path entry in the current
                // content.
                IContent embeddedContent = content.getEntryAsContent(classPathStrings[i]);
                // If the embedded class path entry was not found, it might be
                // in one of the fragments if the current content is the bundle,
                // so try to search the fragments if necessary.
                for (int fragIdx = 0;
                    searchFragments && (embeddedContent == null)
                        && (m_fragmentContents != null) && (fragIdx < m_fragmentContents.length);
                    fragIdx++)
                {
                    embeddedContent = m_fragmentContents[fragIdx].getEntryAsContent(classPathStrings[i]);
                }
                // If we found the embedded content, then add it to the
                // class path content list.
                if (embeddedContent != null)
                {
                    localContentList.add(embeddedContent);
                }
                else
                {
// TODO: FRAMEWORK - Per the spec, this should fire a FrameworkEvent.INFO event;
//       need to create an "Eventer" class like "Logger" perhaps.
                    m_logger.log(Logger.LOG_INFO,
                        "Class path entry not found: "
                        + classPathStrings[i]);
                }
            }
        }

        // If there is nothing on the class path, then include
        // "." by default, as per the spec.
        if (localContentList.size() == 0)
        {
            localContentList.add(content);
        }

        // Now add the local contents to the global content list and return it.
        contentList.addAll(localContentList);
        return contentList;
    }
}