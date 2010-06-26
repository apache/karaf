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
package org.apache.felix.karaf.tooling.features;

import static org.apache.felix.karaf.tooling.features.ManifestUtils.getExports;
import static org.apache.felix.karaf.tooling.features.ManifestUtils.getMandatoryImports;
import static org.apache.felix.karaf.tooling.features.ManifestUtils.matches;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.jar.Manifest;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.felix.karaf.features.Feature;
import org.apache.felix.karaf.features.Repository;
import org.apache.felix.karaf.features.internal.RepositoryImpl;
import org.apache.felix.utils.manifest.Clause;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.DefaultArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;

/**
 * Validates a features XML file
 * 
 * @version $Revision: 1.1 $
 * @goal validate
 * @execute phase="process-resources"
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Validates the features XML file
 */
@SuppressWarnings("unchecked")
public class ValidateFeaturesMojo extends MojoSupport {

    private static final String MVN_URI_PREFIX = "mvn:";
    private static final String MVN_REPO_SEPARATOR = "!";

    /**
     * The dependency tree builder to use.
     *
     * @component
     * @required
     * @readonly
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * The file to generate
     * 
     * @parameter default-value="${project.build.directory}/classes/features.xml"
     */
    private File file;

    /**
     * karaf config.properties
     * 
     * @parameter default-value="config.properties"
     */
    private String karafConfig;
    
    /**
     * which jre version we wanna parse to get jre exported package in config.properties
     * 
     * @parameter default-value="jre-1.5"
     */
    private String jreVersion;

    /**
     *  The repositories which are included from the plugin config   
     *  @parameter 
     */
     private List<String> repositories;   
    
    /*
     * A map to cache the mvn: uris and the artifacts that correspond with them
     */
    private Map<String, Artifact> bundles = new HashMap<String, Artifact>();

    /*
     * A map to cache manifests that have been extracted from the bundles
     */
    private Map<Artifact, Manifest> manifests = new HashMap<Artifact, Manifest>();

    /*
     * The list of features, includes both the features to be validated and the features from included <repository>s
     */
    private Features features = new Features();

    /*
     * The packages exported by the features themselves -- useful when features depend on other features
     */
    private Map<String, Set<Clause>> featureExports = new HashMap<String, Set<Clause>>();

    /*
     * The set of packages exported by the system bundle and by Karaf itself
     */
    private Set<String> systemExports = new HashSet<String>();

    /**
     * The Mojo's main method
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            prepare();
            Repository repository = new RepositoryImpl(file.toURI());
            analyze(repository);
            validate(repository);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException(String.format("Unable to validate %s: %s", file.getAbsolutePath(), e.getMessage()), e);
        }

    }

    /*
     * Prepare for validation by determing system and Karaf exports
     */
    private void prepare() throws Exception {
        info("== Preparing for validation ==");
        info(" - getting list of system bundle exports");
        readSystemPackages();
        info(" - getting list of provided bundle exports");
        readProvidedBundles();
    }

    /*
     * Analyse the descriptor and any <repository>s that might be part of it
     */
    private void analyze(Repository repository) throws Exception {
        info("== Analyzing feature descriptor ==");
        info(" - read %s", file.getAbsolutePath());

        features.add(repository.getFeatures());
        
        // add the repositories from the plugin configuration
        if (repositories != null) {
        	for (String uri : repositories) {
        		getLog().info(String.format(" - adding repository from %s", uri));
        		Repository dependency = new RepositoryImpl(URI.create(translateFromMaven(uri)));
        		features.add(dependency.getFeatures());
        		validateBundlesAvailable(dependency);
        		analyzeExports(dependency);
        	}
        }

        for (URI uri : repository.getRepositories()) {
            Artifact artifact = resolve(uri.toString());
            Repository dependency  = new RepositoryImpl(new File(localRepo.getBasedir(), localRepo.pathOf(artifact)).toURI());
            getLog().info(String.format(" - adding %d known features from %s", dependency.getFeatures().length, uri));
            features.add(dependency.getFeatures());
            // we need to do this to get all the information ready for further processing
            validateBundlesAvailable(dependency);
            analyzeExports(dependency);
        }

    }

    /*
     * Perform the actual validation
     */
    private void validate(Repository repository) throws Exception {
        info("== Validating feature descriptor ==");
        info(" - validating %d features", repository.getFeatures().length);
        info(" - step 1: Checking if all artifacts exist");
        validateBundlesAvailable(repository);
        info("    OK: all %d OSGi bundles have been found", bundles.size());
        info(" - step 2: Checking if all imports for bundles can be resolved");
        validateImportsExports(repository);
        info("== Done! ==========================");
    }


    /*
     * Determine list of exports by bundles that have been marked provided in the pom
     * //TODO: we probably want to figure this out somewhere from the Karaf build itself instead of putting the burden on the user
     */
    private void readProvidedBundles() throws Exception {
        DependencyNode tree = dependencyTreeBuilder.buildDependencyTree(project, localRepo, factory, artifactMetadataSource, new ArtifactFilter() {

            public boolean include(Artifact artifact) {
                return true;
            }

        }, new DefaultArtifactCollector());
        tree.accept(new DependencyNodeVisitor() {
            public boolean endVisit(DependencyNode node) {
                // we want the next sibling too
                return true;
            }

            public boolean visit(DependencyNode node) {
                if (node.getState() != DependencyNode.OMITTED_FOR_CONFLICT) {
                    Artifact artifact = node.getArtifact();
                    info("    scanning %s for exports", artifact);
                    if (Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) && !artifact.getType().equals("pom")) {
                        try {
                            for (Clause clause : ManifestUtils.getExports(getManifest(artifact))) {
                                getLog().debug(" adding " + clause.getName() + " to list of available packages");
                                systemExports.add(clause.getName());
                            }
                        } catch (ArtifactResolutionException e) {
                            error("Unable to find bundle exports for %s: %s", e, artifact, e.getMessage());
                        } catch (ArtifactNotFoundException e) {
                            error("Unable to find bundle exports for %s: %s", e, artifact, e.getMessage());
                        } catch (IOException e) {
                            error("Unable to find bundle exports for %s: %s", e, artifact, e.getMessage());
                        }
                    }
                }
                // we want the children too
                return true;
            }
        });
    }

    /*
     * Read system packages from a properties file
     * //TODO: we should probably grab this file from the Karaf distro itself instead of duplicating it in the plugin
     */
    private void readSystemPackages() throws IOException {
        Properties properties = new Properties();
        if (karafConfig.equals("config.properties")) {
        	properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
        } else {
        	properties.load(new FileInputStream(new File(karafConfig)));
        }

        String packages = (String) properties.get(jreVersion);
        for (String pkg : packages.split(";")) {
            systemExports .add(pkg.trim());
        }
        for (String pkg : packages.split(",")) {
            systemExports .add(pkg.trim());
        }
    }

    /*
     * Analyze exports in all features in the repository without validating the features
     * (e.g. used for <repository> elements found in a descriptor)
     */
    private void analyzeExports(Repository repository) throws Exception {
        for (Feature feature : repository.getFeatures()) {
            Set<Clause> exports = new HashSet<Clause>();
            for (String bundle : feature.getBundles()) {
                exports.addAll(getExports(getManifest(bundles.get(bundle))));
            }
            info("    scanning feature %s for exports", feature.getName());
            featureExports.put(feature.getName(), exports);
        }
    }

    /*
     * Check if all the bundles can be downloaded and are actually OSGi bundles and not plain JARs
     */
    private void validateBundlesAvailable(Repository repository) throws Exception {
        for (Feature feature : repository.getFeatures()) {
            for (String bundle : feature.getBundles()) {
                // this will throw an exception if the artifact can not be resolved
                final Artifact artifact = resolve(bundle);
                bundles.put(bundle, artifact);
                if (isBundle(artifact)) {
                    manifests.put(artifact, getManifest(artifact));
                } else {
                    throw new Exception(String.format("%s is not an OSGi bundle", bundle));
                }
            }
        }
    }

    /*
     * Validate if all features in a repository have bundles which can be resolved
     */
    private void validateImportsExports(Repository repository) throws ArtifactResolutionException, ArtifactNotFoundException, Exception {
        for (Feature feature : repository.getFeatures()) {
            // make sure the feature hasn't been validated before as a dependency
            if (!featureExports.containsKey(feature.getName())) {
                validateImportsExports(feature);
            }
        }
    }

    /*
     * Validate if all imports for a feature are being matched with exports
     */
    private void validateImportsExports(Feature feature) throws Exception {
        Map<Clause, String> imports = new HashMap<Clause, String>();
        Set<Clause> exports = new HashSet<Clause>();
        for (Feature dependency : feature.getDependencies()) {
            if (featureExports.containsKey(dependency.getName())) {
                exports.addAll(featureExports.get(dependency.getName()));
            } else {
                validateImportsExports(features.get(dependency.getName(), dependency.getVersion()));
            }
        }
        for (String bundle : feature.getBundles()) {
            Manifest meta = manifests.get(bundles.get(bundle));
            exports.addAll(getExports(meta));
            for (Clause clause : getMandatoryImports(meta)) {
                imports.put(clause, bundle);   
            }
        }

        // setting up the set of required imports
        Set<Clause> requirements = new HashSet<Clause>();
        requirements.addAll(imports.keySet());

        // now, let's remove requirements whenever we find a matching export for them
        for (Clause element : imports.keySet()) {
            if (systemExports.contains(element.getName())) {
                debug("%s is resolved by a system bundle export or provided bundle", element);
                requirements.remove(element);
                continue;
            }
            for (Clause export : exports) {
                if (matches(element, export)) {
                    debug("%s is resolved by export %s", element, export);
                    requirements.remove(element);
                    continue;
                }
                debug("%s is not resolved by export %s", element, export);
            }
        }

        // if there are any more requirements left here, there's a problem with the feature 
        if (!requirements.isEmpty()) {
            warn("Failed to validate feature %s", feature.getName());
            for (Clause entry : requirements) {
                warn("No export found to match %s (imported by %s)",
                     entry, imports.get(entry));
            }
            throw new Exception(String.format("%d unresolved imports in feature %s",
                                              requirements.size(), feature.getName()));
        }
        info("    OK: imports resolved for %s", feature.getName());
        featureExports.put(feature.getName(), exports);
    }    

    /*
     * Check if the artifact is an OSGi bundle
     */
    private boolean isBundle(Artifact artifact) {
        if ("bundle".equals(artifact.getArtifactHandler().getPackaging())) {
            return true;
        } else {
            try {
                return ManifestUtils.isBundle(getManifest(artifact));
            } catch (ZipException e) {
                getLog().debug("Unable to determine if " + artifact + " is a bundle; defaulting to false", e);
            } catch (IOException e) {
                getLog().debug("Unable to determine if " + artifact + " is a bundle; defaulting to false", e);
            } catch (Exception e) {
                getLog().debug("Unable to determine if " + artifact + " is a bundle; defaulting to false", e);
            }
        }
        return false;
    }

    /*
     * Extract the META-INF/MANIFEST.MF file from an artifact
     */
    private Manifest getManifest(Artifact artifact) throws ArtifactResolutionException, ArtifactNotFoundException, 
                                                           ZipException, IOException {
        File localFile = new File(localRepo.pathOf(artifact));
        ZipFile file;
        if (localFile.exists()) {
            // avoid going over to the repository if the file is already on the disk
            file = new ZipFile(localFile);
        } else {
            resolver.resolve(artifact, remoteRepos, localRepo);
            file = new ZipFile(artifact.getFile());
        }
        // let's replace syserr for now to hide warnings being issues by the Manifest reading process
        PrintStream original = System.err;
        try {
            System.setErr(new PrintStream(new ByteArrayOutputStream()));
            return new Manifest(file.getInputStream(file.getEntry("META-INF/MANIFEST.MF")));
        } finally {
            System.setErr(original);
        }
    }

    /*
     * Resolve an artifact, downloading it from remote repositories when necessary
     */
    private Artifact resolve(String bundle) throws Exception, ArtifactNotFoundException {
        Artifact artifact = getArtifact(bundle);
        if (bundle.indexOf(MVN_REPO_SEPARATOR) >= 0) {
            if (bundle.startsWith(MVN_URI_PREFIX)) {
                bundle = bundle.substring(MVN_URI_PREFIX.length());
            }
            String repo = bundle.substring(0, bundle.indexOf(MVN_REPO_SEPARATOR));
            ArtifactRepository repository = new DefaultArtifactRepository(artifact.getArtifactId() + "-repo", repo,
                                                                          new DefaultRepositoryLayout());
            List<ArtifactRepository> repos = new LinkedList<ArtifactRepository>();
            repos.add(repository);
            resolver.resolve(artifact, repos, localRepo);
        } else {
            resolver.resolve(artifact, remoteRepos, localRepo);
        }
        if (artifact == null) {
            throw new Exception("Unable to resolve artifact for uri " + bundle);
        } else {
            return artifact;
        }
    }

    /*
     * Create an artifact for a given mvn: uri
     */
    private Artifact getArtifact(String uri) {
        // remove the mvn: prefix when necessary
        if (uri.startsWith(MVN_URI_PREFIX)) {
            uri = uri.substring(MVN_URI_PREFIX.length());
        }
        // remove the repository url when specified
        if (uri.contains(MVN_REPO_SEPARATOR)) {
            uri = uri.split(MVN_REPO_SEPARATOR)[1];
        }
        String[] elements = uri.split("/");
        switch (elements.length) {
        case 5:
            return factory.createArtifactWithClassifier(elements[0], elements[1], elements[2], elements[3], elements[4]);
        case 3:
            return factory.createArtifact(elements[0], elements[1], elements[2], Artifact.SCOPE_PROVIDED, "jar");
        default:
            return null;
        }
        
    }

    /*
     * Helper method for debug logging
     */
    private void debug(String message, Object... parms) {
        if (getLog().isDebugEnabled()) {
            getLog().debug(String.format(message, parms));
        }
    }

    /*
     * Helper method for info logging
     */
    private void info(String message, Object... parms) {
        getLog().info(String.format(message, parms));
    }

    /*
     * Helper method for warn logging
     */
    private void warn(String message, Object... parms) {
        getLog().warn(String.format(message, parms));
    }

    /*
     * Helper method for error logging
     */
    private void error(String message, Exception error, Object... parms) {
        getLog().error(String.format(message, parms), error);
    }

    /*
     * Convenience collection for holding features
     */
    private class Features {
        
        private List<Feature> features = new LinkedList<Feature>();
        
        public void add(Feature feature) {
           features.add(feature); 
        }

        public Feature get(String name, String version) throws Exception {
            for (Feature feature : features) {
                if (name.equals(feature.getName()) && version.equals(feature.getVersion())) {
                    return feature;
                }
            }
            throw new Exception(String.format("Unable to find definition for feature %s (version %s)",
                                              name, version));
        }

        public void add(Feature[] array) {
            for (Feature feature : array) {
                add(feature);
            }   
        }
    }
}
