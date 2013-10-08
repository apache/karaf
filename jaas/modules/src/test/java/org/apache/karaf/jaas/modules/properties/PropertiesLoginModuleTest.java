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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import junit.framework.Assert;

import org.junit.Test;

public class PropertiesLoginModuleTest {

    @Test
    public void testNullUsersFile() {
        try {
            testWithUsersFile(null);
            Assert.fail("LoginException expected");
        } catch (LoginException e) {
            Assert.assertEquals("The property users may not be null", e.getMessage());
        }
    }

    @Test
    public void testNonExistantPropertiesFile() throws LoginException, IOException, UnsupportedCallbackException {
        try {
            testWithUsersFile("/test/users.properties");
        } catch (LoginException e) {
            Assert.assertEquals("Users file not found at /test/users.properties", e.getMessage());
        }
    }

    @Test
    public void testNullPassword() throws Exception {
        PropertiesLoginModule module = new PropertiesLoginModule();
        Subject subject = new Subject();
        CallbackHandler handler = new NullHandler();
        Map<String, String> options = new HashMap<String, String>();
        options.put("users", this.getClass().getClassLoader().getResource("org/apache/karaf/jaas/modules/properties/test.properties").getFile());
        module.initialize(subject, handler, null, options);

        try {
            module.login();
            Assert.fail("LoginException expected");
        } catch (LoginException e) {
            Assert.assertEquals("Password can not be null", e.getMessage());
        }
    }

    private void testWithUsersFile(String usersFilePath) throws LoginException {
        PropertiesLoginModule module = new PropertiesLoginModule();
        Subject sub = new Subject();
        CallbackHandler handler = new NamePasswordHandler("test", "test");
        Map<String, String> options = new HashMap<String, String>();
        options.put("users", usersFilePath);
        module.initialize(sub, handler, null, options);
        module.login();
    }

}
