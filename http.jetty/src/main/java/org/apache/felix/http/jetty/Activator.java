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

import java.util.Properties;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.OsgiServletHandler;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.component.LifeCycle;

import org.mortbay.log.Log;
import org.mortbay.log.Logger;
import org.mortbay.log.StdErrLog;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
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
public class Activator implements BundleActivator
{
    /** Standard OSGi port property for HTTP service */
    public static final String  HTTP_PORT    = "org.osgi.service.http.port";
    
    /** Standard OSGi https port property for HTTP service */
    public static final String  HTTPS_PORT   = "org.osgi.service.http.port.secure";
    
    /** Felix and Jetty specific property. Enables Jetty debug messages */
    public static final String  HTTP_DEBUG   = "org.apache.felix.http.jetty.debug";

    /** Felix specific property. This property will be looked up to determine the
        name of the service property to set with the http port used. If not supplied
        then the HTTP_PORT property name will be used for the service property */
    public static final String  HTTP_SVCPROP_PORT    = "org.apache.felix.http.svcprop.port";
    
    /** Felix specific property. This property will be looked up to determine the
        name of the service property to set with the https port used. If not supplied
        then the HTTPS_PORT property name will be used for the service property */
    public static final String  HTTPS_SVCPROP_PORT   = "org.apache.felix.http.svcprop.port.secure";
    
    /** Legacy Oscar property support. Controls whether to enable HTTPS */
    public static final String  OSCAR_HTTPS_ENABLE   = "org.ungoverned.osgi.bundle.https.enable";
    
    protected static boolean debug = false;
    private static ServiceTracker m_logTracker = null;

    private BundleContext m_bundleContext = null;
    private ServiceRegistration m_svcReg = null;
    private HttpServiceFactory m_httpServ = null;
    private Server m_server = null;
    private OsgiServletHandler m_hdlr = null;

    private int m_httpPort;
    private int m_httpsPort;

    private Properties m_svcProperties = new Properties();
    
    //
    // Main class instance code
    //

    public void start( BundleContext bundleContext ) throws BundleException
    {
        m_bundleContext = bundleContext;

        // org.mortbay.util.Loader needs this (used for JDK 1.4 log classes)
        Thread.currentThread().setContextClassLoader( this.getClass().getClassLoader() );

        debug = getBooleanProperty(HTTP_DEBUG, false);
        
        // get default HTTP and HTTPS ports as per the OSGi spec
        m_httpPort = getIntProperty(HTTP_PORT, 80);
        m_httpsPort = getIntProperty(HTTPS_PORT, 443);

        m_logTracker = new ServiceTracker( bundleContext, LogService.class.getName(), null );
        m_logTracker.open();

        // set the Jetty logger to be LogService based
        initializeJettyLogger();

        try
        {
            initializeJetty();

        }
        catch ( Exception ex )
        {
            //TODO: maybe throw a bundle exception in here?
            log( LogService.LOG_INFO, "Http2", ex );
            return;
        }

        m_httpServ = new HttpServiceFactory();
        m_svcReg = m_bundleContext.registerService( HttpService.class.getName(), m_httpServ, m_svcProperties );
        // OSGi spec states the properties should not be changed after registration,
        // so create new copy for later  clone for updates
        m_svcProperties = new Properties(m_svcProperties);
    }


    public void stop( BundleContext bundleContext ) throws BundleException
    {
        //TODO: wonder if we need to closedown service factory ???

        if ( m_svcReg != null )
        {
            m_svcReg.unregister();
        }

        try
        {
            m_server.stop();
        }
        catch ( Exception e )
        {
            //TODO: log some form of error
        }

        // replace non-LogService logger for jetty
        Log.setLog( new StdErrLog() );
        
        m_logTracker.close();
    }

    
    public int getIntProperty(String name, int dflt_val)
    {
        int retval = dflt_val;
        
        try
        {
            retval = Integer.parseInt( m_bundleContext.getProperty( name ) );
        }
        catch ( Exception e )
        {
            // maybe log a message saying using default?
            retval = dflt_val;
        }
        
        return retval;
    }
    
    
    public boolean getBooleanProperty(String name, boolean dflt_val)
    {
        boolean retval = dflt_val;
        
        String strval = m_bundleContext.getProperty( name );
        if ( strval != null)
        {
            if (strval.toLowerCase().equals( "true" ) ||
                strval.toLowerCase().equals( "yes" ))
            {
                retval = true;
            }
            else
            {
                // poss should raise error/warn here
                retval = false;
            }
        }
        
        return retval;
    }
    
    
    public String getStringProperty(String name, String dflt_val)
    {
        String retval = dflt_val;
        
        String strval = m_bundleContext.getProperty( name );
        if ( strval != null)
        {
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
    
    protected void initializeJetty() throws Exception
    {
        //TODO: Maybe create a separate "JettyServer" object here?
        // Realm
        HashUserRealm realm = new HashUserRealm( "OSGi HTTP Service Realm" );

        // Create server
        m_server = new Server();
        m_server.addUserRealm( realm );

        // Add a regular HTTP listener
        Connector connector = new SelectChannelConnector();
        connector.addLifeCycleListener(
                new ConnectorListener(getStringProperty(HTTP_SVCPROP_PORT, HTTP_PORT))
            );
        
        connector.setPort( m_httpPort );
        connector.setMaxIdleTime( 60000 );
        m_server.addConnector( connector );

        // See if we need to add an HTTPS listener
        if ( getBooleanProperty( OSCAR_HTTPS_ENABLE, false ))
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


    //TODO: Just a basic implementation to give us a working HTTPS port. A better
    //      long-term solution may be to separate out the SSL provider handling,
    //      keystore, passwords etc. into it's own pluggable service
    protected void initializeHTTPS() throws Exception
    {
        String sslProvider = m_bundleContext.getProperty( "org.ungoverned.osgi.bundle.https.provider" );
        if ( sslProvider == null )
        {
            sslProvider = "org.mortbay.http.SunJsseListener";
        }

        SslSocketConnector s_listener = new SslSocketConnector();
        s_listener.addLifeCycleListener(
                new ConnectorListener(getStringProperty(HTTPS_SVCPROP_PORT, HTTPS_PORT))
            );
                
        s_listener.setPort( m_httpsPort );
        s_listener.setMaxIdleTime( 60000 );

        // Set default jetty properties for supplied values. For any not set,
        // Jetty will fallback to checking system properties.
        String keystore = m_bundleContext.getProperty( "org.ungoverned.osgi.bundle.https.keystore" );
        if ( keystore != null )
        {
            s_listener.setKeystore( keystore );
        }

        String passwd = m_bundleContext.getProperty( "org.ungoverned.osgi.bundle.https.password" );
        if ( passwd != null )
        {
            System.setProperty( SslSocketConnector.PASSWORD_PROPERTY, passwd );
            s_listener.setPassword( passwd );
        }

        String keyPasswd = m_bundleContext.getProperty( "org.ungoverned.osgi.bundle.https.key.password" );
        if ( keyPasswd != null )
        {
            System.setProperty( SslSocketConnector.KEYPASSWORD_PROPERTY, keyPasswd );
            s_listener.setKeyPassword( keyPasswd );
        }

        m_server.addConnector( s_listener );
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