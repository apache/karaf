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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Hashtable;
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
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClientCreator;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SftpScpRbacTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Configuration previousConfig;
    private String previousKarafBase;
    private String previousKarafEtc;
    private SshServer server;
    private SshClient client;
    private Path baseDir;

    @Before
    public void setUp() throws Exception {
        previousConfig = Configuration.getConfiguration();
        Configuration.setConfiguration(new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                return new AppConfigurationEntry[]{
                        new AppConfigurationEntry(RoleLoginModule.class.getName(),
                                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, emptyMap())
                };
            }
        });

        baseDir = tempFolder.newFolder("karaf-base").toPath();
        Path etcDir = Files.createDirectories(baseDir.resolve("etc"));
        Files.write(etcDir.resolve("users.properties"), "karaf=karaf\n".getBytes(StandardCharsets.UTF_8));

        previousKarafBase = System.getProperty("karaf.base");
        previousKarafEtc = System.getProperty("karaf.etc");
        System.setProperty("karaf.base", baseDir.toString());
        System.setProperty("karaf.etc", etcDir.toString());

        Dictionary<String, Object> props = new Hashtable<>();
        props.put("sshPort", "0");
        props.put("sshHost", "127.0.0.1");
        props.put("sshRole", "ssh");
        props.put("sftpEnabled", "true");
        Activator activator = new Activator() {
            @Override
            protected void reconfigure() {
            }
        };
        activator.updated(props);

        SessionFactory sessionFactory = EasyMock.createNiceMock(SessionFactory.class);
        EasyMock.replay(sessionFactory);

        server = activator.createSshServer(sessionFactory);
        server.start();

        client = SshClient.setUpDefaultClient();
        client.start();
    }

    @After
    public void tearDown() throws Exception {
        if (client != null) {
            client.stop();
        }
        if (server != null) {
            server.stop(true);
        }
        if (previousKarafBase != null) {
            System.setProperty("karaf.base", previousKarafBase);
        } else {
            System.clearProperty("karaf.base");
        }
        if (previousKarafEtc != null) {
            System.setProperty("karaf.etc", previousKarafEtc);
        } else {
            System.clearProperty("karaf.etc");
        }
        Configuration.setConfiguration(previousConfig);
    }

    private ClientSession connectAndAuth(String username) throws IOException {
        ClientSession session = client.connect(username, "127.0.0.1", server.getPort())
                .verify(5000).getSession();
        session.addPasswordIdentity(username);
        assertTrue(session.auth().verify(5000).isSuccess());
        return session;
    }

    @Test
    public void viewerCannotUseSftpOrScp() throws Exception {
        try (ClientSession session = connectAndAuth("viewer")) {
            try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                fail("Viewer user without sftpRole should not be able to open SFTP subsystem");
            } catch (IOException expected) {
                // expected
            }

            ScpClient scp = ScpClientCreator.instance().createScpClient(session);
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scp.download("etc/users.properties", baos);
                fail("Viewer user without sftpRole should not be able to download via SCP");
            } catch (IOException expected) {
                // expected
            }

            try {
                scp.upload("evil".getBytes(StandardCharsets.UTF_8), "etc/evil.txt",
                        java.util.Collections.emptyList(), null);
                fail("Viewer user without sftpRole should not be able to upload via SCP");
            } catch (IOException expected) {
                // expected
            }
        }
    }

    @Test
    public void adminCanUseSftpAndScp() throws Exception {
        try (ClientSession session = connectAndAuth("admin")) {
            try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
                SftpClient.Attributes attrs = sftp.stat("etc/users.properties");
                assertTrue(attrs.isRegularFile());
            }

            ScpClient scp = ScpClientCreator.instance().createScpClient(session);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scp.download("etc/users.properties", baos);
            assertArrayEquals("karaf=karaf\n".getBytes(StandardCharsets.UTF_8), baos.toByteArray());
        }
    }

    public static class RoleLoginModule implements LoginModule {
        private Subject subject;
        private String username;

        @Override
        public void initialize(Subject subject, CallbackHandler callbackHandler,
                               Map<String, ?> sharedState, Map<String, ?> options) {
            this.subject = subject;
            NameCallback nc = new NameCallback("user");
            try {
                callbackHandler.handle(new Callback[]{nc});
            } catch (IOException | UnsupportedCallbackException e) {
                throw new IllegalArgumentException(e);
            }
            this.username = nc.getName();
        }

        @Override
        public boolean login() {
            return username != null;
        }

        @Override
        public boolean commit() {
            subject.getPrincipals().add(new UserPrincipal(username));
            subject.getPrincipals().add(new RolePrincipal("ssh"));
            if ("admin".equals(username)) {
                subject.getPrincipals().add(new RolePrincipal("admin"));
            } else if ("viewer".equals(username)) {
                subject.getPrincipals().add(new RolePrincipal("viewer"));
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
