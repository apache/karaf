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

import static java.util.Collections.singleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import org.apache.commons.ssl.PKCS8Key;
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;

public class OpenSSHKeyPairProvider extends AbstractKeyPairProvider {
    private File keyFile;
    private String password;

    public OpenSSHKeyPairProvider(File keyFile) {
        this.keyFile = keyFile;
    }

    @Override
    public Iterable<KeyPair> loadKeys() {
        try (FileInputStream is = new FileInputStream(keyFile)) {
            KeyPair kp = getKeyPair(is);
            return singleton(kp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private KeyPair getKeyPair(FileInputStream is) throws GeneralSecurityException, IOException {
        PKCS8Key pkcs8 = new PKCS8Key(is, password == null ? null : password.toCharArray());
        KeyPair kp = new KeyPair(pkcs8.getPublicKey(), pkcs8.getPrivateKey());
        return kp;
    }
    
}
