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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.Repository;

public class RepositoryImpl implements Repository
{
    private String m_name = null;
    private long m_lastmodified = System.currentTimeMillis();
    private String m_uri = null;
    private Resource[] m_resources = null;
    private Referral[] m_referrals = null;
    private Set m_resourceSet = new HashSet();

    public RepositoryImpl()
    {
    }

    public RepositoryImpl(Resource[] resources)
    {
        m_resources = resources;
    }

    public String getURI()
    {
        return m_uri;
    }

    protected void setURI(String uri)
    {
        m_uri = uri;
    }

    public Resource[] getResources()
    {
        if (m_resources == null)
        {
            m_resources = (Resource[]) m_resourceSet.toArray(new Resource[m_resourceSet.size()]);
            Arrays.sort(m_resources, new ResourceComparator());

        }
        return m_resources;
    }

    public void addResource(Resource resource)
    {
        // Set resource's repository.
        if (resource instanceof ResourceImpl)
        {
            ((ResourceImpl) resource).setRepository(this);
        }

        // Add to resource array.
        m_resourceSet.remove(resource);
        m_resourceSet.add(resource);
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

    public void setLastModified(long lastModified)
    {
        m_lastmodified = lastModified;
    }

    public void setLastModified(String s)
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

}