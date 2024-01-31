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

import static org.apache.karaf.features.internal.service.Overrides.OVERRIDE_RANGE;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionCleaner;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.LocationPattern;
import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.ConfigFile;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.service.Overrides;
import org.apache.karaf.tooling.utils.MavenUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.osgi.framework.Version;

/**
 * Add features to a repository directory
 */
@Mojo(name = "features-add-to-repository", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class AddToRepositoryMojo extends AbstractFeatureMojo {

    @Parameter(defaultValue = "${project.build.directory}/features-repo")
    protected File repository;

    /**
     * If set to true the exported bundles will be directly copied into the repository dir.
     * If set to false the default maven repository layout will be used
     */
    @Parameter
    private boolean flatRepoLayout;

    @Parameter
    protected List<CopyFileBasedDescriptor> copyFileBasedDescriptors;

    @Parameter(defaultValue = "false")
    private boolean timestampedSnapshot;

    /**
     * Location of <code>overrides.properties</code> file.
     */
    @Parameter
    protected String overridesURI;

    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<Feature> featuresSet = resolveFeatures();

        Set<String> overrideLines = Overrides.loadOverrides(overridesURI);
        Map<String,Override> overrides = new HashMap<>();
        overrideLines.forEach((String overrideLine) -> {
            Override override = new Override(overrideLine);
            try {
                overrides.put(override.getMatchLocation(), override);
            } catch (MalformedURLException e) {
                getLog().warn("unable to parse overrides.properties entry: " + overrideLine, e);
            }
        });
        getLog().debug(String.format("found %d overrides in %s: %s", overrides.size(), overridesURI, String.join(", ", overrides.keySet())));

        for (Artifact descriptor : descriptorArtifacts) {
            copy(descriptor, repository);
        }

        for (Feature feature : featuresSet) {
            copyBundlesToDestRepository(feature.getBundle().stream().map(bundle -> this.override(bundle, overrides)).collect(Collectors.toList()));
            for(Conditional conditional : feature.getConditional()) {
                copyBundlesConditionalToDestRepository(conditional.getBundles().stream().map(bundle -> this.override(bundle, overrides)).collect(Collectors.toList()));
            }
            copyConfigFilesToDestRepository(feature.getConfigfile());
        }
        
        copyFileBasedDescriptorsToDestRepository();
        
    }

    private void copyBundlesConditionalToDestRepository(List<? extends BundleInfo> artifactRefsConditional) throws MojoExecutionException {
        for (BundleInfo artifactRef : artifactRefsConditional) {
            if (ignoreDependencyFlag || !artifactRef.isDependency()) {
                Artifact artifact = resourceToArtifact(artifactRef.getLocation(), skipNonMavenProtocols);
                // Avoid getting NPE on artifact.getFile in some cases 
                resolveArtifact(artifact, remoteRepos);
                if (artifact != null) {
                    copy(artifact, repository);
                }
            }
        }
    }
    
    private void copyBundlesToDestRepository(List<? extends Bundle> artifactRefs) throws MojoExecutionException {
        for (Bundle artifactRef : artifactRefs) {
            Artifact artifact = resourceToArtifact(artifactRef.getLocation(), skipNonMavenProtocols);
            // Avoid getting NPE on artifact.getFile in some cases 
            resolveArtifact(artifact, remoteRepos);
            if (artifact != null) {
                copy(artifact, repository);
            }
        }
    }

    private void copyConfigFilesToDestRepository(List<? extends ConfigFile> artifactRefs) throws MojoExecutionException {
        for (ConfigFile artifactRef : artifactRefs) {
            Artifact artifact = resourceToArtifact(artifactRef.getLocation(), skipNonMavenProtocols);
            // Avoid getting NPE on artifact.getFile in some cases
            resolveArtifact(artifact, remoteRepos);
            if (artifact != null) {
                copy(artifact, repository);
            }
        }
    }

    protected void copy(Artifact artifact, File destRepository) {
        try {
            getLog().info("Copying artifact: " + artifact);
            File destFile = new File(destRepository, getRelativePath(artifact));
            if (artifact.getFile() == null) {
                throw new IllegalStateException("Artifact is not present in local repo."); 
            }
            copy(artifact.getFile(), destFile);
        } catch (Exception e) {
            getLog().warn("Error copying artifact " + artifact, e);
        }
    }

    /**
     * Get relative path for artifact
     * TODO consider DefaultRepositoryLayout
     * @param artifact
     * @return relative path of the given artifact in a default repo layout
     */
    private String getRelativePath(Artifact artifact) {
        String dir = (this.flatRepoLayout) ? "" : MavenUtil.getDir(artifact);
        String name = MavenUtil.getFileName(artifact, timestampedSnapshot);
        return dir + name;
    }

    private void copyFileBasedDescriptorsToDestRepository() {
        if (copyFileBasedDescriptors != null) {
            for (CopyFileBasedDescriptor fileBasedDescriptor : copyFileBasedDescriptors) {
                File destDir = new File(repository, fileBasedDescriptor.getTargetDirectory());
                File destFile = new File(destDir, fileBasedDescriptor.getTargetFileName());
                copy(fileBasedDescriptor.getSourceFile(), destFile);
            }
        }
    }

    private <T extends BundleInfo> T override(T bundle, Map<String, Override> overrides) {
        try {
            String bundleLocation = Override.parseMatchLocation(bundle.getLocation());
            if (overrides.containsKey(bundleLocation)) {
                Override override = overrides.get(bundleLocation);
                getLog().debug("checking bundle " + bundle.getLocation() + " for overrides.properties clause " + override.location);
                if (shouldOverride(bundle, override)) {
                    getLog().debug("overrides.properties overriding " + bundle.getLocation() + " to " + override.location);
                    return override.getBundle();
                }
            } else {
                getLog().debug("no overrides.properties entry matching bundle " + bundleLocation);
            }
        } catch (MalformedURLException e) {
            getLog().warn("unable to parse groupId and artifactId for bundle " + bundle.getLocation(), e);
        }
        return bundle;
    }

    boolean shouldOverride(BundleInfo bundle, Override override) {
        String bundleLocationString = bundle.getLocation();

        try {
            if (!override.matchesLocation(bundleLocationString)) {
                getLog().debug(override.getMatchLocation() + " does not match " + Override.parseMatchLocation(bundleLocationString) + " from " + bundleLocationString);
                return false;
            }
        } catch (MalformedURLException e) {
            getLog().warn("failed while trying to parse bundle location " + bundleLocationString, e);
            return false;
        }

        LocationPattern bundleLocation = new LocationPattern(bundleLocationString);
        Version bundleVersion = new Version(VersionCleaner.clean(bundleLocation.getVersionString()));
        Version overrideVersion = override.getBundleVersion();

        VersionRange overrideRange = override.getVersionRange();

        return overrideRange.contains(bundleVersion) && bundleVersion.compareTo(overrideVersion) < 0;
    }

    public static Clause parseClause(String override) {
        Clause[] cs = Parser.parseClauses(new String[]{override});
        if (cs.length != 1) {
            throw new IllegalStateException("Override contains more than one clause: " + override);
        }
        return cs[0];
    }

    static class Override {
        private String location;
        private String range;

        /*
         * cache objects for when we iterate
         */
        private Bundle bundle;
        private Version bundleVersion;
        private VersionRange versionRange;
        private String matchLocation;
        private LocationPattern locationPattern;

        public Override(String override) {
            init(parseClause(override));
        }

        protected void init(Clause clause) {
            this.location = clause.getName();
            this.range = clause.getAttribute(OVERRIDE_RANGE);
        }

        @SuppressWarnings("unchecked")
        public <T extends BundleInfo> T getBundle() {
            if (this.bundle == null) {
                this.bundle = new Bundle(this.location);
            }
            return (T)this.bundle;
        }

        public Version getBundleVersion() {
            if (this.bundleVersion == null) {
                LocationPattern bundleLocation = this.getLocationPattern();
                this.bundleVersion = new Version(VersionCleaner.clean(bundleLocation.getVersionString()));
            }
            return this.bundleVersion;
        }

        public LocationPattern getLocationPattern() {
            if (this.locationPattern == null) {
                this.locationPattern = new LocationPattern(this.location);
            }
            return this.locationPattern;
        }

        public VersionRange getVersionRange() {
            if (this.versionRange == null) {
                if (this.range == null) {
                    /**
                     * In the runtime {@link Overrides} code, this uses the bundle compatibility inside
                     * the resource, which is usually looser (matching major.minor.0), so I'm doing
                     * this slightly differently than the code in Overrides and explicitly making the
                     * range include all micro versions.
                     */
                    Version mavenBundleVersion = this.getBundleVersion();
                    Version v1 = new Version(mavenBundleVersion.getMajor(), mavenBundleVersion.getMinor(), 0);
                    Version v2 = new Version(mavenBundleVersion.getMajor(), mavenBundleVersion.getMinor() + 1, 0);
                    this.versionRange = new VersionRange(false, v1, v2, true);
                } else {
                    this.versionRange = new VersionRange(this.range);
                }
            }
            return this.versionRange;
        }

        public boolean matchesLocation(String locationString) throws MalformedURLException {
            if (locationString == null) return false;

            return parseMatchLocation(locationString).equals(getMatchLocation());
        }

        private static String parseMatchLocation(String location) throws MalformedURLException {
            if (location == null) return null;

            String locationString = location.startsWith("mvn:") ? location.substring(4) : location;
            org.apache.karaf.util.maven.Parser parser = new org.apache.karaf.util.maven.Parser(locationString);
            return parser.getGroup() + "/" + parser.getArtifact();
        }

        private String getMatchLocation() throws MalformedURLException {
            if (this.matchLocation == null) {
                this.matchLocation = parseMatchLocation(location);
            }

            return this.matchLocation;
        }

    }
}
