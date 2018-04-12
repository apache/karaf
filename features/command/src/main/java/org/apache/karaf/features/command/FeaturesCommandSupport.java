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
import java.util.*;
import java.util.regex.Pattern;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;

public abstract class FeaturesCommandSupport implements Action {
    protected EnumSet<FeaturesService.Option> options = EnumSet.noneOf(FeaturesService.Option.class);  

    @Reference
    private FeaturesService featuresService;

    @Override
    public Object execute() throws Exception {
        if (featuresService == null) {
            throw new IllegalStateException("FeaturesService not found");
        }
        doExecute(featuresService);
        return null;
    }

    protected abstract void doExecute(FeaturesService admin) throws Exception;

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }
    
    protected void addOption(FeaturesService.Option option, boolean shouldAdd) {
        if (shouldAdd) {
            options.add(option);
        }
    }

    protected Set<URI> selectRepositories(String nameOrUrl, String version) throws Exception {
        Set<URI> uris = new LinkedHashSet<>();
        String effectiveVersion = (version == null) ? "LATEST" : version;
        URI uri = featuresService.getRepositoryUriFor(nameOrUrl, effectiveVersion);
        if (uri == null) {
            // add regex support on installed repositories
            Pattern pattern = Pattern.compile(nameOrUrl);
            for (Repository repository : featuresService.listRepositories()) {
                URI u = repository.getURI();
                String rname = repository.getName();
                if (pattern.matcher(u.toString()).matches()
                        || rname != null && pattern.matcher(rname).matches()) {
                    uris.add(u);
                }
            }
        } else {
            uris.add(uri);
        }
        return uris;
    }

    protected String getFeatureId(FeaturesService admin, String nameOrId) throws Exception {
        Feature[] matchingFeatures = admin.getFeatures(nameOrId);
        if (matchingFeatures.length == 0) {
            throw new IllegalArgumentException("No matching feature found for " + nameOrId);
        }
        if (matchingFeatures.length > 1) {
            throw new IllegalArgumentException("More than one matching feature found for " + nameOrId);
        }
        return matchingFeatures[0].getId();
    }

    protected List<String> getFeatureIds(FeaturesService admin, List<String> nameOrIds) throws Exception {
        List<String> ids = new ArrayList<>();
        for (String nameOrId : nameOrIds) {
            for (Feature f : admin.getFeatures(nameOrId)) {
                ids.add(f.getId());
            }
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("No matching feature found for " + nameOrIds);
        }
        return ids;
    }
}
