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

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.apache.sshd.common.session.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSSHKeyPairProvider extends AbstractKeyPairProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSSHKeyPairProvider.class);
    private Path privateKeyPath;
    private Path publicKeyPath;
    private String password;
    private KeyPair cachedKey;
    private String algorithm;
    private int keySize;

    public OpenSSHKeyPairProvider(Path privateKeyPath, Path publicKeyPath, String algorithm, int keySize, String password) {
        this.privateKeyPath = privateKeyPath;
        this.publicKeyPath = publicKeyPath;
        this.algorithm = algorithm;
        this.keySize = keySize;
        this.password = password;
    }

    @Override
    public synchronized Iterable<KeyPair> loadKeys(SessionContext sessionContext) throws IOException, GeneralSecurityException {
        if (cachedKey != null) {
            return singleton(cachedKey);
        }
        if (!privateKeyPath.toFile().exists()) {
            createServerKey();
        }

        // 1. Try to read the PKCS8 private key. If it is RSA or DSA we can infer the public key directly from the
        // private key, so there is no need to load the public key.
        try (InputStream is = Files.newInputStream(privateKeyPath)) {
            KeyPair kp = KeyPairLoader.getKeyPair(is, password);
            cachedKey = kp;
            return singleton(kp);
        } catch (Exception e) {
            // 2. Failed to parse PKCS8 private key. Try to parse it directly and use the public key to create a KeyPair
            // This is what will happen if it is an elliptic curve key for example
            LOGGER.warn("Failed to parse keypair in {}. Attempting to parse it 'directly'", privateKeyPath);
            try {
                KeyPair kp = getKeyPairUsingPublicKeyFile();
                LOGGER.info("Successfully loaded key pair");
                cachedKey = kp;
                return singleton(cachedKey);
            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e1) {
                // 3. On a failure, see if we are dealing with a "legacy" keypair.
                LOGGER.warn("Failed to parse keypair in {}. Attempting to parse it as a legacy 'simple' key", privateKeyPath);
                try {
                    KeyPair kp = convertLegacyKey(privateKeyPath);
                    LOGGER.info("Successfully loaded legacy simple key. Converted to PEM format");
                    cachedKey = kp;
                    return singleton(kp);
                } catch (Exception nested) {
                    LOGGER.warn(privateKeyPath + " is not a 'simple' key either", nested);
                }
            }
            throw new RuntimeException(e);
        }
    }

    private KeyPair convertLegacyKey(Path privateKeyPath) throws GeneralSecurityException, IOException {
        KeyPair keypair = null;
        try (ObjectInputStream r = new KeyPairObjectInputStream(Files.newInputStream(privateKeyPath))) {
            keypair = (KeyPair)r.readObject();
        }
        catch (ClassNotFoundException e) {
            throw new InvalidKeySpecException("Missing classes: " + e.getMessage(), e);
        }
        new PemWriter(privateKeyPath, publicKeyPath).writeKeyPair(algorithm, keypair);
        return keypair;
    }

    private KeyPair getKeyPairUsingPublicKeyFile() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

        // Read private key
        String content = new String(Files.readAllBytes(privateKeyPath), StandardCharsets.UTF_8);
        content = content.replace("-----BEGIN PRIVATE KEY-----", "");
        content = content.replace("-----END PRIVATE KEY-----", "");

        PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(content));
        PrivateKey privateKey = keyFactory.generatePrivate(encodedKeySpec);

        // Read public key
        content = new String(Files.readAllBytes(publicKeyPath), StandardCharsets.UTF_8);
        content = content.replace("-----BEGIN PUBLIC KEY-----", "");
        content = content.replace("-----END PUBLIC KEY-----", "");

        X509EncodedKeySpec encodedX509KeySpec = new X509EncodedKeySpec(Base64.getMimeDecoder().decode(content));
        PublicKey publicKey = keyFactory.generatePublic(encodedX509KeySpec);

        return new KeyPair(publicKey, privateKey);
    }

    private KeyPair createServerKey() {
        try {
            LOGGER.info("Creating ssh server private key at " + privateKeyPath);
            KeyPair kp = new OpenSSHKeyPairGenerator(algorithm, keySize).generate();
            new PemWriter(privateKeyPath, publicKeyPath).writeKeyPair(algorithm, kp);
            LOGGER.debug("Changing key files permissions");
            Set<PosixFilePermission> permissions = new HashSet<>();
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
            try {
                Files.setPosixFilePermissions(privateKeyPath, permissions);
                Files.setPosixFilePermissions(publicKeyPath, permissions);
            } catch (Exception e) {
                LOGGER.debug("Can't change file permissions", e);
            }
            return kp;
        } catch (Exception e) {
            throw new RuntimeException("Key file generation failed", e);
        }
    }

    /**
     * Check the first Object that is resolved is a KeyPair instance
     */
    private static class KeyPairObjectInputStream extends ObjectInputStream {

        private boolean valid;

        public KeyPairObjectInputStream(InputStream is) throws IOException {
            super(is);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            if (!valid) {
                if (!desc.getName().equals(KeyPair.class.getName())) {
                    throw new InvalidClassException("Unauthorized deserialization attempt", desc.getName());
                }
                valid = true;
            }
            return super.resolveClass(desc);
        }
    }

}
