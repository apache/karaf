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

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Dependency;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.apache.karaf.features.internal.model.ObjectFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResult;
import org.xml.sax.SAXException;

import static org.apache.karaf.deployer.kar.KarArtifactInstaller.FEATURE_CLASSIFIER;

/**
 * Generates the features XML file
 * NB this requires a recent maven-install-plugin such as 2.3.1
 *
 * @goal features-generate-descriptor
 * @phase compile
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Generates the features XML file starting with an optional source feature.xml and adding
 * project dependencies as bundles and feature/car dependencies
 */
@SuppressWarnings("unchecked")
public class GenerateDescriptorMojo extends AbstractLogEnabled implements Mojo {

    /**
     * An (optional) input feature file to extend.  This is highly recommended as it is the only way to add <code>&lt;feature/&gt;</code>
     * elements to the individual features that are generated.  Note that this file is filtered using standard Maven
     * resource interpolation, allowing attributes of the input file to be set with information such as ${project.version}
     * from the current build.
     * <p/>
     * When dependencies are processed, if they are duplicated in this file, the dependency here provides the baseline
     * information and is supplemented by additional information from the dependency.
     *
     * @parameter default-value="${project.basedir}/src/main/feature/feature.xml"
     */
    private File inputFile;

    /**
     * (wrapper) The filtered input file. This file holds the result of Maven resource interpolation and is generally
     * not necessary to change, although it may be helpful for debugging.
     *
     * @parameter default-value="${project.build.directory}/feature/filteredInputFeature.xml"
     */
    private File filteredInputFile;

    /**
     * (wrapper) The file to generate.  This file is attached as a project output artifact.
     *
     * @parameter default-value="${project.build.directory}/feature/feature.xml"
     */
    private File outputFile;

    /**
     * The resolver to use for the feature.  Normally null or "OBR" or "(OBR)"
     *
     * @parameter default-value="${resolver}"
     */
    private String resolver;

    /**
     * (wrapper) The artifact type for attaching the generated file to the project
     *
     * @parameter default-value="xml"
     */
    private String attachmentArtifactType = "xml";

    /**
     * (wrapper) The artifact classifier for attaching the generated file to the project
     *
     * @parameter default-value="features"
     */
    private String attachmentArtifactClassifier = "features";

    /**
     * Specifies whether features dependencies of this project will be included inline of the the
     * final output (<code>true</code>) or simply referenced as output artifact dependencies (<code>false</code>).
     * If <code>true</code>, feature dependencies xml descriptors are read and their contents added to the features descriptor under assembly.
     * If <code>false</code>, feature dependencies are added to the assembled feature as dependencies.
     * Setting this value to <code>true</code> is especially helpful in multiproject builds where subprojects build their own features
     * using <code>aggregateFeatures = false</code>, then combined with <code>aggregateFeatures = true</code> in an
     * aggregation project with explicit dependencies to the child projects.
     *
     * @parameter default-value="false"
     */
    private boolean aggregateFeatures = false;

    /**
     * If present, the bundles added to the feature constructed from the dependencies will be marked with this default
     * startlevel.  If this parameter is not present, no startlevel attribute will be created. Finer resolution for specific
     * dependencies can be obtained by specifying the dependency in the file referenced by the <code>inputFile</code> parameter.
     *
     * @parameter
     */
    private Integer startLevel;

    /**
     * Installation mode. If present, generate "feature.install" attribute:
     * 
     * <a href="https://github.com/apache/karaf/blob/trunk/features/core/src/main/resources/org/apache/karaf/features/karaf-features-1.1.0.xsd">
     * Installation mode.
     * </a>
     *
     * Can be either manual or auto. Specifies whether the feature should be automatically installed when
     * dropped inside the deploy folder. Note: This attribute doesn't affect feature descriptors that are installed from the
     * command line or as part of the org.apache.karaf.features.cfg.
     *
     * @parameter
     */
    private String installMode;

    /**
     * Flag indicating whether transitive dependencies should be included (<code>true</code>) or not (<code>false</code>).
     * <p/>
     * N.B. Note the default value of this is true, but is suboptimal in cases where specific <code>&lt;feature/&gt;</code> dependencies are
     * provided by the <code>inputFile</code> parameter.
     *
     * @parameter default-value="true"
     */
    private boolean includeTransitiveDependency;

    /**
     * The standard behavior is to add dependencies as <code>&lt;bundle&gt;</code> elements to a <code>&lt;feature&gt;</code>
     * with the same name as the artifactId of the project.  This flag disables that behavior.
     *
     * @parameter default-value="true"
     */
    private boolean addBundlesToPrimaryFeature;

    // *************************************************
    // READ-ONLY MAVEN PLUGIN PARAMETERS
    // *************************************************

    /**
     * (wrapper) The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The maven project's helper.
     *
     * @component
     * @required
     * @readonly
     */
    protected MavenProjectHelper projectHelper;

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     * @required
     * @readonly
     */
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @required
     * @readonly
     */
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of project dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    private List<RemoteRepository> projectRepos;

    /**
     * The project's remote repositories to use for the resolution of plugins and their dependencies.
     *
     * @parameter default-value="${project.remotePluginRepositories}"
     * @required
     * @readonly
     */
    private List<RemoteRepository> pluginRepos;

    /**
     * @component role="org.apache.maven.shared.filtering.MavenResourcesFiltering" role-hint="default"
     * @required
     * @readonly
     */
    protected MavenResourcesFiltering mavenResourcesFiltering;

    /**
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * @plexus.requirement role-hint="default"
     * @component
     * @required
     * @readonly
     */
    protected MavenFileFilter mavenFileFilter;

    //dependencies we are interested in
    protected Map<Artifact, String> localDependencies;
    //log of what happened during search
    protected String treeListing;

    //maven log
    private Log log;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            DependencyHelper dependencyHelper = new DependencyHelper(pluginRepos, projectRepos, repoSession, repoSystem);
            dependencyHelper.getDependencies(project, includeTransitiveDependency);
            this.localDependencies = dependencyHelper.getLocalDependencies();
            this.treeListing = dependencyHelper.getTreeListing();
            File dir = outputFile.getParentFile();
            if (dir.isDirectory() || dir.mkdirs()) {
                PrintStream out = new PrintStream(new FileOutputStream(outputFile));
                try {
                    writeFeatures(out);
                } finally {
                    out.close();
                }
                // now lets attach it
                projectHelper.attachArtifact(project, attachmentArtifactType, attachmentArtifactClassifier, outputFile);

            } else {
                throw new MojoExecutionException("Could not create directory for features file: " + dir);
            }
        } catch (Exception e) {
            getLogger().error(e.getMessage());
            throw new MojoExecutionException("Unable to create features.xml file: " + e, e);
        }
    }
    
    private void ensureInstallMode(Feature feature){
    	if( feature == null ){
    		return;
    	}
    	if("auto".equalsIgnoreCase(installMode)){
            feature.setInstall("auto");
            return;
    	}
    	// default
        feature.setInstall("manual");
    }

    /*
     * Write all project dependencies as feature
     */
    private void writeFeatures(PrintStream out) throws ArtifactResolutionException, ArtifactNotFoundException,
            IOException, JAXBException, SAXException, ParserConfigurationException, XMLStreamException, MojoExecutionException {
        getLogger().info("Generating feature descriptor file " + outputFile.getAbsolutePath());
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
            if (test.getName().equals(project.getArtifactId())) {
                feature = test;
            }
        }
        if (feature == null) {
            feature = objectFactory.createFeature();
            feature.setName(project.getArtifactId());
        }
        if (!feature.hasVersion()) {
            feature.setVersion(project.getArtifact().getBaseVersion());
        }
        if (feature.getDescription() == null) {
            feature.setDescription(project.getName());
        }
        if (resolver != null) {
            feature.setResolver(resolver);
        }
        
        ensureInstallMode(feature);
        
        if (project.getDescription() != null && feature.getDetails() == null) {
            feature.setDetails(project.getDescription());
        }
        for (Map.Entry<Artifact, String> entry : localDependencies.entrySet()) {
            Artifact artifact = entry.getKey();
            if (DependencyHelper.isFeature(artifact)) {
                if (aggregateFeatures && FEATURE_CLASSIFIER.equals(artifact.getClassifier())) {
                    File featuresFile = resolve(artifact);
                    if (featuresFile == null || !featuresFile.exists()) {
                        throw new MojoExecutionException("Cannot locate file for feature: " + artifact + " at " + featuresFile);
                    }
                    Features includedFeatures = readFeaturesFile(featuresFile);
                    //TODO check for duplicates?
                    features.getFeature().addAll(includedFeatures.getFeature());
                }
            } else if (addBundlesToPrimaryFeature) {
                String bundleName = MavenUtil.artifactToMvn(artifact);
                File bundleFile = resolve(artifact);
                Manifest manifest = getManifest(bundleFile);

                if (manifest == null || !ManifestUtils.isBundle(getManifest(bundleFile))) {
                    bundleName = "wrap:" + bundleName;
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
                    feature.getBundle().add(bundle);
                }
                if ("runtime".equals(entry.getValue())) {
                    bundle.setDependency(true);
                }
                if (startLevel != null && bundle.getStartLevel() == 0) {
                    bundle.setStartLevel(startLevel);
                }
            }
        }

        if ((!feature.getBundle().isEmpty() || !feature.getFeature().isEmpty()) && !features.getFeature().contains(feature)) {
            features.getFeature().add(feature);
        }

        JaxbUtil.marshal(features, out);
        try {
            checkChanges(features, objectFactory);
        } catch (Exception e) {
            throw new MojoExecutionException("Features contents have changed", e);
        }
        getLogger().info("...done!");
    }

    /**
     * Extract the MANIFEST from the give file.
     */

    private Manifest getManifest(File file) throws IOException {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
        } catch (Exception e) {
            getLogger().warn("Error while opening artifact", e);
            return null;
        }

        try {
            is.mark(256 * 1024);
            JarInputStream jar = new JarInputStream(is);
            Manifest m = jar.getManifest();
            if (m == null) {
                getLogger().warn("Manifest not present in the first entry of the zip - " + file.getName());
            }
            return m;
        } finally {
            if (is != null) { // just in case when we did not open bundle
                is.close();
            }
        }
    }

    private File resolve(Artifact artifact) {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(projectRepos);

        getLog().debug("Resolving artifact " + artifact +
                " from " + projectRepos);

        ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch (org.sonatype.aether.resolution.ArtifactResolutionException e) {
            getLog().warn("could not resolve " + artifact, e);
            return null;
        }

        getLog().debug("Resolved artifact " + artifact + " to " +
                result.getArtifact().getFile() + " from "
                + result.getRepository());
        return result.getArtifact().getFile();
    }


    private Features readFeaturesFile(File featuresFile) throws XMLStreamException, JAXBException, IOException {
        Features features;
        InputStream in = new FileInputStream(featuresFile);
        try {
            features = JaxbUtil.unmarshal(in, false);
        } finally {
            in.close();
        }
        return features;
    }

    public void setLog(Log log) {
        this.log = log;
    }

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
     *
     * @parameter default-value="false"
     */
    private boolean checkDependencyChange;

    /**
     * (wrapper) Location of dependency cache.  This file is generated to contain known dependencies and is generally
     * located in SCM so that it may be used across separate developer builds. This is parameter is ignored unless
     * <code>checkDependencyChange</code> is set to <code>true</code>.
     *
     * @parameter default-value="${basedir}/src/main/history/dependencies.xml"
     */
    private File dependencyCache;

    /**
     * Location of filtered dependency file.
     *
     * @parameter default-value="${basedir}/target/history/dependencies.xml"
     * @readonly
     */
    private File filteredDependencyCache;

    /**
     * Whether to fail on changed dependencies (default, <code>true</code>) or warn (<code>false</code>). This is parameter is ignored unless
     * <code>checkDependencyChange</code> is set to <code>true</code> and <code>dependencyCache</code> exists to compare
     * against.
     *
     * @parameter default-value="true"
     */
    private boolean failOnDependencyChange;

    /**
     * Copies the contents of dependency change logs that are generated to stdout. This is parameter is ignored unless
     * <code>checkDependencyChange</code> is set to <code>true</code> and <code>dependencyCache</code> exists to compare
     * against.
     *
     * @parameter default-value="false"
     */
    private boolean logDependencyChanges;

    /**
     * Whether to overwrite the file referenced by <code>dependencyCache</code> if it has changed.  This is parameter is
     * ignored unless <code>checkDependencyChange</code> is set to <code>true</code>, <code>failOnDependencyChange</code>
     * is set to <code>false</code> and <code>dependencyCache</code> exists to compare against.
     *
     * @parameter default-value="false"
     */
    private boolean overwriteChangedDependencies;

    //filtering support
    /**
     * The character encoding scheme to be applied when filtering resources.
     *
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     */
    protected String encoding;

    /**
     * Expression preceded with the String won't be interpolated
     * \${foo} will be replaced with ${foo}
     *
     * @parameter expression="${maven.resources.escapeString}"
     */
    protected String escapeString = "\\";

    /**
     * System properties.
     *
     * @parameter
     */
    protected Map<String, String> systemProperties;

    private Map<String, String> previousSystemProperties;

    private void checkChanges(Features newFeatures, ObjectFactory objectFactory) throws Exception, IOException, JAXBException, XMLStreamException {
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

            Collections.sort(feature.getBundle(), new Comparator<Bundle>() {

                public int compare(Bundle bundle, Bundle bundle1) {
                    return bundle.getLocation().compareTo(bundle1.getLocation());
                }
            });
            Collections.sort(feature.getFeature(), new Comparator<Dependency>() {
                public int compare(Dependency dependency, Dependency dependency1) {
                    return dependency.getName().compareTo(dependency1.getName());
                }
            });

            if (dependencyCache.exists()) {
                //filter dependencies file
                filter(dependencyCache, filteredDependencyCache);
                //read dependency types, convert to dependencies, compare.
                Features oldfeatures = readFeaturesFile(filteredDependencyCache);
                Feature oldFeature = oldfeatures.getFeature().get(0);

                List<Bundle> addedBundles = new ArrayList<Bundle>(feature.getBundle());
                List<Bundle> removedBundles = new ArrayList<Bundle>();
                for (Bundle test : oldFeature.getBundle()) {
                    boolean t1 = addedBundles.contains(test);
                    int s1 = addedBundles.size();
                    boolean t2 = addedBundles.remove(test);
                    int s2 = addedBundles.size();
                    if (t1 != t2) {
                        getLogger().warn("dependencies.contains: " + t1 + ", dependencies.remove(test): " + t2);
                    }
                    if (t1 == (s1 == s2)) {
                        getLogger().warn("dependencies.contains: " + t1 + ", size before: " + s1 + ", size after: " + s2);
                    }
                    if (!t2) {
                        removedBundles.add(test);
                    }
                }

                List<Dependency> addedDependencys = new ArrayList<Dependency>(feature.getFeature());
                List<Dependency> removedDependencys = new ArrayList<Dependency>();
                for (Dependency test : oldFeature.getFeature()) {
                    boolean t1 = addedDependencys.contains(test);
                    int s1 = addedDependencys.size();
                    boolean t2 = addedDependencys.remove(test);
                    int s2 = addedDependencys.size();
                    if (t1 != t2) {
                        getLogger().warn("dependencies.contains: " + t1 + ", dependencies.remove(test): " + t2);
                    }
                    if (t1 == (s1 == s2)) {
                        getLogger().warn("dependencies.contains: " + t1 + ", size before: " + s1 + ", size after: " + s2);
                    }
                    if (!t2) {
                        removedDependencys.add(test);
                    }
                }
                if (!addedBundles.isEmpty() || !removedBundles.isEmpty() || !addedDependencys.isEmpty() || !removedDependencys.isEmpty()) {
                    saveDependencyChanges(addedBundles, removedBundles, addedDependencys, removedDependencys, objectFactory);
                    if (overwriteChangedDependencies) {
                        writeDependencies(features, dependencyCache);
                    }
                } else {
                    getLog().info(saveTreeListing());
                }

            } else {
                writeDependencies(features, dependencyCache);
            }
        }
    }

    protected void saveDependencyChanges(Collection<Bundle> addedBundles, Collection<Bundle> removedBundles, Collection<Dependency> addedDependencys, Collection<Dependency> removedDependencys, ObjectFactory objectFactory)
            throws Exception {
        File addedFile = new File(filteredDependencyCache.getParentFile(), "dependencies.added.xml");
        Features added = toFeatures(addedBundles, addedDependencys, objectFactory);
        writeDependencies(added, addedFile);

        File removedFile = new File(filteredDependencyCache.getParentFile(), "dependencies.removed.xml");
        Features removed = toFeatures(removedBundles, removedDependencys, objectFactory);
        writeDependencies(removed, removedFile);

        StringWriter out = new StringWriter();
        out.write(saveTreeListing());

        out.write("Dependencies have changed:\n");
        if (!addedBundles.isEmpty() || !addedDependencys.isEmpty()) {
            out.write("\tAdded dependencies are saved here: " + addedFile.getAbsolutePath() + "\n");
            if (logDependencyChanges) {
                JaxbUtil.marshal(added, out);
            }
        }
        if (!removedBundles.isEmpty() || !removedDependencys.isEmpty()) {
            out.write("\tRemoved dependencies are saved here: " + removedFile.getAbsolutePath() + "\n");
            if (logDependencyChanges) {
                JaxbUtil.marshal(removed, out);
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

    private Features toFeatures(Collection<Bundle> addedBundles, Collection<Dependency> addedDependencys, ObjectFactory objectFactory) {
        Features features = objectFactory.createFeaturesRoot();
        Feature feature = objectFactory.createFeature();
        feature.getBundle().addAll(addedBundles);
        feature.getFeature().addAll(addedDependencys);
        features.getFeature().add(feature);
        return features;
    }


    private void writeDependencies(Features features, File file) throws JAXBException, IOException {
        file.getParentFile().mkdirs();
        if (!file.getParentFile().exists() || !file.getParentFile().isDirectory()) {
            throw new IOException("Cannot create directory at " + file.getParent());
        }
        FileOutputStream out = new FileOutputStream(file);
        try {
            JaxbUtil.marshal(features, out);
        } finally {
            out.close();
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
            List filters = mavenFileFilter.getDefaultFilterWrappers(project, null, true, session, null);
            mavenFileFilter.copyFile(sourceFile, targetFile, true, filters, encoding, true);
        } catch (MavenFilteringException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected String saveTreeListing() throws IOException {
        File treeListFile = new File(filteredDependencyCache.getParentFile(), "treeListing.txt");
        OutputStream os = new FileOutputStream(treeListFile);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
        try {
            writer.write(treeListing);
        } finally {
            writer.close();
        }
        return "\tTree listing is saved here: " + treeListFile.getAbsolutePath() + "\n";
    }

}
