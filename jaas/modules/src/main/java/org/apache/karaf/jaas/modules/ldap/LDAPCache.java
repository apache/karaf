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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.ObjectChangeListener;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LDAPCache implements Closeable, NamespaceChangeListener, ObjectChangeListener {

    private static final ConcurrentMap<LDAPOptions, LDAPCache> CACHES = new ConcurrentHashMap<>();

    private static Logger LOGGER = LoggerFactory.getLogger(LDAPLoginModule.class);

    public static void clear() {
        while (!CACHES.isEmpty()) {
            LDAPOptions options = CACHES.keySet().iterator().next();
            LDAPCache cache = CACHES.remove(options);
            if (cache != null) {
                cache.clearCache();
            }
        }
    }

    public static LDAPCache getCache(LDAPOptions options) {
        LDAPCache cache = CACHES.get(options);
        if (cache == null) {
            CACHES.putIfAbsent(options, new LDAPCache(options));
            cache = CACHES.get(options);
        }
        return cache;
    }

    private final Map<String, String[]> userDnAndNamespace;
    private final Map<String, String[]> userRoles;
    private final Map<String, String[]> userPubkeys;
    private final LDAPOptions options;
    private DirContext context;

    public LDAPCache(LDAPOptions options) {
        this.options = options;
        userDnAndNamespace = new HashMap<>();
        userRoles = new HashMap<>();
        userPubkeys = new HashMap<>();
    }

    @Override
    public synchronized void close() {
        clearCache();
        if (context != null) {
            try {
                context.close();
            } catch (NamingException e) {
                // Ignore
            } finally {
                context = null;
            }
        }
    }

    private boolean isContextAlive() {
        boolean alive = false;
        if (context != null) {
            try {
                context.getAttributes("");
                alive = true;
            } catch (Exception e) {
                // Ignore
            }
        }
        return alive;
    }

    public synchronized DirContext open() throws NamingException {
        if (isContextAlive()) {
            return context;
        }
        clearCache();
        context = new InitialDirContext(options.getEnv());

        EventDirContext eventContext = ((EventDirContext) context.lookup(""));

        final SearchControls constraints = new SearchControls();
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);

        if (!options.getDisableCache()) {
            String filter = options.getUserFilter();
            filter = filter.replaceAll(Pattern.quote("%u"), Matcher.quoteReplacement("*"));
            filter = filter.replace("\\", "\\\\");
            eventContext.addNamingListener(options.getUserBaseDn(), filter, constraints, this);

            filter = options.getRoleFilter();
            if (filter != null) {
                filter = filter.replaceAll(Pattern.quote("%u"), Matcher.quoteReplacement("*"));
                filter = filter.replaceAll(Pattern.quote("%dn"), Matcher.quoteReplacement("*"));
                filter = filter.replaceAll(Pattern.quote("%fqdn"), Matcher.quoteReplacement("*"));
                filter = filter.replace("\\", "\\\\");
                eventContext.addNamingListener(options.getRoleBaseDn(), filter, constraints, this);
            }
        }

        return context;
    }

    public synchronized String[] getUserDnAndNamespace(String user) throws Exception {
        String[] result = userDnAndNamespace.get(user);
        if (result == null) {
            result = doGetUserDnAndNamespace(user);
            if (result != null && !options.getDisableCache()) {
                userDnAndNamespace.put(user, result);
            }
        }
        return result;
    }

    protected String[] doGetUserDnAndNamespace(String user) throws NamingException {
        DirContext context = open();

        SearchControls controls = new SearchControls();
        if (options.getUserSearchSubtree()) {
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        } else {
            controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        }

        String filter = options.getUserFilter();
        filter = filter.replaceAll(Pattern.quote("%u"), Matcher.quoteReplacement(user));
        filter = filter.replace("\\", "\\\\");

        LOGGER.debug("Looking for the user in LDAP with ");
        LOGGER.debug("  base DN: " + options.getUserBaseDn());
        LOGGER.debug("  filter: " + filter);

        NamingEnumeration<SearchResult> namingEnumeration = context.search(options.getUserBaseDn(), filter, controls);
        try {
            if (!namingEnumeration.hasMore()) {
                LOGGER.warn("User " + user + " not found in LDAP.");
                return null;
            }
            LOGGER.debug("Found the user DN.");
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

            return new String[]{userDN, userDNNamespace};
        } finally {
            if (namingEnumeration != null) {
                try {
                    namingEnumeration.close();
                } catch (NamingException e) {
                    // Ignore
                }
            }
        }
    }

    public synchronized String[] getUserRoles(String user, String userDn, String userDnNamespace) throws Exception {
        String[] result = userRoles.get(userDn);
        if (result == null) {
            result = doGetUserRoles(user, userDn, userDnNamespace);
            if (!options.getDisableCache()) {
                userRoles.put(userDn, result);
            }
        }
        return result;
    }

    public synchronized String[] getUserPubkeys(String userDn) throws NamingException {
        String[] result = userPubkeys.get(userDn);
        if (result == null) {
            result = doGetUserPubkeys(userDn);
            if (!options.getDisableCache()) {
                userPubkeys.put(userDn, result);
            }
        }
        return result;
    }


    protected Set<String> tryMappingRole(String role) {
        Set<String> roles = new HashSet<>();
        if (options.getRoleMapping().isEmpty()) {
            return roles;
        }
        Set<String> karafRoles = options.getRoleMapping().get(role);
        if (karafRoles != null) {
            // add all mapped roles
            for (String karafRole : karafRoles) {
                LOGGER.debug("LDAP role {} is mapped to Karaf role {}", role, karafRole);
                roles.add(karafRole);
            }
        }
        return roles;
    }


    private String[] doGetUserRoles(String user, String userDn, String userDnNamespace) throws NamingException {
        DirContext context = open();

        SearchControls controls = new SearchControls();
        if (options.getRoleSearchSubtree()) {
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        } else {
            controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        }

        String filter = options.getRoleFilter();
        if (filter != null) {
            filter = filter.replaceAll(Pattern.quote("%u"), Matcher.quoteReplacement(user));
            filter = filter.replaceAll(Pattern.quote("%dn"), Matcher.quoteReplacement(userDn));
            filter = filter.replaceAll(Pattern.quote("%fqdn"), Matcher.quoteReplacement(userDnNamespace));
            filter = filter.replace("\\", "\\\\");

            LOGGER.debug("Looking for the user roles in LDAP with ");
            LOGGER.debug("  base DN: {}", options.getRoleBaseDn());
            LOGGER.debug("  filter: {}", filter);

            NamingEnumeration<SearchResult> namingEnumeration = context.search(options.getRoleBaseDn(), filter, controls);
            List<String> rolesList = new ArrayList<>();
            try {
                while (namingEnumeration.hasMore()) {
                    SearchResult result = namingEnumeration.next();
                    Attributes attributes = result.getAttributes();
                    Attribute roles1 = attributes.get(options.getRoleNameAttribute());
                    if (roles1 != null) {
                        for (int i = 0; i < roles1.size(); i++) {
                            String role = (String) roles1.get(i);
                            if (role != null) {
                                LOGGER.debug("User {} is a member of role {}", user, role);
                                // handle role mapping
                                Set<String> roleMappings = tryMappingRole(role);
                                if (roleMappings.isEmpty()) {
                                    rolesList.add(role);
                                } else {
                                    rolesList.addAll(roleMappings);
                                }
                            }
                        }
                    }
                }
            } catch (PartialResultException e) {
                // Workaround for AD servers not handling referrals correctly.
                if (options.getIgnorePartialResultException()) {
                    LOGGER.debug("PartialResultException encountered and ignored", e);
                }
                else {
                    throw e;
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

            return rolesList.toArray(new String[rolesList.size()]);
        } else {
            LOGGER.debug("The user role filter is null so no roles are retrieved");
            return new String[] {};
        }
    }

    private String[] doGetUserPubkeys(String userDn) throws NamingException {
        DirContext context = open();

        String userPubkeyAttribute = options.getUserPubkeyAttribute();
        if (userPubkeyAttribute != null) {
            LOGGER.debug("Looking for public keys of user {} in attribute {}", userDn, userPubkeyAttribute);

            Attributes attributes = context.getAttributes(userDn, new String[]{userPubkeyAttribute});
            Attribute pubkeyAttribute = attributes.get(userPubkeyAttribute);

            List<String> pubkeyList = new ArrayList<>();
            if (pubkeyAttribute != null) {
                for (int i = 0; i < pubkeyAttribute.size(); i++) {
                    String pk = (String) pubkeyAttribute.get(i);
                    if (pk != null) {
                        pubkeyList.add(pk);
                    }
                }
            }
            return pubkeyList.toArray(new String[pubkeyList.size()]);
        } else {
            LOGGER.debug("The user public key attribute is null so no keys were retrieved");
            return new String[] {};
        }
    }


    @Override
    public void objectAdded(NamingEvent evt) {
        clearCache();
    }

    @Override
    public void objectRemoved(NamingEvent evt) {
        clearCache();
    }

    @Override
    public void objectRenamed(NamingEvent evt) {
        clearCache();
    }

    @Override
    public void objectChanged(NamingEvent evt) {
        clearCache();
    }

    @Override
    public void namingExceptionThrown(NamingExceptionEvent evt) {
        clearCache();
    }

    protected synchronized void clearCache() {
        userDnAndNamespace.clear();
        userRoles.clear();
        userPubkeys.clear();
    }
}
