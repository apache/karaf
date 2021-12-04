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

import java.io.IOException;
import java.util.List;
import org.apache.sshd.common.cipher.Cipher;
import org.apache.sshd.common.kex.KeyExchange;
import org.apache.sshd.common.kex.KeyExchangeFactory;
import org.apache.sshd.common.mac.Mac;
import org.apache.sshd.common.NamedFactory;

import org.junit.Assert;
import org.junit.Test;

public class SshUtilsTest {
   
    @Test
    public void testCiphersDefault() throws IOException {
        // verify our default configuration...
        String ciphers = "aes128-ctr,aes128-cbc";

        List<NamedFactory<Cipher>> list = SshUtils.buildCiphers(ciphers.split(","));

        // verify that all configured ciphers are actually resolved...
        for (String cipher : ciphers.split(",")) {
            boolean found = false;
            for (NamedFactory<Cipher> factory : list) {
                if (factory.getName().equalsIgnoreCase(cipher)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Assert.fail("Configured default cipher '" + cipher + "' cannot be resolved");
            }
        }
    }

    @Test
    public void testMacsDefault() throws IOException {
        // verify our default configuration...
        String macs = "hmac-sha2-512,hmac-sha2-256,hmac-sha1";

        List<NamedFactory<Mac>> list = SshUtils.buildMacs(macs.split(","));

        // verify that all configured HMACs are actually resolved...
        for (String mac : macs.split(",")) {
            boolean found = false;
            for (NamedFactory<Mac> factory : list) {
                if (factory.getName().equalsIgnoreCase(mac)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Assert.fail("Configured default HMAC '" + mac + "' cannot be resolved");
            }
        }
    }

    @Test
    public void testKexAlgorithmsDefault() throws IOException {
        // verify our default configuration...
        String kexAlgorithms = "diffie-hellman-group-exchange-sha256,ecdh-sha2-nistp521,ecdh-sha2-nistp384,ecdh-sha2-nistp256";

        List<KeyExchangeFactory> list = SshUtils.buildKexAlgorithms(kexAlgorithms.split(","));

        // verify that all configured key exchange algorithms are actually resolved...
        for (String kex : kexAlgorithms.split(",")) {
            boolean found = false;
            for (KeyExchangeFactory factory : list) {
                if (factory.getName().equalsIgnoreCase(kex)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Assert.fail("Configured default key exchange algorithm '" + kex + "' cannot be resolved");
            }
        }
    }
}

