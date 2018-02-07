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
package org.apache.karaf.jaas.modules.ldap;

import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.ldap.LDAPCache;
import org.apache.karaf.jaas.modules.ldap.LDAPOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Karaf JAAS backing engine to support basic list funcitonality
 * for the LDAP login module.  Modification is not supported
 * at this time
 */
public class LDAPBackingEngine implements BackingEngine {

    private LDAPCache cache;
    private LDAPOptions options;

    private static Logger LOGGER = LoggerFactory.getLogger(LDAPBackingEngine.class);

    public LDAPBackingEngine(Map<String, ?> options) {
        this.options = new LDAPOptions(options);
        cache = LDAPCache.getCache(this.options);
    }

    @Override
    public void addUser(String username, String password) {
        throw new UnsupportedOperationException("Adding a user is not supporting in LDAP");
    }

    @Override
    public void deleteUser(String username) {
        throw new UnsupportedOperationException("Deleting a user is not supporting in LDAP");
    }

    @Override
    public UserPrincipal lookupUser(String username) {
        DirContext context = null;
        try {
            context = cache.open();

            SearchControls controls = new SearchControls();
            if (options.getUserSearchSubtree()) {
                controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            } else {
                controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            }

            String filter = options.getUserFilter();
            filter = filter.replaceAll(Pattern.quote("%u"), username);
            filter = filter.replace("\\", "\\\\");

            LOGGER.debug("Looking for user {} in LDAP with", username);
            LOGGER.debug("   base DN: {}", options.getUserBaseDn());
            LOGGER.debug("   filter: {}", filter);

            NamingEnumeration<SearchResult> namingEnumeration = context.search(options.getUserBaseDn(), filter, controls);
            if (namingEnumeration.hasMore()) {
                return new UserPrincipal(username);
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public List<UserPrincipal> listUsers() {
        DirContext context = null;

        ArrayList<UserPrincipal> users = new ArrayList<>();

        try {
            context = cache.open();

            SearchControls controls = new SearchControls();
            if (options.getUserSearchSubtree()) {
                controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            } else {
                controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            }

            String filter = options.getUserFilter();
            filter = filter.replaceAll(Pattern.quote("%u"), "*");
            filter = filter.replace("\\", "\\\\");

            LOGGER.debug("Looking for the users in LDAP with ");
            LOGGER.debug("  base DN: " + options.getUserBaseDn());
            LOGGER.debug("  filter: " + filter);

            NamingEnumeration<SearchResult> namingEnumeration = context.search(options.getUserBaseDn(), filter, controls);
            try {
                while (namingEnumeration.hasMore()) {
                    SearchResult result = namingEnumeration.next();

                    // We need to do the following because slashes are handled badly. For example, when searching
                    // for a user with lots of special characters like cn=admin,=+<>#;\
                    // SearchResult contains 2 different results:
                    //
                    // SearchResult.getName = cn=admin\,\=\+\<\>\#\;\\\\
                    // SearchResult.getNameInNamespace = cn=admin\,\=\+\<\>#\;\\,ou=people,dc=example,dc=com
                    //
                    // the second escapes the slashes correctly.
                    String userDNNamespace = result.getNameInNamespace();
                    // handle case where cn, ou, dc case doesn't match
                    int indexOfUserBaseDN = userDNNamespace.toLowerCase().indexOf("," + options.getUserBaseDn().toLowerCase());
                    String userDN = (indexOfUserBaseDN > 0) ?
                            userDNNamespace.substring(0, indexOfUserBaseDN) :
                            result.getName();

                    // we need to pull out the cn=, uid=, ect.. from the user name to get the actual user name
                    String userName = userDN;
                    if (userDN.contains("=")) userName = userDN.split("=")[1];
                  
                    users.add(new UserPrincipal(userName));

                }
            } finally {
                if (namingEnumeration != null) {
                    try {
                        namingEnumeration.close();
                    } catch (NamingException e) {
                        // Ignore
                    }
                }
            }
            
            return users;

        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<GroupPrincipal> listGroups(UserPrincipal user) {
        // for now return empty list, group implementation is not supported
        return Collections.emptyList();
    }

    @Override
    public Map<GroupPrincipal, String> listGroups() {
        // for now return empty list, group implementation is not supported
        return Collections.emptyMap();
    }

    @Override
    public void addGroup(String username, String group) {
        throw new UnsupportedOperationException("Adding a group is not supporting in LDAP");
    }

    @Override
    public void createGroup(String group) {
        throw new UnsupportedOperationException("Creating a group is not supporting in LDAP");
    }

    @Override
    public void deleteGroup(String username, String group) {
        throw new UnsupportedOperationException("Deleting a group is not supporting in LDAP");
    }

    @Override
    public List<RolePrincipal> listRoles(Principal principal) {
        try {
            String[] userAndNameSpace = cache.getUserDnAndNamespace(principal.getName());
            if (userAndNameSpace == null || userAndNameSpace.length < 2) return Collections.emptyList();

            ArrayList<RolePrincipal> roles = new ArrayList<>();
            for (String role : cache.getUserRoles(principal.getName(), userAndNameSpace[0], userAndNameSpace[1])) {
                roles.add(new RolePrincipal(role));
            }
            return roles;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addRole(String username, String role) {
        throw new UnsupportedOperationException("Adding a role is not supporting in LDAP");
    }

    @Override
    public void deleteRole(String username, String role) {
        throw new UnsupportedOperationException("Deleting a role is not supporting in LDAP");
    }

    @Override
    public void addGroupRole(String group, String role) {
        throw new UnsupportedOperationException("Adding a group role is not supporting in LDAP");
    }

    @Override
    public void deleteGroupRole(String group, String role) {
        throw new UnsupportedOperationException("Deleting a group role is not supporting in LDAP");
    }
}
