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

import java.util.List;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "obr", name = "list", description = "Lists OBR bundles, optionally providing the given packages.")
@Service
public class ListCommand extends ObrCommandSupport {

    @Argument(index = 0, name = "packages", description = "A list of packages separated by whitespaces.", required = false, multiValued = true)
    List<String> packages;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Override
    void doExecute(RepositoryAdmin admin) throws Exception {
        StringBuilder substr = new StringBuilder();

        if (packages != null) {
            for (String packageName : packages) {
                substr.append(" ");
                substr.append(packageName);
            }
        }

        String query;
        if ((substr == null) || (substr.length() == 0)) {
            query = "(|(presentationname=*)(symbolicname=*))";
        } else {
            query = "(|(presentationname=*" + substr + "*)(symbolicname=*" + substr + "*))";
        }
        Resource[] resources = admin.discoverResources(query);

        ShellTable table = new ShellTable();
        table.column("Name");
        table.column("Symbolic Name");
        table.column("Version");
        table.emptyTableText("No matching bundles");

        for (Resource resource : resources) {
            table.addRow().addContent(emptyIfNull(resource.getPresentationName()),
                    emptyIfNull(resource.getSymbolicName()),
                    emptyIfNull(resource.getVersion()));
        }

        table.print(System.out, !noFormat);
    }

    private String emptyIfNull(Object st) {
        return st == null ? "" : st.toString();
    }

}
