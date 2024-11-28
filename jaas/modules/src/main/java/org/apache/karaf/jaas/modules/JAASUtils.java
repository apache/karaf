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

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import java.security.Principal;
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

    /**
     * Determines the starting index of role and group definitions for a given key in a file-based login module.
     * @param name the property key to evaluate, representing a group or a username
     * @return 0 if the key starts with the group prefix, otherwise 1
     */
    public static int getFirstRoleIndex(String name) {
        if (name.trim().startsWith(BackingEngine.GROUP_PREFIX))
            return 0;
        return 1;
    }

    public static void addRole(Set<Principal> principals, String role) {
        role = role.trim();
        if (!role.isEmpty())
            principals.add(new RolePrincipal(role.trim()));
    }
}