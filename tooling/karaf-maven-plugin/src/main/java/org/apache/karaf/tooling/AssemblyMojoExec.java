package org.apache.karaf.tooling;

import org.apache.karaf.tooling.utils.IoUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
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
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
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

    private final BuilderFactory builderFactory;

    AssemblyMojoExec(final Log log, final BuilderFactory builderFactory) {
        this.log = log;
        this.builderFactory = builderFactory;
    }

    void doExecute(final AssemblyMojo mojo) throws Exception {
        validateAndCleanMojo(mojo);
        deleteAnyPreviouslyGeneratedAssembly(mojo);
        generateAssemblyDirectory(mojo);
    }

    private void validateAndCleanMojo(final AssemblyMojo mojo) {
        setNullListsToEmpty(mojo);
        setNullMapsToEmpty(mojo);
        verifyProfilesUrlIsProvidedIfProfilesAreUsed(mojo);
        updateDeprecatedConfiguration(mojo);
    }


    private void deleteAnyPreviouslyGeneratedAssembly(final AssemblyMojo mojo) {
        IoUtils.deleteRecursive(mojo.getWorkDirectory());
        mojo.getWorkDirectory()
            .mkdirs();
    }

    private void generateAssemblyDirectory(final AssemblyMojo mojo) throws Exception {
        builderFactory.create(mojo)
                      .generateAssembly();
        addProjectBuildOutputToAssembly(mojo);
        overlayAssemblyFromProjectFiles(mojo);
        markAssemblyBinFilesAsExecutable(mojo);
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

    private void setNullMapsToEmpty(final AssemblyMojo mojo) {
        final Map<Supplier<Map<String, String>>, Consumer<Map<String, String>>> mappers = new HashMap<>();
        mappers.put(mojo::getTranslatedUrls, mojo::setTranslatedUrls);
        mappers.put(mojo::getConfig, mojo::setConfig);
        mappers.put(mojo::getSystem, mojo::setSystem);
        mappers.entrySet()
               .stream()
               .filter(entry -> entry.getKey()
                                     .get() == null)
               .map(Map.Entry::getValue)
               .forEach(setter -> setter.accept(new HashMap<>()));
    }

    private void verifyProfilesUrlIsProvidedIfProfilesAreUsed(final AssemblyMojo mojo) {
        if (profilesAreUsed(mojo) && mojo.getProfilesUri() == null) {
            throw new IllegalArgumentException("profilesDirectory must be specified");
        }
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
        whereIsPosix(mojo.getWorkDirectory()).map(workDirectory -> new File(workDirectory, "bin"))
                                             .map(binDirectory -> binDirectory.listFiles(nonBatchFiles()))
                                             .map(Stream::of)
                                             .ifPresent(files -> files.map(File::getAbsolutePath)
                                                                      .map(Paths::get)
                                                                      .forEach(this::setFilePermissions));
    }

    private boolean profilesAreUsed(final AssemblyMojo mojo) {
        final int startupProfileCount = mojo.getStartupProfiles()
                                            .size();
        final int bootProfileCount = mojo.getBootProfiles()
                                         .size();
        final int installedProfileCount = mojo.getInstalledProfiles()
                                              .size();
        return startupProfileCount + bootProfileCount + installedProfileCount > 0;
    }

    private Optional<File> whereIsPosix(final File directory) {
        return directory.toPath()
                        .getFileSystem()
                        .supportedFileAttributeViews()
                        .stream()
                        .filter("posix"::matches)
                        .findAny()
                        .map(v -> directory);
    }

    private FileFilter nonBatchFiles() {
        return pathname -> !pathname.toString()
                                    .endsWith(".bat");
    }

    private void setFilePermissions(final Path filename) {
        try {
            Files.setPosixFilePermissions(filename, EXECUTABLE_PERMISSIONS);
        } catch (IOException e) {
            // non-posix filesystem should never have gotten this far
        }
    }

}
