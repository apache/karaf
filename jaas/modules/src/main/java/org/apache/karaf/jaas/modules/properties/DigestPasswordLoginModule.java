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

import org.apache.cxf.interceptor.security.NameDigestPasswordCallbackHandler;
import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAAS Login module for user / password, based on two properties files.
 */
public class  DigestPasswordLoginModule extends AbstractKarafLoginModule {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(DigestPasswordLoginModule.class);

    static final String USER_FILE = "users";

    private String usersFile;
    

    public void initialize(Subject sub, CallbackHandler handler, Map sharedState, Map options) {
        super.initialize(sub,handler,options);
        usersFile = (String) options.get(USER_FILE);
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
            userInfos = (String) users.get(user);
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
            storedPassword = UsernameToken.doPasswordDigest(digestCallbackHandler.getNonce(), 
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

        principals = new HashSet<Principal>();
        principals.add(new UserPrincipal(user));
        for (int i = 1; i < infos.length; i++) {
            if (infos[i].trim().startsWith(PropertiesBackingEngine.GROUP_PREFIX)) {
                // it's a group reference
                principals.add(new GroupPrincipal(infos[i].trim().substring(PropertiesBackingEngine.GROUP_PREFIX.length())));
                String groupInfo = (String) users.get(infos[i].trim());
                if (groupInfo != null) {
                    String[] roles = groupInfo.split(",");
                    for (int j = 1; j < roles.length; j++) {
                        principals.add(new RolePrincipal(roles[j].trim()));
                    }
                }
            } else {
                // it's an user reference
                principals.add(new RolePrincipal(infos[i].trim()));
            }
        }

        users.clear();

        if (debug) {
            LOGGER.debug("Successfully logged in {}", user);
        }
        return true;
    }

    public boolean abort() throws LoginException {
        clear();
        if (debug) {
            LOGGER.debug("abort");
        }
        return true;
    }

    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        if (debug) {
            LOGGER.debug("logout");
        }
        return true;
    }

}
