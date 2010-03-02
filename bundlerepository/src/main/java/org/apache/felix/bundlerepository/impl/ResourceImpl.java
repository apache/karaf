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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Version;

public class ResourceImpl implements Resource
{

    private final Map m_map = new HashMap();
    private Repository m_repo;
    private List m_capList = new ArrayList();
    private List m_reqList = new ArrayList();

    private String m_resourceURI = null;
    private String m_docURI = null;
    private String m_licenseURI = null;
    private String m_sourceURI = null;
    private String m_javadocURI = null;
    private boolean m_converted = false;

    private int m_hash;

    public ResourceImpl()
    {
        this(null);
    }

    public ResourceImpl(ResourceImpl resource)
    {
        if (resource != null)
        {
            m_map.putAll(resource.getProperties());
            m_capList.addAll(resource.m_capList);
            m_reqList.addAll(resource.m_reqList);
        }
    }

    public boolean equals(Object o)
    {
        if (o instanceof Resource)
        {
            if (getSymbolicName() == null || getVersion() == null)
            {
                return this == o;
            }
            return getSymbolicName().equals(((Resource) o).getSymbolicName())
                && getVersion().equals(((Resource) o).getVersion());
        }
        return false;
    }

    public int hashCode()
    {
        if (m_hash == 0)
        {
            if (getSymbolicName() == null || getVersion() == null)
            {
                m_hash =  super.hashCode();
            }
            else
            {
                m_hash = getSymbolicName().hashCode() ^ getVersion().hashCode();
            }
        }
        return m_hash;
    }

    public Repository getRepository() {
        return m_repo;
    }

    public void setRepository(Repository repository) {
        this.m_repo = repository;
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
        Version v = (Version) m_map.get(VERSION);
        v = (v == null) ? Version.emptyVersion : v;
        return v;
    }

    public URL getURL()
    {
        if (!m_converted)
        {
            convertURItoURL();
        }
        return (URL) m_map.get(Resource.URI);
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
        List catList = (List) m_map.get(CATEGORY);
        if (catList == null)
        {
            return new String[0];
        }
        return (String[]) catList.toArray(new String[catList.size()]);
    }

    protected void addCategory(CategoryImpl cat)
    {
        List catList = (List) m_map.get(CATEGORY);
        if (catList == null)
        {
            catList = new ArrayList();
            m_map.put(CATEGORY, catList);
        }
        catList.add(cat.getId());
    }

    public boolean isLocal()
    {
        return false;
    }

    /**
     * Default setter method when setting parsed data from the XML file. 
    **/
    protected Object put(Object key, Object value)
    {
        key = key.toString().toLowerCase();
        m_converted = false;
        m_hash = 0;
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
        else if (key.equals(JAVADOC_URL))
        {
            m_javadocURI = (String) value;
        }
        else if (key.equals(URI))
        {
            m_resourceURI = (String) value;
        }
        else
        {
            if (key.equals(VERSION))
            {
                value = Version.parseVersion(value.toString());
            }
            else if (key.equals(SIZE))
            {
                value = Long.valueOf(value.toString());
            }
            else if (key.equals(CATEGORY))
            {
                if (value instanceof Collection)
                {
                    value = new ArrayList((Collection) value);
                }
                else
                {
                    value = Arrays.asList(value.toString().split(","));
                }
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
                    m_map.put(URI, new URL(base, m_resourceURI));
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
                if (m_javadocURI != null)
                {
                    m_map.put(JAVADOC_URL, new URL(base, m_javadocURI));
                }
                m_converted = true;
            }
            catch (MalformedURLException ex)
            {
                ex.printStackTrace(System.err);
            }
        }
    }

    public String toString()
    {
        return getId();
    }
}