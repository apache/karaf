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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.karaf.tooling.utils.IoUtils;
import org.apache.karaf.tooling.utils.MavenUtil;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.karaf.tooling.utils.ReactorMavenResolver;
import org.apache.karaf.tools.utils.model.KarafPropertyEdits;
import org.apache.karaf.tools.utils.model.io.stax.KarafPropertyInstructionsModelStaxReader;
import org.apache.karaf.util.Version;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.WorkspaceReader;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.FrameworkFactory;

import static java.util.stream.Collectors.toList;

/**
 * Creates a customized Karaf distribution by installing features and setting up
 * configuration files.
 *
 * <p>The plugin gets features from feature.xml files and KAR
 * archives declared as dependencies or as files configured with the
 * [startup|boot|installed]Respositories parameters. It picks up other files, such as config files,
 * from ${project.build.directory}/classes. Thus, a file in src/main/resources/etc
 * will be copied by the resource plugin to ${project.build.directory}/classes/etc,
 * and then added to the assembly by this goal.
 */
@Mojo(name = "assembly", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
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
     * Optional location for custom features processing XML configuration
     * (<code>etc/org.apache.karaf.features.cfg</code>)
     */
    @Parameter
    protected File featuresProcessing;

    /**
     * If greater than 0, the feature resolver concurrency, otherwise it defaults to the machine one.
     */
    @Parameter
    protected int resolverParallelism;

    /*
     * There are three builder stages related to maven dependency scopes:
     *  - Stage.Startup : scope=compile
     *  - Stage.Boot : scope=runtime
     *  - Stage.Installed : scope=provided
     * There's special category not related to stage - Blacklisted
     *
     * There are five kinds of artifacts/dependencies that may go into any of the above stages/categories/scopes:
     *  - kars: maven artifacts with "kar" type
     *  - repositories: maven artifacts with "features" classifier
     *  - features: Karaf feature names (name[/version])
     *  - bundles: maven artifacts with "jar" or "bundle" type
     *  - profiles: directories with Karaf 4 profiles
     * (Not all artifacts/dependencies may be connected with every stage/category/scope.)
     *
     * Blacklisting:
     *  - kars: there are no blacklisted kars
     *  - repositories: won't be processed at all (also affects transitive repositories)
     *  - features: will be removed from JAXB model of features XML after loading
     *  - bundles: will be removed from features of JAXB model after loading
     *  - profiles: will be removed
     *
     * Stage.Startup:
     *  - bundles: will be put to etc/startup.properties
     *  - features: their bundles will be put to etc/startup.properties
     *  - repositories: will be used to resolve startup bundles/feature before adding them to etc/startup.properties
     *  - kars: unpacked to assembly, detected features XML repositories added as Stage.Startup repositories
     *
     * Stage.Boot:
     *  - bundles: special etc/<UUID>.xml features XML file will be created with <UUID> feature.
     *      etc/org.apacha.karaf.features.cfg will have this features XML file in featuresRepositories property and
     *      the feature itself in featuresBoot property
     *  - features: will be added to etc/org.apacha.karaf.features.cfg file, featuresBoot property
     *      also features from Stage.Startup will be used here.
     *  - repositories: will be added to etc/org.apacha.karaf.features.cfg file, featuresRepositories property
     *      also repositories from Stage.Startup will be used here.
     *  - kars: unpacked to assembly, detected features XML repositories added as Stage.Boot repositories
     *
     * Stage.Installed:
     *  - bundles: will be copied to system/
     *  - features: their bundles and config files will be copied to system/
     *  - repositories: will be used to find Stage.Installed features
     *      also repositories from Stage.Boot will be searched for Stage.Installed features
     *  - kars: unpacked to assembly, detected features XML repositories added as Stage.Installed repositories
     */

    /**
     * For given stage (startup, boot, install) if there are no stage-specific features and profiles, all features
     * from stage-specific repositories will be used.
     */
    @Parameter(defaultValue = "true")
    protected boolean installAllFeaturesByDefault = true;

    /**
     * An environment identifier that may be used to select different variant of PID configuration file, e.g.,
     * <code>org.ops4j.pax.url.mvn.cfg#docker</code>.
     */
    @Parameter
    private String environment;

    /**
     * Default start level for bundles in features that don't specify it.
     */
    @Parameter
    protected int defaultStartLevel = 30;

    /**
     * List of additional allowed protocols on bundles location URI
     */
    @Parameter
    private List<String> extraProtocols;

    /**
     * List of compile-scope features XML files to be used in startup stage (etc/startup.properties)
     */
    @Parameter
    private List<String> startupRepositories;
    /**
     * List of runtime-scope features XML files to be used in boot stage (etc/org.apache.karaf.features.cfg)
     */
    @Parameter
    private List<String> bootRepositories;
    /**
     * List of provided-scope features XML files to be used in install stage
     */
    @Parameter
    private List<String> installedRepositories;
    /**
     * List of blacklisted repository URIs. Blacklisted URI may use globs and version ranges. See
     * {@link org.apache.karaf.features.LocationPattern}.
     */
    @Parameter
    private List<String> blacklistedRepositories;

    /**
     * List of features from compile-scope features XML files and KARs to be installed into system repo
     * and listed in etc/startup.properties.
     */
    @Parameter
    private List<String> startupFeatures;
    /**
     * List of features from runtime-scope features XML files and KARs to be installed into system repo
     * and listed in featuresBoot property in etc/org.apache.karaf.features.cfg
     */
    @Parameter
    private List<String> bootFeatures;
    /**
     * List of features from provided-scope features XML files and KARs to be installed into system repo
     * and not mentioned elsewhere.
     */
    @Parameter
    private List<String> installedFeatures;
    /**
     * <p>List of feature blacklisting clauses. Each clause is in one of the formats ({@link org.apache.karaf.features.FeaturePattern}):<ul>
     *     <li><code>feature-name</code></li>
     *     <li><code>feature-name;range=version-or-range</code></li>
     *     <li><code>feature-name/version-or-range</code></li>
     * </ul></p>
     */
    @Parameter
    private List<String> blacklistedFeatures;

    /**
     * List of compile-scope bundles added to etc/startup.properties
     */
    @Parameter
    private List<String> startupBundles;
    /**
     * List of runtime-scope bundles wrapped in special feature added to featuresBoot property
     * in etc/org.apache.karaf.features.cfg
     */
    @Parameter
    private List<String> bootBundles;
    /**
     * List of provided-scope bundles added to system repo
     */
    @Parameter
    private List<String> installedBundles;
    /**
     * List of blacklisted bundle URIs. Blacklisted URI may use globs and version ranges. See
     * {@link org.apache.karaf.features.LocationPattern}.
     */
    @Parameter
    private List<String> blacklistedBundles;

    /**
     * List of profile URIs to use
     */
    @Parameter
    private List<String> profilesUris;

    /**
     * List of profiles names to load from configured <code>profilesUris</code> and use as startup profiles.
     */
    @Parameter
    private List<String> startupProfiles;
    /**
     * List of profiles names to load from configured <code>profilesUris</code> and use as boot profiles.
     */
    @Parameter
    private List<String> bootProfiles;
    /**
     * List of profiles names to load from configured <code>profilesUris</code> and use as installed profiles.
     */
    @Parameter
    private List<String> installedProfiles;
    /**
     * List of blacklisted profile names (possibly using <code>*</code> glob)
     */
    @Parameter
    private List<String> blacklistedProfiles;

    /**
     * When assembly custom distribution, we can include generated and added profiles in the distribution itself,
     * in <code>${karaf.etc}/profiles</code> directory.
     */
    @Parameter(defaultValue = "false")
    private boolean writeProfiles;

    /**
     * When assembly custom distribution, we can also generate an XML/XSLT report with the summary of bundles.
     * This parameter specifies target directory, to which <code>bundle-report.xml</code> and <code>bundle-report-full.xml</code>
     * (along with XSLT stylesheet) will be written.
     */
    @Parameter
    private String generateConsistencyReport;

    /**
     * When generating consistency report, we can specify project name. By default it's "Apache Karaf"
     */
    @Parameter(defaultValue = "Apache Karaf")
    private String consistencyReportProjectName;

    /**
     * When generating consistency report, we can specify project version. By default it's "${project.version}"
     */
    @Parameter(defaultValue = "${project.version}")
    private String consistencyReportProjectVersion;

    /*
     * KARs are not configured using Maven plugin configuration, but rather detected from dependencies.
     * All KARs are just unzipped into the assembly being constructed, but additionally KAR's embedded
     * features XML repositories are added to relevant stage.
     */

    private List<String> startupKars = new ArrayList<>();
    private List<String> bootKars = new ArrayList<>();
    private List<String> installedKars = new ArrayList<>();

    /**
     * TODOCUMENT
     */
    @Parameter
    private Builder.BlacklistPolicy blacklistPolicy = Builder.BlacklistPolicy.Discard;

    /**
     * Ignore the dependency attribute (dependency="[true|false]") on bundles, effectively forcing their
     * installation.
     */
    @Parameter(defaultValue = "false")
    protected boolean ignoreDependencyFlag;

    /**
     * <p>Additional libraries to add into assembled distribution. Libraries are specified using
     * <code>name[;url:=&lt;url&gt;][;type:=&lt;type&gt;][;export:=true|false][;delegate:=true|false]</code>
     * syntax. If there's no <code>url</code> header directive, <code>name</code> is used as URI. Otherwise
     * <code>name</code> is used as target file name to use.
     *
     * <p><code>type</code> may be:<ul>
     *     <li>endorsed - library will be added to <code>${karaf.home}/lib/endorsed</code></li>
     *     <li>extension - library will be added to <code>${karaf.home}/lib/ext</code></li>
     *     <li>boot - library will be added to <code>${karaf.home}/lib/boot</code></li>
     *     <li>by default, library is put directly into <code>${karaf.home}/lib</code> - these libraries will
     *     be used in default classloader for OSGi framework which will load {@link FrameworkFactory} implementation.</li>
     * </ul>
     *
     * <p><code>export</code> flag determines whether packages from <code>Export-Package</code> manifest
     * header of the library will be added to <code>org.osgi.framework.system.packages.extra</code> property in
     * <code>${karaf.etc}/config.properties</code>.
     *
     * <p><code>delegate</code> flag determines whether packages from <code>Export-Pavkage</code> manifest
     * header of the library will be added to <code>org.osgi.framework.bootdelegation</code> property in
     * <code>${karaf.etc}/config.properties</code>.
     */
    @Parameter
    protected List<String> libraries;

    /**
     * Use <code>reference:file:gr/oup/Id/artifactId/version/artifactId-version-classifier.type</code> style
     * urls in <code>etc/startup.properties</code>.
     */
    // see:
    //  - org.apache.felix.framework.cache.BundleArchive.createRevisionFromLocation()
    //  - org.apache.karaf.main.Main.installAndStartBundles()
    @Parameter(defaultValue = "false")
    protected boolean useReferenceUrls;

    /**
     * Include project build output directory in the assembly. This allows (filtered or unfiltered) Maven
     * resources directories to be used to provide additional resources in the assembly.
     */
    @Parameter(defaultValue = "true")
    protected boolean includeBuildOutputDirectory;

    /**
     * Karaf version changes the way some configuration files are prepared (to adjust to given Karaf version
     * requirements).
     */
    @Parameter
    protected Builder.KarafVersion karafVersion = Builder.KarafVersion.v4x;

    /**
     * Specify the version of Java SE to be assumed for osgi.ee. The value will be used in
     * <code>etc/config.properties</code> file, in <code>java.specification.version</code> placeholder used in
     * several properties:<ul>
     *     <li><code>org.osgi.framework.system.packages</code></li>
     *     <li><code>org.osgi.framework.system.capabilities</code></li>
     * </ul>
     * <p>Valid values are: 1.6, 1.7, 1.8, 9
     */
    @Parameter(defaultValue = "1.8")
    protected String javase;

    /**
     * Specify which framework to use
     * (one of framework, framework-logback, static-framework, static-framework-logback, custom).
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

    @Parameter
    protected KarafPropertyEdits propertyEdits;

    /**
     * Glob specifying which configuration PIDs in the selected boot features
     * should be extracted to <code>${karaf.etc}</code> directory. By default all PIDs are extracted.
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

    /**
     * Specify a list of additional properties that should be added to <code>${karaf.etc}/config.properties</code>
     */
    @Parameter
    protected Map<String, String> config;

    /**
     * Specify a list of additional properties that should be added to <code>${karaf.etc}/system.properties</code>
     */
    @Parameter
    protected Map<String, String> system;


    /**
     * List of files to delete from the source assembly.
     * Note that it is done after the target assembly is done so it can also remove side effects of the configuration.
     */
    @Parameter
    protected List<String> filesToRemove;

    @Component(role = WorkspaceReader.class, hint = "reactor")
    protected WorkspaceReader reactor;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            setNullListsToEmpty();
            setNullMapsToEmpty();

            doExecute();
        } catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to build assembly", e);
        }
    }

    /**
     * Main processing method. Most of the work involves configuring and invoking {@link Builder a profile builder}.
     */
    protected void doExecute() throws Exception {
        if (!startupProfiles.isEmpty() || !bootProfiles.isEmpty() || !installedProfiles.isEmpty()) {
            if (profilesUris.size() == 0) {
                throw new IllegalArgumentException("profilesUris option must be specified");
            }
        }

        Builder builder = Builder.newInstance();

        // Set up miscellaneous options
        builder.extraProtocols(extraProtocols);
        builder.offline(mavenSession.isOffline());
        builder.localRepository(localRepo.getBasedir());
        builder.resolverWrapper((resolver) -> new ReactorMavenResolver(reactor, resolver));
        builder.javase(javase);
        builder.karafVersion(karafVersion);
        builder.useReferenceUrls(useReferenceUrls);
        builder.defaultAddAll(installAllFeaturesByDefault);
        builder.ignoreDependencyFlag(ignoreDependencyFlag);
        builder.propertyEdits(configurePropertyEdits());
        builder.translatedUrls(configureTranslatedUrls());
        builder.pidsToExtract(pidsToExtract);
        builder.writeProfiles(writeProfiles);
        builder.generateConsistencyReport(generateConsistencyReport);
        builder.setConsistencyReportProjectName(consistencyReportProjectName);
        builder.setConsistencyReportProjectVersion(consistencyReportProjectVersion);
        builder.environment(environment);
        builder.defaultStartLevel(defaultStartLevel);
        if (featuresProcessing != null) {
            builder.setFeaturesProcessing(featuresProcessing.toPath());
        }
        if (resolverParallelism > 0) {
            builder.resolverParallelism(resolverParallelism);
        }

        // Set up remote repositories from Maven build, to be used by pax-url-aether resolver
        String remoteRepositories = MavenUtil.remoteRepositoryList(project.getRemoteProjectRepositories());
        getLog().info("Using repositories:");
        for (String r : remoteRepositories.split(",")) {
            getLog().info("   " + r);
        }
        builder.mavenRepositories(remoteRepositories);

        // Set up config and system properties
        config.forEach(builder::config);
        system.forEach(builder::system);

        // Set up blacklisted items
        builder.blacklistBundles(blacklistedBundles);
        builder.blacklistFeatures(blacklistedFeatures);
        builder.blacklistProfiles(blacklistedProfiles);
        builder.blacklistRepositories(blacklistedRepositories);
        builder.blacklistPolicy(blacklistPolicy);

        // Creating system directory
        configureWorkDirectory();
        getLog().info("Creating work directory: " + workDirectory);
        builder.homeDirectory(workDirectory.toPath());

        // Loading KARs and features repositories
        getLog().info("Loading direct KAR and features XML dependencies");
        processDirectMavenDependencies();

        // Set up profiles and libraries
        profilesUris.forEach(builder::profilesUris);
        libraries.forEach(builder::libraries);

        // Startup stage
        detectStartupKarsAndFeatures(builder);
        builder.defaultStage(Builder.Stage.Startup)
               .kars(toArray(startupKars))
               .repositories(startupFeatures.isEmpty() && startupProfiles.isEmpty() && installAllFeaturesByDefault, toArray(startupRepositories))
               .features(toArray(startupFeatures))
               .bundles(toArray(startupBundles))
               .profiles(toArray(startupProfiles));

        // Installed stage
        builder.defaultStage(Builder.Stage.Installed)
                .kars(toArray(installedKars))
                .repositories(installedFeatures.isEmpty() && installedProfiles.isEmpty() && installAllFeaturesByDefault, toArray(installedRepositories))
                .features(toArray(installedFeatures))
                .bundles(toArray(installedBundles))
                .profiles(toArray(installedProfiles));

        // Boot stage
        builder.defaultStage(Builder.Stage.Boot)
                .kars(toArray(bootKars))
                .repositories(bootFeatures.isEmpty() && bootProfiles.isEmpty() && installAllFeaturesByDefault, toArray(bootRepositories))
                .features(toArray(bootFeatures))
                .bundles(toArray(bootBundles))
                .profiles(toArray(bootProfiles));

        // Generate the assembly
        builder.generateAssembly();

        // Include project classes content if not specified otherwise
        if (includeBuildOutputDirectory)
            IoUtils.copyDirectory(new File(project.getBuild().getOutputDirectory()), workDirectory);

        // Overwrite assembly dir contents with source directory (not filtered) when directory exists
        if (sourceDirectory.exists())
            IoUtils.copyDirectory(sourceDirectory, workDirectory);

        // Chmod the bin/* scripts
        File[] files = new File(workDirectory, "bin").listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.getName().endsWith(".bat")) {
                    try {
                        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"));
                    } catch (Throwable ignore) {
                        // we tried our best, perhaps the OS does not support POSIX file perms.
                    }
                }
            }
        }

        if (filesToRemove != null) {
            final Path base = workDirectory.toPath();
            filesToRemove.forEach(toDrop -> {
                final int lastSep = Math.max(toDrop.lastIndexOf('/'), toDrop.lastIndexOf(File.separatorChar));
                final boolean startsWithDir = toDrop.contains(File.separator) || toDrop.contains("/");
                final String name = !startsWithDir ? toDrop : toDrop.substring(lastSep + 1);
                final Path dir = !startsWithDir ? base : base.resolve(toDrop.substring(0, lastSep));
                final int wildcard = name.lastIndexOf('*');
                final Predicate<String> matcher;
                if (wildcard >= 0) {
                    final String suffix = name.substring(wildcard + 1);
                    final String prefix = name.substring(0, wildcard);
                    matcher = n -> n.startsWith(prefix) && n.endsWith(suffix);
                } else {
                    // we likely bet this case will not happen often (to ignore the version at least)
                    // so we don't optimize this branch by deleting directly the file
                    matcher = name::equals;
                }
                try {
                    final List<Path> toDelete = Files.list(dir)
                            .filter(it -> matcher.test(it.getFileName().toString()))
                            .collect(toList());
                    if (toDelete.isEmpty()) {
                        getLog().info("File deletion '" + toDrop + "' ignored (not found)");
                    } else {
                        toDelete.stream().peek(it -> getLog().info("Deleting '" + base.relativize(it) + "'")).forEach(it -> {
                            try {
                                if (Files.isDirectory(it)) {
                                    IoUtils.deleteRecursive(it.toFile());
                                } else {
                                    Files.delete(it);
                                }
                            } catch (final IOException e) {
                                throw new IllegalStateException(e);
                            }
                        });
                    }
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }

    private void configureWorkDirectory() {
        IoUtils.deleteRecursive(workDirectory);
        workDirectory.mkdirs();
        new File(workDirectory, "etc").mkdirs();
        new File(workDirectory, "system").mkdirs();
    }

    /**
     * <p>Turns direct maven dependencies into startup/boot/installed artifacts.</p>
     * <p>{@link MavenProject#getDependencyArtifacts()} is deprecated, but we don't want (?) transitive
     * dependencies given by {@link MavenProject#getArtifacts()}.</p>
     */
    @SuppressWarnings("deprecation")
    private void processDirectMavenDependencies() {
        for (Artifact artifact : project.getDependencyArtifacts()) {
            Builder.Stage stage = Builder.Stage.fromMavenScope(artifact.getScope());
            if (stage == null) {
                continue;
            }
            String uri = artifactToMvn(artifact);
            switch (getType(artifact)) {
                case "kar":
                    addUris(stage, uri, startupKars, bootKars, installedKars);
                    break;
                case "features":
                    addUris(stage, uri, startupRepositories, bootRepositories, installedRepositories);
                    break;
                case "bundle":
                    addUris(stage, uri, startupBundles, bootBundles, installedBundles);
                    break;
            }
        }
    }

    private void addUris(Builder.Stage stage, String uri, List<String> startup, List<String> boot, List<String> installed) {
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

    /**
     * <p>Custom distribution is created from at least one <em>startup KAR</em> and one <em>startup</em>
     * feature. Such startup KAR + feature is called <em>framework</em>.</p>
     *
     * <p>We can specify one of 5 <em>frameworks</em>:<ul>
     *     <li>framework: <code>mvn:org.apache.karaf.features/framework/VERSION/kar</code> and <code>framework</code> feature</li>
     *     <li>framework-logback: <code>mvn:org.apache.karaf.features/framework/VERSION/kar</code> and <code>framework-logback</code> feature</li>
     *     <li>static-framework: <code>mvn:org.apache.karaf.features/static/VERSION/kar</code> and <code>static-framework</code> feature</li>
     *     <li>static-framework-logback: <code>mvn:org.apache.karaf.features/static/VERSION/kar</code> and <code>static-framework-logback</code> feature</li>
     *     <li>custom: both startup KAR and startup feature has to be specified explicitly</li>
     * </ul></p>
     * @param builder
     */
    private void detectStartupKarsAndFeatures(Builder builder) {
        boolean hasStandardKarafFrameworkKar = false;
        boolean hasCustomFrameworkKar = false;
        for (Iterator<String> iterator = startupKars.iterator(); iterator.hasNext(); ) {
            String kar = iterator.next();
            if (kar.startsWith("mvn:org.apache.karaf.features/framework/")
                    || kar.startsWith("mvn:org.apache.karaf.features/static/")) {
                hasStandardKarafFrameworkKar = true;
                iterator.remove();
                if (framework == null) {
                    framework = kar.startsWith("mvn:org.apache.karaf.features/framework/")
                            ? "framework" : "static-framework";
                }
                getLog().info("   Standard startup Karaf KAR found: " + kar);
                builder.kars(Builder.Stage.Startup, false, kar);
                break;
            }
        }

        if (!hasStandardKarafFrameworkKar) {
            if ("custom".equals(framework)) {
                // we didn't detect standard Karaf KAR (framework or static), so we expect at least one
                // other KAR dependency with compile scope and at least one startup feature
                if (startupKars.isEmpty()) {
                    throw new IllegalArgumentException("Custom KAR was declared, but there's no Maven dependency with type=kar and scope=compile." +
                            " Please specify at least one KAR for custom assembly.");
                }
                if (startupFeatures.isEmpty()) {
                    throw new IllegalArgumentException("Custom KAR was declared, but there's no startup feature declared." +
                            " Please specify at least one startup feature defined in features XML repository inside custom startup KAR or startup repository.");
                }
                hasCustomFrameworkKar = true;
                for (String startupKar : startupKars) {
                    getLog().info("   Custom startup KAR found: " + startupKar);
                }
            } else if (framework == null) {
                throw new IllegalArgumentException("Can't determine framework to use (framework, framework-logback, static-framework, static-framework-logback, custom)." +
                        " Please specify valid \"framework\" option or add Maven dependency with \"kar\" type and \"compile\" scope for one of standard Karaf KARs.");
            } else {
                String realKarafVersion = Version.karafVersion();
                String kar;
                switch (framework) {
                    case "framework":
                    case "framework-logback":
                        kar = "mvn:org.apache.karaf.features/framework/" + realKarafVersion + "/kar";
                        break;
                    case "static-framework":
                    case "static-framework-logback":
                        kar = "mvn:org.apache.karaf.features/static/" + realKarafVersion + "/kar";
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported framework: " + framework);
                }
                getLog().info("   Standard startup KAR implied from framework (" + framework + "): " + kar);
                builder.kars(Builder.Stage.Startup, false, kar);
            }
        }

        if (hasStandardKarafFrameworkKar && !startupFeatures.contains(framework)) {
            getLog().info("   Feature " + framework + " will be added as a startup feature");
            builder.features(Builder.Stage.Startup, framework);
        }
    }

    private KarafPropertyEdits configurePropertyEdits() throws IOException, XMLStreamException {
        KarafPropertyEdits edits = null;
        if (propertyFileEdits != null) {
            File file = new File(propertyFileEdits);
            if (file.exists()) {
                try (InputStream editsStream = new FileInputStream(propertyFileEdits)) {
                    KarafPropertyInstructionsModelStaxReader kipmsr = new KarafPropertyInstructionsModelStaxReader();
                    edits = kipmsr.read(editsStream, true);
                }
            }
        }
        if (edits == null && propertyEdits != null) {
            edits = propertyEdits;
        } else if (edits != null && propertyEdits != null && !propertyEdits.getEdits().isEmpty()) {
            edits.getEdits().addAll(propertyEdits.getEdits());
        }
        return edits;
    }

    private Map<String,String> configureTranslatedUrls() {
        Map<String, String> urls = new HashMap<>();
        List<Artifact> artifacts = new ArrayList<>(project.getAttachedArtifacts());
        artifacts.add(project.getArtifact());
        for (Artifact artifact : artifacts) {
            if (artifact.getFile() != null && artifact.getFile().exists()) {
                String mvnUrl = artifactToMvn(artifact);
                urls.put(mvnUrl, artifact.getFile().toURI().toString());
            }
        }
        urls.putAll(translatedUrls);
        return urls;
    }

    private String getType(Artifact artifact) {
        // Identify kars
        if ("kar".equals(artifact.getType())) {
            return "kar";
        }
        if ("zip".equals(artifact.getType())) {
            try (ZipFile zip = new ZipFile(artifact.getFile())) {
                if (zip.getEntry("META-INF/KARAF.MF") != null) {
                    return "kar";
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        // Identify features
        if ("features".equals(artifact.getClassifier())) {
            return "features";
        }
        if ("xml".equals(artifact.getType())) {
            try (InputStream is = new FileInputStream(artifact.getFile())) {
                XMLInputFactory xif = XMLInputFactory.newFactory();
                xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
                xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
                xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
                XMLStreamReader r = xif.createXMLStreamReader(is);
                r.nextTag();
                QName name = r.getName();
                if (name.getLocalPart().equals("features")
                        && (name.getNamespaceURI().isEmpty()
                                || name.getNamespaceURI().startsWith("http://karaf.apache.org/xmlns/features/"))) {
                    return "features";
                }

            } catch (Exception e) {
                // Ignore
            }
        }
        // Identify bundles
        if ("bundle".equals(artifact.getType())) {
            return "bundle";
        }
        if ("jar".equals(artifact.getType())) {
            try (JarFile jar = new JarFile(artifact.getFile())) {
                Manifest manifest = jar.getManifest();
                if (manifest != null
                        && manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) != null) {
                    return "bundle";
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return "unknown";
    }

    private String artifactToMvn(Artifact artifact) {
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

    private void setNullListsToEmpty() {
        startupRepositories = nonNullList(startupRepositories);
        bootRepositories = nonNullList(bootRepositories);
        installedRepositories = nonNullList(installedRepositories);
        blacklistedRepositories = nonNullList(blacklistedRepositories);
        extraProtocols = nonNullList(extraProtocols);
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
        libraries = nonNullList(libraries);
        profilesUris = nonNullList(profilesUris);
    }

    private void setNullMapsToEmpty() {
        config = nonNullMap(config);
        system = nonNullMap(system);
        translatedUrls = nonNullMap(translatedUrls);
    }

    private List<String> nonNullList(List<String> list) {
        final List<String> nonNullList = list == null ? new ArrayList<>() : list;
        return nonNullList.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Map<String, String> nonNullMap(Map<String, String> map) {
        return map == null ? new LinkedHashMap<>() : map;
    }

}
