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
package org.apache.felix.framework.searchpolicy;

import java.util.ArrayList;
import java.util.List;
import org.apache.felix.moduleloader.ICapability;

/**
 * This utility class is a resolved package, which is comprised of a
 * set of <tt>ICapability</tt>s that is calculated by the resolver
 * algorithm. A given resolved package may have a single capability,
 * as is the case with imported packages, or it may have multiple
 * capabilities, as is the case with required bundles.
 */
class ResolvedPackage
{
    public final String m_name;
    public final CandidateSet m_cs;
    public final List m_capList = new ArrayList();

    public ResolvedPackage(String name, CandidateSet cs)
    {
        super();
        m_name = name;
        m_cs = cs;
    }

    public boolean isSubset(ResolvedPackage rp)
    {
        if (m_capList.size() > rp.m_capList.size())
        {
            return false;
        }
        else if (!m_name.equals(rp.m_name))
        {
            return false;
        }
        // Determine if the target set of source modules is a subset.
        return rp.m_capList.containsAll(m_capList);
    }

    public Object clone()
    {
        ResolvedPackage rp = new ResolvedPackage(m_name, m_cs);
        rp.m_capList.addAll(m_capList);
        return rp;
    }

    public void merge(ResolvedPackage rp)
    {
        // Merge required packages, avoiding duplicate
        // package sources and maintaining ordering.
        for (int capIdx = 0; capIdx < rp.m_capList.size(); capIdx++)
        {
            if (!m_capList.contains(rp.m_capList.get(capIdx)))
            {
                m_capList.add(rp.m_capList.get(capIdx));
            }
        }
    }

    public String toString()
    {
        return toString("", new StringBuffer()).toString();
    }

    public StringBuffer toString(String padding, StringBuffer sb)
    {
        sb.append(padding);
        sb.append(m_name);
        sb.append(" from [");
        for (int i = 0; i < m_capList.size(); i++)
        {
            ICapability cap = (ICapability) m_capList.get(i);
            sb.append(cap.getModule());
            if ((i + 1) < m_capList.size())
            {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb;
    }
}
