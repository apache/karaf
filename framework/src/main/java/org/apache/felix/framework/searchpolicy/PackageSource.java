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

import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.Capability;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IModule;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * This utility class represents a source for a given package, where
 * the package is indicated by a particular module and the module's
 * capability associated with that package. This class also implements
 * <tt>Comparable</tt> so that two package sources can be compared based
 * on version and bundle identifiers.
 */
public class PackageSource implements Comparable
{
    public IModule m_module = null;
    public ICapability m_capability = null;

    public PackageSource(IModule module, ICapability capability)
    {
        super();
        m_module = module;
        m_capability = capability;
    }

    public int compareTo(Object o)
    {
        PackageSource ps = (PackageSource) o;
        Version thisVersion = null;
        Version version = null;
        if (m_capability.getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
        {
            thisVersion = ((Capability) m_capability).getPackageVersion();
            version = ((Capability) ps.m_capability).getPackageVersion();
        }
        else if (m_capability.getNamespace().equals(ICapability.MODULE_NAMESPACE))
        {
            thisVersion = (Version) m_capability.getProperties().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
            version = (Version) ps.m_capability.getProperties().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
        }
        if ((thisVersion != null) && (version != null))
        {
            int cmp = thisVersion.compareTo(version);
            if (cmp < 0)
            {
                return 1;
            }
            else if (cmp > 0)
            {
                return -1;
            }
            else
            {
                long thisId = Util.getBundleIdFromModuleId(m_module.getId());
                long id = Util.getBundleIdFromModuleId(ps.m_module.getId());
                if (thisId < id)
                {
                    return -1;
                }
                else if (thisId > id)
                {
                    return 1;
                }
                return 0;
            }
        }
        else
        {
            return -1;
        }
    }

    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((m_capability == null) ? 0 : m_capability.hashCode());
        result = PRIME * result + ((m_module == null) ? 0 : m_module.hashCode());
        return result;
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null)
        {
            return false;
        }
        if (getClass() != o.getClass())
        {
            return false;
        }
        PackageSource ps = (PackageSource) o;
        return m_module.equals(ps.m_module) && (m_capability == ps.m_capability);
    }
}
