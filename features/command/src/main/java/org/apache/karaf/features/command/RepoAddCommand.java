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
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;

@Command(scope = "feature", name = "repo-add", description = "Add a features repository")
public class RepoAddCommand extends AbstractAction {

    @Argument(index = 0, name = "name/url", description = "Shortcut name of the features repository or the full URL", required = true, multiValued = false)
    private String nameOrUrl;
    
    @Argument(index = 1, name = "version", description = "The version of the features repository if using features repository name as first argument. It should be empty if using the URL", required = false, multiValued = false)
    private String version;

    @Option(name = "-i", aliases = { "--install" }, description = "Install all features contained in the features repository", required = false, multiValued = false)
    private boolean install;
    
    private FeatureFinder featureFinder;
    private FeaturesService featuresService;
    
    public void setFeatureFinder(FeatureFinder featureFinder) {
        this.featureFinder = featureFinder;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    protected Object doExecute() throws Exception {
        String effectiveVersion = (version == null) ? "LATEST" : version;
        URI uri = featureFinder.getUriFor(nameOrUrl, effectiveVersion);
        if (uri == null) {
            uri = new URI(nameOrUrl);
        }
        System.out.println("Adding feature url " + uri);
        featuresService.addRepository(uri, install);
        return null;
    }

}
