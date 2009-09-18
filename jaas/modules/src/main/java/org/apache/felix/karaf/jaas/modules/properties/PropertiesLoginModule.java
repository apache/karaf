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
package org.apache.felix.karaf.jaas.modules.properties;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.karaf.jaas.modules.RolePrincipal;
import org.apache.felix.karaf.jaas.modules.UserPrincipal;

/**
 * JAAS Login module for user / password, based on two properties files.
 *
 */
public class PropertiesLoginModule implements LoginModule {

    private static final String USER_FILE = "users";
    private static final Log LOG = LogFactory.getLog(PropertiesLoginModule.class);

    private Subject subject;
    private CallbackHandler callbackHandler;
    private boolean debug;
    private String usersFile;
    private String user;
    private Set principals = new HashSet();

    public void initialize(Subject sub, CallbackHandler handler, Map sharedState, Map options) {
        this.subject = sub;
        this.callbackHandler = handler;

        debug = "true".equalsIgnoreCase((String) options.get("debug"));
        usersFile = (String) options.get(USER_FILE) + "";

        if (debug) {
            LOG.debug("Initialized debug=" + debug + " usersFile=" + usersFile);
        }
    }

    public boolean login() throws LoginException {
        Properties users = new Properties();
        File f = new File(usersFile);
        try {
            users.load(new java.io.FileInputStream(f));
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
        user = ((NameCallback) callbacks[0]).getName();
        char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
        if (tmpPassword == null) {
            tmpPassword = new char[0];
        }

        String userInfos = (String) users.get(user);
        if (userInfos == null) {
            throw new FailedLoginException("User does not exist");
        }
        String[] infos = userInfos.split(",");
        if (!new String(tmpPassword).equals(infos[0])) {
            throw new FailedLoginException("Password does not match");
        }

        principals = new HashSet<Principal>();
        principals.add(new UserPrincipal(user));
        for (int i = 1; i < infos.length; i++) {
            principals.add(new RolePrincipal(infos[i]));
        }

        users.clear();

        if (debug) {
            LOG.debug("login " + user);
        }
        return true;
    }

    public boolean commit() throws LoginException {
        subject.getPrincipals().addAll(principals);
        clear();
        if (debug) {
            LOG.debug("commit");
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

    private void clear() {
        user = null;
    }
}
