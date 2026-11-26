/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.impl.console.osgi.secured;

import static org.apache.karaf.shell.impl.console.osgi.secured.SecuredSessionFactoryImpl.isFileAccessAllowed;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.function.Predicate;

import org.junit.Test;

public class SecuredSessionFactoryImplFileAccessTest {

    private static final String LOG = "file:///opt/karaf/data/log/karaf.log";
    private static final String USERS = "file:///opt/karaf/etc/users.properties";

    private static final Predicate<String> ADMIN = "admin"::equals;
    private static final Predicate<String> VIEWER = "viewer"::equals;

    private static Dictionary<String, Object> config(String... keyValues) {
        Dictionary<String, Object> config = new Hashtable<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            config.put(keyValues[i], keyValues[i + 1]);
        }
        return config;
    }

    @Test
    public void onlyAdminWithoutShellAcl() {
        assertTrue(isFileAccessAllowed(null, "cat", USERS, ADMIN));
        assertFalse(isFileAccessAllowed(null, "cat", USERS, VIEWER));
    }

    @Test
    public void onlyAdminWithoutFileRule() {
        // for example an etc/org.apache.karaf.command.acl.shell.cfg kept from an older version
        Dictionary<String, Object> config = config("nano", "admin", "exec", "admin");
        assertTrue(isFileAccessAllowed(config, "cat", USERS, ADMIN));
        assertFalse(isFileAccessAllowed(config, "cat", USERS, VIEWER));
    }

    @Test
    public void shippedRule() {
        Dictionary<String, Object> config = config(".file", "admin");
        assertTrue(isFileAccessAllowed(config, "cat", USERS, ADMIN));
        assertFalse(isFileAccessAllowed(config, "cat", USERS, VIEWER));
        assertFalse(isFileAccessAllowed(config, "tac", LOG, VIEWER));
    }

    @Test
    public void ruleCanOpenFilesToOtherRoles() {
        Dictionary<String, Object> config = config(".file", "admin,viewer");
        assertTrue(isFileAccessAllowed(config, "cat", USERS, VIEWER));
    }

    @Test
    public void argumentRule() {
        Dictionary<String, Object> config = config(".file", "admin", ".file[/.*file:///opt/karaf/data/log/.*/]", "viewer");
        assertTrue(isFileAccessAllowed(config, "less", LOG, VIEWER));
        assertFalse(isFileAccessAllowed(config, "cat", USERS, VIEWER));
        assertTrue(isFileAccessAllowed(config, "cat", USERS, ADMIN));
        assertFalse(isFileAccessAllowed(config, "cat", LOG, ADMIN));
    }
}
