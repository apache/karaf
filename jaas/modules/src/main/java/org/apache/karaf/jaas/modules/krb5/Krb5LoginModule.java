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
package org.apache.karaf.jaas.modules.krb5;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Karaf Kerberos login module.
 */
@SuppressWarnings("restriction")
public class Krb5LoginModule implements LoginModule {

    private com.sun.security.auth.module.Krb5LoginModule loginModule = new com.sun.security.auth.module.Krb5LoginModule();

    @Override
    public void initialize(Subject _subject, CallbackHandler _callbackHandler, Map<String, ?> _sharedState, Map<String, ?> _options) {
        Map<String, Object> options = new HashMap<>(_options);
        // interpolate system properties like ${karaf.etc} in options
        for (Map.Entry<String, ?> entry : _options.entrySet()) {
            if (entry.getValue() instanceof String) {
                options.put(entry.getKey(), Krb5LoginModule.interpolate((String)entry.getValue()));
            } else {
                options.put(entry.getKey(), entry.getValue());
            }
        }
        this.loginModule.initialize(_subject, _callbackHandler, _sharedState, options);
    }

    @Override
    public boolean login() throws LoginException {
        return loginModule.login();
    }

    @Override
    public boolean commit() throws LoginException {
        return loginModule.commit();
    }

    @Override
    public boolean abort() throws LoginException {
        return loginModule.abort();
    }

    @Override
    public boolean logout() throws LoginException {
        return loginModule.logout();
    }

    private static String interpolate(String _value) {
        String value = _value;
        Matcher matcher = Pattern.compile("\\$\\{([^}]+)\\}").matcher(value);
        while (matcher.find()) {
            String rep = System.getProperty(matcher.group(1));
            if (rep != null) {
                value = value.replace(matcher.group(0), rep);
                matcher.reset(value);
            }
        }
        return value;
    }
}
