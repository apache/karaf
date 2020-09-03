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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class to convert a private key file into a KeyPair object
 */
public final class KeyPairLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyPairLoader.class);

    private KeyPairLoader() {
        // complete
    }

    public static KeyPair getKeyPair(InputStream is) throws GeneralSecurityException, IOException {
        return getKeyPair(is, null);
    }

    public static KeyPair getKeyPair(InputStream is, String password) throws GeneralSecurityException, IOException {
        try (PEMParser parser = new PEMParser(new InputStreamReader(is))) {
            Object o = parser.readObject();

            JcaPEMKeyConverter pemConverter = new JcaPEMKeyConverter();

            if (o instanceof PEMEncryptedKeyPair) {
                if (password == null) {
                    throw new GeneralSecurityException("A password must be supplied to read an encrypted key pair");
                }
                JcePEMDecryptorProviderBuilder decryptorBuilder = new JcePEMDecryptorProviderBuilder();
                PEMDecryptorProvider pemDecryptor = decryptorBuilder.build(password.toCharArray());
                o = pemConverter.getKeyPair(((PEMEncryptedKeyPair) o).decryptKeyPair(pemDecryptor));
            } else if (o instanceof PKCS8EncryptedPrivateKeyInfo) {
                if (password == null) {
                    throw new GeneralSecurityException("A password must be supplied to read an encrypted key pair");
                }

                JceOpenSSLPKCS8DecryptorProviderBuilder jce = new JceOpenSSLPKCS8DecryptorProviderBuilder();
                try {
                    InputDecryptorProvider decProv = jce.build(password.toCharArray());
                    o = ((PKCS8EncryptedPrivateKeyInfo)o).decryptPrivateKeyInfo(decProv);
                } catch (OperatorCreationException | PKCSException ex) {
                    LOGGER.debug("Error decrypting key pair", ex);
                    throw new GeneralSecurityException("Error decrypting key pair", ex);
                }
            }

            if (o instanceof PEMKeyPair) {
                return pemConverter.getKeyPair((PEMKeyPair)o);
            } else if (o instanceof KeyPair) {
                return (KeyPair) o;
            } else if (o instanceof PrivateKeyInfo) {
                PrivateKey privateKey = pemConverter.getPrivateKey((PrivateKeyInfo)o);
                PublicKey publicKey = convertPrivateToPublicKey(privateKey);
                if (publicKey != null) {
                    return new KeyPair(publicKey, privateKey);
                }
            }
        }

        throw new GeneralSecurityException("Failed to parse input stream");
    }

    private static PublicKey convertPrivateToPublicKey(PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (privateKey instanceof RSAPrivateCrtKey) {
            KeySpec keySpec = new RSAPublicKeySpec(((RSAPrivateCrtKey)privateKey).getModulus(),
                                                            ((RSAPrivateCrtKey)privateKey).getPublicExponent());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } else if (privateKey instanceof ECPrivateKey) {
            ECPrivateKey ecPrivateKey = (ECPrivateKey)privateKey;

            // Derive the public point by multiplying the generator by the private value
            ECParameterSpec paramSpec = EC5Util.convertSpec(ecPrivateKey.getParams());
            ECPoint q = paramSpec.getG().multiply(ecPrivateKey.getS());

            KeySpec keySpec = new ECPublicKeySpec(q, paramSpec);

            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePublic(keySpec);
        } else if (privateKey instanceof DSAPrivateKey) {
            DSAPrivateKey dsaPrivateKey = (DSAPrivateKey)privateKey;

            BigInteger q = dsaPrivateKey.getParams().getQ();
            BigInteger p = dsaPrivateKey.getParams().getP();
            KeySpec keySpec = new DSAPublicKeySpec(q.modPow(dsaPrivateKey.getX(), p),
                                                   p,
                                                   q,
                                                   dsaPrivateKey.getParams().getG());

            KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            return keyFactory.generatePublic(keySpec);
        } else {
            LOGGER.warn("Unable to convert private key to public key. Only RSA, DSA + ECDSA supported");
            return null;
        }
    }

}
