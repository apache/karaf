/*
 *
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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Hashtable;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith ( FrameworkRunner.class )
@CreateLdapServer(transports = {@CreateTransport(protocol = "LDAPS", ssl = true)},
 keyStore = "src/test/resources/org/apache/karaf/jaas/modules/ldap/ldaps.jks", certificatePassword = "123456")
@CreateDS(name = "LdapPoolingTest-class",
 partitions = { @CreatePartition(name = "example", suffix = "dc=example,dc=com") })
@ApplyLdifFiles(
   "org/apache/karaf/jaas/modules/ldap/example.com.ldif"
)
public class LdapPoolingTest extends AbstractLdapTestUnit {

    private SSLContext sslContext;

    @Before
    public void keystore() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(new FileInputStream("src/test/resources/org/apache/karaf/jaas/modules/ldap/ldaps.jks"), "123456".toCharArray());
        kmf.init(ks, "123456".toCharArray());
        tmf.init(ks);

        sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
    }

    /**
     * @see <a href="http://docs.oracle.com/javase/jndi/tutorial/ldap/connect/config.html">LDAP connection pool</a>
     * @throws Exception
     */
    @Test
    public void testSSLConnectionPool() throws Exception {
        System.setProperty("com.sun.jndi.ldap.connect.pool.maxsize", "2");
        System.setProperty("com.sun.jndi.ldap.connect.pool.protocol", "ssl");
        System.setProperty("com.sun.jndi.ldap.connect.pool.debug", "all");
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("java.naming.provider.url", "ldaps://localhost:" + getLdapServer().getPortSSL() + "/ou=system");
        env.put("java.naming.ldap.factory.socket", ManagedSSLSocketFactory.class.getName());
        env.put("java.naming.security.protocol", "ssl");
        env.put("java.naming.security.principal", "uid=admin,ou=system");
        env.put("java.naming.security.credentials", "secret");
        env.put("java.naming.security.authentication", "simple");

        final int[] socketsCreated = new int[] { 0 };
        ManagedSSLSocketFactory.setSocketFactory(new ManagedSSLSocketFactory(sslContext.getSocketFactory()) {
            @Override
            public Socket createSocket(String host, int port) throws IOException {
                socketsCreated[0]++;
                return super.createSocket(host, port);
            }
        });
        InitialDirContext context = new InitialDirContext(env);
        context.close();
        new InitialDirContext(env);
        context.close();
        ManagedSSLSocketFactory.setSocketFactory(null);

        assertThat(socketsCreated[0], equalTo(1));
    }

    @Test
    public void testSSLConnectionWithoutPool() throws Exception {
        System.setProperty("com.sun.jndi.ldap.connect.pool.maxsize", "2");
        System.setProperty("com.sun.jndi.ldap.connect.pool.protocol", "ssl");
        System.setProperty("com.sun.jndi.ldap.connect.pool.debug", "all");
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("com.sun.jndi.ldap.connect.pool", "false");
        env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("java.naming.provider.url", "ldaps://localhost:" + getLdapServer().getPortSSL() + "/ou=system");
        env.put("java.naming.ldap.factory.socket", ManagedSSLSocketFactory.class.getName());
        env.put("java.naming.security.protocol", "ssl");
        env.put("java.naming.security.principal", "uid=admin,ou=system");
        env.put("java.naming.security.credentials", "secret");
        env.put("java.naming.security.authentication", "simple");

        final int[] socketsCreated = new int[] { 0 };
        ManagedSSLSocketFactory.setSocketFactory(new ManagedSSLSocketFactory(sslContext.getSocketFactory()) {
            @Override
            public Socket createSocket(String host, int port) throws IOException {
                socketsCreated[0]++;
                return super.createSocket(host, port);
            }
        });
        InitialDirContext context = new InitialDirContext(env);
        context.close();
        new InitialDirContext(env);
        context.close();
        ManagedSSLSocketFactory.setSocketFactory(null);

        assertThat(socketsCreated[0], equalTo(2));
    }

}
