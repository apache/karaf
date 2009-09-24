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
package org.apache.felix.http.jetty.internal;

import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SslSelectChannelConnector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.servlet.*;
import org.mortbay.log.Log;
import org.apache.felix.http.base.internal.DispatcherServlet;
import org.apache.felix.http.base.internal.logger.SystemLogger;

import java.util.Properties;
import java.util.Dictionary;

public final class JettyService
    implements ManagedService, Runnable
{
    /** PID for configuration of the HTTP service. */
    private static final String PID = "org.apache.felix.http";

    private final JettyConfig config;
    private final BundleContext context;
    private boolean running;
    private Thread thread;
    private ServiceRegistration configServiceReg;
    private Server server;
    private DispatcherServlet dispatcher;

    public JettyService(BundleContext context, DispatcherServlet dispatcher)
    {
        this.context = context;
        this.config = new JettyConfig(this.context);
        this.dispatcher = dispatcher;
    }
    
    public void start()
        throws Exception
    {
        JettyLogger.init();

        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, PID);
        this.configServiceReg = this.context.registerService(ManagedService.class.getName(), this, props);

        this.thread = new Thread(this, "Jetty HTTP Service");
        this.thread.start();
    }

    public void stop()
        throws Exception
    {
        if (this.configServiceReg != null) {
            this.configServiceReg.unregister();
        }

        this.running = false;
        this.thread.interrupt();

        try {
            this.thread.join(3000);
        } catch (InterruptedException e) {
            // Do nothing
        }
    }

    public void updated(Dictionary props)
        throws ConfigurationException
    {
        this.config.update(props);
        if (this.running && (this.thread != null)) {
            this.thread.interrupt();
        }
    }

    private void startJetty()
    {
        try {
            initializeJetty();
        } catch (Exception e) {
            SystemLogger.error("Exception while initializing Jetty.", e);
        }
    }

    private void stopJetty()
    {
        try {
            this.server.stop();
        } catch (Exception e) {
            SystemLogger.error("Exception while stopping Jetty.", e);
        }
    }

    private void initializeJetty()
        throws Exception
    {
        HashUserRealm realm = new HashUserRealm("OSGi HTTP Service Realm");
        this.server = new Server();
        this.server.addUserRealm(realm);

        if (this.config.isUseHttp()) {
            initializeHttp();
        }

        if (this.config.isUseHttps()) {
            initializeHttps();
        }

        Context context = new Context(this.server, "/", Context.SESSIONS);
        context.addServlet(new ServletHolder(this.dispatcher), "/*");

        this.server.start();
        SystemLogger.info("Started jetty " + Server.getVersion() + " at port " + this.config.getHttpPort());
    }

    private void initializeHttp()
        throws Exception
    {
        Connector connector = new SelectChannelConnector();
        connector.setPort(this.config.getHttpPort());
        connector.setMaxIdleTime(60000);
        this.server.addConnector(connector);
    }

    private void initializeHttps()
        throws Exception
    {
        SslSelectChannelConnector connector = new SslSelectChannelConnector();
        connector.setPort(this.config.getHttpsPort());
        connector.setMaxIdleTime(60000);
        
        if (this.config.getKeystore() != null) {
            connector.setKeystore(this.config.getKeystore());
        }
        
        if (this.config.getPassword() != null) {
            System.setProperty(SslSelectChannelConnector.PASSWORD_PROPERTY, this.config.getPassword());
            connector.setPassword(this.config.getPassword());
        }
        
        if (this.config.getKeyPassword() != null) {
            System.setProperty(SslSelectChannelConnector.KEYPASSWORD_PROPERTY, this.config.getKeyPassword());
            connector.setKeyPassword(this.config.getKeyPassword());
        }
        
        if (this.config.getTruststore() != null) {
            connector.setTruststore(this.config.getTruststore());
        }
        
        if (this.config.getTrustPassword() != null) {
            connector.setTrustPassword(this.config.getTrustPassword());
        }
        
        if ("wants".equals(this.config.getClientcert())) {
            connector.setWantClientAuth(true);
        } else if ("needs".equals(this.config.getClientcert())) {
            connector.setNeedClientAuth(true);
        }

        this.server.addConnector(connector);
    }

    public void run()
    {
        this.running = true;
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        while (this.running) {
            startJetty();

            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // we will definitely be interrupted
                }
            }

            stopJetty();
        }
    }
}
