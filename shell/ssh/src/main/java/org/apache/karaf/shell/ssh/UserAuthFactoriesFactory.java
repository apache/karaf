/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.ssh;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sshd.server.auth.UserAuthFactory;
import org.apache.sshd.server.auth.keyboard.UserAuthKeyboardInteractiveFactory;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.auth.pubkey.UserAuthPublicKeyFactory;

/**
 * <p>A factory for user authentication factories to set on
 * {@link org.apache.sshd.server.SshServer#setUserAuthFactories(java.util.List)} based on a
 * comma-separated list of authentication methods.</p>
 *
 * <p>Currently, the following methods are supported:</p>
 * <ul>
 *     <li><code>password</code> Password authentication against a given JAAS domain.</li>
 *     <li><code>publickey</code> Public key authentication against an OpenSSH <code>authorized_keys</code> file.</li>
 * </ul>
 */
public class UserAuthFactoriesFactory {

    public static final String PASSWORD_METHOD = "password";
    public static final String PUBLICKEY_METHOD = "publickey";
    public static final String KEYBOARD_INTERACTIVE_METHOD = "keyboard-interactive";

    private Set<String> methodSet;
    private List<UserAuthFactory> factories;

   public void setAuthMethods(String[] methods) {
        this.methodSet = new HashSet<>();
        this.factories = new ArrayList<>();
        for (String am : methods) {
            if (PASSWORD_METHOD.equals(am)) {
                this.factories.add(new UserAuthPasswordFactory());
            } else if (KEYBOARD_INTERACTIVE_METHOD.equals(am)) {
                this.factories.add(new UserAuthKeyboardInteractiveFactory());
            } else if (PUBLICKEY_METHOD.equals(am)) {
                this.factories.add(new UserAuthPublicKeyFactory());
            } else {
                throw new IllegalArgumentException("Invalid authentication method " + am + " specified");
            }
            this.methodSet.add(am);
        }
    }

    public List<UserAuthFactory> getFactories() {
        return factories;
    }

    public boolean isPublickeyEnabled() {
        return this.methodSet.contains(PUBLICKEY_METHOD);
    }

    public boolean isPasswordEnabled() {
        return this.methodSet.contains(PASSWORD_METHOD);
    }

    public boolean isKeyboardInteractive() {
        return this.methodSet.contains(KEYBOARD_INTERACTIVE_METHOD);
    }

}
