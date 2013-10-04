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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.service.command.CommandSession;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.SshAgentFactory;
import org.apache.sshd.agent.SshAgentServer;
import org.apache.sshd.agent.common.AgentDelegate;
import org.apache.sshd.agent.local.AgentImpl;
import org.apache.sshd.agent.local.AgentServerProxy;
import org.apache.sshd.agent.local.ChannelAgentForwarding;
import org.apache.sshd.common.Channel;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.server.session.ServerSession;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarafAgentFactory implements SshAgentFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(KarafAgentFactory.class);

    private final Map<String, AgentServerProxy> proxies = new ConcurrentHashMap<String, AgentServerProxy>();
    private final Map<String, SshAgent> locals = new ConcurrentHashMap<String, SshAgent>();

    private BundleContext bundleContext;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public NamedFactory<Channel> getChannelForwardingFactory() {
        return new ChannelAgentForwarding.Factory();
    }

    public SshAgent createClient(Session session) throws IOException {
        String proxyId = session.getFactoryManager().getProperties().get(SshAgent.SSH_AUTHSOCKET_ENV_NAME);
        if (proxyId == null) {
            throw new IllegalStateException("No " + SshAgent.SSH_AUTHSOCKET_ENV_NAME + " environment variable set");
        }
        AgentServerProxy proxy = proxies.get(proxyId);
        if (proxy != null) {
            return proxy.createClient();
        }
        SshAgent agent = locals.get(proxyId);
        if (agent != null) {
            return new AgentDelegate(agent);
        }
        throw new IllegalStateException("No ssh agent found");
    }

    public SshAgentServer createServer(Session session) throws IOException {
        if (!(session instanceof ServerSession)) {
            throw new IllegalStateException("The session used to create an agent server proxy must be a server session");
        }
        final AgentServerProxy proxy = new AgentServerProxy((ServerSession) session);
        proxies.put(proxy.getId(), proxy);
        return new SshAgentServer() {
            public String getId() {
                return proxy.getId();
            }
            public void close() {
                proxies.remove(proxy.getId());
                proxy.close();
            }
        };
    }

    public void registerCommandSession(CommandSession session) {
        try {
            String user = (String) session.get("USER");
            SshAgent agent = new AgentImpl();
            URL url = bundleContext.getBundle().getResource("karaf.key");
            InputStream is = url.openStream();
            ObjectInputStream r = new ObjectInputStream(is);
            KeyPair keyPair = (KeyPair) r.readObject();
            agent.addIdentity(keyPair, "karaf");
            String agentId = "local:" + user;
            session.put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, agentId);
            locals.put(agentId, agent);
        } catch (Throwable e) {
            LOGGER.warn("Error starting ssh agent for local console", e);
        }
    }

    public void unregisterCommandSession(CommandSession session) {
        try {
            String agentId = (String) session.get(SshAgent.SSH_AUTHSOCKET_ENV_NAME);
            session.put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, null);
            if (agentId != null) {
                locals.remove(agentId);
            }
        } catch (Throwable e) {
            LOGGER.warn("Error stopping ssh agent for local console", e);
        }
    }

}
