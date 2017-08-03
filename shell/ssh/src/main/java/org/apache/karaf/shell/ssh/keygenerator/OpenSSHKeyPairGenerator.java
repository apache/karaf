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

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.apache.sshd.common.cipher.ECCurves;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSSHKeyPairGenerator {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private int keySize;
    private String algorithm;
    
    public OpenSSHKeyPairGenerator(String algorithm, int keySize) {
        this.algorithm = algorithm;
        this.keySize = keySize;
    }

    public KeyPair generate() throws GeneralSecurityException {
        KeyPairGenerator generator = SecurityUtils.getKeyPairGenerator(algorithm);
        if (keySize != 0) {
            generator.initialize(keySize);
            log.info("generateKeyPair(" + algorithm + ") generating host key - size=" + keySize);
        } else if (KeyUtils.EC_ALGORITHM.equals(algorithm)) {
            // If left to our own devices choose the biggest key size possible
            int numCurves = ECCurves.SORTED_KEY_SIZE.size();
            ECCurves curve = ECCurves.SORTED_KEY_SIZE.get(numCurves - 1);
            generator.initialize(curve.getParameters());
        }
        return generator.generateKeyPair();
    }
}
