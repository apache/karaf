/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.karaf.jaas.modules.audit;

import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.karaf.jaas.boot.principal.ClientPrincipal;
import org.apache.karaf.jaas.modules.JAASUtils;

import static java.util.stream.Collectors.toList;

public abstract class AbstractAuditLoginModule implements LoginModule {

    enum Action {
        ATTEMPT,
        SUCCESS,
        FAILURE,
        LOGOUT
    }

    protected Subject subject;
    private CallbackHandler handler;
    private String username;
    private boolean enabled;

    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        enabled = Boolean.parseBoolean(JAASUtils.getString(options, "enabled"));
        handler = callbackHandler;
    }

    protected abstract void audit(Action action, String user);

    public boolean login() throws LoginException {
        NameCallback user = new NameCallback("User name:");
        Callback[] callbacks = new Callback[]{user};
        try {
            handler.handle(callbacks);
        } catch (Exception e) {
            throw (LoginException) new LoginException("Unable to process callback: " + e.getMessage()).initCause(e);
        }
        if (callbacks.length != 1) {
            throw new IllegalStateException("Number of callbacks changed by server!");
        }
        user = (NameCallback) callbacks[0];
        username = user.getName();
        if (enabled && username != null) {
            audit(Action.ATTEMPT, username);
        }
        return false;
    }

    public boolean commit() throws LoginException {
        if (enabled && username != null) {
            audit(Action.SUCCESS, username);
        }
        return false;
    }

    public boolean abort() throws LoginException {
        if (enabled && username != null) { //work around initial "fake" login
            audit(Action.FAILURE, username);
            username = null;
        }
        return false;
    }

    public boolean logout() throws LoginException {
        if (enabled && username != null) {
            audit(Action.LOGOUT, username);
            username = null;
        }
        return false;
    }

    protected String getPrincipalInfo() {
        String principalInfo;
        List<String> principalInfos = subject.getPrincipals(ClientPrincipal.class).stream().map(ClientPrincipal::getName).collect(toList());

        if (principalInfos.size() > 0) {
            principalInfo = String.join(", ", principalInfos);
        } else {
            principalInfo = "no client principals found";
        }

        return principalInfo;
    }
}
