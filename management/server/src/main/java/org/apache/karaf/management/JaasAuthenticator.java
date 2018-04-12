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
package org.apache.karaf.management;

import org.apache.karaf.jaas.boot.principal.ClientPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;

import java.rmi.server.RemoteServer;
import java.security.Principal;

import javax.management.remote.JMXAuthenticator;
import javax.security.auth.Subject;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public class JaasAuthenticator implements JMXAuthenticator {

    private String realm;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Subject authenticate(Object credentials) throws SecurityException {
        if (!(credentials instanceof String[])) {
            throw new IllegalArgumentException("Expected String[2], got "
                            + (credentials != null ? credentials.getClass().getName() : null));
        }

        final String[] params = (String[]) credentials;
        if (params.length != 2) {
            throw new IllegalArgumentException("Expected String[2] but length was " + params.length);
        }
        try {
            Subject subject = new Subject();
            try {
                subject.getPrincipals().add(new ClientPrincipal("jmx", RemoteServer.getClientHost()));
            } catch (Throwable t) {
                // Ignore
            }
            LoginContext loginContext = new LoginContext(realm, subject, callbacks -> {
                for (int i = 0; i < callbacks.length; i++) {
                    if (callbacks[i] instanceof NameCallback) {
                        ((NameCallback) callbacks[i]).setName(params[0]);
                    } else if (callbacks[i] instanceof PasswordCallback) {
                        ((PasswordCallback) callbacks[i]).setPassword((params[1].toCharArray()));
                    } else {
                        throw new UnsupportedCallbackException(callbacks[i]);
                    }
                }
            });
            loginContext.login();

            int roleCount = 0;
            for (Principal principal : subject.getPrincipals()) {
                if (principal instanceof RolePrincipal) {
                    roleCount++;
                }
            }

            if (roleCount == 0) {
                throw new FailedLoginException("User doesn't have role defined");
            }

            return subject;
        } catch (LoginException e) {
            throw new SecurityException("Authentication failed", e);
        }
    }
}
