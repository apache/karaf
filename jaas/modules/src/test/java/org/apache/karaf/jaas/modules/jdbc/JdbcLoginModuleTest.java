/*
 *
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
package org.apache.karaf.jaas.modules.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.sql.DataSource;

import org.apache.derby.jdbc.EmbeddedDataSource40;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.NamePasswordCallbackHandler;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JdbcLoginModuleTest {

    private EmbeddedDataSource40 dataSource;
    private Map<String, Object> options;

    @Before
    public void setUp() throws Exception {
        System.setProperty("derby.stream.error.file", "target/derby.log");

        // Create datasource
        dataSource = new EmbeddedDataSource40();
        dataSource.setDatabaseName("memory:db");
        dataSource.setCreateDatabase("create");

        // Delete tables
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("drop table USERS");
                }
            } catch (SQLException e) {
                // Ignore
            }
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("drop table ROLES");
                }
            } catch (SQLException e) {
                // Ignore
            }
            connection.commit();
        }

        // Create tables
        try (Connection connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("create table USERS (USERNAME VARCHAR(32) PRIMARY KEY, PASSWORD VARCHAR(32))");
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("create table ROLES (USERNAME VARCHAR(32), ROLE VARCHAR(1024))");
            }
            connection.commit();
        }

        // Mocks
        BundleContext context = EasyMock.createMock(BundleContext.class);
        ServiceReference reference = EasyMock.createMock(ServiceReference.class);

        // Create options
        options = new HashMap<>();
        options.put(JDBCUtils.DATASOURCE, "osgi:" + DataSource.class.getName());
        options.put(BundleContext.class.getName(), context);

        expect(context.getServiceReferences(DataSource.class.getName(), null)).andReturn(new ServiceReference[] { reference });
        expect((DataSource)context.getService(reference)).andReturn(dataSource);
        expect(context.ungetService(reference)).andReturn(true);

        EasyMock.replay(context);
    }

    @Test
    public void testLoginModule() throws Exception {
        JDBCBackingEngine engine = new JDBCBackingEngine(dataSource);
        engine.addUser("abc", "xyz");
        engine.addRole("abc", "role1");

        JDBCLoginModule module = new JDBCLoginModule();

        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("abc", "xyz"), null, options);

        module.login();
        module.commit();

        assertFalse(subject.getPrincipals(UserPrincipal.class).isEmpty());
        assertEquals("abc", subject.getPrincipals(UserPrincipal.class).iterator().next().getName());
        assertFalse(subject.getPrincipals(RolePrincipal.class).isEmpty());
        assertEquals("role1", subject.getPrincipals(RolePrincipal.class).iterator().next().getName());
    }

    @Test
    public void testLoginModuleWithGroups() throws Exception {
        JDBCBackingEngine engine = new JDBCBackingEngine(dataSource);
        engine.addGroupRole("group1", "role2");
        engine.addUser("abc", "xyz");
        engine.addRole("abc", "role1");
        engine.addGroup("abc", "group1");

        JDBCLoginModule module = new JDBCLoginModule();

        Subject subject = new Subject();
        module.initialize(subject, new NamePasswordCallbackHandler("abc", "xyz"), null, options);

        module.login();
        module.commit();

        assertTrue(subject.getPrincipals().contains(new UserPrincipal("abc")));
        assertTrue(subject.getPrincipals().contains(new GroupPrincipal("group1")));
        assertTrue(subject.getPrincipals().contains(new RolePrincipal("role1")));
        assertTrue(subject.getPrincipals().contains(new RolePrincipal("role2")));
    }

    @Test
    public void testEngine() throws Exception {
        JDBCBackingEngine engine = new JDBCBackingEngine(dataSource);

        assertTrue(engine.listUsers().isEmpty());

        engine.addUser("abc", "xyz");

        assertTrue(engine.listUsers().contains(new UserPrincipal("abc")));
        assertTrue(engine.listRoles(new UserPrincipal("abc")).isEmpty());
        assertTrue(engine.listRoles(new GroupPrincipal("group1")).isEmpty());
        assertTrue(engine.listGroups(new UserPrincipal("abc")).isEmpty());

        engine.addRole("abc", "role1");

        assertTrue(engine.listUsers().contains(new UserPrincipal("abc")));
        assertTrue(engine.listRoles(new UserPrincipal("abc")).contains(new RolePrincipal("role1")));
        assertTrue(engine.listRoles(new GroupPrincipal("group1")).isEmpty());
        assertTrue(engine.listGroups(new UserPrincipal("abc")).isEmpty());

        engine.addGroupRole("group1", "role2");

        assertTrue(engine.listUsers().contains(new UserPrincipal("abc")));
        assertTrue(engine.listRoles(new UserPrincipal("abc")).contains(new RolePrincipal("role1")));
        assertTrue(engine.listRoles(new GroupPrincipal("group1")).contains(new RolePrincipal("role2")));
        assertTrue(engine.listGroups(new UserPrincipal("abc")).isEmpty());

        engine.addGroup("abc", "group1");

        assertTrue(engine.listUsers().contains(new UserPrincipal("abc")));
        assertTrue(engine.listRoles(new UserPrincipal("abc")).contains(new RolePrincipal("role1")));
        assertTrue(engine.listRoles(new UserPrincipal("abc")).contains(new RolePrincipal("role2")));
        assertTrue(engine.listRoles(new GroupPrincipal("group1")).contains(new RolePrincipal("role2")));
        assertTrue(engine.listGroups(new UserPrincipal("abc")).contains(new GroupPrincipal("group1")));

        engine.deleteRole("abc", "role1");

        assertTrue(engine.listUsers().contains(new UserPrincipal("abc")));
        assertTrue(engine.listRoles(new UserPrincipal("abc")).contains(new RolePrincipal("role2")));
        assertTrue(engine.listRoles(new GroupPrincipal("group1")).contains(new RolePrincipal("role2")));
        assertTrue(engine.listGroups(new UserPrincipal("abc")).contains(new GroupPrincipal("group1")));

        engine.deleteGroupRole("group1", "role2");

        assertTrue(engine.listUsers().contains(new UserPrincipal("abc")));
        assertTrue(engine.listRoles(new UserPrincipal("abc")).isEmpty());
        assertTrue(engine.listRoles(new GroupPrincipal("group1")).isEmpty());
        assertTrue(engine.listGroups(new UserPrincipal("abc")).contains(new GroupPrincipal("group1")));

        engine.addGroupRole("group1", "role3");

        assertTrue(engine.listUsers().contains(new UserPrincipal("abc")));
        assertTrue(engine.listRoles(new UserPrincipal("abc")).contains(new RolePrincipal("role3")));
        assertTrue(engine.listRoles(new GroupPrincipal("group1")).contains(new RolePrincipal("role3")));
        assertTrue(engine.listGroups(new UserPrincipal("abc")).contains(new GroupPrincipal("group1")));

        engine.deleteGroup("abc", "group1");

        assertTrue(engine.listUsers().contains(new UserPrincipal("abc")));
        assertTrue(engine.listRoles(new UserPrincipal("abc")).isEmpty());
        assertTrue(engine.listRoles(new GroupPrincipal("group1")).isEmpty());
        assertTrue(engine.listGroups(new UserPrincipal("abc")).isEmpty());

        engine.deleteUser("abc");

        assertTrue(engine.listUsers().isEmpty());
        assertTrue(engine.listRoles(new UserPrincipal("abc")).isEmpty());
        assertTrue(engine.listRoles(new GroupPrincipal("group1")).isEmpty());
        assertTrue(engine.listGroups(new UserPrincipal("abc")).isEmpty());
    }
}
