/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.karaf.jaas.config;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

/**
 * Based on http://svn.apache.org/repos/asf/geronimo/trunk/modules/management/
 *              src/java/org/apache/geronimo/management/geronimo/KeystoreInstance.java
 *
 * @version $Rev: $ $Date: $
 */
public interface KeystoreInstance {

    String getName();

    int getRank();

    String[] listPrivateKeys();

    String[] listTrustCertificates();

    Certificate getCertificate(String alias);

    String getCertificateAlias(Certificate cert);

    Certificate[] getCertificateChain(String alias);

    PrivateKey getPrivateKey(String alias);

    boolean isKeystoreLocked();

    boolean isKeyLocked(String keyAlias);

    KeyManager[] getKeyManager(String algorithm, String keyAlias) throws NoSuchAlgorithmException,
                                UnrecoverableKeyException, KeyStoreException, KeystoreIsLocked;

    TrustManager[] getTrustManager(String algorithm) throws KeyStoreException, NoSuchAlgorithmException, KeystoreIsLocked;

}
