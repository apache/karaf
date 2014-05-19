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

import org.apache.karaf.jaas.config.KeystoreManager;
import org.apache.karaf.management.ConnectorServerFactory;
import org.apache.karaf.management.JaasAuthenticator;
import org.apache.karaf.management.KarafMBeanServerGuard;
import org.apache.karaf.management.MBeanServerFactory;
import org.apache.karaf.management.RmiRegistryFactory;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.Managed;
import org.apache.karaf.util.tracker.ProvideService;
import org.apache.karaf.util.tracker.RequireService;
import org.apache.karaf.util.tracker.Services;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;

@Services(
        requires = {
                @RequireService(ConfigurationAdmin.class),
                @RequireService(KeystoreManager.class)
        },
        provides = @ProvideService(MBeanServer.class)
)
@Managed("org.apache.karaf.management")
public class Activator extends BaseActivator implements ManagedService {

    private ConnectorServerFactory connectorServerFactory;
    private RmiRegistryFactory rmiRegistryFactory;
    private MBeanServerFactory mbeanServerFactory;

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
                "service:jmx:rmi://0.0.0.0:" + rmiServerPort + "/jndi/rmi://0.0.0.0:" + rmiRegistryPort + "/karaf-" + System.getProperty("karaf.name"));

        boolean daemon = getBoolean("daemon", true);
        boolean threaded = getBoolean("threaded", true);
        ObjectName objectName = new ObjectName(getString("objectName", "connector:name=rmi"));
        long keyStoreAvailabilityTimeout = getLong("keyStoreAvailabilityTimeout", 5000);
        String authenticatorType = getString("authenticatorType", "password");
        boolean secured = getBoolean("secured", false);
        String secureAlgorithm = getString("secureAlgorithm", "default");
        String secureProtocol = getString("secureProtocol", "TLS");
        String keyStore = getString("keyStore", "karaf.ks");
        String keyAlias = getString("keyAlias", "karaf");
        String trustStore = getString("trustStore", "karaf.ts");

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(configurationAdmin);
        guard.init();

        rmiRegistryFactory = new RmiRegistryFactory();
        rmiRegistryFactory.setCreate(true);
        rmiRegistryFactory.setLocate(true);
        rmiRegistryFactory.setHost(rmiRegistryHost);
        rmiRegistryFactory.setPort(rmiRegistryPort);
        rmiRegistryFactory.setBundleContext(bundleContext);
        rmiRegistryFactory.init();

        mbeanServerFactory = new MBeanServerFactory();
        mbeanServerFactory.setLocateExistingServerIfPossible(true);
        mbeanServerFactory.init();

        MBeanServer mbeanServer = mbeanServerFactory.getServer();

        JaasAuthenticator jaasAuthenticator = new JaasAuthenticator();
        jaasAuthenticator.setRealm(jmxRealm);

        connectorServerFactory = new ConnectorServerFactory();
        connectorServerFactory.setServer(mbeanServer);
        connectorServerFactory.setServiceUrl(serviceUrl);
        connectorServerFactory.setRmiServerHost(rmiServerHost);
        connectorServerFactory.setDaemon(daemon);
        connectorServerFactory.setThreaded(threaded);
        connectorServerFactory.setObjectName(objectName);
        Map<String, Object> environment = new HashMap<String, Object>();
        environment.put("jmx.remote.authenticator", jaasAuthenticator);
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

        JMXSecurityMBeanImpl securityMBean = new JMXSecurityMBeanImpl();
        securityMBean.setMBeanServer(mbeanServer);
        registerMBean(securityMBean, "type=security,area=jmx");

        register(MBeanServer.class, mbeanServer);
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
    }

}
