/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.examples.jaas.app;

import org.osgi.service.component.annotations.Component;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;

@Component(service = SecuredService.class)
public class SecuredServiceImpl implements SecuredService {

    @Override
    public boolean action(String username, String password) throws Exception {
        LoginContext loginContext = new LoginContext("karaf", callbacks -> {
           for (Callback callback : callbacks) {
               if (callback instanceof NameCallback) {
                   ((NameCallback) callback).setName(username);
               } else if (callback instanceof PasswordCallback) {
                   ((PasswordCallback) callback).setPassword(password.toCharArray());
               } else {
                   throw new UnsupportedCallbackException(callback);
               }
           }
        });
        loginContext.login();
        return (loginContext.getSubject() != null);
    }

}
