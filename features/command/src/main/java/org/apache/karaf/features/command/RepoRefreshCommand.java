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

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;

@Command(scope = "feature", name = "repo-refresh", description = "Refresh a features repository")
public class RepoRefreshCommand extends AbstractAction {
    @Argument(index = 0, name = "Feature name or uri", description = "Shortcut name of the feature repository or the full URI", required = false, multiValued = false)
    private String nameOrUrl;
    
    @Argument(index = 1, name = "Feature version", description = "The version of the feature if using the feature name. Should be empty if using the uri", required = false, multiValued = false)
    private String version;

    private FeatureFinder featureFinder;
    private FeaturesService featuresService;
    
    public void setFeatureFinder(FeatureFinder featureFinder) {
        this.featureFinder = featureFinder;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    protected Object doExecute() throws Exception {
    	if (nameOrUrl != null) {
    		String effectiveVersion = (version == null) ? "LATEST" : version;
        	URI uri = featureFinder.getUriFor(nameOrUrl, effectiveVersion);
        	if (uri == null) {
        		uri = new URI(nameOrUrl);
        	}
        	System.out.println("Refreshing feature url " + uri);
        	featuresService.refreshRepository(uri);
    	} else {
    		refreshAll();
    	}
        return null;
    }

	private void refreshAll() {
		Repository[] repos = featuresService.listRepositories();
		for (Repository repo : repos) {
			try {
				System.out.println("Refreshing feature url " + repo.getURI());
				featuresService.refreshRepository(repo.getURI());
			} catch (Exception e) {
				System.err.println("Error refreshing " + repo.getURI().toString() + ": " +e.getMessage());
			}
		}
	}

}
