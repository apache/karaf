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

import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import javax.security.auth.login.AppConfigurationEntry;
import java.io.File;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KarafRealm implements JaasRealm, ManagedService {

    private static final String KARAF_ETC = System.getProperty("karaf.base") + File.separatorChar + "etc";
    private static final String REALM = "karaf";
    private static final String PROPERTIES_MODULE = "org.apache.karaf.jaas.modules.properties.PropertiesLoginModule";
    private static final String PUBLIC_KEY_MODULE = "org.apache.karaf.jaas.modules.publickey.PublickeyLoginModule";

    private static final String ENCRYPTION_NAME = "encryption.name";
    private static final String ENCRYPTION_ENABLED = "encryption.enabled";
    private static final String ENCRYPTION_PREFIX = "encryption.prefix";
    private static final String ENCRYPTION_SUFFIX = "encryption.suffix";
    private static final String ENCRYPTION_ALGORITHM = "encryption.algorithm";
    private static final String ENCRYPTION_ENCODING = "encryption.encoding";
    private static final String MODULE = "org.apache.karaf.jaas.module";

    private final BundleContext bundleContext;
    private volatile Map<String, Object> properties;

    public KarafRealm(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.properties = new HashMap<String, Object>();
        populateDefault(this.properties);
    }

    private void populateDefault(Map<String, Object> props) {
        props.put("detailed.login.exception", "false");
        props.put(ENCRYPTION_NAME, "");
        props.put(ENCRYPTION_ENABLED, "false");
        props.put(ENCRYPTION_PREFIX, "{CRYPT}");
        props.put(ENCRYPTION_SUFFIX, "{CRYPT}");
        props.put(ENCRYPTION_ALGORITHM, "MD5");
        props.put(ENCRYPTION_ENCODING, "hexadecimal");
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        Map<String, Object> props = new HashMap<String, Object>();
        populateDefault(props);
        for (Enumeration<String> keyEnum = properties.keys(); keyEnum.hasMoreElements(); ) {
            String key = keyEnum.nextElement();
            props.put(key, properties.get(key));
        }
        this.properties = props;
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

        return new AppConfigurationEntry[] {
                new AppConfigurationEntry(ProxyLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, propertiesOptions),
                new AppConfigurationEntry(ProxyLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, publicKeyOptions)
        };
    }

}
