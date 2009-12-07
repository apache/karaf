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
package org.apache.felix.useradmin;

import org.apache.felix.useradmin.impl.UserAdminServiceImpl;
import org.osgi.framework.Filter;
import org.osgi.service.useradmin.Role;

/**
 * UserAdminRepository manager.
 * Provides methods for storing roles, removing and finding
 * from roles repository.
 * 
 * @version $Rev$ $Date$
 */
public interface UserAdminRepositoryManager
{
    /**
     * Initialising roles repository manager.
     * 
     * @param userAdmin role dependency needs to be injected.
     */
    void initialize(UserAdminServiceImpl userAdmin);

    /**
     * Finding role by role name.
     * 
     * @param name role name.
     * @return Role instance or null if can't find it.
     */
    Role findRoleByName(String name);

    /**
     * Finding Role by role type and property of a role.
     * 
     * @param roleType role type User,etc.
     * @param key key value of property.
     * @param value property value.
     * @return Role instance or null.
     */
    Object findRoleByTypeAndKeyValue(int roleType, String key, String value);

    /**
     * Find roles by filter.
     * 
     * @param filter @see org.osgi.framework.Filter.
     * @return array of Roles.
     */
    Role[] findRolesByFilter(Filter filter);

    /**
     * Saving role with specific name and type.
     * 
     * @param name role name.
     * @param type role type.
     * @param userAdmin role dependency.
     * @return role if created successfully if not null.
     */
    Role save(String name, int type, UserAdminServiceImpl userAdmin);

    /**
     * Remove role with provided name.
     * 
     * @param name role name.
     * @return removed Role if any.
     */
    Role remove(String name);

    /**
     * Flushing changes into the store file.
     */
    void flush();
}
