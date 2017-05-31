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
package org.apache.karaf.management.internal;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.karaf.jaas.config.KeystoreInstance;
import org.apache.karaf.jaas.config.KeystoreManager;
import org.apache.karaf.management.ConnectorServerFactory;
import org.apache.karaf.management.JaasAuthenticator;
import org.apache.karaf.management.KarafMBeanServerGuard;
import org.apache.karaf.management.MBeanServerFactory;
import org.apache.karaf.management.RmiRegistryFactory;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.Managed;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
        requires = {
                @RequireService(ConfigurationAdmin.class),
                @RequireService(KeystoreManager.class)
        },
        provides = @ProvideService(MBeanServer.class)
)
@Managed("org.apache.karaf.management")
public class Activator extends BaseActivator implements ManagedService {
    
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class); 

    private ConnectorServerFactory connectorServerFactory;
    private RmiRegistryFactory rmiRegistryFactory;
    private MBeanServerFactory mbeanServerFactory;
    
    private ServiceTracker<KeystoreInstance, KeystoreInstance> keystoreInstanceServiceTracker;

    protected void doStart() throws Exception {
        // Verify dependencies
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        KeystoreManager keystoreManager = getTrackedService(KeystoreManager.class);
        if (configurationAdmin == null || keystoreManager == null) {
            return;
        }

        String rmiRegistryHost = getString("rmiRegistryHost", "");
        int rmiRegistryPort = getInt("rmiRegistryPort", 1099);
        String rmiServerHost = getString("rmiServerHost", "0.0.0.0");
        int rmiServerPort = getInt("rmiServerPort", 44444);

        String jmxRealm = getString("jmxRealm", "karaf");
        String serviceUrl = getString("serviceUrl",
                "service:jmx:rmi://" + rmiServerHost + ":" + rmiServerPort + "/jndi/rmi://" + rmiRegistryHost + ":" + rmiRegistryPort + "/karaf-" + System.getProperty("karaf.name"));

        boolean daemon = getBoolean("daemon", true);
        boolean threaded = getBoolean("threaded", true);
        ObjectName objectName = new ObjectName(getString("objectName", "connector:name=rmi"));
        long keyStoreAvailabilityTimeout = getLong("keyStoreAvailabilityTimeout", 5000);
        String authenticatorType = getString("authenticatorType", "password");
        final boolean secured = getBoolean("secured", false);
        String secureAlgorithm = getString("secureAlgorithm", "default");
        String secureProtocol = getString("secureProtocol", "TLS");
        String keyStore = getString("keyStore", "karaf.ks");
        String keyAlias = getString("keyAlias", "karaf");
        String trustStore = getString("trustStore", "karaf.ts");
        boolean createRmiRegistry = getBoolean("createRmiRegistry", true);
        boolean locateRmiRegistry = getBoolean("locateRmiRegistry", true);
        boolean locateExistingMBeanServerIfPossible = getBoolean("locateExistingMBeanServerIfPossible", true);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(configurationAdmin);

        rmiRegistryFactory = new RmiRegistryFactory();
        rmiRegistryFactory.setCreate(createRmiRegistry);
        rmiRegistryFactory.setLocate(locateRmiRegistry);
        rmiRegistryFactory.setHost(rmiRegistryHost);
        rmiRegistryFactory.setPort(rmiRegistryPort);
        rmiRegistryFactory.setBundleContext(bundleContext);
        rmiRegistryFactory.init();

        mbeanServerFactory = new MBeanServerFactory();
        mbeanServerFactory.setLocateExistingServerIfPossible(locateExistingMBeanServerIfPossible);
        mbeanServerFactory.init();

        MBeanServer mbeanServer = mbeanServerFactory.getServer();

        JaasAuthenticator jaasAuthenticator = new JaasAuthenticator();
        jaasAuthenticator.setRealm(jmxRealm);

        connectorServerFactory = new ConnectorServerFactory();
        connectorServerFactory.setServer(mbeanServer);
        connectorServerFactory.setServiceUrl(serviceUrl);
        connectorServerFactory.setGuard(guard);
        connectorServerFactory.setRmiServerHost(rmiServerHost);
        connectorServerFactory.setDaemon(daemon);
        connectorServerFactory.setThreaded(threaded);
        connectorServerFactory.setObjectName(objectName);
        Map<String, Object> environment = new HashMap<>();
        environment.put("jmx.remote.authenticator", jaasAuthenticator);
        try {
            connectorServerFactory.setEnvironment(environment);
            connectorServerFactory.setKeyStoreAvailabilityTimeout(keyStoreAvailabilityTimeout);
            connectorServerFactory.setAuthenticatorType(authenticatorType);
            connectorServerFactory.setSecured(secured);
            connectorServerFactory.setAlgorithm(secureAlgorithm);
            connectorServerFactory.setSecureProtocol(secureProtocol);
            connectorServerFactory.setKeyStore(keyStore);
            connectorServerFactory.setKeyAlias(keyAlias);
            connectorServerFactory.setTrustStore(trustStore);
            connectorServerFactory.setKeystoreManager(keystoreManager);
            connectorServerFactory.init();
        } catch (Exception e) {
            LOG.error("Can't init JMXConnectorServer: " + e.getMessage());
        }

        JMXSecurityMBeanImpl securityMBean = new JMXSecurityMBeanImpl();
        securityMBean.setMBeanServer(mbeanServer);
        securityMBean.setGuard(guard);
        registerMBean(securityMBean, "type=security,area=jmx");

        register(MBeanServer.class, mbeanServer);
        
        keystoreInstanceServiceTracker = new ServiceTracker<>(
            bundleContext, KeystoreInstance.class, new ServiceTrackerCustomizer<KeystoreInstance, KeystoreInstance>() {
                @Override
                public KeystoreInstance addingService(ServiceReference<KeystoreInstance> reference) {
                    if (secured) {
                        try {
                            connectorServerFactory.init();
                        } catch (Exception e) {
                            LOG.error("Can't re-init JMXConnectorServer with SSL enabled when register a keystore:" + e.getMessage());
                        }
                    }
                    return null;
                }
                @Override
                public void modifiedService(ServiceReference<KeystoreInstance> reference, KeystoreInstance service) {
                }
                @Override
                public void removedService(ServiceReference<KeystoreInstance> reference, KeystoreInstance service) {
                    if (secured) {
                        try {
                            connectorServerFactory.init();
                        } catch (Exception e) {
                            LOG.error("Can't re-init JMXConnectorServer with SSL enabled when unregister a keystore: " + e.getMessage());
                        }
                    }
                }
            });
        keystoreInstanceServiceTracker.open();
    }

    protected void doStop() {
        super.doStop();
        if (connectorServerFactory != null) {
            try {
                connectorServerFactory.destroy();
            } catch (Exception e) {
                logger.warn("Error destroying ConnectorServerFactory", e);
            }
            connectorServerFactory = null;
        }
        if (mbeanServerFactory != null) {
            try {
                mbeanServerFactory.destroy();
            } catch (Exception e) {
                logger.warn("Error destroying MBeanServerFactory", e);
            }
            mbeanServerFactory = null;
        }
        if (rmiRegistryFactory != null) {
            try {
                rmiRegistryFactory.destroy();
            } catch (Exception e) {
                logger.warn("Error destroying RMIRegistryFactory", e);
            }
            rmiRegistryFactory = null;
        }
        if (keystoreInstanceServiceTracker != null) {
            keystoreInstanceServiceTracker.close();
        }
    }

}
