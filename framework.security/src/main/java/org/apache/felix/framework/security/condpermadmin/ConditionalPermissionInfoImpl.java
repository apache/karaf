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
package org.apache.felix.framework.security.condpermadmin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.felix.framework.security.util.Permissions;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * Simple storage class for condperminfos. Additionally, this class can be used
 * to encode and decode infos.
 */
public final class ConditionalPermissionInfoImpl implements
    ConditionalPermissionInfo
{
    private static final Random RANDOM = new Random();
    static final ConditionInfo[] CONDITION_INFO = new ConditionInfo[0];
    static final PermissionInfo[] PERMISSION_INFO = new PermissionInfo[0];
    private final Object m_lock = new Object();
    private final String m_name;
    private final boolean m_allow;
    private volatile ConditionalPermissionAdminImpl m_cpai;
    private ConditionInfo[] m_conditions;
    private PermissionInfo[] m_permissions;

    public ConditionalPermissionInfoImpl(String encoded)
    {
        StringTokenizer tok = new StringTokenizer(encoded, "\n");
        String access = tok.nextToken().trim();
        if (!(access.equals("ALLOW {") || access.equals("DENY {")))
        {
            throw new IllegalArgumentException();
        }
        m_allow = access.equals("ALLOW {");
        m_cpai = null;
        m_name = tok.nextToken().trim().substring(1);
        List conditions = new ArrayList();
        List permissions = new ArrayList();
        for (String current = tok.nextToken().trim();; current = tok
            .nextToken().trim())
        {
            if (current.equals("}"))
            {
                break;
            }
            else if (current.startsWith("["))
            {
                conditions.add(new ConditionInfo(current));
            }
            else if (current.startsWith("("))
            {
                permissions.add(new PermissionInfo(current));
            }
            else
            {
                if (!current.startsWith("#"))
                {
                    throw new IllegalArgumentException();
                }
            }
        }

        m_conditions = conditions.isEmpty() ? CONDITION_INFO
            : (ConditionInfo[]) conditions.toArray(new ConditionInfo[conditions
                .size()]);
        m_permissions = permissions.isEmpty() ? PERMISSION_INFO
            : (PermissionInfo[]) permissions
                .toArray(new PermissionInfo[permissions.size()]);
    }

    public ConditionalPermissionInfoImpl(ConditionalPermissionAdminImpl cpai,
        String name, boolean access)
    {
        m_allow = access;
        m_name = name;
        m_cpai = cpai;
        m_conditions = CONDITION_INFO;
        m_permissions = PERMISSION_INFO;
    }

    public ConditionalPermissionInfoImpl(ConditionInfo[] conditions,
        PermissionInfo[] permisions, ConditionalPermissionAdminImpl cpai,
        boolean access)
    {
        m_allow = access;
        m_name = Long.toString(RANDOM.nextLong() ^ System.currentTimeMillis());
        m_cpai = cpai;
        m_conditions = conditions;
        m_permissions = permisions;
    }

    public ConditionalPermissionInfoImpl(String name,
        ConditionInfo[] conditions, PermissionInfo[] permisions,
        ConditionalPermissionAdminImpl cpai, boolean access)
    {
        m_allow = access;
        m_name = (name != null) ? name : Long.toString(RANDOM.nextLong()
            ^ System.currentTimeMillis());
        m_conditions = conditions;
        m_permissions = permisions;
        m_cpai = cpai;
    }

    public void delete()
    {
        Object sm = System.getSecurityManager();
        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(Permissions.ALL_PERMISSION);
        }

        synchronized (m_lock)
        {
            m_cpai.write(m_name, null);
            m_conditions = CONDITION_INFO;
            m_permissions = PERMISSION_INFO;
        }
    }

    public ConditionInfo[] getConditionInfos()
    {
        synchronized (m_lock)
        {
            return (ConditionInfo[]) m_conditions.clone();
        }
    }

    ConditionInfo[] _getConditionInfos()
    {
        synchronized (m_lock)
        {
            return m_conditions;
        }
    }

    void setConditionsAndPermissions(ConditionInfo[] conditions,
        PermissionInfo[] permissions)
    {
        synchronized (m_lock)
        {
            m_conditions = conditions;
            m_permissions = permissions;
        }
    }

    public String getName()
    {
        return m_name;
    }

    public PermissionInfo[] getPermissionInfos()
    {
        synchronized (m_lock)
        {
            return (PermissionInfo[]) m_permissions.clone();
        }
    }

    PermissionInfo[] _getPermissionInfos()
    {
        synchronized (m_lock)
        {
            return m_permissions;
        }
    }

    public String getEncoded()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append(m_allow ? "ALLOW " : "DENY ");
        buffer.append('{');
        buffer.append('\n');
        buffer.append('#');
        buffer.append(m_name);
        buffer.append('\n');
        synchronized (m_lock)
        {
            writeTo(m_conditions, buffer);
            writeTo(m_permissions, buffer);
        }
        buffer.append('}');
        return buffer.toString();
    }

    private void writeTo(Object[] elements, StringBuffer buffer)
    {
        for (int i = 0; i < elements.length; i++)
        {
            buffer.append(elements[i]);
            buffer.append('\n');
        }
    }

    public String toString()
    {
        return getEncoded();
    }

    public String getAccessDecision()
    {
        return m_allow ? ConditionalPermissionInfo.ALLOW
            : ConditionalPermissionInfo.DENY;
    }

    public boolean isAllow()
    {
        return m_allow;
    }
}
