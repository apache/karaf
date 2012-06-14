/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.karaf.jaas.modules;

import java.util.List;

public interface BackingEngine {

    /**
     * Create a new User.
     *
     * @param username
     * @param password
     */
    void addUser(String username, String password);

    /**
     * Delete User
     *
     * @param username
     */
    void deleteUser(String username);

    /**
     * List Users
     */
    List<UserPrincipal> listUsers();

    /**
     * List Roles for {@param user}.
     *
     * @param user
     * @return
     */
    List<RolePrincipal> listRoles(UserPrincipal user);

    /**
     * Add a role to the user
     *
     * @param username
     * @param role
     */
    void addRole(String username, String role);

    /**
     * Remove a role from a user.
     *
     * @param username
     * @param role
     */
    void deleteRole(String username, String role);

}
