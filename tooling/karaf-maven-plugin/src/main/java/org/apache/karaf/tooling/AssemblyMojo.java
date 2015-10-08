/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.tooling;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.karaf.tooling.utils.IoUtils;
import org.apache.karaf.tooling.utils.MavenUtil;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

/**
 * Installs kar dependencies into a server-under-construction in target/assembly
 */
@Mojo(name = "assembly", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class AssemblyMojo extends MojoSupport {

    /**
     * Base directory used to copy the resources during the build (working directory).
     */
    @Parameter(defaultValue = "${project.build.directory}/assembly")
    protected File workDirectory;

    /**
     * Features configuration file (etc/org.apache.karaf.features.cfg).
     */
    @Parameter(defaultValue = "${project.build.directory}/assembly/etc/org.apache.karaf.features.cfg")
    protected File featuresCfgFile;

    /**
     * startup.properties file.
     */
    @Parameter(defaultValue = "${project.build.directory}/assembly/etc/startup.properties")
    protected File startupPropertiesFile;

    /**
     * Directory used during build to construction the Karaf system repository.
     */
    @Parameter(defaultValue="${project.build.directory}/assembly/system")
    protected File systemDirectory;

    /**
     * default start level for bundles in features that don't specify it.
     */
    @Parameter
    protected int defaultStartLevel = 30;

    @Parameter
    private List<String> startupRepositories;
    @Parameter
    private List<String> bootRepositories;
    @Parameter
    private List<String> installedRepositories;

    /**
     * List of features from runtime-scope features xml and kars to be installed into system and listed in startup.properties.
     */
    @Parameter
    private List<String> startupFeatures;

    /**
     * List of features from runtime-scope features xml and kars to be installed into system repo and listed in features service boot features.
     */
    @Parameter
    private List<String> bootFeatures;

    /**
     * List of features from runtime-scope features xml and kars to be installed into system repo and not mentioned elsewhere.
     */
    @Parameter
    private List<String> installedFeatures;

    @Parameter
    private List<String> blacklistedFeatures;

    @Parameter
    private List<String> startupBundles;
    @Parameter
    private List<String> bootBundles;
    @Parameter
    private List<String> installedBundles;
    @Parameter
    private List<String> blacklistedBundles;
    
    @Parameter
    private String profilesUri;

    @Parameter
    private List<String> bootProfiles;

    @Parameter
    private List<String> startupProfiles;

    @Parameter
    private List<String> installedProfiles;

    @Parameter
    private List<String> blacklistedProfiles;

    @Parameter
    private Builder.BlacklistPolicy blacklistPolicy;

    /**
     * Ignore the dependency attribute (dependency="[true|false]") on bundle
     */
    @Parameter(defaultValue = "false")
    protected boolean ignoreDependencyFlag;

    /**
     * Additional feature repositories
     */
    @Parameter
    protected List<String> featureRepositories;

    @Parameter
    protected List<String> libraries;

    /**
     * Use reference: style urls in startup.properties
     */
    @Parameter(defaultValue = "false")
    protected boolean useReferenceUrls;

    /**
     * Include project build output directory in the assembly
     */
    @Parameter(defaultValue = "true")
    protected boolean includeBuildOutputDirectory;

    @Parameter
    protected boolean installAllFeaturesByDefault = true;

    @Parameter
    protected Builder.KarafVersion karafVersion = Builder.KarafVersion.v4x;

    /**
     * Specify the version of Java SE to be assumed for osgi.ee.
     */
    @Parameter(defaultValue = "1.7")
    protected String javase;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            doExecute();
        }
        catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        }
        catch (Exception e) {
            throw new MojoExecutionException("Unable to build assembly", e);
        }
    }

    protected void doExecute() throws Exception {
        startupRepositories = nonNullList(startupRepositories);
        bootRepositories = nonNullList(bootRepositories);
        installedRepositories = nonNullList(installedRepositories);
        startupBundles = nonNullList(startupBundles);
        bootBundles = nonNullList(bootBundles);
        installedBundles = nonNullList(installedBundles);
        blacklistedBundles = nonNullList(blacklistedBundles);
        startupFeatures = nonNullList(startupFeatures);
        bootFeatures = nonNullList(bootFeatures);
        installedFeatures = nonNullList(installedFeatures);
        blacklistedFeatures = nonNullList(blacklistedFeatures);
        startupProfiles = nonNullList(startupProfiles);
        bootProfiles = nonNullList(bootProfiles);
        installedProfiles = nonNullList(installedProfiles);
        blacklistedProfiles = nonNullList(blacklistedProfiles);

        if (!startupProfiles.isEmpty() || !bootProfiles.isEmpty() || !installedProfiles.isEmpty()) {
            if (profilesUri == null) {
                throw new IllegalArgumentException("profilesDirectory must be specified");
            }
        }

        if (featureRepositories != null && !featureRepositories.isEmpty()) {
            getLog().warn("Use of featureRepositories is deprecated, use startupRepositories, bootRepositories or installedRepositories instead");
            startupRepositories.addAll(featureRepositories);
            bootRepositories.addAll(featureRepositories);
            installedRepositories.addAll(featureRepositories);
        }

        StringBuilder remote = new StringBuilder();
        for (Object obj : project.getRemoteProjectRepositories()) {
            if (remote.length() > 0) {
                remote.append(",");
            }
            remote.append(invoke(obj, "getUrl"));
            remote.append("@id=").append(invoke(obj, "getId"));
            if (!((Boolean) invoke(getPolicy(obj, false), "isEnabled"))) {
                remote.append("@noreleases");
            }
            if ((Boolean) invoke(getPolicy(obj, true), "isEnabled")) {
                remote.append("@snapshots");
            }
        }
        getLog().info("Using repositories: " + remote.toString());

        Builder builder = Builder.newInstance();
        builder.offline(mavenSession.isOffline());
        builder.localRepository(localRepo.getBasedir());
        builder.mavenRepositories(remote.toString());
        builder.javase(javase);

        // Set up blacklisted items
        builder.blacklistBundles(blacklistedBundles);
        builder.blacklistFeatures(blacklistedFeatures);
        builder.blacklistProfiles(blacklistedProfiles);
        builder.blacklistPolicy(blacklistPolicy);

        // creating system directory
        getLog().info("Creating work directory");
        builder.homeDirectory(workDirectory.toPath());
        IoUtils.deleteRecursive(workDirectory);
        workDirectory.mkdirs();

        List<String> startupKars = new ArrayList<>();
        List<String> bootKars = new ArrayList<>();
        List<String> installedKars = new ArrayList<>();

        // Loading kars and features repositories
        getLog().info("Loading kar and features repositories dependencies");
        for (Artifact artifact : project.getDependencyArtifacts()) {
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
                switch (stage) {
                case Startup:   startupKars.add(uri); break;
                case Boot:      bootKars.add(uri); break;
                case Installed: installedKars.add(uri); break;
                }
            } else if ("features".equals(artifact.getClassifier())) {
                String uri = artifactToMvn(artifact);
                switch (stage) {
                case Startup:   startupRepositories.add(uri); break;
                case Boot:      bootRepositories.add(uri); break;
                case Installed: installedRepositories.add(uri); break;
                }
            } else if ("jar".equals(artifact.getType()) || "bundle".equals(artifact.getType())) {
                String uri = artifactToMvn(artifact);
                switch (stage) {
                case Startup:   startupBundles.add(uri); break;
                case Boot:      bootBundles.add(uri); break;
                case Installed: installedBundles.add(uri); break;
                }
            }
        }

        builder.karafVersion(karafVersion)
               .useReferenceUrls(useReferenceUrls)
               .defaultAddAll(installAllFeaturesByDefault)
               .ignoreDependencyFlag(ignoreDependencyFlag);
        if (profilesUri != null) {
            builder.profilesUris(profilesUri);
        }
        if (libraries != null) {
            builder.libraries(libraries.toArray(new String[libraries.size()]));
        }
        // Startup
        builder.defaultStage(Builder.Stage.Startup)
               .kars(toArray(startupKars))
               .repositories(startupFeatures.isEmpty() && startupProfiles.isEmpty() && installAllFeaturesByDefault, toArray(startupRepositories))
               .features(toArray(startupFeatures))
               .bundles(toArray(startupBundles))
               .profiles(toArray(startupProfiles));
        // Boot
        builder.defaultStage(Builder.Stage.Boot)
                .kars(toArray(bootKars))
                .repositories(bootFeatures.isEmpty() && bootProfiles.isEmpty() && installAllFeaturesByDefault, toArray(bootRepositories))
                .features(toArray(bootFeatures))
                .bundles(toArray(bootBundles))
                .profiles(toArray(bootProfiles));
        // Installed
        builder.defaultStage(Builder.Stage.Installed)
                .kars(toArray(installedKars))
                .repositories(installedFeatures.isEmpty() && installedProfiles.isEmpty() && installAllFeaturesByDefault, toArray(installedRepositories))
                .features(toArray(installedFeatures))
                .bundles(toArray(installedBundles))
                .profiles(toArray(installedProfiles));

        // Generate the assembly
        builder.generateAssembly();

        // Include project classes content
        if (includeBuildOutputDirectory)
            IoUtils.copyDirectory(new File(project.getBuild().getOutputDirectory()), workDirectory);

        // Chmod the bin/* scripts
        File[] files = new File(workDirectory, "bin").listFiles();
        if( files!=null ) {
            for (File file : files) {
                if( !file.getName().endsWith(".bat") ) {
                    try {
                        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"));
                    } catch (Throwable ignore) {
                        // we tried our best, perhaps the OS does not support posix file perms.
                    }
                }
            }
        }
    }

    private Object invoke(Object object, String getter) throws MojoExecutionException {
        try {
            return object.getClass().getMethod(getter).invoke(object);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to build remote repository from " + object.toString(), e);
        }
    }

    private Object getPolicy(Object object, boolean snapshots) throws MojoExecutionException {
        return invoke(object, "getPolicy", new Class[] { Boolean.TYPE }, new Object[] { snapshots });
    }

    private Object invoke(Object object, String getter, Class[] types, Object[] params) throws MojoExecutionException {
        try {
            return object.getClass().getMethod(getter, types).invoke(object, params);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to build remote repository from " + object.toString(), e);
        }
    }

    private String artifactToMvn(Artifact artifact) throws MojoExecutionException {
        String uri;

        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String version = artifact.getBaseVersion();
        String type = artifact.getArtifactHandler().getExtension();
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
