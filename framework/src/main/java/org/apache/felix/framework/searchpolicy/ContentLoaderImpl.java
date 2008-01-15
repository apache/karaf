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
import java.util.Enumeration;
import java.util.Vector;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.SecureAction;
import org.apache.felix.moduleloader.*;

public class ContentLoaderImpl implements IContentLoader
{
    private Logger m_logger = null;
    private IContent m_content = null;
    private IContent[] m_contentPath = null;
    private ISearchPolicy m_searchPolicy = null;
    private IURLPolicy m_urlPolicy = null;
    private ContentClassLoader m_classLoader = null;
    private ProtectionDomain m_protectionDomain = null;
    private static SecureAction m_secureAction = new SecureAction();

    public ContentLoaderImpl(Logger logger, IContent content,
        IContent[] contentPath)
    {
        this(logger, content, contentPath, null);
    }

    public ContentLoaderImpl(Logger logger, IContent content,
        IContent[] contentPath, ProtectionDomain protectionDomain)
    {
        m_logger = logger;
        m_content = content;
        m_contentPath = contentPath;
        m_protectionDomain = protectionDomain;
    }

    public Logger getLogger()
    {
        return m_logger;
    }

    public void open()
    {
        m_content.open();
        for (int i = 0; (m_contentPath != null) && (i < m_contentPath.length); i++)
        {
            m_contentPath[i].open();
        }
    }

    public void close()
    {
        m_content.close();
        for (int i = 0; (m_contentPath != null) && (i < m_contentPath.length); i++)
        {
            m_contentPath[i].close();
        }
    }

    public IContent getContent()
    {
        return m_content;
    }

    public IContent[] getClassPath()
    {
        return m_contentPath;
    }

    public void setSearchPolicy(ISearchPolicy searchPolicy)
    {
        m_searchPolicy = searchPolicy;
    }

    public ISearchPolicy getSearchPolicy()
    {
        return m_searchPolicy;
    }

    public void setURLPolicy(IURLPolicy urlPolicy)
    {
        m_urlPolicy = urlPolicy;
    }

    public IURLPolicy getURLPolicy()
    {
        return m_urlPolicy;
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
        for (int i = 0;
            (url == null) &&
            (i < getClassPath().length); i++)
        {
            if (getClassPath()[i].hasEntry(name))
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
        // clean or meaninful, but the Spring guys want it.
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
            for (int i = 0; i < getClassPath().length; i++)
            {
                if (getClassPath()[i].hasEntry(name))
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
        return m_contentPath[index - 1].hasEntry(urlPath);
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
        return m_contentPath[index - 1].getEntryAsStream(urlPath);
    }

    public String toString()
    {
        return m_searchPolicy.toString();
    }
}