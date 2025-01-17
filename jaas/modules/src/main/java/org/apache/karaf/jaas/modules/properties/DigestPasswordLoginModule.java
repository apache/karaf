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
package org.apache.karaf.jaas.modules.properties;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.JAASUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAAS Login module for user / password, based on two properties files.
 */
public class  DigestPasswordLoginModule extends AbstractKarafLoginModule {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(DigestPasswordLoginModule.class);

    static final String USER_FILE = "users";

    private MessageDigest digest;

    private String usersFile;


    public void initialize(Subject sub, CallbackHandler handler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(sub,handler,options);
        usersFile = JAASUtils.getString(options, USER_FILE);
        if (debug) {
            LOGGER.debug("Initialized debug={} usersFile={}", debug, usersFile);
        }
    }

    public String doPasswordDigest(String nonce, String created, String password) {
        String passwdDigest = null;
        try {
            passwdDigest = doPasswordDigest(nonce, created, password.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
                LOGGER.debug(e.getMessage(), e);
        }
        return passwdDigest;
    }

    public String doPasswordDigest(String nonce, String created, byte[] password) {
        String passwdDigest = null;
        try {
            byte[] b1 = nonce != null ? new Base64().decode(nonce) : new byte[0];
            byte[] b2 = created != null ? created.getBytes(StandardCharsets.UTF_8) : new byte[0];
            byte[] b3 = password;
            byte[] b4 = new byte[b1.length + b2.length + b3.length];
            int offset = 0;
            System.arraycopy(b1, 0, b4, offset, b1.length);
            offset += b1.length;

            System.arraycopy(b2, 0, b4, offset, b2.length);
            offset += b2.length;

            System.arraycopy(b3, 0, b4, offset, b3.length);

            byte[] digestBytes = generateDigest(b4);
            passwdDigest = new String(Base64.encodeBase64(digestBytes));
        } catch (Exception e) {
            LOGGER.debug(e.getMessage(), e);
        }
        return passwdDigest;
    }

    /**
     * Generate a (SHA1) digest of the input bytes. The MessageDigest instance that backs this
     * method is cached for efficiency.
     * @param inputBytes the bytes to digest
     * @return the digest of the input bytes
     */
    public synchronized byte[] generateDigest(byte[] inputBytes) {
        try {
            if (digest == null) {
                digest = MessageDigest.getInstance("SHA-1");
            }
            return digest.digest(inputBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error in generating digest", e);
        }
    }

    public boolean login() throws LoginException {
        if (usersFile == null) {
            throw new LoginException("The property users may not be null");
        }
        File f = new File(usersFile);
        if (!f.exists()) {
            throw new LoginException("Users file not found at " + f);
        }

        Properties users;
        try {
            users = new Properties(f);
        } catch (IOException ioe) {
            throw new LoginException("Unable to load user properties file " + f);
        }

        Callback[] callbacks = new Callback[2];

        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);
        if (callbackHandler != null) {
            try {
                callbackHandler.handle(callbacks);
            } catch (IOException ioe) {
                throw new LoginException(ioe.getMessage());
            } catch (UnsupportedCallbackException uce) {
                throw new LoginException(uce.getMessage() + " not available to obtain information from user");
            }
        }
        // user callback get value
        if (((NameCallback) callbacks[0]).getName() == null) {
            throw new LoginException("Username can not be null");
        }
        user = ((NameCallback) callbacks[0]).getName();
        if (user.startsWith(PropertiesBackingEngine.GROUP_PREFIX)) {
            // you can't log in under a group name
            throw new FailedLoginException("login failed");
        }

        // password callback get value
        if (((PasswordCallback) callbacks[1]).getPassword() == null) {
            throw new LoginException("Password can not be null");
        }
        String password = new String(((PasswordCallback) callbacks[1]).getPassword());

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
        String storedPassword = infos[0];

        CallbackHandler myCallbackHandler = null;

        try {
            Field field = callbackHandler.getClass().getDeclaredField("ch");
            field.setAccessible(true);
            myCallbackHandler = (CallbackHandler) field.get(callbackHandler);
        } catch (Exception e) {
            throw new LoginException("Unable to load underlying callback handler");
        }

        if (myCallbackHandler instanceof NameDigestPasswordCallbackHandler) {
            NameDigestPasswordCallbackHandler digestCallbackHandler = (NameDigestPasswordCallbackHandler)myCallbackHandler;
            storedPassword = doPasswordDigest(digestCallbackHandler.getNonce(),
                                                            digestCallbackHandler.getCreatedTime(),
                                                            storedPassword);
        }

        // check the provided password
        if (!checkPassword(password, storedPassword)) {
        	if (!this.detailedLoginExcepion) {
        		throw new FailedLoginException("login failed");
        	} else {
        		throw new FailedLoginException("Password for " + user + " does not match");
        	}
        }

        principals = new HashSet<>();
        principals.add(new UserPrincipal(user));
        for (int i = 1; i < infos.length; i++) {
            if (infos[i].trim().startsWith(PropertiesBackingEngine.GROUP_PREFIX)) {
                // it's a group reference
                principals.add(new GroupPrincipal(infos[i].trim().substring(PropertiesBackingEngine.GROUP_PREFIX.length())));
                String groupInfo = users.get(infos[i].trim());
                if (groupInfo != null) {
                    String[] roles = groupInfo.split(",");
                    for (int j = 0; j < roles.length; j++) {
                        addRole(principals, roles[j].trim());
                    }
                }
            } else {
                // it's an user reference
                addRole(principals, infos[i].trim());
            }
        }

        users.clear();

        if (debug) {
            LOGGER.debug("Successfully logged in {}", user);
        }
        succeeded = true;
        return true;
    }

    private void addRole(Set<Principal> principals, String trimmedRole) {
        if (!trimmedRole.isEmpty())
            principals.add(new RolePrincipal(trimmedRole));
    }
}
