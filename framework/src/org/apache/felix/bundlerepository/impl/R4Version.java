/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.bundlerepository.impl;

import java.util.StringTokenizer;

import org.apache.felix.bundlerepository.IVersion;

public class R4Version implements Comparable, IVersion
{
    private int m_major = 0;
    private int m_minor = 0;
    private int m_micro = 0;
    private String m_qualifier = "";
    private boolean m_isInclusive = true;

    private static final String SEPARATOR = ".";

    public R4Version(String versionString)
    {
        this(versionString, true);
    }

    public R4Version(String versionString, boolean isInclusive)
    {
        if (versionString == null)
        {
            versionString = "0.0.0";
        }
        Object[] objs = parseVersion(versionString);
        m_major = ((Integer) objs[0]).intValue();
        m_minor = ((Integer) objs[1]).intValue();
        m_micro = ((Integer) objs[2]).intValue();
        m_qualifier = (String) objs[3];
        m_isInclusive = isInclusive;
    }

    private static Object[] parseVersion(String versionString)
    {
        String s = versionString.trim();
        Object[] objs = new Object[4];
        objs[0] = objs[1] = objs[2] = new Integer(0);
        objs[3] = "";
        StringTokenizer tok = new StringTokenizer(s, SEPARATOR);
        try
        {
            objs[0] = Integer.valueOf(tok.nextToken());
            if (tok.hasMoreTokens())
            {
                objs[1] = Integer.valueOf(tok.nextToken());
                if (tok.hasMoreTokens())
                {
                    objs[2] = Integer.valueOf(tok.nextToken());
                    if (tok.hasMoreTokens())
                    {
                        objs[3] = tok.nextToken();
                    }
                }
            }
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException("Invalid version: " + versionString);
        }

        if ((((Integer) objs[0]).intValue() < 0) ||
            (((Integer) objs[0]).intValue() < 0) ||
            (((Integer) objs[0]).intValue() < 0))
        {
            throw new IllegalArgumentException("Invalid version: " + versionString);
        }

        return objs;
    }

    /* (non-Javadoc)
     * @see org.ungoverned.osgi.service.impl.Version#equals(java.lang.Object)
    **/
    public boolean equals(Object object)
    {
        if (!(object instanceof R4Version))
        {
            return false;
        }
        IVersion v = (IVersion) object;
        return
            (v.getMajorComponent() == m_major) &&
            (v.getMinorComponent() == m_minor) &&
            (v.getMicroComponent() == m_micro) &&
            (v.getQualifierComponent().equals(m_qualifier));
    }

    /* (non-Javadoc)
     * @see org.ungoverned.osgi.service.impl.Version#getMajorComponent()
    **/
    public int getMajorComponent()
    {
        return m_major;
    }

    /* (non-Javadoc)
     * @see org.ungoverned.osgi.service.impl.Version#getMinorComponent()
    **/
    public int getMinorComponent()
    {
        return m_minor;
    }

    /* (non-Javadoc)
     * @see org.ungoverned.osgi.service.impl.Version#getMicroComponent()
    **/
    public int getMicroComponent()
    {
        return m_micro;
    }

    /* (non-Javadoc)
     * @see org.ungoverned.osgi.service.impl.Version#getQualifierComponent()
    **/
    public String getQualifierComponent()
    {
        return m_qualifier;
    }

    /* (non-Javadoc)
     * @see org.ungoverned.osgi.service.impl.Version#isInclusive()
    **/
    public boolean isInclusive()
    {
        return m_isInclusive;
    }

    /* (non-Javadoc)
     * @see org.ungoverned.osgi.service.impl.Version#compareTo(java.lang.Object)
    **/
    public int compareTo(Object o)
    {
        if (!(o instanceof R4Version))
            throw new ClassCastException();

        if (equals(o))
            return 0;

        if (isGreaterThan((IVersion) o))
            return 1;

        return -1;
    }

    private boolean isGreaterThan(IVersion v)
    {
        if (v == null)
        {
            return false;
        }

        if (m_major > v.getMajorComponent())
        {
            return true;
        }
        if (m_major < v.getMajorComponent())
        {
            return false;
        }
        if (m_minor > v.getMinorComponent())
        {
            return true;
        }
        if (m_minor < v.getMinorComponent())
        {
            return false;
        }
        if (m_micro > v.getMicroComponent())
        {
            return true;
        }
        if (m_micro < v.getMicroComponent())
        {
            return false;
        }
        if (m_qualifier.compareTo(v.getQualifierComponent()) > 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.ungoverned.osgi.service.impl.Version#toString()
    **/
    public String toString()
    {
        if (m_qualifier.length() == 0)
        {
            return m_major + "." + m_minor + "." + m_micro; 
        }
        return m_major + "." + m_minor + "." + m_micro + "." + m_qualifier; 
    }
}