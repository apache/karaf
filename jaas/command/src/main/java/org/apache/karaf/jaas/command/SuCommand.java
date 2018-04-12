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
import org.apache.karaf.shell.support.ShellUtil;
import org.apache.karaf.util.jaas.JaasHelper;

@Command(scope = "jaas", name = "su", description = "Substitute user identity")
@Service
public class SuCommand implements Action {

    @Option(name = "--realm")
    String realm = "karaf";

    @Argument(description = "Name of the user to substitute")
    String user = "karaf";

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
                    String password = SuCommand.this.session.readLine("Password: ", '*');
                    ((PasswordCallback) callback).setPassword(password.toCharArray());
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
        });
        loginContext.login();

        JaasHelper.doAs(subject, (PrivilegedExceptionAction<Object>) () -> {
            final Session newSession = session.getFactory().create(
                    System.in, System.out, System.err, SuCommand.this.session.getTerminal(), null, null);
            Object oldIgnoreInterrupts = session.get(Session.IGNORE_INTERRUPTS);
            try {
                session.put(Session.IGNORE_INTERRUPTS, Boolean.TRUE);
                String name = "Karaf local console user " + ShellUtil.getCurrentUserName();
                Thread thread = new Thread(newSession, name);
                thread.start();
                thread.join();
            } finally {
                session.put(Session.IGNORE_INTERRUPTS, oldIgnoreInterrupts);
            }
            return null;
        });

        loginContext.logout();
        return null;
    }

}
