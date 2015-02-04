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

import javax.management.MBeanException;
import javax.management.openmbean.TabularData;

import java.util.Map;

/**
 * JDBC MBean
 */
public interface JdbcMBean {

    /**
     * Get the list of JDBC datasources.
     *
     * @return a tabular data containing the list of JDBC datasources.
     * @throws MBeanException
     */
    TabularData getDatasources() throws MBeanException;

    /**
     * Create a JDBC datasource.
     *
     * @param name the JDBC datasource name
     * @param driverName org.osgi.driver.name of the DataSourceFactory to use
     * @param driverClass org.osgi.driver.class of the DataSourceFactory to use
     * @param databaseName name of the database to access
     * @param url JDBC URL
     * @param user Database username
     * @param password Database password
     * @throws MBeanException
     */
    void create(String name, String driverName, String driverClass, String databaseName, String url, String user, String password) throws MBeanException;

    /**
     * Delete a JDBC datasource.
     *
     * @param name the JDBC datasource name (the one used at creation time).
     * @throws MBeanException
     */
    void delete(String name) throws MBeanException;

    /**
     * Get details about a JDBC datasource.
     *
     * @param datasource the JDBC datasource name.
     * @return a map (property/value) containing JDBC datasource details.
     * @throws MBeanException
     */
    Map<String, String> info(String datasource) throws MBeanException;

    /**
     * Get the tables available on a JDBC datasource.
     *
     * @param datasource the JDBC datasource name.
     * @return a tabular data containg datasource tables.
     * @throws MBeanException
     */
    TabularData tables(String datasource) throws MBeanException;

    /**
     * Execute a SQL command on a JDBC datasource.
     *
     * @param datasource the JDBC datasource name.
     * @param command the SQL command to execute.
     * @throws MBeanException
     */
    void execute(String datasource, String command) throws MBeanException;

    /**
     * Execute a SQL query on a JDBC datasource.
     *
     * @param datasource the JDBC datasource name.
     * @param query the SQL query to execute.
     * @return a tabular data with the result of execute (columns/values).
     * @throws MBeanException
     */
    TabularData query(String datasource, String query) throws MBeanException;

}
