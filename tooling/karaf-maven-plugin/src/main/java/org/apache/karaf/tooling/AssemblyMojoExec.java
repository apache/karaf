package org.apache.karaf.tooling;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.karaf.tooling.utils.IoUtils;
import org.apache.karaf.tooling.utils.MavenUtil;
import org.apache.karaf.tools.utils.model.KarafPropertyEdits;
import org.apache.karaf.tools.utils.model.io.stax.KarafPropertyInstructionsModelStaxReader;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * Executor for the {@link AssemblyMojo}.
 *
 * <p>When maven executes the {@code assembly} mojo on the {@code karaf-maven-plugin}
 * it instantiates the {@link AssemblyMojo}, populates all of its annotated {@code Parameter}
 * fields, then executes it. This class handles that execution, and uses the
 * {@link AssemblyMojo} as its parameter.</p>
 */
public class AssemblyMojoExec {

    private final Log log;

    private final Supplier<Builder> builderSupplier;

    public AssemblyMojoExec(
            final Log log, final Supplier<Builder> builderSupplier
                           ) {
        this.log = log;
        this.builderSupplier = builderSupplier;
    }

    protected void doExecute(final AssemblyMojo mojo) throws Exception {
        final List<String> startupRepositories = nonNullList(mojo.getStartupRepositories());
        final List<String> bootRepositories = nonNullList(mojo.getBootRepositories());
        final List<String> installedRepositories = nonNullList(mojo.getInstalledRepositories());
        final List<String> startupBundles = nonNullList(mojo.getStartupBundles());
        final List<String> bootBundles = nonNullList(mojo.getBootBundles());
        final List<String> installedBundles = nonNullList(mojo.getInstalledBundles());
        final List<String> blacklistedBundles = nonNullList(mojo.getBlacklistedBundles());
        final List<String> startupFeatures = nonNullList(mojo.getStartupFeatures());
        final List<String> bootFeatures = nonNullList(mojo.getBootFeatures());
        final List<String> installedFeatures = nonNullList(mojo.getInstalledFeatures());
        final List<String> blacklistedFeatures = nonNullList(mojo.getBlacklistedFeatures());
        final List<String> startupProfiles = nonNullList(mojo.getStartupProfiles());
        final List<String> bootProfiles = nonNullList(mojo.getBootProfiles());
        final List<String> installedProfiles = nonNullList(mojo.getInstalledProfiles());
        final List<String> blacklistedProfiles = nonNullList(mojo.getBlacklistedProfiles());
        final List<String> blacklistedRepositories = nonNullList(mojo.getBlacklistedRepositories());

        if (!startupProfiles.isEmpty() || !bootProfiles.isEmpty() || !installedProfiles.isEmpty()) {
            if (mojo.getProfilesUri() == null) {
                throw new IllegalArgumentException("profilesDirectory must be specified");
            }
        }

        if (mojo.getFeatureRepositories() != null && !mojo.getFeatureRepositories()
                                                          .isEmpty()) {
            log.warn("Use of featureRepositories is deprecated, use startupRepositories, bootRepositories or "
                     + "installedRepositories instead");
            startupRepositories.addAll(mojo.getFeatureRepositories());
            bootRepositories.addAll(mojo.getFeatureRepositories());
            installedRepositories.addAll(mojo.getFeatureRepositories());
        }

        StringBuilder remote = new StringBuilder();
        for (RemoteRepository repository : mojo.getProject()
                                               .getRemoteProjectRepositories()) {
            if (remote.length() > 0) {
                remote.append(",");
            }
            remote.append(repository.getUrl());
            remote.append("@id=")
                  .append(repository.getId());
            if (!repository.getPolicy(false)
                           .isEnabled()) {
                remote.append("@noreleases");
            }
            if (repository.getPolicy(true)
                          .isEnabled()) {
                remote.append("@snapshots");
            }
        }
        log.info("Using repositories: " + remote.toString());

        Builder builder = builderSupplier.get();
        builder.offline(mojo.getMavenSession()
                            .isOffline());
        builder.localRepository(mojo.getLocalRepo()
                                    .getBasedir());
        builder.mavenRepositories(remote.toString());
        builder.javase(mojo.getJavase());

        // Set up config and system props
        if (mojo.getConfig() != null) {
            mojo.getConfig()
                .forEach(builder::config);
        }
        if (mojo.getSystem() != null) {
            mojo.getSystem()
                .forEach(builder::system);
        }

        // Set up blacklisted items
        builder.blacklistBundles(blacklistedBundles);
        builder.blacklistFeatures(blacklistedFeatures);
        builder.blacklistProfiles(blacklistedProfiles);
        builder.blacklistRepositories(blacklistedRepositories);
        builder.blacklistPolicy(mojo.getBlacklistPolicy());

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
                urls.put(mvnUrl, artifact.getFile()
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
        IoUtils.deleteRecursive(mojo.getWorkDirectory());
        mojo.getWorkDirectory()
            .mkdirs();

        List<String> startupKars = new ArrayList<>();
        List<String> bootKars = new ArrayList<>();
        List<String> installedKars = new ArrayList<>();

        // Loading kars and features repositories
        log.info("Loading kar and features repositories dependencies");
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
            if ("kar".equals(artifact.getType())) {
                String uri = artifactToMvn(artifact);
                addUriByStage(stage, uri, startupKars, bootKars, installedKars);
            } else if ("features".equals(artifact.getClassifier()) || "karaf".equals(artifact.getClassifier())) {
                String uri = artifactToMvn(artifact);
                addUriByStage(stage, uri, startupRepositories, bootRepositories, installedRepositories);
            } else if ("jar".equals(artifact.getType()) || "bundle".equals(artifact.getType())) {
                String uri = artifactToMvn(artifact);
                addUriByStage(stage, uri, startupBundles, bootBundles, installedBundles);
            }
        }

        builder.karafVersion(mojo.getKarafVersion())
               .useReferenceUrls(mojo.getUseReferenceUrls())
               .defaultAddAll(mojo.getInstallAllFeaturesByDefault())
               .ignoreDependencyFlag(mojo.getIgnoreDependencyFlag());
        if (mojo.getProfilesUri() != null) {
            builder.profilesUris(mojo.getProfilesUri());
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
        if (!startupFeatures.contains(mojo.getFramework())) {
            builder.features(Builder.Stage.Startup, mojo.getFramework());
        }
        builder.defaultStage(Builder.Stage.Startup)
               .kars(toArray(startupKars))
               .repositories(
                       startupFeatures.isEmpty() && startupProfiles.isEmpty() && mojo.getInstallAllFeaturesByDefault(),
                       toArray(startupRepositories)
                            )
               .features(toArray(startupFeatures))
               .bundles(toArray(startupBundles))
               .profiles(toArray(startupProfiles));
        // Boot
        builder.defaultStage(Builder.Stage.Boot)
               .kars(toArray(bootKars))
               .repositories(
                       bootFeatures.isEmpty() && bootProfiles.isEmpty() && mojo.getInstallAllFeaturesByDefault(),
                       toArray(bootRepositories)
                            )
               .features(toArray(bootFeatures))
               .bundles(toArray(bootBundles))
               .profiles(toArray(bootProfiles));
        // Installed
        builder.defaultStage(Builder.Stage.Installed)
               .kars(toArray(installedKars))
               .repositories(installedFeatures.isEmpty() && installedProfiles.isEmpty()
                             && mojo.getInstallAllFeaturesByDefault(), toArray(installedRepositories))
               .features(toArray(installedFeatures))
               .bundles(toArray(installedBundles))
               .profiles(toArray(installedProfiles));

        // Generate the assembly
        builder.generateAssembly();

        // Include project classes content
        if (mojo.getIncludeBuildOutputDirectory()) {
            IoUtils.copyDirectory(new File(mojo.getProject()
                                               .getBuild()
                                               .getOutputDirectory()), mojo.getWorkDirectory());
        }

        // Overwrite assembly dir contents
        if (mojo.getSourceDirectory()
                .exists()) {
            IoUtils.copyDirectory(mojo.getSourceDirectory(), mojo.getWorkDirectory());
        }

        // Chmod the bin/* scripts
        File[] files = new File(mojo.getWorkDirectory(), "bin").listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.getName()
                         .endsWith(".bat")) {
                    try {
                        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"));
                    } catch (Throwable ignore) {
                        // we tried our best, perhaps the OS does not support posix file perms.
                    }
                }
            }
        }
    }

    private void addUriByStage(
            final Builder.Stage stage, final String uri, final List<String> startup, final List<String> boot,
            final List<String> installed
                              ) {
        switch (stage) {
            case Startup:
                startup.add(uri);
                break;
            case Boot:
                boot.add(uri);
                break;
            case Installed:
                installed.add(uri);
                break;
        }
    }

    private String artifactToMvn(Artifact artifact) throws MojoExecutionException {
        String uri;

        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String version = artifact.getBaseVersion();
        String type = artifact.getArtifactHandler()
                              .getExtension();
        String classifier = artifact.getClassifier();

        if (MavenUtil.isEmpty(classifier)) {
            if ("jar".equals(type)) {
                uri = String.format("mvn:%s/%s/%s", groupId, artifactId, version);
            } else {
                uri = String.format("mvn:%s/%s/%s/%s", groupId, artifactId, version, type);
            }
        } else {
            uri = String.format("mvn:%s/%s/%s/%s/%s", groupId, artifactId, version, type, classifier);
        }
        return uri;
    }

    private String[] toArray(List<String> strings) {
        return strings.toArray(new String[strings.size()]);
    }

    private List<String> nonNullList(List<String> list) {
        return list == null ? new ArrayList<String>() : list;
    }

}
