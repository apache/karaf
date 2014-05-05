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

import org.apache.felix.gogo.commands.Command;

import java.util.List;
import java.util.Map;

@Command(scope = "jdbc", name = "datasources", description = "List the JDBC datasources")
public class DataSourcesCommand extends JdbcCommandSupport {

    private final static String JDBC_DATASOURCES_STRING_FORMAT = "%10s %15s %10s %45s %5s";

    public Object doExecute() throws Exception {

        System.out.println(String.format(JDBC_DATASOURCES_STRING_FORMAT, "Name", "Product", "Version", "URL", "Status"));

        List<String> datasources = this.getJdbcService().datasources();
        for (String datasource : datasources) {
            try {
                Map<String, String> info = this.getJdbcService().info(datasource);
                System.out.println(String.format(JDBC_DATASOURCES_STRING_FORMAT, datasource, info.get("db.product"), info.get("db.version"), info.get("url"), "OK"));
            } catch (Exception e) {
                System.out.println(String.format(JDBC_DATASOURCES_STRING_FORMAT, datasource, "", "", "", "Error"));
            }
        }

        return null;
    }

}
