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

import static org.apache.karaf.jaas.modules.PrincipalHelper.names;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.NamePasswordCallbackHandler;
import org.junit.Assert;
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
            Map<String, String> options = new HashMap<>();
            options.put(PropertiesLoginModule.USER_FILE, f.getAbsolutePath());
            Subject subject = new Subject();
            module.initialize(subject, new NamePasswordCallbackHandler("abc", "xyz"), null, options);

            Assert.assertEquals("Precondition", 0, subject.getPrincipals().size());
            Assert.assertTrue(module.login());
            Assert.assertTrue(module.commit());

            Assert.assertEquals(2, subject.getPrincipals().size());

            assertThat(names(subject.getPrincipals(UserPrincipal.class)), containsInAnyOrder("abc"));
            assertThat(names(subject.getPrincipals(RolePrincipal.class)), containsInAnyOrder("myrole"));

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
            Map<String, String> options = new HashMap<>();
            options.put(PropertiesLoginModule.USER_FILE, f.getAbsolutePath());
            module.initialize(new Subject(), new NamePasswordCallbackHandler("abc", "abc"), null, options);
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
            pbe.addRole("pqr", ""); // should be ignored
            pbe.addGroup("pqr", "group1");
            pbe.addGroupRole("group1", "r1");
            pbe.addGroupRole("group1", ""); // should be ignored
            pbe.addGroupRole("group1", "r2");

            PropertiesLoginModule module = new PropertiesLoginModule();
            Map<String, String> options = new HashMap<>();
            options.put(PropertiesLoginModule.USER_FILE, f.getAbsolutePath());
            Subject subject = new Subject();
            module.initialize(subject, new NamePasswordCallbackHandler("pqr", "abc"), null, options);

            Assert.assertEquals("Precondition", 0, subject.getPrincipals().size());
            Assert.assertTrue(module.login());
            Assert.assertTrue(module.commit());

            Assert.assertEquals(4, subject.getPrincipals().size());
            assertThat(names(subject.getPrincipals(UserPrincipal.class)), containsInAnyOrder("pqr"));
            assertThat(names(subject.getPrincipals(GroupPrincipal.class)), containsInAnyOrder("group1"));
            assertThat(names(subject.getPrincipals(RolePrincipal.class)), containsInAnyOrder("r1", "r2"));
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
            Map<String, String> options = new HashMap<>();
            options.put(PropertiesLoginModule.USER_FILE, f.getAbsolutePath());
            module.initialize(new Subject(), new NamePasswordCallbackHandler(name, "group"), null, options);
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
            testWithUsersFile(File.separator + "test" + File.separator + "users.properties");
        } catch (LoginException e) {
            Assert.assertEquals("Users file not found at " + File.separator + "test" + File.separator + "users.properties", e.getMessage());
        }
    }

    @Test
    public void testNullPassword() throws Exception {
        PropertiesLoginModule module = new PropertiesLoginModule();
        Subject subject = new Subject();
        CallbackHandler handler = new NullHandler();
        Map<String, String> options = new HashMap<>();
        options.put(PropertiesLoginModule.USER_FILE, getTestUsersFile());
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
        Map<String, String> options = new HashMap<>();
        options.put(PropertiesLoginModule.USER_FILE, usersFilePath);
        module.initialize(sub, handler, null, options);
        module.login();
    }
    
    @Test
    public void testNullCallbackHandler() {
        PropertiesLoginModule module = new PropertiesLoginModule();
        Subject subject = new Subject();
        Map<String, String> options = new HashMap<>();
        options.put(PropertiesLoginModule.USER_FILE, getTestUsersFile());
        module.initialize(subject, null, null, options );
        try {
            module.login();
            Assert.fail("LoginException expected");
        } catch (LoginException e) {
            Assert.assertEquals("Username can not be null", e.getMessage());
        }
    }

    private String getTestUsersFile() {
        return this.getClass().getClassLoader().getResource("org/apache/karaf/jaas/modules/properties/test.properties").getFile();
    }
}
