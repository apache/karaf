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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
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
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.RolePrincipal;
import org.apache.karaf.jaas.modules.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.jaas.modules.encryption.BasicEncryption.base64Encode;

public class PublickeyLoginModule extends AbstractKarafLoginModule {

    private final Logger LOG = LoggerFactory.getLogger(PublickeyLoginModule.class);

    private static final String USERS_FILE = "users";

    private String usersFile;

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, options);
        usersFile = (String) options.get(USERS_FILE) + "";
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

        // check if the stored password is flagged as encrypted
        String encryptedKey = getEncryptedPassword(storedKey);
        if (!storedKey.equals(encryptedKey)) {
            if (debug) {
                LOG.debug("The key isn't flagged as encrypted, encrypt it.");
            }
            if (debug) {
                LOG.debug("Rebuild the user informations string.");
            }
            userInfos = encryptedKey + ",";
            for (int i = 2; i < infos.length; i++) {
                if (i == (infos.length - 1)) {
                    userInfos = userInfos + infos[i];
                } else {
                    userInfos = userInfos + infos[i] + ",";
                }
            }
            if (debug) {
                LOG.debug("Push back the user informations in the users properties.");
            }
            users.put(user, userInfos);
            try {
                if (debug) {
                    LOG.debug("Store the users properties file.");
                }
                users.save();
            } catch (IOException ioe) {
                LOG.warn("Unable to write user properties file " + f, ioe);
            }
            storedKey = encryptedKey;
        }

        // check the provided password
        if (!checkPassword(getString(key), storedKey)) {
            if (!this.detailedLoginExcepion) {
                throw new FailedLoginException("login failed");
            } else {
                throw new FailedLoginException("Public key for " + user + " does not match");
            }
        }

        principals = new HashSet<Principal>();
        principals.add(new UserPrincipal(user));
        for (int i = 1; i < infos.length; i++) {
            principals.add(new RolePrincipal(infos[i]));
        }

        users.clear();

        if (debug) {
            LOG.debug("Successfully logged in " + user);
        }
        return true;
    }

    private String getString(PublicKey key) throws FailedLoginException {
        try {
            if (key instanceof DSAPublicKey) {
                DSAPublicKey dsa = (DSAPublicKey) key;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                write(dos, "ssh-dss");
                write(dos, dsa.getParams().getP());
                write(dos, dsa.getParams().getQ());
                write(dos, dsa.getParams().getG());
                write(dos, dsa.getY());
                dos.close();
                return base64Encode(baos.toByteArray());
            } else if (key instanceof RSAKey) {
                RSAPublicKey rsa = (RSAPublicKey) key;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                write(dos, "ssh-rsa");
                write(dos, rsa.getPublicExponent());
                write(dos, rsa.getModulus());
                dos.close();
                return base64Encode(baos.toByteArray());
            } else {
                throw new FailedLoginException("Unsupported key type " + key.getClass().toString());
            }
        } catch (IOException e) {
            throw new FailedLoginException("Unable to check public key");
        }
    }

    private void write(DataOutputStream dos, BigInteger integer) throws IOException {
        byte[] data = integer.toByteArray();
        dos.writeInt(data.length);
        dos.write(data, 0, data.length);
    }

    private void write(DataOutputStream dos, String str) throws IOException {
        byte[] data = str.getBytes();
        dos.writeInt(data.length);
        dos.write(data);
    }

    public boolean abort() throws LoginException {
        clear();
        if (debug) {
            LOG.debug("abort");
        }
        return true;
    }

    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        if (debug) {
            LOG.debug("logout");
        }
        return true;
    }

}
