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
import org.apache.karaf.features.command.completers.InstalledRepoUriCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "repo-refresh", description = "Refresh a features repository")
@Service
public class RepoRefreshCommand extends FeaturesCommandSupport {
    
    @Argument(index = 0, name = "Feature name or uri", description = "Shortcut name of the feature repository or the full URI", required = false, multiValued = false)
    @Completion(InstalledRepoUriCompleter.class)
    private String nameOrUrl;
    
    @Argument(index = 1, name = "Feature version", description = "The version of the feature if using the feature name. Should be empty if using the uri", required = false, multiValued = false)
    private String version;

    @Override
    protected void doExecute(FeaturesService featuresService) throws Exception {
        List<URI> uris = new ArrayList<>();
    	if (nameOrUrl != null) {
    		String effectiveVersion = (version == null) ? "LATEST" : version;
        	URI uri = featuresService.getRepositoryUriFor(nameOrUrl, effectiveVersion);
        	if (uri == null) {
                // add regex support on installed repositories
                Pattern pattern = Pattern.compile(nameOrUrl);
                for (Repository repository : featuresService.listRepositories()) {
                    URI u = repository.getURI();
                    Matcher matcher = pattern.matcher(u.toString());
                    if (matcher.matches()) {
                        uris.add(u);
                    }
                }
        	} else {
                uris.add(uri);
            }
    	} else {
            Repository[] repos = featuresService.listRepositories();
            for (Repository repo : repos) {
                uris.add(repo.getURI());
            }
    	}
        for (URI uri : uris) {
            try {
                System.out.println("Refreshing feature url " + uri);
                featuresService.refreshRepository(uri);
            } catch (Exception e) {
                System.err.println("Error refreshing " + uri.toString() + ": " + e.getMessage());
            }
        }
    }

}
