/*
 *
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

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LDAPCache {

    public static final String CACHE_TIME_TO_LIVE = "cache.timeToLive";
    public static final int DEFAULT_TIME_TO_LIVE = 60 * 60; // One hour

    private static final ConcurrentMap<Map<String, ?>, LDAPCache> CACHES = new ConcurrentHashMap<>();

    public static void clear() {
        CACHES.clear();
    }

    public static LDAPCache getCache(Map<String, ?> options) {
        LDAPCache cache = CACHES.get(options);
        if (cache == null) {
            CACHES.putIfAbsent(options, new LDAPCache(options));
            cache = CACHES.get(options);
        }
        return cache;
    }

    private final int timeToLive;
    private final ExpiringMap<String, String[]> userDnAndNamespace;
    private final ExpiringMap<String, String[]> userRoles;

    public LDAPCache(Map<String, ?> options) {
        if (options.containsKey(CACHE_TIME_TO_LIVE)) {
            timeToLive = Integer.parseInt(options.get(CACHE_TIME_TO_LIVE).toString());
        } else {
            timeToLive = DEFAULT_TIME_TO_LIVE;
        }
        userDnAndNamespace = new ExpiringMap<>(timeToLive);
        userRoles = new ExpiringMap<>(timeToLive);
    }

    public String[] getUserDnAndNamespace(String user, Callable<String[]> callable) throws Exception {
        String[] result = userDnAndNamespace.get(user);
        if (result == null) {
            result = callable.call();
            if (result != null) {
                userDnAndNamespace.put(user, result);
            }
        }
        return result;
    }

    public String[] getUserRoles(String userDN, Callable<String[]> callable) throws Exception {
        String[] result = userRoles.get(userDN);
        if (result == null) {
            result = callable.call();
            if (result != null) {
                userRoles.put(userDN, result);
            }
        }
        return result;
    }
}
