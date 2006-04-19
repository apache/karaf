/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.http.jetty;

import java.lang.reflect.Constructor;

import org.mortbay.http.HashUserRealm;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.util.Code;
import org.mortbay.util.InetAddrPort;
import org.osgi.framework.*;
import org.osgi.service.http.HttpService;
import org.mortbay.http.SunJsseListener;
import org.mortbay.http.JsseListener;

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
    protected static boolean debug = false;

    private BundleContext m_bundleContext = null;
    private ServiceRegistration m_svcReg = null;
    private HttpServiceFactory  m_httpServ = null;
    private HttpServer m_server = null;

    private int m_httpPort;
    private int m_httpsPort;


    public void start(BundleContext bundleContext)
        throws BundleException
    {
        m_bundleContext = bundleContext;

        // org.mortbay.util.Loader needs this (used for JDK 1.4 log classes)
        Thread.currentThread().setContextClassLoader(
                this.getClass().getClassLoader());
        
        String optDebug =
            m_bundleContext.getProperty("org.apache.felix.http.jetty.debug");
        if (optDebug != null && optDebug.toLowerCase().equals("true"))
        {
            Code.setDebug(true);
            debug = true;
        }

        // get default HTTP and HTTPS ports as per the OSGi spec
        try
        {
            m_httpPort = Integer.parseInt(m_bundleContext.getProperty(
                    "org.osgi.service.http.port"));
        }
        catch (Exception e)
        {
            // maybe log a message saying using default?
            m_httpPort = 80;
        }

        try
        {
            // TODO: work out how/when we should use the HTTPS port
            m_httpsPort = Integer.parseInt(m_bundleContext.getProperty(
                    "org.osgi.service.http.port.secure"));
        }
        catch (Exception e)
        {
            // maybe log a message saying using default?
            m_httpsPort = 443;
        }

        try
        {
            initializeJetty();

        } catch (Exception ex) {
            //TODO: maybe throw a bundle exception in here?
            System.out.println("Http2: " + ex);
            return;
        }

        m_httpServ = new HttpServiceFactory();
        m_svcReg = m_bundleContext.registerService(
            HttpService.class.getName(), m_httpServ, null);
    }

    
    public void stop(BundleContext bundleContext)
        throws BundleException
    {
        //TODO: wonder if we need to closedown service factory ???

        if (m_svcReg != null)
        {
            m_svcReg.unregister();
        }

        try
        {
            m_server.stop();
        }
        catch (Exception e)
        {
            //TODO: log some form of error
        }
    }

    protected void initializeJetty()
        throws Exception
    {
        //TODO: Maybe create a separate "JettyServer" object here?
        // Realm
        HashUserRealm realm =
            new HashUserRealm("OSGi HTTP Service Realm");

        // Create server
        m_server = new HttpServer();
        m_server.addRealm(realm);

        // Add a regular HTTP listener
        SocketListener listener = null;
        listener = (SocketListener)
            m_server.addListener(new InetAddrPort(m_httpPort));        
        listener.setMaxIdleTimeMs(60000);
        
        // See if we need to add an HTTPS listener
        String enableHTTPS = m_bundleContext.getProperty("org.ungoverned.osgi.bundle.https.enable");
        if (enableHTTPS != null && enableHTTPS.toLowerCase().equals("true"))
        {
            initializeHTTPS();
        }
        
        m_server.start();
    }

    //TODO: Just a basic implementation to give us a working HTTPS port. A better
    //      long-term solution may be to separate out the SSL provider handling, 
    //      keystore, passwords etc. into it's own pluggable service
    protected void initializeHTTPS()
        throws Exception
    {
        String sslProvider = m_bundleContext.getProperty("org.ungoverned.osgi.bundle.https.provider");
        if (sslProvider == null)
        {
            sslProvider = "org.mortbay.http.SunJsseListener";
        }

        // Set default jetty properties for supplied values. For any not set, 
        // Jetty will fallback to checking system properties.
        String keystore = m_bundleContext.getProperty("org.ungoverned.osgi.bundle.https.keystore");
        if (keystore != null)
        {
            System.setProperty(JsseListener.KEYSTORE_PROPERTY, keystore);
        }

        String passwd = m_bundleContext.getProperty("org.ungoverned.osgi.bundle.https.password");
        if (passwd != null)
        {
            System.setProperty(JsseListener.PASSWORD_PROPERTY, passwd);
        }
        
        String keyPasswd = m_bundleContext.getProperty("org.ungoverned.osgi.bundle.https.key.password");
        if (keyPasswd != null)
        {
            System.setProperty(JsseListener.KEYPASSWORD_PROPERTY, keyPasswd);
        }

        //SunJsseListener s_listener = new SunJsseListener(new InetAddrPort(m_httpsPort));
        Object args[] = { new InetAddrPort(m_httpsPort) };
        Class argTypes[] = { args[0].getClass() };
        Class clazz = Class.forName(sslProvider);
        Constructor cstruct = clazz.getDeclaredConstructor(argTypes);
        JsseListener s_listener = (JsseListener) cstruct.newInstance(args);

        m_server.addListener(s_listener);        
        s_listener.setMaxIdleTimeMs(60000);
    }

    
    protected static void debug(String txt)
    {
        if (debug)
        {
            System.err.println(">>Oscar HTTP: " + txt);
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

        public Object getService(Bundle bundle,
                ServiceRegistration registration)
        {
            Object srv = new HttpServiceImpl(bundle, m_server); 
            debug("** http service get:" + bundle + ", service: " + srv);
            return srv;
        }

        public void ungetService(Bundle bundle,
                ServiceRegistration registration, Object service)
        {
            debug("** http service unget:" + bundle + ", service: " 
            + service);
            ((HttpServiceImpl) service).unregisterAll();
        }
    }
    
}