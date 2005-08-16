/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.osgi.moduleloader;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;

class ModuleURLConnection extends URLConnection
{
    private ModuleManager m_mgr = null;
    private int m_contentLength;
    private long m_contentTime;
    private String m_contentType;
    private InputStream m_is;

    public ModuleURLConnection(ModuleManager mgr, URL url)
    {
        super(url);
        m_mgr = mgr;
    }

    public void connect() throws IOException
    {
        if (!connected)
        {
            // The URL is constructed like this:
            // module://<module-id>/<source-idx>/<resource-path>
            Module module = m_mgr.getModule(url.getHost());
            if (module == null)
            {
                throw new IOException("Unable to find bundle's module.");
            }

            String resource = url.getFile();
            if (resource == null)
            {
                throw new IOException("Unable to find resource: " + url.toString());
            }
            if (resource.startsWith("/"))
            {
                resource = resource.substring(1);
            }
            int rsIdx = -1;
            try
            {
                rsIdx = Integer.parseInt(resource.substring(0, resource.indexOf("/")));
            }
            catch (NumberFormatException ex)
            {
                new IOException("Error parsing resource index.");
            }
            resource = resource.substring(resource.indexOf("/") + 1);

            // Get the resource bytes from the resource source.
            byte[] bytes = null;
            ResourceSource[] resSources = module.getResourceSources();
            if ((resSources != null) && (rsIdx < resSources.length))
            {
                if (resSources[rsIdx].hasResource(resource))
                {
                    bytes = resSources[rsIdx].getBytes(resource);
                }
            }

            if (bytes == null)
            {
                throw new IOException("Unable to find resource: " + url.toString());
            }

            m_is = new ByteArrayInputStream(bytes);
            m_contentLength = bytes.length;
            m_contentTime = 0L;  // TODO: Change this.
            m_contentType = URLConnection.guessContentTypeFromName(resource);
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
            try {
                connect();
            } catch(IOException ex) {
                return -1;
            }
        }
        return m_contentLength;
    }

    public long getLastModified()
    {
        if (!connected)
        {
            try {
                connect();
            } catch(IOException ex) {
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
            try {
                connect();
            } catch(IOException ex) {
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