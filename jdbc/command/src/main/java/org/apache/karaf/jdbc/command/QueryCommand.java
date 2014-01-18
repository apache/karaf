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

import java.util.List;
import java.util.Map;

@Command(scope = "jdbc", name = "query", description = "Execute a SQL query on a JDBC datasource")
public class QueryCommand extends JdbcCommandSupport {

    @Argument(index = 0, name = "datasource", description = "The JDBC datasource to use", required = true, multiValued = false)
    String datasource;

    @Argument(index = 1, name = "query", description = "The SQL query to execute", required = true, multiValued = false)
    String query;

    public Object doExecute() throws Exception {

        Map<String, List<String>> map = this.getJdbcService().query(datasource, query);
        int rowCount = 0;
        for (String column : map.keySet()) {
            System.out.print(column + "\t");
            rowCount = map.get(column).size();
        }

        System.out.println("");

        for (int i = 0; i < rowCount; i++) {
            for (String column : map.keySet()) {
                System.out.print(map.get(column).get(i) + "\t");
            }
            System.out.println("");
        }

        return null;
    }

}
