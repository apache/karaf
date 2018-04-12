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

import java.io.File;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateCrtKey;

import org.apache.sshd.common.config.keys.KeyUtils;
import org.junit.Assert;
import org.junit.Test;

public class OpenSSHGeneratorKeyFileProviderTest {
    
    @Test
    public void writeSshKey() throws Exception {
        File temp = File.createTempFile(this.getClass().getCanonicalName(), ".pem");
        temp.deleteOnExit();

        KeyPair kp = new OpenSSHKeyPairGenerator(KeyUtils.RSA_ALGORITHM, 1024).generate();
        new PemWriter(temp).writeKeyPair(KeyUtils.RSA_ALGORITHM, kp);

        //File path = new File("/home/cschneider/.ssh/id_rsa");
        OpenSSHKeyPairProvider prov = new OpenSSHKeyPairProvider(temp, KeyUtils.RSA_ALGORITHM, 1024);
        KeyPair keys = prov.loadKeys().iterator().next();
        Assert.assertNotNull(keys);
        Assert.assertTrue("Loaded key is not RSA Key", keys.getPrivate() instanceof RSAPrivateCrtKey);
    }
    
}
