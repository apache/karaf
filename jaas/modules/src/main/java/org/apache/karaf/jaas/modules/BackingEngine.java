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
import java.util.Map;

import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;

public interface BackingEngine {

    String GROUP_PREFIX = "_g_:";
    
    /**
     * Create a new user.
     *
     * @param username the user name.
     * @param password the user password.
     */
    void addUser(String username, String password);

    /**
     * Delete an user.
     *
     * @param username the user name.
     */
    void deleteUser(String username);

    /**
     * List all users.
     *
     * @return the list of {@link UserPrincipal}.
     */
    List<UserPrincipal> listUsers();

    /**
     * Retrieve the {@link UserPrincipal} corresponding to an username, or {@code null} if user doesn't exist.
     *
     * @param username The username.
     * @return The {@link UserPrincipal} or {@code null}.
     */
    UserPrincipal lookupUser(String username);

    /**
     * List groups that a user is member of.
     *
     * @param user the {@link UserPrincipal}.
     * @return the list of {@link GroupPrincipal}.
     */
    List<GroupPrincipal> listGroups(UserPrincipal user);
    
    /**
     * List all groups.
     *
     * @return the groups.
     */
    Map<GroupPrincipal, String> listGroups();

    /**
     * Add a user into a given group.
     *
     * @param username the user name.
     * @param group the group.
     */
    void addGroup(String username, String group);
    
    /**
     * Create a group
     *
     * @param group the group.
     */
    void createGroup(String group);

    /**
     * Remove a user from a group.
     *
     * @param username the user name.
     * @param group the group.
     */
    void deleteGroup(String username, String group);

    /**
     * List Roles for <code>principal</code>. This could either be a
     * {@link UserPrincipal} or a {@link GroupPrincipal}.
     *
     * @param principal the principal.
     * @return the list of roles.
     */
    List<RolePrincipal> listRoles(Principal principal);

    /**
     * Add a role to the user.
     *
     * @param username the user name.
     * @param role the role.
     */
    void addRole(String username, String role);

    /**
     * Remove a role from a user.
     *
     * @param username the user name.
     * @param role the role.
     */
    void deleteRole(String username, String role);

    /**
     * Add a role in a group.
     *
     * @param group the group.
     * @param role the role.
     */
    void addGroupRole(String group, String role);

    /**
     * Remove a role from a group.
     *
     * @param group the group.
     * @param role the role.
     */
    void deleteGroupRole(String group, String role);

}
