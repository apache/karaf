package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.maven.artifact.Artifact;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Parse Artifacts.
 */
class ArtifactParser {

    private static final String TYPE_KAR = "kar";

    private static final String TYPE_REPOSITORY = "repository";

    private static final String TYPE_BUNDLE = "bundle";

    private final MavenUriParser mavenUriParser;

    private final Builder builder;

    private final Map<String, Builder.Stage> scopeToStage = new HashMap<>();

    ArtifactParser(final MavenUriParser mavenUriParser, final Builder builder) {
        this.mavenUriParser = mavenUriParser;
        this.builder = builder;
        init();
    }

    private void init() {
        scopeToStage.put("compile", Builder.Stage.Startup);
        scopeToStage.put("runtime", Builder.Stage.Boot);
        scopeToStage.put("provided", Builder.Stage.Installed);
    }

    void parse(final AssemblyMojo mojo) {
        final ArtifactLists artifactLists = new ArtifactLists();
        artifactLists.addStartupBundles(mojo.getStartupBundles());
        artifactLists.addBootBundles(mojo.getBootBundles());
        artifactLists.addInstalledBundles(mojo.getInstalledBundles());
        artifactLists.addStartupRepositories(mojo.getStartupRepositories());
        artifactLists.addBootRepositories(mojo.getStartupRepositories());
        artifactLists.addInstalledRepositories(mojo.getStartupRepositories());
        addArtifactsToLists(mojo.getProject()
                                .getDependencyArtifacts(), artifactLists);
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
        builder.libraries(toArray(mojo.getLibraries()));
    }

    private void addArtifactsToLists(final Collection<Artifact> artifacts, final ArtifactLists lists) {
        artifacts.forEach(artifact -> Optional.ofNullable(scopeToStage.get(artifact.getScope()))
                                              .ifPresent(stage -> addArtifactToList(lists, artifact, stage)));
    }

    private void addArtifactToList(
            final ArtifactLists lists, final Artifact artifact, final Builder.Stage stage
                                  ) {
        final Map<String, Consumer<Artifact>> artifactLoaders = buildLoadArtifactHandlers(lists, stage);
        getTargetType(artifact).ifPresent(type -> artifactLoaders.get(type)
                                                                 .accept(artifact));
    }

    private Map<String, Consumer<Artifact>> buildLoadArtifactHandlers(
            final ArtifactLists lists, final Builder.Stage stage
                                                                     ) {
        final Map<String, Consumer<Artifact>> loaders = new HashMap<>(3);
        loaders.put(TYPE_KAR,
                    (artifact) -> addArtifactToStageList(stage, artifact, lists.getStartupKars(), lists.getBootKars(),
                                                         lists.getInstalledKars()
                                                        )
                   );
        loaders.put(TYPE_REPOSITORY,
                    (artifact) -> addArtifactToStageList(stage, artifact, lists.getStartupRepositories(),
                                                         lists.getBootRepositories(), lists.getInstalledRepositories()
                                                        )
                   );
        loaders.put(TYPE_BUNDLE, (artifact) -> addArtifactToStageList(stage, artifact, lists.getStartupBundles(),
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
                .ifPresent(list -> list.add(mavenUriParser.artifactToMvnUri(artifact)));
    }

    private Optional<String> getTargetType(final Artifact artifact) {
        String type = null;
        if ("kar".equals(artifact.getType())) {
            type = TYPE_KAR;
        } else if ("features".equals(artifact.getClassifier()) || "karaf".equals(artifact.getClassifier())) {
            type = TYPE_REPOSITORY;
        } else if ("jar".equals(artifact.getType()) || "bundle".equals(artifact.getType())) {
            type = TYPE_BUNDLE;
        }
        return Optional.ofNullable(type);
    }

    private String[] toArray(List<String> strings) {
        return strings.toArray(new String[strings.size()]);
    }

}
