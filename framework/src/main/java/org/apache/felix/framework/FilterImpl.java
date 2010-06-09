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
package org.apache.felix.framework;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import org.apache.felix.framework.capabilityset.Attribute;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.capabilityset.Directive;
import org.apache.felix.framework.capabilityset.SimpleFilter;
import org.apache.felix.framework.resolver.Module;
import org.apache.felix.framework.util.StringMap;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class FilterImpl implements Filter
{
    private final SimpleFilter m_filter;

    public FilterImpl(String filterStr) throws InvalidSyntaxException
    {
        try
        {
            m_filter = SimpleFilter.parse(filterStr);
        }
        catch (Throwable th)
        {
            throw new InvalidSyntaxException(th.getMessage(), filterStr);
        }
    }

    public boolean match(ServiceReference sr)
    {
        return CapabilitySet.matches(new ServiceReferenceCapability(sr), m_filter);
    }

    public boolean match(Dictionary dctnr)
    {
        return CapabilitySet.matches(new DictionaryCapability(dctnr, false), m_filter);
    }

    public boolean matchCase(Dictionary dctnr)
    {
        return CapabilitySet.matches(new DictionaryCapability(dctnr, true), m_filter);
    }

    public boolean equals(Object o)
    {
        return toString().equals(o.toString());
    }

    public int hashCode()
    {
        return toString().hashCode();
    }

    public String toString()
    {
        return m_filter.toString();
    }

    static class DictionaryCapability implements Capability
    {
        private final StringMap m_map;
        private final Dictionary m_dict;

        public DictionaryCapability(Dictionary dict, boolean caseSensitive)
        {
            m_dict = dict;
            if (!caseSensitive)
            {
                m_map = new StringMap(false);
                if (dict != null)
                {
                    Enumeration keys = dict.keys();
                    while (keys.hasMoreElements())
                    {
                        Object key = keys.nextElement();
                        if (m_map.get(key) == null)
                        {
                            m_map.put(key, key);
                        }
                        else
                        {
                            throw new IllegalArgumentException(
                                "Duplicate attribute: " + key.toString());
                        }
                    }
                }
            }
            else
            {
                m_map = null;
            }
        }

        public Module getModule()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getNamespace()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Directive getDirective(String name)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public List<Directive> getDirectives()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Attribute getAttribute(String name)
        {
            String key = name;
            Object value = null;
            if (m_dict != null)
            {
                // If attribute names are case insensitive, then look in
                // the case insensitive key map to find the actual case of
                // the key.
                if (m_map != null)
                {
                    key = (String) m_map.get(name);
                }
                // If the key could not be found in the case insensitive
                // key map, then avoid doing the dictionary lookup on it.
                if (key != null)
                {
                    value = m_dict.get(key);
                }
            }
            return (value == null) ? null : new Attribute(key, value, false);
        }

        public List<Attribute> getAttributes()
        {
            return new ArrayList();
        }

        public List<String> getUses()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    static class ServiceReferenceCapability implements Capability
    {
        private final ServiceReference m_sr;

        public ServiceReferenceCapability(ServiceReference sr)
        {
            m_sr = sr;
        }

        public Module getModule()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getNamespace()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Directive getDirective(String name)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public List<Directive> getDirectives()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Attribute getAttribute(String name)
        {
            Object value = m_sr.getProperty(name);
            return (value == null) ? null : new Attribute(name, value, false);
        }

        public List<Attribute> getAttributes()
        {
            return new ArrayList();
        }

        public List<String> getUses()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
