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

import static org.apache.karaf.jaas.modules.BackingEngine.GROUP_PREFIX;
import static org.apache.karaf.jaas.modules.PrincipalHelper.names;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;

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
        List<String> uaInfo = List.of(p.get("a").split(","));
        assertEquals(1, uaInfo.size());
        assertEquals("", uaInfo.get(0));

        // update empty password on existing user with no roles
        engine.addUserInternal("a", "pass1");
        uaInfo = List.of(p.get("a").split(","));
        assertEquals(1, uaInfo.size());
        assertEquals("pass1", uaInfo.get(0));

        // update password on existing user with no roles
        engine.addUserInternal("a", "pass2");
        uaInfo = List.of(p.get("a").split(","));
        assertEquals(1, uaInfo.size());
        assertEquals("pass2", uaInfo.get(0));

        // update password on existing user with roles and groups
        engine.addRole("a", "role1");
        engine.addGroup("a", "g1");
        engine.addGroupRole("g1", "role2");
        engine.addUserInternal("a", "pass3");
        uaInfo = List.of(p.get("a").split(","));
        assertEquals(3, uaInfo.size());
        assertThat(uaInfo, contains("pass3", "role1", getGroupRef("g1")));
    }

    private String getGroupRef(String groupName) {
        return GROUP_PREFIX + groupName;
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
        // non-existing user
        assertTrue(engine.listRoles(p, "a").isEmpty());

        // empty roles in groups
        p.put(getGroupRef("g1"), "");
        GroupPrincipal gpg1 = PrincipalHelper.getGroup(engine, "g1");
        assertTrue(engine.listRoles(gpg1).isEmpty());
        p.put(getGroupRef("g1"), ",,,");
        assertTrue(engine.listRoles(gpg1).isEmpty());

        // empty roles in users
        p.put("a", "pass1");
        UserPrincipal upa = PrincipalHelper.getUser(engine, "a");
        assertTrue(engine.listRoles(upa).isEmpty());
        p.put("a", "pass1,,,");
        assertTrue(engine.listRoles(upa).isEmpty());

        // duplicate role
        p.put("a", "pass1,role1,role1");
        List<RolePrincipal> roles = engine.listRoles(upa);
        assertEquals(1, roles.size());
        assertEquals("role1", roles.get(0).getName());

        // roles in nested group
        p.put("a", "pass1,role1,role1," + getGroupRef("g1"));
        p.put(getGroupRef("g1"), getGroupRef("g2"));
        p.put(getGroupRef("g2"), "role2");
        roles = engine.listRoles(upa);
        assertEquals(2, roles.size());
        assertThat(names(roles), containsInAnyOrder("role1", "role2"));
    }

    @Test
    public void addRoleTest() {
        // non-existing user
        engine.addRole("a", "role1");
        assertNull(p.get("a"));

        // empty info in users
        engine.addUser("a", "");
        engine.addRole("a", "role1");
        // user empty password is preserved
        List<String> uaInfo = List.of(p.get("a").split(","));
        assertEquals(2, uaInfo.size());
        assertThat(uaInfo, containsInAnyOrder("", "role1"));

        // duplicate role
        engine.addRole("a", "role1");
        uaInfo = List.of(p.get("a").split(","));
        assertEquals(2, uaInfo.size());
        assertThat(uaInfo, containsInAnyOrder("", "role1"));

        // empty info in groups
        engine.createGroup("g1");
        engine.addGroupRole("g1", "role2");
        List<String> g1Info = List.of(p.get(getGroupRef("g1")).split(","));
        assertEquals(1, g1Info.size());
        assertEquals("role2", g1Info.get(0));
    }

    @Test
    public void deleteRoleTest() {
        // non-existing user
        engine.deleteRole("a", "role1");
        assertNull(p.get("a"));

        // delete in group
        engine.createGroup("g1");
        engine.addGroupRole("g1", "role1");
        engine.addGroupRole("g1", "role2");
        engine.addGroupRole("g1", "role3");
        engine.deleteGroupRole("g1", "role1");
        List<String> g1Info = List.of(p.get(getGroupRef("g1")).split(","));
        assertEquals(2, g1Info.size());
        assertThat(g1Info, containsInAnyOrder("role2", "role3"));

        // delete in user
        engine.addUser("a", "");
        engine.addRole("a", "role4");
        engine.addRole("a", "role5");
        engine.addRole("a", "role6");
        engine.deleteRole("a", "role4");
        // user empty password is preserved
        List<String> uaInfo = List.of(p.get("a").split(","));
        assertEquals(3, uaInfo.size());
        assertThat(uaInfo, containsInAnyOrder("", "role5", "role6"));
    }

    @Test
    public void listGroupsTest() {
        // non-existing user
        assertTrue(engine.listGroups(p, "a").isEmpty());

        // duplicate group
        p.put("a", "pass1," + getGroupRef("g1") + "," + getGroupRef("g1"));
        UserPrincipal upa = PrincipalHelper.getUser(engine, "a");
        List<GroupPrincipal> groups = engine.listGroups(upa);
        assertEquals(1, groups.size());
        assertEquals("g1", groups.get(0).getName());

        // nested group
        p.put(getGroupRef("g1"), getGroupRef("g2"));
        groups = engine.listGroups(upa);
        assertEquals(2, groups.size());
        assertThat(names(groups), containsInAnyOrder("g1", "g2"));

        // duplicate nested group
        p.put(getGroupRef("g2"), getGroupRef("g3") + "," + getGroupRef("g3"));
        groups = engine.listGroups(upa);
        assertEquals(3, groups.size());
        assertThat(names(groups), containsInAnyOrder("g1", "g2", "g3"));
    }

    @Test
    public void addGroupTest() {
        // duplicate group
        engine.addUser("a", "");
        engine.addGroup("a", "g1");
        engine.addGroup("a", "g1");
        List<String> uaInfo = List.of(p.get("a").split(","));
        assertEquals(2, uaInfo.size());
        assertThat(uaInfo, containsInAnyOrder("", getGroupRef("g1")));
    }

    @Test
    public void deleteGroupTest() {
        // group has only 1 user reference
        engine.addUser("a", "");
        engine.addGroup("a", "g1");
        engine.deleteGroup("a", "g1");
        assertNull(p.get(getGroupRef("g1")));

        // group is referenced by multiple users
        engine.addGroup("a", "g1");
        engine.addUser("b", "");
        engine.addGroup("b", "g1");
        engine.deleteGroup("a", "g1");
        assertNotNull(p.get(getGroupRef("g1")));

        // group is referenced by other groups
        engine.addGroup("a", "g2");
        p.put(getGroupRef("g3"), getGroupRef("g2"));
        engine.deleteGroup("a", "g2");
        assertNotNull(p.get(getGroupRef("g2")));
    }

    @After
    public void cleanup() {
        if (!f.delete()) {
            fail("Could not delete temporary file: " + f);
        }
    }

}