/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Paul Campbell
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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

    private final Builder builder;

    private final Map<String, String> frameworkUris = new HashMap<>();

    ArtifactFrameworkParser(final Builder builder) {
        this.builder = builder;
        frameworkUris.put(FRAMEWORK, KARAF_FRAMEWORK_DYNAMIC);
        frameworkUris.put(FRAMEWORK_LOGBACK, KARAF_FRAMEWORK_DYNAMIC);
        frameworkUris.put(STATIC_FRAMEWORK, KARAF_FRAMEWORK_STATIC);
        frameworkUris.put(STATIC_FRAMEWORK_LOGBACK, KARAF_FRAMEWORK_STATIC);
    }

    void parse(final AssemblyMojo mojo, final ArtifactLists artifactLists) {
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
