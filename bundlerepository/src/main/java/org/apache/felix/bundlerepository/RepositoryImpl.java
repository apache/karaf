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
package org.apache.felix.bundlerepository;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.felix.bundlerepository.metadataparser.XmlCommonHandler;
import org.apache.felix.bundlerepository.metadataparser.kxmlsax.KXml2SAXParser;
import org.osgi.service.obr.*;

public class RepositoryImpl implements Repository
{
    private String m_name = null;
    private long m_lastmodified = 0;
    private URL m_url = null;
    private Resource[] m_resources = null;
    private int m_hopCount = 1;

    // Reusable comparator for sorting resources by name.
    private ResourceComparator m_nameComparator = new ResourceComparator();

    public RepositoryImpl(URL url) throws Exception
    {
        m_url = url;
        parseRepositoryFile(m_hopCount);
    }

    public URL getURL()
    {
        return m_url;
    }

    protected void setURL(URL url)
    {
        m_url = url;
    }

    public Resource[] getResources()
    {
        return m_resources;
    }

    public void addResource(Resource resource)
    {
        // Set resource's repository.
        ((ResourceImpl) resource).setRepository(this);

        // Add to resource array.
        if (m_resources == null)
        {
            m_resources = new Resource[] { resource };
        }
        else
        {
            Resource[] newResources = new Resource[m_resources.length + 1];
            System.arraycopy(m_resources, 0, newResources, 0, m_resources.length);
            newResources[m_resources.length] = resource;
            m_resources = newResources;
        }

        Arrays.sort(m_resources, m_nameComparator);
    }

    public String getName()
    {
        return m_name;
    }

    public void setName(String name)
    {
        m_name = name;
    }

    public long getLastModified()
    {
        return m_lastmodified;
    }

    public void setLastmodified(String s)
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddhhmmss.SSS");
        try
        {
            m_lastmodified = format.parse(s).getTime();
        }
        catch (ParseException ex)
        {
        }
    }

    /**
     * Default setter method when setting parsed data from the XML file,
     * which currently ignores everything. 
    **/
    protected Object put(Object key, Object value)
    {
        // Ignore everything for now.
        return null;
    }

    private void parseRepositoryFile(int hopCount) throws Exception
    {
// TODO: OBR - Implement hop count.
        InputStream is = null;
        BufferedReader br = null;

        try
        {
            // Do it the manual way to have a chance to 
            // set request properties as proxy auth (EW).
            URLConnection conn = m_url.openConnection(); 

            // Support for http proxy authentication
            String auth = System.getProperty("http.proxyAuth");
            if ((auth != null) && (auth.length() > 0))
            {
                if ("http".equals(m_url.getProtocol()) ||
                    "https".equals(m_url.getProtocol()))
                {
                    String base64 = Util.base64Encode(auth);
                    conn.setRequestProperty(
                        "Proxy-Authorization", "Basic " + base64);
                }
            }

            if (m_url.getPath().endsWith(".zip"))
            {
                ZipInputStream zin = new ZipInputStream(conn.getInputStream());
                ZipEntry entry = zin.getNextEntry();
                while (entry != null)
                {
                    if (entry.getName().equals("repository.xml"))
                    {
                        is = zin;
                        break;
                    }
                    entry = zin.getNextEntry();
                }
            }
            else
            {
                is = conn.getInputStream();
            } 

            if (is != null)
            {
                // Create the parser Kxml
                XmlCommonHandler handler = new XmlCommonHandler();
                Object factory = new Object() {
                    public RepositoryImpl newInstance()
                    {
                        return RepositoryImpl.this;
                    }
                };

                // Get default setter method for Repository.
                Method repoSetter = RepositoryImpl.class.getDeclaredMethod(
                    "put", new Class[] { Object.class, Object.class });

                // Get default setter method for Resource.
                Method resSetter = ResourceImpl.class.getDeclaredMethod(
                    "put", new Class[] { Object.class, Object.class });

                // Map XML tags to types.
                handler.addType("repository", factory, Repository.class, repoSetter);
                handler.addType("resource", ResourceImpl.class, Resource.class, resSetter);
                handler.addType("category", CategoryImpl.class, null, null);
                handler.addType("require", RequirementImpl.class, Requirement.class, null);
                handler.addType("capability", CapabilityImpl.class, Capability.class, null);
                handler.addType("p", PropertyImpl.class, null, null);
                handler.setDefaultType(String.class, null, null);

                br = new BufferedReader(new InputStreamReader(is));
                KXml2SAXParser parser;
                parser = new KXml2SAXParser(br);
                parser.parseXML(handler);
            }
            else
            {
                // This should not happen.
                throw new Exception("Unable to get input stream for repository.");
            }
        }
        finally
        {
            try
            {
                if (is != null) is.close();
            }
            catch (IOException ex)
            {
                // Not much we can do.
            }
        }
    }
}
