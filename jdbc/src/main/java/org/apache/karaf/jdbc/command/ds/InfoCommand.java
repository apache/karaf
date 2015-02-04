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
import org.apache.karaf.jdbc.command.completers.DataSourcesNameCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.Map;

@Command(scope = "jdbc", name = "ds-info", description = "Display details about a JDBC datasource")
@Service
public class InfoCommand extends JdbcCommandSupport {

    @Argument(index = 0, name = "datasource", description = "The JDBC datasource name", required = true, multiValued = false)
    @Completion(DataSourcesNameCompleter.class)
    String datasource;

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();

        table.column("Property");
        table.column("Value");

        Map<String, String> info = this.getJdbcService().info(datasource);
        for (String property : info.keySet()) {
            table.addRow().addContent(property, info.get(property));
        }

        table.print(System.out);

        return null;
    }

}
