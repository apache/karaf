package org.apache.felix.bundlerepository;

import java.util.*;

import org.osgi.service.obr.Capability;

public class CapabilityImpl implements Capability
{
    private String m_name = null;
    private Map m_map = null;

    public CapabilityImpl()
    {
        m_map = new TreeMap(new Comparator() {
            public int compare(Object o1, Object o2)
            {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }
        });
    }

    public String getName()
    {
        return m_name;
    }

    public void setName(String name)
    {
        m_name = name;
    }

    public Map getProperties()
    {
        return m_map;
    }

    public void addP(PropertyImpl prop)
    {
        m_map.put(prop.getN(), prop.getV());
    }
}