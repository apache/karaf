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

import java.util.List;
import java.util.Map;

import org.apache.karaf.jdbc.command.JdbcCommandSupport;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "jdbc", name = "ds-list", description = "List the JDBC datasources")
@Service
public class ListCommand extends JdbcCommandSupport {

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("Name");
        table.column("Product");
        table.column("Version");
        table.column("URL");
        table.column("Status");

        List<String> datasources = this.getJdbcService().datasources();
        for (String dsName: datasources) {
            try {
                Map<String, String> info = this.getJdbcService().info(dsName);
                table.addRow().addContent(dsName, info.get("db.product"), info.get("db.version"), info.get("url"), "OK");
            } catch (Exception e) {
                table.addRow().addContent(dsName, "", "", "", "Error " + e.getMessage());
            }
        }

        table.print(System.out);
        return null;
    }

}
