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

import org.apache.felix.useradmin.CredentialAuthenticator;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * <p>
 * This <tt>UserImpl</tt>class represents User role.
 * A User can be configured with credentials, password,properties etc.<p>
 * 
 * @see org.osgi.service.useradmin.Role
 * @see org.osgi.service.useradmin.User
 * @version $Rev$ $Date$
 */
public class UserImpl extends RoleImpl implements User
{
    private static final long serialVersionUID = 9207444218182653967L;
    /**
     * this variable represents user credentials.
     */
    private Dictionary credentials;

    /**
     * Constructs new User. 
     */
    public UserImpl()
    {
        super();
        this.credentials = new RoleCredentials(this);
    }

    /**
     * @see org.osgi.service.useradmin.User#getCredentials()
     */
    public Dictionary getCredentials()
    {
        return credentials;
    }

    /**
     * @see org.osgi.service.useradmin.User#hasCredential(String, Object)
     */
    public boolean hasCredential(String key, Object value)
    {
        if (!userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service is not available");
        }
        Object rvalue = credentials.get(key);
        if (rvalue == null)
        {
            return false;
        }
        CredentialAuthenticator authenticator = userAdmin.getAuthenticator();
        if (authenticator == null)
        {
            return false;
        }

        if (value instanceof String || value instanceof byte[])
        {
            return authenticator.authenticate(value, rvalue);
        }
        else
        {
            throw new IllegalArgumentException("value must be of type String or byte[]");
        }

    }

    /**
     * @see org.osgi.service.useradmin.User#getType()
     */
    public int getType()
    {
        return Role.USER;
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
}
