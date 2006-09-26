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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.apache.felix.moduleloader.*;

public class SystemBundleContentLoader implements IContentLoader
{
    private Logger m_logger = null;
    private ISearchPolicy m_searchPolicy = null;
    private IURLPolicy m_urlPolicy = null;

    public SystemBundleContentLoader(Logger logger)
    {
        m_logger = logger;
    }

    public void open()
    {
        // Nothing needed here.
    }

    public void close()
    {
        // Nothing needed here.
    }

    public IContent getContent()
    {
        return null;
    }

    public ISearchPolicy getSearchPolicy()
    {
        return m_searchPolicy;
    }

    public void setSearchPolicy(ISearchPolicy searchPolicy)
    {
        m_searchPolicy = searchPolicy;
    }

    public IURLPolicy getURLPolicy()
    {
        return m_urlPolicy;
    }

    public void setURLPolicy(IURLPolicy urlPolicy)
    {
        m_urlPolicy = urlPolicy;
    }

    public Class getClass(String name)
    {
        try
        {
            return getClass().getClassLoader().loadClass(name);
        }
        catch (ClassNotFoundException ex)
        {
            m_logger.log(
                Logger.LOG_WARNING,
                ex.getMessage(),
                ex);
        }
        return null;
    }

    public URL getResource(String name)
    {
        return getClass().getClassLoader().getResource(name);
    }

    public Enumeration getResources(String name)
    {
       try
       {
           return getClass().getClassLoader().getResources(name);
       }
       catch (IOException ex)
       {
           return null;
       }
    }

    public URL getResourceFromContent(String name)
    {
        // There is no content for the system bundle, so return null.
        return null;
    }

    public boolean hasInputStream(String urlPath) throws IOException
    {
        return (getClass().getClassLoader().getResource(urlPath) != null);
    }

    public InputStream getInputStream(String urlPath) throws IOException
    {
        return getClass().getClassLoader().getResourceAsStream(urlPath);
    }

    public String findLibrary(String name)
    {
        // No native libs associated with the system bundle.
        return null;
    }
}