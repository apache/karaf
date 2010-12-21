/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.jaas.config.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.apache.karaf.jaas.config.JaasRealm;

public class OsgiConfiguration extends Configuration {

    private final List<JaasRealm> realms = new ArrayList<JaasRealm>();

    public synchronized void init() {
        Configuration.setConfiguration(this);
    }

    public synchronized void close() {
        realms.clear();
        Configuration.setConfiguration(null);
    }

    public synchronized void register(JaasRealm realm, Map<String,?> properties) {
        realms.add(realm);
    }

    public synchronized void unregister(JaasRealm realm, Map<String,?> properties) {
        for (Iterator<JaasRealm> it = realms.iterator(); it.hasNext();) {
            if (it.next() == realm) {
                it.remove();
            }
        }
    }

    public synchronized AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        JaasRealm realm = null;
        for (JaasRealm r : realms) {
            if (r.getName().equals(name)) {
                if (realm == null || r.getRank() > realm.getRank()) {
                    realm = r;
                }
            }
        }
        if (realm != null) {
            return realm.getEntries();
        }
        return null;
    }

    public synchronized void refresh() {
        // Nothing to do, as we auto-update the configuration
    }
}
