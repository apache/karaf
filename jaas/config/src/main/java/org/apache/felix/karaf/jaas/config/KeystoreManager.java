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

import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

/**
 * Based on http://svn.apache.org/repos/asf/geronimo/trunk/modules/management/
 *                      src/java/org/apache/geronimo/management/geronimo/KeystoreManager.java
 *
 */
public interface KeystoreManager {

    KeystoreInstance getKeystore(String name);

    /**
     * Gets a SSLContext using one Keystore to access the private key
     * and another to provide the list of trusted certificate authorities.
     * @param provider
     * @param protocol The SSL protocol to use
     * @param algorithm The SSL algorithm to use
     * @param keyStore The key keystore name as provided by listKeystores.  The
     *                 KeystoreInstance for this keystore must be unlocked.
     * @param keyAlias The name of the private key in the keystore.  The
     *                 KeystoreInstance for this keystore must have unlocked
     *                 this key.
     * @param trustStore The trust keystore name as provided by listKeystores.
     *                   The KeystoreInstance for this keystore must have
     *                   unlocked this key.
     *
     * @throws KeystoreIsLocked Occurs when the requested key keystore cannot
     *                          be used because it has not been unlocked.
     * @throws KeyIsLocked Occurs when the requested private key in the key
     *                     keystore cannot be used because it has not been
     *                     unlocked.
     */
    SSLContext createSSLContext(String provider, String protocol,
                                String algorithm, String keyStore,
                                String keyAlias, String trustStore) throws GeneralSecurityException;

    /**
     * Gets a ServerSocketFactory using one Keystore to access the private key
     * and another to provide the list of trusted certificate authorities.
     * @param provider
     * @param protocol The SSL protocol to use
     * @param algorithm The SSL algorithm to use
     * @param keyStore The key keystore name as provided by listKeystores.  The
     *                 KeystoreInstance for this keystore must be unlocked.
     * @param keyAlias The name of the private key in the keystore.  The
     *                 KeystoreInstance for this keystore must have unlocked
     *                 this key.
     * @param trustStore The trust keystore name as provided by listKeystores.
     *                   The KeystoreInstance for this keystore must have
     *                   unlocked this key.
     *
     * @throws KeystoreIsLocked Occurs when the requested key keystore cannot
     *                          be used because it has not been unlocked.
     * @throws KeyIsLocked Occurs when the requested private key in the key
     *                     keystore cannot be used because it has not been
     *                     unlocked.
     */
    SSLServerSocketFactory createSSLServerFactory(String provider, String protocol,
                                                  String algorithm, String keyStore,
                                                  String keyAlias, String trustStore) throws GeneralSecurityException;

    /**
     * Gets a SocketFactory using one Keystore to access the private key
     * and another to provide the list of trusted certificate authorities.
     * @param provider The SSL provider to use, or null for the default
     * @param protocol The SSL protocol to use
     * @param algorithm The SSL algorithm to use
     * @param keyStore The key keystore name as provided by listKeystores.  The
     *                 KeystoreInstance for this keystore must be unlocked.
     * @param keyAlias The name of the private key in the keystore.  The
     *                 KeystoreInstance for this keystore must have unlocked
     *                 this key.
     * @param trustStore The trust keystore name as provided by listKeystores.
     *                   The KeystoreInstance for this keystore must have
     *                   unlocked this key.
     *
     * @throws KeystoreIsLocked Occurs when the requested key keystore cannot
     *                          be used because it has not been unlocked.
     * @throws KeyIsLocked Occurs when the requested private key in the key
     *                     keystore cannot be used because it has not been
     *                     unlocked.
     * @throws GeneralSecurityException
     */
    SSLSocketFactory createSSLFactory(String provider, String protocol,
                                      String algorithm, String keyStore,
                                      String keyAlias, String trustStore) throws GeneralSecurityException;

}
