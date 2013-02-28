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
package org.apache.karaf.jaas.config.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URL;
import java.net.URI;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.karaf.jaas.config.KeystoreInstance;
import org.apache.karaf.jaas.config.KeystoreIsLocked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ResourceKeystoreInstance implements KeystoreInstance {

    private final Logger logger = LoggerFactory.getLogger(ResourceKeystoreInstance.class);

    private static final String JKS = "JKS";

    private String name;
    private int rank;
    private URL path;
    private String keystorePassword;
    private Map keyPasswords = new HashMap();
    private File keystoreFile; // Only valid after startup and if the resource points to a file

    // The following variables are the state of the keystore, which should be chucked if the file on disk changes
    private List privateKeys = new ArrayList();
    private List trustCerts = new ArrayList();
    private KeyStore keystore;
    private long keystoreReadDate = Long.MIN_VALUE;

    /**
     * @return the keystoreName
     */
    public String getName() {
        return name;
    }

    /**
     * @param keystoreName the keystoreName to set
     */
    public void setName(String keystoreName) {
        this.name = keystoreName;
    }

    /**
     * @return the rank
     */
    public int getRank() {
        return rank;
    }

    /**
     * @param rank the rank to set
     */
    public void setRank(int rank) {
        this.rank = rank;
    }

    /**
     * @return the keystorePath
     */
    public URL getPath() {
        return path;
    }

    /**
     * @param keystorePath the keystorePath to set
     */
    public void setPath(URL keystorePath) throws IOException {
        this.path = keystorePath;
        if (keystorePath.getProtocol().equals("file")) {
            URI uri = URI.create(keystorePath.toString().replace(" ", "%20"));
            this.keystoreFile = new File(uri.getSchemeSpecificPart());
        }
    }

    /**
     * @param keystorePassword the keystorePassword to set
     */
    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    /**
     * @param keyPasswords the keyPasswords to set
     */
    public void setKeyPasswords(String keyPasswords) {
        if (keyPasswords != null) {
            String[] keys = keyPasswords.split("\\]\\!\\[");
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                int pos = key.indexOf('=');
                this.keyPasswords.put(key.substring(0, pos), key.substring(pos + 1).toCharArray());
            }
        }
    }

    public Certificate getCertificate(String alias) {
        if (!loadKeystoreData()) {
            return null;
        }
        try {
            return keystore.getCertificate(alias);
        } catch (KeyStoreException e) {
            logger.error("Unable to read certificate from keystore", e);
        }
        return null;
    }

    public String getCertificateAlias(Certificate cert) {
        if (!loadKeystoreData()) {
            return null;
        }
        try {
            return keystore.getCertificateAlias(cert);
        } catch (KeyStoreException e) {
            logger.error("Unable to read retrieve alias for given certificate from keystore", e);
        }
        return null;
    }

    public Certificate[] getCertificateChain(String alias) {
        if (!loadKeystoreData()) {
            return null;
        }
        try {
            return keystore.getCertificateChain(alias);
        } catch (KeyStoreException e) {
            logger.error("Unable to read certificate chain from keystore", e);
        }
        return null;
    }

    public KeyManager[] getKeyManager(String algorithm, String keyAlias) throws KeystoreIsLocked,
                                    NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        if (isKeystoreLocked()) {
            throw new KeystoreIsLocked("Keystore '" + name + "' is locked.");
        }
        if (!loadKeystoreData()) {
            return null;
        }
        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(algorithm);
        keyFactory.init(keystore, (char[]) keyPasswords.get(keyAlias));
        return keyFactory.getKeyManagers();
    }

    public PrivateKey getPrivateKey(String alias) {
        if (!loadKeystoreData()) {
            return null;
        }
        try {
            if (isKeyLocked(alias)) {
                return null;
            }
            Key key = keystore.getKey(alias, (char[]) keyPasswords.get(alias));
            if (key instanceof PrivateKey) {
                return (PrivateKey) key;
            }
        } catch (KeyStoreException e) {
            logger.error("Unable to read private key from keystore", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unable to read private key from keystore", e);
        } catch (UnrecoverableKeyException e) {
            logger.error("Unable to read private key from keystore", e);
        }
        return null;
    }

    public TrustManager[] getTrustManager(String algorithm) throws KeyStoreException,
                                            NoSuchAlgorithmException, KeystoreIsLocked {
        if (isKeystoreLocked()) {
            throw new KeystoreIsLocked("Keystore '" + name + "' is locked.");
        }
        if (!loadKeystoreData()) {
            return null;
        }
        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(algorithm);
        trustFactory.init(keystore);
        return trustFactory.getTrustManagers();
    }

    public boolean isKeyLocked(String keyAlias) {
        return keyPasswords.get(keyAlias) == null;
    }

    public boolean isKeystoreLocked() {
        return keystorePassword == null;
    }

    public String[] listPrivateKeys() {
        if (!loadKeystoreData()) {
            return null;
        }
        return (String[]) privateKeys.toArray(new String[privateKeys.size()]);
    }

    public String[] listTrustCertificates() {
        if (!loadKeystoreData()) {
            return null;
        }
        return (String[]) trustCerts.toArray(new String[trustCerts.size()]);
    }

    // ==================== Internals =====================

    private boolean loadKeystoreData() {
        // Check to reload the data if needed
        if (keystoreFile != null && keystoreReadDate >= keystoreFile.lastModified()) {
            return true;
        }
        // If not a file, just not reload the data if it has already been loaded
        if (keystoreFile == null && keystore != null) {
            return true;
        }
        // Check if the file is invalid
        if (keystoreFile != null && (!keystoreFile.exists() || !keystoreFile.canRead())) {
            throw new IllegalArgumentException("Invalid keystore file (" + path + " = " + keystoreFile.getAbsolutePath() + ")");
        }
        // Load the keystore data
        try {
            keystoreReadDate = System.currentTimeMillis();
            privateKeys.clear();
            trustCerts.clear();
            if (keystore == null) {
                keystore = KeyStore.getInstance(JKS);
            }
            InputStream in = new BufferedInputStream(path.openStream());
            keystore.load(in, keystorePassword == null ? new char[0] : keystorePassword.toCharArray());
            in.close();
            Enumeration aliases = keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                if (keystore.isKeyEntry(alias)) {
                    privateKeys.add(alias);
                } else if (keystore.isCertificateEntry(alias)) {
                    trustCerts.add(alias);
                }
            }
            return true;
        } catch (KeyStoreException e) {
            logger.error("Unable to open keystore with provided password", e);
        } catch (IOException e) {
            logger.error("Unable to open keystore with provided password", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unable to open keystore with provided password", e);
        } catch (CertificateException e) {
            logger.error("Unable to open keystore with provided password", e);
        }
        return false;
    }

}
