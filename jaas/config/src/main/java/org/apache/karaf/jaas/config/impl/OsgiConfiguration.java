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

import java.util.List;
import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.util.collections.CopyOnWriteArrayIdentityList;
import org.slf4j.LoggerFactory;

public class OsgiConfiguration extends Configuration {

    private final List<JaasRealm> realms = new CopyOnWriteArrayIdentityList<>();
    private Configuration defaultConfiguration;

    public void init() {
        try {
            defaultConfiguration = Configuration.getConfiguration();
        } catch (Throwable ex) {
            // default configuration for fallback could not be retrieved
            LoggerFactory.getLogger(OsgiConfiguration.class).warn("Unable to retrieve default configuration", ex);
        }
        Configuration.setConfiguration(this);
    }

    public void close() {
        realms.clear();
        Configuration.setConfiguration(defaultConfiguration);
    }

    public void register(JaasRealm realm, Map<String,?> properties) {
        if (realm != null) {
            realms.add(realm);
        }
    }

    public void unregister(JaasRealm realm, Map<String,?> properties) {
        if (realm != null) {
            realms.remove(realm);
        }
    }

    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
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
        } else if (defaultConfiguration != null) {
            return defaultConfiguration.getAppConfigurationEntry(name);
        }
        return null;
    }

    public void refresh() {
        if (defaultConfiguration != null) {
            defaultConfiguration.refresh();
        }
    }
}
