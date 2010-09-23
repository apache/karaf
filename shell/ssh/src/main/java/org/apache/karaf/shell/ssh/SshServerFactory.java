/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.ssh;

import java.util.Map;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.jaas.JaasPasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.fusesource.cade.Configurable;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshServerFactory implements Configurable<SshConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshServerFactory.class);

    private String serverId;
    private BlueprintContainer blueprintContainer;
    private boolean start;

    private SshServer server;

    public SshServerFactory() {
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public BlueprintContainer getBlueprintContainer() {
        return blueprintContainer;
    }

    public void setBlueprintContainer(BlueprintContainer blueprintContainer) {
        this.blueprintContainer = blueprintContainer;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public void start(final SshConfig config) {
        if (start) {
            try {
                server = (SshServer) blueprintContainer.getComponentInstance(serverId);
                server.setPort(config.sshPort());
                server.setHost(config.sshHost());
                server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(config.hostKey()));
                server.setPasswordAuthenticator(newJaasPasswordAuthenticator((config.sshRealm())));
                server.start();
            } catch (Exception e) {
                LOGGER.info("Error updating SSH server", e);
            }
        }
    }

    private JaasPasswordAuthenticator newJaasPasswordAuthenticator(String domain) {
        JaasPasswordAuthenticator auth = new JaasPasswordAuthenticator();
        auth.setDomain(domain);
        return auth;
    }

    public void stop() {
        if (start && server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                LOGGER.info("Error stopping SSH server", e);
            } finally {
                server = null;
            }
        }
    }

    public void setup(SshConfig sshConfig) {
        stop();
        start(sshConfig);
    }

    public void deleted() {
        stop();
    }

}
