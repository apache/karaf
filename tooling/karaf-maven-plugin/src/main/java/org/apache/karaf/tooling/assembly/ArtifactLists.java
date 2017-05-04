package org.apache.karaf.tooling.assembly;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists of artifacts, kars and features.
 */
class ArtifactLists {

    private final List<String> startupKars = new ArrayList<>();

    private final List<String> bootKars = new ArrayList<>();

    private final List<String> installedKars = new ArrayList<>();

    private final List<String> startupBundles = new ArrayList<>();

    private final List<String> bootBundles = new ArrayList<>();

    private final List<String> installedBundles = new ArrayList<>();

    private final List<String> startupRepositories = new ArrayList<>();

    private final List<String> bootRepositories = new ArrayList<>();

    private final List<String> installedRepositories = new ArrayList<>();

    void addStartupBundles(final List<String> startup) {
        this.startupBundles.addAll(startup);
    }

    void addBootBundles(final List<String> boot) {
        this.bootBundles.addAll(boot);
    }

    void addInstalledBundles(final List<String> installed) {
        this.installedBundles.addAll(installed);
    }

    void addStartupRepositories(final List<String> startup) {
        this.startupRepositories.addAll(startup);
    }

    void addBootRepositories(final List<String> boot) {
        this.bootRepositories.addAll(boot);
    }

    void addInstalledRepositories(final List<String> installed) {
        this.installedRepositories.addAll(installed);
    }

    List<String> getStartupBundles() {
        return startupBundles;
    }

    List<String> getBootBundles() {
        return bootBundles;
    }

    List<String> getInstalledBundles() {
        return installedBundles;
    }

    List<String> getStartupKars() {
        return startupKars;
    }

    List<String> getBootKars() {
        return bootKars;
    }

    List<String> getInstalledKars() {
        return installedKars;
    }

    void removeStartupKar(final String kar) {
        startupKars.remove(kar);
    }

    List<String> getStartupRepositories() {
        return startupRepositories;
    }

    List<String> getBootRepositories() {
        return bootRepositories;
    }

    List<String> getInstalledRepositories() {
        return installedRepositories;
    }
}
