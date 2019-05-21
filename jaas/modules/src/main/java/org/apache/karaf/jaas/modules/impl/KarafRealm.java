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
    private static final String PROPERTIES_MODULE = "org.apache.karaf.jaas.modules.properties.PropertiesLoginModule";
    private static final String PUBLIC_KEY_MODULE = "org.apache.karaf.jaas.modules.publickey.PublickeyLoginModule";
    private static final String FILE_AUDIT_MODULE = "org.apache.karaf.jaas.modules.audit.FileAuditLoginModule";
    private static final String LOG_AUDIT_MODULE = "org.apache.karaf.jaas.modules.audit.LogAuditLoginModule";
    private static final String EVENTADMIN_AUDIT_MODULE = "org.apache.karaf.jaas.modules.audit.EventAdminAuditLoginModule";

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
        Map<String, Object> propertiesOptions = new HashMap<>();
        propertiesOptions.put(BundleContext.class.getName(), bundleContext);
        propertiesOptions.put(ProxyLoginModule.PROPERTY_MODULE, PROPERTIES_MODULE);
        propertiesOptions.put(ProxyLoginModule.PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));
        propertiesOptions.put("users", KARAF_ETC + File.separatorChar + "users.properties");
        propertiesOptions.put("detailed.login.exception", properties.get("detailed.login.exception"));
        propertiesOptions.put("encryption.name", properties.get("encryption.name"));
        propertiesOptions.put("encryption.enabled", properties.get("encryption.enabled"));
        propertiesOptions.put("encryption.prefix", properties.get("encryption.prefix"));
        propertiesOptions.put("encryption.suffix", properties.get("encryption.suffix"));
        propertiesOptions.put("encryption.algorithm", properties.get("encryption.algorithm"));
        propertiesOptions.put("encryption.encoding", properties.get("encryption.encoding"));

        Map<String, Object> publicKeyOptions = new HashMap<>();
        publicKeyOptions.put(BundleContext.class.getName(), bundleContext);
        publicKeyOptions.put(ProxyLoginModule.PROPERTY_MODULE, PUBLIC_KEY_MODULE);
        publicKeyOptions.put(ProxyLoginModule.PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));
        publicKeyOptions.put("users", KARAF_ETC + File.separatorChar + "keys.properties");
        publicKeyOptions.put("detailed.login.exception", properties.get("detailed.login.exception"));

        Map<String, Object> fileOptions = new HashMap<>();
        fileOptions.put(BundleContext.class.getName(), bundleContext);
        fileOptions.put(ProxyLoginModule.PROPERTY_MODULE, FILE_AUDIT_MODULE);
        fileOptions.put(ProxyLoginModule.PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));
        fileOptions.put("enabled", properties.get("audit.file.enabled"));
        fileOptions.put("file", properties.get("audit.file.file"));

        Map<String, Object> logOptions = new HashMap<>();
        logOptions.put(BundleContext.class.getName(), bundleContext);
        logOptions.put(ProxyLoginModule.PROPERTY_MODULE, LOG_AUDIT_MODULE);
        logOptions.put(ProxyLoginModule.PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));
        logOptions.put("enabled", properties.get("audit.log.enabled"));
        logOptions.put("logger", properties.get("audit.log.logger"));
        logOptions.put("level", properties.get("audit.log.level"));

        Map<String, Object> eventadminOptions = new HashMap<>(properties);
        eventadminOptions.put(BundleContext.class.getName(), bundleContext);
        eventadminOptions.put(ProxyLoginModule.PROPERTY_MODULE, EVENTADMIN_AUDIT_MODULE);
        eventadminOptions.put(ProxyLoginModule.PROPERTY_BUNDLE, Long.toString(bundleContext.getBundle().getBundleId()));
        eventadminOptions.put("enabled", properties.get("audit.eventadmin.enabled"));
        eventadminOptions.put("topic", properties.get("audit.eventadmin.topic"));

        return new AppConfigurationEntry[] {
                new AppConfigurationEntry(ProxyLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL, propertiesOptions),
                new AppConfigurationEntry(ProxyLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL, publicKeyOptions),
                new AppConfigurationEntry(ProxyLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL, fileOptions),
                new AppConfigurationEntry(ProxyLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL, logOptions),
                new AppConfigurationEntry(ProxyLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL, eventadminOptions)
        };
    }

}
