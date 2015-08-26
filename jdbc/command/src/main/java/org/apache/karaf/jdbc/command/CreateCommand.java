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
package org.apache.karaf.jdbc.command;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(scope = "jdbc", name = "create", description = "Create a JDBC datasource")
public class CreateCommand extends JdbcCommandSupport {

    @Argument(index = 0, name = "name", description = "The JDBC datasource name", required = true, multiValued = false)
    String name;

    @Option(name = "-t", aliases = { "--type" }, description = "The JDBC datasource type (generic, MySQL, MSSQL, Oracle, Postgres, H2, HSQL, Derby)", required = false, multiValued = false)
    String type;

    @Option(name = "-d", aliases = { "--driver" }, description = "The classname of the JDBC driver to use. NB: this option is used only the type generic", required = false, multiValued = false)
    String driver;

    @Option(name = "-v", aliases = { "--version" }, description = "The version of the driver to use", required = false, multiValued = false)
    String version;

    @Option(name = "-url", description = "The JDBC URL to use", required = false, multiValued = false)
    String url;

    @Option(name = "-u", aliases = { "--username" }, description = "The database username", required = false, multiValued = false)
    String username;

    @Option(name = "-p", aliases = { "--password" }, description = "The database password", required = false, multiValued = false)
    String password;
    
    @Option(name = "-sn", aliases = { "--servername" }, description = "The server name of the database machine, applicable for MSSQL only", required = false, multiValued = false)
    String servername;
    
    @Option(name = "-dbn", aliases = { "--databasename" }, description = "The database name, applicable for MSSQL only", required = false, multiValued = false)
    String databasename;
    
    @Option(name = "-ptn", aliases = { "--portnumber" }, description = "The portnumber for MS SQL SERVER, applicable for MSSQL only", required = false, multiValued = false)
    String portnumber = "1433";

    @Option(name = "-i", aliases = { "--install-bundles" }, description = "Try to install the bundles providing the JDBC driver", required = false, multiValued = false)
    boolean installBundles = false;

    public Object doExecute() throws Exception {
        if (type.equals("MSSQL")) {
            this.getJdbcService().create(name, type, driver, version, username, password, servername, databasename, portnumber, installBundles);
        } else {
            this.getJdbcService().create(name, type, driver, version, url, username, password, installBundles);
        }
        return null;
    }

}
