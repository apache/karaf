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


import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;

public class PropertiesBackingEngineTest extends TestCase {

    public void testUserRoles() throws IOException {
        File f = File.createTempFile(getClass().getName(), ".tmp");
        try {
            Properties p = new Properties(f);

            PropertiesBackingEngine engine = new PropertiesBackingEngine(p);
            engine.addUser("a", "aa");
            assertEquals(1, engine.listUsers().size());
            UserPrincipal upa = engine.listUsers().iterator().next();
            assertEquals("a", upa.getName());
            engine.addUser("b", "bb");

            engine.addRole("a", "role1");
            engine.addRole("a", "role2");
            assertEquals(2, engine.listRoles(upa).size());

            boolean foundR1 = false;
            boolean foundR2 = false;
            for (RolePrincipal rp : engine.listRoles(upa)) {
                if ("role1".equals(rp.getName())) {
                    foundR1 = true;
                } else if ("role2".equals(rp.getName())) {
                    foundR2 = true;
                }
            }
            assertTrue(foundR1);
            assertTrue(foundR2);

            engine.addGroup("a", "g");
            engine.addGroupRole("g", "role2");
            engine.addGroupRole("g", "role3");
            engine.addGroup("b", "g");
            engine.addGroup("b", "g2");
            engine.addGroupRole("g2", "role4");

            assertEquals(3, engine.listRoles(upa).size());
            boolean foundR1_2 = false;
            boolean foundR2_2 = false;
            boolean foundR3_2 = false;
            for (RolePrincipal rp : engine.listRoles(upa)) {
                if ("role1".equals(rp.getName())) {
                    foundR1_2 = true;
                } else if ("role2".equals(rp.getName())) {
                    foundR2_2 = true;
                } else if ("role3".equals(rp.getName())) {
                    foundR3_2 = true;
                }
            }
            assertTrue(foundR1_2);
            assertTrue(foundR2_2);
            assertTrue(foundR3_2);

            // Check that the loading works
            PropertiesBackingEngine engine2 = new PropertiesBackingEngine(new Properties(f));
            assertEquals(2, engine2.listUsers().size());
            UserPrincipal upa_2 = null;
            UserPrincipal upb_2 = null;
            for (UserPrincipal u : engine2.listUsers()) {
                if ("a".equals(u.getName())) {
                    upa_2 = u;
                } else if ("b".equals(u.getName())) {
                    upb_2 = u;
                }
            }
            assertEquals(3, engine2.listRoles(upa_2).size());
            boolean foundR1_3 = false;
            boolean foundR2_3 = false;
            boolean foundR3_3 = false;
            for (RolePrincipal rp : engine2.listRoles(upa_2)) {
                if ("role1".equals(rp.getName())) {
                    foundR1_3 = true;
                } else if ("role2".equals(rp.getName())) {
                    foundR2_3 = true;
                } else if ("role3".equals(rp.getName())) {
                    foundR3_3 = true;
                }
            }
            assertTrue(foundR1_3);
            assertTrue(foundR2_3);
            assertTrue(foundR3_3);
            assertEquals(3, engine2.listRoles(upb_2).size());
            boolean foundR2_4 = false;
            boolean foundR3_4 = false;
            boolean foundR4_4 = false;
            for (RolePrincipal rp : engine2.listRoles(upb_2)) {
                if ("role2".equals(rp.getName())) {
                    foundR2_4 = true;
                } else if ("role3".equals(rp.getName())) {
                    foundR3_4 = true;
                } else if ("role4".equals(rp.getName())) {
                    foundR4_4 = true;
                }
            }
            assertTrue(foundR2_4);
            assertTrue(foundR3_4);
            assertTrue(foundR4_4);

            // Let's start removing some things...
            UserPrincipal upb = null;
            for (UserPrincipal up : engine.listUsers()) {
                if ("b".equals(up.getName())) {
                    upb = up;
                }
            }
            assertEquals(1, engine.listGroups(upa).size());
            assertEquals(2, engine.listGroups(upb).size());

            GroupPrincipal gp = engine.listGroups(upa).iterator().next();
            engine.deleteGroupRole("g", "role2");
            assertEquals(1, engine.listRoles(gp).size());
            assertEquals("role3", engine.listRoles(gp).iterator().next().getName());

            // Check that the user roles are reported correctly
            assertEquals("role2 should still be there as it was added to the user directly too",
                    3, engine.listRoles(upa).size());
            boolean foundR1_5 = false;
            boolean foundR2_5 = false;
            boolean foundR3_5 = false;
            for (RolePrincipal rp : engine.listRoles(upa)) {
                if ("role1".equals(rp.getName())) {
                    foundR1_5 = true;
                } else if ("role2".equals(rp.getName())) {
                    foundR2_5 = true;
                } else if ("role3".equals(rp.getName())) {
                    foundR3_5 = true;
                }
            }
            assertTrue(foundR1_5);
            assertTrue(foundR2_5);
            assertTrue(foundR3_5);
            assertEquals(2, engine.listRoles(upb).size());
            boolean foundR3_6 = false;
            boolean foundR4_6 = false;
            for (RolePrincipal rp : engine.listRoles(upb)) {
                if ("role3".equals(rp.getName())) {
                    foundR3_6 = true;
                } else if ("role4".equals(rp.getName())) {
                    foundR4_6 = true;
                }
            }
            assertTrue(foundR3_6);
            assertTrue(foundR4_6);

            engine.deleteGroup("b", "g");
            engine.deleteGroup("b", "g2");
            assertEquals(0, engine.listRoles(upb).size());
            engine.deleteUser("b");

            engine.deleteUser("a");
            assertEquals("Properties should be empty now", 0, p.size());
        } finally {
            if (!f.delete()) {
                fail("Could not delete temporary file: " + f);
            }
        }
    }
}
