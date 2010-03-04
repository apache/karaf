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

import java.net.URI;
import java.net.URISyntaxException;
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

    private Map m_uris;

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
        convertURIs();
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

    public String getURI()
    {
        convertURIs();
        return (String) m_map.get(Resource.URI);
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
        put(key.toString(), value.toString(), null);
        return null;
    }

    protected void put(String key, String value, String type)
    {
        key = key.toLowerCase();
        m_hash = 0;
        if ("uri".equals(type) || URI.equals(key))
        {
            if (m_uris == null)
            {
                m_uris = new HashMap();
            }
            m_uris.put(key, value);
        }
        else if ("version".equals(type) || VERSION.equals(key))
        {
            m_map.put(key, Version.parseVersion(value));
        }
        else if ("long".equals(type) || SIZE.equals(key))
        {
            m_map.put(key, Long.valueOf(value.toString()));
        }
        else if (CATEGORY.equals(key))
        {
            m_map.put(key, Arrays.asList(value.toString().split(",")));
        }
        else
        {
            m_map.put(key, value);
        }
    }

    private void convertURIs()
    {
        if (m_uris != null)
        {
            for (Iterator it = m_uris.keySet().iterator(); it.hasNext();)
            {
                String key = (String) it.next();
                String val = (String) m_uris.get(key);
                m_map.put(key, resolveUri(val));
            }
            m_uris = null;
        }
    }

    private String resolveUri(String uri)
    {
        try
        {
            if (m_repo != null && m_repo.getURI() != null)
            {
                return new URI(m_repo.getURI()).resolve(uri).toString();
            }
        }
        catch (Throwable t)
        {
        }
        return uri;
    }

    public String toString()
    {
        return getId();
    }
}