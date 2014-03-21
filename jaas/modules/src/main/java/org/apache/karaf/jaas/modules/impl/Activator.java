package org.apache.karaf.jaas.modules.impl;

import java.util.Hashtable;

import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.EncryptionService;
import org.apache.karaf.jaas.modules.encryption.BasicEncryptionService;
import org.apache.karaf.jaas.modules.properties.PropertiesBackingEngineFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

public class Activator implements BundleActivator {

    private ServiceRegistration<BackingEngineFactory> propertiesBackingEngineFactoryServiceRegistration;
    private ServiceRegistration<EncryptionService> basicEncryptionServiceServiceRegistration;
    private ServiceRegistration karafRealmServiceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        propertiesBackingEngineFactoryServiceRegistration =
            context.registerService(BackingEngineFactory.class, new PropertiesBackingEngineFactory(), null);

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_RANKING, -1);
        props.put("name", "basic");
        basicEncryptionServiceServiceRegistration =
                context.registerService(EncryptionService.class, new BasicEncryptionService(), props);

        props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, "org.apache.karaf.jaas");
        karafRealmServiceRegistration =
                context.registerService(new String[] {
                        JaasRealm.class.getName(),
                        ManagedService.class.getName()
                }, new KarafRealm(context), props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        karafRealmServiceRegistration.unregister();
        basicEncryptionServiceServiceRegistration.unregister();
        propertiesBackingEngineFactoryServiceRegistration.unregister();
    }
}
