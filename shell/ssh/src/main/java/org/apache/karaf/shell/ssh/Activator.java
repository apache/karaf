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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.karaf.shell.api.action.lifecycle.Manager;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.util.tracker.SingleServiceTracker;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activate this bundle
 */
public class Activator implements BundleActivator, ManagedService {

    static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    ServiceRegistration registration;

    List<Session> sessions = new CopyOnWriteArrayList<Session>();

    BundleContext bundleContext;
    SingleServiceTracker<SessionFactory> sessionFactoryTracker;
    ServiceTracker<Session, Session> sessionTracker;
    Dictionary<String, ?> configuration;

    final KarafAgentFactory agentFactory = new KarafAgentFactory();

    SessionFactory sessionFactory;
    SshClientFactory sshClientFactory;
    final Callable<SshServer> sshServerFactory = new Callable<SshServer>() {
        @Override
        public SshServer call() throws Exception {
            return createSshServer(sessionFactory);
        }
    };
    SshServer server;
    final List<SshServer> servers = new ArrayList<SshServer>();


    public void start(BundleContext context) throws Exception {
        bundleContext = context;

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, "org.apache.karaf.shell");
        registration = bundleContext.registerService(ManagedService.class, this, props);

        sshClientFactory = new SshClientFactory(agentFactory, new File(context.getProperty("user.home"), ".sshkaraf/known_hosts"));

        sessionFactoryTracker = new SingleServiceTracker<SessionFactory>(bundleContext, SessionFactory.class, new SingleServiceTracker.SingleServiceListener() {
            @Override
            public void serviceFound() {
                bindSessionFactory(sessionFactoryTracker.getService());
            }
            @Override
            public void serviceLost() {
                unbindSessionFactory();
            }
            @Override
            public void serviceReplaced() {
                serviceLost();
                serviceFound();
            }
        });
        sessionFactoryTracker.open();

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

    private void bindSessionFactory(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.sessionFactory.getRegistry().register(sshServerFactory, SshServer.class);
        this.sessionFactory.getRegistry().register(sshClientFactory);
        this.sessionFactory.getRegistry().getService(Manager.class).register(SshServerAction.class);
        this.sessionFactory.getRegistry().getService(Manager.class).register(SshAction.class);
        if (Boolean.parseBoolean(Activator.this.bundleContext.getProperty("karaf.startRemoteShell"))) {
            server = createSshServer(sessionFactory);
            try {
                server.start();
            } catch (IOException e) {
                LOGGER.warn("Exception caught while starting SSH server", e);
            }
        }
    }

    private void unbindSessionFactory() {
        this.sessionFactory.getRegistry().getService(Manager.class).unregister(SshAction.class);
        this.sessionFactory.getRegistry().getService(Manager.class).unregister(SshServerAction.class);
        this.sessionFactory.getRegistry().unregister(sshClientFactory);
        this.sessionFactory.getRegistry().unregister(sshServerFactory);
        SshServer srv = server;
        server = null;
        if (srv != null) {
            try {
                srv.stop();
            } catch (InterruptedException e) {
                LOGGER.warn("Exception caught while stopping SSH server", e);
            }
        }
    }

    public void stop(BundleContext context) {
        registration.unregister();
        sessionTracker.close();
        sessionFactoryTracker.close();
        synchronized (servers) {
            for (SshServer server : servers) {
                try {
                    server.stop();
                } catch (InterruptedException e) {
                    LOGGER.warn("Exception caught while stopping SSH server", e);
                }
            }
        }
    }

    @Override
    public void updated(Dictionary<String, ?> configuration) throws ConfigurationException {
        this.configuration = configuration;
    }

    private int getInt(String key, int def) {
        Dictionary<String, ?> config = this.configuration;
        if (config != null) {
            Object val = config.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            } else if (val != null) {
                return Integer.parseInt(val.toString());
            }
        }
        return def;
    }

    private long getLong(String key, long def) {
        Dictionary<String, ?> config = this.configuration;
        if (config != null) {
            Object val = config.get(key);
            if (val instanceof Number) {
                return ((Number) val).longValue();
            } else if (val != null) {
                return Long.parseLong(val.toString());
            }
        }
        return def;
    }

    private String getString(String key, String def) {
        Dictionary<String, ?> config = this.configuration;
        if (config != null) {
            Object val = config.get(key);
            if (val != null) {
                return val.toString();
            }
        }
        return def;
    }

    protected SshServer createSshServer(SessionFactory sessionFactory) {
        int sshPort           = getInt("sshPort", 8181);
        String sshHost        = getString("sshHost", "0.0.0.0");
        long sshIdleTimeout   = getLong("sshIdleTimeout", 1800000);
        String sshRealm       = getString("sshRealm", "karaf");
        String hostKey        = getString("hostKey", System.getProperty("karaf.base") + "/etc/host.key");
        String authMethods    = getString("authMethods", "keyboard-interactive,password,publickey");
        int keySize           = getInt("keySize", 1024);
        String algorithm      = getString("algorithm", "DSA");
        String macs           = getString("macs", "hmac-sha1");
        String ciphers        = getString("ciphers", "aes256-ctr,aes192-ctr,aes128-ctr,arcfour256");

        SimpleGeneratorHostKeyProvider keyPairProvider = new SimpleGeneratorHostKeyProvider();
        keyPairProvider.setPath(hostKey);
        keyPairProvider.setKeySize(keySize);
        keyPairProvider.setAlgorithm(algorithm);

        KarafJaasAuthenticator authenticator = new KarafJaasAuthenticator(sshRealm);

        UserAuthFactoriesFactory authFactoriesFactory = new UserAuthFactoriesFactory();
        authFactoriesFactory.setAuthMethods(authMethods);

        SshServer server = SshServer.setUpDefaultServer();
        server.setPort(sshPort);
        server.setHost(sshHost);
        server.setMacFactories(SshUtils.buildMacs(macs));
        server.setCipherFactories(SshUtils.buildCiphers(ciphers));
        server.setShellFactory(new ShellFactoryImpl(sessionFactory));
        server.setCommandFactory(new ScpCommandFactory(new ShellCommandFactory(sessionFactory)));
        server.setSubsystemFactories(Arrays.<NamedFactory<org.apache.sshd.server.Command>>asList(new SftpSubsystem.Factory()));
        server.setKeyPairProvider(keyPairProvider);
        server.setPasswordAuthenticator(authenticator);
        server.setPublickeyAuthenticator(authenticator);
        server.setFileSystemFactory(new KarafFileSystemFactory());
        server.setUserAuthFactories(authFactoriesFactory.getFactories());
        server.setAgentFactory(agentFactory);
        server.getProperties().put(SshServer.IDLE_TIMEOUT, Long.toString(sshIdleTimeout));

        synchronized (servers) {
            servers.add(server);
        }
        return server;
    }

}
