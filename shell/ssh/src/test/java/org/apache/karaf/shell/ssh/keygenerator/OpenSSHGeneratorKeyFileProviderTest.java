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
package org.apache.karaf.shell.ssh.keygenerator;

import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.Assert;
import org.junit.Test;

public class OpenSSHGeneratorKeyFileProviderTest {

    @Test
    public void writeSshKey() throws Exception {
        File privateKeyTemp = File.createTempFile(this.getClass().getCanonicalName(), ".priv");
        privateKeyTemp.deleteOnExit();
        File publicKeyTemp = File.createTempFile(this.getClass().getCanonicalName(), ".pub");
        publicKeyTemp.deleteOnExit();

        KeyPair kp = new OpenSSHKeyPairGenerator(KeyUtils.RSA_ALGORITHM, 1024).generate();
        new PemWriter(privateKeyTemp.toPath(), publicKeyTemp.toPath()).writeKeyPair(KeyUtils.RSA_ALGORITHM, kp);

        OpenSSHKeyPairProvider prov =
            new OpenSSHKeyPairProvider(privateKeyTemp.toPath(), publicKeyTemp.toPath(), KeyUtils.RSA_ALGORITHM, 1024, null);
        KeyPair keys = prov.loadKeys(null).iterator().next();
        Assert.assertNotNull(keys);
        Assert.assertTrue("Loaded key is not RSA Key", keys.getPrivate() instanceof RSAPrivateCrtKey);
        Assert.assertTrue("Loaded key is not RSA Key", keys.getPublic() instanceof RSAPublicKey);
    }

    @Test
    public void convertSimpleKey() throws Exception {
        File privateKeyTemp = File.createTempFile(this.getClass().getCanonicalName(), ".priv");
        privateKeyTemp.deleteOnExit();
        File publicKeyTemp = File.createTempFile(this.getClass().getCanonicalName(), ".pub");
        publicKeyTemp.deleteOnExit();

        SimpleGeneratorHostKeyProvider simpleGenerator = new SimpleGeneratorHostKeyProvider(privateKeyTemp.toPath());
        simpleGenerator.setKeySize(2048);
        simpleGenerator.setAlgorithm("DSA");
        List<KeyPair> keys = simpleGenerator.loadKeys(null);
        KeyPair simpleKeyPair = keys.stream().findFirst().get();

        Assert.assertEquals("DSA", simpleKeyPair.getPrivate().getAlgorithm());

        OpenSSHKeyPairProvider provider =
            new OpenSSHKeyPairProvider(privateKeyTemp.toPath(), publicKeyTemp.toPath(), "DSA", 2048, null);
        KeyPair convertedKeyPair = provider.loadKeys(null).iterator().next();
        Assert.assertEquals("DSA", convertedKeyPair.getPrivate().getAlgorithm());

        Assert.assertArrayEquals(simpleKeyPair.getPrivate().getEncoded(),convertedKeyPair.getPrivate().getEncoded());
        Assert.assertArrayEquals(simpleKeyPair.getPublic().getEncoded(),convertedKeyPair.getPublic().getEncoded());

        //also test that the original file has been replaced
        KeyPair keyPair = KeyPairLoader.getKeyPair(Files.newInputStream(privateKeyTemp.toPath()));
        Assert.assertArrayEquals(simpleKeyPair.getPrivate().getEncoded(),keyPair.getPrivate().getEncoded());
    }

    @Test
    public void writeECSshKey() throws Exception {
        File privateKeyTemp = File.createTempFile(this.getClass().getCanonicalName(), ".priv");
        privateKeyTemp.deleteOnExit();
        File publicKeyTemp = File.createTempFile(this.getClass().getCanonicalName(), ".pub");
        publicKeyTemp.deleteOnExit();

        KeyPair kp = new OpenSSHKeyPairGenerator(KeyUtils.EC_ALGORITHM, 256).generate();
        new PemWriter(privateKeyTemp.toPath(), publicKeyTemp.toPath()).writeKeyPair(KeyUtils.EC_ALGORITHM, kp);

        OpenSSHKeyPairProvider prov =
            new OpenSSHKeyPairProvider(privateKeyTemp.toPath(), publicKeyTemp.toPath(), KeyUtils.EC_ALGORITHM, 256, null);
        KeyPair keys = prov.loadKeys(null).iterator().next();
        Assert.assertNotNull(keys);
        Assert.assertTrue("Loaded key is not EC Key", keys.getPrivate() instanceof ECPrivateKey);
        Assert.assertTrue("Loaded key is not EC Key", keys.getPublic() instanceof ECPublicKey);
    }

    @Test
    public void loadEncryptedPrivateKey() throws Exception {
        Path privateKeyPath = Paths.get(this.getClass().getResource("../rsa.pem").toURI());

        // First we try to load without specifying a password...
        OpenSSHKeyPairProvider prov =
            new OpenSSHKeyPairProvider(privateKeyPath, null, KeyUtils.RSA_ALGORITHM, 1024, null);
        try {
            prov.loadKeys(null);
            fail("Failure expected on a decryption failure");
        } catch (Exception ex) {
            // expected
        }

        // Now we provide the wrong password
        prov = new OpenSSHKeyPairProvider(privateKeyPath, null, KeyUtils.RSA_ALGORITHM, 1024, "password");
        try {
            prov.loadKeys(null);
            fail("Failure expected on a decryption failure");
        } catch (Exception ex) {
            // expected
        }

        // Now it should work
        prov = new OpenSSHKeyPairProvider(privateKeyPath, null, KeyUtils.RSA_ALGORITHM, 1024, "security");
        KeyPair keys = prov.loadKeys(null).iterator().next();
        Assert.assertNotNull(keys);
        Assert.assertTrue("Loaded key is not RSA Key", keys.getPrivate() instanceof RSAPrivateCrtKey);
        Assert.assertTrue("Loaded key is not RSA Key", keys.getPublic() instanceof RSAPublicKey);
    }

}
