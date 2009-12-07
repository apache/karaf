/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.impl;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.felix.useradmin.UserAdminRepository;
import org.apache.felix.useradmin.UserAdminRepositoryManager;
import org.apache.felix.useradmin.Version;
import org.osgi.framework.Filter;
import org.osgi.service.log.LogService;
import org.osgi.service.useradmin.Role;

/**
 * This class <tt>UserAdminRepositoryManagerImpl</tt> implements UserAdminRepositoryManager.
 * Providing operations for saving,removing,flushing data to the repository.
 * All public method are guarded by lock.
 * 
 * @version $Rev$ $Date$
 */
public class UserAdminRepositoryManagerImpl implements UserAdminRepositoryManager
{
    private Logger logger;
    private Object lock = new Object();
    private UserAdminRepository store;

    /**
     * Constructs manager for UserAdminRepositoryManager.
     * @param logger Logger instance.
     * @param store backing store instance.
     */
    public UserAdminRepositoryManagerImpl(Logger logger, UserAdminRepository store)
    {
        this.logger = logger;
        this.store = store;

    }

    /**
     * @see org.apache.felix.useradmin.UserAdminRepositoryManager#initialize(org.apache.felix.useradmin.impl.UserAdminServiceImpl)
     */
    public void initialize(UserAdminServiceImpl userAdmin)
    {
        logger.log(LogService.LOG_DEBUG, "Initializing repository manager");
        synchronized (lock)
        {
            store.load();
            Hashtable cache = store.getRepositoryCache();
            initializePredefinedRole(cache);
            injectDependencyToEntity(cache, userAdmin);
        }

    }

    /**
     * Initialising predefined Role Role.USER_ANYONE.
     * @param storeCache store cache.
     */
    private void initializePredefinedRole(Hashtable storeCache)
    {
        RoleImpl role = new RoleImpl();
        role.setName(Role.USER_ANYONE);
        storeCache.put(Role.USER_ANYONE, role);
    }

    /**
     * Injects UserAdmin to Role objects during the startup
     * @param storeCache cache of the store.
     * @param userAdmin UserAdmin instance.
     */
    private void injectDependencyToEntity(Hashtable storeCache, UserAdminServiceImpl userAdmin)
    {
        Enumeration en = storeCache.elements();
        while (en.hasMoreElements())
        {
            RoleImpl role = (RoleImpl) en.nextElement();
            role.setUserAdmin(userAdmin);
        }
    }

    /**
     * @see org.apache.felix.useradmin.UserAdminRepositoryManager#findRoleByName(java.lang.String)
     */
    public Role findRoleByName(String name)
    {
        if (name != null)
        {
            Hashtable cache = store.getRepositoryCache();
            return (Role) cache.get(name);
        }
        return null;
    }

    /**
     * @GuardedBy lock.
     * @see org.apache.felix.useradmin.UserAdminRepositoryManager#findRoleByTypeAndKeyValue(int,
     * java.lang.String, java.lang.String)
     */
    public Object findRoleByTypeAndKeyValue(int roleType, String key, String value)
    {
        synchronized (lock)
        {
            Vector temp = new Vector();
            Hashtable cache = store.getRepositoryCache();
            for (Enumeration en = cache.elements(); en.hasMoreElements();)
            {
                Role role = (Role) en.nextElement();
                if (role.getType() == roleType)
                {
                    Dictionary properties = role.getProperties();
                    Object val = properties.get(key);
                    if (value.equals(val))
                    {
                        temp.add(role);
                    }
                }
            }
            return temp.isEmpty() || temp.size() > 1 ? null : temp.get(0);
        }

    }

    /**
     * <p>If a null filter is specified, all Role objects managed by User Admin service 
     * are returned.</p>
     * 
     * @GuardedBy lock.
     * @see org.apache.felix.useradmin.impl.UserAdminRepositoryManager#findRolesByFilter(org.osgi.framework.Filter)
     */
    public Role[] findRolesByFilter(Filter filter)
    {
        synchronized (lock)
        {
            Hashtable cache = store.getRepositoryCache();
            Enumeration en = cache.elements();
            if (filter == null)
            {
                Role[] rs = new Role[cache.size()];
                for (int i = 0; en.hasMoreElements(); i++)
                {
                    Role role = (Role) en.nextElement();
                    rs[i] = role;
                }
                return rs;
            }
            else
            {
                Vector temp = new Vector();
                for (int i = 0; en.hasMoreElements(); i++)
                {
                    Role role = (Role) en.nextElement();
                    if (filter.match(role.getProperties()))
                    {
                        temp.add(role);
                    }
                }
                Role[] rs = new Role[temp.size()];
                temp.copyInto(rs);

                return rs;
            }
        }
    }

    /**
     * @GuardedBy lock.
     * @see org.apache.felix.useradmin.impl.UserAdminRepositoryManager#save(java.lang.String, int,
     * org.apache.felix.useradmin.impl.UserAdminServiceImpl)
     */
    public Role save(String name, int type, UserAdminServiceImpl userAdmin)
    {
        synchronized (lock)
        {
            Role role = (Role) findRoleByName(name);
            if (role != null)
            {
                return null;
            }

            switch (type)
            {
                case Role.USER:
                    role = new UserImpl();
                    ((RoleImpl) role).setName(name);
                    ((RoleImpl) role).setUserAdmin(userAdmin);
                    break;
                case Role.GROUP:
                    role = new GroupImpl();
                    ((RoleImpl) role).setName(name);
                    ((RoleImpl) role).setUserAdmin(userAdmin);
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            Version versionableRole = (Version) role;
            versionableRole.increaseVersion();
            Hashtable cache = store.getRepositoryCache();
            cache.put(role.getName(), role);
            store.flush();
            return role;
        }
    }

    /**
     * @GuardedBy lock.
     * @see org.apache.felix.useradmin.UserAdminRepositoryManager#remove(java.lang.String)
     */
    public Role remove(String name)
    {
        synchronized (lock)
        {
            if (name == null)
            {
                return null;
            }
            Hashtable cache = store.getRepositoryCache();
            Role role = (Role) cache.remove(name);
            if (role != null)
            {
                store.flush();
            }
            return role;
        }

    }

    /**
     * Flushing store cache content into the repository file.
     * @GuardedBy lock.
     * @see org.apache.felix.useradmin.UserAdminRepository#flush()
     */
    public void flush()
    {
        logger.log(LogService.LOG_DEBUG, "Flushing current state of repository cache into the file");
        synchronized (lock)
        {
            store.flush();
        }

    }
}
