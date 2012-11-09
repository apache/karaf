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

import java.io.StringReader;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.security.auth.Subject;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.karaf.jaas.authz.AuthorizationService;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;

public class AuthorizationServiceImpl implements AuthorizationService {

    public static final String NAMESPACE = "http://karaf.apache.org/xmlns/authz/v1.0.0";
    public static final QName ENTRIES = new QName(NAMESPACE, "entries");
    public static final QName ENTRY = new QName(NAMESPACE, "entry");
    public static final String PERMISSION = "permission";
    public static final String ROLES = "roles";
    public static final String TYPE = "type";

    /**
     * Add the roles to the ACLs list
     */
    public static final String TYPE_ADD = "add";

    /**
     * Set the ACLs to the given roles
     */

    public static final String TYPE_SET = "set";

    /**
     * Remove the given roles from the ACLs list
     */
    public static final String TYPE_REM = "rem";

    /**
     * All roles are matched using the wildcard
     */
    public static final String ANY_ROLE = "*";

    private List<AuthorizationEntry> entries = Collections.emptyList();

    public List<String> getPrincipals(Subject subject) {
        List<String> principals = new ArrayList<String>();
        if (subject != null) {
            for (Principal principal : subject.getPrincipals()) {
                principals.add(principal.getClass().getName() + ":" + principal.getName());
            }
        }
        return principals;
    }

    public boolean isPermitted(Subject subject, String permission) {
        return isPermitted(getPrincipals(subject), permission);
    }

    public void checkPermission(Subject subject, String permission) {
        checkPermission(getPrincipals(subject), permission);
    }

    public void checkRole(Subject subject, String role) {
        checkRole(getPrincipals(subject), role);
    }

    public boolean hasRole(Subject subject, String role) {
        return hasRole(getPrincipals(subject), role);
    }

    public void checkPermission(List<String> principals, String permission) {
        if (!isPermitted(principals, permission)) {
            throw new SecurityException("Unauthorized permission: " + permission);
        }
    }

    public boolean isPermitted(List<String> principals, String permission) {
        Set<String> acls = getAcls(permission);
        for (String acl : acls) {
            if (hasRole(principals, acl)) {
                return true;
            }
        }
        return false;
    }

    public void checkRole(List<String> principals, String role) {
        if (!hasRole(principals, role)) {
            throw new SecurityException("Unauthorized permission: missing role " + role);
        }
    }

    public boolean hasRole(List<String> principals, String role) {
        if (ANY_ROLE.equals(role)) {
            return true;
        }
        if (role.indexOf(':') < 0) {
            role = RolePrincipal.class.getName() + ":" + role;
        }
        return principals.contains(role);
    }

    public Set<String> getAcls(String permission) {
        Set<String> acls = new HashSet<String>();
        WildcardPermission perm = new WildcardPermission(permission);
        for (AuthorizationEntry entry : entries) {
            if (entry.implies(perm)) {
                if (TYPE_ADD.equalsIgnoreCase(entry.getType())) {
                    acls.addAll(entry.getAcls());
                } else if (TYPE_SET.equalsIgnoreCase(entry.getType())) {
                    acls.clear();
                    acls.addAll(entry.getAcls());
                } else if (TYPE_REM.equalsIgnoreCase(entry.getType())) {
                    acls.removeAll(entry.getAcls());
                }
            }
        }
        return acls;
    }


    public void setAcls(String acls) {
        List<AuthorizationEntry> entries = new ArrayList<AuthorizationEntry>();
        if (acls != null) {
            try {
                XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader(acls));
                int event;
                while ((event = reader.next()) != XMLStreamConstants.END_DOCUMENT) {
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        if (ENTRIES.equals(reader.getName())) {
                            // Do nothing
                        } else if (ENTRY.equals(reader.getName())) {
                            String permission = "";
                            String roles = "";
                            String type = TYPE_ADD;
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                String name = reader.getAttributeLocalName(i);
                                if (PERMISSION.equals(name)) {
                                    permission = reader.getAttributeValue(i);
                                } else if (ROLES.equals(name)) {
                                    roles = reader.getAttributeValue(i);
                                } else if (TYPE.equals(name)) {
                                    type = reader.getAttributeValue(i);
                                    if (!TYPE_ADD.equals(type) && !TYPE_SET.equals(type) && !TYPE_REM.equals(type)) {
                                        throw new IllegalArgumentException("Unsupported ACL entry type: " + type);
                                    }
                                }
                            }
                            entries.add(new AuthorizationEntry(permission, roles, type));
                        } else {
                            throw new IllegalArgumentException("Unsupported element " + reader.getName());
                        }
                    }
                }
            } catch (XMLStreamException e) {
                throw new IllegalArgumentException("Error parsing ACL entries", e);
            }
        }
        this.entries = entries;
    }
}
