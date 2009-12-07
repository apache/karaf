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
import java.util.Hashtable;

import org.apache.felix.useradmin.Base64;
import org.apache.felix.useradmin.CredentialAuthenticator;
import org.apache.felix.useradmin.UserAdminEventDispatcher;
import org.apache.felix.useradmin.UserAdminRepositoryManager;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminPermission;

/**
 * This class <tt>RoleProperties</tt> represents role properties.
 * Act as a base class for different types of properties.
 *
 * @version $Rev$ $Date$
 */
public class RoleProperties extends Hashtable
{
    private static final long serialVersionUID = -2989683398828827588L;
    protected RoleImpl role;

    /**
     * Constructs new RoleProperties.
     * @param role Role instance.
     */
    public RoleProperties(RoleImpl role)
    {
        this.role = role;
    }

    /**
     * Clears the properties.
     * User needs to have proper change permissions.
     */
    public synchronized void clear()
    {
        if (!role.userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service not available");
        }

        Enumeration e = keys();
        while (e.hasMoreElements())
        {
            String key = (String) e.nextElement();
            role.userAdmin.checkPermission(new UserAdminPermission(key, getChangeAction()));

        }
        super.clear();
        role.increaseVersion();
        UserAdminRepositoryManager repositoryManager = role.userAdmin.getRepositoryManager();
        repositoryManager.flush();
        UserAdminEventDispatcher eventDispatcher = role.userAdmin.getEventAdminDispatcher();
        eventDispatcher.dispatchEventAsynchronusly(new UserAdminEvent(role.userAdmin.getServiceRef(),
            UserAdminEvent.ROLE_CHANGED, role));

    }

    /**
     * Getting property with specified key.
     */
    public synchronized Object get(Object key)
    {
        if (!role.userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service not available");
        }
        Object value = super.get(key);
        if (value == null)
        {
            return null;
        }
        else
        {
            // only encoding properties
            if (getChangeAction().equals(UserAdminPermission.CHANGE_PROPERTY))
            {
                CredentialAuthenticator authenticator = role.userAdmin.getAuthenticator();
                if (authenticator == null)
                {
                    return null;
                }
                Base64 base64 = authenticator.getBase64();
                return base64.decrypt(value);
            }
            return value;
        }

    }

    /**
     * Removing properties with specified key.
     * User of this methods needs to have proper permissions.
     * For removing credentials UserAdminPermission#CHANGE_CREDENTIAL
     * For removing properties UserAdminPermission#CHANGE_PROPERTY.
     */
    public synchronized Object remove(Object key)
    {
        if (!role.userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service not available");
        }

        if (key instanceof String)
        {
            // Check that the caller is allowed to remove the property.
            UserAdminServiceImpl userAdmin = role.userAdmin;
            role.userAdmin.checkPermission(new UserAdminPermission((String) key, getChangeAction()));
            Object res = super.remove(key);
            role.increaseVersion();
            UserAdminRepositoryManager repositoryManager = role.userAdmin.getRepositoryManager();
            repositoryManager.flush();
            UserAdminEventDispatcher eventDispatcher = userAdmin.getEventAdminDispatcher();
            eventDispatcher.dispatchEventAsynchronusly(new UserAdminEvent(userAdmin.getServiceRef(),
                UserAdminEvent.ROLE_CHANGED, role));

            return res;
        }
        else
        {
            throw new IllegalArgumentException("The key must be a String, got " + key.getClass());
        }

    }

    /**
     * <p>
     * Putting new property key-value pair into properties.
     * User needs to have proper change permissions.
     * All values are encoded at least with Base64.</p> 
     */
    public synchronized Object put(Object key, Object value)
    {
        if (!role.userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service not available");
        }

        if (key instanceof String)
        {
            if (value instanceof String || value instanceof byte[])
            {
                UserAdminServiceImpl userAdmin = role.userAdmin;
                userAdmin.checkPermission(new UserAdminPermission((String) key, getChangeAction()));
                Object res = null;
                CredentialAuthenticator authenticator = userAdmin.getAuthenticator();
                if (authenticator == null)
                {
                    return null;
                }
                if (getChangeAction().equals(UserAdminPermission.CHANGE_PROPERTY))
                {
                    // for properties only base64
                    Base64 base64 = authenticator.getBase64();
                    res = base64.encrypt(value);
                    super.put(key, res);
                }
                else
                {
                    // for credentials using base64 or different algorithm SHA-1, etc.
                    res = authenticator.encryptCredential(value);
                    super.put(key, res);
                }

                UserAdminRepositoryManager repositoryManager = role.userAdmin.getRepositoryManager();
                repositoryManager.flush();
                UserAdminEventDispatcher eventDispatcher = userAdmin.getEventAdminDispatcher();
                eventDispatcher.dispatchEventAsynchronusly(new UserAdminEvent(userAdmin.getServiceRef(),
                    UserAdminEvent.ROLE_CHANGED, role));
                return res;
            }
            else
            {
                throw new IllegalArgumentException("The value must be of type String or byte[],  got " + value.getClass());
            }
        }
        else
        {
            throw new IllegalArgumentException("The key must be a String, got " + key.getClass());
        }

    }

    public String toString()
    {
        return "#Properties";
    }

    /**
     * The permission need to modify the properties.
     */
    protected String getChangeAction()
    {
        return UserAdminPermission.CHANGE_PROPERTY;
    }
}
