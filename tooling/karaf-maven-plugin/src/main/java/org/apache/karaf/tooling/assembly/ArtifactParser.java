package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.karaf.tooling.RealKarafVersion;
import org.apache.maven.artifact.Artifact;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
        configureStartupPhase(mojo, artifactLists);
        configureBootPhase(mojo, artifactLists);
        configureInstalledPhase(mojo, artifactLists);
        addLibraries(mojo);
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
        groupArtifactsByStage(artifacts).forEach((key, value) -> addArtifactsToStageLists(key, value, lists));
    }

    private void addArtifactsToStageLists(
            final Builder.Stage stage, final List<Artifact> artifacts, final ArtifactLists lists
                                         ) {
        final Map<String, Consumer<Artifact>> handlers = getArtifactHandlers(lists, stage);
        artifacts.forEach(artifact -> getArtifactType(artifact).ifPresent(type -> handlers.get(type)
                                                                                          .accept(artifact)));
    }

    private Optional<String> getArtifactType(final Artifact artifact) {
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

    private Map<String, Consumer<Artifact>> getArtifactHandlers(
            final ArtifactLists lists, final Builder.Stage stage
                                                               ) {
        final Map<String, Consumer<Artifact>> loaders = new HashMap<>(3);
        loaders.put(TYPE_KAR, karArtifactHandler(lists, stage));
        loaders.put(TYPE_REPOSITORY, repositoryArtifactHandler(lists, stage));
        loaders.put(TYPE_BUNDLE, bundleArtifactHandler(lists, stage));
        return Collections.unmodifiableMap(loaders);
    }

    private Consumer<Artifact> karArtifactHandler(final ArtifactLists lists, final Builder.Stage stage) {
        final Map<Builder.Stage, List<String>> listsByStage =
                mapListsByStage(lists.getStartupKars(), lists.getBootKars(), lists.getInstalledKars());
        return (artifact) -> addArtifactToStageList(stage, artifact, listsByStage);
    }

    private Map<Builder.Stage, List<String>> mapListsByStage(
            final List<String> startup, final List<String> boot, final List<String> installed
                                                            ) {
        final Map<Builder.Stage, List<String>> listsByStage = new HashMap<>();
        listsByStage.put(Builder.Stage.Startup, startup);
        listsByStage.put(Builder.Stage.Boot, boot);
        listsByStage.put(Builder.Stage.Installed, installed);
        return Collections.unmodifiableMap(listsByStage);
    }

    private void addArtifactToStageList(
            final Builder.Stage stage, final Artifact artifact, final Map<Builder.Stage, List<String>> listsByStage
                                       ) {
        Optional.ofNullable(listsByStage.get(stage))
                .ifPresent(list -> list.add(mavenUriParser.artifactToMvnUri(artifact)));
    }

    private Consumer<Artifact> repositoryArtifactHandler(final ArtifactLists lists, final Builder.Stage stage) {
        final Map<Builder.Stage, List<String>> listsByStage =
                mapListsByStage(lists.getStartupRepositories(), lists.getBootRepositories(),
                                lists.getInstalledRepositories()
                               );
        return (artifact) -> addArtifactToStageList(stage, artifact, listsByStage);
    }

    private Consumer<Artifact> bundleArtifactHandler(final ArtifactLists lists, final Builder.Stage stage) {
        final Map<Builder.Stage, List<String>> listsByStage =
                mapListsByStage(lists.getStartupBundles(), lists.getBootBundles(), lists.getInstalledBundles());
        return (artifact) -> addArtifactToStageList(stage, artifact, listsByStage);
    }

    private Map<Builder.Stage, List<Artifact>> groupArtifactsByStage(final Collection<Artifact> artifacts) {
        return artifacts.stream()
                        .filter(artifact -> scopeToStage.get(artifact.getScope()) != null)
                        .collect(Collectors.groupingBy(this::getStage, Collectors.toList()));
    }

    private void addFrameworkKar(final AssemblyMojo mojo, final ArtifactLists artifactLists) {
        final String kar = findSelectedFrameworkKar(artifactLists.getStartupKars()).orElseGet(
                () -> getFrameworkKar(mojo.getFramework()));
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
        return new RealKarafVersion().get();
    }

    private Optional<String> findSelectedFrameworkKar(final List<String> startupKars) {
        return startupKars.stream()
                          .filter(this::isFrameworkUri)
                          .findAny();
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

    private void configureStartupPhase(final AssemblyMojo mojo, final ArtifactLists artifactLists) {
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

    private String[] toArray(List<String> strings) {
        return strings.toArray(new String[strings.size()]);
    }

    private void configureBootPhase(final AssemblyMojo mojo, final ArtifactLists artifactLists) {
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

    private void configureInstalledPhase(final AssemblyMojo mojo, final ArtifactLists artifactLists) {
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

    private void addLibraries(final AssemblyMojo mojo) {
        builder.libraries(toArray(mojo.getLibraries()));
    }

    private Builder.Stage getStage(final Artifact artifact) {
        return scopeToStage.get(artifact.getScope());
    }

    private boolean isFrameworkUri(final String kar) {
        return kar.startsWith(KARAF_FRAMEWORK_DYNAMIC) || kar.startsWith(KARAF_FRAMEWORK_STATIC);
    }

}
