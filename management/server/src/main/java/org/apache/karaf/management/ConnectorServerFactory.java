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

import org.apache.karaf.jaas.config.KeystoreManager;
import org.apache.karaf.management.internal.MBeanInvocationHandler;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.BindException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ServerSocketChannel;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;
import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;

public class ConnectorServerFactory {

    private enum AuthenticatorType {NONE, PASSWORD, CERTIFICATE}

    private MBeanServer server;
    private KarafMBeanServerGuard guard;
    private String serviceUrl;
    private boolean jmxmpEnabled;
    private String jmxmpServiceUrl;
    private String rmiServerHost;
    private Map<String, Object> environment;
    private Map<String, Object> jmxmpEnvironment;
    private ObjectName objectName;
    private ObjectName jmxmpObjectName;
    private boolean threaded = false;
    private boolean daemon = false;
    private JMXConnectorServer connectorServer;
    private Remote remoteServerStub;
    private RMIJRMPServerImpl rmiServer;
    private JMXConnectorServer jmxmpConnectorServer;

    private long keyStoreAvailabilityTimeout = 5000;
    private AuthenticatorType authenticatorType = AuthenticatorType.PASSWORD;
    private boolean secured;
    private KeystoreManager keystoreManager;
    private String algorithm;
    private String secureProtocol;
    private String[] enabledProtocols;
    private String[] enabledCipherSuites;
    private String keyStore;
    private String trustStore;
    private String keyAlias;

    private int port = Registry.REGISTRY_PORT;
    private String host;
    private Registry registry;
    private boolean locate;
    private boolean create = true;
    private boolean locallyCreated;
    private BundleContext bundleContext;

    public MBeanServer getServer() {
        return server;
    }

    public void setServer(MBeanServer server) {
        this.server = server;
    }

    public KarafMBeanServerGuard getGuard() {
        return guard;
    }

    public void setGuard(KarafMBeanServerGuard guard) {
        this.guard = guard;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public boolean isJmxmpEnabled() {
        return this.jmxmpEnabled;
    }

    public void setJmxmpEnabled(boolean jmxmpEnabled) {
        this.jmxmpEnabled = jmxmpEnabled;
    }

    public String getJmxmpServiceUrl() {
        return jmxmpServiceUrl;
    }

    public void setJmxmpServiceUrl(String jmxmpServiceUrl) {
        this.jmxmpServiceUrl = jmxmpServiceUrl;
    }

    public String getRmiServerHost() {
        return this.rmiServerHost;
    }

    public void setRmiServerHost(String rmiServerHost) {
        this.rmiServerHost = rmiServerHost;
    }

    public Map<String, Object> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, Object> environment) {
        this.environment = environment;
    }

    public Map<String, Object> getJmxmpEnvironment() {
        return this.jmxmpEnvironment;
    }

    public void setJmxmpEnvironment(Map<String,Object> jmxmpEnvironment) {
        this.jmxmpEnvironment = jmxmpEnvironment;
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }

    public ObjectName getJmxmpObjectName() {
        return this.jmxmpObjectName;
    }

    public void setJmxmpObjectName(ObjectName jmxmpObjectName) {
        this.jmxmpObjectName = jmxmpObjectName;
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
     * Authenticator type to use. Acceptable values are "none", "password", and "certificate"
     *
     * @param value The authenticator type: "none", "password", "certificate".
     */
    public void setAuthenticatorType(String value) {
        this.authenticatorType = AuthenticatorType.valueOf(value.toUpperCase());
    }

    /**
     * Use this param to allow KeyStoreManager to wait for expected keystores to be loaded by other bundle
     *
     * @param keyStoreAvailabilityTimeout The keystore timeout.
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
     * As different JVMs have different implementations available, the default algorithm can be used by supplying the value "Default".
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

    /**
     * @return the create
     */
    public boolean isCreate() {
        return create;
    }

    /**
     * @param create the create to set
     */
    public void setCreate(boolean create) {
        this.create = create;
    }

    /**
     * @return the locate
     */
    public boolean isLocate() {
        return locate;
    }

    /**
     * @param locate the locate to set
     */
    public void setLocate(boolean locate) {
        this.locate = locate;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() throws Exception {

        JMXServiceURL url = new JMXServiceURL(this.serviceUrl);

        if (registry == null && locate) {
            try {
                Registry reg = LocateRegistry.getRegistry(host, getPort());
                reg.list();
                registry = reg;
            } catch (RemoteException e) {
                // ignore
            }
        }
        if (registry == null && create) {
            registry = new JmxRegistry(getPort(), getBindingName(url));
            locallyCreated = true;
        }
        if (registry != null) {
            // register the registry as an OSGi service
            Hashtable<String, Object> props = new Hashtable<>();
            props.put("port", getPort());
            props.put("host", getHost());
            bundleContext.registerService(Registry.class, registry, props);
        }

        if (this.server == null) {
            throw new IllegalArgumentException("server must be set");
        }
        if ( isClientAuth() ) {
            this.secured = true;
        }

        if ( this.secured ) {
            setupSsl();
        } else {
            setupKarafRMIServerSocketFactory();
        }

        if ( ! AuthenticatorType.PASSWORD.equals( this.authenticatorType ) ) {
            this.environment.remove( "jmx.remote.authenticator" );
        }

        MBeanInvocationHandler handler = new MBeanInvocationHandler(server, guard);
        MBeanServer guardedServer = (MBeanServer) Proxy.newProxyInstance(server.getClass().getClassLoader(), new Class[]{ MBeanServer.class }, handler);
        rmiServer = new RMIJRMPServerImpl(url.getPort(), 
                                          (RMIClientSocketFactory)environment.get(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE), 
                                          (RMIServerSocketFactory)environment.get(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE), environment);

        // Create the connector server now.
        this.connectorServer = new RMIConnectorServer(url, environment, rmiServer, guardedServer);

        if (this.objectName != null) {
            this.server.registerMBean(this.connectorServer, this.objectName);
        }

        if (jmxmpEnabled) {
        	Security.addProvider(new PlainSaslServer.SaslPlainProvider());
            JMXServiceURL jmxmpUrl = new JMXServiceURL(this.jmxmpServiceUrl);
            this.jmxmpConnectorServer = JMXConnectorServerFactory.newJMXConnectorServer(jmxmpUrl, this.jmxmpEnvironment, guardedServer);
            if (this.jmxmpObjectName != null) {
                this.server.registerMBean(this.jmxmpConnectorServer, this.jmxmpObjectName);
            }
        }

        try {
            if (this.threaded) {
                Thread connectorThread = new Thread(() -> {
                    try {
                        Thread.currentThread().setContextClassLoader(ConnectorServerFactory.class.getClassLoader());
                        connectorServer.start();
                        remoteServerStub = rmiServer.toStub();
                        if (jmxmpEnabled && jmxmpConnectorServer != null) {
                            jmxmpConnectorServer.start();
                        }
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
                });
                connectorThread.setName("JMX Connector Thread [" + this.serviceUrl + "]");
                connectorThread.setDaemon(this.daemon);
                connectorThread.start();
            } else {
                this.connectorServer.start();
                remoteServerStub = rmiServer.toStub();
                if (jmxmpEnabled && jmxmpConnectorServer != null) {
                    jmxmpConnectorServer.start();
                }
            }
        } catch (Exception ex) {
            if (this.objectName != null) {
                doUnregister(this.objectName);
            }
            if (jmxmpEnabled && this.jmxmpObjectName != null) {
                doUnregister(this.jmxmpObjectName);
            }
            throw ex;
        }
    }

    protected static String getBindingName(final JMXServiceURL jmxServiceURL) {
        final String urlPath = jmxServiceURL.getURLPath();

        try {
            if (urlPath.startsWith("/jndi/")) {
                return new URI(urlPath.substring(6)).getPath()
                        .replaceAll("^/+", "").replaceAll("/+$", "");
            }
        } catch (URISyntaxException e) {
            // ignore
        }

        return "jmxrmi"; // use the default
    }

    public void destroy() throws Exception {
        try {
            if (this.connectorServer != null) {
                this.connectorServer.stop();
            }
            if (this.jmxmpEnabled && this.jmxmpConnectorServer != null) {
                this.jmxmpConnectorServer.stop();
            }

            if (registry != null && locallyCreated) {
                Registry reg = registry;
                registry = null;
                UnicastRemoteObject.unexportObject(reg, true);

                // clear TCPEndpointCache
                try {
                    Class<?> cls = getClass().getClassLoader().loadClass("sun.rmi.transport.tcp.TCPEndpoint");
                    Field localEndpointsField = cls.getDeclaredField("localEndpoints");
                    Field ssfField = cls.getDeclaredField("ssf");
                    localEndpointsField.setAccessible(true);
                    ssfField.setAccessible(true);
                    Object localEndpoints = localEndpointsField.get(null);
                    if (localEndpoints != null) {
                        Map<Object, Object> map = (Map<Object, Object>) localEndpoints;
                        for (Iterator<Object> it = map.keySet().iterator(); it.hasNext(); ) {
                            Object key = it.next();
                            Object ssf = ssfField.get(key);
                            if (ssf != null && ssf.getClass().getPackage().getName().equals("org.apache.karaf.management")) {
                                it.remove();
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } finally {
            if (this.objectName != null) {
                doUnregister(this.objectName);
            }
            if (this.jmxmpEnabled && this.jmxmpObjectName != null) {
                doUnregister(this.jmxmpObjectName);
            }
        }
    }

    protected void doUnregister(ObjectName objectName) {
        try {
            if (this.objectName != null && this.server.isRegistered(objectName)) {
                this.server.unregisterMBean(objectName);
            }
            if (this.jmxmpObjectName != null && this.server.isRegistered(jmxmpObjectName)) {
                this.server.unregisterMBean(jmxmpObjectName);
            }
        } catch (JMException ex) {
            // Ignore
        }
    }

    private void setupSsl() throws GeneralSecurityException {
        SSLServerSocketFactory sssf = keystoreManager.createSSLServerFactory(null, secureProtocol, algorithm, keyStore, keyAlias, trustStore,keyStoreAvailabilityTimeout);
        RMIServerSocketFactory rssf = new KarafSslRMIServerSocketFactory(sssf, isClientAuth(), getRmiServerHost(), getEnabledProtocols(),
                                                                         getEnabledCipherSuites());
        RMIClientSocketFactory rcsf = new SslRMIClientSocketFactory();
        environment.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, rssf);
        environment.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, rcsf);
        //@TODO secure RMI connector as well?
        //env.put("com.sun.jndi.rmi.factory.socket", rcsf);
    }

    private void setupKarafRMIServerSocketFactory() {
        RMIServerSocketFactory rssf = new KarafRMIServerSocketFactory(getRmiServerHost());
        environment.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, rssf);
    }

    private static class KarafSslRMIServerSocketFactory implements RMIServerSocketFactory {
        private SSLServerSocketFactory sssf;
        private boolean clientAuth;
        private String rmiServerHost;
        private String[] enabledProtocols;
        private String[] enabledCipherSuites;

        public KarafSslRMIServerSocketFactory(SSLServerSocketFactory sssf, boolean clientAuth, String rmiServerHost,
                                              String[] enabledProtocols,
                                              String[] enabledCipherSuites) {
            this.sssf = sssf;
            this.clientAuth = clientAuth;
            this.rmiServerHost = rmiServerHost;
            this.enabledProtocols = enabledProtocols;
            this.enabledCipherSuites = enabledCipherSuites;
        }

        public ServerSocket createServerSocket(int port) throws IOException {
            InetAddress host = InetAddress.getByName(rmiServerHost);
            if (host.isLoopbackAddress()) {
                final SSLServerSocket ss = (SSLServerSocket) sssf.createServerSocket(port, 50, host);
                ss.setNeedClientAuth(clientAuth);
                if (this.enabledProtocols != null && this.enabledProtocols.length > 0) {
                    ss.setEnabledProtocols(this.enabledProtocols);
                }
                if (this.enabledCipherSuites != null && this.enabledCipherSuites.length > 0) {
                    ss.setEnabledCipherSuites(this.enabledCipherSuites);
                }
                return new LocalOnlySSLServerSocket(ss);
            } else {
                final SSLServerSocket ss = (SSLServerSocket) sssf.createServerSocket(port, 50, InetAddress.getByName(rmiServerHost));
                ss.setNeedClientAuth(clientAuth);
                if (this.enabledProtocols != null && this.enabledProtocols.length > 0) {
                    ss.setEnabledProtocols(this.enabledProtocols);
                }
                if (this.enabledCipherSuites != null && this.enabledCipherSuites.length > 0) {
                    ss.setEnabledCipherSuites(this.enabledCipherSuites);
                }
                return ss;
            }
        }
    }

    private static class KarafRMIServerSocketFactory implements RMIServerSocketFactory {
        private String rmiServerHost;

        public KarafRMIServerSocketFactory(String rmiServerHost) {
            this.rmiServerHost = rmiServerHost;
        }

        public ServerSocket createServerSocket(int port) throws IOException {
            InetAddress host = InetAddress.getByName(rmiServerHost);
            if (host.isLoopbackAddress()) {
                final ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket(port, 50, host);
                return new LocalOnlyServerSocket(ss);
            } else {
                final ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket(port, 50, InetAddress.getByName(rmiServerHost));
                return ss;
            }
        }
    }

    private static class LocalOnlyServerSocket extends ServerSocket {

        private final ServerSocket ss;

        public LocalOnlyServerSocket(ServerSocket ss) throws IOException {
            this.ss = ss;
        }

        @Override
        public void bind(SocketAddress endpoint) throws IOException {
            ss.bind(endpoint);
        }

        @Override
        public void bind(SocketAddress endpoint, int backlog) throws IOException {
            ss.bind(endpoint, backlog);
        }

        @Override
        public InetAddress getInetAddress() {
            return ss.getInetAddress();
        }

        @Override
        public int getLocalPort() {
            return ss.getLocalPort();
        }

        @Override
        public SocketAddress getLocalSocketAddress() {
            return ss.getLocalSocketAddress();
        }

        @Override
        public Socket accept() throws IOException {
            return checkLocal(ss.accept());
        }

        @Override
        public void close() throws IOException {
            ss.close();
        }

        @Override
        public ServerSocketChannel getChannel() {
            return ss.getChannel();
        }

        @Override
        public boolean isBound() {
            return ss.isBound();
        }

        @Override
        public boolean isClosed() {
            return ss.isClosed();
        }

        @Override
        public void setSoTimeout(int timeout) throws SocketException {
            ss.setSoTimeout(timeout);
        }

        @Override
        public int getSoTimeout() throws IOException {
            return ss.getSoTimeout();
        }

        @Override
        public void setReuseAddress(boolean on) throws SocketException {
            ss.setReuseAddress(on);
        }

        @Override
        public boolean getReuseAddress() throws SocketException {
            return ss.getReuseAddress();
        }

        @Override
        public String toString() {
            return ss.toString();
        }

        @Override
        public void setReceiveBufferSize(int size) throws SocketException {
            ss.setReceiveBufferSize(size);
        }

        @Override
        public int getReceiveBufferSize() throws SocketException {
            return ss.getReceiveBufferSize();
        }

        @Override
        public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
            ss.setPerformancePreferences(connectionTime, latency, bandwidth);
        }
    }

    private static class LocalOnlySSLServerSocket extends SSLServerSocket {

        private final SSLServerSocket ss;

        public LocalOnlySSLServerSocket(SSLServerSocket ss) throws IOException {
            this.ss = ss;
        }

        @Override
        public void bind(SocketAddress endpoint) throws IOException {
            ss.bind(endpoint);
        }

        @Override
        public void bind(SocketAddress endpoint, int backlog) throws IOException {
            ss.bind(endpoint, backlog);
        }

        @Override
        public InetAddress getInetAddress() {
            return ss.getInetAddress();
        }

        @Override
        public int getLocalPort() {
            return ss.getLocalPort();
        }

        @Override
        public SocketAddress getLocalSocketAddress() {
            return ss.getLocalSocketAddress();
        }

        @Override
        public Socket accept() throws IOException {
            return checkLocal(ss.accept());
        }

        @Override
        public void close() throws IOException {
            ss.close();
        }

        @Override
        public ServerSocketChannel getChannel() {
            return ss.getChannel();
        }

        @Override
        public boolean isBound() {
            return ss.isBound();
        }

        @Override
        public boolean isClosed() {
            return ss.isClosed();
        }

        @Override
        public void setSoTimeout(int timeout) throws SocketException {
            ss.setSoTimeout(timeout);
        }

        @Override
        public int getSoTimeout() throws IOException {
            return ss.getSoTimeout();
        }

        @Override
        public void setReuseAddress(boolean on) throws SocketException {
            ss.setReuseAddress(on);
        }

        @Override
        public boolean getReuseAddress() throws SocketException {
            return ss.getReuseAddress();
        }

        @Override
        public String toString() {
            return ss.toString();
        }

        @Override
        public void setReceiveBufferSize(int size) throws SocketException {
            ss.setReceiveBufferSize(size);
        }

        @Override
        public int getReceiveBufferSize() throws SocketException {
            return ss.getReceiveBufferSize();
        }

        @Override
        public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
            ss.setPerformancePreferences(connectionTime, latency, bandwidth);
        }
        public String[] getEnabledCipherSuites() {
            return ss.getEnabledCipherSuites();
        }

        public void setEnabledCipherSuites(String[] strings) {
            ss.setEnabledCipherSuites(strings);
        }

        public String[] getSupportedCipherSuites() {
            return ss.getSupportedCipherSuites();
        }

        public String[] getSupportedProtocols() {
            return ss.getSupportedProtocols();
        }

        public String[] getEnabledProtocols() {
            return ss.getEnabledProtocols();
        }

        public void setEnabledProtocols(String[] strings) {
            ss.setEnabledProtocols(strings);
        }

        public void setNeedClientAuth(boolean b) {
            ss.setNeedClientAuth(b);
        }

        public boolean getNeedClientAuth() {
            return ss.getNeedClientAuth();
        }

        public void setWantClientAuth(boolean b) {
            ss.setWantClientAuth(b);
        }

        public boolean getWantClientAuth() {
            return ss.getWantClientAuth();
        }

        public void setUseClientMode(boolean b) {
            ss.setUseClientMode(b);
        }

        public boolean getUseClientMode() {
            return ss.getUseClientMode();
        }

        public void setEnableSessionCreation(boolean b) {
            ss.setEnableSessionCreation(b);
        }

        public boolean getEnableSessionCreation() {
            return ss.getEnableSessionCreation();
        }

        public SSLParameters getSSLParameters() {
            return ss.getSSLParameters();
        }

        public void setSSLParameters(SSLParameters sslParameters) {
            ss.setSSLParameters(sslParameters);
        }
    }

    private static Socket checkLocal(Socket socket) throws IOException {
        InetAddress addr = socket.getInetAddress();
        if (addr != null) {
            if (addr.isLoopbackAddress()) {
                return socket;
            } else {
                try {
                    Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                    while (nis.hasMoreElements()) {
                        NetworkInterface ni = nis.nextElement();
                        Enumeration<InetAddress> ads = ni.getInetAddresses();
                        while (ads.hasMoreElements()) {
                            InetAddress ad = ads.nextElement();
                            if (ad.equals(addr)) {
                                return socket;
                            }
                        }
                    }
                } catch (SocketException e) {
                    // Ignore
                }
            }
        }
        try {
            socket.close();
        } catch (Exception e) {
            // Ignore
        }
        throw new IOException("Only connections from clients running on the host where the RMI remote objects have been exported are accepted.");
    }

    public String[] getEnabledProtocols() {
        return enabledProtocols;
    }

    public void setEnabledProtocols(String[] enabledProtocols) {
        this.enabledProtocols = enabledProtocols;
    }

    public String[] getEnabledCipherSuites() {
        return enabledCipherSuites;
    }

    public void setEnabledCipherSuites(String[] enabledCipherSuites) {
        this.enabledCipherSuites = enabledCipherSuites;
    }

    /*
     * Better to use the internal API than re-invent the wheel.
     */
    @SuppressWarnings("restriction")
    private class JmxRegistry extends sun.rmi.registry.RegistryImpl {
        private final String lookupName;

        JmxRegistry(final int port, final String lookupName) throws RemoteException {
            super(port, null, new KarafRMIServerSocketFactory(getHost()));
            this.lookupName = lookupName;
        }

        @Override
        public Remote lookup(String s) throws RemoteException, NotBoundException {
            return lookupName.equals(s) ? remoteServerStub : null;
        }

        @Override
        public void bind(String s, Remote remote) throws RemoteException, AlreadyBoundException, AccessException {
        }

        @Override
        public void unbind(String s) throws RemoteException, NotBoundException, AccessException {
        }

        @Override
        public void rebind(String s, Remote remote) throws RemoteException, AccessException {
        }

        @Override
        public String[] list() throws RemoteException {
            return new String[] {lookupName};
        }
    }

}
