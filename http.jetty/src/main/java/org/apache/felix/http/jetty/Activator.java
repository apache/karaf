/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.http.jetty;

import java.util.Dictionary;
import java.util.Properties;

import org.mortbay.component.LifeCycle;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.OsgiServletHandler;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.log.Log;
import org.mortbay.log.StdErrLog;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 *  Basic implementation of OSGi HTTP service 1.1.
 *
 *  TODO:
 *
 *      - fuller suite of testing and compatibility tests
 *
 *      - only exposed params are those defined in the OSGi spec. Jetty is
 *        very tunable via params, some of which it may be useful to expose
 *
 *      - no cacheing is performed on delivered resources. Although not part
 *        of the OSGi spec, it also isn't precluded and would enhance
 *        performance in a high usage environment. Jetty's ResourceHandler
 *        class could be a model for this.
 *
 *      - scanning the Jetty ResourceHandler class it's clear that there are
 *        many other sophisticated areas to do with resource handling such
 *        as checking date and range fields in the http headers. It's not clear
 *        whether any of these play a part in the OSGi service - the spec
 *        just describes "returning the contents of the URL to the client" which
 *        doesn't state what other HTTP handling might be compliant or desirable
 */
public class Activator implements BundleActivator, ManagedService, Runnable {
    private static final Properties EMPTY_PROPS = new Properties();
    
    public static final boolean DEFAULT_HTTP_ENABLE = true;
    public static final boolean DEFAULT_HTTPS_ENABLE = false;
    public static final boolean DEFAULT_USE_NIO = true;
    public static final int DEFAULT_HTTPS_PORT = 443;
    public static final int DEFAULT_HTTP_PORT = 80;
    public static final String DEFAULT_SSL_PROVIDER = "org.mortbay.http.SunJsseListener";
    public static final String DEFAULT_HTTPS_CLIENT_CERT = "none";
    
    /** Felix specific property to override the SSL provider. */
    public static final String FELIX_SSL_PROVIDER = "org.apache.felix.https.provider";
    public static final String OSCAR_SSL_PROVIDER = "org.ungoverned.osgi.bundle.https.provider";
    
    /** Felix specific property to override the keystore key password. */
    public static final String FELIX_KEYSTORE_KEY_PASSWORD = "org.apache.felix.https.keystore.key.password";
    public static final String OSCAR_KEYSTORE_KEY_PASSWORD = "org.ungoverned.osgi.bundle.https.key.password";
    
    /** Felix specific property to override the keystore file location. */
    public static final String FELIX_KEYSTORE = "org.apache.felix.https.keystore";
    public static final String OSCAR_KEYSTORE = "org.ungoverned.osgi.bundle.https.keystore";
    
    /** Felix specific property to override the keystore password. */
    public static final String FELIX_KEYSTORE_PASSWORD = "org.apache.felix.https.keystore.password";
    public static final String OSCAR_KEYSTORE_PASSWORD = "org.ungoverned.osgi.bundle.https.password";

    /** Standard OSGi port property for HTTP service */
    public static final String  HTTP_PORT    = "org.osgi.service.http.port";
    
    /** Standard OSGi port property for HTTPS service */
    public static final String  HTTPS_PORT   = "org.osgi.service.http.port.secure";
    
    /** Felix specific property to enable debug messages */
    public static final String FELIX_HTTP_DEBUG = "org.apache.felix.http.debug";
    public static final String  HTTP_DEBUG   = "org.apache.felix.http.jetty.debug";

    /** Felix specific property to determine the
        name of the service property to set with the http port used. If not supplied
        then the HTTP_PORT property name will be used for the service property */
    public static final String  HTTP_SVCPROP_PORT    = "org.apache.felix.http.svcprop.port";
    
    /** Felix specific property to determine the
        name of the service property to set with the https port used. If not supplied
        then the HTTPS_PORT property name will be used for the service property */
    public static final String  HTTPS_SVCPROP_PORT   = "org.apache.felix.http.svcprop.port.secure";

    /** Felix specific property to control whether NIO will be used. If not supplied
        then will default to true. */
    public static final String  HTTP_NIO             = "org.apache.felix.http.nio";
    
    /** Felix specific property to control whether to enable HTTPS. */
    public static final String FELIX_HTTPS_ENABLE = "org.apache.felix.https.enable";
    public static final String  OSCAR_HTTPS_ENABLE   = "org.ungoverned.osgi.bundle.https.enable";
    
    /** Felix specific property to control whether to enable HTTP. */
    public static final String FELIX_HTTP_ENABLE = "org.apache.felix.http.enable";
    
    /** Felix specific property to control whether to want or require HTTPS client certificates. Valid values are "none", "wants", "needs". Default is "none". */
    public static final String FELIX_HTTPS_CLIENT_CERT = "org.apache.felix.https.clientcertificate";

    /** Felix specific property to override the truststore file location. */
    public static final String FELIX_TRUSTSTORE = "org.apache.felix.https.truststore";
    
    /** Felix specific property to override the truststore password. */
    public static final String FELIX_TRUSTSTORE_PASSWORD = "org.apache.felix.https.truststore.password";
    
    /** PID for configuration of the HTTP service. */
    protected static final String PID = "org.apache.felix.http";
    
    protected static boolean debug = false;
    private static ServiceTracker m_logTracker = null;

    private BundleContext m_bundleContext = null;
    private ServiceRegistration m_svcReg = null;
    private HttpServiceFactory m_httpServ = null;
    private Server m_server = null;
    private OsgiServletHandler m_hdlr = null;

    private int m_httpPort;
    private int m_httpsPort;
    private boolean m_useNIO;
    private String m_sslProvider;
    private String m_httpsPortProperty;
    private String m_keystore;
    private String m_passwd;
    private String m_keyPasswd;
    private boolean m_useHttps;
    private String m_httpPortProperty;
    private String m_truststore;
    private String m_trustpasswd;

    private Properties m_svcProperties = new Properties();
    private boolean m_useHttp;
    private String m_clientcert;
    private ServiceRegistration m_configSvcReg;
    private volatile boolean m_running;
    private volatile Thread m_thread;

    public void start(BundleContext bundleContext) throws BundleException {
        m_bundleContext = bundleContext;

        m_logTracker = new ServiceTracker( bundleContext, LogService.class.getName(), null );
        m_logTracker.open();

        setConfiguration(EMPTY_PROPS);
        
        m_running = true;
        m_thread = new Thread(this, "Jetty HTTP Service Launcher");
        m_thread.start();
        
        m_configSvcReg = m_bundleContext.registerService(ManagedService.class.getName(), this, new Properties() {{ put(Constants.SERVICE_PID, PID); }} );
    }
    
    public void stop(BundleContext bundleContext) throws BundleException {
        if (m_configSvcReg != null) {
            m_configSvcReg.unregister();
        }
        
        m_running = false;
        m_thread.interrupt();
        try {
            m_thread.join(3000);
        }
        catch (InterruptedException e) {
            // not much we can do here
        }
        
        m_logTracker.close();
    }
    
    private void startJetty() {
        try {
            initializeJetty();
        }
        catch (Exception ex) {
            log(LogService.LOG_ERROR, "Exception while initializing Jetty.", ex);
            return;
        }

        m_httpServ = new HttpServiceFactory();
        m_svcReg = m_bundleContext.registerService( HttpService.class.getName(), m_httpServ, m_svcProperties );
        // OSGi spec states the properties should not be changed after registration,
        // so create new copy for later  clone for updates
        m_svcProperties = new Properties(m_svcProperties);
    }

    private void stopJetty() {
        if (m_svcReg != null) {
            m_svcReg.unregister();
            // null the registration, because the listener assumes a non-null registration is valid
            m_svcReg = null;
        }

        try {
            m_server.stop();
        }
        catch (Exception e) {
            log(LogService.LOG_ERROR, "Exception while stopping Jetty.", e);
        }
    }

    /**
     * The main loop for running Jetty. We run Jetty in its own thread now because we need to
     * modify this thread's context classloader. The main loop starts Jetty and then waits until
     * it is interrupted. Then it stops Jetty, and either quits or restarts (depending on the
     * reason why the thread was interrupted, because the bundle was stopped or the configuration
     * was updated).
     */
    public void run() {
        // org.mortbay.util.Loader needs this (used for JDK 1.4 log classes)
        Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );
        
        while (m_running) {
            // start jetty
            initializeJettyLogger();
            startJetty();
            
            // wait
            synchronized (this) {
                try {
                    wait();
                }
                catch (InterruptedException e) {
                    // we will definitely be interrupted
                }
            }
            
            // stop jetty
            stopJetty();
            destroyJettyLogger();
        }
    }
    
    
    public void updated(Dictionary props) throws ConfigurationException {
        if (props == null) {
            // fall back to default configuration
            setConfiguration(EMPTY_PROPS);
        }
        else {
            // let's see what we've got
            setConfiguration(props);
        }
        // notify the thread that the configuration was updated, causing Jetty to
        // restart
        if (m_thread != null) {
            m_thread.interrupt();
        }
    }
    
    private void setConfiguration(Dictionary props) {
        debug = getBooleanProperty(props, FELIX_HTTP_DEBUG, getBooleanProperty(props, HTTP_DEBUG, false));
        
        // get default HTTP and HTTPS ports as per the OSGi spec
        m_httpPort = getIntProperty(props, HTTP_PORT, DEFAULT_HTTP_PORT);
        m_httpsPort = getIntProperty(props, HTTPS_PORT, DEFAULT_HTTPS_PORT);
        // collect other properties, default to legacy names only if new ones are not available
        m_useNIO = getBooleanProperty(props, HTTP_NIO, DEFAULT_USE_NIO);
        m_sslProvider = getStringProperty(props, FELIX_SSL_PROVIDER, getStringProperty(props, OSCAR_SSL_PROVIDER, DEFAULT_SSL_PROVIDER));
        m_httpsPortProperty = getStringProperty(props, HTTPS_SVCPROP_PORT, HTTPS_PORT);
        m_keystore = getStringProperty(props, FELIX_KEYSTORE, m_bundleContext.getProperty(OSCAR_KEYSTORE));
        m_passwd = getStringProperty(props, FELIX_KEYSTORE_PASSWORD, m_bundleContext.getProperty(OSCAR_KEYSTORE_PASSWORD));
        m_keyPasswd = getStringProperty(props, FELIX_KEYSTORE_KEY_PASSWORD, m_bundleContext.getProperty(OSCAR_KEYSTORE_KEY_PASSWORD));
        m_useHttps = getBooleanProperty(props, FELIX_HTTPS_ENABLE, getBooleanProperty(props, OSCAR_HTTPS_ENABLE, DEFAULT_HTTPS_ENABLE));
        m_httpPortProperty = getStringProperty(props, HTTP_SVCPROP_PORT, HTTP_PORT);
        m_useHttp = getBooleanProperty(props, FELIX_HTTP_ENABLE, DEFAULT_HTTP_ENABLE);
        m_truststore = getStringProperty(props, FELIX_TRUSTSTORE, null);
        m_trustpasswd = getStringProperty(props, FELIX_TRUSTSTORE_PASSWORD, null);
        m_clientcert = getStringProperty(props, FELIX_HTTPS_CLIENT_CERT, DEFAULT_HTTPS_CLIENT_CERT);
    }

    private String getProperty(Dictionary props, String name) {
        String result = (String) props.get(name);
        if (result == null) {
            result = m_bundleContext.getProperty(name);
        }
        return result;
    }
    
    private int getIntProperty(Dictionary props, String name, int dflt_val) {
        int retval = dflt_val;
        try {
            retval = Integer.parseInt(getProperty(props, name));
        }
        catch (Exception e) {
            retval = dflt_val;
        }
        return retval;
    }
        
    private boolean getBooleanProperty(Dictionary props, String name, boolean dflt_val) {
        boolean retval = dflt_val;
        String strval = getProperty(props, name);
        if (strval != null) {
            retval = (strval.toLowerCase().equals("true") || strval.toLowerCase().equals("yes"));
        }
        return retval;
    }
   
    private String getStringProperty(Dictionary props, String name, String dflt_val) {
        String retval = dflt_val;
        String strval = getProperty(props, name);
        if (strval != null) {
            retval = strval;
        }
        return retval;
    }

    protected void initializeJettyLogger() {
        String oldProperty = System.getProperty( "org.mortbay.log.class" );
        System.setProperty( "org.mortbay.log.class", LogServiceLog.class.getName() );
        
        if (!(Log.getLog() instanceof LogServiceLog)) {
            Log.setLog( new LogServiceLog() );
        }
        
        Log.getLog().setDebugEnabled( debug );
        
        if (oldProperty != null) {
            System.setProperty( "org.mortbay.log.class", oldProperty );
        }
    }
    
    private void destroyJettyLogger() {
        // replace non-LogService logger for jetty
        Log.setLog( new StdErrLog() );
    }
    
    protected void initializeJetty() throws Exception
    {
        //TODO: Maybe create a separate "JettyServer" object here?
        // Realm
        HashUserRealm realm = new HashUserRealm( "OSGi HTTP Service Realm" );

        // Create server
        m_server = new Server();
        m_server.addUserRealm( realm );

        if (m_useHttp)
        {
            initializeHTTP();
        }

        if (m_useHttps)
        {
            initializeHTTPS();
        }

        // setup the Jetty web application context shared by all Http services
        m_hdlr = new OsgiServletHandler();

        Context hdlrContext = new Context( m_server, new SessionHandler(), null, m_hdlr, null );
        hdlrContext.setClassLoader( getClass().getClassLoader() );
        hdlrContext.setContextPath( "/" );
        debug( " adding handler context : " + hdlrContext );

        try
        {
            hdlrContext.start();
        }
        catch ( Exception e )
        {
            // make sure we unwind the adding process
            log( LogService.LOG_ERROR, "Exception Starting Jetty Handler Context", e );
        }

        m_server.start();
    }

    private void initializeHTTP() {
        Connector connector = m_useNIO ? 
                              (Connector) new SelectChannelConnector() : (Connector) new SocketConnector();
        connector.addLifeCycleListener(
                new ConnectorListener(m_httpPortProperty)
            );
        
        connector.setPort( m_httpPort );
        connector.setMaxIdleTime( 60000 );
        m_server.addConnector( connector );
    }

    //TODO: Just a basic implementation to give us a working HTTPS port. A better
    //      long-term solution may be to separate out the SSL provider handling,
    //      keystore, passwords etc. into it's own pluggable service
    protected void initializeHTTPS() throws Exception
    {
        if (m_useNIO) {
            SelectChannelConnector s_listener = (SelectChannelConnector) Class.forName("org.mortbay.jetty.security.SslSelectChannelConnector").newInstance();
            s_listener.addLifeCycleListener(new ConnectorListener(m_httpsPortProperty));
            s_listener.setPort(m_httpsPort);
            s_listener.setMaxIdleTime(60000);
            if (m_keystore != null) {
                s_listener.getClass().getMethod("setKeystore", new Class[] {String.class}).invoke(s_listener, new Object[] { m_keystore });
            }
            if (m_passwd != null) {
                System.setProperty("jetty.ssl.password" /* SslSelectChannelConnector.PASSWORD_PROPERTY */ , m_passwd);
                s_listener.getClass().getMethod("setPassword", new Class[] {String.class}).invoke(s_listener, new Object[] { m_passwd });
            }
            if (m_keyPasswd != null) {
                System.setProperty("jetty.ssl.keypassword" /* SslSelectChannelConnector.KEYPASSWORD_PROPERTY */, m_keyPasswd);
                s_listener.getClass().getMethod("setKeyPassword", new Class[] {String.class}).invoke(s_listener, new Object[] { m_keyPasswd });
            }
            if (m_truststore != null) {
                s_listener.getClass().getMethod("setTruststore", new Class[] {String.class}).invoke(s_listener, new Object[] { m_truststore });
            }
            if (m_trustpasswd != null) {
                s_listener.getClass().getMethod("setTrustPassword", new Class[] {String.class}).invoke(s_listener, new Object[] { m_trustpasswd });
            }
            if ("wants".equals(m_clientcert)) {
                s_listener.getClass().getMethod("setWantClientAuth", new Class[] { Boolean.TYPE }).invoke(s_listener, new Object[] { Boolean.TRUE });
            }
            else if ("needs".equals(m_clientcert)) {
                s_listener.getClass().getMethod("setNeedClientAuth", new Class[] { Boolean.TYPE }).invoke(s_listener, new Object[] { Boolean.TRUE });
            }
            m_server.addConnector(s_listener);
        }
        else {
            SslSocketConnector s_listener = new SslSocketConnector();
            s_listener.addLifeCycleListener(new ConnectorListener(m_httpsPortProperty));
            s_listener.setPort(m_httpsPort);
            s_listener.setMaxIdleTime(60000);
            if (m_keystore != null) {
                s_listener.setKeystore(m_keystore);
            }
            if (m_passwd != null) {
                System.setProperty(SslSocketConnector.PASSWORD_PROPERTY, m_passwd);
                s_listener.setPassword(m_passwd);
            }
            if (m_keyPasswd != null) {
                System.setProperty(SslSocketConnector.KEYPASSWORD_PROPERTY, m_keyPasswd);
                s_listener.setKeyPassword(m_keyPasswd);
            }
            if (m_truststore != null) {
                s_listener.setTruststore(m_truststore);
            }
            if (m_trustpasswd != null) {
                s_listener.setTrustPassword(m_trustpasswd);
            }
            if ("wants".equals(m_clientcert)) {
                s_listener.setWantClientAuth(true);
                s_listener.getClass().getMethod("setWantClientAuth", new Class[] {Boolean.class}).invoke(s_listener, new Object[] { Boolean.TRUE });
            }
            else if ("needs".equals(m_clientcert)) {
                s_listener.setNeedClientAuth(true);
            }
            m_server.addConnector(s_listener);
        }
    }

    public static void debug( String txt )
    {
        if ( debug )
        {
            log( LogService.LOG_DEBUG, ">>Felix HTTP: " + txt, null );
        }
    }

    public static void log( int level, String message, Throwable throwable )
    {
        LogService log = ( LogService ) m_logTracker.getService();
        if ( log != null )
        {
            log.log( level, message, throwable );
        }
        else
        {
            System.out.println( message );
            if ( throwable != null )
            {
                throwable.printStackTrace( System.out );
            }
        }
    }

    // Inner class to provide basic service factory functionality
    public class HttpServiceFactory implements ServiceFactory
    {
        public HttpServiceFactory()
        {
            // Initialize the statics for the service implementation.
            HttpServiceImpl.initializeStatics();
        }

        public Object getService( Bundle bundle, ServiceRegistration registration )
        {
            Object srv = new HttpServiceImpl( bundle, m_server, m_hdlr );
            debug( "** http service get:" + bundle + ", service: " + srv );
            return srv;
        }

        public void ungetService( Bundle bundle, ServiceRegistration registration, Object service )
        {
            debug( "** http service unget:" + bundle + ", service: " + service );
            ( ( HttpServiceImpl ) service ).unregisterAll();
        }
    }
    
    // Innner class to listen for connector startup and register service
    // properties for actual ports used. Possible connections may have deferred
    // startup, so this should ensure "port" is retrieved once available
    public class ConnectorListener implements LifeCycle.Listener
    {
        String m_svcPropName;
        
        public ConnectorListener(String svcPropName)
        {
            m_svcPropName = svcPropName;
        }
        
        public void lifeCycleFailure(LifeCycle event, Throwable cause) {}
           
        public void lifeCycleStarted(LifeCycle event)
        {
            Connector conn = (Connector) event;
            int actualPort = conn.getLocalPort();
            
            debug( "** http set service prop:" + m_svcPropName + ", value: " + actualPort );
            
            m_svcProperties.setProperty(m_svcPropName, String.valueOf(actualPort));
            
            if (m_svcReg != null)
            {
                m_svcReg.setProperties(m_svcProperties);
                m_svcProperties = new Properties(m_svcProperties);
            }
        }
        
        public void lifeCycleStarting(LifeCycle event) {}
           
        public void lifeCycleStopped(LifeCycle event) {}
           
        public void lifeCycleStopping(LifeCycle event) {}         
    }
}