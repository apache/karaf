package org.apache.karaf.shell.ssh;

import org.junit.Test;

import java.security.KeyPair;

public class OpenSSHGeneratorKeyFileProviderTest {

    @Test
    public void readSshKey() throws Exception {
        OpenSSHGeneratorFileKeyProvider prov = new OpenSSHGeneratorFileKeyProvider("src/test/resources/org/apache/karaf/shell/ssh/test.pem");
        prov.setOverwriteAllowed(false);
        KeyPair keys = prov.loadKeys().iterator().next();
        // how would we tell if they read 'correctly'? Well, the base class will throw if the key isn't reasonable.
    }
}
