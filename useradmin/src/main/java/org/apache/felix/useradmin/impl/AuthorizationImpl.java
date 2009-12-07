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

import java.util.Vector;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * @see org.osgi.service.useradmin.Authorization
 * 
 * @version $Rev$ $Date$
 */
public class AuthorizationImpl implements Authorization
{
    private User user;
    private UserAdminServiceImpl userAdmin;
    private Vector workingOnRoles;

    /**
     * <p>Construct new Authorization object with provided
     * user and UserAdmin service implementation.<p>
     * 
     * @param user User for who authorization can be checked
     * @param userAdmin UserAdmin service implementation
     */
    public AuthorizationImpl(User user, UserAdminServiceImpl userAdmin)
    {
        this.user = user;
        this.userAdmin = userAdmin;
        this.workingOnRoles = new Vector();
    }

    /**
     * @see org.osgi.service.useradmin.Authorization#getName()
     */
    public String getName()
    {
        if (!userAdmin.isAlive())
        {
            throw new IllegalStateException("UserAdmin service  is not available");
        }
        return user != null ? user.getName() : null;
    }

    /**
     * Looking for all Roles implied by this Authorization object.
     * @see org.osgi.service.useradmin.Authorization#getRoles()
     */
    public String[] getRoles()
    {
        if (!userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service is not available");
        }

        Role[] roles = null;
        try
        {
            roles = userAdmin.getRoles(null);
            Vector result = new Vector();

            for (int i = 0; i < roles.length; i++)
            {
                String roleName = roles[i].getName();
                // we don't include user.anyone role
                // we include implied roles
                if (hasRole(roleName) && !Role.USER_ANYONE.equals(roleName))
                {
                    result.addElement(roleName);
                }
            }

            if (result.size() == 0)
            {
                return null;
            }

            String[] res = new String[result.size()];
            result.copyInto(res);
            return res;
        }
        catch (InvalidSyntaxException e)
        {
            // not possible
        }
        return null;

    }

    /**
     * @see org.osgi.service.useradmin.Authorization#hasRole(java.lang.String)
     */
    public boolean hasRole(String name)
    {
        if (!userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service is not available");
        }
        Role role = userAdmin.getRole(name);
        if (role != null)
        {
            return ((RoleImpl) role).impliedBy(this);
        }
        return false;

    }

    /**
     * Adds working role to working on roles by this Autorization object.
     * 
     * @param role to be added role.
     */
    protected void addWorkingOnRole(Role role)
    {
        workingOnRoles.addElement(role);
    }

    /**
     * Removes working on role.
     * 
     * @param role to be removed from working by this Autorization object roles.
     */
    protected void removeWorkingOnRole(Role role)
    {
        workingOnRoles.removeElement(role);
    }

    /**
     * Check if current Autorization object is working on provided role.
     * This check will avoid loop when Autorization is looking for imply roles.
     * 
     * @param role Role on which Autorization object is working.
     * @return true if this Autorization object is already working on provided role false if not.
     */
    protected boolean isWorkingOnRole(Role role)
    {
        return workingOnRoles.contains(role);
    }
}
