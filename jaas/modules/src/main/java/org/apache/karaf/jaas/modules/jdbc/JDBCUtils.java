/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.jaas.modules.jdbc;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import javax.naming.InitialContext;
import javax.sql.DataSource;

public final class JDBCUtils {

    public static final String DATASOURCE = "datasource";
    public static final String JNDI = "jndi:";
    public static final String OSGI = "osgi:";

    private JDBCUtils() { }

    /**
     * Look up a datasource from the url. The datasource can be passed either as jndi name or bundles ldap filter.
     *
     * @param bc the bundle context.
     * @param url the datasource URL.
     * @return the {@link DataSource} object.
     * @throws Exception in case of datasource creation failure.
     */
    public static DataSource createDatasource(BundleContext bc, String url) throws Exception {
        Object ds = doCreateDatasource(bc, url);
        if (ds == null) {
            throw new Exception("Unable to create datasource for " + url);
        }
        return DataSource.class.cast(ds);
    }

    protected static Object doCreateDatasource(BundleContext bc, String url) throws Exception {
        url = (url != null) ? url.trim() : null;
        if (url == null || url.isEmpty()) {
            throw new Exception("Illegal datasource url format. Datasource URL cannot be null or empty.");
        } else if (url.startsWith(JNDI)) {
            String jndiName = url.substring(JNDI.length());
            // secure JNDI scheme
            URI uri = new URI(jndiName);
            String scheme = uri.getScheme();
            if (scheme == null || scheme.equals("java")) {
                throw new Exception("Unsupported JNDI URI: " + jndiName);
            }
            InitialContext ic = new InitialContext();
            try {
                return ic.lookup(jndiName);
            } finally {
                ic.close();
            }
        } else if (url.startsWith(OSGI)) {
            String osgiFilter = url.substring(OSGI.length());
            String clazz = null;
            String filter = null;
            String[] tokens = osgiFilter.split("/", 2);
            if (tokens.length > 0) {
                clazz = tokens[0];
            }
            if (tokens.length > 1) {
                filter = tokens[1];
            }
            ServiceReference<?>[] references = bc.getServiceReferences(clazz, filter);
            if (references != null) {
                ServiceReference<?> ref = references[0];
                Object ds = bc.getService(ref);
                bc.ungetService(ref);
                return ds;
            } else {
                throw new Exception("Unable to find service reference for datasource: " + clazz + "/" + filter);
            }
        } else {
            throw new Exception("Illegal datasource url format " + url);
        }
    }

    protected static int rawUpdate(DataSource dataSource, String query, String... params) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    statement.setString(i + 1, params[i]);
                }
                int res = statement.executeUpdate();
                if (!connection.getAutoCommit()) {
                    connection.commit();
                }
                return res;
            }
        }
    }

    protected static int rawUpdate(Connection connection, String query, String... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                statement.setString(i + 1, params[i]);
            }
            return statement.executeUpdate();
        }
    }

    protected static List<String> rawSelect(DataSource dataSource, String query, String... params) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return rawSelect(connection, query, params);
        }
    }

    protected static List<String> rawSelect(Connection connection, String query, String... params) throws SQLException {
        List<String> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                statement.setString(i + 1, params[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(resultSet.getString(1));
                }
            }
        }
        return results;
    }

}
