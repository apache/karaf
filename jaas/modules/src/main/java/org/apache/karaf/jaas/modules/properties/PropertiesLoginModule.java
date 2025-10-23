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
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.karaf.jaas.modules.JAASUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAAS Login module for user / password, based on two properties files.
 */
public class PropertiesLoginModule extends AbstractKarafLoginModule {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(PropertiesLoginModule.class);

    static final String USER_FILE = "users";

    private String usersFile;


    public void initialize(Subject sub, CallbackHandler handler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(sub,handler,options);
        usersFile = JAASUtils.getString(options, USER_FILE);
        if (debug) {
            LOGGER.debug("Initialized debug={} usersFile={}", debug, usersFile);
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

        // check the provided password
        if (!checkPassword(password, storedPassword)) {
        	if (!this.detailedLoginExcepion) {
        		throw new FailedLoginException("login failed");
        	} else {
        		throw new FailedLoginException("Password for " + user + " does not match");
        	}
        }

        principals = JAASUtils.getPrincipals(user, users);

        users.clear();

        if (debug) {
            LOGGER.debug("Successfully logged in {}", user);
        }
        succeeded = true;
        return true;
    }

}