package org.apache.karaf.tooling.assembly;

import org.apache.karaf.tooling.utils.IoUtils;
import org.apache.maven.plugin.logging.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Executor for the {@link AssemblyMojo}.
 *
 * <p>When maven executes the {@code assembly} mojo on the {@code karaf-maven-plugin}
 * it instantiates the {@link AssemblyMojo}, populates all of its annotated {@code Parameter}
 * fields, then executes it. This class handles that execution, and uses the
 * {@link AssemblyMojo} as its parameter.</p>
 */
class AssemblyMojoExec {

    private final Log log;

    private final BuilderFactory builderFactory;

    private final AssemblyOutfitter assemblyOutfitter;

    AssemblyMojoExec(
            final Log log, final BuilderFactory builderFactory, final AssemblyOutfitter assemblyOutfitter
                    ) {
        this.log = log;
        this.builderFactory = builderFactory;
        this.assemblyOutfitter = assemblyOutfitter;
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
        assemblyOutfitter.outfit();
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
    private boolean profilesAreUsed(final AssemblyMojo mojo) {
        final int startupProfileCount = mojo.getStartupProfiles()
                                            .size();
        final int bootProfileCount = mojo.getBootProfiles()
                                         .size();
        final int installedProfileCount = mojo.getInstalledProfiles()
                                              .size();
        return startupProfileCount + bootProfileCount + installedProfileCount > 0;
    }

}
