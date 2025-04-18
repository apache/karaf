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

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class JAASUtils {

    private JAASUtils() {
        // complete
    }

    public static String getString(Map<String, ?> options, String key) {
        Object val = options.get(key);
        if (val instanceof String) {
            val = ((String)val).trim();
        }
        return (String)val;
    }

    public static Set<Principal> getPrincipals(String user, Properties users, String[] infos) {
        Set<Principal> principals = new HashSet<>();
        principals.add(new UserPrincipal(user));

        for (int i = 1; i < infos.length; i++) {
            if (infos[i].trim().startsWith(BackingEngine.GROUP_PREFIX)) {
                // it's a group reference
                principals.add(new GroupPrincipal(infos[i].trim().substring(BackingEngine.GROUP_PREFIX.length())));
                String groupInfo = users.get(infos[i].trim());
                if (groupInfo != null) {
                    String[] roles = groupInfo.split(",");
                    for (int j = 0; j < roles.length; j++) {
                        addRole(principals, roles[j]);
                    }
                }
            } else {
                // it's an user reference
                addRole(principals, infos[i]);
            }
        }
        return principals;
    }

    static void addRole(Set<Principal> principals, String role) {
        role = role.trim();
        if (!role.isEmpty())
            principals.add(new RolePrincipal(role));
    }
}