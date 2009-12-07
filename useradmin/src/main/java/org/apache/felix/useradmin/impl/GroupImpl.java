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

import java.util.Enumeration;
import java.util.Vector;

import org.apache.felix.useradmin.UserAdminRepositoryManager;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;

/**
 * This class represents Group role.
 * Group is an aggregation of basic and required roles.
 * Basic and required roles are used in the autorization phase.
 * 
 * @see org.osgi.service.useradmin.Group
 * @version $Rev$ $Date$
 */
public class GroupImpl extends UserImpl implements Group
{
    private static final long serialVersionUID = -6218617211170379394L;
    private Vector members = new Vector();
    private Vector requiredMembers = new Vector();
    private transient Object lock = new Object();

    /**
     * Construct new Group role.
     */
    public GroupImpl()
    {
        super();
    }

    /**
     * @see org.osgi.service.useradmin.Group#addMember(Role)
     */
    public boolean addMember(Role role)
    {
        if (!userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service is not available");
        }
        userAdmin.checkPermission(userAdmin.getUserAdminPermission());
        synchronized (lock)
        {

            if (!(role instanceof RoleImpl) || ((RoleImpl) role).userAdmin != userAdmin)
            {
                throw new IllegalArgumentException("Not correct role");
            }
            String name = role.getName();
            if (members.contains(name) || requiredMembers.contains(name))
            {
                return false;
            }
            members.addElement(name);
            increaseVersion();
            UserAdminRepositoryManager repositoryManager = userAdmin.getRepositoryManager();
            repositoryManager.flush();
            return true;

        }

    }

    /**
     * @see org.osgi.service.useradmin.Group#addRequiredMember(Role)
     */
    public boolean addRequiredMember(Role role)
    {
        if (!userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service is not available");
        }
        userAdmin.checkPermission(userAdmin.getUserAdminPermission());
        synchronized (lock)
        {
            if (!(role instanceof RoleImpl) || ((RoleImpl) role).userAdmin != userAdmin)
            {
                throw new IllegalArgumentException("Not correct role");
            }
            String name = role.getName();
            if (members.contains(name) || requiredMembers.contains(name))
            {
                return false;
            }
            requiredMembers.addElement(name);

            increaseVersion();
            UserAdminRepositoryManager repositoryManager = userAdmin.getRepositoryManager();
            repositoryManager.flush();
            return true;

        }

    }

    /**
     * @see org.osgi.service.useradmin.Group#getMembers()
     */
    public Role[] getMembers()
    {
        if (!userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service is not available");
        }
        Role[] rs = new Role[members.size()];
        if (rs.length == 0)
        {
            return null;
        }
        synchronized (lock)
        {
            Enumeration en = members.elements();
            for (int i = 0; en.hasMoreElements(); i++)
            {
                rs[i] = userAdmin.getRole((String) en.nextElement());
            }

            return rs;
        }
    }

    /**
     * @see org.osgi.service.useradmin.Group#getRequiredMembers()
     */
    public Role[] getRequiredMembers()
    {
        if (!userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service is not available");
        }
        Role[] rs = new Role[requiredMembers.size()];
        if (rs.length == 0)
        {
            return null;
        }
        synchronized (lock)
        {
            Enumeration en = requiredMembers.elements();
            for (int i = 0; en.hasMoreElements(); i++)
            {
                rs[i] = userAdmin.getRole((String) en.nextElement());
            }
            return rs;
        }
    }

    /**
     * @see org.osgi.service.useradmin.Group#removeMember(Role)
     */
    public boolean removeMember(Role role)
    {
        if (!userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service is not available");
        }
        userAdmin.checkPermission(userAdmin.getUserAdminPermission());
        if (role == null || !(role instanceof RoleImpl))
        {
            throw new IllegalArgumentException("Bad role");
        }
        String name = role.getName();
        synchronized (lock)
        {
            boolean removed = members.remove(name) || requiredMembers.remove(name);
            if (removed)
            {
                UserAdminRepositoryManager storeManager = userAdmin.getRepositoryManager();
                storeManager.flush();
                increaseVersion();
            }
            return removed;
        }

    }

    /**
     * @see org.osgi.service.useradmin.Group#getType()
     */
    public int getType()
    {
        return Role.GROUP;
    }

    /**
     * Checks if this role is implied by provided Authorization object.
     * @see org.osgi.service.useradmin.Autorization
     */
    protected boolean impliedBy(AuthorizationImpl authorization)
    {
        if (!userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service is not available");
        }

        if (authorization.isWorkingOnRole(this))
        {
            //loop
            return false;
        }
        authorization.addWorkingOnRole(this);
        // First check that all required roles are implied.
        synchronized (lock)
        {
            for (Enumeration en = requiredMembers.elements(); en.hasMoreElements();)
            {
                RoleImpl role = (RoleImpl) userAdmin.getRole((String) en.nextElement());
                if (!role.impliedBy(authorization))
                {
                    authorization.removeWorkingOnRole(this);
                    return false;
                }
            }
            // Next check that at least one basic role is implied.
            for (Enumeration en = members.elements(); en.hasMoreElements();)
            {
                RoleImpl role = (RoleImpl) userAdmin.getRole((String) en.nextElement());
                if (role.impliedBy(authorization))
                {
                    authorization.removeWorkingOnRole(this);
                    return true;
                }
            }
        }

        authorization.removeWorkingOnRole(this);
        return false;
    }
}
