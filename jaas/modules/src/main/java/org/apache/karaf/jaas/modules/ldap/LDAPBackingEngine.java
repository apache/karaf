package org.apache.karaf.jaas.modules.ldap;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.ldap.LDAPLoginModule;

import java.util.Map;

/**
 * Created by andyphillips404 on 5/31/17.
 */

public class LDAPBackingEngineFactory implements BackingEngineFactory {
    @Override
    public String getModuleClass() {
        return LDAPLoginModule.class.getName();
    }

    @Override
    public BackingEngine build(Map<String, ?> options) {
        return new LDAPBackingEngine(options);
    }
}
