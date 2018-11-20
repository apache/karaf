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

import java.util.List;
import java.util.Map;

import org.apache.karaf.jdbc.command.completers.DataSourcesNameCompleter;
import org.apache.karaf.jdbc.command.completers.SqlCompleter;
import org.apache.karaf.jdbc.command.parsing.JdbcParser;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Parsing;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "jdbc", name = "query", description = "Execute a SQL query on a JDBC datasource")
@Parsing(JdbcParser.class)
@Service
public class QueryCommand extends JdbcCommandSupport {

    @Argument(index = 0, name = "datasource", description = "The JDBC datasource name, its JNDI name or service.id to use", required = true, multiValued = false)
    @Completion(DataSourcesNameCompleter.class)
    String datasource;

    @Argument(index = 1, name = "query", description = "The SQL query to execute", required = true, multiValued = false)
    @Completion(SqlCompleter.class)
    String query;

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();

        Map<String, List<String>> map = this.getJdbcService().query(datasource, query);
        int rowCount = 0;
        for (String column : map.keySet()) {
            table.column(column);
            rowCount = map.get(column).size();
        }

        for (int i = 0; i < rowCount; i++) {
            Row row = table.addRow();
            for (String column : map.keySet()) {
                row.addContent(map.get(column).get(i));
            }
        }

        table.print(System.out);

        return null;
    }

}
