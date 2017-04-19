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
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Creates a customized Karaf distribution by installing features and setting up
 * configuration files. The plugin gets features from feature.xml files and KAR
 * archives declared as dependencies or as files configured with the
 * featureRespositories parameter. It picks up other files, such as config files,
 * from ${project.build.directory}/classes. Thus, a file in src/main/resources/etc
 * will be copied by the resource plugin to ${project.build.directory}/classes/etc,
 * and then added to the assembly by this goal.
 */
@Mojo(name = "assembly", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME,
      threadSafe = true)
public class AssemblyMojo extends MojoSupport {

    /**
     * Base directory used to overwrite resources in generated assembly after the build (resource directory).
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources/assembly")
    protected File sourceDirectory;

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
    @Parameter(defaultValue = "${project.build.directory}/assembly/system")
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

    @Parameter
    private List<String> blacklistedRepositories;

    /**
     * List of features from runtime-scope features xml and kars to be installed into system and listed in
     * startup.properties.
     */
    @Parameter
    private List<String> startupFeatures;

    /**
     * List of features from runtime-scope features xml and kars to be installed into system repo and listed in features
     * service boot features.
     */
    @Parameter
    private List<String> bootFeatures;

    /**
     * List of features from runtime-scope features xml and kars to be installed into system repo and not mentioned
     * elsewhere.
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
    private Builder.BlacklistPolicy blacklistPolicy = Builder.BlacklistPolicy.Discard;

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
    @Parameter(defaultValue = "1.8")
    protected String javase;

    /**
     * Specify which framework to use
     * (one of framework, framework-logback, static-framework, static-framework-logback).
     */
    @Parameter
    protected String framework;

    /**
     * Specify an XML file that instructs this goal to apply edits to
     * one or more standard Karaf property files.
     * The contents of this file are documented in detail on
     * <a href="karaf-property-instructions-model.html">this page</a>.
     * This allows you to
     * customize these files without making copies in your resources
     * directories. Here's a simple example:
     * <pre>
     * {@literal
      <property-edits xmlns="http://karaf.apache.org/tools/property-edits/1.0.0">
         <edits>
          <edit>
            <file>config.properties</file>
            <operation>put</operation>
            <key>karaf.framework</key>
            <value>equinox</value>
          </edit>
          <edit>
            <file>config.properties</file>
            <operation>extend</operation>
            <key>org.osgi.framework.system.capabilities</key>
            <value>my-magic-capability</value>
          </edit>
          <edit>
            <file>config.properties</file>
            <operation prepend='true'>extend</operation>
            <key>some-other-list</key>
            <value>my-value-goes-first</value>
            </edit>
         </edits>
      </property-edits>
    }
     </pre>
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/karaf/assembly-property-edits.xml")
    protected String propertyFileEdits;

    /**
     * Glob specifying which configuration pids in the selected boot features
     * should be extracted to the etc directory.
     */
    @Parameter
    protected List<String> pidsToExtract = Collections.singletonList("*");

    /**
     * Specify a set of translated urls to use instead of downloading the artifacts
     * from their original locations.  The given set will be extended with already
     * built artifacts from the maven project.
     */
    @Parameter
    protected Map<String, String> translatedUrls;

    @Parameter
    protected Map<String, String> config;

    @Parameter
    protected Map<String, String> system;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            new AssemblyMojoExec(getLog(), Builder::newInstance).doExecute(this);
        } catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to build assembly", e);
        }
    }

    File getSourceDirectory() {
        return sourceDirectory;
    }

    void setSourceDirectory(final File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    @Override
    public File getWorkDirectory() {
        return workDirectory;
    }

    void setWorkDirectory(final File workDirectory) {
        this.workDirectory = workDirectory;
    }

    List<String> getStartupRepositories() {
        return startupRepositories;
    }

    void setStartupRepositories(final List<String> startupRepositories) {
        this.startupRepositories = startupRepositories;
    }

    List<String> getBootRepositories() {
        return bootRepositories;
    }

    void setBootRepositories(final List<String> bootRepositories) {
        this.bootRepositories = bootRepositories;
    }

    List<String> getInstalledRepositories() {
        return installedRepositories;
    }

    void setInstalledRepositories(final List<String> installedRepositories) {
        this.installedRepositories = installedRepositories;
    }

    List<String> getBlacklistedRepositories() {
        return blacklistedRepositories;
    }

    List<String> getStartupFeatures() {
        return startupFeatures;
    }

    void setStartupFeatures(final List<String> startupFeatures) {
        this.startupFeatures = startupFeatures;
    }

    List<String> getBootFeatures() {
        return bootFeatures;
    }

    void setBootFeatures(final List<String> bootFeatures) {
        this.bootFeatures = bootFeatures;
    }

    List<String> getInstalledFeatures() {
        return installedFeatures;
    }

    void setInstalledFeatures(final List<String> installedFeatures) {
        this.installedFeatures = installedFeatures;
    }

    List<String> getBlacklistedFeatures() {
        return blacklistedFeatures;
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

    List<String> getBlacklistedBundles() {
        return blacklistedBundles;
    }

    String getProfilesUri() {
        return profilesUri;
    }

    void setProfilesUri(final String profilesUri) {
        this.profilesUri = profilesUri;
    }

    List<String> getBootProfiles() {
        return bootProfiles;
    }

    void setBootProfiles(final List<String> bootProfiles) {
        this.bootProfiles = bootProfiles;
    }

    List<String> getStartupProfiles() {
        return startupProfiles;
    }

    void setStartupProfiles(final List<String> startupProfiles) {
        this.startupProfiles = startupProfiles;
    }

    List<String> getInstalledProfiles() {
        return installedProfiles;
    }

    void setInstalledProfiles(final List<String> installedProfiles) {
        this.installedProfiles = installedProfiles;
    }

    List<String> getBlacklistedProfiles() {
        return blacklistedProfiles;
    }

    Builder.BlacklistPolicy getBlacklistPolicy() {
        return blacklistPolicy;
    }

    List<String> getFeatureRepositories() {
        return featureRepositories;
    }

    void setFeatureRepositories(final List<String> featureRepositories) {
        this.featureRepositories = featureRepositories;
    }

    List<String> getLibraries() {
        return libraries;
    }

    void setLibraries(final List<String> libraries) {
        this.libraries = libraries;
    }

    void setIncludeBuildOutputDirectory(final boolean includeBuildOutputDirectory) {
        this.includeBuildOutputDirectory = includeBuildOutputDirectory;
    }

    void setInstallAllFeaturesByDefault(final boolean installAllFeaturesByDefault) {
        this.installAllFeaturesByDefault = installAllFeaturesByDefault;
    }

    Builder.KarafVersion getKarafVersion() {
        return karafVersion;
    }

    String getJavase() {
        return javase;
    }

    void setJavase(final String javase) {
        this.javase = javase;
    }

    String getFramework() {
        return framework;
    }

    void setFramework(final String framework) {
        this.framework = framework;
    }

    String getPropertyFileEdits() {
        return propertyFileEdits;
    }

    void setPropertyFileEdits(final String propertyFileEdits) {
        this.propertyFileEdits = propertyFileEdits;
    }

    List<String> getPidsToExtract() {
        return pidsToExtract;
    }

    Map<String, String> getTranslatedUrls() {
        return translatedUrls;
    }

    void setTranslatedUrls(final Map<String, String> translatedUrls) {
        this.translatedUrls = translatedUrls;
    }

    Map<String, String> getConfig() {
        return config;
    }

    void setConfig(final Map<String, String> config) {
        this.config = config;
    }

    Map<String, String> getSystem() {
        return system;
    }

    void setSystem(final Map<String, String> system) {
        this.system = system;
    }

    MavenSession getMavenSession() {
        return mavenSession;
    }

    ArtifactRepository getLocalRepo() {
        return localRepo;
    }

    boolean getUseReferenceUrls() {
        return useReferenceUrls;
    }

    boolean getInstallAllFeaturesByDefault() {
        return installAllFeaturesByDefault;
    }

    boolean getIgnoreDependencyFlag() {
        return ignoreDependencyFlag;
    }

    boolean getIncludeBuildOutputDirectory() {
        return includeBuildOutputDirectory;
    }

    void setLocalRepo(final ArtifactRepository localRepo) {
        this.localRepo = localRepo;
    }

    void setProject(final MavenProject project) {
        this.project = project;
    }

    void setBlacklistedRepositories(final List<String> blacklistedRepositories) {
        this.blacklistedRepositories = blacklistedRepositories;
    }

    void setBlacklistedFeatures(final List<String> blacklistedFeatures) {
        this.blacklistedFeatures = blacklistedFeatures;
    }

    void setStartupBundles(final List<String> startupBundles) {
        this.startupBundles = startupBundles;
    }

    void setBootBundles(final List<String> bootBundles) {
        this.bootBundles = bootBundles;
    }

    void setInstalledBundles(final List<String> installedBundles) {
        this.installedBundles = installedBundles;
    }

    void setBlacklistedBundles(final List<String> blacklistedBundles) {
        this.blacklistedBundles = blacklistedBundles;
    }

    void setBlacklistedProfiles(final List<String> blacklistedProfiles) {
        this.blacklistedProfiles = blacklistedProfiles;
    }

    void setPidsToExtract(final List<String> pidsToExtract) {
        this.pidsToExtract = pidsToExtract;
    }

}
