/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.jaas.modules.publickey;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.JAASUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublickeyLoginModule extends AbstractKarafLoginModule {

    private final Logger LOG = LoggerFactory.getLogger(PublickeyLoginModule.class);

    private static final String USERS_FILE = "users";
    private static final Map<String, String> nistSecMap;

    static {
        // From RFC-5656
        Map<String, String> map = new HashMap<>();
        map.put("nistp256", "secp256r1");
        map.put("nistp384", "secp384r1");
        map.put("nistp521", "secp521r1");
        map.put("1.3.132.0.1", "sect163k1");
        map.put("1.2.840.10045.3.1.1", "secp192r1");
        map.put("1.3.132.0.33", "secp224r1");
        map.put("1.3.132.0.26", "sect233k1");
        map.put("1.3.132.0.27", "sect233r1");
        map.put("1.3.132.0.16", "sect283k1");
        map.put("1.3.132.0.36", "sect409k1");
        map.put("1.3.132.0.37", "sect409r1");
        map.put("1.3.132.0.38", "sect571k1");

        nistSecMap = Collections.unmodifiableMap(map);
    }
    private String usersFile;

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, options);
        usersFile = options.get(USERS_FILE) + "";
        if (debug) {
            LOG.debug("Initialized debug=" + debug + " usersFile=" + usersFile);
        }
    }

    public boolean login() throws LoginException {
        File f = new File(usersFile);
        Properties users;
        try {
            users = new Properties(f);
        } catch (IOException ioe) {
            throw new LoginException("Unable to load user properties file " + f);
        }

        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PublickeyCallback();
        try {
            callbackHandler.handle(callbacks);
        } catch (IOException ioe) {
            throw new LoginException(ioe.getMessage());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException(uce.getMessage() + " not available to obtain information from user");
        }
        String user = ((NameCallback) callbacks[0]).getName();
        if (user == null) {
            throw new FailedLoginException("Unable to retrieve user name");
        }
        PublicKey key = ((PublickeyCallback) callbacks[1]).getPublicKey();
        if (key == null) {
            throw new FailedLoginException("Unable to retrieve public key");
        }

        // user infos container read from the users properties file
        String userInfos = null;

        try {
            userInfos = users.get(user);
        } catch (NullPointerException e) {
            //error handled in the next statement
        }
        if (userInfos == null) {
            if (!this.detailedLoginExcepion) {
                throw new FailedLoginException("login failed");
            } else {
                throw new FailedLoginException("User " + user + " does not exist");
            }
        }

        // the password is in the first position
        String[] infos = userInfos.split(",");
        String storedKey = infos[0];

        // check the provided password
        if (!equals(key, storedKey)) {
            if (!this.detailedLoginExcepion) {
                throw new FailedLoginException("login failed");
            } else {
                throw new FailedLoginException("Public key for " + user + " does not match");
            }
        }

        principals = new HashSet<>();
        principals.add(new UserPrincipal(user));
        for (int i = 1; i < infos.length; i++) {
            if (infos[i].trim().startsWith(BackingEngine.GROUP_PREFIX)) {
                // it's a group reference
                principals.add(new GroupPrincipal(infos[i].trim().substring(BackingEngine.GROUP_PREFIX.length())));
                String groupInfo = users.get(infos[i].trim());
                if (groupInfo != null) {
                    String[] roles = groupInfo.split(",");
                    for (int j = 0; j < roles.length; j++) {
                        JAASUtils.addRole(principals, roles[j]);
                    }
                }
            } else {
                // it's an user reference
                JAASUtils.addRole(principals, infos[i]);
            }
        }

        users.clear();

        if (debug) {
            LOG.debug("Successfully logged in " + user);
        }
        succeeded = true;
        return true;
    }

    public static boolean equals(PublicKey key, String storedKey) throws FailedLoginException {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(storedKey)));
            String identifier = readString(dis);
            if (key instanceof DSAPublicKey) {
                if (!"ssh-dss".equals(identifier)) {
                    return false;
                }

                BigInteger p = readBigInteger(dis);
                BigInteger q = readBigInteger(dis);
                BigInteger g = readBigInteger(dis);
                BigInteger y = readBigInteger(dis);

                KeyFactory keyFactory = KeyFactory.getInstance("DSA");
                KeySpec publicKeySpec = new DSAPublicKeySpec(y, p, q, g);
                PublicKey generatedPublicKey = keyFactory.generatePublic(publicKeySpec);

                return key.equals(generatedPublicKey);
            } else if (key instanceof RSAKey) {
                if (!"ssh-rsa".equals(identifier)) {
                    return false;
                }

                BigInteger exponent = readBigInteger(dis);
                BigInteger modulus = readBigInteger(dis);

                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                KeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
                PublicKey generatedPublicKey = keyFactory.generatePublic(publicKeySpec);

                return key.equals(generatedPublicKey);
            } else if (key instanceof ECPublicKey) {
                String ecIdentifier = readString(dis);
                if (!identifier.equals("ecdsa-sha2-" + ecIdentifier)
                    || !nistSecMap.containsKey(ecIdentifier)) {
                    return false;
                }

                // Overall size of the x + y coordinates. We only support uncompressed points here, so
                // to read x + y we ignore the "04" byte using (size - 1) / 2
                int size = dis.readInt();
                byte[] bytes = new byte[(size - 1) / 2];

                dis.skipBytes(1);
                dis.read(bytes, 0, bytes.length);
                BigInteger x = new BigInteger(bytes);

                dis.read(bytes, 0, bytes.length);
                BigInteger y = new BigInteger(bytes);

                KeyFactory keyFactory = KeyFactory.getInstance("EC");
                AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
                parameters.init(new ECGenParameterSpec(nistSecMap.get(ecIdentifier)));
                ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
                ECPoint pubPoint = new ECPoint(x, y);
                KeySpec keySpec = new ECPublicKeySpec(pubPoint, ecParameters);
                PublicKey generatedPublicKey = keyFactory.generatePublic(keySpec);

                return key.equals(generatedPublicKey);
            } else {
                throw new FailedLoginException("Unsupported key type " + key.getClass().toString());
            }
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidParameterSpecException e) {
            throw new FailedLoginException("Unable to check public key");
        }
    }

    private static String readString(DataInputStream dis) throws IOException {
        int size = dis.readInt();
        byte[] bytes = new byte[size];
        dis.read(bytes, 0, bytes.length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static BigInteger readBigInteger(DataInputStream dis) throws IOException {
        int size = dis.readInt();
        byte[] bytes = new byte[size];
        dis.read(bytes, 0, bytes.length);
        return new BigInteger(bytes);
    }

}
