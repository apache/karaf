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

package org.apache.felix.sigil.obr.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.felix.sigil.repository.AbstractBundleRepository;
import org.xml.sax.SAXException;

public abstract class AbstractOBRBundleRepository extends AbstractBundleRepository
{
    private static SAXParserFactory factory = SAXParserFactory.newInstance();

    private URL obrURL;
    private File obrlCache;
    private File bundleCache;
    private long updatePeriod;
    private final File authFile;
    private final Properties authMap = new Properties();
    private long authLastModified;

    public AbstractOBRBundleRepository(String id, URL repositoryURL, File obrCache, File bundleCache, long updatePeriod, File authFile)
    {
        super(id);
        this.obrURL = repositoryURL;
        this.obrlCache = (repositoryURL.getProtocol().equals("file") ? null : obrCache);
        this.bundleCache = bundleCache;
        this.updatePeriod = updatePeriod;
        this.authFile = authFile;
    }

    public void refresh()
    {
        if (obrlCache != null)
            obrlCache.delete();
    }

    protected void readBundles(OBRListener listener)
    {
        File index = syncOBRIndex();
        OBRHandler handler = new OBRHandler(getObrURL(), getBundleCache(), listener);
        try
        {
            SAXParser parser = factory.newSAXParser();
            if (getObrlCache() != null)
                parser.parse(index, handler);
            else
                parser.parse(getObrURL().toExternalForm(), handler);
        }
        catch (ParserConfigurationException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (SAXException e)
        {
            System.out.println("Failed to parse " + index);
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String getAuth(String url) throws IOException
    {
        if (authFile == null)
            return null;

        if (authFile.lastModified() > authLastModified)
        {
            authMap.clear();
            authMap.load(new FileInputStream(authFile));
        }

        String authKey = "";

        for (Object okey : authMap.keySet())
        {
            String key = (String) okey;
            if (url.startsWith(key) && key.length() > authKey.length())
                authKey = key;
        }

        return authMap.getProperty(authKey);
    }

    private File syncOBRIndex()
    {
        File file = null;
        if ("file".equals(getObrURL().getProtocol()))
        {
            try
            {
                file = new File(getObrURL().toURI());
            }
            catch (URISyntaxException e)
            {
                // should be impossible ?
                throw new IllegalStateException("Failed to convert file url to uri", e);
            }
        }
        else
        {
            file = getObrlCache();
            if (isUpdated())
            {
                cacheIndex(file);
            }
        }

        return file;
    }

    private void cacheIndex(File file)
    {
        InputStream in = null;
        OutputStream out = null;

        try
        {
            URL url = getObrURL();
            URLConnection c = url.openConnection();
            String auth = getAuth(url.toString());
            if (auth != null)
            {
                c.setRequestProperty("Authorization", "Basic " + auth);
            }

            c.connect();
            in = c.getInputStream();
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
            {
                throw new IOException("Failed to create obr cache dir "
                    + file.getParentFile());
            }
            out = new FileOutputStream(file);
            stream(in, out);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            getObrlCache().setLastModified(0);
        }
        finally
        {
            close(in, out);
        }
    }

    private void close(InputStream in, OutputStream out)
    {
        if (in != null)
        {
            try
            {
                in.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (out != null)
        {
            try
            {
                out.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void stream(InputStream in, OutputStream out) throws IOException
    {
        byte[] buf = new byte[1024];
        for (;;)
        {
            int r = in.read(buf);
            if (r == -1)
            {
                break;
            }
            out.write(buf, 0, r);
        }
        out.flush();
    }

    private boolean isUpdated()
    {
        if (getObrlCache() == null)
            return false;

        if (!getObrlCache().exists())
            return true;

        return getObrlCache().lastModified() + getUpdatePeriod() < System.currentTimeMillis();
    }

    private URL getObrURL()
    {
        return obrURL;
    }

    private File getObrlCache()
    {
        return obrlCache;
    }

    private File getBundleCache()
    {
        return bundleCache;
    }

    private long getUpdatePeriod()
    {
        return updatePeriod;
    }
}
