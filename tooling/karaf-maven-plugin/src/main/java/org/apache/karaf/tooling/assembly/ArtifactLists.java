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

    void addStartupKars(final List<String> startup) {
        this.startupKars.addAll(startup);
    }

    void addBootKars(final List<String> boot) {
        this.bootKars.addAll(boot);
    }

    void addInstalledKars(final List<String> installed) {
        this.installedKars.addAll(installed);
    }

    void addStartupBundles(final List<String> startup) {
        this.startupBundles.addAll(startup);
    }

    void addBootBundles(final List<String> boot) {
        this.bootBundles.addAll(boot);
    }

    void addInstalledBundles(final List<String> installed) {
        this.installedBundles.addAll(installed);
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
}
