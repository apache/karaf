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
import org.apache.karaf.jdbc.command.completers.DataSourcesFileNameCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "jdbc", name = "ds-delete", description = "Delete a JDBC datasource")
@Service
public class DeleteCommand extends JdbcCommandSupport {

    @Argument(index = 0, name = "name", description = "The JDBC datasource name (the one used at creation time)", required = true, multiValued = false)
    @Completion(DataSourcesFileNameCompleter.class)
    String name;

    @Override
    public Object execute() throws Exception {
        this.getJdbcService().delete(name);
        return null;
    }

}
