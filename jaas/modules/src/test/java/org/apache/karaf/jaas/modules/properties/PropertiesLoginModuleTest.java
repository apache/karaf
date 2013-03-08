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
