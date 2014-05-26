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

import java.util.Map;
import java.util.Set;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "feature", name = "requirement-list", description = "List provisioning requirements.")
@Service
public class RequirementList implements Action {

    @Reference
    private FeaturesService featuresService;

    @Option(name = "--no-format", description = "Disable table rendered output")
    boolean noFormat;

    @Override
    public Object execute() throws Exception {
        Map<String, Set<String>> requirements = featuresService.listRequirements();

        ShellTable table = new ShellTable();
        table.column("Region");
        table.column("Requirement");
        table.emptyTableText("No requirements defined");

        for (Map.Entry<String, Set<String>> entry : requirements.entrySet()) {
            for (String requirement : entry.getValue()) {
                table.addRow().addContent(entry.getKey(), requirement);
            }
        }

        table.print(System.out, !noFormat);

        return null;
    }
}
