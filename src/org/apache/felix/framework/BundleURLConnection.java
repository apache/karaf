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
package org.apache.felix.framework;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;

import org.apache.felix.moduleloader.*;

class BundleURLConnection extends URLConnection
{
    private ModuleManager m_mgr = null;
    private int contentLength;
    private long contentTime;
    private String contentType;
    private InputStream is;

    public BundleURLConnection(ModuleManager mgr, URL url)
    {
        super(url);
        m_mgr = mgr;
    }

    public void connect() throws IOException
    {
        if (!connected)
        {
            // The URL is constructed like this:
            // bundle://<module-id>/<source-idx>/<resource-path>

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

            is = new ByteArrayInputStream(bytes);
            contentLength = bytes.length;
            contentTime = 0L;  // TODO: Change this.
            contentType = URLConnection.guessContentTypeFromName(resource);
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
        return is;
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
        return contentLength;
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
        if(contentTime != -1L)
            return contentTime;
        else
            return 0L;
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
        return contentType;
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