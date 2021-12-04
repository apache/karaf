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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.SshAgentFactory;
import org.apache.sshd.agent.SshAgentServer;
import org.apache.sshd.agent.common.AgentDelegate;
import org.apache.sshd.agent.local.AgentImpl;
import org.apache.sshd.agent.local.AgentServerProxy;
import org.apache.sshd.agent.local.LocalAgentFactory;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.common.channel.ChannelFactory;
import org.apache.sshd.common.session.ConnectionService;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarafAgentFactory implements SshAgentFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(KarafAgentFactory.class);

    private final Map<String, AgentServerProxy> proxies = new ConcurrentHashMap<>();
    private final Map<String, SshAgent> locals = new ConcurrentHashMap<>();

    private static final KarafAgentFactory INSTANCE = new KarafAgentFactory();

    public static KarafAgentFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public List<ChannelFactory> getChannelForwardingFactories(FactoryManager factoryManager) {
        return LocalAgentFactory.DEFAULT_FORWARDING_CHANNELS;
    }

    @Override
    public SshAgent createClient(Session session, FactoryManager manager) throws IOException {
        String proxyId = (String) manager.getProperties().get(SshAgent.SSH_AUTHSOCKET_ENV_NAME);
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

    public SshAgentServer createServer(ConnectionService service) throws IOException {
        Session session = service.getSession();
        if (!(session instanceof ServerSession)) {
            throw new IllegalStateException("The session used to create an agent server proxy must be a server session");
        }
        final AgentServerProxy proxy = new AgentServerProxy(service);
        proxies.put(proxy.getId(), proxy);
        return new SshAgentServer() {
            public String getId() {
                return proxy.getId();
            }

            @Override
            public boolean isOpen() {
                return proxy.isOpen();
            }

            public void close() throws IOException {
                proxies.remove(proxy.getId());
                proxy.close();
            }
        };
    }

    public void registerSession(org.apache.karaf.shell.api.console.Session session) {
        try {
            String user = (String) session.get("USER");
            SshAgent agent = new AgentImpl();
            String agentId = "local:" + user;
            session.put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, agentId);
            locals.put(agentId, agent);
        } catch (Throwable e) {
            LOGGER.warn("Error starting ssh agent for local console", e);
        }
    }

    public void unregisterSession(org.apache.karaf.shell.api.console.Session session) {
        try {
            if (session != null && session.get(SshAgent.SSH_AUTHSOCKET_ENV_NAME) != null) {
                String agentId = (String) session.get(SshAgent.SSH_AUTHSOCKET_ENV_NAME);
                session.put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, null);
                if (agentId != null) {
                    locals.remove(agentId);
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Error stopping ssh agent for local console", e);
        }
    }

}
