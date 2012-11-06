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
package org.apache.karaf.jaas.authz;

import java.util.List;
import javax.security.auth.Subject;

/**
 * The <code>AuthorizationService</code> interface allows bundles
 * to apply authorization based security mechanisms to various
 * operations.
 *
 * Each principal is represented in its string form by concatenating
 * the principal class name with the principal name, separated by a
 * column.
 *
 */
public interface AuthorizationService {

    /**
     * Retrieve the list of principals associated to a given subject.
     *
     * @param subject the JAAS subject
     * @return the list of principals
     */
    List<String> getPrincipals(Subject subject);

    /**
     * Asserts that the subject has the given permission.
     *
     * @param subject the subject
     * @param permission the permission to check
     * @throws SecurityException if the subject does not have the needed permission
     */
    void checkPermission(Subject subject, String permission);

    /**
     * Check whether the subject has the given permission.
     *
     * @param subject the subject
     * @param permission the permission to check
     * @return a boolean indicating if the subject has the needed permission
     */
    boolean isPermitted(Subject subject, String permission);

    /**
     * Asserts that the subject has the given role.
     *
     * @param subject the subject
     * @param role the role to check
     * @throws SecurityException if the subject does not have the needed role
     */
    void checkRole(Subject subject, String role);

    /**
     * Check whether the subject has the given role.
     *
     * @param subject the subject
     * @param role the role to check
     * @return a boolean indicating if the subject has the needed role
     */
    boolean hasRole(Subject subject, String role);

    /**
     * Asserts that the principals list provides the given permission.
     *
     * @param principals the principals list
     * @param permission the permission to check
     * @throws SecurityException if the subject does not have the needed permission
     */
    void checkPermission(List<String> principals, String permission);

    /**
     * Check whether the principals list provides the given permission.
     *
     * @param principals the principals list
     * @param permission the permission to check
     * @return a boolean indicating if the subject has the needed permission
     */
    boolean isPermitted(List<String> principals, String permission);

    /**
     * Asserts that the principals list has the given role.
     *
     * @param principals the principals list
     * @param role the role to check
     * @throws SecurityException if the subject does not have the needed role
     */
    void checkRole(List<String> principals, String role);

    /**
     * Check whether the principals list has the given role.
     *
     * @param principals the principals list
     * @param role the role to check
     * @return a boolean indicating if the principals list has the needed role
     */
    boolean hasRole(List<String> principals, String role);

}
