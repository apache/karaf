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

import org.apache.mina.util.Base64;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;

/**
 * A public key authenticator, which reads an OpenSSL2 <code>authorized_keys</code> file.
 */
public class KarafPublickeyAuthenticator implements PublickeyAuthenticator {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(KarafPublickeyAuthenticator.class);

    private String authorizedKeys;
    private boolean active;

    public static final class AuthorizedKey {

        private final String alias;
        private final String format;
        private final PublicKey publicKey;

        public AuthorizedKey(String alias, String format, PublicKey publicKey) {
            super();
            this.alias = alias;
            this.format = format;
            this.publicKey = publicKey;
        }

        public String getAlias() {
            return this.alias;
        }
        
        public String getFormat() {
            return this.format;
        }

        public PublicKey getPublicKey() {
            return this.publicKey;
        }

    }

    private static final class PublicKeyComparator implements Comparator<PublicKey> {

        public int compare(PublicKey a, PublicKey b) {
            if (a instanceof DSAPublicKey) {
                if (b instanceof DSAPublicKey) {
                    DSAPublicKey da = (DSAPublicKey) a;
                    DSAPublicKey db = (DSAPublicKey) b;
                    int r = da.getParams().getG().compareTo(db.getParams().getG());
                    if (r != 0) {
                        return r;
                    }
                    r = da.getParams().getP().compareTo(db.getParams().getP());
                    if (r != 0) {
                        return r;
                    }
                    r = da.getParams().getQ().compareTo(db.getParams().getQ());
                    if (r != 0) {
                        return r;
                    }
                    return da.getY().compareTo(db.getY());
                } else {
                    return -1;
                }
            } else if (a instanceof RSAPublicKey) {
                if (b instanceof RSAPublicKey) {
                    RSAPublicKey da = (RSAPublicKey) a;
                    RSAPublicKey db = (RSAPublicKey) b;
                    int r = da.getPublicExponent().compareTo(db.getPublicExponent());
                    if (r != 0) {
                        return r;
                    }
                    return da.getModulus().compareTo(db.getModulus());
                } else {
                    return -1;
                }
            } else {
                throw new IllegalArgumentException("Only RSA and DAS keys are supported.");
            }
        }
    }

    private final class AuthorizedKeysProvider extends TimerTask {

        private Map<PublicKey, AuthorizedKey> keys;
        private Long lastModificationDate;
        private Boolean fileAvailable;

        @Override
        public void run() {
            try {
                File af = new File(KarafPublickeyAuthenticator.this.authorizedKeys);
                if (af.exists()) {
                    Long newModificationDate = Long.valueOf(af.lastModified());
                    if ((this.fileAvailable != null && !this.fileAvailable.booleanValue()) || !newModificationDate.equals(this.lastModificationDate)) {
                        LOGGER.debug("Parsing authorized keys file {}...", KarafPublickeyAuthenticator.this.authorizedKeys);
                        this.fileAvailable = Boolean.TRUE;
                        this.lastModificationDate = newModificationDate;
                        Map<PublicKey, AuthorizedKey> newKeys = KarafPublickeyAuthenticator.parseAuthorizedKeys(new FileInputStream(af));
                        this.setKeys(newKeys);
                        LOGGER.debug("Successfully parsed {} keys from file {}", newKeys.size(), KarafPublickeyAuthenticator.this.authorizedKeys);
                    }
                } else {
                    if (this.fileAvailable != null && this.fileAvailable.booleanValue()) {
                        LOGGER.debug("Authorized keys file {} disappeared, will recheck every minute", KarafPublickeyAuthenticator.this.authorizedKeys);
                    } else if (this.fileAvailable == null) {
                        LOGGER.debug("Authorized keys file {} does not exist, will recheck every minute", KarafPublickeyAuthenticator.this.authorizedKeys);
                    }
                    this.fileAvailable = Boolean.FALSE;
                    this.lastModificationDate = null;
                    this.setKeys(null);
                }
            } catch (Throwable e) {
                LOGGER.error("Error parsing authorized keys file {}", KarafPublickeyAuthenticator.this.authorizedKeys, e);
                this.fileAvailable = Boolean.FALSE;
                this.lastModificationDate = null;
                this.setKeys(null);
            }
        }

        private synchronized void setKeys(Map<PublicKey, AuthorizedKey> keys) {
            this.keys = keys;
        }

        public synchronized AuthorizedKey getKey(PublicKey publicKey) {
            if (this.keys == null) {
                return null;
            }
            return this.keys.get(publicKey);
        }

    }

    private Timer parseAuthorizedKeysTimer;
    private AuthorizedKeysProvider authorizedKeysProvider;

    private static final int getInt(byte[] b, int pos) {
        return (((int) b[pos] & 0xff) << 24) +
                (((int) b[pos+1] & 0xff) << 16) +
                (((int) b[pos+2] & 0xff) << 8) +
                ((int) b[pos+3] & 0xff);
    }

    /**
     * Parse an <code>authorized_keys</code> file in OpenSSH style.
     *
     * @param is the input stream to read.
     * @return a map of authorized public keys.
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static final Map<PublicKey, AuthorizedKey> parseAuthorizedKeys(InputStream is) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            Base64 decoder = new Base64();

            KeyFactory rsaKeyGen = KeyFactory.getInstance("RSA");
            KeyFactory dsaKeyGen = KeyFactory.getInstance("DSA");

            LineNumberReader reader = new LineNumberReader(new InputStreamReader(is, "UTF-8"));
            
            Map<PublicKey, AuthorizedKey> ret = new TreeMap<PublicKey, AuthorizedKey>(new PublicKeyComparator());

            String line;

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("[ \\t]+", 3);
                if (tokens.length != 3) {
                    throw new IOException("Authorized keys file line " + reader.getLineNumber() + " does not contain 3 tokens.");
                }
                byte[] rawKey = decoder.decode(tokens[1].getBytes("UTF-8"));
                if (getInt(rawKey, 0) != 7 || !new String(rawKey, 4, 7, "UTF-8").equals(tokens[0])) {
                    throw new IOException("Authorized keys file line " + reader.getLineNumber() + " contains a key with a format that does not match the first token.");
                }
                PublicKey pk;
                if (tokens[0].equals("ssh-dss")) {
                    int pos = 11;

                    int n = getInt(rawKey, pos);
                    pos += 4;
                    BigInteger p = new BigInteger(1, KarafPublickeyAuthenticator.arraysCopyOfRange(rawKey, pos, pos + n));
                    pos += n;

                    n = getInt(rawKey, pos);
                    pos += 4;
                    BigInteger q = new BigInteger(1, KarafPublickeyAuthenticator.arraysCopyOfRange(rawKey, pos, pos + n));
                    pos += n;

                    n = getInt(rawKey, pos);
                    pos += 4;
                    BigInteger g = new BigInteger(1, KarafPublickeyAuthenticator.arraysCopyOfRange(rawKey, pos, pos + n));
                    pos += n;

                    n = getInt(rawKey, pos);
                    pos += 4;
                    BigInteger y = new BigInteger(1, KarafPublickeyAuthenticator.arraysCopyOfRange(rawKey, pos, pos + n));
                    pos += n;

                    if (pos != rawKey.length) {
                        throw new IOException("Authorized keys file line " + reader.getLineNumber() + " contains a DSA key with extra garbage.");
                    }

                    DSAPublicKeySpec ps = new DSAPublicKeySpec(y, p, q, g);
                    pk = dsaKeyGen.generatePublic(ps);
                } else if (tokens[0].equals("ssh-rsa")) {
                    int pos = 11;

                    int n = getInt(rawKey, pos);
                    pos += 4;
                    BigInteger e = new BigInteger(1, KarafPublickeyAuthenticator.arraysCopyOfRange(rawKey, pos, pos + n));
                    pos += n;

                    n = getInt(rawKey, pos);
                    pos += 4;
                    BigInteger modulus = new BigInteger(1, KarafPublickeyAuthenticator.arraysCopyOfRange(rawKey, pos, pos + n));
                    pos += n;

                    if (pos != rawKey.length) {
                        throw new IOException("Authorized keys file line " + reader.getLineNumber() + " contains a RSA key with extra garbage.");
                    }

                    RSAPublicKeySpec ps = new RSAPublicKeySpec(modulus, e);
                    pk = rsaKeyGen.generatePublic(ps);
                } else {
                    throw new IOException("Authorized keys file line " + reader.getLineNumber() + " does not start with ssh-dss or ssh-rsa.");
                }
                
                ret.put(pk, new AuthorizedKey(tokens[2], tokens[0], pk));
            }
            return ret;
        } finally {
            is.close();
        }
    }

    public boolean authenticate(String username, PublicKey publicKey, ServerSession session) {
        AuthorizedKey ak = this.authorizedKeysProvider.getKey(publicKey);
        if (ak == null) {
            LOGGER.error("Failed authenticate of user {} from {} with unknown public key.", username, session.getIoSession().getRemoteAddress());
            return false;
        }
        LOGGER.debug("Successful authentication of user {} from {} with public key {}.", new Object[]{ username, session.getIoSession().getRemoteAddress(), ak.getAlias() });
        return true;
    }

    public void setAuthorizedKeys(String path) {
        this.authorizedKeys = path;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void startTimer() {
        if (this.active) {
            this.parseAuthorizedKeysTimer = new Timer();
            this.authorizedKeysProvider = new AuthorizedKeysProvider();
            this.parseAuthorizedKeysTimer.schedule(this.authorizedKeysProvider, 10, 60000L);
        }
    }

    public void stopTimer() {
        if (this.parseAuthorizedKeysTimer != null) {
            this.parseAuthorizedKeysTimer.cancel();
            this.parseAuthorizedKeysTimer = null;
        }
    }

    private static byte[] arraysCopyOfRange(byte[] original, int from, int to) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        byte[] copy = new byte[newLength];
        System.arraycopy(original, from, copy, 0,
                Math.min(original.length - from, newLength));
        return copy;
    }

}
