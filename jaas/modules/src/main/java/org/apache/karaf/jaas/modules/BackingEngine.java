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

import java.security.Principal;
import java.util.List;

import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;

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
     * List groups that a user is in.
     *
     * @param user
     * @return
     */
    List<GroupPrincipal> listGroups(UserPrincipal user);

    /**
     * Add an user to a group.
     *
     * @param username
     * @param group
     */
    void addGroup(String username, String group);

    /**
     * Remove an user from a group.
     *
     * @param username
     * @param group
     */
    void deleteGroup(String username, String group);

    /**
     * List Roles for {@param principal}. This could either be a
     * {@link UserPrincipal} or a {@link GroupPrincipal}.
     *
     * @param principal
     * @return
     */
    List<RolePrincipal> listRoles(Principal principal);

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

    /**
     * Add a role in a group.
     *
     * @param group
     * @param role
     */
    void addGroupRole(String group, String role);

    /**
     * Remove a role from a group.
     *
     * @param group
     * @param role
     */
    void deleteGroupRole(String group, String role);

}
