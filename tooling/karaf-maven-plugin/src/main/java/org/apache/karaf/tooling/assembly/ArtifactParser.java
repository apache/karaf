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

    private static final String KARAF_FRAMEWORK_DYNAMIC = "mvn:org.apache.karaf.features/framework/";

    private static final String KARAF_FRAMEWORK_STATIC = "mvn:org.apache.karaf.features/static/";

    private static final String FRAMEWORK_SUFFIX = "/xml/features";

    private static final String FRAMEWORK = "framework";

    private static final String FRAMEWORK_LOGBACK = "framework-logback";

    private static final String STATIC_FRAMEWORK = "static-framework";

    private static final String STATIC_FRAMEWORK_LOGBACK = "static-framework-logback";

    private final MavenUriParser mavenUriParser;

    private final Builder builder;

    private final Map<String, Builder.Stage> scopeToStage = new HashMap<>();

    private final Map<String, String> frameworkUris = new HashMap<>();

    ArtifactParser(final MavenUriParser mavenUriParser, final Builder builder) {
        this.mavenUriParser = mavenUriParser;
        this.builder = builder;
        init();
    }

    private void init() {
        scopeToStage.put("compile", Builder.Stage.Startup);
        scopeToStage.put("runtime", Builder.Stage.Boot);
        scopeToStage.put("provided", Builder.Stage.Installed);

        frameworkUris.put(FRAMEWORK, KARAF_FRAMEWORK_DYNAMIC);
        frameworkUris.put(FRAMEWORK_LOGBACK, KARAF_FRAMEWORK_DYNAMIC);
        frameworkUris.put(STATIC_FRAMEWORK, KARAF_FRAMEWORK_STATIC);
        frameworkUris.put(STATIC_FRAMEWORK_LOGBACK, KARAF_FRAMEWORK_STATIC);
    }

    void parse(final AssemblyMojo mojo) {
        final ArtifactLists artifactLists = buildArtifactLists(mojo);
        addFrameworkKar(mojo, artifactLists);
        startup(mojo, artifactLists);
        boot(mojo, artifactLists);
        installed(mojo, artifactLists);
        builder.libraries(toArray(mojo.getLibraries()));
    }

    private ArtifactLists buildArtifactLists(final AssemblyMojo mojo) {
        final ArtifactLists artifactLists = new ArtifactLists();
        artifactLists.addStartupBundles(mojo.getStartupBundles());
        artifactLists.addBootBundles(mojo.getBootBundles());
        artifactLists.addInstalledBundles(mojo.getInstalledBundles());
        artifactLists.addStartupRepositories(mojo.getStartupRepositories());
        artifactLists.addBootRepositories(mojo.getStartupRepositories());
        artifactLists.addInstalledRepositories(mojo.getStartupRepositories());
        addArtifactsToLists(mojo.getProject()
                                .getDependencyArtifacts(), artifactLists);
        return artifactLists;
    }

    private void addArtifactsToLists(final Collection<Artifact> artifacts, final ArtifactLists lists) {
        artifacts.forEach(artifact -> Optional.ofNullable(scopeToStage.get(artifact.getScope()))
                                              .ifPresent(stage -> addArtifactToList(lists, artifact, stage)));
    }

    private void addArtifactToList(
            final ArtifactLists lists, final Artifact artifact, final Builder.Stage stage
                                  ) {
        getTargetType(artifact).ifPresent(type -> getLoadArtifactHandlers(lists, stage).get(type)
                                                                                       .accept(artifact));
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

    private Map<String, Consumer<Artifact>> getLoadArtifactHandlers(
            final ArtifactLists lists, final Builder.Stage stage
                                                                   ) {
        final Map<String, Consumer<Artifact>> loaders = new HashMap<>(3);
        loaders.put(TYPE_KAR, karArtifactLoadHandler(lists, stage));
        loaders.put(TYPE_REPOSITORY, repositoryArtifactLoadHandler(lists, stage));
        loaders.put(TYPE_BUNDLE, bundleArtifactLoadHandler(lists, stage));
        return Collections.unmodifiableMap(loaders);
    }

    private Consumer<Artifact> karArtifactLoadHandler(final ArtifactLists lists, final Builder.Stage stage) {
        return (artifact) -> addArtifactToStageList(stage, artifact, lists.getStartupKars(), lists.getBootKars(),
                                                    lists.getInstalledKars()
                                                   );
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

    private Consumer<Artifact> repositoryArtifactLoadHandler(final ArtifactLists lists, final Builder.Stage stage) {
        return (artifact) -> addArtifactToStageList(stage, artifact, lists.getStartupRepositories(),
                                                    lists.getBootRepositories(), lists.getInstalledRepositories()
                                                   );
    }

    private Consumer<Artifact> bundleArtifactLoadHandler(final ArtifactLists lists, final Builder.Stage stage) {
        return (artifact) -> addArtifactToStageList(stage, artifact, lists.getStartupBundles(), lists.getBootBundles(),
                                                    lists.getInstalledBundles()
                                                   );
    }

    private void addFrameworkKar(final AssemblyMojo mojo, final ArtifactLists artifactLists) {
        final String kar =
                findSelectedFrameworkKar(artifactLists).orElseGet(() -> getFrameworkKar(mojo.getFramework()));
        artifactLists.removeStartupKar(kar);
        setFrameworkIfMissing(mojo, kar);
        builder.kars(Builder.Stage.Startup, false, kar);
    }

    private String getFrameworkKar(final String framework) {
        return Optional.ofNullable(frameworkUris.get(framework))
                       .map(uri -> uri + (getRealKarafVersion() + FRAMEWORK_SUFFIX))
                       .orElseThrow(() -> new IllegalArgumentException("Unsupported framework: " + framework));
    }

    private String getRealKarafVersion() {
        try (InputStream is = getClass().getResourceAsStream("versions.properties")) {
            Properties versions = new Properties();
            versions.load(is);
            return versions.getProperty("karaf-version");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Optional<String> findSelectedFrameworkKar(final ArtifactLists artifactLists) {
        return artifactLists.getStartupKars()
                            .stream()
                            .filter(kar -> kar.startsWith(KARAF_FRAMEWORK_DYNAMIC) || kar.startsWith(
                                    KARAF_FRAMEWORK_STATIC))
                            .findAny();
    }

    private void setFrameworkIfMissing(final AssemblyMojo mojo, final String kar) {
        if (mojo.getFramework() == null) {
            mojo.setFramework(getFrameworkFromUri(kar));
        }
    }

    private String getFrameworkFromUri(final String kar) {
        if (kar.startsWith(KARAF_FRAMEWORK_DYNAMIC)) {
            return FRAMEWORK;
        }
        return STATIC_FRAMEWORK;
    }

    private void startup(final AssemblyMojo mojo, final ArtifactLists artifactLists) {
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
    }

    private String[] toArray(List<String> strings) {
        return strings.toArray(new String[strings.size()]);
    }

    private void boot(final AssemblyMojo mojo, final ArtifactLists artifactLists) {
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
    }

    private void installed(final AssemblyMojo mojo, final ArtifactLists artifactLists) {
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
    }

}
