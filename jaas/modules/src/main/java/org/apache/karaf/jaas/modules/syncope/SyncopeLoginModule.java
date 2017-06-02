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
package org.apache.karaf.jaas.modules.syncope;

import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.AbstractKarafLoginModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.*;

/**
 * Karaf login module which uses Apache Syncope backend.
 */
public class SyncopeLoginModule extends AbstractKarafLoginModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(SyncopeLoginModule.class);

    public final static String ADDRESS = "address";
    public final static String ADMIN_USER = "admin.user"; // for the backing engine
    public final static String ADMIN_PASSWORD = "admin.password"; // for the backing engine

    private String address;

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, options);
        address = (String) options.get(ADDRESS);
    }

    public boolean login() throws LoginException {
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);

        try {
            callbackHandler.handle(callbacks);
        } catch (IOException ioException) {
            throw new LoginException(ioException.getMessage());
        } catch (UnsupportedCallbackException unsupportedCallbackException) {
            throw new LoginException(unsupportedCallbackException.getMessage() + " not available to obtain information from user.");
        }

        user = ((NameCallback) callbacks[0]).getName();

        char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
        if (tmpPassword == null) {
            tmpPassword = new char[0];
        }
        String password = new String(tmpPassword);
        principals = new HashSet<>();

        // authenticate the user on Syncope
        LOGGER.debug("Authenticate user {} on Syncope located {}", user, address);
        DefaultHttpClient client = new DefaultHttpClient();
        Credentials creds = new UsernamePasswordCredentials(user, password);
        client.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
        HttpGet get = new HttpGet(address + "/users/self");
        get.setHeader("Content-Type", "application/xml");
        List<String> roles = new ArrayList<>();
        try {
            CloseableHttpResponse response = client.execute(get);
            LOGGER.debug("Syncope HTTP response status code: {}", response.getStatusLine().getStatusCode());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                LOGGER.warn("User {} not authenticated", user);
                return false;
            }
            LOGGER.debug("User {} authenticated", user);
            LOGGER.debug("Populating principals with user");
            principals.add(new UserPrincipal(user));
            LOGGER.debug("Retrieving user {} roles", user);
            roles = extractingRoles(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            LOGGER.error("User {} authentication failed", user, e);
            throw new LoginException("User " + user + " authentication failed: " + e.getMessage());
        }

        LOGGER.debug("Populating principals with roles");
        for (String role : roles) {
            principals.add(new RolePrincipal(role));
        }

        return true;
    }

    /**
     * Extract the user roles from the Syncope entity response.
     *
     * @param response the HTTP response from Syncope.
     * @return the list of user roles.
     * @throws Exception in case of extraction failure.
     */
    protected List<String> extractingRoles(String response) throws Exception {
        List<String> roles = new ArrayList<>();
        if (response != null && !response.isEmpty()) {
            // extract the <memberships> element if it exists
            int index = response.indexOf("<memberships>");
            if (index != -1) {
                response = response.substring(index + "<memberships>".length());
                index = response.indexOf("</memberships>");
                response = response.substring(0, index);
    
                // looking for the roleName elements
                index = response.indexOf("<roleName>");
                while (index != -1) {
                    response = response.substring(index + "<roleName>".length());
                    int end = response.indexOf("</roleName>");
                    if (end == -1) {
                        index = -1;
                    }
                    String role = response.substring(0, end);
                    roles.add(role);
                    response = response.substring(end + "</roleName>".length());
                    index = response.indexOf("<roleName>");
                }
            }

        }
        return roles;
    }

    public boolean abort() {
        return true;
    }

    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        return true;
    }

}
