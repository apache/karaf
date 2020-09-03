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


package org.apache.karaf.itests.ssh;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.keyverifier.RequiredServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.ClientSession.ClientSessionEvent;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.EnumSet;
import java.util.Set;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

/**
 * Test use of PEM keys.
 */
public class SshKeyFormatTest extends SshCommandTestBase {

    @Configuration
    public Option[] config() {
        File keyFile = new File("src/test/resources/org/apache/karaf/itests/ssh/test.pem");
        return options(composite(super.config()),
                editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "hostKey", keyFile.getAbsolutePath()),
                bundle("mvn:org.bouncycastle/bcprov-jdk15on/1.66"),
                bundle("mvn:org.bouncycastle/bcpkix-jdk15on/1.66"),
                bundle("mvn:com.google.guava/guava/16.0.1")
                );
    }


    @Test
    public void usePemKey() throws Exception {
        SshClient client = SshClient.setUpDefaultClient();
        URL testPemURL = Resources.getResource(SshKeyFormatTest.class, "test.pem");
        ByteSource source = Resources.asByteSource(testPemURL);
        KeyPair keyPair = getKeyPair(source.openStream());

        String sshPort = getSshPort();

        client.setServerKeyVerifier(new RequiredServerKeyVerifier(keyPair.getPublic()));
        client.start();
        ConnectFuture future = client.connect("karaf", "localhost", Integer.parseInt(sshPort));
        future.await();
        ClientSession session = future.getSession();

        Set<ClientSessionEvent> ret = EnumSet.of(ClientSessionEvent.WAIT_AUTH);
        while (ret.contains(ClientSessionEvent.WAIT_AUTH)) {
            session.addPasswordIdentity("karaf");
            session.auth().verify();
            ret = session.waitFor(EnumSet.of(ClientSessionEvent.WAIT_AUTH, ClientSessionEvent.CLOSED, ClientSessionEvent.AUTHED), 0);
        }
        if (ret.contains(ClientSessionEvent.CLOSED)) {
            throw new Exception("Could not open SSH channel");
        }
        session.close(true);
    }

    public static KeyPair getKeyPair(InputStream is) throws GeneralSecurityException, IOException {
        try (PEMParser parser = new PEMParser(new InputStreamReader(is))) {
            Object o = parser.readObject();

            JcaPEMKeyConverter pemConverter = new JcaPEMKeyConverter();
            return pemConverter.getKeyPair((PEMKeyPair)o);
        }
    }

}
