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
package org.apache.karaf.jdbc.command.ds;

import org.apache.karaf.jdbc.command.JdbcCommandSupport;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "jdbc", name = "ds-create", description = "Create a JDBC datasource config for pax-jdbc-config from a DataSourceFactory")
@Service
public class CreateCommand extends JdbcCommandSupport {
    @Argument(index = 0, name = "name", description = "The JDBC datasource name", required = true, multiValued = false)
    String name;
    
    @Option(name = "-dn", aliases = { "--driverName" }, description = "org.osgi.driver.name property of the DataSourceFactory", required = false, multiValued = false)
    String driverName;
    
    @Option(name = "-dc", aliases = { "--driverClass" }, description = "org.osgi.driver.class property  of the DataSourceFactory", required = false, multiValued = false)
    String driverClass;

    @Option(name = "-dbName", description = "Database name to use", required = false, multiValued = false)
    String databaseName;
    
    @Option(name = "-url", description = "The JDBC URL to use", required = false, multiValued = false)
    String url;

    @Option(name = "-u", aliases = { "--username" }, description = "The database username", required = false, multiValued = false)
    String username;

    @Option(name = "-p", aliases = { "--password" }, description = "The database password", required = false, multiValued = false)
    String password;

    @Override
    public Object execute() throws Exception {
        this.getJdbcService().create(name, driverName, driverClass, databaseName, url, username, password);
        return null;
    }

}
