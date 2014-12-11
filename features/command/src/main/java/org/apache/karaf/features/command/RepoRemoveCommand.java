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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.command.completers.InstalledRepoNameCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "repo-remove", description = "Removes the specified repository features service.")
@Service
public class RepoRemoveCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "repository", description = "Name or url of the repository to remove.", required = true, multiValued = false)
    @Completion(InstalledRepoNameCompleter.class)
    private String repository;

    @Option(name = "-u", aliases = { "--uninstall-all" }, description = "Uninstall all features from the repository", required = false, multiValued = false)
    private boolean uninstall;

    protected void doExecute(FeaturesService featuresService) throws Exception {
		List<URI> uris = new ArrayList<>();
		Pattern pattern = Pattern.compile(repository);
		for (Repository r : featuresService.listRepositories()) {
			if (r.getName() != null && !r.getName().isEmpty()) {
				// repository has a name, try regex on the name
				Matcher nameMatcher = pattern.matcher(r.getName());
				if (nameMatcher.matches()) {
					uris.add(r.getURI());
				} else {
					// the name regex doesn't match, fallback to repository URI regex
					Matcher uriMatcher = pattern.matcher(r.getURI().toString());
					if (uriMatcher.matches()) {
						uris.add(r.getURI());
					}
				}
			} else {
				// the repository name is not defined, use repository URI regex
				Matcher uriMatcher = pattern.matcher(r.getURI().toString());
				if (uriMatcher.matches()) {
					uris.add(r.getURI());
				}
			}
		}

		for (URI uri : uris) {
			System.out.println("Removing features repository " + uri);
			featuresService.removeRepository(uri, uninstall);
		}
    }
}
