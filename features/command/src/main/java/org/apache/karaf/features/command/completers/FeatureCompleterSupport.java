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
package org.apache.karaf.features.command.completers;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.Candidate;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;

/**
 * Base completer for feature commands.
 */
public abstract class FeatureCompleterSupport implements Completer {

    /**
     * Feature service.
     */
    @Reference
    protected FeaturesService featuresService;

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    @Override
    public int complete(Session session, final CommandLine commandLine, final List<String> candidates) {
        List<Candidate> cands = new ArrayList<>();
        completeCandidates(session, commandLine, cands);
        for (Candidate cand : cands) {
            candidates.add(cand.value());
        }
        return candidates.isEmpty() ? -1 : 0;
    }

    @Override
    public void completeCandidates(Session session, CommandLine commandLine, List<Candidate> candidates) {
        try {
            for (Feature feature : featuresService.listFeatures()) {
                if (acceptsFeature(feature)) {
                    add(candidates, feature);
                }
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    protected void add(List<Candidate> candidates, Feature feature) {
        candidates.add(new Candidate(
                feature.getName(), feature.getName(), null,
                feature.getDescription(), null, null, true));
    }

    /**
     * Method for filtering features.
     *
     * @param feature The feature.
     * @return True if feature should be available in completer.
     */
    protected abstract boolean acceptsFeature(Feature feature);
}
