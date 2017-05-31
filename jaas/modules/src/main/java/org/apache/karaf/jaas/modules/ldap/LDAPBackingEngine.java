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
 * Created by andyphillips404 on 5/31/17.
 */
public class LDAPBackingEngine implements BackingEngine {

    LDAPCache cache;
    LDAPOptions options;

    private static Logger LOGGER = LoggerFactory.getLogger(LDAPBackingEngine.class);

    public LDAPBackingEngine(Map<String, ?> options) {
        this.options = new LDAPOptions(options);
        cache = LDAPCache.getCache(this.options);
    }

    @Override
    public void addUser(String username, String password) {
        throw new RuntimeException("Adding a user is not supporting in LDAP");
    }

    @Override
    public void deleteUser(String username) {
        throw new RuntimeException("Deleting a user is not supporting in LDAP");
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

            NamingEnumeration namingEnumeration = context.search(options.getUserBaseDn(), filter, controls);
            try {
                while (namingEnumeration.hasMore()) {
                    SearchResult result = (SearchResult) namingEnumeration.next();

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

                    // Only list principals with roles, since without a role they cannot log in
                    UserPrincipal userPrincipal = new UserPrincipal(userDN);
                    if (listRoles(userPrincipal).size() > 0)
                        users.add(userPrincipal);


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
        throw new RuntimeException("Adding a group is not supporting in LDAP");

    }

    @Override
    public void createGroup(String group) {
        throw new RuntimeException("Creating a group is not supporting in LDAP");

    }

    @Override
    public void deleteGroup(String username, String group) {
        throw new RuntimeException("Deleting a group is not supporting in LDAP");

    }

    @Override
    public List<RolePrincipal> listRoles(Principal principal) {

        try {
            String[] userAndNameSpace = cache.getUserDnAndNamespace(principal.getName());
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
        throw new RuntimeException("Adding a role is not supporting in LDAP");

    }

    @Override
    public void deleteRole(String username, String role) {
        throw new RuntimeException("Deleting a role is not supporting in LDAP");

    }

    @Override
    public void addGroupRole(String group, String role) {
        throw new RuntimeException("Adding a group role is not supporting in LDAP");

    }

    @Override
    public void deleteGroupRole(String group, String role) {
        throw new RuntimeException("Deleting a group role is not supporting in LDAP");

    }


}
