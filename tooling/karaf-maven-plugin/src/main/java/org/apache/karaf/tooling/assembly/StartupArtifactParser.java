/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;

import java.util.List;

/**
 * Artifact parser for the startup phase.
 */
class StartupArtifactParser extends AbstractPhasedArtifactParser {

    void parse(final Builder builder, final AssemblyMojo mojo, final ArtifactLists artifactLists) {
        final List<String> startupFeatures = mojo.getStartupFeatures();
        addFrameworkFeatureIfMissing(builder, mojo.getFramework(), startupFeatures);
        final List<String> startupProfiles = mojo.getStartupProfiles();
        final boolean addAll =
                startupFeatures.isEmpty() && startupProfiles.isEmpty() && mojo.getInstallAllFeaturesByDefault();
        builder.defaultStage(Builder.Stage.Startup)
               .kars(toArray(artifactLists.getStartupKars()))
               .repositories(addAll, toArray(artifactLists.getStartupRepositories()))
               .features(toArray(startupFeatures))
               .bundles(toArray(artifactLists.getStartupBundles()))
               .profiles(toArray(startupProfiles));
    }

    private void addFrameworkFeatureIfMissing(
            final Builder builder, final String framework, final List<String> startupFeatures
                                             ) {
        final boolean frameworkIsMissing = !startupFeatures.contains(framework);
        if (frameworkIsMissing) {
            builder.features(Builder.Stage.Startup, framework);
        }
    }

}
