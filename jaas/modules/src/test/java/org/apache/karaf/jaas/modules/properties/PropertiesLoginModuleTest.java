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
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import junit.framework.Assert;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.junit.Test;

public class PropertiesLoginModuleTest {

    @Test
    public void testBasicLogin() throws Exception {
        File f = File.createTempFile(getClass().getName(), ".tmp");
        try {
            Properties p = new Properties(f);
            PropertiesBackingEngine pbe = new PropertiesBackingEngine(p);
            pbe.addUser("abc", "xyz");
            pbe.addRole("abc", "myrole");
            pbe.addUser("pqr", "abc");

            PropertiesLoginModule module = new PropertiesLoginModule();
            Map<String, String> options = new HashMap<String, String>();
            options.put(PropertiesLoginModule.USER_FILE, f.getAbsolutePath());
            CallbackHandler cb = new CallbackHandler() {
                @Override
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback cb : callbacks) {
                        if (cb instanceof NameCallback) {
                            ((NameCallback) cb).setName("abc");
                        } else if (cb instanceof PasswordCallback) {
                            ((PasswordCallback) cb).setPassword("xyz".toCharArray());
                        }
                    }
                }
            };
            Subject subject = new Subject();
            module.initialize(subject, cb, null, options);

            Assert.assertEquals("Precondition", 0, subject.getPrincipals().size());
            Assert.assertTrue(module.login());
            Assert.assertTrue(module.commit());

            Assert.assertEquals(2, subject.getPrincipals().size());

            boolean foundUser = false;
            boolean foundRole = false;
            for (Principal pr : subject.getPrincipals()) {
                if (pr instanceof UserPrincipal) {
                    Assert.assertEquals("abc", pr.getName());
                    foundUser = true;
                } else if (pr instanceof RolePrincipal) {
                    Assert.assertEquals("myrole", pr.getName());
                    foundRole = true;
                }
            }
            Assert.assertTrue(foundUser);
            Assert.assertTrue(foundRole);

            Assert.assertTrue(module.logout());
            Assert.assertEquals("Principals should be gone as the user has logged out", 0, subject.getPrincipals().size());
        } finally {
            if (!f.delete()) {
                Assert.fail("Could not delete temporary file: " + f);
            }
        }
    }

    @Test
    public void testLoginIncorrectPassword() throws Exception {
        File f = File.createTempFile(getClass().getName(), ".tmp");
        try {
            Properties p = new Properties(f);
            PropertiesBackingEngine pbe = new PropertiesBackingEngine(p);
            pbe.addUser("abc", "xyz");
            pbe.addUser("pqr", "abc");

            PropertiesLoginModule module = new PropertiesLoginModule();
            Map<String, String> options = new HashMap<String, String>();
            options.put(PropertiesLoginModule.USER_FILE, f.getAbsolutePath());
            CallbackHandler cb = new CallbackHandler() {
                @Override
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback cb : callbacks) {
                        if (cb instanceof NameCallback) {
                            ((NameCallback) cb).setName("abc");
                        } else if (cb instanceof PasswordCallback) {
                            ((PasswordCallback) cb).setPassword("abc".toCharArray());
                        }
                    }
                }
            };
            module.initialize(new Subject(), cb, null, options);
            try {
                module.login();
                Assert.fail("The login should have failed as the passwords didn't match");
            } catch (FailedLoginException fle) {
                // good
            }
        } finally {
            if (!f.delete()) {
                Assert.fail("Could not delete temporary file: " + f);
            }
        }
    }

    @Test
    public void testLoginWithGroups() throws Exception {
        File f = File.createTempFile(getClass().getName(), ".tmp");
        try {
            Properties p = new Properties(f);
            PropertiesBackingEngine pbe = new PropertiesBackingEngine(p);
            pbe.addUser("abc", "xyz");
            pbe.addRole("abc", "myrole");
            pbe.addUser("pqr", "abc");
            pbe.addGroup("pqr", "group1");
            pbe.addGroupRole("group1", "r1");

            PropertiesLoginModule module = new PropertiesLoginModule();
            Map<String, String> options = new HashMap<String, String>();
            options.put(PropertiesLoginModule.USER_FILE, f.getAbsolutePath());
            CallbackHandler cb = new CallbackHandler() {
                @Override
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback cb : callbacks) {
                        if (cb instanceof NameCallback) {
                            ((NameCallback) cb).setName("pqr");
                        } else if (cb instanceof PasswordCallback) {
                            ((PasswordCallback) cb).setPassword("abc".toCharArray());
                        }
                    }
                }
            };
            Subject subject = new Subject();
            module.initialize(subject, cb, null, options);

            Assert.assertEquals("Precondition", 0, subject.getPrincipals().size());
            Assert.assertTrue(module.login());
            Assert.assertTrue(module.commit());

            Assert.assertEquals(3, subject.getPrincipals().size());
            boolean foundUser = false;
            boolean foundRole = false;
            boolean foundGroup = false;
            for (Principal pr : subject.getPrincipals()) {
                if (pr instanceof UserPrincipal) {
                    Assert.assertEquals("pqr", pr.getName());
                    foundUser = true;
                } else if (pr instanceof GroupPrincipal) {
                    Assert.assertEquals("group1", pr.getName());
                    foundGroup = true;
                } else if (pr instanceof RolePrincipal) {
                    Assert.assertEquals("r1", pr.getName());
                    foundRole = true;
                }
            }
            Assert.assertTrue(foundUser);
            Assert.assertTrue(foundGroup);
            Assert.assertTrue(foundRole);
        } finally {
            if (!f.delete()) {
                Assert.fail("Could not delete temporary file: " + f);
            }
        }
    }

    // This is a fairly important test that ensures that you cannot log in under the name of a
    // group directly.
    @Test
    public void testCannotLoginAsGroupDirectly() throws Exception {
        testCannotLoginAsGroupDirectly("group1");
        testCannotLoginAsGroupDirectly("_g_:group1");
        testCannotLoginAsGroupDirectly(PropertiesBackingEngine.GROUP_PREFIX + "group1");
    }

    private void testCannotLoginAsGroupDirectly(final String name) throws IOException, LoginException {
        File f = File.createTempFile(getClass().getName(), ".tmp");
        try {
            Properties p = new Properties(f);
            PropertiesBackingEngine pbe = new PropertiesBackingEngine(p);
            pbe.addUser("abc", "xyz");
            pbe.addRole("abc", "myrole");
            pbe.addUser("pqr", "abc");
            pbe.addGroup("pqr", "group1");
            pbe.addGroupRole("group1", "r1");

            PropertiesLoginModule module = new PropertiesLoginModule();
            Map<String, String> options = new HashMap<String, String>();
            options.put(PropertiesLoginModule.USER_FILE, f.getAbsolutePath());
            CallbackHandler cb = new CallbackHandler() {
                @Override
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (Callback cb : callbacks) {
                        if (cb instanceof NameCallback) {
                            ((NameCallback) cb).setName(name);
                        } else if (cb instanceof PasswordCallback) {
                            ((PasswordCallback) cb).setPassword("group".toCharArray());
                        }
                    }
                }
            };
            module.initialize(new Subject(), cb, null, options);
            try {
                module.login();
                Assert.fail("The login should have failed as you cannot log in under a group name directly");
            } catch (FailedLoginException fle) {
                // good
            }
        } finally {
            if (!f.delete()) {
                Assert.fail("Could not delete temporary file: " + f);
            }
        }
    }

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
