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
package org.apache.karaf.tooling.features;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.karaf.tooling.utils.DependencyHelper;
import org.apache.karaf.tooling.utils.DependencyHelperFactory;
import org.apache.karaf.tooling.utils.IoUtils;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Installs kar dependencies into a server-under-construction in target/assembly
 */
@Mojo(name = "install-kars", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class InstallKarsMojo extends MojoSupport {

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
    private List<String> startupBundles;
    @Parameter
    private List<String> bootBundles;
    @Parameter
    private List<String> installedBundles;
    
    @Parameter
    private String profilesUri;

    @Parameter
    private List<String> bootProfiles;

    @Parameter
    private List<String> startupProfiles;

    @Parameter
    private List<String> installedProfiles;

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

    @Parameter
    protected boolean installAllFeaturesByDefault = true;

    @Parameter
    protected Builder.KarafVersion karafVersion = Builder.KarafVersion.v4x;

    // an access layer for available Aether implementation
    protected DependencyHelper dependencyHelper;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.dependencyHelper = DependencyHelperFactory.createDependencyHelper(this.container, this.project, this.mavenSession, getLog());
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
        startupFeatures = nonNullList(startupFeatures);
        bootFeatures = nonNullList(bootFeatures);
        installedFeatures = nonNullList(installedFeatures);
        startupProfiles = nonNullList(startupProfiles);
        bootProfiles = nonNullList(bootProfiles);
        installedProfiles = nonNullList(installedProfiles);

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

        Builder builder = Builder.newInstance();

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
                String uri = dependencyHelper.artifactToMvn(artifact);
                switch (stage) {
                case Startup:   startupKars.add(uri); break;
                case Boot:      bootKars.add(uri); break;
                case Installed: installedKars.add(uri); break;
                }
            } else if ("features".equals(artifact.getClassifier())) {
                String uri = dependencyHelper.artifactToMvn(artifact);
                switch (stage) {
                case Startup:   startupRepositories.add(uri); break;
                case Boot:      bootRepositories.add(uri); break;
                case Installed: installedRepositories.add(uri); break;
                }
            } else if ("jar".equals(artifact.getType()) || "bundle".equals(artifact.getType())) {
                String uri = dependencyHelper.artifactToMvn(artifact);
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

        builder.generateAssembly();
    }

    private String[] toArray(List<String> strings) {
        return strings.toArray(new String[strings.size()]);
    }

    private List<String> nonNullList(List<String> list) {
        return list == null ? new ArrayList<String>() : list;
    }

}
