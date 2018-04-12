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
package org.apache.karaf.jaas.modules.properties;

import static org.apache.karaf.jaas.modules.PrincipalHelper.names;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PropertiesBackingEngineTest {
    private File f;

    @Before
    public void start() throws IOException {
        f = File.createTempFile(getClass().getName(), ".tmp");
    }

    @Test
    public void testUserRoles() throws IOException {
        Properties p = new Properties(f);

        PropertiesBackingEngine engine = new PropertiesBackingEngine(p);
        engine.addUser("a", "aa");
        engine.addUser("b", "bb");

        engine.addRole("a", "role1");
        engine.addRole("a", "role2");
        
        UserPrincipal upa = getUser(engine, "a");
        Assert.assertThat(names(engine.listRoles(upa)), containsInAnyOrder("role1", "role2"));

        engine.addGroup("a", "g");
        engine.addGroupRole("g", "role2");
        engine.addGroupRole("g", "role3");
        engine.addGroup("b", "g");
        engine.addGroup("b", "g2");
        engine.addGroupRole("g2", "role4");

        Assert.assertThat(names(engine.listUsers()), containsInAnyOrder("a", "b"));
        Assert.assertThat(names(engine.listRoles(upa)), containsInAnyOrder("role1", "role2", "role3"));

        checkLoading();

        assertNotNull(engine.lookupUser("a"));
        assertEquals("a", engine.lookupUser("a").getName());

        // removing some stuff
        UserPrincipal upb = getUser(engine, "b");
        assertEquals(1, engine.listGroups(upa).size());
        assertEquals(2, engine.listGroups(upb).size());

        GroupPrincipal gp = engine.listGroups(upa).iterator().next();
        engine.deleteGroupRole("g", "role2");
        Assert.assertThat(names(engine.listRoles(gp)), containsInAnyOrder("role3"));

        // role2 should still be there as it was added to the user directly too
        Assert.assertThat(names(engine.listRoles(upa)), containsInAnyOrder("role1", "role2", "role3"));
        Assert.assertThat(names(engine.listRoles(upb)), containsInAnyOrder("role3", "role4"));

        engine.deleteGroup("b", "g");
        engine.deleteGroup("b", "g2");
        assertEquals(0, engine.listRoles(upb).size());

        engine.deleteUser("b");
        engine.deleteUser("a");
        assertEquals("Properties should be empty now", 0, p.size());
    }

    private void checkLoading() throws IOException {
        PropertiesBackingEngine engine = new PropertiesBackingEngine(new Properties(f));
        assertEquals(2, engine.listUsers().size());
        UserPrincipal upa_2 = getUser(engine, "a");
        UserPrincipal upb_2 = getUser(engine, "b");
 
        assertEquals(3, engine.listRoles(upa_2).size());
        Assert.assertThat(names(engine.listRoles(upa_2)), containsInAnyOrder("role1", "role2", "role3"));

        assertEquals(3, engine.listRoles(upb_2).size());
        Assert.assertThat(names(engine.listRoles(upb_2)), containsInAnyOrder("role2", "role3", "role4"));
    }
    
    private UserPrincipal getUser(PropertiesBackingEngine engine, String name) {
        List<UserPrincipal> matchingUsers = engine.listUsers().stream()
            .filter(user->name.equals(user.getName())).collect(Collectors.toList());
        Assert.assertFalse("User with name " + name + " was not found", matchingUsers.isEmpty());
        return matchingUsers.iterator().next();
    }

    @After
    public void cleanup() {
        if (!f.delete()) {
            fail("Could not delete temporary file: " + f);
        }
    }

}
