package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;

import java.util.List;

/**
 * Artifact parser for the installed phase.
 */
class InstalledArtifactParser extends AbstractPhasedArtifactParser {

    void parse(final Builder builder, final AssemblyMojo mojo, final ArtifactLists artifactLists) {
        final List<String> installedFeatures = mojo.getInstalledFeatures();
        final List<String> installedProfiles = mojo.getInstalledProfiles();
        final boolean addAll =
                installedFeatures.isEmpty() && installedProfiles.isEmpty() && mojo.getInstallAllFeaturesByDefault();
        builder.defaultStage(Builder.Stage.Installed)
               .kars(toArray(artifactLists.getInstalledKars()))
               .repositories(addAll, toArray(artifactLists.getInstalledRepositories()))
               .features(toArray(installedFeatures))
               .bundles(toArray(artifactLists.getInstalledBundles()))
               .profiles(toArray(installedProfiles));
    }

}
