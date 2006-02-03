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
package org.apache.felix.moduleloader;

import java.net.URL;

import org.apache.felix.framework.Logger;

public class ModuleImpl implements IModule
{
    private Logger m_logger = null;
    private String m_id = null;
    private IContentLoader m_contentLoader = null;

    ModuleImpl(Logger logger, String id)
    {
        m_logger = logger;
        m_id = id;
    }

    public String getId()
    {
        return m_id;
    }

    public IContentLoader getContentLoader()
    {
        return m_contentLoader;
    }

    protected void setContentLoader(IContentLoader contentLoader)
    {
        m_contentLoader = contentLoader;
    }

    public Class getClass(String name)
    {
        try
        {
            return m_contentLoader.getSearchPolicy().findClass(name);
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
        try
        {
            return m_contentLoader.getSearchPolicy().findResource(name);
        }
        catch (ResourceNotFoundException ex)
        {
            m_logger.log(
                Logger.LOG_WARNING,
                ex.getMessage(),
                ex);
        }
        return null;
    }

    public String toString()
    {
        return m_id;
    }
}