/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.features.command.completers;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

import java.util.ArrayList;
import java.util.List;

@Service
public class FeatureVersionCompleter implements Completer {

    @Reference
    private FeaturesService featuresService;

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        String[] args = commandLine.getArguments();
        // args look like this at this point: [feature:status, wrapper, '']
        if (args.length >= 3) {
            String featureArg = args[1];
            try {
                List<String> versions = getAllVersionsOfFeature(featureArg, featuresService);
                delegate.getStrings().addAll(versions);
                return delegate.complete(session, commandLine, candidates);
            } catch (Exception e) {
                // Ignore
            }
        }

        return delegate.complete(session, commandLine, candidates);
    }

    private List<String> getAllVersionsOfFeature(String feature, FeaturesService featuresService) throws Exception {
        List<String> versions = new ArrayList<>();
        for (Repository repo : featuresService.listRepositories()) {
            for (Feature f : repo.getFeatures()) {
                if (f.getName().equals(feature)) {
                    versions.add(f.getVersion());
                }
            }
        }

        return versions;
    }
}
