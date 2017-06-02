package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.karaf.tools.utils.model.KarafPropertyEdits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spy for the {@link Builder}.
 */
class BuilderSpy extends Builder {

    private Map<Stage, List<String>> allFeaturesByStage = new HashMap<>();

    private Map<Stage, List<String>> allKarsByStage = new HashMap<>();

    private Map<Stage, List<String>> allRepositoriesByStage = new HashMap<>();

    private Map<Stage, List<String>> allBundlesByStage = new HashMap<>();

    private Map<Stage, List<String>> allProfilesByStage = new HashMap<>();

    private Map<String, String> allTranslatedUrls = new HashMap<>();

    private boolean propertyEditsCalled;

    private String allMavenRepositories;

    private List<String> allLibraries = new ArrayList<>();

    private boolean configCalled;

    private boolean systemCalled;

    private Exception exceptionToThrow;

    @Override
    public Builder features(final Stage stage, final String... features) {
        recordByStage(allFeaturesByStage, stage, features);
        return super.features(stage, features);
    }

    @Override
    public Builder kars(final Stage stage, final boolean addAll, final String... kars) {
        recordByStage(allKarsByStage, stage, kars);
        return super.kars(stage, addAll, kars);
    }

    @Override
    public Builder translatedUrls(final Map<String, String> translatedUrls) {
        allTranslatedUrls.putAll(translatedUrls);
        return super.translatedUrls(translatedUrls);
    }

    @Override
    public void generateAssembly() throws Exception {
        if (exceptionToThrow != null) {
            throw exceptionToThrow;
        }
    }

    @Override
    public Builder repositories(final Stage stage, final boolean addAll, final String... repositories) {
        recordByStage(allRepositoriesByStage, stage, repositories);
        return super.repositories(stage, addAll, repositories);
    }

    @Override
    public Builder bundles(final Stage stage, final String... bundles) {
        recordByStage(allBundlesByStage, stage, bundles);
        return super.bundles(stage, bundles);
    }

    @Override
    public Builder profiles(final Stage stage, final String... profiles) {
        recordByStage(allProfilesByStage, stage, profiles);
        return super.bundles(stage, profiles);
    }

    @Override
    public Builder propertyEdits(final KarafPropertyEdits propertyEdits) {
        propertyEditsCalled = true;
        return super.propertyEdits(propertyEdits);
    }

    @Override
    public Builder mavenRepositories(final String mavenRepositories) {
        this.allMavenRepositories = mavenRepositories;
        return super.mavenRepositories(mavenRepositories);
    }

    @Override
    public Builder libraries(final String... libraries) {
        this.allLibraries.addAll(Arrays.asList(libraries));
        return super.libraries(libraries);
    }

    @Override
    public Builder config(final String key, final String value) {
        configCalled = true;
        return super.config(key, value);
    }

    @Override
    public Builder system(final String key, final String value) {
        systemCalled = true;
        return super.system(key, value);
    }

    private void recordByStage(
            final Map<Stage, List<String>> listByStage, final Stage stage, final String[] items
                              ) {
        if (!listByStage.containsKey(stage)) {
            listByStage.put(stage, new ArrayList<>());
        }
        listByStage.get(stage)
                   .addAll(Arrays.asList(items));
    }

    List<String> getAllFeaturesForStage(final Stage stage) {
        return allFeaturesByStage.get(stage);
    }

    List<String> getAllKarsForStage(final Stage stage) {
        return allKarsByStage.get(stage);
    }

    List<String> getAllRepositoriesForStage(final Stage stage) {
        return allRepositoriesByStage.get(stage);
    }

    List<String> getAllBundlesForStage(final Stage stage) {
        return allBundlesByStage.get(stage);
    }

    Map<String, String> getAllTranslatedUrls() {
        return allTranslatedUrls;
    }

    boolean isPropertyEditsCalled() {
        return propertyEditsCalled;
    }

    List<String> getAllProfilesForStage(final Stage stage) {
        return allProfilesByStage.get(stage);
    }

    String getAllMavenRepositories() {
        return allMavenRepositories;
    }

    List<String> getAllLibraries() {
        return allLibraries;
    }

    boolean isConfigCalled() {
        return configCalled;
    }

    boolean isSystemCalled() {
        return systemCalled;
    }

    void willThrow(final Exception exceptionToThrow) {
        this.exceptionToThrow = exceptionToThrow;
    }
}
