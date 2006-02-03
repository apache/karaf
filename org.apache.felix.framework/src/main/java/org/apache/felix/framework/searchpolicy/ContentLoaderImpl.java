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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.felix.framework.Logger;
import org.apache.felix.moduleloader.*;

public class ContentLoaderImpl implements IContentLoader
{
    private Logger m_logger = null;
    private IContent m_content = null;
    private IContent[] m_contentPath = null;
    private ISearchPolicy m_searchPolicy = null;
    private IURLPolicy m_urlPolicy = null;
    private ContentClassLoader m_classLoader = null;

    public ContentLoaderImpl(Logger logger, IContent content, IContent[] contentPath)
    {
        m_logger = logger;
        m_content = content;
        m_contentPath = contentPath;
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
        if (m_classLoader == null)
        {
            m_classLoader = new ContentClassLoader(this);
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
        if (m_classLoader == null)
        {
            m_classLoader = new ContentClassLoader(this);
        }

        return m_classLoader.getResourceFromModule(name);
    }

    public InputStream getResourceAsStream(String name)
        throws IOException
    {
        if (name.startsWith("/"))
        {
            name = name.substring(1);
        }
        // The name is the path contructed like this:
        // <class-path-index> / <relative-resource-path>
        int idx = Integer.parseInt(name.substring(0, name.indexOf('/')));
        name = name.substring(name.indexOf('/') + 1);
        return m_contentPath[idx].getEntryAsStream(name);
    }

    public String toString()
    {
        return m_searchPolicy.toString();
    }
}