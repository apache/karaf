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
package org.apache.karaf.jaas.command;

import java.security.PrivilegedExceptionAction;
import java.util.List;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.util.jaas.JaasHelper;

@Command(scope = "jaas", name = "sudo", description = "Execute a command as another user")
@Service
public class SudoCommand implements Action {

    @Option(name = "--realm")
    String realm = "karaf";

    @Option(name = "--user")
    String user = "karaf";

    @Argument(multiValued = true)
    List<String> command;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        Subject subject = new Subject();
        LoginContext loginContext = new LoginContext(realm, subject, callbacks -> {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    ((NameCallback) callback).setName(user);
                } else if (callback instanceof PasswordCallback) {
                    String password = SudoCommand.this.session.readLine("Password: ", '*');
                    ((PasswordCallback) callback).setPassword(password.toCharArray());
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
        });
        loginContext.login();

        final StringBuilder sb = new StringBuilder();
        for (String s : command) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(s);
        }
        JaasHelper.doAs(subject, (PrivilegedExceptionAction<Object>) () -> session.execute(sb));

        loginContext.logout();
        return null;
    }

}
