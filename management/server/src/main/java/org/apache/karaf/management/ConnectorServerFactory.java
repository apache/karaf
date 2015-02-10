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
package org.apache.karaf.management;

import org.apache.karaf.jaas.config.KeystoreInstance;
import org.apache.karaf.jaas.config.KeystoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.security.GeneralSecurityException;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;

public class ConnectorServerFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorServerFactory.class);

    private enum AuthenticatorType { NONE, PASSWORD, CERTIFICATE };

    private MBeanServer server;
    private String serviceUrl;
    private String rmiServerHost;
    private Map environment;
    private ObjectName objectName;
    private boolean threaded = false;
    private boolean daemon = false;
    private JMXConnectorServer connectorServer;

    private long keyStoreAvailabilityTimeout = 5000;
    private AuthenticatorType authenticatorType = AuthenticatorType.PASSWORD;
    private boolean secured;
    private KeystoreManager keystoreManager;
    private String algorithm;
    private String secureProtocol;
    private String keyStore;
    private String trustStore;
    private String keyAlias;

    public MBeanServer getServer() {
        return server;
    }

    public void setServer(MBeanServer server) {
        this.server = server;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public Map getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map environment) {
        this.environment = environment;
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    public boolean isThreaded() {
        return threaded;
    }

    public void setThreaded(boolean threaded) {
        this.threaded = threaded;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public String getAuthenticatorType() {
        return this.authenticatorType.name().toLowerCase();
    }

    /**
     * Authenticator type to use. Acceptable values are "none", "password", and "certificate".
     *
     * @param value the authenticator type to use.
     */
    public void setAuthenticatorType(String value) {
        this.authenticatorType = AuthenticatorType.valueOf(value.toUpperCase());
    }

    /**
     * Use this param to allow the KeystoreManager to wait for expected keystores to be loaded by other bundle
     *
     * @param keyStoreAvailabilityTimeout the keystore availability timeout in milliseconds
     */
    public void setKeyStoreAvailabilityTimeout(long keyStoreAvailabilityTimeout) {
        this.keyStoreAvailabilityTimeout = keyStoreAvailabilityTimeout;
    }

    public boolean isSecured() {
        return this.secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public void setKeystoreManager(KeystoreManager keystoreManager) {
        this.keystoreManager = keystoreManager;
    }

    public KeystoreManager getKeystoreManager() {
        return this.keystoreManager;
    }

    public String getKeyStore() {
        return this.keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public String getTrustStore() {
        return this.trustStore;
    }

    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    public String getKeyAlias() {
        return this.keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Algorithm to use.
     * As different JVMs have different implementation available, the default algorithm can be used by supplying the value "Default".
     *
     * @param algorithm the algorithm to use, or "Default" to use the default from {@link javax.net.ssl.KeyManagerFactory#getDefaultAlgorithm()}
     */
    public void setAlgorithm(String algorithm) {
        if ("default".equalsIgnoreCase(algorithm)) {
            this.algorithm = KeyManagerFactory.getDefaultAlgorithm();
        } else {
            this.algorithm = algorithm;
        }
    }

    public String getSecureProtocol() {
        return this.secureProtocol;
    }

    public void setSecureProtocol(String secureProtocol) {
        this.secureProtocol = secureProtocol;
    }

    private boolean isClientAuth() {
        return this.authenticatorType.equals(AuthenticatorType.CERTIFICATE);
    }

    public void init() throws Exception {
        if (this.server == null) {
            throw new IllegalArgumentException("server must be set");
        }
        JMXServiceURL url = new JMXServiceURL(this.serviceUrl);
        setupKarafRMIServerSocketFactory();
        if (isClientAuth()) {
            this.secured = true;
        }

        if (this.secured) {
            try {
                this.setupSsl();
            } catch (Exception e) {
                LOGGER.error("Can't init JMXConnectorServer with SSL enabled: " + e.getMessage());
                return;
            }
        }

        if (!AuthenticatorType.PASSWORD.equals(this.authenticatorType)) {
            this.environment.remove("jmx.remote.authenticator");
        }

        this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, this.environment, this.server);
        if (this.objectName != null) {
            this.server.registerMBean(this.connectorServer, this.objectName);
        }

        try {
            if (this.threaded) {
                Thread connectorThread = new Thread() {
                    public void run() {
                        try {
                            Thread.currentThread().setContextClassLoader(ConnectorServerFactory.class.getClassLoader());
                            connectorServer.start();
                        } catch (IOException ex) {
                            if (ex.getCause() instanceof BindException){
                                // we want just the port message
                                int endIndex = ex.getMessage().indexOf("nested exception is");
                                // check to make sure we do not get an index out of range
                                if (endIndex > ex.getMessage().length() || endIndex < 0){
                                    endIndex = ex.getMessage().length();
                                }
                                throw new RuntimeException("\n" + ex.getMessage().substring(0, endIndex) +
                                        "\nYou may have started two containers.  If you need to start a second container or the default ports are already in use " +
                                        "update the config file etc/org.apache.karaf.management.cfg and change the Registry Port and Server Port to unused ports");
                            }
                            throw new RuntimeException("Could not start JMX connector server", ex);
                        }
                    }
                };
                connectorThread.setName("JMX Connector Thread [" + this.serviceUrl + "]");
                connectorThread.setDaemon(this.daemon);
                connectorThread.start();
            }
            else {
                this.connectorServer.start();
            }
        } catch (Exception ex) {
            doUnregister(this.objectName);
            throw ex;
        }
    }

    public void destroy() throws Exception {
        try {
            this.connectorServer.stop();
        } finally {
            doUnregister(this.objectName);
        }
    }

    protected void doUnregister(ObjectName objectName) {
        try {
            if (this.objectName != null && this.server.isRegistered(objectName)) {
                this.server.unregisterMBean(objectName);
            }
        }
        catch (JMException ex) {
            // Ignore
        }
    }

    private void setupSsl() throws GeneralSecurityException {
        SSLServerSocketFactory sslServerSocketFactory = keystoreManager.createSSLServerFactory(null, secureProtocol, algorithm, keyStore, keyAlias, trustStore, keyStoreAvailabilityTimeout);
        RMIServerSocketFactory rmiServerSocketFactory = new KarafSslRMIServerSocketFactory(sslServerSocketFactory, this.isClientAuth(), getRmiServerHost());
        RMIClientSocketFactory rmiClientSocketFactory = new SslRMIClientSocketFactory();
        environment.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, rmiServerSocketFactory);
        environment.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, rmiClientSocketFactory);
        // TODO secure RMI connector as well ?
        // environment.put("com.sun.jndi.rmi.factory.socket", rmiClientSocketFactory);
    }

    private void setupKarafRMIServerSocketFactory() {
        RMIServerSocketFactory rmiServerSocketFactory = new KarafRMIServerSocketFactory(getRmiServerHost());
        environment.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, rmiServerSocketFactory);
    }
    
    public String getRmiServerHost() {
        return rmiServerHost;
    }

    public void setRmiServerHost(String rmiServerHost) {
        this.rmiServerHost = rmiServerHost;
    }

    private static class KarafSslRMIServerSocketFactory implements RMIServerSocketFactory {
        private SSLServerSocketFactory sslServerSocketFactory;
        private boolean clientAuth;
        private String rmiServerHost;

        public KarafSslRMIServerSocketFactory(SSLServerSocketFactory sslServerSocketFactory, boolean clientAuth, String rmiServerHost) {
            this.sslServerSocketFactory = sslServerSocketFactory;
            this.clientAuth = clientAuth;
            this.rmiServerHost = rmiServerHost;
        }

        public ServerSocket createServerSocket(int port) throws IOException {
            SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port, 50, InetAddress.getByName(rmiServerHost));
            sslServerSocket.setNeedClientAuth(clientAuth);
            return sslServerSocket;
        }
    }
    
    private static class KarafRMIServerSocketFactory implements RMIServerSocketFactory {
        private String rmiServerHost;

        public KarafRMIServerSocketFactory(String rmiServerHost) {
            this.rmiServerHost = rmiServerHost;
        }

        public ServerSocket createServerSocket(int port) throws IOException {
            ServerSocket serverSocket = (ServerSocket) ServerSocketFactory.getDefault().createServerSocket(port, 50, InetAddress.getByName(rmiServerHost));
            return serverSocket;
        }
    }
    
    public void register(KeystoreInstance keystore, Map<String,?> properties) {
        if (this.secured) {
            try {
                this.init();
            } catch (Exception e) {
                LOGGER.error("Can't re-init JMXConnectorServer with SSL enabled when register a keystore:" + e.getMessage());
            }
        }
    }

    public void unregister(KeystoreInstance keystore, Map<String,?> properties) {
        if (this.secured) {
            try {
                this.init();
            } catch (Exception e) {
                LOGGER.error("Can't re-init JMXConnectorServer with SSL enabled when unregister a keystore: " + e.getMessage());
            }
        }
    }

}
