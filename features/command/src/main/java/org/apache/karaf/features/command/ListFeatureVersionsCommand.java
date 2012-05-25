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
package org.apache.karaf.features.command;

import java.util.Arrays;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.table.ShellTable;

@Command(scope = "feature", name = "version-list", description = "Lists all versions of a feature available from the currently available repositories.")
public class ListFeatureVersionsCommand extends FeaturesCommandSupport {

	@Argument(index = 0, name = "feature", description = "Name of feature.", required = true, multiValued = false)
	String feature;

    protected void doExecute(FeaturesService admin) throws Exception {
        ShellTable table = new ShellTable();
        table.column("Version");
        table.column("Repository");
        table.column("Repository URL");
        table.emptyTableText("No versions available for features '" + feature + "'");
             
        for (Repository r : Arrays.asList(admin.listRepositories())) {
            for (Feature f : r.getFeatures()) {

                if (f.getName().equals(feature)) {
                    table.addRow().addContent(f.getVersion(), r.getName(), r.getURI());
                }
            }
        }

        table.print(System.out);
    }

}
