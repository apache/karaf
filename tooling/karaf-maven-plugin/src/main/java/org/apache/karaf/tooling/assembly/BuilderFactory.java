package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.RemoteRepository;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory for creating a {@link Builder} for an {@link AssemblyMojo}.
 */
class BuilderFactory {

    private final Log log;

    private final Builder builder;

    private final MavenUriTranslator mavenUriTranslator;

    private final ProfileEditsParser profileEditsParser;

    private final ArtifactParser artifactParser;
    BuilderFactory(
            final Log log, final Builder builder, final MavenUriTranslator mavenUriTranslator,
            final ProfileEditsParser profileEditsParser, final ArtifactParser artifactParser
                  ) {
        this.log = log;
        this.builder = builder;
        this.mavenUriTranslator = mavenUriTranslator;
        this.profileEditsParser = profileEditsParser;
        this.artifactParser = artifactParser;
    }

    Builder create(final AssemblyMojo mojo) throws IOException, XMLStreamException {
        builder.offline(mojo.getMavenSession()
                            .isOffline());
        builder.localRepository(mojo.getLocalRepo()
                                    .getBasedir());
        builder.javase(mojo.getJavase());
        mojo.getConfig()
            .forEach(builder::config);
        mojo.getSystem()
            .forEach(builder::system);
        builder.karafVersion(mojo.getKarafVersion());
        builder.useReferenceUrls(mojo.getUseReferenceUrls());
        builder.defaultAddAll(mojo.getInstallAllFeaturesByDefault());
        builder.ignoreDependencyFlag(mojo.getIgnoreDependencyFlag());
        if (mojo.getProfilesUri() != null) {
            builder.profilesUris(mojo.getProfilesUri());
        }
        profileEditsParser.parse(mojo)
                          .ifPresent(builder::propertyEdits);
        builder.pidsToExtract(mojo.getPidsToExtract());
        builder.blacklistPolicy(mojo.getBlacklistPolicy());
        builder.blacklistBundles(mojo.getBlacklistedBundles());
        builder.blacklistFeatures(mojo.getBlacklistedFeatures());
        builder.blacklistProfiles(mojo.getBlacklistedProfiles());
        builder.blacklistRepositories(mojo.getBlacklistedRepositories());

        final MavenProject mavenProject = mojo.getProject();
        final List<RemoteRepository> remoteProjectRepositories = mavenProject.getRemoteProjectRepositories();
        final String mavenRepositories = getMavenRepositories(remoteProjectRepositories);
        log.info("Using repositories: " + mavenRepositories);
        builder.mavenRepositories(mavenRepositories);
        builder.translatedUrls(mavenUriTranslator.getTranslatedUris(mojo.getProject(), mojo.getTranslatedUrls()));
        // creating system directory
        log.info("Creating work directory");
        builder.homeDirectory(mojo.getWorkDirectory()
                                  .toPath());
        // Loading kars and features repositories
        log.info("Loading kar and features repositories dependencies");
        artifactParser.parse(mojo);
        if (mojo.getLibraries() != null) {
            builder.libraries(mojo.getLibraries()
                                  .toArray(new String[mojo.getLibraries()
                                                          .size()]));
        }

        return builder;
    }

    private String getMavenRepositories(final List<RemoteRepository> repositories) {
        return repositories.stream()
                           .map(this::getRemoteRepositoryAsString)
                           .collect(Collectors.joining(","));
    }

    private String getRemoteRepositoryAsString(final RemoteRepository repository) {
        final String releases = repository.getPolicy(false)
                                          .isEnabled() ? "" : "@noreleases";
        final String snapshots = repository.getPolicy(true)
                                           .isEnabled() ? "@snapshots" : "";
        return repository.getUrl() + "@id=" + repository.getId() + releases + snapshots;
    }

}
