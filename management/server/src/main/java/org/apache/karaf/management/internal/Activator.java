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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.karaf.jaas.config.KeystoreManager;
import org.apache.karaf.management.ConnectorServerFactory;
import org.apache.karaf.management.JaasAuthenticator;
import org.apache.karaf.management.KarafMBeanServerGuard;
import org.apache.karaf.management.MBeanServerFactory;
import org.apache.karaf.management.RmiRegistryFactory;
import org.apache.karaf.util.tracker.SingleServiceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, ManagedService, SingleServiceTracker.SingleServiceListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private AtomicBoolean scheduled = new AtomicBoolean();
    private BundleContext bundleContext;
    private Dictionary<String, ?> configuration;
    private ServiceRegistration registration;
    private SingleServiceTracker<ConfigurationAdmin> configAdminTracker;
    private SingleServiceTracker<KeystoreManager> keystoreManagerTracker;
    private ServiceRegistration<MBeanServer> serverRegistration;
    private ServiceRegistration securityRegistration;
    private ConnectorServerFactory connectorServerFactory;
    private RmiRegistryFactory rmiRegistryFactory;
    private MBeanServerFactory mbeanServerFactory;

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        scheduled.set(true);

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, "org.apache.karaf.management");
        registration = bundleContext.registerService(ManagedService.class, this, props);

        configAdminTracker = new SingleServiceTracker<ConfigurationAdmin>(
                bundleContext, ConfigurationAdmin.class, this);
        keystoreManagerTracker = new SingleServiceTracker<KeystoreManager>(
                bundleContext, KeystoreManager.class, this);
        configAdminTracker.open();
        keystoreManagerTracker.open();

        scheduled.set(false);
        reconfigure();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        keystoreManagerTracker.close();
        configAdminTracker.close();
        registration.unregister();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        this.configuration = properties;
        reconfigure();
    }

    @Override
    public void serviceFound() {
        reconfigure();
    }

    @Override
    public void serviceLost() {
        reconfigure();
    }

    @Override
    public void serviceReplaced() {
        reconfigure();
    }

    protected void reconfigure() {
        if (scheduled.compareAndSet(false, true)) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    scheduled.set(false);
                    doStop();
                    try {
                        doStart();
                    } catch (Exception e) {
                        LOGGER.warn("Error starting management layer", e);
                        doStop();
                    }
                }
            });
        }
    }

    protected void doStart() throws Exception {
        // This can happen while the bundle is starting as we register
        // the ManagedService before creating the service trackers
        if (configAdminTracker == null || keystoreManagerTracker == null) {
            return;
        }
        // Verify dependencies
        ConfigurationAdmin configurationAdmin = configAdminTracker.getService();
        KeystoreManager keystoreManager = keystoreManagerTracker.getService();
        Dictionary<String, ?> config = configuration;
        if (configurationAdmin == null || keystoreManager == null) {
            return;
        }

        String rmiRegistryHost = getString(config, "rmiRegistryHost", "");
        int rmiRegistryPort = getInt(config, "rmiRegistryPort", 1099);
        String rmiServerHost = getString(config, "rmiServerHost", "0.0.0.0");
        int rmiServerPort = getInt(config, "rmiServerPort", 44444);

        String jmxRealm = getString(config, "jmxRealm", "karaf");
        String serviceUrl = getString(config, "serviceUrl",
                "service:jmx:rmi://0.0.0.0:" + rmiServerPort + "/jndi/rmi://0.0.0.0:" + rmiRegistryPort + "/karaf-" + System.getProperty("karaf.name"));

        boolean daemon = getBoolean(config, "daemon", true);
        boolean threaded = getBoolean(config, "threaded", true);
        ObjectName objectName = new ObjectName(getString(config, "objectName", "connector:name=rmi"));
        long keyStoreAvailabilityTimeout = getLong(config, "keyStoreAvailabilityTimeout", 5000);
        String authenticatorType = getString(config, "authenticatorType", "password");
        boolean secured = getBoolean(config, "secured", false);
        String secureAlgorithm = getString(config, "secureAlgorithm", "default");
        String secureProtocol = getString(config, "secureProtocol", "TLS");
        String keyStore = getString(config, "keyStore", "karaf.ks");
        String keyAlias = getString(config, "keyAlias", "karaf");
        String trustStore = getString(config, "trustStore", "karaf.ts");

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

        try {
            JMXSecurityMBeanImpl securityMBean = new JMXSecurityMBeanImpl();
            securityMBean.setMBeanServer(mbeanServer);
            Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put("jmx.objectname", "org.apache.karaf:type=security,area=jmx,name=" + System.getProperty("karaf.name"));
            securityRegistration = bundleContext.registerService(
                    getInterfaceNames(securityMBean),
                    securityMBean,
                    props
            );
        } catch (NotCompliantMBeanException e) {
            LOGGER.warn("Error creating JMX security mbean", e);
        }

        serverRegistration = bundleContext.registerService(MBeanServer.class, mbeanServer, null);
    }

    protected void doStop() {
        if (securityRegistration != null) {
            securityRegistration.unregister();
            securityRegistration = null;
        }
        if (serverRegistration != null) {
            serverRegistration.unregister();
            serverRegistration = null;
        }
        if (connectorServerFactory != null) {
            try {
                connectorServerFactory.destroy();
            } catch (Exception e) {
                LOGGER.warn("Error destroying ConnectorServerFactory", e);
            }
            connectorServerFactory = null;
        }
        if (mbeanServerFactory != null) {
            try {
                mbeanServerFactory.destroy();
            } catch (Exception e) {
                LOGGER.warn("Error destroying MBeanServerFactory", e);
            }
            mbeanServerFactory = null;
        }
        if (rmiRegistryFactory != null) {
            try {
                rmiRegistryFactory.destroy();
            } catch (Exception e) {
                LOGGER.warn("Error destroying RMIRegistryFactory", e);
            }
            rmiRegistryFactory = null;
        }
    }

    private String[] getInterfaceNames(Object object) {
        List<String> names = new ArrayList<String>();
        for (Class cl = object.getClass(); cl != Object.class; cl = cl.getSuperclass()) {
            addSuperInterfaces(names, cl);
        }
        return names.toArray(new String[names.size()]);
    }

    private void addSuperInterfaces(List<String> names, Class clazz) {
        for (Class cl : clazz.getInterfaces()) {
            names.add(cl.getName());
            addSuperInterfaces(names, cl);
        }
    }

    private int getInt(Dictionary<String, ?> config, String key, int def) {
        if (config != null) {
            Object val = config.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            } else if (val != null) {
                return Integer.parseInt(val.toString());
            }
        }
        return def;
    }

    private long getLong(Dictionary<String, ?> config, String key, long def) {
        if (config != null) {
            Object val = config.get(key);
            if (val instanceof Number) {
                return ((Number) val).longValue();
            } else if (val != null) {
                return Long.parseLong(val.toString());
            }
        }
        return def;
    }

    private boolean getBoolean(Dictionary<String, ?> config, String key, boolean def) {
        if (config != null) {
            Object val = config.get(key);
            if (val instanceof Boolean) {
                return (Boolean) val;
            } else if (val != null) {
                return Boolean.parseBoolean(val.toString());
            }
        }
        return def;
    }

    private String getString(Dictionary<String, ?> config, String key, String def) {
        if (config != null) {
            Object val = config.get(key);
            if (val != null) {
                return val.toString();
            }
        }
        return def;
    }

}
