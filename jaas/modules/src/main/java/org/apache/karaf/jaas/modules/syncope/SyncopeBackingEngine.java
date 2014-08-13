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
    }

    public void deleteUser(String username) {
        if (username.startsWith(GROUP_PREFIX)) {
            throw new IllegalArgumentException("Group prefix " + GROUP_PREFIX + " not permitted with Syncope backend");
        }
        HttpDelete request = new HttpDelete(address + "/users/" + username);
        try {
            HttpResponse response = client.execute(request);
            logger.warn("Status code: " + response.getStatusLine().getStatusCode());
            logger.warn(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            throw new RuntimeException("Error deleting user", e);
        }
    }

    public List<UserPrincipal> listUsers() {
        HttpGet request = new HttpGet(address + "/users");
        try {
            HttpResponse response = client.execute(request);
            logger.warn("Status code: " + response.getStatusLine().getStatusCode());
            logger.warn(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            throw new RuntimeException("Error listing user", e);
        }
        return new ArrayList<UserPrincipal>();
    }

    public List<RolePrincipal> listRoles(Principal principal) {
        HttpGet request = new HttpGet(address + "/users/" + principal.getName());
        try {
            HttpResponse response  = client.execute(request);
            logger.warn("Status code: " + response.getStatusLine().getStatusCode());
            logger.warn(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            throw new RuntimeException("Error listing roles", e);
        }
        return new ArrayList<RolePrincipal>();
    }

    public void addRole(String username, String role) {

    }

    public void deleteRole(String username, String role) {

    }

    public List<GroupPrincipal> listGroups(UserPrincipal principal) {
        return new ArrayList<GroupPrincipal>();
    }

    public void addGroup(String username, String group) {

    }

    public void deleteGroup(String username, String group) {

    }

    public void addGroupRole(String group, String role) {

    }

    public void deleteGroupRole(String group, String role) {

    }

}
