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

import java.util.Hashtable;

import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.EncryptionService;
import org.apache.karaf.jaas.modules.encryption.BasicEncryptionService;
import org.apache.karaf.jaas.modules.properties.PropertiesBackingEngineFactory;
import org.apache.karaf.jaas.modules.publickey.PublickeyBackingEngineFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

public class Activator implements BundleActivator {

    private ServiceRegistration<BackingEngineFactory> propertiesBackingEngineFactoryServiceRegistration;
    private ServiceRegistration<BackingEngineFactory> publickeyBackingEngineFactoryServiceRegistration;
    private ServiceRegistration<EncryptionService> basicEncryptionServiceServiceRegistration;
    private ServiceRegistration karafRealmServiceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        propertiesBackingEngineFactoryServiceRegistration =
            context.registerService(BackingEngineFactory.class, new PropertiesBackingEngineFactory(), null);
        publickeyBackingEngineFactoryServiceRegistration =
            context.registerService(BackingEngineFactory.class, new PublickeyBackingEngineFactory(), null);

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
