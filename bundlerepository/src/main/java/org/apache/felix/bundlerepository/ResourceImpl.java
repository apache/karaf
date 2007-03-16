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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.osgi.framework.Version;
import org.osgi.service.obr.*;

public class ResourceImpl implements Resource
{
    private final String URI = "uri";

    private Repository m_repo = null;
    private Map m_map = null;
    private List m_catList = new ArrayList();
    private List m_capList = new ArrayList();
    private List m_reqList = new ArrayList();

    private String m_resourceURI = null;
    private String m_docURI = null;
    private String m_licenseURI = null;
    private String m_sourceURI = null;
    private boolean m_converted = false;

    public ResourceImpl()
    {
        this(null);
    }

    public ResourceImpl(ResourceImpl resource)
    {
        m_map = new TreeMap(new Comparator() {
            public int compare(Object o1, Object o2)
            {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }
        });

        if (resource != null)
        {
            m_map.putAll(resource.getProperties());
            m_catList.addAll(resource.m_catList);
            m_capList.addAll(resource.m_capList);
            m_reqList.addAll(resource.m_reqList);
        }
    }

    public boolean equals(Object o)
    {
        if (o instanceof Resource)
        {
            return ((Resource) o).getSymbolicName().equals(getSymbolicName())
                && ((Resource) o).getVersion().equals(getVersion());
        }
        return false;
    }

    public int hashCode()
    {
        return getSymbolicName().hashCode() ^ getVersion().hashCode();
    }

    public Map getProperties()
    {
        if (!m_converted)
        {
            convertURItoURL();
        }
        return m_map;
    }

    public String getPresentationName()
    {
        return (String) m_map.get(PRESENTATION_NAME);
    }

    public String getSymbolicName()
    {
        return (String) m_map.get(SYMBOLIC_NAME);
    }

    public String getId()
    {
        return (String) m_map.get(ID);
    }

    public Version getVersion()
    {
        return (Version) m_map.get(VERSION);
    }

    public URL getURL()
    {
        if (!m_converted)
        {
            convertURItoURL();
        }
        return (URL) m_map.get(URL);
    }

    public Requirement[] getRequirements()
    {
        return (Requirement[]) m_reqList.toArray(new Requirement[m_reqList.size()]);
    }

    protected void addRequire(Requirement req)
    {
        m_reqList.add(req);
    }

    public Capability[] getCapabilities()
    {
        return (Capability[]) m_capList.toArray(new Capability[m_capList.size()]);
    }

    protected void addCapability(Capability cap)
    {
        m_capList.add(cap);
    }

    public String[] getCategories()
    {
        return (String[]) m_catList.toArray(new String[m_catList.size()]);
    }

    protected void addCategory(CategoryImpl cat)
    {
        m_catList.add(cat.getId());
    }

    public Repository getRepository()
    {
        return m_repo;
    }

    protected void setRepository(Repository repo)
    {
        m_repo = repo;
    }

    /**
     * Default setter method when setting parsed data from the XML file. 
    **/
    protected Object put(Object key, Object value)
    {
        // Capture the URIs since they might be relative, so we
        // need to defer setting the actual URL value until they
        // are used so that we will know our repository and its
        // base URL.
        if (key.equals(LICENSE_URL))
        {
            m_licenseURI = (String) value;
        }
        else if (key.equals(DOCUMENTATION_URL))
        {
            m_docURI = (String) value;
        }
        else if (key.equals(SOURCE_URL))
        {
            m_sourceURI = (String) value;
        }
        else if (key.equals(URI))
        {
            m_resourceURI = (String) value;
        }
        else
        {
            if (key.equals(VERSION))
            {
                value = new Version(value.toString());
            }
            else if (key.equals(SIZE))
            {
                value = Long.valueOf(value.toString());
            }
    
            return m_map.put(key, value);
        }

        return null;
    }

    private void convertURItoURL()
    {
        if (m_repo != null)
        {
            try
            {
                URL base = m_repo.getURL();
                if (m_resourceURI != null)
                {
                    m_map.put(URL, new URL(base, m_resourceURI));
                }
                if (m_docURI != null)
                {
                    m_map.put(DOCUMENTATION_URL, new URL(base, m_docURI));
                }
                if (m_licenseURI != null)
                {
                    m_map.put(LICENSE_URL, new URL(base, m_licenseURI));
                }
                if (m_sourceURI != null)
                {
                    m_map.put(SOURCE_URL, new URL(base, m_sourceURI));
                }
                m_converted = true;
            }
            catch (MalformedURLException ex)
            {
                ex.printStackTrace(System.err);
            }
        }
    }
}