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
package org.apache.karaf.kar.command;

import java.util.List;

import org.apache.karaf.features.command.completers.InstalledRepoNameCompleter;
import org.apache.karaf.kar.KarService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "kar", name = "create", description = "Create a kar file for a list of feature repos")
@Service
public class CreateKarCommand implements Action {
    
    @Argument(index = 0, name = "repoName", description = "Repository name. The kar will contain all features of the named repository by default", required = true, multiValued = false)
    @Completion(InstalledRepoNameCompleter.class)
    private String repoName;
    
    @Argument(index = 1, name = "features", description = "Names of the features to include. If set then only these features will be added", required = false, multiValued = true)
    private List<String> features;

    @Reference
    private KarService karService;

    @Override
    public Object execute() throws Exception {
        karService.create(repoName, features, System.out);
        return null;
    }
    
}
