/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.karaf.jaas.modules.ldap;

import static org.apache.karaf.jaas.modules.ldap.LdapPropsUpdater.ldapProps;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Collections;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.LoginException;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
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
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.crypto.checksum.ChecksumType;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.NamePasswordCallbackHandler;
import org.apache.karaf.jaas.modules.krb5.KarafKerberosITest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(FrameworkRunner.class)
@CreateDS(name = "GSSAPILdapLoginModuleTest-class",
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
        "ou: users",

        "dn: ou=groups,dc=example,dc=com",
        "objectClass: top",
        "objectClass: organizationalUnit",
        "ou: groups",

        "dn: cn=admin,ou=groups,dc=example,dc=com",
        "objectClass: top",
        "objectClass: groupOfNames",
        "cn: admin",
        "member: uid=hnelson,ou=users,dc=example,dc=com"
})
public class GSSAPILdapLoginModuleTest extends KarafKerberosITest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Set up a partition for EXAMPLE.COM and add user and service principals to test authentication with.
        KerberosTestUtils.fixServicePrincipalName(
                "ldap/" + KerberosTestUtils.getHostName() + "@EXAMPLE.COM", null, getLdapServer());
        setupEnv(TcpTransport.class,
                EncryptionType.AES128_CTS_HMAC_SHA1_96, ChecksumType.HMAC_SHA1_96_AES128);

        kdcServer.getConfig().setPaEncTimestampRequired(false);

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        File config = new File(basedir + "/target/test-classes/org/apache/karaf/jaas/modules/ldap/gssapi.login.config");

        System.setProperty("java.security.auth.login.config", config.toString());

        ldapProps("org/apache/karaf/jaas/modules/ldap/gssapi.ldap.properties",
                  GSSAPILdapLoginModuleTest::replacePortAndAddress);
    }

    public static String replacePortAndAddress(String line) {
        return line.replaceAll("portno", "" + getLdapServer().getPort())
            .replaceAll("address", KerberosTestUtils.getHostName());
    }

    @After
    public void tearDown() throws Exception {
        LDAPCache.clear();
        super.tearDown();
    }

    @Test
    @Ignore("KARAF-6823: doesn't work with JDK >= 11.0.8")
    public void testSuccess() throws Exception {

        Properties options = ldapLoginModuleOptions();
        GSSAPILdapLoginModule module = new GSSAPILdapLoginModule();

        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("hnelson", "secret"), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
        assertTrue(module.commit());

        assertEquals(3, subject.getPrincipals().size());

        boolean foundKrb5User = false;
        boolean foundUser = false;
        boolean foundRole = false;
        boolean foundTicket = false;

        for (Principal pr : subject.getPrincipals()) {
            if (pr instanceof KerberosPrincipal) {
                assertEquals("hnelson@EXAMPLE.COM", pr.getName());
                foundKrb5User = true;
            } else if (pr instanceof UserPrincipal) {
                assertEquals("hnelson", pr.getName());
                foundUser = true;
            } else if (pr instanceof RolePrincipal) {
                assertEquals("admin", pr.getName());
                foundRole = true;
            }
        }
        for (Object crd : subject.getPrivateCredentials()) {
            if (crd instanceof KerberosTicket) {
                assertEquals("hnelson@EXAMPLE.COM", ((KerberosTicket) crd).getClient().getName());
                assertEquals("krbtgt/EXAMPLE.COM@EXAMPLE.COM", ((KerberosTicket) crd).getServer().getName());
                foundTicket = true;
                break;
            }
        }

        assertTrue("Principals should contains kerberos user", foundKrb5User);
        assertTrue("Principals should contains ldap user", foundUser);
        assertTrue("Principals should contains ldap role", foundRole);
        assertTrue("PricatePrincipals should contains kerberos ticket", foundTicket);

        assertTrue(module.logout());
        assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
    }

    @Test(expected = LoginException.class)
    @Ignore("KARAF-6823: doesn't work with JDK >= 11.0.8")
    public void testUsernameFailure() throws Exception {

        Properties options = ldapLoginModuleOptions();
        GSSAPILdapLoginModule module = new GSSAPILdapLoginModule();

        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("hnelson0", "secret"), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login()); // should throw LoginException
    }

    @Test(expected = LoginException.class)
    @Ignore("KARAF-6823: doesn't work with JDK >= 11.0.8")
    public void testPasswordFailure() throws Exception {

        Properties options = ldapLoginModuleOptions();
        GSSAPILdapLoginModule module = new GSSAPILdapLoginModule();

        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("hnelson", "secret0"), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login());
    }

    @Test(expected = LoginException.class)
    @Ignore("KARAF-6823: doesn't work with JDK >= 11.0.8")
    public void testUserNotFound() throws Exception {

        Properties options = ldapLoginModuleOptions();
        GSSAPILdapLoginModule module = new GSSAPILdapLoginModule();

        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("test", "test"), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertFalse(module.login());
    }

    @Test(expected = LoginException.class)
    @Ignore("KARAF-6823: doesn't work with JDK >= 11.0.8")
    public void testNoRealm() throws Exception {

        Properties options = ldapLoginModuleOptions();
        options.remove(GSSAPILdapLoginModule.REALM_PROPERTY);
        GSSAPILdapLoginModule module = new GSSAPILdapLoginModule();

        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("hnelson0", "secret"), null, options);

        assertEquals("Precondition", 0, subject.getPrincipals().size());
        assertTrue(module.login()); // should throw LoginException
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
        createPrincipal("uid=" + USER_UID, "Last", "admin",
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

    protected Properties ldapLoginModuleOptions() throws IOException {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }
        File file = new File(basedir + "/target/test-classes/org/apache/karaf/jaas/modules/ldap/gssapi.ldap.properties");
        return new Properties(file);
    }
}
