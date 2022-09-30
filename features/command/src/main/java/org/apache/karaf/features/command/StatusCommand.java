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
package org.apache.karaf.features.command;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.command.completers.AllFeatureCompleter;
import org.apache.karaf.features.command.completers.FeatureVersionCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "status", description = "Get the feature's current status")
@Service
public class StatusCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "feature", description = "Name of the feature.", required = true)
    @Completion(AllFeatureCompleter.class)
    String feature;

    @Argument(index = 1, name = "version", description = "Optional version of the feature.")
    @Completion(value = FeatureVersionCompleter.class)
    String version;

    @Override
    protected void doExecute(FeaturesService admin) throws Exception {
        System.out.println(getState(admin));
    }

    private String getState(FeaturesService featuresService) throws Exception {
        String id;
        if (version == null) {
            id = featuresService.getFeature(feature).getId();
        } else {
            id = featuresService.getFeature(feature, version).getId();
        }
        return featuresService.getState(id).name();
    }
}
