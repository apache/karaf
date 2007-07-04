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
import java.net.URLConnection;
import java.security.Permission;

import org.apache.felix.framework.util.Util;
import org.apache.felix.moduleloader.IModule;

class URLHandlersBundleURLConnection extends URLConnection
{
    private Felix m_framework;
    private int m_contentLength;
    private long m_contentTime;
    private String m_contentType;
    private InputStream m_is;

    public URLHandlersBundleURLConnection(URL url, Felix framework)
        throws IOException
    {
        super(url);
        m_framework = framework;

        // If we don't have a framework instance, try to find
        // one from the call context.
        if (m_framework == null)
        {
            m_framework = URLHandlers.getFrameworkFromContext();
        }

        // If there is still no framework, then error.
        if (m_framework == null)
        {
            throw new IOException("Unable to find framework for URL: " + url);
        }
        // Verify that the resource pointed to be the URL exists.
        // The URL is constructed like this:
        //     bundle://<module-id>:<bundle-classpath-index>/<resource-path>
        // Where <module-id> = <bundle-id>.<revision>
        long bundleId = Util.getBundleIdFromModuleId(url.getHost());
        FelixBundle bundle = (FelixBundle) m_framework.getBundle(bundleId);
        if (bundle == null)
        {
            throw new IOException("No bundle associated with resource: " + url);
        }
        int revision = Util.getModuleRevisionFromModuleId(url.getHost());
        IModule[] modules = bundle.getInfo().getModules();
        if ((modules == null) || (revision < 0) || (revision >= modules.length) ||
            !modules[revision].getContentLoader().hasInputStream(url.getPort(), url.getPath()))
        {
            throw new IOException("Resource does not exist: " + url);
        }
    }

    public void connect() throws IOException
    {
        if (!connected)
        {
            // The URL is constructed like this:
        //     bundle://<module-id>:<module-classpath-index>/<resource-path>
            // Where <module-id> = <bundle-id>.<revision>
            long bundleId = Util.getBundleIdFromModuleId(url.getHost());
            FelixBundle bundle = (FelixBundle) m_framework.getBundle(bundleId);
            if (bundle == null)
            {
                throw new IOException("No bundle associated with resource: " + url);
            }
            int revision = Util.getModuleRevisionFromModuleId(url.getHost());
            IModule[] modules = bundle.getInfo().getModules();
            if ((modules == null) || (revision < 0) || (revision >= modules.length))
            {
                throw new IOException("Resource does not exist: " + url);
            }
            m_is = bundle.getInfo().getModules()[revision]
                .getContentLoader().getInputStream(url.getPort(), url.getPath());
            m_contentLength = (m_is == null) ? 0 : m_is.available();
            m_contentTime = 0L;
            m_contentType = URLConnection.guessContentTypeFromName(url.getFile());
            connected = true;
        }
    }

    public InputStream getInputStream()
        throws IOException
    {
        if (!connected)
        {
            connect();
        }
        return m_is;
    }

    public int getContentLength()
    {
        if (!connected)
        {
            try
            {
                connect();
            }
            catch(IOException ex)
            {
                return -1;
            }
        }
        return m_contentLength;
    }

    public long getLastModified()
    {
        if (!connected)
        {
            try
            {
                connect();
            }
            catch(IOException ex)
            {
                return 0;
            }
        }
        if (m_contentTime != -1L)
        {
            return m_contentTime;
        }
        else
        {
            return 0L;
        }
    }

    public String getContentType()
    {
        if (!connected)
        {
            try
            {
                connect();
            }
            catch (IOException ex)
            {
                return null;
            }
        }
        return m_contentType;
    }

    public Permission getPermission()
    {
        // TODO: This should probably return a FilePermission
        // to access the bundle JAR file, but we don't have the
        // necessary information here to construct the absolute
        // path of the JAR file...so it would take some
        // re-arranging to get this to work.
        return null;
    }
}