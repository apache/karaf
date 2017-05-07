package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;

import java.util.List;

/**
 * Artifact parser for the startup phase.
 */
class StartupArtifactParser extends AbstractPhasedArtifactParser {

    private final Builder builder;

    StartupArtifactParser(final Builder builder) {
        this.builder = builder;
    }

    void parse(final AssemblyMojo mojo, final ArtifactLists artifactLists) {
        final List<String> startupFeatures = mojo.getStartupFeatures();
        addFrameworkFeatureIfMissing(mojo.getFramework(), startupFeatures);
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

    private void addFrameworkFeatureIfMissing(final String framework, final List<String> startupFeatures) {
        final boolean frameworkIsMissing = !startupFeatures.contains(framework);
        if (frameworkIsMissing) {
            builder.features(Builder.Stage.Startup, framework);
        }
    }

}
