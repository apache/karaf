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

import org.osgi.service.useradmin.UserAdminPermission;

/**
 * <p>
 * This class <tt>RoleCredentials</tt> represents role credentials hashtable. User of this class needs to have proper
 * permissions UserAdminPermission#CHANGE_CREDENTIAL to modify credentials and UserAdminPermission#GET_CREDENTIAL to get
 * credentials.
 * Inherits methods from RoleProperties.
 * </p>
 * 
 * @see org.apache.felix.useradmin.impl.RoleProperties
 * 
 * @version $Rev$ $Date$
 */
public class RoleCredentials extends RoleProperties
{
    private static final long serialVersionUID = 8503492916531864487L;

    /**
     * Constructs new RoleCredentials.
     * @param role Role instance.
     */
    public RoleCredentials(RoleImpl role)
    {
        super(role);
    }

    /**
     * The permission need to modify the credentials.
     */
    protected String getChangeAction()
    {
        return UserAdminPermission.CHANGE_CREDENTIAL;
    }

    /**
     * Gets credential for specified key.
     * User of this method needs to have UserAdminPermission#GET_CREDENTIAL permissions.
     */
    public synchronized Object get(Object key)
    {
        if (!role.userAdmin.isAlive())
        {
            throw new IllegalStateException("User Admin Service not available");
        }

        if (key instanceof String)
        {
            // Check that the caller are allowed to get the credential.
            role.userAdmin.checkPermission(new UserAdminPermission((String) key, UserAdminPermission.GET_CREDENTIAL));
            return super.get(key);
        }
        else
        {
            throw new IllegalArgumentException("The key must be a String, got " + key.getClass());
        }
    }

    public String toString()
    {
        return "#Credentials#";
    }
}
