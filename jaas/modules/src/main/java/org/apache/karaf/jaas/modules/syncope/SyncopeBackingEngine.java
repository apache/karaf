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
package org.apache.karaf.jaas.modules.syncope;

import org.apache.felix.utils.json.JSONParser;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SyncopeBackingEngine implements BackingEngine {

    private final Logger logger = LoggerFactory.getLogger(SyncopeBackingEngine.class);

    private String address;
    private boolean version2;

    private DefaultHttpClient client;

    public SyncopeBackingEngine(String address, String version, String adminUser, String adminPassword) {
        this.address = address;
        version2 = version != null && (version.equals("2.x") || version.equals("2"));

        client = new DefaultHttpClient();
        Credentials creds = new UsernamePasswordCredentials(adminUser, adminPassword);
        client.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
    }

    public void addUser(String username, String password) {
        if (username.startsWith(GROUP_PREFIX)) {
            throw new IllegalArgumentException("Group prefix " + GROUP_PREFIX + " not permitted with Syncope backend");
        }
        if (version2) {
            addUserSyncope2(username, password);
        } else {
            addUserSyncope1(username, password);
        }
    }

    private void addUserSyncope1(String username, String password) {
        HttpPost request = new HttpPost(address + "/users");
        request.setHeader("Content-Type", "application/xml");
        String userTO = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<user>" +
                "<attributes>" +
                "<attribute><readonly>false</readonly><schema>fullname</schema><value>" + username + "</value></attribute>" +
                "<attribute><readonly>false</readonly><schema>surname</schema><value>" + username + "</value></attribute>" +
                "<attribute><readonly>false</readonly><schema>userId</schema><value>" + username + "@karaf.apache.org</value></attribute>" +
                "</attributes>" +
                "<password>" + password + "</password>" +
                "<username>" + username + "</username>" +
                "</user>";
        try {
            StringEntity entity = new StringEntity(userTO);
            request.setEntity(entity);
            HttpResponse response = client.execute(request);
        } catch (Exception e) {
            logger.error("Can't add user {}", username, e);
            throw new RuntimeException("Can't add user " + username, e);
        }
    }

    private void addUserSyncope2(String username, String password) {
        HttpPost request = new HttpPost(address + "/users");
        request.setHeader("Content-Type", "application/json");
        String userTO = "{" +
                  "\"@class\": \"org.apache.syncope.common.lib.to.UserTO\"," +
                  "\"type\": \"USER\"," +
                  "\"realm\": \"/\"," +
                  "\"username\": \"" + username + "\"," +
                  "\"password\": \"" + password + "\"," +
                  "\"plainAttrs\": [" +
                    "{ \"schema\": \"surname\", \"values\": [\"" + username + "\"] }," +
                    "{ \"schema\": \"fullname\", \"values\": [\"" + username + "\"] }," +
                    "{ \"schema\": \"userId\", \"value\": [\"" + username + "@karaf.apache.org\"] }" +
                "}";
        try {
            StringEntity entity = new StringEntity(userTO);
            request.setEntity(entity);
            HttpResponse response = client.execute(request);
        } catch (Exception e) {
            logger.error("Can't add user {}", username, e);
            throw new RuntimeException("Can't add user " + username, e);
        }
    }

    public void deleteUser(String username) {
        if (username.startsWith(GROUP_PREFIX)) {
            throw new IllegalArgumentException("Group prefix " + GROUP_PREFIX + " not permitted with Syncope backend");
        }
        HttpDelete request = new HttpDelete(address + "/users/" + username);
        if (version2) {
            request.setHeader("Content-Type", "application/json");
        } else {
            request.setHeader("Content-Type", "application/xml");
        }
        try {
            client.execute(request);
        } catch (Exception e) {
            logger.error("Can't delete user {}", username, e);
            throw new RuntimeException("Can't delete user " + username, e);
        }
    }

    public List<UserPrincipal> listUsers() {
        if (version2) {
            return listUsersSyncope2();
        } else {
            return listUsersSyncope1();
        }
    }

    private List<UserPrincipal> listUsersSyncope1() {
        List<UserPrincipal> users = new ArrayList<>();
        HttpGet request = new HttpGet(address + "/users");
        request.setHeader("Content-Type", "application/xml");
        try {
            HttpResponse response = client.execute(request);
            String responseTO = EntityUtils.toString(response.getEntity());
            if (responseTO != null && !responseTO.isEmpty()) {
                // extracting the user
                int index = responseTO.indexOf("<username>");
                while (index != -1) {
                    responseTO = responseTO.substring(index + "<username>".length());
                    int end = responseTO.indexOf("</username>");
                    if (end == -1) {
                        index = -1;
                    }
                    String username = responseTO.substring(0, end);
                    users.add(new UserPrincipal(username));
                    responseTO = responseTO.substring(end + "</username>".length());
                    index = responseTO.indexOf("<username>");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error listing users", e);
        }
        return users;
    }

    private List<UserPrincipal> listUsersSyncope2() {
        List<UserPrincipal> users = new ArrayList<>();
        HttpGet request = new HttpGet(address + "/users");
        request.setHeader("Content-Type", "application/json");
        try {
            HttpResponse httpResponse = client.execute(request);
            String response = EntityUtils.toString(httpResponse.getEntity());
            JSONParser parser = new JSONParser(response);
            List<Map<String, Object>> results = (List<Map<String, Object>>) parser.getParsed().get("result");
            for (Map<String, Object> result : results) {
                users.add(new UserPrincipal((String) result.get("username")));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error listing users", e);
        }
        return users;
    }

    @Override
    public UserPrincipal lookupUser(String username) {
        if (version2) {
            return lookupUserSyncope2(username);
        } else {
            return lookupUserSyncope1(username);
        }
    }

    private UserPrincipal lookupUserSyncope1(String username) {
        HttpGet request = new HttpGet(address + "/users?username=" + username);
        request.setHeader("Content-Type", "application/xml");
        try {
            HttpResponse response = client.execute(request);
            String responseTO = EntityUtils.toString(response.getEntity());
            if (responseTO != null && !responseTO.isEmpty()) {
                return new UserPrincipal(username);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting user", e);
        }
        return null;
    }

    private UserPrincipal lookupUserSyncope2(String username) {
        HttpGet request = new HttpGet(address + "/users/" + username);
        request.setHeader("Content-Type", "application/json");
        try {
            HttpResponse httpResponse = client.execute(request);
            String response = EntityUtils.toString(httpResponse.getEntity());
            if (response != null && !response.isEmpty()) {
                return new UserPrincipal(username);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting user", e);
        }
        return null;
    }

    public List<RolePrincipal> listRoles(Principal principal) {
        if (version2) {
            return listRolesSyncope2(principal);
        } else {
            return listRolesSyncope1(principal);
        }
    }

    private List<RolePrincipal> listRolesSyncope1(Principal principal) {
        List<RolePrincipal> roles = new ArrayList<>();
        HttpGet request = new HttpGet(address + "/users?username=" + principal.getName());
        request.setHeader("Content-Type", "application/xml");
        try {
            HttpResponse response  = client.execute(request);
            String responseTO = EntityUtils.toString(response.getEntity());
            if (responseTO != null && !responseTO.isEmpty()) {
                int index = responseTO.indexOf("<roleName>");
                while (index != 1) {
                    responseTO = responseTO.substring(index + "<roleName>".length());
                    int end = responseTO.indexOf("</roleName>");
                    if (end == -1) {
                        index = -1;
                        break;
                    }
                    String role = responseTO.substring(0, end);
                    roles.add(new RolePrincipal(role));
                    responseTO = responseTO.substring(end + "</roleName>".length());
                    index = responseTO.indexOf("<roleName>");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error listing roles", e);
        }
        return roles;
    }

    private List<RolePrincipal> listRolesSyncope2(Principal principal) {
        List<RolePrincipal> result = new ArrayList<>();
        HttpGet request = new HttpGet(address + "/users/" + principal.getName());
        request.setHeader("Content-Type", "application/json");
        try {
            HttpResponse httpResponse = client.execute(request);
            String response = EntityUtils.toString(httpResponse.getEntity());
            if (response != null && !response.isEmpty()) {
                JSONParser parser = new JSONParser(response);
                List<String> roles = (List<String>) parser.getParsed().get("roles");
                for (String role : roles) {
                    result.add(new RolePrincipal(role));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error listing roles", e);
        }
        return result;
    }

    public void addRole(String username, String role) {
        throw new RuntimeException("Roles management should be done on the Syncope side");
    }

    public void deleteRole(String username, String role) {
        throw new RuntimeException("Roles management should be done on the Syncope side");
    }

    public List<GroupPrincipal> listGroups(UserPrincipal principal) {
        return new ArrayList<>();
    }

    public void addGroup(String username, String group) {
        throw new RuntimeException("Group management is not supported by Syncope backend");
    }

    public void deleteGroup(String username, String group) {
        throw new RuntimeException("Group management is not supported by Syncope backend");
    }

    public void addGroupRole(String group, String role) {
        throw new RuntimeException("Group management is not supported by Syncope backend");
    }

    public void deleteGroupRole(String group, String role) {
        throw new RuntimeException("Group management is not supported by Syncope backend");
    }

    public Map<GroupPrincipal, String> listGroups() {
        throw new RuntimeException("Group management is not supported by Syncope backend");
    }

    public void createGroup(String group) {
            throw new RuntimeException("Group management is not supported by Syncope backend");
    }

}
