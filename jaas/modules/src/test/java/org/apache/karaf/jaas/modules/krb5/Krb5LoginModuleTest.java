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
package org.apache.karaf.jaas.modules.krb5;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.Krb5LoginConfiguration;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.kerberos.kdc.KerberosTestUtils;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.KerberosUtils;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.crypto.checksum.ChecksumType;
import org.apache.karaf.jaas.modules.NamePasswordCallbackHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.karaf.jaas.modules.PrincipalHelper.names;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(FrameworkRunner.class)
@CreateDS(name = "Krb5LoginModuleTest-class",
        partitions =
                {
                        @CreatePartition(
                                name = "example",
                                suffix = "dc=example,dc=com",
                                contextEntry = @ContextEntry(
                                        entryLdif =
                                                "dn: dc=example,dc=com\n" +
                                                        "dc: example\n" +
                                                        "objectClass: top\n" +
                                                        "objectClass: domain\n\n"),
                                indexes =
                                        {
                                                @CreateIndex(attribute = "objectClass"),
                                                @CreateIndex(attribute = "dc"),
                                                @CreateIndex(attribute = "ou")
                                        })
                },
        additionalInterceptors =
                {
                        KeyDerivationInterceptor.class
                })
@CreateLdapServer(
        transports =
                {
                        @CreateTransport(protocol = "LDAP")
                },
        saslHost = "localhost",
        saslPrincipal = "ldap/localhost@EXAMPLE.COM",
        saslMechanisms =
                {
                        @SaslMechanism(name = SupportedSaslMechanisms.PLAIN, implClass = PlainMechanismHandler.class),
                        @SaslMechanism(name = SupportedSaslMechanisms.CRAM_MD5, implClass = CramMd5MechanismHandler.class),
                        @SaslMechanism(name = SupportedSaslMechanisms.DIGEST_MD5, implClass = DigestMd5MechanismHandler.class),
                        @SaslMechanism(name = SupportedSaslMechanisms.GSSAPI, implClass = GssapiMechanismHandler.class),
                        @SaslMechanism(name = SupportedSaslMechanisms.NTLM, implClass = NtlmMechanismHandler.class),
                        @SaslMechanism(name = SupportedSaslMechanisms.GSS_SPNEGO, implClass = NtlmMechanismHandler.class)
                })
@CreateKdcServer(
        transports =
                {
                        @CreateTransport(protocol = "UDP", port = 6088),
                        @CreateTransport(protocol = "TCP", port = 6088)
                })
@ApplyLdifs({
        "dn: ou=users,dc=example,dc=com",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "ou: users"
})
public class Krb5LoginModuleTest extends KarafKerberosITest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        setupEnv(TcpTransport.class,
                EncryptionType.AES128_CTS_HMAC_SHA1_96, ChecksumType.HMAC_SHA1_96_AES128);

        // Set up a partition for EXAMPLE.COM and add user and service principals to test authentication with.
        KerberosTestUtils.fixServicePrincipalName(
                "ldap/" + KerberosTestUtils.getHostName() + "@EXAMPLE.COM", null, getLdapServer());

        kdcServer.getConfig().setPaEncTimestampRequired(false);
        // Use our custom configuration to avoid reliance on external config
        Configuration.setConfiguration(new Krb5LoginConfiguration());
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testKeytabSuccess() throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put("debug", "true");
        props.put("useKeyTab", "true");
        props.put("keyTab", createKeytab());
        props.put("principal", "hnelson@EXAMPLE.COM");
        props.put("doNotPrompt", "true");
        props.put("storeKey", "true");
        props.put("detailed.login.exception", "true");
        Subject subject = new Subject();
        Krb5LoginModule module = new Krb5LoginModule();

        module.initialize(subject, null, null, props);

        assertEquals("Precondition", 0, subject.getPrincipals().size());

        Assert.assertTrue(module.login());
        Assert.assertTrue(module.commit());

        assertEquals(1, subject.getPrincipals().size());
        assertThat(names(subject.getPrincipals(KerberosPrincipal.class)), containsInAnyOrder("hnelson@EXAMPLE.COM"));

        KerberosTicket ticket = subject.getPrivateCredentials(KerberosTicket.class).iterator().next();
        assertEquals("hnelson@EXAMPLE.COM", ticket.getClient().getName());
        assertEquals("krbtgt/EXAMPLE.COM@EXAMPLE.COM", ticket.getServer().getName());

        Assert.assertTrue(module.logout());

    }

    @Test(expected = LoginException.class)
    public void testKeytabFailure() throws Exception {

        Map<String, Object> props = new HashMap<>();
        props.put("debug", "true");
        props.put("useKeyTab", "true");
        props.put("keyTab", createKeytab());
        props.put("principal", "hnelson0@EXAMPLE.COM");
        props.put("doNotPrompt", "true");
        props.put("storeKey", "true");
        props.put("detailed.login.exception", "true");

        Subject subject = new Subject();
        Krb5LoginModule module = new Krb5LoginModule();
        module.initialize(subject, null, null, props);

        assertEquals("Precondition", 0, subject.getPrincipals().size());

        Assert.assertFalse(module.login());

    }

    @Test
    public void testLoginSuccess() throws Exception {
        Subject subject = new Subject();
        Krb5LoginModule module = new Krb5LoginModule();
        module.initialize(subject, new NamePasswordCallbackHandler("hnelson", "secret"), null, new HashMap<>());

        assertEquals("Precondition", 0, subject.getPrincipals().size());

        Assert.assertTrue(module.login());
        Assert.assertTrue(module.commit());

        assertEquals(1, subject.getPrincipals().size());
        assertThat(names(subject.getPrincipals(KerberosPrincipal.class)), containsInAnyOrder("hnelson@EXAMPLE.COM"));

        KerberosTicket ticket = subject.getPrivateCredentials(KerberosTicket.class).iterator().next();
        assertEquals("hnelson@EXAMPLE.COM", ticket.getClient().getName());
        assertEquals("krbtgt/EXAMPLE.COM@EXAMPLE.COM", ticket.getServer().getName());

        Assert.assertTrue(module.logout());

    }

    @Test(expected = LoginException.class)
    public void testLoginUsernameFailure() throws Exception {
        Subject subject = new Subject();

        Krb5LoginModule module = new Krb5LoginModule();
        module.initialize(subject, new NamePasswordCallbackHandler("hnelson0", "secret"), null, new HashMap<>());

        assertEquals("Precondition", 0, subject.getPrincipals().size());

        Assert.assertFalse(module.login());

    }

    @Test(expected = LoginException.class)
    public void testLoginPasswordFailure() throws Exception {
        Subject subject = new Subject();

        Krb5LoginModule module = new Krb5LoginModule();
        module.initialize(subject, new NamePasswordCallbackHandler("hnelson", "secret0"), null, new HashMap<>());

        assertEquals("Precondition", 0, subject.getPrincipals().size());

        Assert.assertFalse(module.login());

    }

    protected void setupEnv(Class<? extends Transport> transport, EncryptionType encryptionType,
                            ChecksumType checksumType)
            throws Exception {
        // create krb5.conf with proper encryption type
        String krb5confPath = createKrb5Conf(checksumType, encryptionType, transport == TcpTransport.class);
        System.setProperty("java.security.krb5.conf", krb5confPath);

        // change encryption type in KDC
        kdcServer.getConfig().setEncryptionTypes(Collections.singleton(encryptionType));

        // create principals
        createPrincipal("uid=" + USER_UID, "Last", "First Last",
                USER_UID, USER_PASSWORD, USER_UID + "@" + REALM);

        createPrincipal("uid=krbtgt", "KDC Service", "KDC Service",
                "krbtgt", "secret", "krbtgt/" + REALM + "@" + REALM);

        String servicePrincipal = LDAP_SERVICE_NAME + "/" + HOSTNAME + "@" + REALM;
        createPrincipal("uid=ldap", "Service", "LDAP Service",
                "ldap", "randall", servicePrincipal);
    }

    private void createPrincipal(String rdn, String sn, String cn,
                                 String uid, String userPassword, String principalName) throws LdapException {
        Entry entry = new DefaultEntry();
        entry.setDn(rdn + "," + USERS_DN);
        entry.add("objectClass", "top", "person", "inetOrgPerson", "krb5principal", "krb5kdcentry");
        entry.add("cn", cn);
        entry.add("sn", sn);
        entry.add("uid", uid);
        entry.add("userPassword", userPassword);
        entry.add("krb5PrincipalName", principalName);
        entry.add("krb5KeyVersionNumber", "0");
        conn.add(entry);
    }

    private KeytabEntry createKeytabEntry() throws ParseException {
        String principalName = "hnelson@EXAMPLE.COM";
        int principalType = 1;

        String zuluTime = "20070217235745Z";
        Date date = null;

        synchronized (KerberosUtils.UTC_DATE_FORMAT) {
            date = KerberosUtils.UTC_DATE_FORMAT.parse(zuluTime);
        }

        KerberosTime timeStamp = new KerberosTime(date.getTime());

        byte keyVersion = 1;
        String passPhrase = "secret";
        Map<EncryptionType, EncryptionKey> keys = KerberosKeyFactory.getKerberosKeys(principalName, passPhrase);
        EncryptionKey key = keys.get(EncryptionType.AES128_CTS_HMAC_SHA1_96);

        return new KeytabEntry(principalName, principalType, timeStamp, keyVersion, key);
    }

    private String createKeytab() throws Exception {
        File file = folder.newFile("test.keytab");

        List<KeytabEntry> entries = new ArrayList<>();

        entries.add(createKeytabEntry());

        Keytab writer = Keytab.getInstance();
        writer.setEntries(entries);
        writer.write(file);

        return file.getAbsolutePath();
    }
}
