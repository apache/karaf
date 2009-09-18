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
package org.apache.felix.karaf.jaas.config.impl;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.apache.felix.karaf.jaas.config.KeystoreInstance;
import org.apache.felix.karaf.jaas.config.KeystoreIsLocked;
import org.apache.felix.karaf.jaas.config.KeystoreManager;

/**
 * Implementation of KeystoreManager
 */
public class OsgiKeystoreManager implements KeystoreManager {

    private List<KeystoreInstance> keystores = new CopyOnWriteArrayList<KeystoreInstance>();

    public void register(KeystoreInstance keystore, Map<String,?> properties) {
        keystores.add(keystore);
    }

    public void unregister(KeystoreInstance keystore, Map<String,?> properties) {
        keystores.remove(keystore);
    }

    public KeystoreInstance getKeystore(String name) {
        KeystoreInstance keystore = null;
        for (KeystoreInstance ks : keystores) {
            if (ks.getName().equals(name)) {
                if (keystore == null || keystore.getRank() < ks.getRank()) {
                    keystore = ks;
                }
            }
        }
        return keystore;
    }

    public SSLContext createSSLContext(String provider, String protocol, String algorithm, String keyStore, String keyAlias, String trustStore) throws GeneralSecurityException {
        KeystoreInstance keyInstance = getKeystore(keyStore);
        if (keyInstance != null && keyInstance.isKeystoreLocked()) {
            throw new KeystoreIsLocked("Keystore '" + keyStore + "' is locked");
        }
        if (keyInstance != null && keyInstance.isKeyLocked(keyAlias)) {
            throw new KeystoreIsLocked("Key '" + keyAlias + "' in keystore '" + keyStore + "' is locked");
        }
        KeystoreInstance trustInstance = trustStore == null ? null : getKeystore(trustStore);
        if (trustInstance != null && trustInstance.isKeystoreLocked()) {
            throw new KeystoreIsLocked("Keystore '" + trustStore + "' is locked");
        }
        SSLContext context;
        if (provider == null) {
            context = SSLContext.getInstance(protocol);
        } else {
            context = SSLContext.getInstance(protocol, provider);
        }
        context.init(keyInstance == null ? null : keyInstance.getKeyManager(algorithm, keyAlias),
                     trustInstance == null ? null : trustInstance.getTrustManager(algorithm), new SecureRandom());
        return context;
    }

    public SSLServerSocketFactory createSSLServerFactory(String provider, String protocol, String algorithm, String keyStore, String keyAlias, String trustStore) throws GeneralSecurityException {
        SSLContext context = createSSLContext(provider, protocol, algorithm, keyStore, keyAlias, trustStore);
        return context.getServerSocketFactory();
    }

    public SSLSocketFactory createSSLFactory(String provider, String protocol, String algorithm, String keyStore, String keyAlias, String trustStore) throws GeneralSecurityException {
        SSLContext context = createSSLContext(provider, protocol, algorithm, keyStore, keyAlias, trustStore);
        return context.getSocketFactory();
    }
}
