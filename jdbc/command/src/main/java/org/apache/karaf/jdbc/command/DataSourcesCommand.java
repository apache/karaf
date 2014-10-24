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

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.table.ShellTable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Command(scope = "jdbc", name = "datasources", description = "List the JDBC datasources")
public class DataSourcesCommand extends JdbcCommandSupport {

    public Object doExecute() throws Exception {
        ShellTable table = new ShellTable();

        table.column("Name");
        table.column("Product");
        table.column("Version");
        table.column("URL");
        table.column("Status");

        Map<String, Set<String>> datasources = this.getJdbcService().aliases();
        for (Map.Entry<String, Set<String>> entry : datasources.entrySet()) {
            StringBuilder ids = new StringBuilder();
            for (String id : entry.getValue()) {
                if (ids.length() > 0) {
                    ids.append(", ");
                }
                ids.append(id);
            }
            String id = ids.toString();
            try {
                Map<String, String> info = this.getJdbcService().info(entry.getKey());
                table.addRow().addContent(id, info.get("db.product"), info.get("db.version"), info.get("url"), "OK");
            } catch (Exception e) {
                table.addRow().addContent(id, "", "", "", "Error");
            }
        }

        table.print(System.out);

        return null;
    }

}
