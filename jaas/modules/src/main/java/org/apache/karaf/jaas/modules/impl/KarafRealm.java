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
package org.apache.karaf.jaas.modules.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.osgi.framework.BundleContext;

public class KarafRealm implements JaasRealm {

    private static final String KARAF_ETC = System.getProperty("karaf.etc");
    private static final String REALM = "karaf";
    private static final String EVENTADMIN_MODULE = "org.apache.karaf.jaas.modules.eventadmin.EventAdminLoginModule";
    private static final String PROPERTIES_MODULE = "org.apache.karaf.jaas.modules.properties.PropertiesLoginModule";
    private static final String PUBLIC_KEY_MODULE = "org.apache.karaf.jaas.modules.publickey.PublickeyLoginModule";

    private static final String MODULE = "org.apache.karaf.jaas.module";

    private final BundleContext bundleContext;
    private volatile Map<String, Object> properties;

    public KarafRealm(BundleContext bundleContext, Map<String, Object> properties) {
        this.bundleContext = bundleContext;
        updated(properties);
    }

    public void updated(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String getName() {
        return REALM;
    }

    @Override
    public int getRank() {
        return 0;
    }

    @Override
    public AppConfigurationEntry[] getEntries() {
        Map<String, Object> propertiesOptions = new HashMap<String, Object>();
        propertiesOptions.putAll(properties);
        propertiesOptions.put(BundleContext.class.getName(), bundleContext);
        propertiesOptions.put(ProxyLoginModule.PROPERTY_MODULE, PROPERTIES_MODULE);
        propertiesOptions.put(ProxyLoginModule.PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));
        propertiesOptions.put("users", KARAF_ETC + File.separatorChar + "users.properties");

        Map<String, Object> publicKeyOptions = new HashMap<String, Object>();
        publicKeyOptions.putAll(properties);
        publicKeyOptions.put(BundleContext.class.getName(), bundleContext);
        publicKeyOptions.put(ProxyLoginModule.PROPERTY_MODULE, PUBLIC_KEY_MODULE);
        publicKeyOptions.put(ProxyLoginModule.PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));
        publicKeyOptions.put("users", KARAF_ETC + File.separatorChar + "keys.properties");

        Map<String, Object> eventadminOptions = new HashMap<>();
        eventadminOptions.putAll(properties);
        eventadminOptions.put(BundleContext.class.getName(), bundleContext);
        eventadminOptions.put(ProxyLoginModule.PROPERTY_MODULE, EVENTADMIN_MODULE);
        eventadminOptions.put(ProxyLoginModule.PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));

        return new AppConfigurationEntry[] {
                new AppConfigurationEntry(ProxyLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL, eventadminOptions),
                new AppConfigurationEntry(ProxyLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, propertiesOptions),
                new AppConfigurationEntry(ProxyLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, publicKeyOptions)
        };
    }

}
