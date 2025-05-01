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
package org.apache.karaf.jaas.modules;

import static org.apache.karaf.jaas.modules.PrincipalHelper.names;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.properties.PropertiesBackingEngine;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class AbstractPropertiesBackingEngineTest {

    private File f;
    private Properties p;
    private PropertiesBackingEngine engine;

    @Before
    public void start() throws IOException {
        f = File.createTempFile(getClass().getName(), ".tmp");
        p = new Properties(f);
        engine = new PropertiesBackingEngine(p);
    }

    @Test
    public void addUserInternalTest() {
        // non-existing user
        engine.addUserInternal("a", "");

        // update empty password on existing user with no roles
        engine.addUserInternal("a", "pass1");
        assertThat(List.of(p.get("a").split(",")), contains("pass1"));
        UserPrincipal upa = PrincipalHelper.getUser(engine, "a");
        assertTrue(engine.listGroups(upa).isEmpty());
        assertTrue(engine.listRoles(upa).isEmpty());

        // update password on existing user with no roles
        engine.addUserInternal("a", "pass2");
        assertThat(List.of(p.get("a").split(",")), contains("pass2"));
        upa = PrincipalHelper.getUser(engine, "a");
        assertTrue(engine.listGroups(upa).isEmpty());
        assertTrue(engine.listRoles(upa).isEmpty());

        // update password on existing user with roles
        engine.addRole("a", "role1");
        engine.addGroup("a", "g1");
        engine.addGroupRole("g1", "role2");
        engine.addUserInternal("a", "pass3");
        assertThat(List.of(p.get("a").split(",")),
                contains("pass3", "role1", PropertiesBackingEngine.GROUP_PREFIX + "g1"));
        upa = PrincipalHelper.getUser(engine, "a");
        assertThat(names(engine.listGroups(upa)), containsInAnyOrder("g1"));
        assertThat(names(engine.listRoles(upa)), containsInAnyOrder("role1", "role2"));
    }

    @Test
    public void lookupUserTest() {
        engine.addUser("a", "pass1");
        engine.addGroup("a", "g1");

        assertEquals("a", engine.lookupUser("a").getName());
        assertNull(engine.lookupUser("g1"));
        assertNull(engine.lookupUser("test"));
    }

    @Test
    public void listRolesTest() {
        // empty roles in groups
        p.put(PropertiesBackingEngine.GROUP_PREFIX + "g1", ",,,"); // simulate manual editing of the properties file
        GroupPrincipal gpg1 = PrincipalHelper.getGroup(engine, "g1");
        assertTrue(engine.listRoles(gpg1).isEmpty());

        // empty roles in users
        p.put("a", "pass1,,,");
        UserPrincipal upa = PrincipalHelper.getUser(engine, "a");
        assertTrue(engine.listRoles(upa).isEmpty());

        // duplicate role
        p.put("a", "pass1,role1,role1");
        upa = PrincipalHelper.getUser(engine, "a");
        List<RolePrincipal> roles = engine.listRoles(upa);
        assertEquals(1, roles.size());
        assertEquals("role1", roles.get(0).getName());
    }

    @Test
    public void addRoleTest() {
        // empty info in groups
        engine.createGroup("g1");
        engine.addGroupRole("g1", "role1");
        GroupPrincipal gpg1 = PrincipalHelper.getGroup(engine, "g1");
        assertThat(names(engine.listRoles(gpg1)), contains("role1"));

        // empty info in users
        engine.addUser("a", "");
        engine.addRole("a", "role2");
        UserPrincipal upa = PrincipalHelper.getUser(engine, "a");
        assertThat(names(engine.listRoles(upa)), contains("role2"));
        // user empty password is preserved
        assertThat(List.of(p.get("a").split(",")), containsInAnyOrder("", "role2"));

        // duplicate role
        engine.addRole("a", "role2");
        upa = PrincipalHelper.getUser(engine, "a");
        assertThat(names(engine.listRoles(upa)), contains("role2"));
        assertThat(List.of(p.get("a").split(",")), containsInAnyOrder("", "role2"));

        // duplicate group
        engine.addUser("b", "");
        engine.addGroup("b", "g2");
        engine.addGroup("b", "g2");
        UserPrincipal upb = PrincipalHelper.getUser(engine, "b");
        assertThat(names(engine.listGroups(upb)), contains("g2"));
        assertThat(List.of(p.get("b").split(",")),
                containsInAnyOrder("", PropertiesBackingEngine.GROUP_PREFIX + "g2"));
    }

    @Test
    public void deleteRoleTest() {
        // delete in group
        engine.createGroup("g1");
        engine.addGroupRole("g1", "role1");
        engine.addGroupRole("g1", "role2");
        engine.addGroupRole("g1", "role3");
        engine.deleteGroupRole("g1", "role1");
        GroupPrincipal gpg1 = PrincipalHelper.getGroup(engine, "g1");
        assertThat(names(engine.listRoles(gpg1)), containsInAnyOrder("role2", "role3"));

        // delete in user
        engine.addUser("a", "");
        engine.addRole("a", "role4");
        engine.addRole("a", "role5");
        engine.addRole("a", "role6");
        engine.deleteRole("a", "role4");
        UserPrincipal upa = PrincipalHelper.getUser(engine, "a");
        assertThat(names(engine.listRoles(upa)), containsInAnyOrder("role5", "role6"));
        // user empty password is preserved
        assertThat(List.of(p.get("a").split(",")), containsInAnyOrder("", "role5", "role6"));
    }

    @After
    public void cleanup() {
        if (!f.delete()) {
            fail("Could not delete temporary file: " + f);
        }
    }

}