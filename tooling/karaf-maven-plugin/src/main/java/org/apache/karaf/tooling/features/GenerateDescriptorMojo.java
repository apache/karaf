/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.features;

import static java.lang.String.format;
import static org.apache.karaf.deployer.kar.KarArtifactInstaller.FEATURE_CLASSIFIER;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.karaf.features.internal.model.*;
import org.apache.karaf.tooling.utils.DependencyHelper;
import org.apache.karaf.tooling.utils.DependencyHelperFactory;
import org.apache.karaf.tooling.utils.LocalDependency;
import org.apache.karaf.tooling.utils.ManifestUtils;
import org.apache.karaf.tooling.utils.MavenUtil;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.karaf.tooling.utils.SimpleLRUCache;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.xml.sax.SAXException;

/**
 * Generates the features XML file starting with an optional source feature.xml and adding
 * project dependencies as bundles and feature/car dependencies.
 *
 * NB this requires a recent maven-install-plugin such as 2.3.1
 */
@Mojo(name = "features-generate-descriptor", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class GenerateDescriptorMojo extends MojoSupport {

    /**
     * An (optional) input feature file to extend. The plugin reads this file, and uses it as a template
     * to create the output.
     * This is highly recommended as it is the only way to add <code>&lt;feature/&gt;</code>
     * elements to the individual features that are generated.  Note that this file is filtered using standard Maven
     * resource interpolation, allowing attributes of the input file to be set with information such as ${project.version}
     * from the current build.
     * <p/>
     * When dependencies are processed, if they are duplicated in this file, the dependency here provides the baseline
     * information and is supplemented by additional information from the dependency.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/feature/feature.xml")
    private File inputFile;

    /**
     * (wrapper) The filtered input file. This file holds the result of Maven resource interpolation and is generally
     * not necessary to change, although it may be helpful for debugging.
     */
    @Parameter(defaultValue = "${project.build.directory}/feature/filteredInputFeature.xml")
    private File filteredInputFile;

    /**
     * The file to generate.  This file is attached as a project output artifact with the classifier specified by
     * <code>attachmentArtifactClassifier</code>.
     */
    @Parameter(defaultValue = "${project.build.directory}/feature/feature.xml")
    private File outputFile;

    /**
     * Exclude some artifacts from the generated feature.
     * See addBundlesToPrimaryFeature for more details.
     *
     */
    @Parameter
    private List<String> excludedArtifactIds = new ArrayList<>();

    /**
     * The resolver to use for the feature.  Normally null or "OBR" or "(OBR)"
     */
    @Parameter(defaultValue = "${resolver}")
    private String resolver;

    /**
     * The artifact type for attaching the generated file to the project
     */
    @Parameter(defaultValue = "xml")
    private String attachmentArtifactType = "xml";

    /**
     * (wrapper) The artifact classifier for attaching the generated file to the project
     */
    @Parameter(defaultValue = "features")
    private String attachmentArtifactClassifier = "features";

    /**
     * Specifies whether features dependencies of this project will be included inline in the
     * final output (<code>true</code>), or simply referenced as output artifact dependencies (<code>false</code>).
     * If <code>true</code>, feature dependencies' xml descriptors are read and their contents added to the features descriptor under assembly.
     * If <code>false</code>, feature dependencies are added to the assembled feature as dependencies.
     * Setting this value to <code>true</code> is especially helpful in multiproject builds where subprojects build their own features
     * using <code>aggregateFeatures = false</code>, then combined with <code>aggregateFeatures = true</code> in an
     * aggregation project with explicit dependencies to the child projects.
     */
    @Parameter(defaultValue = "false")
    private boolean aggregateFeatures = false;

    /**
     * If present, the bundles added to the feature constructed from the dependencies will be marked with this default
     * startlevel.  If this parameter is not present, no startlevel attribute will be created. Finer resolution for specific
     * dependencies can be obtained by specifying the dependency in the file referenced by the <code>inputFile</code> parameter.
     */
    @Parameter
    private Integer startLevel;

    /**
     * Installation mode. If present, generate "feature.install" attribute:
     * <p/>
     * <a href="http://karaf.apache.org/xmlns/features/v1.1.0">Installation mode</a>
     * <p/>
     * Can be either manual or auto. Specifies whether the feature should be automatically installed when
     * dropped inside the deploy folder. Note: this attribute doesn't affect feature descriptors that are installed
     * from the feature:install command or as part of the etc/org.apache.karaf.features.cfg file.
     */
    @Parameter
    private String installMode;

    /**
     * Flag indicating whether transitive dependencies should be included (<code>true</code>) or not (<code>false</code>).
     * <p/>
     * N.B. Note the default value of this is true, but is suboptimal in cases where specific <code>&lt;feature/&gt;</code> dependencies are
     * provided by the <code>inputFile</code> parameter.
     */
    @Parameter(defaultValue = "true")
    private boolean includeTransitiveDependency;

    /**
     * Flag indicating whether the plugin should mark transitive dependencies' <code>&lt;bundle&gt;</code> elements as a dependency.
     * This flag has only an effect when {@link #includeTransitiveDependency} is <code>true</code>.
     */
    @Parameter(defaultValue = "false")
    private boolean markTransitiveAsDependency;

    /**
     * Flag indicating whether the plugin should mark dependencies' in the <code>runtime</code> scope <code>&lt;bundle&gt;</code> elements as a dependency.
     * This flag has only an effect when {@link #includeTransitiveDependency} is <code>true</code>.
     */
    @Parameter(defaultValue = "true")
    private boolean markRuntimeScopeAsDependency;

    /**
     * The standard behavior is to add dependencies as <code>&lt;bundle&gt;</code> elements to a <code>&lt;feature&gt;</code>
     * with the same name as the artifactId of the project.  This flag disables that behavior.
     * If this parameter is <code>true</code>, then two other parameters refine the list of bundles added to the primary feature:
     * <code>excludedArtifactIds</code> and <code>ignoreScopeProvided</code>. Each of these specifies dependent artifacts
     * that should <strong>not</strong> be added to the primary feature.
     * <p>
     *     Note that you may tune the <code>bundle</code> elements by including them in the <code>inputFile</code>.
     *     If the <code>inputFile</code> has a <code>feature</code> element for the primary feature, the plugin will
     *     respect it, so that you can, for example, set the <code>startLevel</code> or <code>start</code> attribute.
     * </p>
     *
     */
    @Parameter(defaultValue = "true")
    private boolean addBundlesToPrimaryFeature;

    /**
     * The standard behavior is to add any dependencies other than those in the <code>runtime</code> scope to the feature bundle.
     * Setting this flag to "true" disables adding any dependencies (transient or otherwise) that are in
     * <code>&lt;scope&gt;provided&lt;/scope&gt;</code>. See <code>addBundlesToPrimaryFeature</code> for more details.
     */
    @Parameter(defaultValue = "false")
    private boolean ignoreScopeProvided;

    /**
     * Flag indicating whether the main project artifact should be included (<code>true</code>) or not (<code>false</code>).
     * This parameter is useful when you add an execution of this plugin to a project with some packaging that is <strong>not</strong>
     * <code>feature</code>. If you don't set this, then you will get a feature that contains the dependencies but
     * not the primary artifact itself.
     * <p/>
     * Assumes the main project artifact is a bundle and the feature will be attached alongside using <code>attachmentArtifactClassifier</code>.
     */
    @Parameter(defaultValue = "false")
    private boolean includeProjectArtifact;

    /**
     * The name of the primary feature. This is the feature that will be created or modified to include the
     * main project artifact and/or the bundles.
     * @see #addBundlesToPrimaryFeature
     * @see #includeProjectArtifact
     */
    @Parameter(defaultValue = "${project.artifactId}")
    private String primaryFeatureName;

    /**
     * Flag indicating whether bundles should use the version range declared in the POM. If <code>false</code>,
     * the actual version of the resolved artifacts will be used.
     */
    @Parameter(defaultValue = "false")
    private boolean useVersionRange;

    /**
     * Flag indicating whether the plugin should determine whether transitive dependencies are declared with
     * a version range. If this flag is set to <code>true</code> and a transitive dependency has been found
     * which had been declared with a version range, that version range will be used to build the appropriate
     * bundle element instead of the newest version. This flag has only an effect when {@link #useVersionRange}
     * is <code>true</code>
     */
    @Parameter(defaultValue = "false")
    private boolean includeTransitiveVersionRanges;

    @Parameter
    private Boolean enableGeneration;

    /**
     * Flag indicating whether the plugin should simplify bundle dependencies. If the flag is set to {@code true}
     * and a bundle dependency is determined to be included in a feature dependency, the bundle dependency is
     * dropped.
     */
    @Parameter(defaultValue = "false")
    private boolean simplifyBundleDependencies;

    /**
     * Maximum size of the artifact LRU cache. This cache is used to prevent repeated artifact-to-file resolution.
     */
    @Parameter(defaultValue = "1024")
    private int artifactCacheSize;

    /**
     * Maximum size of the Features LRU cache. This cache is used to prevent repeated deserialization of features
     * XML files.
     */
    @Parameter(defaultValue = "256")
    private int featuresCacheSize;

    /**
     * Name of features which are prerequisites (they still need to be defined separately).
     */
    @Parameter
    private List<String> prerequisiteFeatures = new ArrayList<>();

    /**
     * Name of features which are dependencies (they still need to be defined separately).
     */
    @Parameter
    private List<String> dependencyFeatures = new ArrayList<>();

    @Parameter(defaultValue = "false")
    private boolean useJson;

    // *************************************************
    // READ-ONLY MAVEN PLUGIN PARAMETERS
    // *************************************************

    /**
     * We can't autowire strongly typed RepositorySystem from Aether because it may be Sonatype (Maven 3.0.x)
     * or Eclipse (Maven 3.1.x/3.2.x) implementation, so we switch to service locator.
     */
    @Component
    private PlexusContainer container;

    @Component
    private RepositorySystem repoSystem;

    @Component
    protected MavenResourcesFiltering mavenResourcesFiltering;

    @Component
    protected MavenFileFilter mavenFileFilter;

	@Component
	private ProjectBuilder mavenProjectBuilder;

    // dependencies we are interested in
    protected Collection<LocalDependency> localDependencies;

    // log of what happened during search
    protected String treeListing;

    // an access layer for available Aether implementation
    protected DependencyHelper dependencyHelper;

    // maven log
    private Log log;

    // If useVersionRange is true, this map will be used to cache
    // resolved MavenProjects
    private final Map<Artifact, MavenProject> resolvedProjects = new HashMap<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (enableGeneration == null) {
                String packaging = this.project.getPackaging();
                enableGeneration = !"feature".equals(packaging);
            }

            if (!enableGeneration) {
                if (inputFile.exists()) {
                    File dir = outputFile.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs()) {
                        throw new MojoExecutionException("Could not create directory for features file: " + dir);
                    }
                    filter(inputFile, outputFile);
                    getLog().info("Generation not enabled");
                    getLog().info("Attaching artifact");
                    //projectHelper.attachArtifact(project, attachmentArtifactType, attachmentArtifactClassifier, outputFile);
                    Artifact artifact = factory.createArtifactWithClassifier(project.getGroupId(), project.getArtifactId(), project.getVersion(), attachmentArtifactType, attachmentArtifactClassifier);
                    artifact.setFile(outputFile);
                    project.setArtifact(artifact);
                    return;
                }
            }

            this.dependencyHelper = DependencyHelperFactory.createDependencyHelper(this.container, this.project,
                this.mavenSession, this.artifactCacheSize, getLog());
            this.dependencyHelper.getDependencies(project, includeTransitiveDependency);
            this.localDependencies = dependencyHelper.getLocalDependencies();
            this.treeListing = dependencyHelper.getTreeListing();
            File dir = outputFile.getParentFile();
            if (dir.isDirectory() || dir.mkdirs()) {
                try (PrintStream out = new PrintStream(new FileOutputStream(outputFile))) {
                    writeFeatures(out);
                }
                getLog().info("Attaching features XML");
                // now lets attach it
                projectHelper.attachArtifact(project, attachmentArtifactType, attachmentArtifactClassifier, outputFile);
            } else {
                throw new MojoExecutionException("Could not create directory for features file: " + dir);
            }
        } catch (Exception e) {
            getLog().error(e.getMessage());
            throw new MojoExecutionException("Unable to create features.xml file: " + e, e);
        }
    }

	private MavenProject resolveProject(final Object artifact) throws MojoExecutionException {
		MavenProject resolvedProject = project;
		if (includeTransitiveVersionRanges) {
			resolvedProject = resolvedProjects.get(artifact);
			if (resolvedProject == null) {
				final ProjectBuildingRequest request = new DefaultProjectBuildingRequest();

				// Fixes KARAF-4626; if the system properties are not transferred to the request,
				// test-feature-use-version-range-transfer-properties will fail
				request.setSystemProperties(System.getProperties());

				request.setResolveDependencies(true);
				request.setRemoteRepositories(project.getPluginArtifactRepositories());
				request.setLocalRepository(localRepo);
				request.setProfiles(new ArrayList<>(mavenSession.getRequest().getProfiles()));
				request.setActiveProfileIds(new ArrayList<>(mavenSession.getRequest().getActiveProfiles()));
				dependencyHelper.setRepositorySession(request);
				final Artifact pomArtifact = repoSystem.createArtifact(dependencyHelper.getGroupId(artifact),
						dependencyHelper.getArtifactId(artifact), dependencyHelper.getBaseVersion(artifact), "pom");
				try {
					resolvedProject = mavenProjectBuilder.build(pomArtifact, request).getProject();
					resolvedProjects.put(pomArtifact, resolvedProject);
				} catch (final ProjectBuildingException e) {
					throw new MojoExecutionException(
							format("Maven-project could not be built for artifact %s", pomArtifact), e);
				}
			}
		}
		return resolvedProject;
	}

	private String getVersionOrRange(final Object parent, final Object artifact) throws MojoExecutionException {
		String versionOrRange = dependencyHelper.getBaseVersion(artifact);
		if (useVersionRange) {
			for (final org.apache.maven.model.Dependency dependency : resolveProject(parent).getDependencies()) {

				if (dependency.getGroupId().equals(dependencyHelper.getGroupId(artifact))
						&& dependency.getArtifactId().equals(dependencyHelper.getArtifactId(artifact))) {
					versionOrRange = dependency.getVersion();
					break;
				}
			}
		}
		return versionOrRange;
	}

    /*
     * Write all project dependencies as feature
     */
    private void writeFeatures(PrintStream out) throws ArtifactResolutionException, ArtifactNotFoundException,
            IOException, JAXBException, SAXException, ParserConfigurationException, XMLStreamException, MojoExecutionException {
        getLog().info("Generating feature descriptor file " + outputFile.getAbsolutePath());
        //read in an existing feature.xml
        ObjectFactory objectFactory = new ObjectFactory();
        Features features;
        if (inputFile.exists()) {
            filter(inputFile, filteredInputFile);
            features = readFeaturesFile(filteredInputFile);
        } else {
            features = objectFactory.createFeaturesRoot();
        }
        if (features.getName() == null) {
            features.setName(project.getArtifactId());
        }

        Feature feature = null;
        for (Feature test : features.getFeature()) {
            if (test.getName().equals(primaryFeatureName)) {
                feature = test;
            }
        }
        if (feature == null) {
            feature = objectFactory.createFeature();
            feature.setName(primaryFeatureName);
        }
        if (!feature.hasVersion()) {
            feature.setVersion(project.getArtifact().getBaseVersion());
        }
        if (feature.getDescription() == null) {
            feature.setDescription(project.getName());
        }
        if (installMode != null) {
            feature.setInstall(installMode);
        }
        if (project.getDescription() != null && feature.getDetails() == null) {
            feature.setDetails(project.getDescription());
        }
        if (includeProjectArtifact) {
            Bundle bundle = objectFactory.createBundle();
            bundle.setLocation(this.dependencyHelper.artifactToMvn(project.getArtifact(), project.getVersion()));
            if (startLevel != null) {
                bundle.setStartLevel(startLevel);
            }
            feature.getBundle().add(bundle);
        }
        boolean needWrap = false;

        // First pass to look for features
        // Track other features we depend on and their repositories (we track repositories instead of building them from
        // the feature's Maven artifact to allow for multi-feature repositories)
        // TODO Initialise the repositories from the existing feature file if any
        Map<Dependency, Feature> otherFeatures = new HashMap<>();
        Map<Feature, String> featureRepositories = new HashMap<>();
        FeaturesCache cache = new FeaturesCache(featuresCacheSize, artifactCacheSize);
        for (final LocalDependency entry : localDependencies) {
            Object artifact = entry.getArtifact();

            if (excludedArtifactIds.contains(this.dependencyHelper.getArtifactId(artifact))) {
                continue;
            }

            processFeatureArtifact(features, feature, otherFeatures, featureRepositories, cache, artifact,
                    entry.getParent(), true);
        }
        // Do not retain cache beyond this point
        cache = null;

        // Second pass to look for bundles
        if (addBundlesToPrimaryFeature) {
            localDependency:
            for (final LocalDependency entry : localDependencies) {
                Object artifact = entry.getArtifact();

                if (excludedArtifactIds.contains(this.dependencyHelper.getArtifactId(artifact))) {
                    continue;
                }

                if (!this.dependencyHelper.isArtifactAFeature(artifact)) {
                    String bundleName = this.dependencyHelper.artifactToMvn(artifact, getVersionOrRange(entry.getParent(), artifact));

                    for (ConfigFile cf : feature.getConfigfile()) {
                        if (bundleName.equals(cf.getLocation().replace('\n', ' ').trim())) {
                            // The bundle matches a configfile, ignore it
                            continue localDependency;
                        }
                    }

                    File bundleFile = this.dependencyHelper.resolve(artifact, getLog());
                    Manifest manifest = getManifest(bundleFile);
                    boolean bundleNeedsWrapping = false;
                    if (manifest == null || !ManifestUtils.isBundle(manifest)) {
                        bundleName = "wrap:" + bundleName;
                        bundleNeedsWrapping = true;
                    }

                    Bundle bundle = null;
                    for (Bundle b : feature.getBundle()) {
                        if (bundleName.equals(b.getLocation())) {
                            bundle = b;
                            break;
                        }
                    }
                    if (bundle == null) {
                        bundle = objectFactory.createBundle();
                        bundle.setLocation(bundleName);
                        // Check the features this feature depends on don't already contain the dependency
                        // TODO Perhaps only for transitive dependencies?
                        boolean includedTransitively =
                            simplifyBundleDependencies && isBundleIncludedTransitively(feature, otherFeatures, bundle);
                        if (!includedTransitively && (!"provided".equals(entry.getScope()) || !ignoreScopeProvided)) {
                            feature.getBundle().add(bundle);
                            needWrap |= bundleNeedsWrapping;
                        }

                        if (
                                (markRuntimeScopeAsDependency && "runtime".equals( entry.getScope() )) ||
                                (markTransitiveAsDependency && entry.isTransitive())
                        ) {
                            bundle.setDependency(true);
                        }
                    }

                    if (startLevel != null && bundle.getStartLevel() == 0) {
                        bundle.setStartLevel(startLevel);
                    }
                }
            }
        }

        if (needWrap) {
            Dependency wrapDependency = new Dependency();
            wrapDependency.setName("wrap");
            wrapDependency.setDependency(false);
            wrapDependency.setPrerequisite(true);
            feature.getFeature().add(wrapDependency);
        }

        if ((!feature.getBundle().isEmpty() || !feature.getFeature().isEmpty()) && !features.getFeature().contains(feature)) {
            features.getFeature().add(feature);
        }

        // Add any missing repositories for the included features
        for (Feature includedFeature : features.getFeature()) {
            for (Dependency dependency : includedFeature.getFeature()) {
                Feature dependedFeature = otherFeatures.get(dependency);
                if (dependedFeature != null && !features.getFeature().contains(dependedFeature)) {
                    String repository = featureRepositories.get(dependedFeature);
                    if (repository != null && !features.getRepository().contains(repository)) {
                        features.getRepository().add(repository);
                    }
                }
            }
        }
        if (useJson) {
            try {
                JacksonUtil.marshal(features, out);
            } catch (Exception e) {
                throw new MojoExecutionException("Can't create features json", e);
            }
        } else {
            JaxbUtil.marshal(features, out);
        }
        try {
            checkChanges(features, objectFactory);
        } catch (Exception e) {
            throw new MojoExecutionException("Features contents have changed", e);
        }
        getLog().info("...done!");
    }

    private void processFeatureArtifact(Features features, Feature feature, Map<Dependency, Feature> otherFeatures,
                                        Map<Feature, String> featureRepositories, FeaturesCache cache,
                                        Object artifact, Object parent, boolean add)
            throws MojoExecutionException, XMLStreamException, JAXBException, IOException {
        if (this.dependencyHelper.isArtifactAFeature(artifact) && FEATURE_CLASSIFIER.equals(
                this.dependencyHelper.getClassifier(artifact))) {
            File featuresFile = this.dependencyHelper.resolve(artifact, getLog());
            if (featuresFile == null || !featuresFile.exists()) {
                throw new MojoExecutionException(
                        "Cannot locate file for feature: " + artifact + " at " + featuresFile);
            }
            Features includedFeatures = cache.getFeature(featuresFile);
            for (String repository : includedFeatures.getRepository()) {
                processFeatureArtifact(features, feature, otherFeatures, featureRepositories, cache,
                        cache.getArtifact(repository), parent, false);
            }
            for (Feature includedFeature : includedFeatures.getFeature()) {
                Dependency dependency = new Dependency(includedFeature.getName(), includedFeature.getVersion());
                dependency.setPrerequisite(prerequisiteFeatures.contains(dependency.getName()));
                dependency.setDependency(dependencyFeatures.contains(dependency.getName()));
                // Determine what dependency we're actually going to use
                Dependency matchingDependency = findMatchingDependency(feature.getFeature(), dependency);
                if (matchingDependency != null) {
                    // The feature already has a matching dependency, merge
                    mergeDependencies(matchingDependency, dependency);
                    dependency = matchingDependency;
                }
                // We mustn't de-duplicate here, we may have seen a feature in !add mode
                otherFeatures.put(dependency, includedFeature);
                if (add) {
                    if (!feature.getFeature().contains(dependency)) {
                        feature.getFeature().add(dependency);
                    }
                    if (aggregateFeatures) {
                        features.getFeature().add(includedFeature);
                    }
                }
                if (!featureRepositories.containsKey(includedFeature)) {
                    featureRepositories.put(includedFeature,
                            this.dependencyHelper.artifactToMvn(artifact, getVersionOrRange(parent, artifact)));
                }
            }
        }
    }

    private static Dependency findMatchingDependency(Collection<Dependency> dependencies, Dependency reference) {
        String referenceName = reference.getName();
        for (Dependency dependency : dependencies) {
            if (referenceName.equals(dependency.getName())) {
                return dependency;
            }
        }
        return null;
    }

    private static void mergeDependencies(Dependency target, Dependency source) {
        if (target.getVersion() == null || Feature.DEFAULT_VERSION.equals(target.getVersion())) {
            target.setVersion(source.getVersion());
        }
        if (source.isDependency()) {
            target.setDependency(true);
        }
        if (source.isPrerequisite()) {
            target.setPrerequisite(true);
        }
    }

    private boolean isBundleIncludedTransitively(Feature feature, Map<Dependency, Feature> otherFeatures,
                                                 Bundle bundle) {
        for (Dependency dependency : feature.getFeature()) {
            // Match dependencies “generously” (we might be matching single-version dependencies with version ranges)
            Dependency otherDependency = findMatchingDependency(otherFeatures.keySet(), dependency);
            Feature otherFeature = otherDependency != null ? otherFeatures.get(otherDependency) : null;
            if (otherFeature != null) {
                if (otherFeature.getBundle().contains(bundle) || isBundleIncludedTransitively(otherFeature,
                    otherFeatures, bundle)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Extract the MANIFEST from the give file.
     */

    private Manifest getManifest(File file) {
        // In case of a maven build below the 'package' phase, references to the 'target/classes'
        // directories are passed in instead of jar-file references.
        if(file.isDirectory()) {
            File manifestFile = new File(file, "META-INF/MANIFEST.MF");
            if(manifestFile.exists() && manifestFile.isFile()) {
                try {
                    InputStream manifestInputStream = new FileInputStream(manifestFile);
                    return new Manifest(manifestInputStream);
                } catch (IOException e) {
                    getLog().warn("Error while reading artifact from directory", e);
                    return null;
                }
            }
            getLog().warn("Manifest not present in the module directory " + file.getAbsolutePath());
            return null;
        } else {
            final InputStream is;
            try {
                is = Files.newInputStream(file.toPath());
            } catch (Exception e) {
                getLog().warn("Error while opening artifact", e);
                return null;
            }

            try (BufferedInputStream bis = new BufferedInputStream(is)) {
                bis.mark(256 * 1024);

                try (JarInputStream jar = new JarInputStream(bis)) {
                    Manifest m = jar.getManifest();
                    if (m == null) {
                        getLog().warn("Manifest not present in the first entry of the zip - " + file.getName());
                    }
                    return m;
                }
            } catch (IOException e) {
                getLog().warn("Error while reading artifact", e);
                return null;
            }
        }
    }

    static Features readFeaturesFile(File featuresFile) throws XMLStreamException, JAXBException, IOException {
        if (JacksonUtil.isJson(featuresFile.toURI().toASCIIString())) {
            return JacksonUtil.unmarshal(featuresFile.toURI().toASCIIString());
        } else {
            return JaxbUtil.unmarshal(featuresFile.toURI().toASCIIString(), false);
        }
    }

    @Override
    public void setLog(Log log) {
        this.log = log;
    }

    @Override
    public Log getLog() {
        if (log == null) {
            setLog(new SystemStreamLog());
        }
        return log;
    }

    //------------------------------------------------------------------------//
    // dependency change detection

    /**
     * Master switch to look for and log changed dependencies.  If this is set to <code>true</code> and the file referenced by
     * <code>dependencyCache</code> does not exist, it will be unconditionally generated.  If the file does exist, it is
     * used to detect changes from previous builds and generate logs of those changes.  In that case,
     * <code>failOnDependencyChange = true</code> will cause the build to fail.
     */
    @Parameter(defaultValue = "false")
    private boolean checkDependencyChange;

    /**
     * (wrapper) Location of dependency cache.  This file is generated to contain known dependencies and is generally
     * located in SCM so that it may be used across separate developer builds. This is parameter is ignored unless
     * <code>checkDependencyChange</code> is set to <code>true</code>.
     */
    @Parameter(defaultValue = "${basedir}/src/main/history/dependencies.xml")
    private File dependencyCache;

    /**
     * Location of filtered dependency file.
     */
    @Parameter(defaultValue = "${basedir}/target/history/dependencies.xml", readonly = true)
    private File filteredDependencyCache;

    /**
     * Whether to fail on changed dependencies (default, <code>true</code>) or warn (<code>false</code>). This is parameter is ignored unless
     * <code>checkDependencyChange</code> is set to <code>true</code> and <code>dependencyCache</code> exists to compare
     * against.
     */
    @Parameter(defaultValue = "true")
    private boolean failOnDependencyChange;

    /**
     * Copies the contents of dependency change logs that are generated to stdout. This is parameter is ignored unless
     * <code>checkDependencyChange</code> is set to <code>true</code> and <code>dependencyCache</code> exists to compare
     * against.
     */
    @Parameter(defaultValue = "false")
    private boolean logDependencyChanges;

    /**
     * Whether to overwrite the file referenced by <code>dependencyCache</code> if it has changed.  This is parameter is
     * ignored unless <code>checkDependencyChange</code> is set to <code>true</code>, <code>failOnDependencyChange</code>
     * is set to <code>false</code> and <code>dependencyCache</code> exists to compare against.
     */
    @Parameter(defaultValue = "false")
    private boolean overwriteChangedDependencies;

    //filtering support
    /**
     * The character encoding scheme to be applied when filtering resources.
     */
    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    protected String encoding;

    /**
     * Expression preceded with the String won't be interpolated
     * \${foo} will be replaced with ${foo}
     */
    @Parameter(defaultValue = "${maven.resources.escapeString}")
    protected String escapeString = "\\";

    /**
     * System properties.
     */
    @Parameter
    protected Map<String, String> systemProperties;

    private void checkChanges(Features newFeatures, ObjectFactory objectFactory) throws Exception {
        if (checkDependencyChange) {
            //combine all the dependencies to one feature and strip out versions
            Features features = objectFactory.createFeaturesRoot();
            features.setName(newFeatures.getName());
            Feature feature = objectFactory.createFeature();
            features.getFeature().add(feature);
            for (Feature f : newFeatures.getFeature()) {
                for (Bundle b : f.getBundle()) {
                    Bundle bundle = objectFactory.createBundle();
                    bundle.setLocation(b.getLocation());
                    feature.getBundle().add(bundle);
                }
                for (Dependency d : f.getFeature()) {
                    Dependency dependency = objectFactory.createDependency();
                    dependency.setName(d.getName());
                    feature.getFeature().add(dependency);
                }
            }

            feature.getBundle().sort(Comparator.comparing(Bundle::getLocation));
            feature.getFeature().sort(Comparator.comparing(Dependency::getName));

            if (dependencyCache.exists()) {
                //filter dependencies file
                filter(dependencyCache, filteredDependencyCache);
                //read dependency types, convert to dependencies, compare.
                Features oldfeatures = readFeaturesFile(filteredDependencyCache);
                Feature oldFeature = oldfeatures.getFeature().get(0);

                List<Bundle> addedBundles = new ArrayList<>(feature.getBundle());
                List<Bundle> removedBundles = new ArrayList<>();
                for (Bundle test : oldFeature.getBundle()) {
                    boolean t1 = addedBundles.contains(test);
                    int s1 = addedBundles.size();
                    boolean t2 = addedBundles.remove(test);
                    int s2 = addedBundles.size();
                    if (t1 != t2) {
                        getLog().warn("dependencies.contains: " + t1 + ", dependencies.remove(test): " + t2);
                    }
                    if (t1 == (s1 == s2)) {
                        getLog().warn("dependencies.contains: " + t1 + ", size before: " + s1 + ", size after: " + s2);
                    }
                    if (!t2) {
                        removedBundles.add(test);
                    }
                }

                List<Dependency> addedDependencys = new ArrayList<>(feature.getFeature());
                List<Dependency> removedDependencys = new ArrayList<>();
                for (Dependency test : oldFeature.getFeature()) {
                    boolean t1 = addedDependencys.contains(test);
                    int s1 = addedDependencys.size();
                    boolean t2 = addedDependencys.remove(test);
                    int s2 = addedDependencys.size();
                    if (t1 != t2) {
                        getLog().warn("dependencies.contains: " + t1 + ", dependencies.remove(test): " + t2);
                    }
                    if (t1 == (s1 == s2)) {
                        getLog().warn("dependencies.contains: " + t1 + ", size before: " + s1 + ", size after: " + s2);
                    }
                    if (!t2) {
                        removedDependencys.add(test);
                    }
                }
                if (!addedBundles.isEmpty() || !removedBundles.isEmpty() || !addedDependencys.isEmpty() || !removedDependencys.isEmpty()) {
                    saveDependencyChanges(addedBundles, removedBundles, addedDependencys, removedDependencys, objectFactory);
                    if (overwriteChangedDependencies) {
                        writeDependencies(features, dependencyCache, useJson);
                    }
                } else {
                    getLog().info(saveTreeListing());
                }

            } else {
                writeDependencies(features, dependencyCache, useJson);
            }
        }
    }

    protected void saveDependencyChanges(Collection<Bundle> addedBundles, Collection<Bundle> removedBundles, Collection<Dependency> addedDependencys, Collection<Dependency> removedDependencys, ObjectFactory objectFactory)
            throws Exception {
        File addedFile = new File(filteredDependencyCache.getParentFile(), "dependencies.added.xml");
        Features added = toFeatures(addedBundles, addedDependencys, objectFactory);
        writeDependencies(added, addedFile, useJson);

        File removedFile = new File(filteredDependencyCache.getParentFile(), "dependencies.removed.xml");
        Features removed = toFeatures(removedBundles, removedDependencys, objectFactory);
        writeDependencies(removed, removedFile, useJson);

        StringWriter out = new StringWriter();
        out.write(saveTreeListing());

        out.write("Dependencies have changed:\n");
        if (!addedBundles.isEmpty() || !addedDependencys.isEmpty()) {
            out.write("\tAdded dependencies are saved here: " + addedFile.getAbsolutePath() + "\n");
            if (logDependencyChanges) {
                if (useJson) {
                    JacksonUtil.marshal(added, out);
                } else {
                    JaxbUtil.marshal(added, out);
                }
            }
        }
        if (!removedBundles.isEmpty() || !removedDependencys.isEmpty()) {
            out.write("\tRemoved dependencies are saved here: " + removedFile.getAbsolutePath() + "\n");
            if (logDependencyChanges) {
                if (useJson) {
                    JacksonUtil.marshal(removed, out);
                } else {
                    JaxbUtil.marshal(removed, out);
                }
            }
        }
        out.write("Delete " + dependencyCache.getAbsolutePath()
                + " if you are happy with the dependency changes.");

        if (failOnDependencyChange) {
            throw new MojoFailureException(out.toString());
        } else {
            getLog().warn(out.toString());
        }
    }

    private static Features toFeatures(Collection<Bundle> addedBundles, Collection<Dependency> addedDependencys, ObjectFactory objectFactory) {
        Features features = objectFactory.createFeaturesRoot();
        Feature feature = objectFactory.createFeature();
        feature.getBundle().addAll(addedBundles);
        feature.getFeature().addAll(addedDependencys);
        features.getFeature().add(feature);
        return features;
    }


    private static void writeDependencies(Features features, File file, boolean useJson) throws JAXBException, IOException {
        file.getParentFile().mkdirs();
        if (!file.getParentFile().exists() || !file.getParentFile().isDirectory()) {
            throw new IOException("Cannot create directory at " + file.getParent());
        }
        try (OutputStream out = new FileOutputStream(file)) {
            if (useJson) {
                JacksonUtil.marshal(features, out);
            } else {
                JaxbUtil.marshal(features, out);
            }
        }
    }

    protected void filter(File sourceFile, File targetFile)
            throws MojoExecutionException {
        try {

            if (StringUtils.isEmpty(encoding)) {
                getLog().warn(
                        "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                                + ", i.e. build is platform dependent!");
            }
            targetFile.getParentFile().mkdirs();

            final MavenResourcesExecution mre = new MavenResourcesExecution();
            mre.setMavenProject(project);
            mre.setMavenSession(mavenSession);
            mre.setFilters(null);
            mre.setEscapedBackslashesInFilePath(true);
            final LinkedHashSet<String> delimiters = new LinkedHashSet<>();
            delimiters.add("${*}");
            mre.setDelimiters(delimiters);

            @SuppressWarnings("rawtypes")
            List filters = mavenFileFilter.getDefaultFilterWrappers(mre);
            mavenFileFilter.copyFile(sourceFile, targetFile, true, filters, encoding, true);
        } catch (MavenFilteringException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected String saveTreeListing() throws IOException {
        File treeListFile = new File(filteredDependencyCache.getParentFile(), "treeListing.txt");
        try (OutputStream os = new FileOutputStream(treeListFile);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
            writer.write(treeListing);
        }
        return "\tTree listing is saved here: " + treeListFile.getAbsolutePath() + "\n";
    }

    private static final class FeaturesCache {
        // Maven-to-Aether Artifact cache, as parsing strings is expensive
        private final SimpleLRUCache<String, DefaultArtifact> artifactCache;
        private final SimpleLRUCache<File, Features> featuresCache;

        FeaturesCache(int featuresCacheSize, int artifactCacheSize) {
            featuresCache = new SimpleLRUCache<>(featuresCacheSize);
            artifactCache = new SimpleLRUCache<>(artifactCacheSize);
        }

        DefaultArtifact getArtifact(String mavenName) {
            return artifactCache.computeIfAbsent(mavenName, MavenUtil::mvnToArtifact);
        }

        Features getFeature(final File featuresFile) throws XMLStreamException, JAXBException, IOException {
            final Features existing = featuresCache.get(featuresFile);
            if (existing != null) {
                return existing;
            }

            final Features computed = readFeaturesFile(featuresFile);
            featuresCache.put(featuresFile, computed);
            return computed;
        }
    }
}
