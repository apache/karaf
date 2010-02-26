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
package org.apache.felix.bundlerepository.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.metadataparser.XmlCommonHandler;
import org.apache.felix.bundlerepository.impl.metadataparser.kxmlsax.KXml2SAXParser;
import org.apache.felix.bundlerepository.Repository;

public class RepositoryImpl implements Repository
{
    public static final String OBR_PARSER_CLASS = "obr.parser.class";
    public static final String OBR_PARSER_CLASS_DEFAULT = "org.apache.felix.bundlerepository.impl.StaxParser";

    private String m_name = null;
    private long m_lastmodified = 0;
    private URL m_url = null;
    private final Logger m_logger;
    private Resource[] m_resources = null;
    private Referral[] m_referrals = null;
    private RepositoryAdminImpl m_repoAdmin = null;
    private List m_resourceList;

    // Reusable comparator for sorting resources by name.
    private ResourceComparator m_nameComparator = new ResourceComparator();

    public RepositoryImpl(RepositoryAdminImpl repoAdmin, URL url, Logger logger)
        throws Exception
    {
        this(repoAdmin, url, Integer.MAX_VALUE, logger);
    }

    public RepositoryImpl(RepositoryAdminImpl repoAdmin, URL url, final int hopCount, Logger logger)
        throws Exception
    {
        m_repoAdmin = repoAdmin;
        m_url = url;
        m_logger = logger;
        m_resourceList = new ArrayList();
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws Exception
                {
                    parseRepositoryFile(hopCount);
                    return null;
                }
            });
        }
        catch (PrivilegedActionException ex)
        {
            throw (Exception) ex.getCause();
        }
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
        if (m_resources == null) {
            Collections.sort(m_resourceList, m_nameComparator);
            m_resources = (Resource[]) m_resourceList.toArray(new Resource[m_resourceList.size()]);
        }
        return m_resources;
    }

    public void addResource(Resource resource)
    {
        // Set resource's repository.
        ((ResourceImpl) resource).setRepository(this);

        // Add to resource array.
        m_resourceList.add(resource);
        m_resources = null;
    }

    public Referral[] getReferrals()
    {
        return m_referrals;
    }

    public void addReferral(Referral referral) throws Exception
    {
        // Add to resource array.
        if (m_referrals == null)
        {
            m_referrals = new Referral[] { referral };
        }
        else
        {
            Referral[] newResources = new Referral[m_referrals.length + 1];
            System.arraycopy(m_referrals, 0, newResources, 0, m_referrals.length);
            newResources[m_referrals.length] = referral;
            m_referrals = newResources;
        }
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

    public boolean isLocal()
    {
        return false;
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

    protected void parseRepositoryFile(int hopCount) throws Exception
    {
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
                if ("http".equals(m_url.getProtocol()) || "https".equals(m_url.getProtocol()))
                {
                    String base64 = Util.base64Encode(auth);
                    conn.setRequestProperty("Proxy-Authorization", "Basic " + base64);
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
                parseRepository(is);


                // resolve referrals
                hopCount--;
                if (hopCount > 0 && m_referrals != null)
                {
                    for (int i = 0; i < m_referrals.length; i++)
                    {
                        Referral referral = m_referrals[i];

                        URL url = new URL(getURL(), referral.getUrl());
                        hopCount = (referral.getDepth() > hopCount) ? hopCount : referral.getDepth();

                        m_repoAdmin.addRepository(url, hopCount);
                    }
                }
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
                if (is != null)
                {
                    is.close();
                }
            }
            catch (IOException ex)
            {
                // Not much we can do.
            }
        }
    }

    protected void parseRepository(InputStream is) throws Exception
    {
        RepositoryParser parser = null;
        try
        {
            String className = (String) RepositoryAdminImpl.m_context.getProperty(OBR_PARSER_CLASS);
            if (className == null || className.length() == 0)
            {
                className = OBR_PARSER_CLASS_DEFAULT;
            }
            parser = (RepositoryParser) Class.forName(className).newInstance();
        }
        catch (Throwable t)
        {
        }
        if (parser == null)
        {
            parser = new KXml2Parser();

        }
        parser.parse(this, is);
    }

    public interface RepositoryParser
    {
        void parse(RepositoryImpl repository, InputStream is) throws Exception;
    }

    public static class KXml2Parser implements RepositoryParser
    {
        public void parse(final RepositoryImpl repository, final InputStream is) throws Exception
        {
            BufferedReader br;// Create the parser Kxml
            XmlCommonHandler handler = new XmlCommonHandler(repository.m_logger);
            Object factory = new Object()
            {
                public RepositoryImpl newInstance()
                {
                    return repository;
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
            handler.addType("referral", Referral.class, null, null);
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
    }
}