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
import org.apache.karaf.shell.console.AbstractAction;

@Command(scope = "feature", name = "url-choose", description = "Add a repository url for well known features.")
public class ChooseUrlCommand extends AbstractAction {

    @Argument(index = 0, name = "Feature name", description = "The name of the feature", required = true, multiValued = false)
    private String name;
    
    @Argument(index = 1, name = "Feature version", description = "The version of the feature", required = false, multiValued = false)
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
        String effectiveVersion = (version == null) ? "LATEST" : version;
        URI uri = featureFinder.getUriFor(name, effectiveVersion);
        if (uri == null) {
            throw new RuntimeException("No feature found for name " + name + " and version " + version);
        }
        System.out.println("Adding feature url " + uri);
        featuresService.addRepository(uri);
        return null;
    }

}
