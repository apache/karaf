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
     * @param name Datasource name 
     * @param driverName Backend database type (osgi.jdbc.driver.name of DataSourceFactory)
     * @param url JDBC URL
     * @param user Database user name
     * @param password Database password
     * @param password2 
     */
    void create(String name, String driverName, String driverClass, String databaseName, String url, String user, String password) throws Exception;

    /**
     * Delete a JDBC datasource identified by a name. Works only
     * for datasources that have a corresponding configuration
     *
     * @param name Datasource name
     */
    void delete(String name) throws Exception;
    
    /**
     * List the JDBC DataSourceFactories available.
     *
     * @return a list of DataSourceFactory names
     */
    List<String> factoryNames() throws Exception;

    /**
     * List the JDBC datasources available.
     *
     * @return a list of datasources names
     */
    List<String> datasources() throws Exception;

    /**
     * Execute a SQL query on a given JDBC datasource.
     *
     * @param datasource the JDBC datasource name.
     * @param query the SQL query to execute.
     * @return the SQL query result (as a String).
     */
    Map<String, List<String>> query(String datasource, String query) throws Exception;

    /**
     * Execute a SQL command on a given JDBC datasource.
     *
     * @param datasource the JDBC datasource name.
     * @param command the SQL command to execute.
     */
    void execute(String datasource, String command) throws Exception;

    /**
     * List the tables available on a given JDBC datasource.
     *
     * @param datasource the JDBC datasource name.
     * @return the list of table names.
     */
    Map<String, List<String>> tables(String datasource) throws Exception;

    /**
     * Get detailed info about a JDBC datasource.
     *
     * @param datasource the JDBC datasource name.
     * @return a map of info (name/value).
     */
    Map<String, String> info(String datasource) throws Exception;

}
