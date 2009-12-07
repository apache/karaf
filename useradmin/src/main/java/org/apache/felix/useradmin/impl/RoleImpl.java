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

import java.io.Serializable;
import java.util.Dictionary;

import org.apache.felix.useradmin.Version;
import org.osgi.service.useradmin.Role;

/**
 * <p>This <tt>RoleImpl</tt>class represents Role.
 * Act as base class for different types of roles User,Group.</p>
 * 
 * @see org.osgi.service.useradmin.Role
 * @version $Rev$ $Date$
 */
public class RoleImpl implements Role, Version, Serializable
{
    private static final long serialVersionUID = -4157076907548034363L;
    /**
     * role version.
     */
    private long version;
    /**
     * role name.
     */
    protected String name;
    /**
     * UserAdmin service instance.
     */
    protected transient UserAdminServiceImpl userAdmin;
    /**
     * role properties.
     */
    private Dictionary properties;

    /**
     * Construct new Role.
     */
    public RoleImpl()
    {
        this.properties = new RoleProperties(this);
    }

    /**
     * @see org.osgi.service.useradmin.Role#getName()
     */
    public String getName()
    {
        return name;
    }

    /**
     * @see org.osgi.service.useradmin.Role#getProperties()
     */
    public Dictionary getProperties()
    {
        return properties;
    }

    /**
     * @see org.osgi.service.useradmin.Role#getType()
     */
    public int getType()
    {
        return Role.ROLE;
    }

    /**
     * @see org.apache.felix.useradmin.Version#getVersion()
     */
    public long getVersion()
    {
        return version;
    }

    /**
     * @see org.apache.felix.useradmin.Version#increaseVersion()
     */
    public void increaseVersion()
    {
        version++;
    }

    /**
     * Checks if this role is implied by provided Authorization object.
     * @see org.osgi.service.useradmin.Autorization
     * @param authorization Authorization instance.
     * @return true if is implied false if not.
     */
    protected boolean impliedBy(AuthorizationImpl authorization)
    {
        if (!userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service is not available");
        }
        String rolename = authorization.getName();
        boolean implied = (rolename != null && rolename.equals(name)) || name.equals(Role.USER_ANYONE);
        return implied;

    }

    /**
     * Setting UserAdmin.
     * @param userAdmin UserAdmin isntance.
     */
    public void setUserAdmin(UserAdminServiceImpl userAdmin)
    {
        this.userAdmin = userAdmin;
    }

    /**
     * Setting role name.
     * @param name role name.
     */
    public void setName(String name)
    {
        this.name = name;
    }
}
