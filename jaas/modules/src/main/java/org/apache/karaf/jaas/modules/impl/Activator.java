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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.EncryptionService;
import org.apache.karaf.jaas.modules.encryption.BasicEncryptionService;
import org.apache.karaf.jaas.modules.ldap.LDAPCache;
import org.apache.karaf.jaas.modules.properties.AutoEncryptionSupport;
import org.apache.karaf.jaas.modules.properties.PropertiesBackingEngineFactory;
import org.apache.karaf.jaas.modules.publickey.PublickeyBackingEngineFactory;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.Managed;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;

@Managed("org.apache.karaf.jaas")
@Services(provides = {
        @ProvideService(JaasRealm.class),
        @ProvideService(BackingEngineFactory.class)
})
public class Activator extends BaseActivator implements ManagedService {

    private static final String ENCRYPTION_NAME = "encryption.name";
    private static final String ENCRYPTION_ENABLED = "encryption.enabled";
    private static final String ENCRYPTION_PREFIX = "encryption.prefix";
    private static final String ENCRYPTION_SUFFIX = "encryption.suffix";
    private static final String ENCRYPTION_ALGORITHM = "encryption.algorithm";
    private static final String ENCRYPTION_ENCODING = "encryption.encoding";

    private static final String EVENTADMIN_ENABLED = "eventadmin.enabled";

    private KarafRealm karafRealm;
    private AutoEncryptionSupport autoEncryptionSupport;

    @Override
    protected void doOpen() throws Exception {
        super.doOpen();
        register(BackingEngineFactory.class, new PropertiesBackingEngineFactory());
        register(BackingEngineFactory.class, new PublickeyBackingEngineFactory());

        Hashtable<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_RANKING, -1);
        props.put("name", "basic");
        register(EncryptionService.class, new BasicEncryptionService(), props);

        Map<String, Object> config = getConfig();

        karafRealm = new KarafRealm(bundleContext, config);
        register(JaasRealm.class, karafRealm);
        if (Boolean.parseBoolean((String) config.get(ENCRYPTION_ENABLED))) {
          autoEncryptionSupport = new AutoEncryptionSupport(config);
        }
    }

    @Override
    protected void doStop() {
        if (autoEncryptionSupport != null) {
          autoEncryptionSupport.close();
        }
        super.doStop();
        LDAPCache.clear();
    }

    @Override
    protected void reconfigure() {
        Map<String, Object> config = getConfig();
        if (karafRealm != null) {
            karafRealm.updated(config);
        }
        if (autoEncryptionSupport != null) {
            autoEncryptionSupport.close();
            autoEncryptionSupport = null;
        }
        if (Boolean.parseBoolean((String) config.get(ENCRYPTION_ENABLED))) {
          autoEncryptionSupport = new AutoEncryptionSupport(config);
        }
    }

    private Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        populate(config, "detailed.login.exception", "false");
        populate(config, ENCRYPTION_NAME, "basic");
        populate(config, ENCRYPTION_ENABLED, "false");
        populate(config, ENCRYPTION_PREFIX, "{CRYPT}");
        populate(config, ENCRYPTION_SUFFIX, "{CRYPT}");
        populate(config, ENCRYPTION_ALGORITHM, "SHA-256");
        populate(config, ENCRYPTION_ENCODING, "hexadecimal");
        populate(config, EVENTADMIN_ENABLED, "true");
        populate(config, "audit.file.enabled", "false");
        populate(config, "audit.file.file", System.getProperty("karaf.data") + "/security/audit.log");
        populate(config, "audit.log.enabled", "false");
        populate(config, "audit.log.logger", "org.apache.karaf.jaas.modules.audit.LogAuditLoginModule");
        populate(config, "audit.log.level", "info");
        populate(config, "audit.eventadmin.enabled", "true");
        populate(config, "audit.eventadmin.topic", "org/apache/karaf/login");
        config.put(BundleContext.class.getName(), bundleContext);
        return config;
    }

    private void populate(Map<String, Object> map, String key, String def) {
        map.put(key, getString(key, def));
    }

}
