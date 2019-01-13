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

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.net.URI;

@Command(scope = "feature", name = "provided", description = "List the features provided by a features repository")
@Service
public class ListProvidedFeaturesCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "repo", description = "The features repository URI", required = true, multiValued = false)
    URI featuresRepositoryUri;

    @Override
    protected void doExecute(FeaturesService service) throws Exception {
        ShellTable table = new ShellTable();
        table.column("Name");
        table.column("Version");
        for (Feature feature : service.repositoryProvidedFeatures(featuresRepositoryUri)) {
            table.addRow().addContent(feature.getName(), feature.getVersion());
        }
        table.print(System.out);
    }

}
