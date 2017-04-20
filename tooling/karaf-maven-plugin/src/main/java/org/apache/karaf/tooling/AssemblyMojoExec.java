package org.apache.karaf.tooling;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.karaf.tooling.utils.IoUtils;
import org.apache.karaf.tools.utils.model.KarafPropertyEdits;
import org.apache.karaf.tools.utils.model.io.stax.KarafPropertyInstructionsModelStaxReader;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Executor for the {@link AssemblyMojo}.
 *
 * <p>When maven executes the {@code assembly} mojo on the {@code karaf-maven-plugin}
 * it instantiates the {@link AssemblyMojo}, populates all of its annotated {@code Parameter}
 * fields, then executes it. This class handles that execution, and uses the
 * {@link AssemblyMojo} as its parameter.</p>
 */
class AssemblyMojoExec {

    private static final Set<PosixFilePermission> EXECUTABLE_PERMISSIONS = PosixFilePermissions.fromString("rwxr-xr-x");

    private final Log log;

    private final Supplier<Builder> builderSupplier;

    AssemblyMojoExec(
            final Log log, final Supplier<Builder> builderSupplier
                    ) {
        this.log = log;
        this.builderSupplier = builderSupplier;
    }

    void doExecute(final AssemblyMojo mojo) throws Exception {
        validateAndCleanMojo(mojo);
        deleteAnyPreviouslyGeneratedAssembly(mojo);
        generateAssembly(mojo);
        addProjectBuildOutputToAssembly(mojo);
        overlayAssemblyFromProjectFiles(mojo);
        markAssemblyBinFilesAsExecutable(mojo);
    }

    private void deleteAnyPreviouslyGeneratedAssembly(final AssemblyMojo mojo) {
        IoUtils.deleteRecursive(mojo.getWorkDirectory());
        mojo.getWorkDirectory()
            .mkdirs();
    }

    private void addProjectBuildOutputToAssembly(final AssemblyMojo mojo) throws IOException {
        if (mojo.getIncludeBuildOutputDirectory()) {
            IoUtils.copyDirectory(new File(mojo.getProject()
                                               .getBuild()
                                               .getOutputDirectory()), mojo.getWorkDirectory());
        }
    }

    private void overlayAssemblyFromProjectFiles(final AssemblyMojo mojo) throws IOException {
        if (mojo.getSourceDirectory()
                .exists()) {
            IoUtils.copyDirectory(mojo.getSourceDirectory(), mojo.getWorkDirectory());
        }
    }

    private void markAssemblyBinFilesAsExecutable(final AssemblyMojo mojo) {
        findPosixFileSystem(mojo.getWorkDirectory()).map(workDirectory -> new File(workDirectory, "bin"))
                                                    .map(binDirectory -> binDirectory.listFiles(nonBatchFiles()))
                                                    .map(Stream::of)
                                                    .ifPresent(files -> files.map(File::getAbsolutePath)
                                                                             .map(Paths::get)
                                                                             .forEach(this::setFilePermissions));
    }

    private FileFilter nonBatchFiles() {
        return pathname -> !pathname.toString()
                                    .endsWith(".bat");
    }

    private Optional<File> findPosixFileSystem(final File directory) {
        return directory.toPath()
                        .getFileSystem()
                        .supportedFileAttributeViews()
                        .stream()
                        .filter("posix"::matches)
                        .findAny()
                        .map(v -> directory);
    }

    private void setFilePermissions(final Path filename) {
        try {
            Files.setPosixFilePermissions(filename, EXECUTABLE_PERMISSIONS);
        } catch (IOException e) {
            // non-posix filesystem should never have gotten this far
        }
    }

    private void generateAssembly(final AssemblyMojo mojo) throws Exception {
        final Builder builder = builderSupplier.get();

        builder.offline(mojo.getMavenSession()
                            .isOffline());
        builder.localRepository(mojo.getLocalRepo()
                                    .getBasedir());
        builder.javase(mojo.getJavase());
        if (mojo.getConfig() != null) {
            mojo.getConfig()
                .forEach(builder::config);
        }
        if (mojo.getSystem() != null) {
            mojo.getSystem()
                .forEach(builder::system);
        }
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

        Map<String, String> urls = new HashMap<>();
        List<Artifact> artifacts = new ArrayList<>(mojo.getProject()
                                                       .getAttachedArtifacts());
        artifacts.add(mojo.getProject()
                          .getArtifact());
        for (Artifact artifact : artifacts) {
            if (artifact.getFile() != null && artifact.getFile()
                                                      .exists()) {
                String mvnUrl =
                        "mvn:" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/" + artifact.getVersion();
                String type = artifact.getType();
                if ("bundle".equals(type)) {
                    type = "jar";
                }
                if (!"jar".equals(type) || artifact.getClassifier() != null) {
                    mvnUrl += "/" + type;
                    if (artifact.getClassifier() != null) {
                        mvnUrl += "/" + artifact.getClassifier();
                    }
                }
                urls.put(
                        mvnUrl, artifact.getFile()
                                        .toURI()
                                        .toString());
            }
        }
        if (mojo.getTranslatedUrls() != null) {
            urls.putAll(mojo.getTranslatedUrls());
        }
        builder.translatedUrls(urls);

        // creating system directory
        log.info("Creating work directory");
        builder.homeDirectory(mojo.getWorkDirectory()
                                  .toPath());

        List<String> startupKars = new ArrayList<>();
        List<String> bootKars = new ArrayList<>();
        List<String> installedKars = new ArrayList<>();

        // Loading kars and features repositories
        log.info("Loading kar and features repositories dependencies");
        final List<String> startupBundles = mojo.getStartupBundles();
        final List<String> bootBundles = mojo.getBootBundles();
        final List<String> installedBundles = mojo.getInstalledBundles();
        for (Artifact artifact : mojo.getProject()
                                     .getDependencyArtifacts()) {
            Builder.Stage stage;
            switch (artifact.getScope()) {
                case "compile":
                    stage = Builder.Stage.Startup;
                    break;
                case "runtime":
                    stage = Builder.Stage.Boot;
                    break;
                case "provided":
                    stage = Builder.Stage.Installed;
                    break;
                default:
                    continue;
            }
            final String uri = artifactToMvnUri(artifact);
            if ("kar".equals(artifact.getType())) {
                addUriByStage(stage, uri, startupKars, bootKars, installedKars);
            } else if ("features".equals(artifact.getClassifier()) || "karaf".equals(artifact.getClassifier())) {
                addUriByStage(stage, uri, mojo.getStartupRepositories(), mojo.getBootRepositories(),
                              mojo.getInstalledRepositories()
                             );
            } else if ("jar".equals(artifact.getType()) || "bundle".equals(artifact.getType())) {
                addUriByStage(stage, uri, startupBundles, bootBundles, installedBundles);
            }
        }

        if (mojo.getLibraries() != null) {
            builder.libraries(mojo.getLibraries()
                                  .toArray(new String[mojo.getLibraries()
                                                          .size()]));
        }
        // Startup
        boolean hasFrameworkKar = false;
        for (String kar : startupKars) {
            if (kar.startsWith("mvn:org.apache.karaf.features/framework/") || kar.startsWith(
                    "mvn:org.apache.karaf.features/static/")) {
                hasFrameworkKar = true;
                startupKars.remove(kar);
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
               .kars(toArray(startupKars))
               .repositories(
                       startupFeatures.isEmpty() && startupProfiles.isEmpty() && mojo.getInstallAllFeaturesByDefault(),
                       toArray(mojo.getStartupRepositories())
                            )
               .features(toArray(startupFeatures))
               .bundles(toArray(startupBundles))
               .profiles(toArray(startupProfiles));
        // Boot
        final List<String> bootFeatures = mojo.getBootFeatures();
        final List<String> bootProfiles = mojo.getBootProfiles();
        builder.defaultStage(Builder.Stage.Boot)
               .kars(toArray(bootKars))
               .repositories(
                       bootFeatures.isEmpty() && bootProfiles.isEmpty() && mojo.getInstallAllFeaturesByDefault(),
                       toArray(mojo.getBootRepositories())
                            )
               .features(toArray(bootFeatures))
               .bundles(toArray(bootBundles))
               .profiles(toArray(bootProfiles));
        // Installed
        final List<String> installedFeatures = mojo.getInstalledFeatures();
        final List<String> installedProfiles = mojo.getInstalledProfiles();
        builder.defaultStage(Builder.Stage.Installed)
               .kars(toArray(installedKars))
               .repositories(installedFeatures.isEmpty() && installedProfiles.isEmpty()
                             && mojo.getInstallAllFeaturesByDefault(), toArray(mojo.getInstalledRepositories()))
               .features(toArray(installedFeatures))
               .bundles(toArray(installedBundles))
               .profiles(toArray(installedProfiles));

        // Generate the assembly
        builder.generateAssembly();
    }

    private void validateAndCleanMojo(final AssemblyMojo mojo) {
        setNullListsToEmpty(mojo);
        verifyProfilesUrlIsProvidedIfProfilesAreUsed(mojo);
        updateDeprecatedConfiguration(mojo);
    }

    private void updateDeprecatedConfiguration(final AssemblyMojo mojo) {
        final boolean featuresRepositoriesUsed = !mojo.getFeatureRepositories()
                                                      .isEmpty();
        if (featuresRepositoriesUsed) {
            log.warn("Use of featureRepositories is deprecated, use startupRepositories, bootRepositories or "
                     + "installedRepositories instead");
            mojo.getStartupRepositories()
                .addAll(mojo.getFeatureRepositories());
            mojo.getBootRepositories()
                .addAll(mojo.getFeatureRepositories());
            mojo.getInstalledRepositories()
                .addAll(mojo.getFeatureRepositories());
        }
    }

    private void verifyProfilesUrlIsProvidedIfProfilesAreUsed(final AssemblyMojo mojo) {
        final int startupProfileCount = mojo.getStartupProfiles()
                                            .size();
        final int bootProfileCount = mojo.getBootProfiles()
                                         .size();
        final int installedProfileCount = mojo.getInstalledProfiles()
                                              .size();
        if (startupProfileCount + bootProfileCount + installedProfileCount > 0) {
            if (mojo.getProfilesUri() == null) {
                throw new IllegalArgumentException("profilesDirectory must be specified");
            }
        }
    }

    private void setNullListsToEmpty(final AssemblyMojo mojo) {
        final Map<Supplier<List<String>>, Consumer<List<String>>> mappers = new HashMap<>();
        mappers.put(mojo::getBlacklistedBundles, mojo::setBlacklistedBundles);
        mappers.put(mojo::getBlacklistedFeatures, mojo::setBlacklistedFeatures);
        mappers.put(mojo::getBlacklistedProfiles, mojo::setBlacklistedProfiles);
        mappers.put(mojo::getBlacklistedRepositories, mojo::setBlacklistedRepositories);
        mappers.put(mojo::getBootBundles, mojo::setBootBundles);
        mappers.put(mojo::getBootFeatures, mojo::setBootFeatures);
        mappers.put(mojo::getBootProfiles, mojo::setBootProfiles);
        mappers.put(mojo::getBootRepositories, mojo::setBootRepositories);
        mappers.put(mojo::getFeatureRepositories, mojo::setFeatureRepositories);
        mappers.put(mojo::getInstalledBundles, mojo::setInstalledBundles);
        mappers.put(mojo::getInstalledFeatures, mojo::setInstalledFeatures);
        mappers.put(mojo::getInstalledProfiles, mojo::setInstalledProfiles);
        mappers.put(mojo::getInstalledRepositories, mojo::setInstalledRepositories);
        mappers.put(mojo::getLibraries, mojo::setLibraries);
        mappers.put(mojo::getPidsToExtract, mojo::setPidsToExtract);
        mappers.put(mojo::getStartupBundles, mojo::setStartupBundles);
        mappers.put(mojo::getStartupFeatures, mojo::setStartupFeatures);
        mappers.put(mojo::getStartupProfiles, mojo::setStartupProfiles);
        mappers.put(mojo::getStartupRepositories, mojo::setStartupRepositories);
        mappers.entrySet()
               .stream()
               .filter(entry -> entry.getKey()
                                     .get() == null)
               .map(Map.Entry::getValue)
               .forEach(setter -> setter.accept(new ArrayList<>()));
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

    private void addUriByStage(
            final Builder.Stage stage, final String uri, final List<String> startup, final List<String> boot,
            final List<String> installed
                              ) {
        final Map<Builder.Stage, List<String>> listByStage = new HashMap<>();
        listByStage.put(Builder.Stage.Startup, startup);
        listByStage.put(Builder.Stage.Boot, boot);
        listByStage.put(Builder.Stage.Installed, installed);
        Optional.ofNullable(listByStage.get(stage))
                .ifPresent(list -> list.add(uri));
    }

    private String artifactToMvnUri(final Artifact artifact) {
        final String classifier = Optional.ofNullable(artifact.getClassifier())
                                          .filter(c -> !"".matches(c))
                                          .map(c -> "/" + c)
                                          .orElse("");
        final String type = "/" + artifact.getArtifactHandler()
                                          .getExtension();
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

}
