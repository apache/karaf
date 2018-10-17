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

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.spi.LoginModule;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.mina.core.session.DummySession;
import org.apache.sshd.common.io.mina.MinaConnector;
import org.apache.sshd.common.io.mina.MinaSession;
import org.apache.sshd.common.random.SingletonRandomFactory;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.session.ServerSessionImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KarafJaasAuthenticatorTest {
    private Configuration configuration;
    private ServerSessionImpl session;

    @Before
    public void init() throws Exception {
        configuration = Configuration.getConfiguration();
        Configuration.setConfiguration(new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                return new AppConfigurationEntry[] {
                        new AppConfigurationEntry(SayYes.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, emptyMap())
                };
            }
        });
        final SshServer server = new SshServer();
        server.setRandomFactory(new SingletonRandomFactory(SecurityUtils.getRandomFactory()));
        this.session = new ServerSessionImpl(server,
                new MinaSession(new MinaConnector(null, null, null), new DummySession()));
    }

    @After
    public void reset() {
        Configuration.setConfiguration(configuration);
    }

    @Test
    public void authenticateOk() {
        final KarafJaasAuthenticator authenticator = new KarafJaasAuthenticator("karaf", "test",
                new Class<?>[]{RolePrincipal.class});
        assertTrue(authenticator.authenticate("test", "test", session));
    }

    @Test
    public void authenticateKo() {
        final KarafJaasAuthenticator authenticator = new KarafJaasAuthenticator("karaf", "test",
                new Class<?>[]{RolePrincipal.class});
        assertFalse(authenticator.authenticate("testko", "test", session));
    }

    @Test
    public void invalidRole() {
        final KarafJaasAuthenticator authenticator = new KarafJaasAuthenticator("karaf", "test",
                new Class<?>[]{RolePrincipal.class});
        assertFalse(authenticator.authenticate("customRole", "test", session));
    }

    @Test
    public void noRole() {
        final KarafJaasAuthenticator authenticator = new KarafJaasAuthenticator("karaf", "test",
                new Class<?>[0]);
        assertFalse(authenticator.authenticate("norole", "test", session));
    }

    @Test
    public void customRole() {
        final KarafJaasAuthenticator authenticator = new KarafJaasAuthenticator("karaf", "test",
                new Class<?>[]{UserPrincipal.class});
        assertTrue(authenticator.authenticate("customRole", "test", session));
    }

    public static class SayYes implements LoginModule {
        private String name;
        private Subject subject;

        @Override
        public void initialize(final Subject subject,
                               final CallbackHandler callbackHandler,
                               final Map<String, ?> sharedState,
                               final Map<String, ?> options) {
            final NameCallback nameCallback = new NameCallback("name?");
            try {
                callbackHandler.handle(new Callback[]{nameCallback});
            } catch (final IOException | UnsupportedCallbackException e) {
                throw new IllegalArgumentException(e);
            }
            this.subject = subject;
            this.name = nameCallback.getName();
        }

        @Override
        public boolean login() {
            return !name.contains("ko");
        }

        @Override
        public boolean commit() {
            switch (name) {
                case "norole":
                    break;
                case "customRole":
                    subject.getPrincipals().add(new UserPrincipal("test"));
                    break;
                case "test":
                    subject.getPrincipals().add(new RolePrincipal("test"));
                    break;
                default:
            }
            return true;
        }

        @Override
        public boolean abort() {
            return true;
        }

        @Override
        public boolean logout() {
            return true;
        }
    }
}
