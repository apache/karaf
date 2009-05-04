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
package org.apache.felix.karaf.management;

import java.io.IOException;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class ConnectorServerFactory {

    private MBeanServer server;
    private String serviceUrl;
    private Map environment;
    private ObjectName objectName;
    private boolean threaded = false;
    private boolean daemon = false;
    private JMXConnectorServer connectorServer;

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

    public void init() throws Exception {
        if (this.server == null) {
            throw new IllegalArgumentException("server must be set");
        }
        JMXServiceURL url = new JMXServiceURL(this.serviceUrl);
        this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, this.environment, this.server);
        if (this.objectName != null) {
            this.server.registerMBean(this.connectorServer, this.objectName);
        }
        try {
            if (this.threaded) {
                Thread connectorThread = new Thread() {
                    public void run() {
                        try {
                            connectorServer.start();
                        } catch (IOException ex) {
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
}
