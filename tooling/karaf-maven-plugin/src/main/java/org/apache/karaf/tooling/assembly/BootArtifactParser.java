package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;

import java.util.List;

/**
 * Artifact parser for the boot phase.
 */
class BootArtifactParser extends AbstractPhasedArtifactParser {

    void parse(final Builder builder, final AssemblyMojo mojo, final ArtifactLists artifactLists) {
        final List<String> bootFeatures = mojo.getBootFeatures();
        final List<String> bootProfiles = mojo.getBootProfiles();
        final boolean addAll =
                bootFeatures.isEmpty() && bootProfiles.isEmpty() && mojo.getInstallAllFeaturesByDefault();
        builder.defaultStage(Builder.Stage.Boot)
               .kars(toArray(artifactLists.getBootKars()))
               .repositories(addAll, toArray(artifactLists.getBootRepositories()))
               .features(toArray(bootFeatures))
               .bundles(toArray(artifactLists.getBootBundles()))
               .profiles(toArray(bootProfiles));
    }

}
