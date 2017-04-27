package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.karaf.tools.utils.model.KarafPropertyEdits;
import org.apache.karaf.tools.utils.model.io.stax.KarafPropertyInstructionsModelStaxReader;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.RemoteRepository;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Factory for creating a {@link Builder} for an {@link AssemblyMojo}.
 */
class BuilderFactory {

    private final Log log;

    private final Builder builder;

    private Map<String, Builder.Stage> scopeToStage = new HashMap<String, Builder.Stage>();

    BuilderFactory(final Log log, final Builder builder) {
        this.log = log;
        this.builder = builder;
        init();
    }

    private void init() {
        scopeToStage.put("compile", Builder.Stage.Startup);
        scopeToStage.put("runtime", Builder.Stage.Boot);
        scopeToStage.put("provided", Builder.Stage.Installed);
    }

    Builder create(final AssemblyMojo mojo) throws IOException, XMLStreamException {
        // configure builder
        //  online/offline
        //  local repository location
        //  java se version
        //  Set up config and system props
        //  Set up blacklisted items
        //  property file edits
        //  pids to extract
        //  karaf version
        //  set whether to use reference urls
        //  set whether to install all features by default
        //  set whether to ignore dependency flag
        //  set the profiles url
        // repositories/features/kars/bundles/dependencies
        //  maven remote repositories
        //  translated urls from project attached artifacts
        //  add each project dependency artifact to kars, repositories and bundles by stage
        //  ensure a framework kar is added to startup kars
        //  configure repositories, features, bundles and profiles for each stage: startup, boot and installed
        //  set the libraries
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

        if (mojo.getPropertyFileEdits() != null) {
            File file = new File(mojo.getPropertyFileEdits());
            if (file.exists()) {
                KarafPropertyEdits edits;
                try (InputStream editsStream = new FileInputStream(mojo.getPropertyFileEdits())) {
                    KarafPropertyInstructionsModelStaxReader kipmsr = new KarafPropertyInstructionsModelStaxReader();
                    edits = kipmsr.read(editsStream, true);
                }
                builder.propertyEdits(edits);
            }
        }
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

        addTranslatedUrls(mojo);

        // creating system directory
        log.info("Creating work directory");
        builder.homeDirectory(mojo.getWorkDirectory()
                                  .toPath());

        // Loading kars and features repositories
        log.info("Loading kar and features repositories dependencies");
        final ArtifactLists artifactLists = new ArtifactLists();
        artifactLists.addStartupBundles(mojo.getStartupBundles());
        artifactLists.addBootBundles(mojo.getBootBundles());
        artifactLists.addInstalledBundles(mojo.getInstalledBundles());
        artifactLists.addStartupRepositories(mojo.getStartupRepositories());
        artifactLists.addBootRepositories(mojo.getStartupRepositories());
        artifactLists.addInstalledRepositories(mojo.getStartupRepositories());
        addArtifactsToLists(mojo.getProject()
                                .getDependencyArtifacts(), artifactLists);

        if (mojo.getLibraries() != null) {
            builder.libraries(mojo.getLibraries()
                                  .toArray(new String[mojo.getLibraries()
                                                          .size()]));
        }
        // Startup
        boolean hasFrameworkKar = false;
        for (String kar : artifactLists.getStartupKars()) {
            if (kar.startsWith("mvn:org.apache.karaf.features/framework/") || kar.startsWith(
                    "mvn:org.apache.karaf.features/static/")) {
                hasFrameworkKar = true;
                artifactLists.removeStartupKar(kar);
                if (mojo.getFramework() == null) {
                    mojo.setFramework(kar.startsWith("mvn:org.apache.karaf.features/framework/") ? "framework"
                                                                                                 : "static-framework");
                }
                builder.kars(Builder.Stage.Startup, false, kar);
                break;
            }
        }
        if (!hasFrameworkKar) {
            Properties versions = new Properties();
            try (InputStream is = getClass().getResourceAsStream("versions.properties")) {
                versions.load(is);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            String realKarafVersion = versions.getProperty("karaf-version");
            String kar;
            switch (mojo.getFramework()) {
                case "framework":
                    kar = "mvn:org.apache.karaf.features/framework/" + realKarafVersion + "/xml/features";
                    break;
                case "framework-logback":
                    kar = "mvn:org.apache.karaf.features/framework/" + realKarafVersion + "/xml/features";
                    break;
                case "static-framework":
                    kar = "mvn:org.apache.karaf.features/static/" + realKarafVersion + "/xml/features";
                    break;
                case "static-framework-logback":
                    kar = "mvn:org.apache.karaf.features/static/" + realKarafVersion + "/xml/features";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported framework: " + mojo.getFramework());
            }
            builder.kars(Builder.Stage.Startup, false, kar);
        }
        final List<String> startupFeatures = mojo.getStartupFeatures();
        if (!startupFeatures.contains(mojo.getFramework())) {
            builder.features(Builder.Stage.Startup, mojo.getFramework());
        }
        final List<String> startupProfiles = mojo.getStartupProfiles();
        builder.defaultStage(Builder.Stage.Startup)
               .kars(toArray(artifactLists.getStartupKars()))
               .repositories(
                       startupFeatures.isEmpty() && startupProfiles.isEmpty() && mojo.getInstallAllFeaturesByDefault(),
                       toArray(artifactLists.getStartupRepositories())
                            )
               .features(toArray(startupFeatures))
               .bundles(toArray(artifactLists.getStartupBundles()))
               .profiles(toArray(startupProfiles));
        // Boot
        final List<String> bootFeatures = mojo.getBootFeatures();
        final List<String> bootProfiles = mojo.getBootProfiles();
        builder.defaultStage(Builder.Stage.Boot)
               .kars(toArray(artifactLists.getBootKars()))
               .repositories(
                       bootFeatures.isEmpty() && bootProfiles.isEmpty() && mojo.getInstallAllFeaturesByDefault(),
                       toArray(artifactLists.getBootRepositories())
                            )
               .features(toArray(bootFeatures))
               .bundles(toArray(artifactLists.getBootBundles()))
               .profiles(toArray(bootProfiles));
        // Installed
        final List<String> installedFeatures = mojo.getInstalledFeatures();
        final List<String> installedProfiles = mojo.getInstalledProfiles();
        builder.defaultStage(Builder.Stage.Installed)
               .kars(toArray(artifactLists.getInstalledKars()))
               .repositories(
                       installedFeatures.isEmpty() && installedProfiles.isEmpty()
                       && mojo.getInstallAllFeaturesByDefault(), toArray(artifactLists.getInstalledRepositories()))
               .features(toArray(installedFeatures))
               .bundles(toArray(artifactLists.getInstalledBundles()))
               .profiles(toArray(installedProfiles));

        return builder;
    }

    private void addTranslatedUrls(final AssemblyMojo mojo) {
        Map<String, String> urls = new HashMap<>();
        List<Artifact> artifacts = new ArrayList<>(mojo.getProject()
                                                       .getAttachedArtifacts());
        artifacts.add(mojo.getProject()
                          .getArtifact());
        for (Artifact artifact : artifacts) {
            if (artifact.getFile() != null) {
                if (artifact.getFile()
                            .exists()) {
                    final String mvnUri = artifactToMvnUri(artifact);
                    final String localUri = artifact.getFile()
                                                    .toURI()
                                                    .toString();
                    urls.put(mvnUri, localUri);
                }
            }
        }
        urls.putAll(mojo.getTranslatedUrls());
        builder.translatedUrls(urls);
    }

    private void addArtifactsToLists(final Collection<Artifact> artifacts, final ArtifactLists lists) {
        artifacts.forEach(artifact -> Optional.ofNullable(scopeToStage.get(artifact.getScope()))
                                              .ifPresent(stage -> addArtifactToList(lists, artifact, stage)));
    }

    private void addArtifactToList(
            final ArtifactLists lists, final Artifact artifact, final Builder.Stage stage
                                  ) {
        final Map<TargetType, Consumer<Artifact>> artifactLoaders = buildLoadArtifactHandlers(lists, stage);
        getTargetType(artifact).ifPresent(type -> artifactLoaders.get(type)
                                                                 .accept(artifact));
    }

    private Map<TargetType, Consumer<Artifact>> buildLoadArtifactHandlers(
            final ArtifactLists lists, final Builder.Stage stage
                                                                         ) {
        final Map<TargetType, Consumer<Artifact>> loaders = new HashMap<>(3);
        loaders.put(TargetType.Kar,
                    (artifact) -> addArtifactToStageList(stage, artifact, lists.getStartupKars(), lists.getBootKars(),
                                                         lists.getInstalledKars()
                                                        )
                   );
        loaders.put(TargetType.Repository,
                    (artifact) -> addArtifactToStageList(stage, artifact, lists.getStartupRepositories(),
                                                         lists.getBootRepositories(), lists.getInstalledRepositories()
                                                        )
                   );
        loaders.put(TargetType.Bundle, (artifact) -> addArtifactToStageList(stage, artifact, lists.getStartupBundles(),
                                                                            lists.getBootBundles(),
                                                                            lists.getInstalledBundles()
                                                                           ));
        return Collections.unmodifiableMap(loaders);
    }

    private void addArtifactToStageList(
            final Builder.Stage stage, final Artifact artifact, final List<String> startup, final List<String> boot,
            final List<String> installed
                                       ) {
        final Map<Builder.Stage, List<String>> listByStage = new HashMap<>();
        listByStage.put(Builder.Stage.Startup, startup);
        listByStage.put(Builder.Stage.Boot, boot);
        listByStage.put(Builder.Stage.Installed, installed);
        Optional.ofNullable(listByStage.get(stage))
                .ifPresent(list -> list.add(artifactToMvnUri(artifact)));
    }

    private Optional<TargetType> getTargetType(final Artifact artifact) {
        TargetType type = null;
        if ("kar".equals(artifact.getType())) {
            type = TargetType.Kar;
        } else if ("features".equals(artifact.getClassifier()) || "karaf".equals(artifact.getClassifier())) {
            type = TargetType.Repository;
        } else if ("jar".equals(artifact.getType()) || "bundle".equals(artifact.getType())) {
            type = TargetType.Bundle;
        }
        return Optional.ofNullable(type);
    }

    private enum TargetType {
        Kar,
        Repository,
        Bundle,
    }

    private String getMavenRepositories(final List<RemoteRepository> repositories) {
        return repositories.stream()
                           .map(this::getRemoteRepositoryAsString)
                           .collect(Collectors.joining(","));
    }

    private String artifactToMvnUri(final Artifact artifact) {
        final String classifier = Optional.ofNullable(artifact.getClassifier())
                                          .filter(c -> !"".matches(c))
                                          .map(c -> "/" + c)
                                          .orElse("");
        String extension = artifact.getArtifactHandler()
                                   .getExtension();
        if ("bundle".equals(extension)) {
            extension = "jar";
        }
        final String type = "/" + extension;
        String suffix = "";
        if (!classifier.isEmpty() || !"/jar".equals(type)) {
            suffix = type + classifier;
        }
        return String.format("mvn:%s/%s/%s%s", artifact.getGroupId(), artifact.getArtifactId(),
                             artifact.getBaseVersion(), suffix
                            );
    }

    private String[] toArray(List<String> strings) {
        return strings.toArray(new String[strings.size()]);
    }

    private String getRemoteRepositoryAsString(final RemoteRepository repository) {
        final String releases = repository.getPolicy(false)
                                          .isEnabled() ? "" : "@noreleases";
        final String snapshots = repository.getPolicy(true)
                                           .isEnabled() ? "@snapshots" : "";
        return repository.getUrl() + "@id=" + repository.getId() + releases + snapshots;
    }

}
