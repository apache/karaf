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
package org.apache.karaf.services.web.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The embedded Tomcat server: one service, one engine and one host, with an HTTP and/or an HTTPS connector.
 * <p>
 * The server is built directly from the Tomcat components rather than with
 * {@link org.apache.catalina.startup.Tomcat}, which changes system properties and the global configuration source.
 */
public class TomcatServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatServer.class);

    private final WebConfig config;
    private final StandardServer server;
    private final StandardService service;
    private final StandardHost host;

    public TomcatServer(WebConfig config, File baseDir) {
        this.config = config;
        if (!baseDir.isDirectory() && !baseDir.mkdirs()) {
            throw new IllegalStateException("Unable to create the Tomcat base directory " + baseDir);
        }
        ClassLoader classLoader = TomcatServer.class.getClassLoader();

        server = new StandardServer();
        server.setPort(-1);
        server.setCatalinaBase(baseDir);
        server.setCatalinaHome(baseDir);
        server.setParentClassLoader(classLoader);

        service = new StandardService();
        service.setName("Tomcat");
        server.addService(service);

        StandardEngine engine = new StandardEngine();
        engine.setName("Tomcat");
        engine.setDefaultHost("localhost");
        service.setContainer(engine);

        host = new StandardHost();
        host.setName("localhost");
        host.setAutoDeploy(false);
        host.setDeployOnStartup(false);
        host.setParentClassLoader(classLoader);
        // Same valve as the one the host would add, but the error pages only show the status: neither the Tomcat
        // version, nor the message and the stack trace of the exceptions
        ErrorReportValve errorReportValve = new ErrorReportValve();
        errorReportValve.setShowServerInfo(false);
        errorReportValve.setShowReport(false);
        host.getPipeline().addValve(errorReportValve);
        engine.addChild(host);

        if (config.httpEnabled) {
            service.addConnector(createConnector(config.httpPort));
        }
        if (config.httpsEnabled) {
            service.addConnector(createHttpsConnector());
        }
    }

    private Connector createConnector(int port) {
        Connector connector = new Connector("HTTP/1.1");
        connector.setPort(port);
        connector.setProperty("address", config.host);
        connector.setProperty("maxThreads", Integer.toString(config.maxThreads));
        connector.setThrowOnFailure(true);
        return connector;
    }

    private Connector createHttpsConnector() {
        Connector connector = createConnector(config.httpsPort);
        connector.setScheme("https");
        connector.setSecure(true);
        connector.setProperty("SSLEnabled", "true");
        SSLHostConfig sslHostConfig = new SSLHostConfig();
        SSLHostConfigCertificate certificate =
                new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.UNDEFINED);
        certificate.setCertificateKeystoreFile(config.getKeystorePath());
        certificate.setCertificateKeystoreType(config.keystoreType);
        if (config.keystorePassword != null) {
            certificate.setCertificateKeystorePassword(config.keystorePassword);
        }
        if (config.keyAlias != null) {
            certificate.setCertificateKeyAlias(config.keyAlias);
        }
        if (config.keyPassword != null) {
            certificate.setCertificateKeyPassword(config.keyPassword);
        }
        sslHostConfig.addCertificate(certificate);
        connector.addSslHostConfig(sslHostConfig);
        return connector;
    }

    public void start() throws LifecycleException {
        server.start();
    }

    public void stop() {
        try {
            if (server.getState().isAvailable()) {
                server.stop();
            }
        } catch (LifecycleException e) {
            throw new IllegalStateException("Unable to stop Tomcat", e);
        } finally {
            try {
                if (server.getState() != LifecycleState.DESTROYED) {
                    server.destroy();
                }
            } catch (LifecycleException e) {
                // the server is stopped, nothing else can be done
            }
        }
    }

    /**
     * Stops the connectors only, which releases their ports while the contexts are still being undeployed. They are
     * destroyed: a connector binds its port when it is initialized, and releases it when it is destroyed.
     */
    public void stopConnectors() {
        for (Connector connector : service.findConnectors()) {
            try {
                connector.stop();
                connector.destroy();
            } catch (LifecycleException e) {
                LOGGER.warn("Unable to stop the connector {}", connector, e);
            }
        }
    }

    public StandardHost getHost() {
        return host;
    }

    public List<String> getEndpoints() {
        List<String> endpoints = new ArrayList<>();
        for (Connector connector : service.findConnectors()) {
            int port = connector.getLocalPort() > 0 ? connector.getLocalPort() : connector.getPort();
            endpoints.add(connector.getScheme() + "://" + config.host + ":" + port);
        }
        return endpoints;
    }

}
