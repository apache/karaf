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
package org.apache.karaf.jdbc;

import java.util.List;
import java.util.Map;

/**
 * JDBC Service.
 */
public interface JdbcService {

    /**
     * Create a JDBC datasource configuration.
     *
     * @param name The datasource name.
     * @param driverName The backend database type (osgi.jdbc.driver.name of DataSourceFactory).
     * @param driverClass The JDBC driver class.
     * @param databaseName The database name.
     * @param url The JDBC URL.
     * @param user The database user name.
     * @param password The database password.
     * @param databaseType The database type (ConnectionPoolDataSource, XADataSource or DataSource).
     * @throws Exception If the service fails.
     */
    void create(String name, String driverName, String driverClass, String databaseName, String url, String user, String password, String databaseType) throws Exception;

    /**
     * Delete a JDBC datasource identified by a name. Works only
     * for datasources that have a corresponding configuration
     *
     * @param name The datasource name to delete.
     * @throws Exception If the service fails.
     */
    void delete(String name) throws Exception;
    
    /**
     * List the JDBC DataSourceFactories available.
     *
     * @return a {@link List} of DataSourceFactory names.
     * @throws Exception If the service fails.
     */
    List<String> factoryNames() throws Exception;

    /**
     * List the JDBC datasources available.
     *
     * @return A {@link List} of datasources names.
     * @throws Exception If the service fails.
     */
    List<String> datasources() throws Exception;

    /**
     * List service IDs of JDBC datasource OSGi services available
     *
     * @return A {@link List} of datasources OSGi service IDs.
     * @throws Exception If the service fails.
     */
    List<Long> datasourceServiceIds() throws Exception;

    /**
     * Execute a SQL query on a given JDBC datasource.
     *
     * @param datasource The JDBC datasource name.
     * @param query The SQL query to execute.
     * @return The SQL query result (as a {@link Map}).
     * @throws Exception If the service fails.
     */
    Map<String, List<String>> query(String datasource, String query) throws Exception;

    /**
     * Execute a SQL command on a given JDBC datasource.
     *
     * @param datasource The JDBC datasource name.
     * @param command The SQL command to execute.
     * @throws Exception If the service fails.
     */
    void execute(String datasource, String command) throws Exception;

    /**
     * List the tables available on a given JDBC datasource.
     *
     * @param datasource the JDBC datasource name.
     * @return A {@link Map} containing the tables.
     * @throws Exception If the service fails.
     */
    Map<String, List<String>> tables(String datasource) throws Exception;

    /**
     * Get detailed info about a JDBC datasource.
     *
     * @param datasource The JDBC datasource name.
     * @return A {@link Map} of info (name/value).
     * @throws Exception If the service fails.
     */
    Map<String, String> info(String datasource) throws Exception;

}
