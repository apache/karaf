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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.karaf.shell.api.action.lifecycle.Manager;
import org.apache.karaf.shell.api.console.CommandLoggingFilter;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.support.RegexCommandLoggingFilter;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.Managed;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activate this bundle
 */
@Services(
        requires = @RequireService(SessionFactory.class)
)
@Managed("org.apache.karaf.shell")
public class Activator extends BaseActivator implements ManagedService {

    static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    ServiceTracker<Session, Session> sessionTracker;
    KarafAgentFactory agentFactory;
    SessionFactory sessionFactory;
    SshServer server;

    @Override
    protected void doOpen() throws Exception {
        agentFactory = new KarafAgentFactory();

        super.doOpen();

        sessionTracker = new ServiceTracker<Session, Session>(bundleContext, Session.class, null) {
            @Override
            public Session addingService(ServiceReference<Session> reference) {
                Session session = super.addingService(reference);
                agentFactory.registerSession(session);
                return session;
            }
            @Override
            public void removedService(ServiceReference<Session> reference, Session session) {
                agentFactory.unregisterSession(session);
                super.removedService(reference, session);
            }
        };
        sessionTracker.open();
    }

    @Override
    protected void doClose() {
        sessionTracker.close();
        super.doClose();
    }

    @Override
    protected void doStart() throws Exception {
        SessionFactory sf = getTrackedService(SessionFactory.class);
        if (sf == null) {
            return;
        }

        RegexCommandLoggingFilter filter = new RegexCommandLoggingFilter();
        filter.setPattern("ssh (.*?)-P +([^ ]+)");
        filter.setGroup(2);
        register(CommandLoggingFilter.class, filter);

        filter = new RegexCommandLoggingFilter();
        filter.setPattern("ssh (.*?)--password +([^ ]+)");
        filter.setGroup(2);
        register(CommandLoggingFilter.class, filter);

        sessionFactory = sf;
        sessionFactory.getRegistry().getService(Manager.class).register(SshAction.class);
        if (Boolean.parseBoolean(bundleContext.getProperty("karaf.startRemoteShell"))) {
            server = createSshServer(sessionFactory);
            this.bundleContext.registerService(SshServer.class, server, null);
            if (server == null) {
                return; // can result from bad specification.
            }
            try {
                server.start();
            } catch (IOException e) {
                LOGGER.warn("Exception caught while starting SSH server", e);
            }
        }
    }

    @Override
    protected void doStop() {
        if (sessionFactory != null) {
            sessionFactory.getRegistry().getService(Manager.class).unregister(SshAction.class);
            sessionFactory = null;
        }
        if (server != null) {
            try {
                server.stop(true);
            } catch (IOException e) {
                LOGGER.warn("Exception caught while stopping SSH server", e);
            }
            server = null;
        }
        super.doStop();
    }

    protected SshServer createSshServer(SessionFactory sessionFactory) {
        int sshPort           = getInt("sshPort", 8181);
        String sshHost        = getString("sshHost", "0.0.0.0");
        long sshIdleTimeout   = getLong("sshIdleTimeout", 1800000);
        String sshRealm       = getString("sshRealm", "karaf");
        String hostKey        = getString("hostKey", System.getProperty("karaf.etc") + "/host.key");
        String hostKeyFormat  = getString("hostKeyFormat", "simple");
        String authMethods    = getString("authMethods", "keyboard-interactive,password,publickey");
        int keySize           = getInt("keySize", 4096);
        String algorithm      = getString("algorithm", "RSA");
        String macs           = getString("macs", "hmac-sha1");
        String ciphers        = getString("ciphers", "aes256-ctr,aes192-ctr,aes128-ctr,arcfour256");
        String welcomeBanner  = getString("welcomeBanner", null);

        AbstractGeneratorHostKeyProvider keyPairProvider;
        if ("simple".equalsIgnoreCase(hostKeyFormat)) {
            keyPairProvider = new SimpleGeneratorHostKeyProvider();
        } else if ("PEM".equalsIgnoreCase(hostKeyFormat)) {
            keyPairProvider = new OpenSSHGeneratorFileKeyProvider();
        } else {
            LOGGER.error("Invalid host key format " + hostKeyFormat);
            return null;
        }

        keyPairProvider.setPath(Paths.get(hostKey));
        if (new File(hostKey).exists()) {
            // do not trash key file if there's something wrong with it.
            keyPairProvider.setOverwriteAllowed(false);
        } else {
            keyPairProvider.setKeySize(keySize);
            keyPairProvider.setAlgorithm(algorithm);
        }

        KarafJaasAuthenticator authenticator = new KarafJaasAuthenticator(sshRealm);

        UserAuthFactoriesFactory authFactoriesFactory = new UserAuthFactoriesFactory();
        authFactoriesFactory.setAuthMethods(authMethods);

        SshServer server = SshServer.setUpDefaultServer();
        server.setPort(sshPort);
        server.setHost(sshHost);
        server.setMacFactories(SshUtils.buildMacs(macs));
        server.setCipherFactories(SshUtils.buildCiphers(ciphers));
        server.setShellFactory(new ShellFactoryImpl(sessionFactory));
        server.setCommandFactory(new ScpCommandFactory.Builder().withDelegate(new ShellCommandFactory(sessionFactory)).build());
        server.setSubsystemFactories(Arrays.<NamedFactory<org.apache.sshd.server.Command>>asList(new SftpSubsystemFactory()));
        server.setKeyPairProvider(keyPairProvider);
        server.setPasswordAuthenticator(authenticator);
        server.setPublickeyAuthenticator(authenticator);
        server.setFileSystemFactory(new VirtualFileSystemFactory(Paths.get(System.getProperty("karaf.base"))));
        server.setUserAuthFactories(authFactoriesFactory.getFactories());
        server.setAgentFactory(agentFactory);
        server.getProperties().put(SshServer.IDLE_TIMEOUT, Long.toString(sshIdleTimeout));
        if (welcomeBanner != null) {
            server.getProperties().put(SshServer.WELCOME_BANNER, welcomeBanner);
        } 
        return server;
    }

}
