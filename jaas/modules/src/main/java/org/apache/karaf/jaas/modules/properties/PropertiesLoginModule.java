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
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.Encryption;
import org.apache.karaf.jaas.modules.RolePrincipal;
import org.apache.karaf.jaas.modules.UserPrincipal;
import org.apache.karaf.util.Properties;

/**
 * <p>
 * JAAS Login module for user / password, based on two properties files.
 * </p>
 *
 * @author gnodet, jbonofre
 */
public class PropertiesLoginModule extends AbstractKarafLoginModule {

    private static final String USER_FILE = "users";
    private static final Log LOG = LogFactory.getLog(PropertiesLoginModule.class);

    private String usersFile;

    public void initialize(Subject sub, CallbackHandler handler, Map sharedState, Map options) {
        super.initialize(sub,handler,options);
        usersFile = (String) options.get(USER_FILE) + "";

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
        callbacks[1] = new PasswordCallback("Password: ", false);
        try {
            callbackHandler.handle(callbacks);
        } catch (IOException ioe) {
            throw new LoginException(ioe.getMessage());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException(uce.getMessage() + " not available to obtain information from user");
        }
        // user callback get value
        user = ((NameCallback) callbacks[0]).getName();
        // password callback get value
        String password = new String(((PasswordCallback) callbacks[1]).getPassword());

        // user infos container read from the users properties file
        String userInfos = null;

        try {
            userInfos = (String) users.get(user);
        } catch (NullPointerException e) {
            //error handled in the next statement
        }
        if (userInfos == null) {
            throw new FailedLoginException("User " + user + " does not exist");
        }
        
        // the password is in the first position
        String[] infos = userInfos.split(",");
        String storedPassword = infos[0];
        
        // check if encryption is enabled
        Encryption encryption = getEncryption();
        if (encryption != null) {
            if (debug) {
                LOG.debug("Encryption is enabled.");
            }
            // check if the stored password is flagged as encrypted
            if (!storedPassword.startsWith("{CRYPT}")) {
                if (debug) {
                    LOG.debug("The password isn't flagged as encrypted, encrypt it.");
                }
                storedPassword = "{CRYPT}" + encryption.encryptPassword(storedPassword);
                if (debug) {
                    LOG.debug("Rebuild the user informations string.");
                }
                userInfos = storedPassword + ",";
                for (int i = 1; i < infos.length; i++) {
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
            }
            storedPassword = storedPassword.substring(7);
        }

        // check the provided password
        boolean result;
        if (encryption == null) {
            result = storedPassword.equals(password);
        } else {
            result = encryption.checkPassword(password, storedPassword);
        }
        if (!result) {
            LOG.error("Check password failed: " + password + " / " + storedPassword);
            throw new FailedLoginException("Password for " + user + " does not match");
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
