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
package org.apache.karaf.jaas.authz.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class AuthorizationEntry {
    
    private final Set<String> acls;
    private final String permission;
    private final String type;
    private final WildcardPermission perm;

    public AuthorizationEntry(String permission, String roles) {
        this(permission, roles, AuthorizationServiceImpl.TYPE_ADD);
    }
    
    public AuthorizationEntry(String permission, String roles, String type) {
        this.permission = permission;
        this.acls = buildRoles(roles);
        this.type = type;
        this.perm = new WildcardPermission(permission);
    }
    
    public String getType() {
        return type;
    }

    public String getPermission() {
        return permission;
    }

    public Set<String> getAcls() {
        return acls;
    }

    public boolean implies(WildcardPermission perm) {
        return this.perm.implies(perm);
    }

    public String getRoles() {
        StringBuffer sb = new StringBuffer();
        if (this.acls != null) {
            for (Iterator<String> iter = this.acls.iterator(); iter.hasNext();) {
                String p = iter.next();
                sb.append(p);
                if (iter.hasNext()) {
                    sb.append(",");
                }
            }
        }
        return sb.toString();
    }
    
    public String toString() {
        return "AuthorizationEntry[resource=" + permission + ", roles=" + getRoles() + "]";
    }
    
    private Set<String> buildRoles(String rolesString) {
        Set<String> roles = new HashSet<String>();
        for (String role : rolesString.split("\\s+")) {
            roles.add(role);
        }
        return roles;
    }
}
