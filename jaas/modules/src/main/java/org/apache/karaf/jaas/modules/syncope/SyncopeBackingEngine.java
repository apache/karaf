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

    private DefaultHttpClient client;

    public SyncopeBackingEngine(String address, String adminUser, String adminPassword) {
        this.address = address;

        client = new DefaultHttpClient();
        Credentials creds = new UsernamePasswordCredentials(adminUser, adminPassword);
        client.getCredentialsProvider().setCredentials(AuthScope.ANY, creds);
    }

    public void addUser(String username, String password) {
        if (username.startsWith(GROUP_PREFIX)) {
            throw new IllegalArgumentException("Group prefix " + GROUP_PREFIX + " not permitted with Syncope backend");
        }
        HttpPost request = new HttpPost(address + "/users");
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

    public void deleteUser(String username) {
        if (username.startsWith(GROUP_PREFIX)) {
            throw new IllegalArgumentException("Group prefix " + GROUP_PREFIX + " not permitted with Syncope backend");
        }
        HttpDelete request = new HttpDelete(address + "/users/" + username);
        try {
            client.execute(request);
        } catch (Exception e) {
            logger.error("Can't delete user {}", username, e);
            throw new RuntimeException("Can't delete user " + username, e);
        }
    }

    public List<UserPrincipal> listUsers() {
        List<UserPrincipal> users = new ArrayList<UserPrincipal>();
        HttpGet request = new HttpGet(address + "/users");
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

    public List<RolePrincipal> listRoles(Principal principal) {
        List<RolePrincipal> roles = new ArrayList<RolePrincipal>();
        HttpGet request = new HttpGet(address + "/users?username=" + principal.getName());
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

    public void addRole(String username, String role) {
        throw new RuntimeException("Roles management should be done on the Syncope side");
    }

    public void deleteRole(String username, String role) {
        throw new RuntimeException("Roles management should be done on the Syncope side");
    }

    public List<GroupPrincipal> listGroups(UserPrincipal principal) {
        return new ArrayList<GroupPrincipal>();
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
