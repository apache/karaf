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
package org.apache.karaf.obr.command;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "obr", name = "url-list", description = "Displays the repository URLs currently associated with the OBR service.")
@Service
public class ListUrlCommand extends ObrCommandSupport {

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    protected void doExecute(RepositoryAdmin admin) {

        ShellTable table = new ShellTable();
        table.column("Index");
        table.column("OBR URL");
        table.emptyTableText("No OBR repository URL");

        Repository[] repos = admin.listRepositories();
        if (repos != null) {
            for (int i = 0; i < repos.length; i++) {
                table.addRow().addContent(i, repos[i].getURI());
            }
        }

        table.print(System.out, !noFormat);
    }

}
