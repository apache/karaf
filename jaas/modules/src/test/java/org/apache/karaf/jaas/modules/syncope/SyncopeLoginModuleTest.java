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
package org.apache.karaf.jaas.modules.syncope;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

public class SyncopeLoginModuleTest {

    @Test
    public void testRolesExtractionSyncope1() throws Exception {
        String syncopeResponse = read("syncope1Response.xml");
        SyncopeLoginModule syncopeLoginModule = new SyncopeLoginModule();
        List<String> roles = syncopeLoginModule.extractingRolesSyncope1(syncopeResponse);
        assertThat(roles, contains("admin", "another"));
    }

    @Test
    public void testRolesExtractionSyncope2() throws Exception {
        String syncopeResponse = read("syncope2Response.json");
        SyncopeLoginModule syncopeLoginModule = new SyncopeLoginModule();
        Map<String, String> options = Collections.singletonMap(SyncopeLoginModule.USE_ROLES_FOR_SYNCOPE2, "true");
        syncopeLoginModule.initialize(null, null, Collections.emptyMap(), options);
        List<String> roles = syncopeLoginModule.extractingRolesSyncope2(syncopeResponse);
        assertThat(roles, contains("admin", "another"));
    }

    @Test
    public void testGroupsExtractionSyncope2() throws Exception {
        String syncopeResponse = read("syncope2Response.json");
        SyncopeLoginModule syncopeLoginModule = new SyncopeLoginModule();
        List<String> roles = syncopeLoginModule.extractingRolesSyncope2(syncopeResponse);
        assertThat(roles, contains("manager"));
    }

    private String read(String resourceName) throws URISyntaxException, IOException {
        URI response = this.getClass().getResource(resourceName).toURI();
        return Files.lines(Paths.get(response), StandardCharsets.UTF_8)
            .collect(Collectors.joining("\n"));
    }

}
