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
package org.apache.felix.framework.security.permissionadmin;

import java.io.IOException;
import java.security.AllPermission;
import java.security.Permission;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.framework.security.condpermadmin.ConditionalPermissionAdminImpl;
import org.apache.felix.framework.security.util.Permissions;
import org.apache.felix.framework.security.util.PropertiesCache;
import org.apache.felix.moduleloader.IContent;
import org.osgi.framework.Bundle;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * This class is a relatively straight forward implementation of the
 * PermissionAdmin service. The only somewhat involved thing is that it respects
 * the presents of a conditionalpermissionadmin service as per spec.
 */
// TODO: Do we need this class at all or can we just emulate it using the
// condpermadmin?
public final class PermissionAdminImpl implements PermissionAdmin
{
    private static final PermissionInfo[] ALL_PERMISSION = new PermissionInfo[] { new PermissionInfo(
        AllPermission.class.getName(), "", "") };

    private final Map m_store = new HashMap();

    private final PropertiesCache m_cache;

    private final Permissions m_permissions;

    private PermissionInfo[] m_default = null;

    public PermissionAdminImpl(Permissions permissions, PropertiesCache cache)
        throws IOException
    {
        m_permissions = permissions;
        m_cache = cache;
        m_cache.read(PermissionInfo[].class, m_store);
    }

    public PermissionInfo[] getDefaultPermissions()
    {
        synchronized (m_store)
        {
            if (m_default == null)
            {
                return null;
            }
            return (PermissionInfo[]) m_default.clone();
        }
    }

    public synchronized String[] getLocations()
    {
        synchronized (m_store)
        {
            if (m_store.isEmpty())
            {
                return null;
            }

            return (String[]) m_store.keySet().toArray(
                new String[m_store.size()]);
        }
    }

    public PermissionInfo[] getPermissions(String location)
    {
        synchronized (m_store)
        {
            if (m_store.containsKey(location))
            {
                return (PermissionInfo[]) ((PermissionInfo[]) m_store
                    .get(location)).clone();
            }
            return null;
        }
    }

    /**
     * This will do the actual permission check as described in the core spec
     * 10.2 It will respect a present condpermadmin service as described in
     * 9.10.
     * 
     * @param location
     *            the location of the bundle.
     * @param bundle
     *            the bundle in question.
     * @param permission
     *            the permission to check.
     * @param cpai
     *            A condpermadmin if one is present else null.
     * @param pd
     *            the protectiondomain
     * @return Boolean.TRUE if the location is bound and the permission is
     *         granted or if there is no cpa and the default permissions imply
     *         the permission Boolean.FALSE otherwise unless the location is not
     *         bound and their is a cpa in which case null is returned.
     */
    public Boolean hasPermission(String location, Bundle bundle,
        Permission permission, ConditionalPermissionAdminImpl cpai,
        ProtectionDomain pd, IContent content)
    {
        PermissionInfo[] permissions = null;
        PermissionInfo[] defaults = null;
        boolean contains = false;
        synchronized (m_store)
        {
            contains = m_store.containsKey(location);
            permissions = (PermissionInfo[]) m_store.get(location);
            defaults = m_default;
        }
        if (contains)
        {
            if (check(permissions, permission, bundle))
            {
                return Boolean.TRUE;
            }
            return check(m_permissions.getImplicit(bundle), permission, bundle) ? Boolean.TRUE
                : Boolean.FALSE;
        }
        else if (cpai == null
            || (cpai.isEmpty() && cpai
                .impliesLocal(bundle, content, permission)))
        {
            if (defaults != null)
            {
                if (check(defaults, permission, null))
                {
                    return Boolean.TRUE;
                }
                return check(m_permissions.getImplicit(bundle), permission,
                    bundle) ? Boolean.TRUE : Boolean.FALSE;
            }
            else
            {
                return Boolean.TRUE;
            }
        }
        else
        {
            return null;
        }
    }

    private boolean check(PermissionInfo[] permissions, Permission permission,
        Bundle bundle)
    {
        Permissions permissionsObject = m_permissions
            .getPermissions(permissions);

        return permissionsObject.implies(permission, bundle);
    }

    public void setDefaultPermissions(PermissionInfo[] permissions)
    {
        Object sm = System.getSecurityManager();
        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(Permissions.ALL_PERMISSION);
        }

        synchronized (m_cache)
        {
            PermissionInfo[] def = null;
            Map store = null;
            synchronized (m_store)
            {
                def = m_default;
                store = new HashMap(m_store);

                m_default = (permissions != null) ? notNull(permissions) : null;
            }

            try
            {
                m_cache.write(setDefaults(store, def));
            }
            catch (IOException ex)
            {
                synchronized (m_store)
                {
                    m_default = def;
                }

                ex.printStackTrace();
                // TODO: log this
                throw new IllegalStateException(ex.getMessage());
            }
        }
    }

    public void setPermissions(String location, PermissionInfo[] permissions)
    {
        Object sm = System.getSecurityManager();
        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(Permissions.ALL_PERMISSION);
        }

        synchronized (m_cache)
        {
            if (location != null)
            {
                Map store = null;
                Map storeCopy = null;
                PermissionInfo[] def = null;
                synchronized (m_store)
                {
                    storeCopy = new HashMap(m_store);
                    if (permissions != null)
                    {
                        m_store.put(location, notNull(permissions));
                    }
                    else
                    {
                        m_store.remove(location);
                    }
                    store = new HashMap(m_store);
                }
                try
                {
                    m_cache.write(setDefaults(store, def));
                }
                catch (IOException ex)
                {
                    synchronized (m_store)
                    {
                        m_store.clear();
                        m_store.putAll(storeCopy);
                    }

                    ex.printStackTrace();
                    // TODO: log this
                    throw new IllegalStateException(ex.getMessage());
                }
            }
        }
    }

    private Map setDefaults(Map store, PermissionInfo[] def)
    {
        if (def != null)
        {
            store.put("DEFAULT", def);
        }
        else
        {
            store.remove("DEFAULT");
        }
        return store;
    }

    private PermissionInfo[] notNull(PermissionInfo[] permissions)
    {
        List result = new ArrayList();

        for (int i = 0; i < permissions.length; i++)
        {
            if (permissions[i] != null)
            {
                result.add(permissions[i]);
            }
        }
        return (PermissionInfo[]) result.toArray(new PermissionInfo[result
            .size()]);
    }
}
