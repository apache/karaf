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
import org.apache.karaf.tooling.RealKarafVersion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Artifact Parser for Framework Kar.
 */
class ArtifactFrameworkParser {

    private static final String FRAMEWORK_SUFFIX = "/xml/features";

    private static final String KARAF_FRAMEWORK_DYNAMIC = "mvn:org.apache.karaf.features/framework/";

    private static final String KARAF_FRAMEWORK_STATIC = "mvn:org.apache.karaf.features/static/";

    private static final String FRAMEWORK = "framework";

    private static final String FRAMEWORK_LOGBACK = "framework-logback";

    private static final String STATIC_FRAMEWORK = "static-framework";

    private static final String STATIC_FRAMEWORK_LOGBACK = "static-framework-logback";

    private final Map<String, String> frameworkUris = new HashMap<>();

    ArtifactFrameworkParser() {
        frameworkUris.put(FRAMEWORK, KARAF_FRAMEWORK_DYNAMIC);
        frameworkUris.put(FRAMEWORK_LOGBACK, KARAF_FRAMEWORK_DYNAMIC);
        frameworkUris.put(STATIC_FRAMEWORK, KARAF_FRAMEWORK_STATIC);
        frameworkUris.put(STATIC_FRAMEWORK_LOGBACK, KARAF_FRAMEWORK_STATIC);
    }

    void parse(
            final Builder builder, final AssemblyMojo mojo, final ArtifactLists artifactLists
              ) {
        final String kar = findSelectedFrameworkKar(artifactLists.getStartupKars()).orElseGet(
                () -> getFrameworkKar(mojo.getFramework()));
        artifactLists.removeStartupKar(kar);
        setFrameworkIfMissing(mojo, kar);
        builder.kars(Builder.Stage.Startup, false, kar);
    }

    private String getFrameworkKar(final String framework) {
        return Optional.ofNullable(framework)
                       .map(frameworkUris::get)
                       .map(uri -> uri + (getRealKarafVersion() + FRAMEWORK_SUFFIX))
                       .orElseThrow(() -> new IllegalArgumentException("Unsupported framework: " + framework));
    }

    private String getRealKarafVersion() {
        return new RealKarafVersion().get();
    }

    private Optional<String> findSelectedFrameworkKar(final List<String> startupKars) {
        return startupKars.stream()
                          .filter(this::isFrameworkUri)
                          .findAny();
    }

    private boolean isFrameworkUri(final String kar) {
        return kar.startsWith(KARAF_FRAMEWORK_DYNAMIC) || kar.startsWith(KARAF_FRAMEWORK_STATIC);
    }

    private void setFrameworkIfMissing(final AssemblyMojo mojo, final String kar) {
        final boolean frameworkIsMissing = mojo.getFramework() == null;
        if (frameworkIsMissing) {
            final String framework = getFrameworkFromUri(kar);
            mojo.setFramework(framework);
        }
    }

    private String getFrameworkFromUri(final String kar) {
        final boolean isDynamicFramework = kar.startsWith(KARAF_FRAMEWORK_DYNAMIC);
        if (isDynamicFramework) {
            return FRAMEWORK;
        }
        return STATIC_FRAMEWORK;
    }

}
