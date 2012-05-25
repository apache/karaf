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

import java.net.URI;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;

@Command(scope = "feature", name = "repo-remove", description = "Removes the specified repository features service.")
public class RepoRemoveCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "repository", description = "Name or url of the repository to remove.", required = true, multiValued = false)
    private String repository;

    protected void doExecute(FeaturesService featuresService) throws Exception {
    	URI uri = null;
    	for (Repository r : featuresService.listRepositories()) {
    		if (r.getName().equals(repository)) {
    			uri = r.getURI();
    			break;
    		}
    	}

    	if (uri == null) {
    	    uri = new URI(repository);
    	}

    	featuresService.removeRepository(uri);
    }
}
